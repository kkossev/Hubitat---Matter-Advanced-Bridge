# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/2.0.0/),
and this project follows Semantic Versioning where applicable.

> **Released vs BETA.** The current **released** version, and the one Hubitat Package Manager
> installs, is **1.9.1**. Versions marked *(BETA)* below were development builds that were never
> published as a package.
>
> **No `[Unreleased]` heading is used in this file (policy set 2026-08-16).** Every entry is filed
> under the current driver version — the one in `static String version()` — from the moment it is
> written. When that version is bumped, the previous section is closed and a new one is opened.
>
> **This file is authoritative for change detail (policy set 2026-08-13).** The `* ver. x.y.z` history
> block in the `Matter_Advanced_Bridge.groovy` header is now a one-or-two-line, user-facing summary per
> release; everything else — internal method names, cluster/attribute IDs, rationale, investigation
> notes — lives here. Entries up to 1.9.1 were derived from that header before it was shortened.
> **Scope: everything in this repository (policy corrected 2026-08-16).** Parent driver, component
> drivers, libraries and the user-facing documentation all record their change detail here. The
> `* ver.` history line in any driver header is a short user-facing summary only. Entries up to 1.9.1
> predate this correction and cover the parent driver only. For a plain-language, user-facing summary — including the
> 0.x alpha history from December 2023 onward — see
> [docs/user/project/revisions-history.md](docs/user/project/revisions-history.md).
>
> No git tags or GitHub releases exist for this repository yet, so version headings are not linked.

## [1.9.3] - 2026-08-19

**Not yet released via HPM.** Parent driver `1.9.3`; *Camera AV Stream* component `1.1.0`,
`matterLib` `1.4.5`. Matter 1.5.1 camera support, developed and hardware-verified against an
**Aqara Camera Hub G350 on firmware 4.5.70** (`SpecificationVersion` 1.5.1.0).

### Added

- **Camera child: mechanical PTZ (cluster `0x0552` CameraAvSettingsUserLevelManagement).** The
  parent now discovers, subscribes and routes `0x0552`, and the *Camera AV Stream* child exposes
  `pan`, `tilt`, `zoom` and `movementState` attributes plus five commands — **Ptz Set Position**,
  **Ptz Relative Move**, **Ptz Move To Preset**, **Ptz Save Preset** and **Ptz Remove Preset**,
  mapping to `MPTZSetPosition` (`0x00`), `MPTZRelativeMove` (`0x01`), `MPTZMoveToPreset` (`0x02`),
  `MPTZSavePreset` (`0x03`) and `MPTZRemovePreset` (`0x04`). Every field of the two move commands
  is optional in the spec, so only the axes the caller actually supplies are sent — a single-axis
  move never disturbs the other two. Values are clamped to the camera's own reported limits
  (`PanMin`/`PanMax` `0x0007`/`0x0008`, `TiltMin`/`TiltMax` `0x0005`/`0x0006`, `ZoomMax` `0x0004`)
  rather than to an assumed 0–100 range: the Aqara G350 reports pan ±170, tilt only −15…+24 and
  zoom 0…81. `MovementState` (`0x0009`) is subscribed — without it there is no feedback that a move
  finished. The position is re-read two seconds after the camera returns to `Idle`, but only if the
  camera has not reported it in the previous four seconds: the G350 does send the final
  `MPTZPosition` just before going `Idle`, so an unconditional read was a wasted round-trip on every
  move, while a quieter camera still gets the readback. Digital PTZ (`DPTZ`, feature bit 0) is
  deliberately not implemented: it needs an allocated video stream id, and the G350 does not
  advertise it.

  Verified on hardware: `ptzRelativeMove(panDelta: 5)` moved pan 40 → 45, and
  `ptzSetPosition(pan: -10, tilt: 20, zoom: 2)` landed all three axes exactly, confirming the signed
  little-endian encoding for negative values. Two G350 behaviours are worth knowing and are
  documented for users — panning trips the camera's own vision occupancy (`motion active` fired
  0.8 s after the command), and one pan-only *relative* move also shifted the reported tilt by two
  units even though no tilt field was transmitted.

  `MPTZSavePreset` is built differently from the other four commands: its `Name` argument is a
  `char_string`, which `matter.cmdField()` cannot express, so the payload is hand-built as raw TLV
  (`15` / `24 00 <id>` / `2C 01 <len> <utf8>` / `18`) and sent through the `invoke()` overload that
  takes a TLV string — the same approach the Door Lock component driver uses for `SetCredential`.
  Verified on hardware: `tlv=152400012C0105746573743218` saved preset 1 as `test2`, and the camera
  reported it back as `MPTZPresets=1:test2`.

  After a save, remove, or move, the affected attribute is re-read only if the camera has not
  already reported it within the last four seconds — the G350 reports both `MPTZPresets` and the
  final `MPTZPosition` on its own, so the unconditional readback was a duplicate every time.

- **Camera child: moving to a preset that does not exist is refused up front.** The camera silently
  ignored `MPTZMoveToPreset` for an unsaved preset, returning no InvokeResponse at all. The driver
  now checks the ID against the camera's `MPTZPresets` table when it has read one. Preset IDs are
  also 1-based per the Matter spec (`min="1"`), so `0` is correctly rejected.
