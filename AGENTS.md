# Matter Advanced Bridge — Driver Guide for AI Agents

This repo contains a **Hubitat Elevation (HE) Matter driver package**: a parent bridge driver that
discovers the endpoints behind a Matter Bridge (Zemismart M1, Aqara M3/E1, SwitchBot, IKEA DIRIGERA,
Home Assistant Matter bridge…), creates one Hubitat **child component device per endpoint**, subscribes
to the supported Matter attributes/events, and routes reports to the children.
Author: Krassimir Kossev (kkossev). License: Apache 2.0.

Community thread: https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252
Wiki: https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki

> **This file (`AGENTS.md`) is the canonical agent guide.**
> `CLAUDE.md` and `CODEX.md` are one-line pointers to it — do not put content there, and do not
> re-fork them.
>
> All three are **tracked and published**, so every clone has them — as are the two working lists
> [`docs/BUGS.md`](docs/BUGS.md) (defects) and [`docs/TODO.md`](docs/TODO.md) (community requests),
> moved out of `docs/maintainer/` on 2026-08-13 precisely so other agents can see what was already
> classified, fixed, requested, or ruled out.
>
> `docs/maintainer/**` is still gitignored and local: plans, status records, investigations, migration
> evidence. A reference to one of those that resolves to nothing simply means you are on a machine
> that never had it — that is expected, not a fault to repair, and they must not be reconstructed or
> committed.

**Bug work:** a reviewed list of known bugs with exact locations, fixes, and verification steps is in
[docs/BUGS.md](docs/BUGS.md) — **tracked and published** since 2026-08-13. If you were asked to fix bugs in this
driver, work from that list —
do not re-derive the findings. Mark items `[x]` there only after the user confirms a hub test.
An earlier independent Codex defect analysis (2026-07-04) was **merged into BUGS.md on 2026-08-13**
and its file deleted — BUGS.md is now the single bug list. Every Codex finding was re-verified and is
cross-referenced inline as `[Codex Xn]`, with the ID mapping in that file's closing appendix.
Note that BUGS.md was written against v1.8.8 — verify a finding still applies before acting on it.

**Open user requests:** feature requests and unresolved user reports harvested from the community
thread are in [docs/TODO.md](docs/TODO.md) — **tracked and published** since 2026-08-13 (posts #418–#439 analyzed on
2026-07-25; earlier posts not mined
yet). Same ground rules as BUGS.md — one item at a time, `[x]` only after a confirmed hub test.

**Architecture note (IMPORTANT):** this is **NOT** the kkossev Zigbee V3 / `deviceProfilesV3`
architecture, and it is **NOT** part of the author's Hubitat Zigbee drivers repo. It is a
**standalone git repo**
(`kkossev/Hubitat---Matter-Advanced-Bridge`, branches `main` = release, `development` = dev) with its
**own local libraries** in `Libraries\` (namespace `kkossev`, but different from the Zigbee libraries)
and **component child drivers** in `Components\`. There is no profiles map — behavior is driven by two
`@Field static` maps in the parent (`SupportedMatterClusters`, `ParsedMatterClusters`) plus
`mapMatterCategory()` and the per-cluster `parseXxx()` methods.

---

## 1. Which file is which

| File | Role |
|---|---|
| **`Matter_Advanced_Bridge.groovy`** | The parent driver, **v1.9.0**. Uses `#include kkossev.matterCommonLib / matterLib / matterUtilitiesLib / matterStateMachinesLib` at the end of the file. Its metadata `importUrl` points at the raw **development** branch. `_DEBUG` must be **`false`** for production; verify the source rather than relying on this guide for its current value. |
| `Libraries\matterCommonLib.groovy` | v1.0.1. `safeToInt/safeHexToInt/safeNumberToInt/safeToDouble`, `getFingerprintData()/getServerList()/isClusterSupported()`, `getDeviceNumber()`, logging helpers. **Included by the parent AND by most component drivers.** |
| `Libraries\matterLib.groovy` | v1.4.5. Reference data: `MatterClusters`, per-cluster attribute/command/event name maps, thermostat mode maps, device-type names, `deviceTypeNames()`, `finalizeDeviceType()` (called by the discovery state machine — do not delete; a dead commented duplicate also sits at the bottom of the parent). Parent-only. |
| `Libraries\matterUtilitiesLib.groovy` | v1.3.4. The `utilities '<cmd> <args>'` command dispatcher plus the reply-driven Info collector. Parent-only. **The custom TLV decoder and `testParse` were deleted in 1.9.0** together with the legacy parse path. |
| `Libraries\Matter_State_Machines.groovy` | library name **`matterStateMachinesLib`**, v1.2.0. `discoverAllStateMachine()` (the `_DiscoverAll` engine), `discoverGlobalElementsStateMachine()`, `readSingleAttrStateMachine()`. Parent-only. |
| `Libraries\matterHealthStatusLib.groovy` | v1.0.2. `ping()` + `parseRttEvent()` for the **component drivers** (NOT included by the parent — the parent has its own ping/rtt code). |
| `Components\*.groovy` | Child component drivers (see §5). Most `#include kkossev.matterCommonLib` + `matterHealthStatusLib`. |
| `Components\Matter_Generic_Component_Door_Lock` | **No `.groovy` extension — do not rename.** v1.5.0. The biggest child driver (Lock + LockCodes). |
| `packageManifest.json` | HPM manifest, **v1.8.8 — now LAGS the driver (1.9.0)**. Points at the **bundle** `MatterAdvancedBridge.zip`, not at raw files. |
| `MatterAdvancedBridge.zip` | The distributed HPM **bundle** (parent + libraries + components, parent entry named `kkossev.MatterAdvancedBridge.groovy`). Must be rebuilt at release. Compare zip↔loose files with explicit **UTF-8** decoding (PowerShell default decoding shows fake mojibake diffs). |
| `MatterAdvancedBridge_BETA.zip` | v1.9.0 beta bundle. Its content and release alignment are tracked by BUGS.md D1/D2. Do not publish it without checking those live statuses and verifying `install.txt`/`update.txt`. |
| `Archives\*.zip` | Historical release bundles 0.6.0…1.8.7. Reference only. |
| `Tests\` | `Jailbreak_the_Children.groovy` (stock-child adoption experiment) + a handover .md. Not part of the package. |
| `CHANGELOG.md` | **New 2026-08-13.** Repo-root technical changelog, Keep a Changelog format, derived from the parent driver's header history. Unreleased work goes under `[Unreleased]` - do **not** invent a version heading for it. Not the same thing as `docs\user\project\revisions-history.md`, which is the plain-language user-facing history and also keeps the 0.x alpha record; the two cross-link. No git tags exist, so version headings are deliberately unlinked. |
| `README.md` | Repository front page. Points at the wiki for current user docs and at `docs\user\` for the migration in progress. |
| `docs\user\` | **Tracked and public** — the canonical user documentation, currently being migrated from the wiki. See §7. |
| `.hubitat\metadata.json` | Local Hubitat VS Code extension metadata (hub code ids). Tooling only — not used by HPM. Current file has case-only duplicate paths/ids; see BUGS.md C8. |
| `docs\maintainer\` | All maintainer working documents. **Local only** — untracked and gitignored, so never assume another clone has them. Subfolders: `bugs\`, `plans\`, `status\`, `archive\`. |
| `docs\BUGS.md` | Reviewed defect list — **tracked and published**. Authoritative for open/closed status; open items are indexed at the top. |
| `docs\TODO.md` | Open user requests from the forum thread — **tracked and published**. |
| `docs\maintainer\plans\` | `MIGRATION_PLAN.md`, `INTERMEDIATE_REFACTORING_PLAN.md`, `CHILD_DRIVERS_REFACTORING.md`, `DOCUMENTATION_MIGRATION_PLAN.md`, plus the older `Matter_Advanced_Bridge_OPTIMIZATION_PLAN.md` and `AGGREGATOR_LABELS_PLAN.md` (written against 1.8.8 — aspirational, verify against current code before acting). |
| `docs\maintainer\status\` | `CHILD_DRIVERS_REFACTORING_STATUS.md` — per-child progress record; `wiki-inventory-2026-07-27.md` and `wiki-url-map.csv` — documentation migration evidence. |
| `docs\maintainer\archive\` | Backups. Currently `wiki-baseline-c4000b7-2026-07-27.bundle`, the preserved wiki git history. |
| `CLAUDE.md`, `CODEX.md` | Compatibility pointers to `AGENTS.md`; do not add guide content there. Stay at the repo root. |

### Current v1.9.0 audit warnings
- `BUGS.md` is authoritative for open/closed status; do not duplicate its complete status inventory here.
- Runtime/behavior findings stay unchecked until their listed hub verification succeeds.
- Before any release, check the D-series package gates, loose files, versions, `_DEBUG`, manifest and
  both ZIP manifests/content.

### Editing / release workflow
- Edit the loose source files. There is **no amalgamated build** here — the hub resolves `#include`
  at compile time, so the four parent libraries must be installed on the hub as Libraries Code.
