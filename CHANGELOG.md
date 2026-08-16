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
> Note the coverage limit: this file tracks the **parent driver**. The component and library drivers
> keep their own `* ver.` history in their own headers and are summarised per release below. For a plain-language, user-facing summary — including the
> 0.x alpha history from December 2023 onward — see
> [docs/user/project/revisions-history.md](docs/user/project/revisions-history.md).
>
> No git tags or GitHub releases exist for this repository yet, so version headings are not linked.

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