- **Camera child: privacy modes and a master on/off switch.** `SoftRecordingPrivacyModeEnabled`
  (`0x0013`), `SoftLivestreamPrivacyModeEnabled` (`0x0014`) and the read-only `HardPrivacyModeOn`
  (`0x0015`) are now subscribed, named and surfaced as `softRecordingPrivacy`,
  `softLivestreamPrivacy` and `hardPrivacy`, with **Set Soft Recording Privacy** and **Set Soft
  Livestream Privacy** commands. The child also gains the `Switch` capability as a master control,
  mapped the way the SmartThings Matter camera driver maps it: `off()` enables both soft privacy
  modes, `on()` clears them; `switch` reads `on` when neither is enabled. `HardPrivacyModeOn`
  reflects the physical shutter and is never written.

  **Verified on hardware 2026-08-19, and worth recording because it contradicts the usual reading
  of the spec:** on the G350, a plain Matter soft-privacy write also closes the physical shutter.
  `off()` wrote `0x0013` and `0x0014` at 21:57:33, and the camera reported `HardPrivacyModeOn=true`
  1.25 s later, unprompted. The soft modes are normally described as a software-only stop that is
  independent of the hard/physical state; Aqara has wired the two together on firmware 4.5.70. The
  practical consequence for users is that **Off closes the shutter**, which is a stronger action
  than the capability name suggests.
- **Camera child: Night Vision is now settable.** `NightVision` (`0x0016`) is a writable
  `TriStateAutoEnum`, not the read-only value the driver and the documentation previously described.
  New **Set Night Vision** command with `Off`/`On`/`Auto`.
- **Camera child: vision occupancy.** A Matter 1.5 camera endpoint carries `OccupancySensing`
  (`0x0406`) with the `VIS` feature bit alongside `0x0551` — the G350 reports FeatureMap `0x0080`.
  The child gained the `MotionSensor` capability and maps `Occupancy` bit 0 to `motion`.

### Fixed

- **Camera child: the `0x0551` FeatureMap was decoded with an early draft bit map, so it reported
  the wrong feature names.** The released cluster (Matter 1.5.1, ClusterRevision 2) numbers the bits
  `0 Audio, 1 Video, 2 Snapshot, 3 Privacy, 4 Speaker, 5 ImageControl, 6 Watermark, 7 OnScreenDisplay,
  8 LocalStorage, 9 HighDynamicRange, 10 NightVision`. The driver had Video and Audio transposed,
  reported Snapshot as `Speaker`, Speaker as `NightVision`, and invented a `TwoWayTalk` bit at 10 —
  there is no such feature bit, two-way talk is advertised by the `TwoWayTalkSupport` attribute
  (`0x0009`) alone. The practical effect on the Aqara G350 (FeatureMap `0x041F`) was that snapshot
  support was hidden and a non-existent TwoWayTalk feature was announced; it now correctly reads
  `[Audio, Video, Snapshot, Privacy, Speaker, NightVision]`.
- **Camera child: `TwoWayTalkSupport` was decoded one value low.** The enum started at `HalfDuplex`
  instead of `NotSupported`, so the G350's `FullDuplex` (raw `2`) was logged as `Unknown(2)`.
  Corrected to `0 NotSupported, 1 HalfDuplex, 2 FullDuplex`.
- **A camera endpoint is no longer created as a Motion Sensor.** `getDeviceDriver()` tested
  `0x0406` before `0x0551`, and a Matter 1.5 camera endpoint carries both, so a freshly discovered
  camera resolved to *Matter Generic Component Motion Sensor*. The camera test now runs first, and
  also matches on `0x0552` for a camera that exposes PTZ on a separate endpoint. Existing installs
  were unaffected only because their child devices predate the firmware that added `0x0406`.
- **Camera child: capability structs are decoded instead of dumped raw.** `VideoSensorParams`,
  `MinViewportResolution` (previously mis-named `MinViewport`), `MicrophoneCapabilities`,
  `SpeakerCapabilities`, `SnapshotCapabilities`, `Viewport`, `SupportedStreamUsages` and
  `StreamUsagePriorities` now render as readable text — `1920x1080 @120fps` rather than
  `[3:120, 0:1920, 1:1080, 2:120]`. Both Matter struct wire shapes are handled (a flat map keyed by
  field id, and a list of `[tag:, value:]` entries). The attribute-name map also gained the
  remaining `0x0551` attributes (`HDRModeEnabled`, `NightVisionIllum`, `MicrophoneAGCEnabled`, the
  image-control, local-recording and status-light attributes) so other cameras decode cleanly.
- **Camera child: `Get Info` now covers both camera clusters.** Its progress bookkeeping is keyed by
  `cluster_attribute` rather than by attribute id alone, so `0x0551` and `0x0552` no longer collide,
  and attribute ids are normalised before comparison because the parent stores AttributeList
  entries as variable-width hex (`00`, `13`, `4000`).

### Documentation

- **SwitchBot Hub Mini Matter Enabled moved from Unknown to Confirmed.** A community report
  (forum user Radial, 2026-08-16) confirmed SwitchBot Bot and SwitchBot Curtain bridged
  successfully through the Hub Mini Matter Enabled. `docs/user/bridges/switchbot.md` records both
  devices, adds a setup note that each device must be explicitly added as a bridged device in the
  SwitchBot app's Hub Mini Matter setup, and keeps the non-Matter original Hub Mini distinguished.
  The compatibility matrix needed no change: the confirmed device type it tracks for this evidence
  (Window Covering) already read Confirmed for SwitchBot.
- **Aqara Soft Sensors documented, and HUB-65 closed with a mechanical explanation.** New "Aqara Soft
  Sensors" section in `docs/user/drivers/signal.md`, alongside the Aqara Signals section it is
  compared against. A Soft Sensor is a Hub M3-only feature (Aqara Home app 6.1.1+, hub firmware
  4.5.40+) that fuses several devices into one room-level presence state computed on the hub;
  confirmed against a live child device, whose fingerprint imports as a standard Occupancy Sensor
  device type (`0107`) with `UniqueID` prefixed `virtual.` and needs no dedicated driver. Combined
  with a Signal's Cloud Running Method, that explains reports of Signal endpoints being less
  reliable than Soft Sensor endpoints — the difference is in Aqara's architecture, so no parsing or
  classification change follows. `docs/user/bridges/aqara.md` gained a matching table row and
  cross-reference; `docs/TODO.md` item 5.4 closed accordingly.