- A **library change** affects only this package (these libraries are not shared with the Zigbee drivers),
  but it affects **every component driver that includes it** (matterCommonLib / matterHealthStatusLib).
- **`CHANGELOG.md` is authoritative for change detail; the in-file header history is a summary.**
  Policy set 2026-08-13. A new `* ver. x.y.z` header line is **one or two lines, user-facing only** —
  what a user would notice or must act on (behaviour changes on upgrade, new/removed preferences, new
  device data). Internal method names, cluster/attribute IDs, rationale and investigation notes belong
  in `CHANGELOG.md`, never in the header. Before shortening any header entry, confirm the detail
  actually exists in `CHANGELOG.md` — as of 1.9.1 it is complete for the **parent driver only**;
  component and library histories have never been transferred, so do not delete those.
- Release checklist: bump `version()` + `timeStamp()` in the parent (and the `@Field static ...Version/Stamp`
  in a changed library/component), add a `* ver. x.y.z  date  kkossev - ...` header history line (short — see above),
  set `_DEBUG = false`, update `packageManifest.json` (`version`, `dateReleased`, prepend `releaseNotes`),
  **rebuild `MatterAdvancedBridge.zip`**, update the wiki revisions-history page.
- Per the project's standing bug-fix workflow: during bug fixing do **NOT** bump
  versions or add history lines after individual fixes — the user says when to bump.
- **Update driver datetime stamps on every code change.** Whenever production code changes in the
  parent driver or a component driver, update that driver's existing datetime stamp in the same change
  (`timeStamp()` in the parent; the `@Field static ...Stamp` value in a component) using the current
  Europe/Sofia local date and time. This is required during development and does not authorize a version
  bump, history entry, manifest edit, or release-artifact rebuild.
- Do not add fingerprints to the parent `metadata{}` — deliberately omitted so the stock driver is
  chosen at pairing (see the comment in `metadata{}`).