- **Camera documentation corrected against the 1.1.0 driver.** Four fixes found by re-checking the
  pages against the source. The *Camera AV Stream* component version read `1.0.2` in both
  `drivers/camera-av-stream.md` and this file's 1.9.3 heading, predating the `1.1.0` line. Three
  pages outside the camera page still described the clusters as **Matter 1.3** and knew nothing of
  `0x0552` — `compatibility/device-types.md`, `compatibility/matrix.md` and `drivers/index.md` now
  say Matter 1.5 and name both clusters. A new limitation records that **two-way talk is not
  available** even where the camera supports it: the driver decodes `TwoWayTalkSupport` and **Get
  Info** reports it (`FullDuplex` on the G350), but the audio rides the same WebRTC transport as the
  video, so the attribute describes the camera rather than anything this driver can do. The camera
  page's "See also" gained the compatibility matrix and Aqara bridge links the sibling pages carry.
- **Door Lock documentation re-verified against the 1.5.0 driver and against the forum record
  (HUB-82).** The page was written on 2026-07-27 against the same driver version, so nothing it
  described had drifted behaviourally, but it was wrong in one place, thin against what the driver
  exposes, and — the substantial finding — a release out of date on lock codes.

  **Lock code management is no longer experimental, and the pages now say so.** Through 1.8.7 and
  1.8.8 the implementation was necessarily blind: `matter.invoke()` exposed neither the IM-level
  `InvokeResponse` status (no StatusIB, no command reference) nor any decoding of command-specific
  response payloads — `SetCredentialResponse` `0x23`, `GetUserResponse` `0x1C`,
  `GetCredentialStatusResponse` `0x25` — so the driver had to infer results from whatever event
  followed. Hubitat's Matter transaction callbacks closed both gaps and 1.9.0 consumes them
  (`callbackType: Invoke`, `handleDoorLockInvokeResponse()`). Setting, deleting and listing codes
  from Lock Code Manager is confirmed working on the **Aqara U200 and U400**, RFID and UWB/Aliro
  operations included — maintainer-tested, stated as settled on 2026-08-22. Corrected on five pages: `drivers/door-lock.md`, `help/known-issues.md`,
  `compatibility/device-types.md`, `bridges/aqara.md`, and the 1.9.0 entry in
  `project/revisions-history.md`, which had not mentioned the change at all.

  **The U200 and U400 reach Hubitat as directly-paired Matter devices, not through a bridge** —
  which is why the compatibility matrix, which tracks bridges, is *not* upgraded by this evidence and
  keeps its 2024 bridged-U100 result. `getting-started/use-with-matter-devices.md` is the page that
  owns them and gained all three locks, plus the note that Hubitat's own platform now ships a
  dedicated Matter lock driver covering the U400 and U200 with Lock Code Manager support, so this
  package is the choice only for what that driver does not surface.

  **A lock's accepted-command list cannot be trusted.** The Nuki 4.0 advertises `SetCredential`,
  `SetUser`, `GetUser` and the rest in `AcceptedCommandList` while reporting FeatureMap `0x00`, and
  any command beyond lock/unlock/unbolt either goes unanswered or drops its Matter session. That is
  the concrete justification for the driver's FeatureMap gate and for **Ignore Compatibility Checks**
  being an advanced option, and it is now documented as such rather than left as a bare "not every
  lock supports every command".

  Also corrected against the source: **Enable debug logging** was documented as defaulting to On,
  but 1.9.2 set `_DEFAULT_LOG_ENABLE = false` in this child; and the advice to "check the state
  variables after running Get Info" had the mechanism wrong, since `state.info` is regenerated by
  `updated()` — the page now says Get Info first, then Save Preferences. Added: the four `LockCodes`
  attributes (`lockCodes`, `codeLength`, `maxCodes`, `codeChanged`) with the defaults `installed()`
  seeds; the real value sets behind `doorState`, `lockAlarm`, `lastLockOperation`,
  `lastOperationSource` and `lastLockOperationError` in place of "string"; a **State variables**
  section for `lockAttr` (about thirty five cluster attributes via `LOCK_ATTR_STORE`), `info` and
  `stats`; that **Get Codes** re-sends stored codes rather than querying the lock and **Set Code
  Length** only moves the Hubitat attribute; and a limitation recording that an unlock by an unknown
  credential is auto-registered in `lockCodes` with a **random six-digit placeholder PIN** that is
  not the lock's real code.

- **The Aqara U400 "stops reporting lock state" report is closed — `_DiscoverAll` fixes it.**
  `docs/TODO.md` 5.3 had sat at NEEDS_EVIDENCE since 2026-08-16; the thread had in fact been resolved
  on 2026-08-15 and the item was never updated. Running `_DiscoverAll` on the parent refreshes
  endpoint information and subscriptions while preserving the child device IDs, and the reporter
  confirmed it. The likely cause was a *Load All Defaults* click or a discovery run against an
  offline device — the second of which 1.9.0 already prevents (item 5.2 / HUB-125). Written up as a
  **Confirmed** entry in `help/known-issues.md` and as step 4 of *Child devices have stopped
  updating* in `help/troubleshooting.md`, which previously ended at Re Subscribe. The reporter's
  follow-up question — could the driver detect and repair this itself — is carved out in 5.3 and
  unanswered.
- **`drivers/signal.md` rewritten to the standard driver-doc structure.** It was the only driver page
  written as a community-thread digest: verbatim forum blockquotes, `Source: post #NNN, <handle>,
  <date>` lines after most paragraphs, contributors named in the body text, and a callout asking
  readers to report a device that needs the driver. All of it is now plain technical statement, in
  the same skeleton every other driver page uses (What it is for / Capabilities / Attributes /
  Commands / Preferences / Known limitations). Per-report evidence with post citations stays on
  `bridges/aqara.md`, which is the page that owns it, so nothing is lost. Two corrections came out of
  re-checking the driver source: `currentPosition` is declared but never set by the driver (it was
  described as reported by the device), and `numberOfButtons` is set when the driver is saved rather
  than being a fixed constant. Section anchors changed — `#aqara-signals`, `#cloud-dependency`,
  `#aqara-soft-sensors` — and the inbound links in `bridges/aqara.md` and `docs/TODO.md` were
  updated to match.


## [1.9.2] - 2026-08-17

**Released via HPM 2026-08-17.** Parent driver `1.9.2`; component drivers *Air Purifier* `1.2.5`,
*Window Shade* `1.2.6`, *Switch* `1.1.3`, *Custom Power Energy* `1.1.5`,
*Camera AV Stream* `1.0.2`, *Custom Contact Sensor* `1.0.2`, *Custom Signal* `1.1.3`,
*Battery* `1.1.2`, *Button* `1.1.2`, *Motion Sensor* `1.1.3`.

### Added

- **Air Purifier child: `filterDaysRemaining` attribute.** Matter's Resource Monitoring cluster
  (`0x0071`) has no filter-lifetime attribute, so the long-dormant `filterLifeTime` preference now
  supplies the expected lifetime and the child derives the remaining days from it, clamped at a
  floor of 0. Two sources, in order of preference: `LastChangedTime` (`0x0004`) gives the days
  elapsed since the filter was reset, converted from Matter epoch-s with an offset of `946684800`
  (2000-01-01 UTC); failing that, `Condition` (`0x0000`, surfaced as `filterUsage`) scales the
  lifetime by the percentage of filter life still left. The fallback is not an edge case — an IKEA
  STARKVIND behind a DIRIGERA bridge advertises an `0x0071` AttributeList of
  `[0x0000, 0x0001, 0x0002]` and never reports `LastChangedTime` (confirmed on device 2026-08-16),
  so the elapsed-time path alone would have left the attribute permanently absent on the very
  device the feature was written for. Recomputed whenever either source is reported, on `updated()`,
  on `refresh()`, and by a daily scheduled job, since neither source moves on its own.
  `updateFilterDaysRemaining()` is deliberately parameterless because it is the `schedule()` target;
  `computeFilterDaysRemaining(Long)` carries the logic. Nothing is written to the device, and the
  preference description now says so. HUB-89.

### Changed

- **The bridge's `Status` attribute is renamed to `_status_`.** Hubitat orders the Current States
  list alphabetically, which buried the driver's own info/progress line among the Matter readings;
  the leading and trailing underscores sort it to the top, where it is actually visible while a
  discovery is running. Renamed in the `metadata{}` declaration and in both `sendInfoEvent()`
  emitters, and the old entry is deleted from the device on update. The attribute carries the
  driver's own progress and information messages rather than a device reading, so nothing is expected
  to reference it by name. Note the PowerSource cluster's own `Status` attribute (`0x002F:0x0000`,
  mapped to `powerSourceStatus`) is unrelated and unchanged.

### Fixed

- **Every custom component driver now implements `parse(Map)`, ending the `MissingMethodException`
  error storm on the children.** Since 1.9.0 the parent forwards each `callbackType: Invoke`
  transaction callback to the child that owns the endpoint (`routeInvokeToCustomChild()`), but only
  the Door Lock and Air Purifier children were ever given the matching `parse(Map)`. Every other
  custom child declared `parse(List<Map>)` only, so each forwarded callback threw
  `groovy.lang.MissingMethodException: No signature of method: ...parse() ... (java.util.LinkedHashMap)`
  — logged as an error in the **child's** log, on every command. Reported for two Window Shade
  children in community post #449 (`clusterInt:258` = `0x0102` WindowCovering) against the released
  1.9.1. Cosmetic only: `status:0` in those callbacks means the Matter command itself succeeded,
  which is why the blinds kept working. `parse(Map)` and `handleInvokeResponse()` were added to
  Window Shade, Switch, Custom Power Energy, Camera AV Stream, Custom Contact Sensor, Custom Signal,
  Battery, Button and Motion Sensor, mirroring the Air Purifier: an `Invoke` with `status == 0` logs
  a debug `Matter command completed` line, a non-zero status logs a warning naming the status,
  endpoint, cluster and command, and any other `callbackType` is ignored at debug level.
  `Matter Generic Component SwitchBot Button` is deliberately excluded — deprecated since 1.8.0 and
  no longer supported. Note that routing is by endpoint, not by which child sent the command, so
  this was never limited to the clusters a given child drives: the parent's `identify` utility and
  `setSwitch` can invoke on any endpoint. `docs/BUGS.md` **B27**.
- **The parent's `try/catch (MissingMethodException)` around `dw.parse(descMap)` is documented as
  not being a safety net.** The platform runs the child's script in the child's own context and logs
  the exception there — the reported errors carry the child device ids, not the bridge's — before
  anything reaches the parent's handler, so the catch could never suppress the user-visible error.
  Behaviour is unchanged (it still keeps a hand-installed stale child from breaking the parent, and
  still rethrows a `MissingMethodException` raised from inside a handler that does exist), but the
  comment claiming drivers "opt in one by one" was replaced: every custom component driver shipped
  in this package must implement `parse(Map)`. The same requirement was added to the release
  checklist in `AGENTS.md`.