- **Run a Groovy syntax check locally** wherever `groovy`/`groovyc` are available (they are on the
  maintainer's machine). Strip the `#include` lines (not valid Groovy) into a temp copy, then parse it.
  This catches structural errors before the user pastes into the hub. It does **not** substitute for a
  live device test.

---

## 2. Runtime architecture

### 2.1 Parse flow — `parse(Map)` ONLY (since 1.9.0)

The driver is built exclusively around HE's **`parse(Map)`** callback. The legacy `parse(String)` text
path, the `newParse` preference, `myParseDescriptionAsMap()` and the custom TLV decoder were **all
deleted in 1.9.0**. Do not reintroduce a dual path.

- The **device data value `newParse`** (not a preference — the preference no longer exists) is what makes
  the Hubitat platform dispatch to `parse(Map)`. `ensureNewParseFlag()` writes it unconditionally to
  `'true'` from `updated()` and `initializeVars()`, and also removes the obsolete stored preference.
- `parse(String)` survives only as a **4-line self-healing stub**: it logs a warning and calls
  `forceNewParseFlag()`. If you ever see `parse(String) is no longer supported ...` in a user's log, the
  device data value was lost — that is the diagnostic, not a reason to restore the old parser.

1. `parse(Map msg)` → `newParseCompatibilityPatch(msg)` — the **critical normalization step**:
   - ensures `cluster`/`endpoint`/`attrId` exist as hex strings (endpoint 2 chars, cluster/attr 4 chars,
     taken from `clusterInt`/`endpointInt`/`attrInt`);
   - **converts List values to lists of 4-char uppercase hex strings** (`integerToHexString(v,2)`).
     Note the cast fails for *nested* lists (e.g. DeviceTypeList structs) and the `catch` keeps the raw
     value — that is why DeviceTypeList arrives at the parsers un-converted;
   - **converts scalar `FFFC` (FeatureMap) and `FFFD` (ClusterRevision) Numbers to uppercase hex strings**.
     → Anything that later reads `NNNN_FFFC`/`NNNN_FFFB` from fingerprint data must use
     `safeHexToInt()`/`safeNumberToInt()`, **never** `safeToInt()` (source of real bugs — see BUGS.md B1);
   - `Invoke` and `Event` callbacks return early, unmodified.
   - Its `// TODO: this patch has to be removed!` comment on the List→hex conversion is **load-bearing** —
     Window Shade, Door Lock, Air Purifier, Button and Camera all expect hex strings. Do not "clean it up".
2. `prepareForParse()` → `checkDriverVersion()`, `checkSubscriptionStatus()`, unschedule command
   timeout, `setHealthStatusOnline()`.
3. `processParsedDescription(descMap)` — **single parameter** since 1.9.0:
   - stats, state-machine confirmation (`checkStateMachineConfirmation` compares against
     `state.stateMachines.toBeConfirmed` and sets `Confirmation=true`) — this runs **early**, before the
     Info collector, so it fires regardless of `state.states.isInfo`;
   - drop messages for disabled child devices;
   - handle `callbackType: SubscriptionResult / WriteAttributes` (log & return), `Invoke` (routed to
     custom children, bypasses attribute-only processing);
   - `checkChildDevicePingResponse()` (child ping RTT);
   - `parseGlobalElements()` — stores `00FE/FFF8/FFF9/FFFA/FFFB/FFFC/FFFD` into
     `state[fingerprintName][<cluster>_<attrId>]`, mirrors into the child's `fingerprintData`,
     and calls `markClusterDataReceived()` for the discovery wait logic;
   - `gatherAttributesValuesInfo()` — the "Info" mode collector (`state.states.isInfo`) and the ping
     RTT calculation for the bridge itself. ⚠️ the bridge-ping branch is an `else if` **after** the
     `isInfo` branch, so a `ping()` issued while `isInfo == true` never completes;
   - dispatch by `ParsedMatterClusters[clusterInt]` → `this."${parserFunc}"(descMap)` inside
     try/catch (exceptions become `logWarn "parserFunc: exception..."` — messages are silently dropped).

### 2.2 Cluster registration pattern (checklist for adding a cluster)

Touch ALL of these, or reports will parse in the parent and go nowhere:
1. `SupportedMatterClusters` — attributes-map name, parser name, `subscriptions:` as a **Map keyed by
   attribute ID** (`[attrId: [isSpammy: true|false]]`), and optional `eventSubscriptions:`
   (`[-1]` = all events). Do not restore the obsolete per-attribute `min`/`max`/`delta` list format.
2. `ParsedMatterClusters` — clusterInt → parser method name.
3. The parser `parseXxx(final Map descMap)` — decodes or forwards to the child.
4. `mapMatterCategory(Map d)` — endpoint → child driver. **Order matters**: first matching branch wins
   for combined endpoints. The actual branch order is:
   0300 (with a nested 010D **device-type** check) → 0008 → 0045 (with nested 0043/0015
   **device-type** checks and an 0080 cluster check for custom contacts) → 005B → 0101 → 0102 →
   0201 → 0202 → 0400 → 0402 → 0403 → 0405 → 0406 → 040D → 042A → 0090/0091 → 0006 →
   003B → 002F → 0551 → fallback `Generic Component Switch`. Do not describe 010D, 0043 or 0015
   as independent ServerList branches.
5. The child driver: capability, `parse(List<Map>)`, handling of `unprocessed`/`handleInChildDriver`.
6. matterLib name maps (`MatterClusters`, `XxxClusterAttributes`, `getAttributesMapByClusterId()`).

### 2.3 Event routing — `sendHubitatEvent(eventMap, descMap|dw, ignoreDuplicates)`

The single choke point for all events (parent attribute `state`, bridge events, child events):
- Child DNI = `"${device.id}-${endpointHex}"` (endpoint 2-char uppercase hex). See also
  `childDniForEndpoint()` / `stockChildDni()` / `legacyMabChildDni()` for the stock-child adoption paths.
- **Noisy post-subscribe filter**: Matter *events* (evtId present) arriving <30 s after (re)subscribe
  (or <10 s later) are dropped (`shouldFilterNoisyPostSubscribeEvent`) — both here and in `parseSwitch`.
- **Duplicate filtering** (`ignoreDuplicates=true`): ordinary reports compare against the destination
  child's/parent's `currentState` by data type. Matter events are never suppressed as duplicates because
  buttons may legitimately repeat a payload. Refresh/discovery bypass the ordinary state comparison but
  use the per-burst `isBurstDuplicate()` guard to suppress repeated derived attributes within that burst.
- Routing: Matter events and the two **internal event names `unprocessed` / `handleInChildDriver`**
  always go through `child.parse([eventMap])`; decoded attributes go through `child.parse()` if the
  child declares the attribute. The legacy direct `child.sendEvent()` fallback for an undeclared
  attribute is retained, but Hubitat discards that event; `warnUndeclaredAttributeOnce()` reports the
  mismatch. Endpoint 00/absent → parent `sendEvent`.
- Current parent parsers forward child-owned reports as `handleInChildDriver` with the **original Map**.
  `unprocessed` remains an internal/legacy name accepted by some children, but new code must not send a
  stringified `descMap`. Air Purifier now consumes the native Map and must not reintroduce
  `patchParseDescriptionMap()` or the old string-reparse path.
- **Preserve wrapper context through child-owned parsing.** An internal event wrapper may carry
  `isRefresh` / `isDiscovery`; pass these flags through every child processing layer. A final child event
  produced in either context must bypass ordinary duplicate/delta suppression, set `isStateChange=true`,
  preserve `isRefresh=true` / `isDiscovery=true`, and append `[refresh]` / `[discovery]` to its description.
  Log the final reading through the established logging helper, not a direct `log.*` call.

### 2.4 Value decoding conventions

- `safeToInt` = decimal String / Number; `safeHexToInt` = hex String and passes numeric values
  (`Integer`, `Long`, `BigInteger`, etc.) through via `intValue()`; `safeNumberToInt` handles an `0x`
  prefix.
  On the Map path values are usually already Integers, so `safeHexToInt` is correct-but-defensive —
  **do not sweep-replace it with `safeToInt`**, that silently breaks any residual hex string.
- **`normalizeDeviceTypeList(List)`** is the single normalizer for Descriptor `DeviceTypeList`.
  The platform delivers at least three shapes: `[[0:19, 1:1]]` (map keyed by tag number — Zemismart M1),
  `[[[tag:0, value:22], …]]` (list of tag/value maps), and plain ids. It returns 4-char uppercase hex ids.
  Used by `parseDescriptorCluster`, `gatherAttributesValuesInfo` and `fingerprintToData` — if you find a
  fourth shape, extend the helper, do not add a local branch.
- Level/hue/sat scaling: `int256ToInt100()` / `*2.54`. Color temp: mireds ↔ Kelvin.
- Temperature: Matter 0.01 °C units; converted per `location.temperatureScale`; **thermostat setpoint
  writes convert °F→°C** (fixed in 1.8.3 — keep it that way).
- Multi-byte hex command parameters are little-endian: `zigbee.swapOctets()` / `byteReverseParameters()`.
- Illuminance: `10^((raw-1)/10000)` lux, sent straight through `sendHubitatEvent()`. The **lgk
  delayed-illumination patch was removed in 1.9.0** (`illumEvent()`, `sendDelayedEventIllum()`,
  `resetStats2()`, `stringToJsonMap()`/`mapToJsonString()`, the `minReportingTimeIllum` preference and
  the JSON-string states `state.stats2`/`state.lastRx2`) — throttling now happens at the source via
  `isSpammy` on 0x0400, see §2.5. `removeObsoleteIlluminanceThrottling()` deletes the leftovers from
  existing installs. **Do not reintroduce driver-side event delaying for a spammy cluster** — mark the
  attribute `isSpammy` instead.
  `MeasuredValue` is **nullable**: `null` = measurement invalid → no event; `0` = too dark to measure
  → 0 lux; `1..0xFFFE` → `round(10^((v-1)/10000))`.
- **Nullable MeasuredValue attributes must go through `nullableMeasuredValue()`** (0x0400 illuminance,
  0x0402 temperature, 0x0403 pressure, 0x0405 humidity). `safeToInt()` defaults `null` to **0**, which
  is indistinguishable from a genuine reading and used to fabricate `0 lux` / `0.0 °C` / `0 kPa` / `0 %`
  events whenever a sensor reported "measurement invalid" (fixed in 1.9.0 — do not reintroduce a bare
  `safeToInt(descMap.value)` here). Thermostat **`LocalTemperature` (0x0201/0x0000) is nullable too**
  and is guarded separately in `parseThermostat` — `convertTemperature()` itself is left untouched
  because the setpoints that also call it are *not* nullable. The concentration clusters 0x040D and
  0x042A are handled separately: the parent forwards the raw Map, and the Air Purifier child accepts
  native nullable numeric values directly. Do not restore the old string/IEEE754-bit-pattern decoder.
- Battery: `BatPercentRemaining` is half-percent (÷2); contains a **Zemismart M1 patch**: raw value 1
  with device model `Zemismart M1 Hub` is rewritten to 200 (=100%) — deliberate, do not remove.
- **Root node battery (endpoint 0).** IKEA Thread devices expose PowerSource (0x002F) on the *root node*,
  not on the application endpoint. Root-node subscriptions come from the `ROOT_NODE_SUBSCRIPTIONS` map
  (0x0033 RebootCount/UpTime, 0x002F BatVoltage/BatPercentRemaining) and are only requested for attributes
  the root node's own AttributeList actually contains — add new root-node clusters there, not as another
  hardcoded block in `fingerprintsToSubscriptionsList()`. `redirectRootNodePowerSource()` then decides the
  destination: on a `MATTER_DEVICE` with exactly one application-endpoint child the report is rewritten to
  that child's endpoint; on a `MATTER_BRIDGE` (or when the child count is anything other than 1) it stays on
  the parent, which declares `capability 'Battery'` and a `batteryVoltage` attribute for that case.
  Untested against real IKEA hardware — written from the spec.

### 2.5 Discovery model (`_DiscoverAll` → Matter_State_Machines)

State lives in:
- `state.bridgeDescriptor` — endpoint 00 (DeviceTypeList, ServerList, PartsList, `0028_FFFB`, `0033_FFFB`…);
- `state.fingerprintXX` — per endpoint: `ServerList`, `DeviceTypeList` (normalized to device-type-ids
  only, 4-char hex), `VendorName/ProductName/NodeLabel…` (from cluster 0x0039),
  `NNNN_FFFB/FFF8/FFF9/FFFC` per supported cluster, and `Subscribe` = list of matched cluster ints;
- `state.subscriptions` — list of `[endpoint, cluster, attrId]` integer triples;
- `state.stateMachines` — SM bookkeeping (`toBeConfirmed`, `Confirmation`, retries, `clusterDataExpected`,
  `discoverAllPingAttempt`).

**The machine has no `setState`/`nextState` helper**: each `case` assigns a local `st`, and the tail
re-arms `runInMillis(period, discoverAllStateMachine, [overwrite:true, data:data])` while `st != 0`.
`st = 0` stops it. To abort from any state: set `state.stateMachines.errorText` and
`st = DISCOVER_ALL_STATE_ERROR` (98) — that path emits the bold `Status` event and stops cleanly.
Note Groovy switch scoping: variables declared in one `case` share the whole switch scope, so a new
`case` must not reuse an existing local name.

Flow of `_DiscoverAll('All')`: `updated()` → **bridge PING gate (states 110/111)** → `INIT` (1) →
**`initializeVars(fullInit=true)` (wipes ALL state, resets preferences to defaults, turns debug logging
off!)** → bridge Descriptor global elements → bridge BasicInformation (0x0028) → bridge
GeneralDiagnostics AttributeList (0x0033, only FFFB — some devices error on attr 0x0000) → **root node
PowerSource AttributeList (0x002F/FFFB, states 16/17, only when 0x002F is in the root `ServerList`; a
timeout here warns and continues — it must never abort the discovery)** → per PartsList
endpoint: Descriptor global elements + BridgedDeviceBasic (0x0039) → per endpoint: match `ServerList`
against `SupportedMatterClusters`, set `state[fp]['Subscribe']`,
`createChildDevices(fingerprintToData(fp))`, read FFFB/FFF8/FFF9/FFFC for each matched cluster (wait via
`clusterDataExpected`), `copyEntireFingerprintToChild()` → `fingerprintsToSubscriptionsList()` →
`reSubscribe()` (cleanSubscribe).
Retries scale with the `discoveryTimeoutScale` preference (enum '1'/'2'/'3').
Disabled child devices are skipped throughout.

**Bridge ping gate (added 1.9.0, `DISCOVER_ALL_STATE_PING_BRIDGE` = 110 / `_WAIT` = 111):** reads
endpoint 0 / cluster 0x0028 / attr 0x0000 and waits for `Confirmation`. Up to
`DISCOVER_ALL_PING_MAX_ATTEMPTS` (3) attempts of `DISCOVER_ALL_PING_MAX_TICKS` (15 ticks ≈ 5 s, does
*not* scale with `discoveryTimeoutScale`); on failure it aborts via the ERROR state. **It runs before
`INIT` on purpose** — an unreachable bridge must not reach `initializeVars(fullInit=true)` and destroy
the fingerprints, subscriptions and preferences. Every `_DiscoverAll` variant enters here; the real
target state is carried in the SM `data` map as `afterPingState`. Do not use the driver's `ping()` here
(see the `isInfo` hazard in §2.1).

Subscription build (`buildSubscriptionPathGroups()` → `sendSubscriptionCommands()`): attribute paths
from `state.subscriptions` (skipping disabled children) + event paths for
clusters with `eventSubscriptions` (0x003B Switch and 0x0101 DoorLock use wildcard `-1`), then
`minimizeByWildcard()` collapses ≥2 endpoints with the same cluster+attr into a single
`ep: -1` wildcard path and **filters out all Descriptor (0x001D) paths** (subscribing to the
Descriptor cluster is known to cause problems — deliberate).
Requires HE platform ≥ 2.3.9.186 (`matter.cleanSubscribe`); min/max intervals from preferences
(`getPrimarySubscriptionMinInterval()` enforces a floor of 1 second). `cleanSubscribeCmd()` is gone;
`subscribeCmd()` remains only as a deprecated stub that warns and returns `null`.
`sendSubscriptionCommands()` is the active entry point used by
`initialize()`, `reSubscribe()` and `sendSubscribeList()`, and it owns `state.lastTx['subscribeTime']`
and `state.states['isSubscribe']`.

**`SupportedMatterClusters.subscriptions` format (changed in 1.9.0):** a Map keyed by attribute ID,
value = an options Map, e.g. `subscriptions : [0x0000: [isSpammy: true]]`. It used to be a List of
single-entry Maps carrying `min`/`max`/`delta` per attribute — **those values were never sent to the
hub** (`cleanSubscribe` takes one global min/max), so they were dropped. The only option honoured
today is `isSpammy`.

**Two-stage subscription (`spammyAttributesMinInterval` preference, 0 = off):** when the preference is
> 0, the paths marked `isSpammy` (0x005B, 0x0400, 0x0402, 0x0403, 0x0405, 0x040D, 0x042A, 0x0090
power/voltage/current/frequency/PF, 0x0033 UpTime) are split out of the primary `cleanSubscribe` and
sent afterwards as a **second, additive `matter.subscribe()`** with a longer minimum interval.
The second command is **not** on a fixed timer: `sendSubscriptionCommands()` sets
`state.states['pendingSpammySubscribe']` and the `SubscriptionResult` callback in `parse()` triggers
`sendSpammySubscription()`; `SPAMMY_SUBSCRIBE_FALLBACK_DELAY` (60 s) is only a fallback for
controllers that never report one. Sending it while the `cleanSubscribe` handshake is still in flight
is a race — do not replace this with a short `runIn()`.
⚠️ **Unverified on hardware:** whether the HE Matter client keeps the primary subscription alive
alongside a second `matter.subscribe()`. If the primary dies when the preference is > 0, this is why.

### 2.6 Child devices

- DNI `"$device.id-$endpointHex"`; child device data: `id` (endpoint hex), `fingerprintName`,
  `product_name`, and **`fingerprintData`** — a JSON copy of the endpoint fingerprint (minus
  Descriptor-only keys, see `DESCRIPTOR_ONLY_KEYS`) that children read via `getFingerprintData()` /
  `getServerList()` / `isClusterSupported()` (matterCommonLib). It survives `minimizeStateVariables`.
- Stock Hubitat drivers are used where possible (RGBW/CT/Dimmer/Contact/Water/Thermostat/Fan/
  Omni Sensor/Pressure); kkossev custom components for the rest (see the table in `mapMatterCategory`).
- Child → parent commands: `componentOn/Off` (checks child's ServerList: OnOff vs Thermostat),
  `componentSetLevel/StartLevelChange/StopLevelChange`, `componentSetColorTemperature/SetHue/
  SetSaturation/SetColor/SetEffect`, `componentOpen/Close/SetPosition/StartPositionChange/
  StopPositionChange`, `componentSetThermostatMode/SetThermostatFanMode/SetHeatingSetpoint/
  SetCoolingSetpoint` (heating clamp 5–35 °C, cooling 16–32 °C — hardcoded), `componentSetSpeed`,
  `componentSetSensitivityLevel`, `componentRefresh` (re-reads the endpoint's subscribed attributes),
  `componentPing` (reads Descriptor 0x001D:0x0000, RTT tracked in `state.pendingPings`),
  `componentIdentify`, `componentLog`.
- **The Door Lock child builds its own Matter TLV commands** and sends them via `parent?.sendToDevice(cmd)`
  (lock/unlock/unlockWithTimeout/unboltDoor/SetCredential 0x22/ClearCredential 0x26). Lock Codes are
  the author's active work-in-progress — see §4.

### 2.7 Parent commands & preferences

Commands: `_DiscoverAll` (All/BasicInfo/PartsList/ChildDevices/Subscribe), `reSubscribe`,
`loadAllDefaults` (panic: wipes settings+states+jobs), `identify`, `refresh` (reads all subscribed
attributes **in chunks of `effectiveReadChunkSize()`** - 20 normally, 8 once the bridge has proved it ignores a full-size batch; refresh window scaled by chunk count),
`ping`, plus `utilities '<cmd> ...'` and `getInfo`/`test` when `_DEBUG=true`.
`getInfo(infoType, endpoint)` → `collectBasicInfo()` / `requestExtendedInfo()` (matterUtilitiesLib). It must
**not** touch `state.states['isInfo']`/`['cluster']`/`state.tmp` — `requestMatterClusterAttributesList()`
arms those and `logRequestedClusterAttrResult()` clears them; arming early makes unrelated reports pile
into `state.tmp`. (The command was declared but had **no implementation at all** until 1.9.0.)
Both funnel into **`infoCollectStateMachine()`** (matterUtilitiesLib), a reply-driven collector: it walks a
queue of cluster hex strings plus the tokens `SERVERLIST` and `BASICINFO` (the latter resolved to 0028/0039
only *after* the Descriptor read, since the ServerList is unknown before that), doing read-AttributeList →
read-values → log per cluster and advancing on `Confirmation` rather than on a timer.
`INFO_COLLECT_MAX_TICKS` × `discoveryTimeoutScale` is the per-step **timeout**, not a delay. This replaced a
fixed `runIn` schedule in 1.9.0: a Basic run went from ~91 s to ~5 s, Extended on a 13-cluster bridge from
~6.5 min to ~21 s. `requestMatterClusterAttributesValues()` returns the last attrId it sent so the machine
can arm confirmation on it. `SHORT_TIMEOUT`/`LONG_TIMEOUT` in the parent are now unused leftovers.
Note: on HE 2.4.0.x the **command parameters are ignored** — the platform calls the no-arg overload, so
`_DiscoverAll` always runs `'All'` and `getInfo` always runs `'Basic'` on endpoint 0. Both keep a no-arg
`xxx()` → `xxxPatched(defaults)` shim plus the real parameterised overloads for platforms that do pass them.
Note: the **Initialize capability is deliberately commented out** (re-subscribing on every reboot is
unwanted). It has nothing to do with installation — `initialize()` is a *reboot* hook.

**Periodic jobs.** `schedulePeriodicJobs()` is the single place that arms `deviceHealthCheck` from the
preferences. **Every path that calls `initializeVars(fullInit = true)` must call it afterwards**, because
that does a bare `unschedule()` which cancels everything. `initialize()` is covered via its trailing
`updated()`; `installed()` and both terminal states of `discoverAllStateMachine` (END *and* ERROR) call it
explicitly. Before 1.9.0 the driver had **no `installed()` at all** — a fresh install scheduled nothing
until the user pressed Save Preferences, and every `_DiscoverAll` silently killed the health check for the
rest of the session. `installed()` defers `updated()` by 3 s on purpose: `device.updateSetting()` writes
made by `initializeVars()` are not visible to `settings` within the same execution.

Preferences: `txtEnable`, `logEnable`, and under Advanced Options:
`healthCheckMethod/Interval` (periodic ping, offline after 3 misses), `discoveryTimeoutScale`,
`traceEnable` (30 min), `minimizeStateVariables` (deletes `fingerprintXX`, `tmp`, `stateMachines`
from state — child `fingerprintData` is the survivor), `cleanSubscribeMin/MaxInterval`,
`spammyAttributesMinInterval` (0 = off; see the two-stage subscription in §2.5 — changing it triggers
a `reSubscribe()`, unless a subscription is already in flight).
(The `newParse` preference was **removed in 1.9.0** and is actively deleted from existing installs.)

---

## 3. Conventions

- **Reuse existing abstractions first.** Before adding direct logging, conversion, parsing, formatting,
  state, or event-handling logic, search the parent driver and included libraries for an existing helper.
  Use the established helper unless there is a documented reason it cannot satisfy the requirement. In
  particular, do not call `log.info`/`log.debug`/`log.warn`/`log.error`/`log.trace` directly when the
  corresponding `logInfo`/`logDebug`/`logWarn`/`logError`/`logTrace` helper is available. When reviewing
  changed code, explicitly check for newly introduced direct `log.*` calls.
- **Verify the code actually deployed on the hub before diagnosing a new local change.** Do not infer
  deployment from event time alone. Check the driver's stored `state.driverVersion`/datetime stamp or
  inspect live parent-wrapper and child logs. A wrapper already containing the expected context flag
  while the child exhibits the old behavior is evidence that the hub is still running older child code.

- Logging helpers (matterCommonLib): `logInfo`/`logError` gated by `txtEnable`, `logDebug` **and
  `logWarn`** gated by `logEnable`, `logTrace` by `traceEnable`. A "missing" warning in the user's
  log usually just means debug logging is off.
- HE Groovy quirks relied on throughout: bare undefined *identifiers* in GStrings resolve to `null`
  (no exception); assignments like `updateStateSubscriptionsList(addOrRemove = 'add', endpoint = 0, ...)`
  are positional args + binding-variable writes (a common kkossev idiom — do not "fix" en masse);
  calling an undefined *method* throws `MissingMethodException`; method on null throws NPE.
- **The HE sandbox blocks reflection — `getClass()` and `.class` are NOT allowed** and fail at runtime,
  not at paste time, so the local Groovy syntax check will not catch them. To identify a value's type
  (e.g. when probing an unknown `descMap.value` shape) use an `instanceof` chain instead. The same
  applies to `e.class.simpleName` in catch blocks — use `e.message`.
- Prefer plain `String` over GString when writing into `state` (`"...".toString()`).
- Endpoint strings 2-char uppercase hex; cluster/attr strings 4-char uppercase hex; `NNNN_FFFB`-style
  keys in fingerprints. `getFingerprintName()`: endpoint '00' → `bridgeDescriptor`, else `fingerprintXX`.
- `@Field static final Boolean _DEBUG` in the parent (and `_DEBUG_LOCK`, `_DEBUG_AIR_PURIFIER`
  in children) gate test commands — they must be `false` in production. Always verify the source.
- **`@Field static final Boolean _TRACE_ALL_MESSAGES`** (parent, default `false`) is the diagnostic
  escape hatch for a failing `_DiscoverAll`. `processParsedDescription()` normally suppresses the
  `parse: descMap:` trace for the FFFx globals *and* for **every** message while
  `state.states.isDiscovery == true`, which leaves a stalled discovery undiagnosable — you cannot tell
  "nothing arrived" from "something arrived that was not a matching attribute report". Setting it
  `true` bypasses both. Very noisy on a large bridge; must be `false` in production. See BUGS.md C26.
- No unit tests anywhere; verification = local Groovy syntax check (see §1) + paste into the hub's
  Drivers Code editor + live device tests.
- Groovy method-size limit (64 KB compiled) applies on HE — keep new switch blocks small.

## 4. Intentionally-disabled / experimental code (do NOT "helpfully" re-enable)

- **RGBW upgrade block** in `parseColorControl` (case FFFB, ~1896–1920): auto-replacing a CT child
  with an RGBW child is commented out; only a manual-change warning is logged. Deleting/recreating the
  child changes its deviceId → breaks dashboards/rules. Leave commented.
- **`parseDoorLock` LockState decoding** commented out: ALL Door Lock reports/events are
  deliberately forwarded to the child (`handleInChildDriver`) since 1.7.x.
- **Descriptor cluster (0x001D) subscription** — deliberately never subscribed (commented entries in
  `SupportedMatterClusters` and `fingerprintsToSubscriptionsList`); `minimizeByWildcard` filters it defensively.
- **BridgedDeviceBasic 0x0039 subscription** — commented out in `SupportedMatterClusters` (read only
  during discovery).
- **Lock Codes** (Door Lock child v1.5.x) — active experiment ("blind implementation", Aqara U200 /
  Nuki tested). Includes deliberate oddities: `setCode` with `overwriteCodes` does Clear-then-Add with
  a fallback timer; unknown keypad credentials are **auto-registered with a random 6-digit placeholder
  PIN** so Lock Code Manager can track them; `state.lastCmdIsTimeout` maps Unlock→'unlocked with timeout'.
  Ask the user before touching any of it.
- **`_DiscoverAll` wipes preferences and turns off debug logging** (`initializeVars(fullInit=true)`;
  `oldLogEnable` is restored only when `_DEBUG`) — looks like a bug, is at least partly deliberate. Ask.
  (Since 1.9.0 the ping gate at least prevents this from happening against an unreachable bridge.)
- **SwitchBot Button child** — written for the pre-1.8.0 parent (expects `currentPosition` events the
  parent no longer sends). See BUGS.md B8 before doing anything.
- **`isMatterBridgeByAnyEndpoint()` / `finalizeDeviceType()` at the bottom of the parent** are dead,
  commented duplicates — the live copies are in `matterLib.groovy`.
- **The List→hex conversion in `newParseCompatibilityPatch()`** carries a `TODO: this patch has to be
  removed!` — it is load-bearing for the children (§2.1). Removing it is a separate, risky project.

## 5. Component drivers quick reference

| Driver | Ver | Capabilities / notes |
|---|---|---|
| Matter Generic Component **Door Lock** | 1.5.0 | Lock + LockCodes (WIP). Builds own TLV. Supports both `parse(Map)` (2.5.1.132+ Invoke/SubscriptionResult callbacks) and `parse(List<Map>)`. Duplicate-event filter by `eventSerial`; `state.lockAttr` mirrors raw attribute values (`LOCK_ATTR_STORE`). |
| Matter Generic Component **Air Purifier** | 1.2.4 | AirQuality 0x005B, CO₂ 0x040D, PM2.5 0x042A and resource monitoring 0x0071/0x0072. Receives native `handleInChildDriver` Maps and supports both `parse(Map)` and `parse(List<Map>)`; do not restore stringified-map parsing. Includes delta-threshold event suppression prefs. Stores version+stamp in `state.driverVersion`, checks it from both parse paths and maintenance entry points, and logs a detected update once without adding a `Status` event. This is component-specific for now; do not retrofit every child unless requested. |
| Matter Custom Component **Power Energy** | 1.1.4 | 0x0090 power (V/A/W/Hz/PF from `handleInChildDriver`, all values ÷1000, PF ÷10000) + 0x0091 cumulative energy (mWh structs → kWh). |
| Matter Generic Component **Window Shade** | 1.2.5 | Hubitat standard OPEN=100/CLOSED=0; `invertPosition` default **true** (Zemismart), `targetAsCurrentPosition`, `substituteOpenClose` prefs; simulated `switch`/`level`; travel-timeout timer. See BUGS.md B18/C10 for live status. |
| Matter Generic Component **Camera AV Stream** | 1.0.1 | Cluster 0x0551 (Matter 1.3+), Aqara G350. Has a dual-shape array normalizer for hex-string vs List values. See BUGS.md B16/D1 for live status. |
| Matter Generic Component **Button** | 1.1.1 | Pushable/Holdable/Releasable/DoubleTapable. Decodes Switch cluster 0x003B *events* (InitialPress/LongPress/ShortRelease/LongRelease/MultiPressOngoing/MultiPressComplete) from `handleInChildDriver`. Behavior depends on FeatureMap bits MS/MSR/MSL/MSM read from `fingerprintData['003B_FFFC']`. Each child = 1 button (`numberOfButtons` always 1). |
| Matter Generic Component **Motion Sensor** | 1.1.2 | MotionSensor + `invertMotion` pref + `setMotion` test command. See BUGS.md B19 for live status of the `updated()` state issue. |
| Matter Custom Component **Signal** | 1.1.2 | Motion→pushed adapter (motion `active` also emits button 1 pushed). |
| Matter Generic Component **Battery** | 1.1.1 | Battery + `bat*` attributes; events decoded in the parent (`parsePowerSource`). |
| Matter Generic Component **Switch** | 1.1.2 | On/Off with no-change suppression. |
| Matter Custom Component **Contact Sensor** | 1.0.1 | ContactSensor + `sensitivityLevel` (cluster 0x0080, e.g. Aqara P100); pref write via `componentSetSensitivityLevel`. |
| Matter Generic Component **SwitchBot Button** | 1.0.2 | Legacy (pre-1.8.0 event names) — see BUGS.md B8. No library includes; header author ymerj. |

None of the children define `parse(String)` any more (the no-op stubs were deleted in 1.9.0).

## 6. Stable symbol map — `Matter_Advanced_Bridge.groovy` (v1.9.0)

Do not maintain exact line counts or line-number ranges here: this file is under active refactoring and
they become wrong immediately. Search by these stable symbols instead:

| Area | Symbols to search for |
|---|---|
| Metadata/configuration | `version()`, `timeStamp()`, `_DEBUG`, `metadata`, `preferences` |
| Cluster registry | `SupportedMatterClusters`, `ParsedMatterClusters`, `ROOT_NODE_SUBSCRIPTIONS` |
| Parse entry and normalization | `parse(String)`, `parse(Map)`, `processParsedDescription`, `newParseCompatibilityPatch` |
| State-machine confirmation and globals | `checkStateMachineConfirmation`, `parseGlobalElements`, `gatherAttributesValuesInfo` |
| Descriptor normalization | `normalizeDeviceTypeList`, `normalizeTagList`, `parseDescriptorCluster` |
| Cluster parsers | methods named `parseXxx`, including `parseSwitch`, `parseDoorLock`, `parseWindowCovering`, `parseThermostat` |
| Event routing | `sendHubitatEvent`, `shouldFilterNoisyPostSubscribeEvent`, `warnUndeclaredAttributeOnce` |
| Lifecycle | `installed`, `updated`, `initialize`, `initializeVars`, `schedulePeriodicJobs`, `loadAllDefaults` |
| Subscription building | `fingerprintsToSubscriptionsList`, `buildSubscriptionPathGroups`, `sendSubscriptionCommands`, `sendSpammySubscription`, `minimizeByWildcard` |
| Child selection/creation | `mapMatterCategory`, `fingerprintToData`, `createChildDevice`, `copyEntireFingerprintToChild` |
| Component commands | methods named `componentXxx`, including `componentRefresh` and `componentPing` |
| Health/stats/version plumbing | `ping`, `deviceCommandTimeout`, `checkDriverVersion`, `driverVersionAndTimeStamp` |
| Included libraries | the four `#include kkossev...` directives at the end of the file |

---

## 7. Documentation rules

[docs/README.md](docs/README.md) is the map of both documentation trees — layout, per-page
conventions, and a "where does this go?" table. Both it and this guide are tracked, so a fresh clone
gets each. The rules below remain the authority; that page points at them.

`docs/user/` **is the current user documentation**, browsed on GitHub. Every page was audited against
the source on 2026-07-27. The wiki is superseded and frozen. Plan and status live in
`docs/maintainer/plans/DOCUMENTATION_MIGRATION_PLAN.md` and
`docs/maintainer/status/documentation-open-items.md` (local only).

Before any Jira write in the `HUB` project, retrieve `HUB-126`, read its complete current
description, and follow it. Do not rely on a cached copy. If it cannot be retrieved, do not write
to Jira and report the blocker.

1. **`AGENTS.md` is the agent instruction source of truth.** It stays at the repository root.
   `CLAUDE.md` and `CODEX.md` are pointers — do not put content in them.
2. **Public user documentation lives in `docs/user/`.** It is tracked and committed. It is the only
   documentation directory that ships.
3. **`docs/maintainer/` is local only** — untracked, gitignored, never published. Plans, unconfirmed
   reports, investigations, and migration evidence belong there. **Exception: the defect list moved
   out.** `docs/BUGS.md` is tracked and published, so other agents can see what was already
   classified, fixed, or ruled out. The same applies to `docs/TODO.md`, the community request list.
   Write both for that audience: no local paths, no hub addresses, no unmasked device serials, and no
   links to `docs/maintainer/**` (they will not resolve in a clone — name the file in backticks
   instead). What stays local is everything else: plans, investigations, migration evidence.
4. **The wiki is not a second source of truth.** It is frozen at baseline commit `c4000b7`
   (2026-07-27) and superseded by `docs/user/`. Do not edit wiki pages, and never copy content
   *from* the wiki into `docs/user/` — it is 2024 content describing limitations that no longer
   exist. The wiki is never deleted; its pages become notices linking to `docs/user/` at cutover.
5. **`docs/BUGS.md` and `docs/TODO.md` are both published.** `docs/user/help/known-issues.md` is
   still not a mirror of `BUGS.md` — the two have different audiences and different bars. `TODO.md`
   credits community members by forum handle with a link to the originating post; keep it factual and
   remove a name on request. A public known issue gets: affected version, symptom, workaround if any, and
   resolution status. Internal source locations and speculative causes stay in `docs/maintainer/`.
6. **A user-visible behavior change requires auditing the applicable page in `docs/user/`** in the
   same change, **and an entry in `CHANGELOG.md`**. Documentation-only edits do not require a driver
   version bump or timestamp change. Note the split: `CHANGELOG.md` (repo root) is the technical
   record for developers; `docs/user/project/revisions-history.md` is the user-facing narrative.
   Both need touching for a behavior change; neither is generated from the other.
7. **Every public support claim needs an evidence label:** Confirmed, Reported, Implemented
   unverified, Unsupported, Unknown, or Historical. Never write a bare `?`, `check`, or `TODO` in a
   compatibility table. "Confirmed" means tested on a named hub/bridge/device combination.
8. **Every technical page carries a status line:**
   `Applies to: <version> | Last verified: YYYY-MM-DD | Status: Current | Experimental | Historical`.
   `Historical` is the honest default for content migrated from the wiki but not yet re-verified.
9. **Audit against the released branch (`main`), not `development`.** When they differ materially,
   document released behaviour and label any upcoming change explicitly.
10. **Verify every claim against current code — never against the changelog or a comment.** The
    driver header records what was true when written, not what is true now. Two examples that
    reached a page before being caught: the 2024 "skip cluster `0x0033` for Aqara M3" workaround was
    reversed in 1.7.4, and the Button driver's `generatePushedOn` preference no longer exists. Read
    the metadata and the method.
11. **Component metadata is not only in the component file.** `#include`d libraries declare commands
    and attributes too — `ping` and `rtt` come from `matterHealthStatusLib`, and the whole
    `utilities` command from `matterUtilitiesLib`. Grep `Libraries/` as well, or you will document a
    driver as having fewer commands than it does.
12. **A declared command or attribute is not necessarily a visible one.** `_DiscoverAll` declares a
    five-value ENUM that never renders, and `attribute 'state'` is never populated. Prefer what the
    device page actually shows; ask for a screenshot when it matters.
13. **Some settings are platform-provided.** `ipAddress` and `networkStatus`, and the
    Default Current State / Show on Home page preferences, come from Hubitat and appear in no
    driver source.
14. **The released version is 1.8.8; 1.9.0 is a BETA.** Call it BETA, not "development branch".
15. **Do not publish absolute local paths, private hub addresses (RFC1918), or personal data.** Strip
    them from screenshots too.
16. **External publication, wiki edits, and link changes require explicit user authorization.**

### Driver pages

`docs/user/drivers/` holds one page per driver: the parent, each of the twelve custom component
drivers, and one shared page for the ten Hubitat stock drivers. Filenames are function-based
kebab-case without the `Matter Generic Component` / `Matter Custom Component` prefix; the page's H1
is the exact driver name as it appears in Hubitat.

`docs/user/drivers/index.md` carries the driver assignment table. Its source of truth is
**`mapMatterCategory()`** in `Matter_Advanced_Bridge.groovy` — when that function changes, that table
changes in the same commit. The assignment is a first-match-wins chain over the endpoint's
**clusters**, not over its Matter device type; device type is only a tie-breaker for colour bulbs
and for contact versus water leak sensors.

Two drivers are never assigned automatically and must be set by hand: `Matter Custom Component
Signal`, and the deprecated `Matter Generic Component SwitchBot Button` (use Button instead).