- **A filter reset is no longer sent to a device that cannot accept it.** `resetFilterCondition()`
  checked the ServerList — which is why the carbon-filter case correctly refused — but not the
  cluster's `AcceptedCommandList` (`0xFFF9`). An IKEA STARKVIND behind a DIRIGERA bridge advertises
  `0x0071` with an **empty** AcceptedCommandList, so the `ResetCondition` invoke went out, the bridge
  swallowed it, and `Condition` never moved: the user got no filter reset and no indication why
  (confirmed on device 2026-08-16). The child now stores the AcceptedCommandList when `getInfo()`
  reads it, and refuses with a warning naming the list. An unknown list is not treated as a refusal,
  so a device whose `0xFFF9` has never been read still behaves as before.
- **`componentSetSpeed()` no longer writes FanMode to an endpoint that has no fan.** An air quality
  sensor is assigned the Air Purifier child for its `005B` cluster — `mapMatterCategory()` checks
  `005B` before `0202` — so **Set Speed** appears on a device with no fan control at all. The write
  went out anyway and an IKEA VINDSTYRKA behind a DIRIGERA bridge answered it with `success:true`
  while never reporting a FanMode, making the `auto` attribute look broken (confirmed on device
  2026-08-16). Now guarded on the child's ServerList. The same method also gained the `deviceNumber`
  validation its `componentOpen()` / `componentClose()` siblings already had, plus a null check on
  the `id` data value, so a missing or malformed `id` warns instead of raising a raw
  `HexUtils.hexStringToInt(null)` NPE — this closes `docs/TODO.md` item **3.4**.
- **Stale `Status` / `status` entries left behind in the bridge device's Current States.** Hubitat
  preserves Current States across a driver type change and never removes an attribute the new driver
  does not declare, so after the rename above the old `Status` would have lingered next to `_status_`.
  A lowercase `status` was already lingering for the same reason, inherited from whichever driver held
  the device before MAB — every event-emitting path in the parent and the libraries was checked, and
  no version of this package back to 0.4.3 has ever written a lowercase `status`. Neither could be
  cleared from the UI: `deleteAllCurrentStates()` (the `loadAllDefaults` panic button) iterates
  `device.properties.supportedAttributes`, which lists **declared** attributes only. New
  `removeObsoleteAttributes()`, driven by the `OBSOLETE_ATTRIBUTES` list, deletes both on the
  version-change path in `checkDriverVersion()` — beside the existing
  `removeObsoleteIlluminanceThrottling()` — and `deleteAllCurrentStates()` now calls it too.
- **Motion Sensor child: toggling *Invert Motion* no longer fabricates an `active` event**
  (`BUGS.md` **B19**). `updated()` inverted `device.currentMotion`, which is not a declared accessor
  on the device object; whatever Hubitat returned for it, the ternary mapped every non-`active`
  value — including a child that has never reported — to `active`, so flipping the preference on a
  fresh sensor announced motion that never happened. It now reads `device.currentValue('motion')`
  and inverts only the exact values `active` and `inactive`; anything else sends no event and logs a
  debug line, leaving the next physical report to establish the state. Static finding, not yet
  reproduced on a hub.
- **Window Shade child: `initialize()` no longer reports the shade as both closed and open**
  (`BUGS.md` **B18**). It seeded `position`, `targetPosition` and `level` to `0` — which the driver's
  own `OPEN=100`/`CLOSED=0` constants define as fully closed — while emitting `windowShade='open'`
  and `switch='on'`, so a newly created or manually initialised child showed contradictory state
  until the first position report arrived. All five attributes now seed to the closed end, and the
  numeric seeds use the `CLOSED` constant rather than a literal. Behaviour after the first real
  report is unchanged.
- **`.hubitat/metadata.json` no longer carries 16 duplicated ids** (`BUGS.md` **C8**). The Hubitat
  VS Code extension keys its records on the absolute file path, so opening the repository folder as
  both `C:\Work\...` and `C:\work\...` made it record every file twice under one id. Fixed by
  normalising the casing and then collapsing by id, keeping the higher `version`; deleting the
  capital-`Work` records instead would have dropped `matterHealthStatusLib` and the SwitchBot Button
  component, which existed only under that spelling. 35 records to 19, ids and paths both unique and
  every path verified present. Local tooling data only — HPM does not read this file and no driver
  behaviour depends on it.

### Changed

- **Air Purifier child: the `auto` attribute is finally populated.** It has been declared since
  1.0.0 and never set by anything. The parent's `parseFanControl()` already maps FanMode 5 to
  `speed = 'auto'`, so the new `syncAutoAttribute()` in the child mirrors that event into `auto`.
  Deliberately done in the child, not in `parseFanControl()`: the stock *Generic Component Fan
  Control* child shares that parse path and declares no `auto` attribute, so emitting from the
  parent would push an undeclared attribute at it. Only `'auto'` counts as on — FanMode 6 maps to
  `'smart'`, which is not a legal `FanControl` value at all (tracked separately, `docs/TODO.md` 3.2).
  HUB-89.

### Removed

- **Air Purifier child: the `Child lock` preference and the `Set Indicator Status` command.**
  Both were carried over from the Zigbee IKEA E2006 (Starkvind) driver, where they are vendor
  attributes. Matter has no equivalent — Fan Control `0x0202` has no lock attribute and there is no
  indicator cluster — so `childLock` was never read by any code and `setIndicatorStatus()` sent a
  local event and stopped at a bare `// TODO!`. The `indicatorStatus` attribute is removed with the
  command. `migrateDriverState()` deletes the stale attribute state and the obsolete setting on
  upgrade, and also installs the new daily filter job on existing installs without requiring a
  **Save Preferences**. Both remaining bare `// TODO!` markers in the child are now resolved. HUB-89.

### Documentation

- `docs/user/drivers/air-purifier.md`: removed the `indicatorStatus` attribute row, the
  **Set Indicator Status** command row and the **Child lock** preference row; added
  `filterDaysRemaining`; rewrote the **Filter life time** row as a Hubitat-side estimate. **Deleted
  the closing claim that "Child lock and filter life time are written to the device, not just stored
  in Hubitat"** — the opposite was true of both. Status line moved to `Applies to: 1.9.2`.
- `docs/user/drivers/air-purifier.md`: `filterDaysRemaining` and **Filter life time** rows describe both
  estimate sources, and note that most purifiers do not report a filter change date.
- `docs/user/configuration/commands-and-states.md` and `docs/user/drivers/matter-advanced-bridge.md`:
  `Status` renamed to `_status_`, with the migration note for rules and dashboards. The bridge driver
  page status line moved to `Applies to: 1.9.2`.
- `AGENTS.md` / this file: corrected the `CHANGELOG.md` scope rule. It records change detail for
  **every** file in the repository — parent driver, component drivers, libraries and user-facing
  documentation alike — not the parent driver alone. In-file `* ver.` header lines are kept as
  short as possible.

## [1.9.1] - 2026-08-13

**Current released version.**

### Added

- A child device's Device Data now shows the bridged device's `serialNumber`
  (cluster `0x0039` attribute `0x000F`) and `uniqueId` (attribute `0x0012`), when the bridge
  reports them. Requested in community post #440.
- **Multi-component bridged devices inherit their identity from the parent endpoint.** On a
  multi-gang wall switch the individual gangs carry no `0x0039` cluster, so they had no name of
  their own and every gang became a generic `Switch` named after the *bridge*. Each gang now takes
  the name, vendor, `serialNumber` and `uniqueId` of the parent Bridged Node endpoint that does
  carry `0x0039`, and sibling components are distinguished by their position in the parent's
  `PartsList` — `Aqara Wall Switch EU - 1`, `Aqara Wall Switch EU - 2`.
- Child Device Data gains `parentEndpoint` when an endpoint's identity was inherited.
- `_TRACE_ALL_MESSAGES` diagnostic flag (`@Field static`, default `false`). Discovery normally
  suppresses the `parse: descMap:` trace for **every** inbound message, which makes a stalled
  `_DiscoverAll` impossible to diagnose — you cannot tell "nothing arrived" from "something arrived
  that was not a matching attribute report". Setting it `true` bypasses that and the FFFx-globals
  suppression.

### Fixed

- `readAttributeSafe` validated every cluster's attribute against the **Descriptor's**
  `AttributeList` and therefore refused valid attributes of any other cluster — reading
  `0x0039:0x0005` (NodeLabel) was rejected as unsupported. The attribute-list key is now chosen the
  same way `requestMatterClusterAttributesValues()` chooses it (`matterStateMachinesLib` 1.2.1).
- **A lost bridge ping was recorded as neither success nor failure.** The command-timeout job was
  cancelled by *any* incoming Matter message, so an unrelated attribute report could clear the timer
  while the ping itself never arrived — no `rtt: timeout` event, no `pingsFail` increment, and the
  driver left believing a ping was still in flight. The timeout is now cancelled only by the reply
  that actually completes the ping. The separate 55-second subscription-handshake timeout is
  unaffected: any inbound message still clears that one, which is what it is for.
- **Multi-path reads that bypassed the chunking safety net.** Chunking was introduced for `refresh()`
  and the cluster-value reads, but three paths never got it. Discovery read the four global attributes
  (`FFFB`/`FFF8`/`FFF9`/`FFFC`) of *every* matched cluster in one unchunked request — 24+ paths on a
  six-cluster endpoint, past the point where some bridges stop answering — and on timeout simply
  carried on with an incomplete fingerprint. `componentRefresh()` was likewise unchunked, with a fixed
  6-second window. And the "this bridge needs small reads" flag was wiped at the start of every
  discovery, so a bridge that had already proved it needs them got full-size reads again on the next
  run; `refresh()` never consulted the flag at all.
  The flag is now remembered for the life of the installation and applied automatically to every
  chunked read, so `refresh()`, `componentRefresh()` and discovery all adapt together. The discovery
  read is chunked, waits proportionally longer for a chunked reply, and on timeout re-requests
  **only the attributes that never arrived**, in small chunks, before giving up.
- **Discovery stalled on every bridged endpoint of one bridge.** A large multi-path read of cluster
  `0x0039` received no response at all — not an error, nothing — while a 9-path read of `0x001D` on
  the same endpoint and a 20-path read on endpoint 0 both succeeded. Confirmed with full tracing, so
  it was not driver-side filtering. Notably the *same* Aqara M3 bridge answers that identical read
  normally from a second Hubitat hub, so this is a lost-response problem on one hub or network path
  rather than a defect in the bridge.
  Each endpoint then burned ~35 s of retries before failing. `discoverGlobalElementsStateMachine()`
  now re-issues the identical read in chunks of `SMALL_READ_CHUNK_SIZE` before giving up, and
  remembers that the bridge needs it, so only the first endpoint pays the timeout.
  Confirmed on the M3: the first endpoint retried once and returned every value, and the next
  endpoint went straight to small chunks with no timeout.

### Developer notes

- Both identifiers were already read during discovery but were discarded by the
  `parseBridgedDeviceBasic()` whitelist, so they never reached `state` or the child's
  `fingerprintData`.
- They are stored only and deliberately **not** sent as events — no child driver declares them, so
  an event would only trip `warnUndeclaredAttributeOnce()` twice per discovery.
- Endpoints without cluster `0x0039` — for example the individual gangs of a multi-gang wall
  switch — get neither key. Both writes are null-guarded, because Hubitat stores a null as the
  literal string `'null'`.
- Existing children are updated on the next `_DiscoverAll`, not only newly created ones.
- The parent search walks up the `PartsList` tree and picks the nearest ancestor that actually
  carries `0x0039` data. An Aggregator endpoint lists the same gangs in *its* `PartsList`, so an
  endpoint can have two parents; the Aggregator is rejected simply because it has no `0x0039`,
  which avoids depending on device types. Nested Aggregators are handled, and a malformed
  `PartsList` cycle terminates safely.
- Label policy: a label the user typed is **never** overwritten. Only a blank label, a generic
  driver-produced one (`Switch`, `Button`, `Unknown`, the product or device-type name), or the
  ambiguous case where sibling components all carry the identical inherited base label are
  replaced. Sibling components legitimately share one serial number — it is one physical device.
- Investigated and closed: Aqara hubs do **not** export the device name assigned in the Aqara Home
  app. `NodeLabel`, `ProductName` and `ProductLabel` all carry the model name, renaming in the app
  changes none of them, and the `UserLabel` / `FixedLabel` clusters are not implemented. Verified
  across three independent controllers.

## [1.9.0] - 2026-07-25

### Added

- New **Spammy attributes minimum reporting interval** preference
  (`spammyAttributesMinInterval`, `0` = off), which sends the attributes marked `isSpammy` in a
  second, additive subscription with a longer minimum interval.
  > **Upgrading from 1.8.x:** this preference defaults to **0 (off)**, and it replaces the removed
  > driver-side illuminance throttling. Until you set it, the previous 10-second illuminance
  > throttling is **not** applied and illuminance reports arrive at the bridge's own rate.
- Root-node battery support: PowerSource cluster `0x002F` on endpoint 0 is now discovered and
  subscribed, driven by `ROOT_NODE_SUBSCRIPTIONS` — relevant to IKEA Thread devices, which expose
  battery on the root node rather than on the application endpoint. On a single-node Matter device
  the report is redirected to that device's application-endpoint child; on a real bridge it stays on
  the parent, which now declares the `Battery` capability and a `batteryVoltage` attribute.
- `installed()` — the driver previously had none, so a fresh installation scheduled no periodic jobs
  until the user pressed Save Preferences.
- `callbackType: Invoke` handling; the bridge is pinged as the first step of the state machines
  before any attributes are read.

### Changed

- **Child device network IDs now follow the Hubitat stock Matter bridge convention**, so that a
  bridge switched from the built-in driver to this one adopts the children that already exist
  instead of creating a duplicate set. New children are created as
  `<parent deviceNetworkId>-<endpoint>` (`stockChildDni()`); children created by MAB 1.8.x and
  earlier use `<parent device id>-<endpoint>` (`legacyMabChildDni()`) and are still resolved.
  `findChildByEndpoint()` tries the stock form first, then the legacy form, so existing
  installations are unaffected — nothing is migrated, renamed or re-created. Every hard-coded
  `"${device.id}-${endpoint}"` lookup was replaced by `findChildByEndpoint()` /
  `childDniForEndpoint()`: `isDeviceDisabled()`, `getDeviceDisplayName()`, `getDw()`,
  `sendHubitatEvent()` and `parseColorControl()`. `normalizeChildEndpoint()` normalises the endpoint
  to upper-case hex first, so a numeric or lower-case endpoint from either source resolves to the
  same child.
- **The driver is now `parse(Map)`-only** — the completion of the conversion started in 1.7.2 and
  enforced in 1.8.0. The `newParse` device data value is what makes the platform dispatch to
  `parse(Map)`, so it is now forced true by `ensureNewParseFlag()` on every `updated()` and by
  `forceNewParseFlag()` if a message ever arrives on the text path; the obsolete `newParse`
  *preference* is removed from existing installations. The residual `parse(String)` entry point no
  longer parses anything — it repairs the lost flag, warns, and drops the message.
  `newParseCompatibilityPatch()` normalises `cluster`, `endpoint` and `attrId` into a consistent
  form, absorbing the differences between Hubitat platform versions (some supply an Integer, some a
  4-character hex String) so that no downstream parser has to care. See also *Removed*, below.
- `SupportedMatterClusters.subscriptions` is now a Map keyed by attribute ID instead of a List of
  single-entry Maps. The old per-attribute `min`/`max`/`delta` values were never sent to the hub
  (`cleanSubscribe` takes one global min/max) and are replaced by an `isSpammy` marker.
- Illuminance readings are now rounded instead of truncated, and the range check uses the Matter
  limit `0xFFFE` instead of an arbitrary 100000 lux, which had been discarding direct sunlight.
- Sending an attribute a child driver does not declare is no longer logged at info level for every
  report — it warns once per child and attribute.
- Air Purifier resource monitoring moved into the child driver.

### Removed

- The driver-side illuminance throttling patch, together with the `minReportingTimeIllum`
  preference and the `stats2`/`lastRx2` state variables. All are deleted from existing installations
  on upgrade. Illuminance is now throttled at the Matter subscription instead.
- The legacy `parse(String)` text path, the `newParse` preference, and the custom TLV decoder — see
  the `parse(Map)` entry under *Changed*, above.

### Fixed

- Nullable measurements no longer report a false `0`. A null `MeasuredValue` was reported as 0 lux,
  0.0 °C, 0 kPa or 0 % because `safeToInt()` defaults to zero. Illuminance, temperature, pressure
  and humidity now go through `nullableMeasuredValue()`, and thermostat `LocalTemperature` is
  guarded separately.
- Endless `colorMode is CT` info logs on the bridge device. Hubitat's stock CT and Dimmer
  components do not declare `colorMode`, so every such event was discarded and the guard that
  checks whether the device is already in CT mode could never be satisfied.
- `_DiscoverAll` no longer silently kills the health check for the rest of the session — its
  `initializeVars(fullInit = true)` ran a bare `unschedule()`. Scheduling was extracted into
  `schedulePeriodicJobs()`.

## [1.8.9] - 2026-05-30 *(BETA)*

### Added

- Aqara G350 video camera support (cluster `0x0551`).

## [1.8.8] - 2026-05-29

**Previous released version** — the one Hubitat Package Manager installed before 1.9.1.

### Changed

- Matter Lock Codes improvements; the default command timeout was doubled.

### Fixed

- Exception handling in `setSwitch()`.

## [1.8.7] - 2026-05-25

### Added

- Matter Lock Codes — first test version.

### Fixed

- FeatureMap bug fix; removed the `ignored invalid illum/lux` warning for zero values.

## [1.8.6] - 2026-05-10

### Added

- Matter Fan control (cluster `0x0202`), tested on an Altitude Boca II ceiling fan. Thanks
  @sbohrer.

## [1.8.5] - 2026-05-08

### Changed

- Merged the development branch to main.

## [1.8.4] - 2026-05-08

### Changed

- `refresh()` reads attributes in chunks of 20 to stay within Matter Read Request PDU size limits
  (Thread MTU is about 1280 bytes). The refresh window is scaled to the number of chunks.

## [1.8.3] - 2026-05-08

### Added

- `componentSetCoolingSetpoint()` (attribute `0x0011` OccupiedCoolingSetpoint), with subscription
  and parsing.

### Fixed

- `componentSetHeatingSetpoint()` did not convert °F to °C before sending, which caused a 95 °F
  clamping bug.
- `ThermostatRunningState` (attribute `0x0029`) is now decoded from its bitmap into the Hubitat
  `thermostatOperatingState` values. Thanks @Murv82.

## [1.8.2] - 2026-04-30

### Added

- Cluster `0x0080` (BooleanStateConfiguration) support: `SensitivityLevel`,
  `SupportedSensitivityLevels`, `DefaultSensitivityLevel`.
- `Matter Custom Component Contact Sensor` child driver with a `sensitivityLevel` attribute, chosen
  by `mapMatterCategory()` when cluster `0x0080` is present — for example the Aqara P100.

### Fixed

- `parsePowerSource()` now uses `safeHexToInt()` for `BatVoltage` and `BatPercentRemaining`.

## [1.8.0] - 2026-02-21

### Added

- PressureMeasurement cluster `0x0403` support, with the `Generic Component Pressure Sensor` driver.
- Button driver improvements.

### Changed

- `newParse: true` is enforced; the old custom parse code was removed.

## [1.7.8] - 2026-03-21

### Added

- Delayed illumination handling. Thanks @lgk. *(Removed again in 1.9.0 — see above.)*

## [1.7.4] - 2026-02-06

### Added

- General Diagnostics (`0x0033`) with `RebootCount` and `UpTime` subscriptions.

### Fixed

- Device `ping()` fix; all events, Door Lock included, are filtered after a reboot or re-subscribe.

## [1.7.1] - 2026-01-26

### Added

- Automatic best-name labelling for all child devices. Thanks @iEnam.

## [1.7.0] - 2026-01-25

### Added

- ALPSTUGA air quality monitor support (CarbonDioxideConcentrationMeasurement).
- `cleanSubscribe` minimum and maximum interval preferences.
- `matterCommonLib.groovy`, shared by the parent and most component drivers.

### Fixed

- `DEVICE_TYPE = 'MATTER_BRIDGE'` bug in `initialize()`.

## [1.6.0] - 2026-01-17

### Changed

- Major refactoring of the Door Lock driver; optimized subscription management.

### Added

- Automatic water leak sensor detection (device type `0x0043`), instead of exposing them as contact
  sensors.

## [1.5.6] - 2026-01-11

### Fixed

- Button events subscription issue — the reason buttons had never worked.
- RGBW child device detection and `DeviceTypeList` parsing.

## [1.5.5] - 2026-01-10

### Added

- **Matter locks now work** — locking and unlocking, not only status. Added `componentPing`.

## [1.5.4] - 2026-01-08

### Added

- `Matter Generic Component Button` driver and the `discoveryTimeoutScale` preference.

## [1.5.0] - 2025-04-04

### Added

- `Matter Custom Component Power Energy` driver. `1.5.1` fixed RMSVoltage and RMSCurrent,
  `1.5.2` added the Signal driver, and `1.5.3` added a workaround for a Hubitat TLV decoding bug.

## [1.4.0] - 2024-12-26

### Changed

- Hubitat platform 2.4.0.x compatibility. `1.4.1` restored the command descriptions.

## [1.3.0] - 2024-10-10

### Added

- `Matter Generic Component Air Purifier` and air quality support (cluster `0x005B`).

### Fixed

- `1.3.1` — null pointer exception in `discoverAllStateMachine()`.

## [1.2.0] - 2024-10-03

### Added

- Matter Thermostat support; adopted the platform's `cleanSubscribe` (requires 2.3.9.186).
- `1.2.1` thermostat fixes and basic Matter event decoding; `1.2.2` added the
  `Matter Generic Component SwitchBot Button` driver by @ymerj *(deprecated — use Button)*.

## [1.1.0] - 2024-07-20

### Added

- `Matter_Generic_Component_Door_Lock`, contributed by @dds82. Added the Identify command and
  reduced battery attribute subscriptions.
- `1.1.1` added the Switch capability to the Door Lock component.

### Fixed

- `1.1.2` skipped General Diagnostics cluster `0x0033` discovery, because Aqara M3 firmware
  4.1.7_0013 returned an error reading attribute `0x0000`. *(Reversed in 1.7.4.)*

## [1.0.0] - 2024-03-16

### Added

- First public release. The 0.x alpha and beta history, from December 2023 to the move to this
  repository in March 2024, is kept in
  [docs/user/project/revisions-history.md](docs/user/project/revisions-history.md).
