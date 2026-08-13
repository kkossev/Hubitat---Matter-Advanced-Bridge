# Bug Findings — Matter Advanced Bridge v1.8.8 / v1.9.0

Deep code-review findings, starting 2026-07-04. Intended as a work list for AI agents applying fixes,
and as a public record of what has been examined in this driver.

> **This file is published.** It is the answer to "was this already looked at?" — it records what was
> classified as a defect, what was fixed and confirmed on real hardware, what was investigated and
> ruled out, and what is deliberately left alone. Before reporting or re-deriving a problem, search
> here first: a `[x]` entry usually carries the evidence and the reasoning, not just the fix.
>
> It is a **maintainer** document, so it names source locations and internal methods, and its line
> numbers go stale quickly — always grep the quoted code before acting. For the user-facing view of
> current problems see [docs/user/help/known-issues.md](user/help/known-issues.md), which is
> deliberately *not* a mirror of this file.
>
> Entries sometimes cite `docs/maintainer/**` files — `TODO.md`, the `plans/` documents, and the
> retired `BUGS2.md` staging file. Those are **maintainer-local and deliberately not published**, so
> those names will not resolve in a clone. Nothing in this file depends on them: each entry states
> its own evidence. **Read [AGENTS.md](../AGENTS.md) at the repository root
first** — it explains the file
layout, the parse/discovery architecture, and the intentionally-disabled code that must not be touched.
An earlier independent review (Codex, 2026-07-04) was **merged into this file on 2026-08-13** and
`BUGS_CODEX.md` deleted — this is now the single bug list. Its findings are cross-referenced inline as
`[Codex Xn]`; the full ID mapping and the provenance are in
[Appendix: the merged Codex review](#appendix-the-merged-codex-review-2026-07-04) at the end.

## Ground rules for fixing

- Line numbers refer to the loose source files at v1.8.8 (parent timeStamp `2026/05/29 07:01 AM`).
- Original-review line numbers refer to v1.8.8. Items B12 onward and section D were reviewed against
  the staged v1.9.0 working tree on 2026-07-25 (parent timeStamp `2026/07/25 8:57 PM`).
- B20/B21 and C22–C24 came from a separate 2026-07-25 log review (Nuki Smart Lock 4.0 `getInfo()`,
  formerly the staging file `BUGS2.md`, now merged here) and were read against the *earlier* snapshot
  of that day (parent timeStamp `2026/07/25 5:17 PM`) — grep their quoted code before applying.
  None were reproduced on the dev hub; they are static findings plus log evidence.

- Library fixes (`Libraries\*.groovy`) affect the parent **and** every component driver that includes
  the library (matterCommonLib, matterHealthStatusLib) — check both sides.
- **One bug at a time, in the order the user (kkossev) chooses.** After each fix the code is uploaded
  to the dev hub and tested; proceed to the next item only after the user confirms success.
- **Do NOT bump `version()`/`timeStamp()`/`@Field ...Version` and do NOT add header changelog lines
  after individual fixes** — the user explicitly says when to bump (release point).
- Remember `packageManifest.json` + `MatterAdvancedBridge.zip` are only touched at release time.
- HE quirk relied on below: bare undefined *identifiers* in GStrings resolve to `null` (no exception);
  calling an undefined *method* throws `MissingMethodException`; a method call on `null` throws NPE.
- Status legend: `[ ]` open, `[x]` fixed & confirmed by the user, `[?]` needs verification on a real
  device / real bridge before changing. Items marked **ASK USER** are (or may be) the author's
  deliberate design — do not change without confirmation.

---

---

## Open items — index (refreshed 2026-08-13, v1.9.1)

**48 closed, 9 open, 1 release gate.** Closed entries are kept deliberately: they carry the evidence
and the ruled-out hypotheses, several are cited by ID from the driver source, and they are what stops
a finding being re-derived. See the note at the end of this section.

Only three open items actually need code written. The rest are staged fixes waiting for hardware.

### Needs a hub/device test — code already applied

| ID | Item | Blocked on |
|---|---|---|
| **B12** | `sendHubitatEvent()` duplicate filter read the parent's state, never the child's | Behaviour change; press Initialize twice and watch `duplicatedCtr` |
| **B14** | Discovery reports fell through the ordinary duplicate filter | Same run as B12 |
| **B16** | Camera FeatureMap/ClusterRevision parsed as decimal | A `0x0551` camera endpoint — none available |
| **B21** | `VendorName` never stored for endpoint 0 | Any directly-paired (non-bridged) Matter device |

### Needs code

| ID | Item | Size |
|---|---|---|
| **B18** | Window Shade initialises 0% as both closed and open | small |
| **B19** | Motion Sensor invert-setting update can throw or fabricate motion | small |
| **C8** | `.hubitat\metadata.json` stale entry + duplicate ids | tooling data file only, no driver risk |

### Open but deliberately not being worked

| ID | Item | Why |
|---|---|---|
| **B15** | `readAttributeSafe` validation | **Needs splitting.** Its Problem 1 is the same defect as **B23**, which is `[x]` and fixed. Only Problem 2 (`minimizeStateVariables` wiping `state.fingerprintXX`) is still open. |
| **B26** | Component wildcard reads bypass chunking | Logged for tracking; no evidence any of them actually fail. Fixing means expanding `-1` reads into concrete paths — a larger change. |
| **D2** `[?]` | v1.9.0 release artifacts not synchronised with loose sources | Release gate, not a defect. Blocks publishing the bundle, not development. |

### Status legend

`[ ]` open · `[x]` fixed **and** confirmed by kkossev on the hub · `[?]` needs verification on real
hardware before changing · **ASK USER** the author's deliberate design — do not change unprompted.

A `[x]` here means a human confirmed it on a real hub, not that the code looks right. Several entries
record fixes that were applied and later found incomplete, which is why the distinction is kept strict.

### Why closed entries are not deleted

They are cited by ID from **published driver source** — `Matter_Advanced_Bridge.groovy` references
B17 (twice) and B20, `Libraries/Matter_State_Machines.groovy` references B24 — and from `AGENTS.md`
and the optimization plans. They also hold the negative results: B24 records why a "9 paths per read"
theory is wrong and must not be re-adopted, B22 records the interleaving hazard B25 had to design
around, and C26 records why `_TRACE_ALL_MESSAGES` exists at all. None of that is recoverable from the
code.

---
## A. Runtime exceptions

### A1. `[x]` `fingerprintsToSubscriptionsList()` throws NPE when an endpoint has no supported clusters — `[Codex A1]`
- Where: `Matter_Advanced_Bridge.groovy:2727–2730` — `List subscribeList = fingerprintMap['Subscribe'] as List`
  followed immediately by `subscribeList.each {...}`.
- Problem: the discovery SM sets `Subscribe` only when at least one ServerList cluster matched
  `SupportedMatterClusters` (`Matter_State_Machines.groovy:758`). Endpoints with only unsupported
  clusters keep a `fingerprintXX` state entry without `Subscribe` → `null.each` → NPE, aborting the
  whole subscription compilation (remaining fingerprints are never processed).
- Failure scenario: bridge exposes one endpoint with e.g. only cluster 0x0050 (ModeSelect) →
  `_DiscoverAll` finishes device discovery, then crashes in the final "Subscribe" step → no/partial
  `state.subscriptions`, devices stop reporting.
- Fix: guard before iterating: `if (!subscribeList) { logDebug "...no supported clusters, skipped"; return }`
  (return inside `state.each` closure = continue).
- Verify: seed a fake `state.fingerprintEE` with `ServerList` but no `Subscribe`, run
  `_DiscoverAll('Subscribe')` — must complete and log the skip.
- Confidence: High.

### A2. `[x]` Same method throws NPE when a cluster's `NNNN_FFFB` AttributeList is missing — `[Codex A2]`
- Where: `Matter_Advanced_Bridge.groovy:2745–2747` — `List clusterAttrList = fingerprintMap[clusterListName];
  clusterAttrList = clusterAttrList.collect { safeHexToInt(it) }` — the `.collect` runs **before** the
  null check at line 2749 (which is therefore dead).
- Trigger: `DISCOVER_ALL_STATE_CLUSTER_DATA_WAIT` explicitly proceeds after a timeout with incomplete
  fingerprint data (`Matter_State_Machines.groovy:842–855`) → `0006_FFFB` etc. may be absent.
- Fix: move the null/empty check above the `.collect` and `return` (skip that attribute group).
- Verify: seed `Subscribe:[6]` with no `0006_FFFB`, run the Subscribe step — no exception, cluster skipped.
- Confidence: High.

### A3. `[x]` `illumEvent()` NPE drops all illuminance reports until Save Preferences (upgrade path)
- Where: `Matter_Advanced_Bridge.groovy:4215–4216` —
  `Map lastRxMap = stringToJsonMap(state.lastRx2)` then `now() - lastRxMap['illumTime']`.
- Problem: `state.lastRx2` is created only by `resetStats2()`, which runs from `updated()` and from
  `initializeVars(fullInit=true)`. After a driver **upgrade** from a pre-1.7.8 version (no `lastRx2`
  in state), `checkDriverVersion()` calls `initializeVars(false)` which does **not** create it →
  first illuminance report: `lastRxMap` is `[:]`, `now() - null` → NPE. The exception is caught by
  the `parserFunc` try/catch in `processParsedDescription()` (logged as a warning), so the driver
  survives, but **every illuminance event is silently lost** until the user opens the device page and
  hits Save Preferences.
- Fix: in `illumEvent()`, default the timestamp:
  `Long illumTime = (lastRxMap['illumTime'] ?: (now() - (minReportingTimeIllum ?: 10) * 1000)) as Long`
  (and write it back). Same defensive pattern already exists in `sendDelayedEventIllum()` line 4245.
- Verify: `state.remove('lastRx2')`, send an illuminance report (or `parseTest`) — event must be emitted.
- Confidence: High (code path); Medium (how many users hit it).

### A4. `[x]` `readSingeAttrStateMachine` crashes when `state.stateMachines` was removed — **VERIFY**
- Where: `Matter_State_Machines.groovy:59` — `if (state['stateMachines'] == null) { state['stateMachines'] = [] }`
  initializes it as an empty **List**, not a Map; the next lines and line 75 then index/assign it with
  String keys (`state['stateMachines']['readSingeAttrState'] = 1` → `putAt(List,String)` →
  MissingPropertyException). Same landmine at `Matter_State_Machines.groovy:444`
  (`state.bridgeDescriptor = []`, comment even asks "or [:] ?") — currently unreachable because
  `initializeVars(fullInit=true)` runs right after and sets `[:]`.
- Trigger: the `minimizeStateVariables` preference/utility **removes `state.stateMachines`**
  (`matterUtilitiesLib.groovy:259`); a subsequent `utilities 'readAttributeSafe ...'` (or any SM entry
  that relies on `initializeStateMachineVars()` alone) hits the List-vs-Map path.
- Fix: change both literals to `[:]`. Trivially safe.
- Verify: on a test hub: enable minimizeStateVariables, then run `utilities 'readAttributeSafe 1 6 0'`.
- Confidence: Medium (exact Groovy failure mode on HE needs the test above; the `[]`→`[:]` fix is safe regardless).

---

## B. Wrong behavior / logic issues

### B1. `[x]` Button child: FeatureMap parsed as decimal → pushed/doubleTapped events lost — `[Codex B2]`, worse than reported
- Where: `Components\Matter_Generic_Component_Button.groovy:457` —
  `Integer featureMap = safeToInt(fingerprint['003B_FFFC'] ?: '1F')`; also line 285 (display path
  `safeToInt(descMap.value)`), line 164 (`getFeatureMap() ?: 0x1F`), line 220 (MultiPressComplete check).
- Problem: `newParseCompatibilityPatch()` stores `FFFC` as an **uppercase hex string** (e.g. `'1E'`).
  `safeToInt('1E')` → exception → **0**. Even the intended fallback `'1F'` fails the same way
  (`safeToInt('1F')` = 0). Consequences for a typical MSM-capable button (FeatureMap 0x1E):
  1. ShortRelease handler: `getFeatureMap() ?: 0x1F` → 0 is falsy → 0x1F assumed → MSM bit set →
     ShortRelease deliberately ignored, "waiting for MultiPressComplete";
  2. MultiPressComplete handler: `getFeatureMap()` → 0 → "device does not declare MSM" → **ignored**.
  Net result: neither path emits `pushed`; double-taps are also lost. (Buttons that only send
  ShortRelease still work by accident via the false 0x1F.)
- Fix: use `safeHexToInt(...)`/`safeNumberToInt(...)` in `getFeatureMap()` (with numeric default
  `0x1F`, not the string), and `safeHexToInt` at line 285. Keep the two handler checks as-is.
- Verify: **VERIFY ON DEVICE** — with a fingerprintData containing `003B_FFFC: '1E'`, single-press →
  `pushed`, double-press → `doubleTapped`.
- Confidence: High (static); device test mandatory (real FeatureMap values differ per vendor).
- **Fixed**: `getFeatureMap()` now uses `safeHexToInt(fingerprint['003B_FFFC'], 0x1F)` with a numeric
  default, and the FFFC display path uses `safeHexToInt(descMap.value)`. Both handler checks unchanged.

### B2. `[x]` Air Purifier `setSpeed`/`cycleSpeed` pass Integers to a String API → no fan command sent — `[Codex B1]`
- Where: child `Components\Matter_Generic_Component_Air_Purifier.groovy:192–252` maps
  'low'→10 … 'high'→50, then `parent?.componentSetSpeed(device, newSpeed)`; parent
  `Matter_Advanced_Bridge.groovy:3249` is `componentSetSpeed(DeviceWrapper dw, String speed)` whose
  switch matches only the label strings ('off','low','medium-low',…) and maps them to Matter FanMode
  0–6. `'30'` matches nothing → warn "speed is not supported", no write.
- Note the double translation is wrong even conceptually: the child's 10/20/30/40/50 values are
  IKEA-Zigbee legacy, the parent expects labels. Also child maps both 'medium-low' and 'medium' to
  different numbers but the parent maps 'medium-low'→2 and 'medium'→2 identically.
- Fix: child should pass the **original `speed` string** straight through (`parent?.componentSetSpeed(device, speed)`);
  `cycleSpeed()` should cycle label strings, not numbers.
- Verify: `setSpeed('medium')` on an Air Purifier / Fan child → parent debug shows a 0x0202:0x0000 write.
- Confidence: High.

### B3. `[x]` `componentSetColor()` computes level but never applies it — `[Codex B5]`
- Where: `Matter_Advanced_Bridge.groovy:3376–3399` — `levelScaled` computed at 3389, the
  MoveToHueAndSaturation command (3392–3397) carries only hue/sat/transition.
- Failure: `setColor([hue:10, saturation:80, level:20])` changes color only; brightness unchanged
  (Hubitat convention is that setColor's level is honored).
- Fix: after sending the color command, `if (colormap.level != null) { componentSetLevel(dw, colormap.level as BigDecimal) }`
  (drop the unused `levelScaled`).
- Verify: setColor with a level different from current → both color and level events return.
- Confidence: High.

### B4. `[x]` CO₂-only endpoints (cluster 0x040D) are parsed but never mapped to a child — `[Codex B3]`
- Where: `Matter_Advanced_Bridge.groovy:2911–2997` (`mapMatterCategory`) — 005B and 042A route to the
  Air Purifier child, **040D is absent**; yet 0x040D is in `SupportedMatterClusters` (363),
  `ParsedMatterClusters` (396), and the Air Purifier child handles `040D_0000` (child lines 431–454).
- Failure: a standalone CO₂ sensor endpoint (`ServerList` has 040D but not 005B/042A — e.g. an
  IKEA ALPSTUGA sub-endpoint) is assigned some other child (often the temperature/humidity Omni
  Sensor first, or the fallback Switch) → `carbonDioxide` never updates.
- Fix: add `if ('040D' in d.ServerList) { return [namespace:'kkossev', driver:'Matter Generic Component Air Purifier', product_name:'Air Quality Sensor'] }`
  next to the 042A branch — **but check ordering**: it must stay AFTER 0400/0402/0403/0405/0406 only if
  the user prefers combined T/H endpoints to remain Omni Sensors. ASK USER about the desired order.
- Verify: fake fingerprint with `ServerList:['040D']` → child = Air Purifier; CO₂ report → event.
- Confidence: High (omission), Medium (which order the user wants).
- **Fixed**: the `040D` branch sits after 0406 and before 042A — combined T/H endpoints stay Omni Sensors.

### B5. `[x]` Energy-only endpoints (cluster 0x0091) never mapped to the Power/Energy child — `[Codex B4]`
- Where: `mapMatterCategory` line 2983 routes only `'0090'`; 0091 is parsed (`parseElectricalEnergyMeasurement`,
  1638) and handled by the Power/Energy child, but an endpoint with 0091 and no 0090 falls through.
- Fix: `if ('0090' in d.ServerList || '0091' in d.ServerList)` on the same branch.
- Verify: fake fingerprint `ServerList:['0091']` → Power/Energy child created.
- Confidence: High (omission), Medium (real devices with 0091-only endpoints are rare).

### B6. `[x]` `utilities 'subscribeSingleAttribute ...'` ignores its add/remove argument
- Where: `Libraries\matterUtilitiesLib.groovy:111–124` — the method validates 4 parameters and parses
  `parameters[1..3]`, but never assigns `parameters[0]`; line 119 calls
  `updateStateSubscriptionsList(addOrRemove, ...)` with `addOrRemove` undefined → `null` (or a stale
  binding value left over from the parent's `addOrRemove = 'add'` idiom) → "unknown action: null".
- Fix: add `String addOrRemove = parameters[0]` before the call.
- Verify: `utilities 'subscribeSingleAttribute add 1 6 0'` → entry appears in `state.subscriptions`.
- Confidence: High.

### B7. `[x]` `utilities 'resetStats'` calls nonexistent `sendMatterEvent()`
- Where: `Libraries\matterUtilitiesLib.groovy:268` — `sendMatterEvent([...])` is defined **nowhere**
  in the package (grep-verified). `MissingMethodException` is swallowed by the `utilities()` try/catch
  (line 165–171), so the stats map IS reset but the command reports
  "Exception ... caught while processing resetStats" and the `initializeCtr` event is never sent.
- Fix: replace with `sendEvent(...)` (or `sendInfoEvent`).
- Verify: `utilities 'resetStats'` → no warning, initializeCtr event visible.
- Confidence: High.

### B8. `[x]` **ASK USER** — SwitchBot Button child is dead since 1.8.0 (listens for events the parent no longer sends)
- Where: `Components\Matter_Generic_Component_SwitchBot_Button.groovy:55–75` expects parsed events
  named `currentPosition`; the parent's `parseSwitch()` (`Matter_Advanced_Bridge.groovy:1403–1422`)
  has forwarded **everything** as `handleInChildDriver` since 1.8.0 ("removing old custom parse code"),
  and grep confirms the parent never emits a `currentPosition` event anymore. The child's `else`
  branch just `sendEvent`s the raw wrapper map.
- Also: this child is never assigned by `mapMatterCategory` (user assigns it manually), and it does
  not include matterCommonLib/matterHealthStatusLib (its `ping()` calls `parent?.componentPing` directly — fine).
- Options: (a) retire/mark deprecated (users switch to 'Matter Generic Component Button');
  (b) add a `handleInChildDriver` branch decoding 003B attribute 0x0001 CurrentPosition to restore it.
  Decision is kkossev's.
- Confidence: High that it cannot work as-is on 1.8.x; intent unknown.

### B9. `[x]` Humidity of exactly 0 % is rejected as invalid — `[Codex C3]`
- Where: `Matter_Advanced_Bridge.groovy:1580` — `if (valueInt <= 0 || valueInt > 100)`.
- Note: v1.8.7 explicitly removed the equivalent zero-rejection for illuminance; humidity kept it.
- Fix: `valueInt < 0`.
- Verify: report with raw value 0 → `humidity 0 %` event.
- Confidence: High (trivially safe); real-world impact low.

### B10. `[x]` **ASK USER** — Door Lock `installed()` seeds `lock` with invalid value `'closed'`
- Where: `Components\Matter_Generic_Component_Door_Lock:1305` —
  `sendEvent(name: 'lock', value: 'closed', ...)`. The Lock capability enum is
  `["locked","unlocked with timeout","unlocked","unknown"]`; `'closed'` is not in it. Dashboards/apps
  keying on lock state see an out-of-enum value until the first real report. The 1.4.1 history says
  "fixed EZ dashboards unhappy face" — this line may be that (possibly misguided) fix, hence ASK USER.
- Suggested fix: `value: 'unknown'` (or `'locked'` if the dashboard needs a "real" state).
- Verify: recreate a lock child → check the seeded `lock` attribute and the EZ dashboard tile.
- Confidence: High that the value is out-of-spec; Medium on what the user wants instead.

### B11. `[x]` `discoveryTimeoutScale` defaults mismatch (preference '2' vs initializeVars '1')
- Where: preference `Matter_Advanced_Bridge.groovy:187` — `defaultValue: '2'` (1.8.8 changelog:
  "changed the default timeout to be x2"); but `initializeVars()` line 4068 writes `'1'` when the
  setting is null (fresh install, `loadAllDefaults`, `_DiscoverAll`'s full init).
- Effect: fresh installs and every `_DiscoverAll('All')` run with 1× timeouts, contradicting the
  1.8.8 change; slow bridges hit spurious discovery timeouts again.
- Fix: change line 4068 to `[value: '2', type: 'enum']` (single source of truth; consider a
  `@Field static final String DISCOVERY_TIMEOUT_SCALE_DEFAULT = '2'`).
- Verify: `loadAllDefaults` → preference shows 2x; `getDiscoveryTimeoutScale()` returns 2.
- Confidence: High.

### B12. `[ ]` `sendHubitatEvent()` duplicate filter reads the PARENT's state, never the child's
- Found 2026-07-25 against **v1.9.0** (not part of the original 1.8.8 review) while analyzing a user log.
- Where: `Matter_Advanced_Bridge.groovy`, `sendHubitatEvent()` duplicate-check block —
  `Object latestEvent = dw?.device?.currentState(name) ?: device.currentState(name)`.
  The comment above it ("Check child device currentState if available, otherwise check parent device for
  bridge events") states the intent; `dw` is already the `ChildDeviceWrapper`, so `.device` does not
  resolve to the child's own state and every child event falls through to `device.currentState(name)` —
  the **parent** bridge device.
- Effect 1 (observed): for the ~30 call sites that pass `ignoreDuplicates = true` the filter never
  matches, because the parent has no such attribute. Static/config attributes are therefore re-sent and
  re-logged after **every** `initialize()` / `reSubscribe()` — in the reported log `colorMode is CT`
  repeats on every `setColorTemperature`, and a single extra Initialize click replayed the whole
  attribute set (battery, setpoint limits, `numberOfButtons`, `supportedThermostatModes`, `colorMode`)
  a second time ~45 s later. `state.stats['duplicatedCtr']` stays at 0, which is the cheap diagnostic.
- Effect 2 (latent, not yet observed): the parent **does** declare `battery` and `batteryVoltage`
  (root-node PowerSource case, metadata line ~164). Once the bridge itself holds one of those, a child
  event carrying the same value would be suppressed as a "duplicate" of the parent's.
- Fix: `Object latestEvent = (dw != null) ? dw.currentState(name) : device.currentState(name)` —
  consult the child when there is one, the parent only for bridge (endpoint 00) events.
- Behavior change to watch on the hub (this is why it was split out of the log-noise fixes):
  genuinely repeated *identical* attribute reports now stop at the parent. Checked before applying —
  no MAB child re-arms a timer from a repeated value: the only `runIn()`s in `Components\` are
  `logsOff`/`clearInfoMode`, the Door Lock's command timeouts, and the Window Shade
  `operationTimeoutTimer` (armed by a command, not by a report). Matter *events* (evtId) and the
  internal `unprocessed`/`handleInChildDriver` names are unaffected (events bypass the filter; the two
  internal names are routed via `child.parse()` and never become attribute states).
  The one visible change is `Matter Custom Component Signal`: it turns every `motion:active` into a
  `pushed` event, so the periodic `active` heartbeat at the subscription max interval will no longer
  produce a phantom button press. That looks like a fix, but confirm with the user.
- Verify: with debug logging on, press Initialize twice — the second burst must log
  `sendHubitatEvent: IGNORED duplicate event: ...` (logTrace) instead of re-sending, and
  `state.stats['duplicatedCtr']` must start counting. `Refresh` and `_DiscoverAll` must still report
  everything (they force `isStateChange` and bypass the filter).
- Confidence: High on the defect; the hub test is about the behavior change, not the diagnosis.
- **Fix IS applied in the working copy** — confirmed against the code 2026-08-13:
  `Matter_Advanced_Bridge.groovy:2646` now reads
  `Object latestEvent = (dw != null) ? dw.currentState(name) : device.currentState(name)`, with a
  comment recording that it used to read `dw?.device?.currentState(name)`. This entry never said so;
  only the 2026-07-25 session log did, which is why it looked unstarted. Still `[ ]` because the
  behaviour change is unverified on the hub.

### B13. `[x]` `getInfo(<endpoint>)` is dead for every endpoint except 0 once the state is minimized
- Found 2026-07-25 against **v1.9.0**, from a user log:
  `Zemismart M1 Matter requestExtendedInfo(): serverList is null!` right after `getInfo() endpoint:6`.
- Where: `Libraries/matterUtilitiesLib.groovy`, `requestExtendedInfo()` —
  `List<String> serverList = state[getFingerprintName(endpoint)]?.ServerList`, and the same state-only
  read in `infoCollectStateMachine()` (the `BASICINFO` and `INFO_STATE_SERVER_LIST` cases) and in
  `requestAndCollectServerListAttributesList()` (parent, ~line 2801).
- Problem: the same "state was minimized away" class as B12/the water-sensor fix. `minimizeStateVariables`
  (preference default **ON**) deletes every `state` key starting with `fingerprint`
  (`matterUtilitiesLib.groovy:366`), so the ServerList is gone. Note the asymmetry that made this look
  device-specific: endpoint 0's fingerprint is `state.bridgeDescriptor`, which does **not** start with
  `fingerprint` and therefore survives — `getInfo(0)` keeps working while `getInfo(<anything else>)`
  fails on any hub running the default preference.
- Second defect, same lines: the warning names neither the endpoint nor the reason, so a minimized
  fingerprint and a genuinely non-existent endpoint produce byte-identical output. In the reported log
  both were possible (that bridge shows `endpointsCount: 5`), and the log alone could not tell them apart.
- Fix: new `getEndpointServerList(Integer endpoint)` in the parent, next to the existing
  `getDeviceServerList(DeviceWrapper)` (~line 3843) and using it — state fingerprint first, then the
  child's `fingerprintData`, which is exactly the copy that survives minimization (AGENTS.md §2.6).
  All four call sites above now use it, and the warning spells out both possible causes.
- Verify: on a bridge whose `state.fingerprint*` keys are gone (enable *Minimize State Variables*),
  run `getInfo` for an endpoint that HAS a child device — the collector must run to
  `Extended Bridge Discovery finished` instead of warning. For an endpoint that does not exist, the new
  warning must name the endpoint and both causes. Re-check `getInfo(0)` (bridge) still works, and that a
  freshly discovered bridge (fingerprints present in state) is unaffected.
- **Device-verified on the dev hub (2026-07-25, Zemismart M1 endpoint 06 "1 Gang Switch"):** the cause was
  the minimized state, not a missing endpoint. `getEndpointServerList(6): the state fingerprint is gone
  (minimized?) - using the fingerprintData of 1 Gang Switch` → `startInfoCollect(): endpoint=6
  queue=[0006, 001D, 0039, 0040]` → `Extended Bridge Discovery finished`. The 0x0041 crash guard still
  fired correctly. The non-existent-endpoint branch of the new message was not exercised.

### B14. `[ ]` Discovery reports still fall through the ordinary duplicate filter
- Found 2026-07-25 against the staged **v1.9.0** working tree.
- Where: `Matter_Advanced_Bridge.groovy:2587-2602`. The burst filter handles refresh and discovery,
  but the ordinary filter only checks `!isRefreshActive`.
- Problem: `!isDiscoveryActive` is missing. During `_DiscoverAll`, the first copy of an attribute can
  pass `isBurstDuplicate()` and then be discarded by the ordinary `currentState()` comparison, despite
  the explicit `isStateChange=true` and `isDiscovery=true` flags.
- Fix: add `&& !isDiscoveryActive` to the ordinary duplicate-filter condition.
- Verify: run `_DiscoverAll` when children already hold the reported values. The first value for each
  attribute must be delivered with `[discovery]`; only exact repeats in the burst should be dropped.
- Confidence: High.
- **Fix applied in the working copy (2026-07-25), awaiting the hub test:** `&& !isDiscoveryActive`
  added to the ordinary duplicate-filter condition (`Matter_Advanced_Bridge.groovy:2602`). Parses locally.

### B15. `[ ]` `readAttributeSafe` validates the wrong AttributeList and fails after minimization
- Found 2026-07-25 against the staged **v1.9.0** working tree.
- Where: `Libraries\Matter_State_Machines.groovy:98-146`, especially
  `List<String> attributeList = state[fingerprintName]['AttributeList']`.
- Problem 1: plain `AttributeList` belongs to Descriptor cluster 0x001D. Other cluster lists are stored
  under keys such as `0006_FFFB`, so a safe read can reject a valid attribute or accept an unrelated
  Descriptor attribute with the same id.
- Problem 2: `minimizeStateVariables` is ON by default and removes every `state.fingerprintXX` key.
  The utility fails even though the child's `fingerprintData` retains ServerList and `NNNN_FFFB` data.
  A4 fixed the List-vs-Map crash but did not make this utility functional afterward.
- Fix: use `getEndpointServerList()` plus state/child `fingerprintData` fallback; use `AttributeList`
  only for 0x001D and `String.format('%04X_FFFB', cluster)` for every other cluster.
- Verify: with minimized state, `utilities 'readAttributeSafe 1 6 0'` must read OnOff when supported,
  while a genuinely absent cluster or attribute must still be rejected.
- Confidence: High.

### B16. `[ ]` Camera FeatureMap and ClusterRevision are parsed as decimal
- Found 2026-07-25 against the staged **v1.9.0** working tree.
- Where: `Components\Matter_Generic_Component_Camera_AV_Stream.groovy:254-265` uses
  `safeToInt(rawVal)` for FFFC and FFFD.
- Problem: `newParseCompatibilityPatch()` normalizes scalar FFFC/FFFD Numbers to uppercase hex strings
  (`Matter_Advanced_Bridge.groovy:732-735`). `001F` therefore becomes 0, while `0010` becomes decimal
  10 instead of hex 16.
- Effect: decoded camera capabilities, diagnostics and `clusterRevision` are wrong.
- Fix: use `safeHexToInt(rawVal)` for both global elements.
- Verify: feed FFFC=`001F` and FFFD=`0010`; the child must store 31 and 16 respectively.
- Confidence: High.
- **Fix applied in the working copy (2026-07-25):** both global elements now use `safeHexToInt(rawVal)`
  (component lines 257 and 264). The other `safeToInt()` calls in that switch were left alone — they
  read genuinely decimal values (volumes, min/max levels), not hex-normalized globals. Parses locally.
  Untested on a device: no 0x0551 camera endpoint is available (see also D1).

### B17. `[x]` Any unrelated Map callback cancels an active bridge-ping timeout
- Found 2026-07-25 against the staged **v1.9.0** working tree.
- Where: every `parse(Map)` calls `prepareForParse()`, which unconditionally runs
  `unschedule('deviceCommandTimeout')` at `Matter_Advanced_Bridge.groovy:484-488`. Only a matching
  0x0028/0x0000 reply clears `state.states.isPing` at lines 1092-1104.
- Problem: a subscription report, write callback, Invoke or other unrelated response can cancel the
  timeout without completing the ping. A lost ping records neither success nor failure and leaves stale
  `isPing` state until a later matching report or another ping.
- Fix: cancel the timeout only when the callback completes the timed command, or give ping its own
  correlated timeout that unrelated traffic cannot cancel.
- Verify: suppress the ping reply while allowing unrelated reports. `deviceCommandTimeout()` must still
  send `rtt: timeout`, increment `pingsFail` and clear `isPing`.
- Confidence: High.
- **HUB-TESTED 2026-08-13 — kkossev ran several discoveries, all clean.** That exercises the
  no-regression half thoroughly: `prepareForParse()` runs on every inbound message, and the discovery
  ping gate (`DISCOVER_ALL_STATE_PING_BRIDGE`) plus the periodic health-check ping both complete
  normally, so the guard is not stranding timers or producing spurious
  `no response received (sleepy device or offline?)` warns during normal traffic. That was the real
  risk this change carried, and it is now excluded.
  **Not exercised:** the failure path itself — deliberately losing a ping reply so
  `deviceCommandTimeout()` fires with `isPing == true`. Verifying that needs a suppressed reply, which
  is awkward to stage. Cheap ongoing check instead: `state.stats.pingsFail` should stay flat during
  normal operation and only move when a ping genuinely goes unanswered.
- **Fix applied 2026-08-13.**
  The key finding while applying it: `deviceCommandTimeout` has **two users with different completion
  rules**, which is why the unconditional cancel existed and why it cannot simply be deleted.
  1. `initialize()` (`:3065`) arms it for **55 s** to guard the subscription handshake, with
     `isPing == false`. Any inbound message legitimately proves the subscription works, so cancelling
     on unrelated traffic is **correct** for this user and must be preserved.
  2. `ping()` (`:5074`) arms it for `COMMAND_TIMEOUT` (15 s) with `isPing = true`. Only the matching
     `0x0028/0x0000` reply completes it.
  The fix therefore *narrows* the cancel rather than removing it: `prepareForParse()` (`:482`) now skips
  `unschedule()` while `state.states?.isPing == true`, and the ping completion path (`:1118`) cancels the
  timer itself when it clears `isPing`. Both users keep their intended semantics.
- Side effects, both wanted:
  - It also removes a scheduler/DB write from the per-message hot path whenever no ping is outstanding
    (the common case) — this closes the `unschedule` half of Step 2 in
    `plans/Matter_Advanced_Bridge_OPTIMIZATION_PLAN.md`.
  - A ping issued while `isInfo == true` never completes, because the ping branch in
    `gatherAttributesValuesInfo()` is an `else if` **after** the `isInfo` branch (AGENTS.md §2.7). That
    stuck `isPing` used to be masked — the unconditional cancel killed the timer and no one noticed.
    It now self-heals after 15 s via a visible `rtt: timeout` + `pingsFail`. That is an improvement, but
    it means the underlying `else if` ordering hazard becomes **visible** rather than silent. Do not
    mistake such a timeout for a regression of this fix.
- Checked and deliberately not changed: `startInfoCollect()` (`matterUtilitiesLib:246`) force-clears
  `isPing`. No unschedule is needed there — once `isPing` is false the normal cancel in
  `prepareForParse()` resumes on the very next message, exactly as before.

### B18. `[ ]` Window Shade initializes 0% as both closed and open
- Found 2026-07-25 against **v1.9.0**, component v1.2.5.
- Where: `Components\Matter_Generic_Component_Window_Shade.groovy:488-493`.
- Problem: the driver defines `OPEN=100` and `CLOSED=0`, then initializes position, targetPosition and
  level to 0 while emitting `windowShade='open'` and `switch='on'`.
- Effect: a new or manually initialized child presents contradictory state until a report arrives.
- Fix: seed `closed`/`off` consistently, or use `unknown` until the first position report.
- Verify: create a child or call `initialize()` and inspect all five attributes before refreshing.
- Confidence: High.

### B19. `[ ]` Motion Sensor invert-setting update can throw or fabricate motion
- Found 2026-07-25 against **v1.9.0**, component v1.1.2.
- Where: `Components\Matter_Generic_Component_Motion_Sensor.groovy:108-112` reads
  `device.currentMotion`.
- Problem: other drivers use `device.currentValue('motion')`; `currentMotion` is not a declared local or
  accessor. Even if HE returns null rather than throwing, the ternary maps every non-`active` value,
  including uninitialized state, to `active`.
- Fix: use `device.currentValue('motion')` and invert only exact `active`/`inactive` values; otherwise
  wait for the next physical report.
- Verify: toggle before the first report (no event/no exception), then after active and inactive reports.
- Confidence: Medium-High; null-to-active is certain, while missing-property behavior depends on HE.

### B20. `[x]` `getInfo()` reads a whole AttributeList in one request — ThreadNetworkDiagnostics times out
- Found 2026-07-25 against **v1.9.0** (parent timeStamp `2026/07/25 5:17 PM`), from a user log of an
  *Extended Bridge Discovery* (`getInfo()` on endpoint 0) against a **Nuki Smart Lock 4.0** paired
  *directly* to Hubitat (Root Node, `PartsList [0001]`) — not behind a bridge. **Line numbers may have
  shifted** (the working copy was being edited the same afternoon) — grep the quoted code before applying.
- Where: `Matter_Advanced_Bridge.groovy:2748–2791` `requestMatterClusterAttributesValues()` —
  the `attributeList.each {}` loop (2777–2785) appends **every** attribute to one `attributePaths`
  list and sends a single `sendToDevice(matter.readAttributes(attributePaths))` at 2789.
- Failure: cluster 0x0035 (ThreadNetworkDiagnostics) has **68 attributes**. The read is issued at
  `21:48:09.576` with all 68 paths, no reply ever arrives, and `infoCollectStateMachine` gives up at
  `21:48:26.205` with `timeout waiting for the attribute values of cluster 0035 (endpoint 0) -
  logging what was received`. The dump then prints the `[0035_FFFB] AttributeList` line and **no
  values at all**. ~17 s of a 47 s discovery run wasted, and the cluster's data is lost every time.
- Root cause: the Matter Read Request exceeds the PDU / Thread MTU (~1280 bytes) — the *same*
  limit that `refresh()` hit and that **v1.8.4 fixed by chunking reads into groups of 20**. That fix
  was applied to `refresh()` only; this path never got it.
- Fix: chunk `attributePaths` the same way `refresh()` does (20 per read), and scale the
  state-machine's per-step timeout by the number of chunks (as `setRefreshRequest()` does).
  Mind the return value: the method returns `lastAttrInt`, which the collector uses as the
  confirmation key — it must remain the last attribute of the **last** chunk.
  Consider folding in the duplicate-`FFFB` waste noted under "Also observed" below.
- Verify: run `getInfo()` on a Thread device's endpoint 0 → the `[0035_xxxx]` values are printed
  and no `timeout waiting for the attribute values` warn appears.
- Confidence: High (static + log evidence). Affects any cluster with ~40+ attributes.
- **VERIFIED on the dev hub (2026-07-25, 23:27 log, same Nuki 4.0).** An *Extended Bridge Discovery*
  now prints the complete `[0035_xxxx]` value list (all 68 attributes, through `003E`) with
  `checkStateMachineConfirmation: ... attrId:FFFD - CONFIRMED!` and **no** `timeout waiting for the
  attribute values` warn; the whole run finished in ~9 s of Matter traffic. The `FFFB` skip is visible
  too — the 8-entry 0x0046 list is read as 7 paths and the `[0046_FFFB] AttributeList` line still
  prints. Small clusters log `1 chunk(s)` and go out through `sendToDevice (String)`, i.e. unchanged.
  **Still untested:** the two `Matter_State_Machines.groovy` sites, which need a `_DiscoverAll` run
  (see the verification checklist).
  **This run alone was not enough:** a later log of a mains-powered Thread *router* showed that the
  chunking introduced an early-confirmation defect — see **B22**. The Nuki is a sleepy end device that
  answers serially, which is exactly why this run looked perfect. B22 was fixed and verified on that
  router (23:45 log), which closes B20 for the `getInfo()` path on **both** device classes.
- **Fix applied in the working copy (2026-07-25).** Scope is wider than the
  suggestion above, because grepping for the pattern found two more unchunked whole-AttributeList reads:
  1. New shared helper `sendChunkedAttributeReads(attributePaths, delayMs = 500)` next to `sendToDevice()`
     in the parent, plus `@Field static final Integer READ_CHUNK_SIZE = 20` (promoted from the local
     constant that `refresh()` had). It `collate()`s the paths, sends one `readAttributes()` per chunk
     through `sendToDevice(cmds, 500)`, records the chunk count in
     `state['stateMachines']['lastReadChunks']` and returns it. A **single** chunk is sent through
     `sendToDevice(String)`, i.e. exactly what every caller did before — so clusters with <20 attributes
     (nearly all of them) behave byte-for-byte as they did.
  2. `requestMatterClusterAttributesValues()` uses the helper. `lastAttrInt` is unchanged and still the
     confirmation key: `collate()` preserves order, so it remains the last attribute of the last chunk.
  3. `discoverGlobalElementsStateMachine()` (`Matter_State_Machines.groovy`, the
     `..._ATTRIBUTE_LIST_WAIT` branch) reads the full `<cluster>_FFFB` of an *arbitrary* cluster and had
     exactly the same exposure — now chunked. `discoverAllStateMachine()`'s
     `DISCOVER_ALL_STATE_BRIDGE_BASIC_INFO_ATTR_VALUES` (0x0028, ~25 attributes, under the limit today)
     was chunked too, for the same latent pattern. Neither needed a timeout change:
     `STATE_MACHINE_MAX_RETRIES = 50` × 330 ms = 16.5 s at scale 1 already absorbs the extra pacing.
  4. `INFO_STATE_VALUES_WAIT` in `matterUtilitiesLib.groovy` adds
     `chunkTicks = (lastReadChunks - 1) * 3` to `maxTicks` — only the info collector needed it
     (17 ticks × 300 ms = 5.1 s at scale 1). 0x0035 → 4 chunks → +9 ticks → ~7.8 s.
     `INFO_STATE_ATTR_LIST_WAIT` reads one attribute and was left alone.
  5. Folded in the duplicate-`FFFB` item from "Also observed": `requestMatterClusterAttributesValues()`
     now skips `0xFFFB` alongside the existing `0x0040`/`0x0041` skip. The dump line still prints — it
     comes from the preceding `requestMatterClusterAttributesList()` read. `FFF8`/`FFF9` were **not**
     skipped: those are the Generated/AcceptedCommandList, real data users want in the dump.
     Known wrinkle: on the `INFO_STATE_ATTR_LIST_WAIT` *timeout* path (values are attempted anyway from a
     previously stored list) the `FFFB` line will now be missing from that run's dump.
  6. `refresh()` was rewritten onto the helper; its window arithmetic is unchanged, with the chunk count
     now computed up front so that `setRefreshRequest()` still runs before the send.
  All three edited files parse-check clean; the `getInfo()` half is hub-verified (see above).

### B21. `[ ]` `VendorName` is never stored for endpoint 0 → display name renders `Bridge#NNNN Device#00 ( Smart Lock)`
- Found 2026-07-25 against **v1.9.0**, same Nuki 4.0 log as B20. One-line fix.
- Where: `Matter_Advanced_Bridge.groovy:1335` — `parseBasicInformationCluster()` (cluster 0x0028)
  stores only `['ProductName', 'NodeLabel', 'SoftwareVersionString', 'Reachable']` into
  `state[fingerprintName]`. Its bridged-device twin `parseBridgedDeviceBasic()` (0x0039) at line 1354
  stores `['VendorName', 'ProductName', 'NodeLabel', 'SoftwareVersionString', 'Reachable', 'ProductLabel']`.
- Failure: `getDeviceDisplayName()` (857–873) builds the label from
  `state[fingerprintName]?.VendorName` at 868–869 — for endpoint 0 that is always `''`, so every log
  line shows an empty vendor slot: `Bridge#7177 Device#00 ( Smart Lock)`. In the log `VendorName =
  Nuki` was received and printed at `21:47:58.062`, yet the display name was still `( Smart Lock)`
  ten seconds later at `21:48:08.051`. `ProductLabel` is likewise dropped for endpoint 0.
- Fix: add `'VendorName', 'ProductLabel'` to the list at 1335 (the surrounding
  `descMap.value != null && descMap.value != ''` guard already prevents empty writes).
- Verify: after a `getInfo()`/`_DiscoverAll` on a directly-paired Matter device, log lines read
  `Device#00 (Nuki Smart Lock)`.
- Confidence: High.
- **Fix applied in the working copy (2026-07-25), with one deviation from the suggestion above:**
  `'VendorName'` and `'ProductLabel'` were added to the list at 1335, but the **event** is still built
  only for the original four names. Reason found while applying: endpoint 00 events land on the
  **parent** device, and the parent's metadata declares `productName`, `nodeLabel`,
  `softwareVersionString` and `reachable` (lines 168–177) but **not** `vendorName`/`productLabel` —
  emitting them would produce undeclared-attribute errors on the hub. Storing them in
  `state[fingerprintName]` is all `getDeviceDisplayName()` needs. If the bridge's vendor should also be
  a visible attribute, that is a separate metadata decision (ASK USER). Parses locally.

---

## C. Minor / cosmetic / dead code

### C1. `[x]` RGBW-upgrade hint never fires: FFFB compared against 2-char strings — `[Codex C1]`
- `Matter_Advanced_Bridge.groovy:1853–1854`: `colorAttrList?.contains('00')`/`contains('01')` — the
  newParse list normalization pads to 4 chars (`'0000'`,`'0001'`), so `hasHue/hasSaturation` are always
  false and the "change the child device type manually to RGBW" logInfo (1863) never appears.
  Fix: normalize with `safeHexToInt` before comparing (0x0000/0x0001).

### C2. `[x]` Undefined variables referenced in log/description lines (log noise only — HE resolves them to null)
- Parent: `parseGeneralDiagnostics` default branch `${attrName}` (1059); `parseOnOffCluster` FFFx case
  `${attrName}` (1257); `newParseCompatibilityPatch` `${description}` (775);
  `fingerprintToData` `${rawDeviceTypeList}`/`${deviceTypeIds}` (3569); `removeAllDevices` (3538) and
  no-arg `createChildDevices` (3602) use `${getDeviceDisplayName(descMap?.endpoint)}` with no `descMap`;
  `componentOpen`/`componentClose`/`componentStopPositionChange`/`componentSetThermostatMode`
  warn-paths use `${deviceNumberPar}` (3232, 3243, 3447, 3459); `setSwitch` logTrace `${onOffCommandsList}` (2790).
- Matter_State_Machines: line 806 `${supportedClusters}`/`${ServerListCluster}` (actual locals:
  `supportedMatterClusters`/`serverListCluster`).
- Button child: default event branch `${evtIdStr}`/`${evtIdInt}` (243); attribute descriptionText
  `${attrIdStr ?: attrIdRaw}` (261). — `[Codex C4]`
- Fix: replace with the real in-scope variables. Zero behavior change.

### C3. `[x]` No-arg `createChildDevices()` is dead code (downgrade of `[Codex A3]`)
- `Matter_Advanced_Bridge.groovy:3578–3604`: grep shows **no callers** and it is not declared as a
  command; discovery uses the `createChildDevices(Map d)` overload. If ever wired up it would NPE on a
  fingerprint without `ServerList` (3587–3588) and logs the undefined `descMap` (see C2).
- **Fixed**: user chose deletion — the no-arg overload was removed; only `createChildDevices(Map d)` remains.

### C4. `[x]` Undeclared/unused locals (work via the binding, fragile under any future @CompileStatic)
- `attributeList = state[fingerprintName][listMapName]` (2239 — also the warn at 2243 says
  "attrListString"); `cmd = matter.invoke(...)` in `componentSetLevel` (3290) and
  `componentSetPosition` (3427); unused `def stock = matter.setLevel(...)` (3291 — dead, delete).
- Fix: add `String`/`List` declarations; delete the dead line.

### C5. `[x]` State maps initialized as Lists — `Matter_State_Machines.groovy:59` and `:444`
- Covered functionally by A4; even if A4 is deferred, change both `= []` to `= [:]` as hygiene.

### C6. `[x]` Periodic energy logged 1000× too small / mislabeled — `[Codex C2]`
- `Components\Matter_Custom Component_Power_Energy.groovy:334, 351` — comments say the struct energy
  is mWh; cumulative uses `/1000`→Wh and `/1000000`→kWh, but Periodic uses `/1000000` and labels it
  **Wh**. Log-only (no event is sent for periodic energy).
- **Fixed**: relabelled to kWh (local renamed `energyWh`→`energyKWh`) rather than changing the divisor —
  `/1000000` from mWh *is* kWh, so this needs no arithmetic change and cannot produce a 1000× wrong log
  if a vendor's unit differs. Cumulative Wh (`/1000`, lines 307/320) untouched.

### C7. `[x]` Copy-paste log labels in `parseColorControl` default branch
- `Matter_Advanced_Bridge.groovy:1889, 1892` — logs say "parseLevelControlCluster"/"unsupported LevelControl"
  inside parseColorControl.

### C8. `[ ]` `.hubitat\metadata.json` stale entry + duplicate ids — `[Codex C5]`
- Lines 63–74: `Components/Matter Generic Component_Energy.groovy` does not exist and shares id 3802
  with the real Power_Energy entry. Local tooling only.
- **Partial fix only**: the stale block was removed, but the 34-entry/id-unique conclusion was wrong.
  The current JSON has 35 records and 16 duplicated ids. Each duplicate is the same path written once
  as `/c:/Work/...` and once as `/c:/work/...`; id 3802 remains duplicated for the real Power/Energy file.
- Fix: keep one canonical-cased record for each file/id, then re-parse JSON and assert id/path uniqueness.
- Tooling only; HPM does not consume this file.

### C9. `[x]` Library `library()` header versions lag their `@Field` version strings
- **No longer reproducible** (verified 2026-07-25): `matterLib.groovy` header and `matterLibVersion`
  are both `'1.4.5'`; `Matter_State_Machines.groovy` has a header `'1.1.3'` and no `@Field` version
  string at all. Resolved by intervening version bumps — nothing to change.

### C10. `[x]` Window Shade `refresh()` writes a contradictory info state
- `Components\Matter_Generic_Component_Window_Shade.groovy:562` —
  `state.standardOpenClose = 'OPEN = 0% CLOSED = 100%'` but since v1.2.1 the constants are OPEN=100 /
  CLOSED=0 (lines 38–39). Misleading text only.
- **Fixed (2026-07-25):** the string had drifted to `"Hubitat standard:  'OPEN = 0% CLOSED = 100%'
  Matter standard: OPEN = 100 and CLOSED = 0"`, which is doubly wrong — Hubitat's `windowShade`
  position scale is the *same* as Matter's here (0 = closed, 100 = open), exactly as the driver's own
  `CLOSED = 0 // Hubitat standard: Closed = 0%` comment says. Replaced with
  `'OPEN = 100% CLOSED = 0% (Hubitat and Matter use the same position scale)'`.
- This is separate from the contradictory runtime initialization in B18, which is still open.

### C11. `[x]` `safeHexToInt()` returns the default for `Long` values
- `Libraries\matterCommonLib.groovy:54–65` — handles Integer and String only; a `Long` (possible for
  large UINT reports in the new parse path) silently becomes 0. No currently-known caller passes a
  Long, but it's a landmine.
- **Fixed**: added `if (val instanceof Number) return ((Number)val).intValue()` before the final
  `return defaultVal`. Placed after the String branch so hex-string handling is unaffected.

### C12. `[x]` Air Purifier: dead AQI code + attribute-enum mismatch
- `pm25Aqi()`/`lerp()` (lines 260–273) are never called (IKEA-driver leftovers).
- The declared attribute enum `airQuality: ['good','moderate','unhealthy for sensitive groups','unhealthy','hazardous']`
  (line 64) does not match the values actually sent from `AirQualityEnum` ('Unknown','Good','Fair',
  'Moderate','Poor','VeryPoor','ExtremelyPoor', capitalized; lines 799–807). HE doesn't enforce enums,
  but apps offering value pickers show the wrong list.
- **Fixed**: `pm25Aqi()`/`lerp()` deleted (grep-verified: no remaining callers); the `airQuality`
  attribute declaration now lists the seven AirQualityEnum values, and the stale enum copy in the
  `005B_0000` case comment was replaced with a pointer to AirQualityEnum.

### C13. `[x]` Battery child doesn't delegate `rtt` to `parseRttEvent()`
- `Components\Matter_Generic_Component_Battery.groovy:63–69` — unlike every other child, its parse()
  has no `rtt` branch, so ping RTT statistics (min/max/avg) are never computed; the raw event is still
  sent. Fix: add the same `if (d.name == 'rtt') { parseRttEvent(d) }` branch.

### C14. `[x]` Door Lock misc (log-only)
- `processEventMaskAttribute` (lines 1405–1421): on the new parse path `descMap.value` is a decimal
  Integer; `rawValue.take(4)` + `safeParseHex()` hex-parses a truncated decimal string → wrong decoded
  mask text in getInfo output. Fix: `Integer mask = safeNumberToInt(descMap.value)`-style handling.
- `DooorLockClusterLockType` (line 2118): typo'd name and declared `Map<String,String>` with Integer
  keys (works — generics unchecked). Rename/retype only in a dedicated cleanup pass (grep callers).
- In info mode, `processDoorLockAttributeReport` returns before the `LOCK_ATTR_STORE` block (497–511
  vs 514–517), so a `getInfo` pass does not refresh `state.lockAttr`. Minor.

### C15. `[x]` Typos (zero-risk batch)
- `pacthedNewParseMap` (parent 442–446), `'infoormation'` (getInfo command text in Power/Energy,
  Window Shade, Door Lock), `'coommand'` (Door Lock 537), `'FrontEsceutcheonRemoved'` (Door Lock 2164),
  `disoverGlobalElementsStateMachine`/`readSingeAttrStateMachine` (method names — renaming needs all
  callers + `runInMillis`/`unschedule` string references updated; treat as ASK USER),
  `'subsciption'`/`'duafter'` (header comments). Note: `descMap.sucess` (line 486) is the **platform's**
  key spelling — do not "fix".

  Implementation completed repository-wide and confirmed by successful development-hub discovery/subscription logs.

### C16. `[x]` FeatureMap hex normalization uses 2-byte minimum width — *no fix required, documented only*
- `Matter_Advanced_Bridge.groovy:779–781` — `HexUtils.integerToHexString(value, 2)` gives *at least*
  4 hex chars; a map32 FeatureMap > 0xFFFF produces longer strings. Current consumers use
  `safeHexToInt` (fine) — only flag: anything comparing FFFC strings by length/equality would break.
  No fix needed now; documented for awareness.

### C17. `[x]` Air Purifier float decode ambiguity — **CONFIRMED ON DEVICE, then fixed**
- `Components\Matter_Generic_Component_Air_Purifier.groovy:307–316` + `processUnprocessed` 407–454:
  values arrive as strings (re-parsed from `descMap.toString()`); `decodeIeee754Float()` treated any
  string without a decimal point as **hex float bits** (`parseUnsignedInt(hex,16)` + `intBitsToFloat`).
- **The bug was real, not hypothetical.** IKEA ALPSTUGA log (2026-07-25 17:30:47, dev hub bridge 8111)
  shows the parent emitting `data:[0:FLOAT:751], value:751, cluster:040D` and
  `data:[0:FLOAT:0], value:0, cluster:042A` — declared FLOAT but stringified with **no decimal point**.
  Under the old decimal-point test, `'751'` decoded as bits 0x751 → ~2.6e-42 → reported as 0 ppm.
- **Fixed** in v1.2.4: `decodeIeee754Float()` now discriminates on the bit-pattern's fixed width
  (`value ==~ /^[0-9A-Fa-f]{8}$/`) instead of looking for a decimal point, falling through to
  `safeToDouble()` otherwise. `'751'` → 751.0 ppm, `'0'` → 0.0 μg/m³, `'40A00000'` → 5.0.
  Residual ambiguity: an 8-digit all-decimal string reads as a bit pattern — documented in the
  method's javadoc and harmless, since neither CO₂ (ppm) nor PM2.5 (μg/m³) reaches 8 digits.
- **Verified end-to-end on device** (child log, 2026-07-25 17:22–17:32): `CO₂ raw: 721` →
  `CO₂ decoded: 721 ppm` → `info CO₂: 721 ppm`, with raw==decoded across ten samples
  (721/738/750/751/760/763/768/771/775/801) and `PM2.5 raw: 0` → `0 μg/m³`. Reporting thresholds
  (CO₂ 10 ppm, PM2.5 1 μg/m³) suppress sub-threshold changes as intended.
- Note: the `descMap.value instanceof Number` branches are dead (values are always Strings there).

### C18. `[x]` `minimizeByWildcard()` defeats the disabled-device subscription filter — *closed as intended*
- `Matter_Advanced_Bridge.groovy:2613–2665`: when ≥2 endpoints share a cluster+attr, paths collapse to
  `ep: -1` (wildcard) — so a disabled child's endpoint gets re-subscribed anyway (its reports are then
  dropped in `isDeviceDisabled()`, so the cost is only traffic).
- **Decision (kkossev, 2026-07-25): leave as-is.** The wildcard collapse is a deliberate trade-off for
  Matter's path-count limits, and `buildSubscriptionPathGroups()` already filters disabled children out
  before wildcarding, so only shared cluster+attr groups are affected. No code change.

### C19. `[x]` Small command-signature papercuts
- `componentSetColorTemperature` (3342–3344): `if (level != null || duration != null)` calls
  `componentSetLevel(dw, level, duration)` — with level null it just logs a warn.
- Window Shade `setLevel(BigDecimal)` (528): the SwitchLevel capability may call
  `setLevel(level, duration)` — MissingMethodException from dashboards that send a fade rate.
- **Fixed**: condition narrowed to `if (level != null)`; Window Shade signature is now
  `setLevel(BigDecimal targetPosition, BigDecimal duration = null)` (duration ignored — Matter
  position moves use the shade's own travel time).

### C20. `[x]` Attribute-name registry is incomplete for parsed clusters
- Found 2026-07-25 against **v1.9.0**.
- Where: `Libraries\matterLib.groovy:getAttributesMapByClusterId()` has no cases for 0x0003
  (Identify), 0x0202 (FanControl) or 0x040D (CarbonDioxideConcentrationMeasurement).
  `SupportedMatterClusters` also names undefined `FanControlClusterAttributes` and
  `FanControlClusterCommands` strings.
- Effect: parsing still works because the fan parser hardcodes its two attributes, but getInfo/logging
  reports `UNKNOWN` attribute names and the cluster-registration checklist is internally inconsistent.
- Fix: add the missing maps/cases (0x040D can reuse `ConcentrationMeasurementClustersAttributes`) and
  either define the FanControl command map or remove the unused registration string.
- Confidence: High; diagnostic impact only.
- **Fixed (2026-07-25):** `getAttributesMapByClusterId()` gained `0003` (the `IdentifyClusterAttributes`
  map already existed, only the case was missing), `0202` and `040D` (reusing
  `ConcentrationMeasurementClustersAttributes`, as suggested). New `FanControlClusterAttributes`
  (12 attributes, 0x0000–0x000B) and `FanControlClusterCommands` (0x0000 Step) maps were added, so
  both registration strings in `SupportedMatterClusters` now resolve.
- Checked while fixing: nothing in the package reads the `attributes:`/`commands:` strings of
  `SupportedMatterClusters` — only `subscriptions` and `eventSubscriptions` are consulted
  (`Matter_Advanced_Bridge.groovy:3121`, `3194`, `3413`). Those strings are documentation, so defining
  the maps was the consistent option rather than deleting the registration.

### C21. `[x]` Contact Sensor version history and stamp use the future year 2027
- `Components\Matter_Custom_Component_Contact_Sensor.groovy:17,24` recorded `2027-07-25` and
  `2027/07/25 8:31 PM`; the working date is 2026-07-25.
- **Fixed (2026-07-25):** both corrected to 2026. Only the *year* was touched — the version string
  `1.0.1` and the time-of-day part of the stamp are unchanged, so this is not a version bump.

### C22. `[x]` Cluster 0x0046 (ICDManagement) is unknown — logs `Cluster null (0x0046)`, attributes `UNKNOWN`
- Found 2026-07-25 against **v1.9.0**, from the Nuki 4.0 `getInfo()` log (see B20).
- Where: `Libraries\matterLib.groovy` — `MatterClusters` (the map around lines 85–100) has no
  `0x0046` entry, and `getClusterAttributesMap()` (166–190) has no `'0046'` branch.
  (`0x0046` in `matterLib.groovy:825` is `ACCoilTemperature`, an unrelated *attribute* id.)
- Failure (from the log):
  ```
  Requesting Attribute List for Cluster null (0x0046) ...
  Cluster null (0x0046) endpoint 0x00 attributes and values list (0xFFFB) :
  [0046_0000] UNKNOWN = 900
  [0046_0001] UNKNOWN = 300
  [0046_0002] UNKNOWN = 1500
  ```
- Fix: add `0x0046 : 'IcdManagement'` to `MatterClusters`, a `IcdManagementClusterAttributes` map,
  and the `if (cluster == '0046')` branch:

  | attr | name | unit | this device |
  |---|---|---|---|
  | 0x0000 | IdleModeDuration | s | 900 |
  | 0x0001 | ActiveModeDuration | ms | 300 |
  | 0x0002 | ActiveModeThreshold | ms | 1500 |

- Note — not purely cosmetic: the presence of 0x0046 means the node is a Matter **ICD**
  (Intermittently Connected Device / sleepy Thread device) with a 300 ms active window. That is the
  direct explanation for this device's 662–1402 ms round-trip times, and very likely a contributing
  factor to B20. Worth surfacing in the log rather than hiding it as `UNKNOWN`.
- Confidence: High.
- **Fixed (2026-07-25):** `0x0046 : 'IcdManagement'` added to `MatterClusters`, a new
  `IcdManagementClusterAttributes` map, and the `if (cluster == '0046')` case. The map covers the full
  spec cluster (0x0000–0x0009), not just the three attributes this device reported — the extra names
  cost nothing and other ICDs expose more. The log will now read `Cluster IcdManagement (0x0046)` with
  `IdleModeDuration = 900` etc.

### C23. `[x]` `parserFunc: NOT PROCESSED` warn storm during `getInfo()` (~90 warns per run)
- Found 2026-07-25 against **v1.9.0**, from the Nuki 4.0 `getInfo()` log (see B20).
- Where: `Matter_Advanced_Bridge.groovy:565` — `else { logWarn "parserFunc: NOT PROCESSED: ${descMap}" }`,
  reached whenever `ParsedMatterClusters[clusterInt]` is null.
- Failure: during a `getInfo()` run every attribute of every utility cluster (001F, 002E, 0030,
  0031, 0033, 0035, 003C, 003E, 003F, 0046) logs a **warn** — ~90 of them in this single log, some
  carrying multi-KB values (see C24 and the 0x003E note below). Nothing is actually lost:
  `gatherAttributesValuesInfo(descMap)` has already consumed the report at line 547, *before* the
  parser lookup at 550–551. A user reading this log reasonably concludes the driver is broken.
- Fix: demote to `logDebug` when `state.states['isInfo'] == true` (info collection is exactly the
  mode in which unparsed utility clusters are expected), or keep a `warn` only for clusters that are
  in `SupportedMatterClusters` but have no entry in `ParsedMatterClusters`. Legitimate TLV Nulls
  (see "Also observed") fold into the same fix.
- Verify: a full `getInfo()` run produces no `NOT PROCESSED` warns; a genuinely unhandled report
  outside info mode still warns.
- Confidence: High.
- **Fixed (2026-07-25):** the `else` branch at `Matter_Advanced_Bridge.groovy:564` now logs
  `logDebug` when `state.states['isInfo'] == true` and keeps `logWarn` otherwise (the first of the two
  options above — the `SupportedMatterClusters`-based variant would still have warned for the utility
  clusters that dominate the log). Guarded with `state.states != null` because `minimizeStateVariables`
  can remove it, and this runs on every single report.

### C24. `[x]` Attribute-name gaps in `matterLib` — `UNKNOWN` and `dummy` in the `getInfo()` dump
- Found 2026-07-25 against **v1.9.0**, from the Nuki 4.0 `getInfo()` log (see B20). Batch with C20
  and other name/typo fixes.
- **NetworkCommissioning 0x0031** — `NetworkCommissioningClusterAttributes`
  (`Libraries\matterLib.groovy:409–418`) stops at `0x0007 LastConnectErrorValue`, so the log shows
  `[0031_0009] UNKNOWN = 4` and `[0031_000A] UNKNOWN = 5`. Missing per spec:
  `0x0008 SupportedWiFiBands`, `0x0009 SupportedThreadFeatures`, `0x000A ThreadVersion`.
- **PowerSourceConfiguration 0x002E** — `PowerSourceConfigurationClusterAttributes`
  (`matterLib.groovy:369–371`) is `0x0000 : 'dummy'`, so the log shows `[002E_0000] dummy = [0001]`.
  The spec name is `Sources`. The same `'dummy'` placeholder sits in `DiagnosticLogsClusterAttributes`
  (0x0032) — that cluster has no attributes at all, so a comment there is clearer than a fake name.
- Fix: fill in the names above. Pure data-map edit, no logic.
- Confidence: High.
- **Fixed (2026-07-25):** `NetworkCommissioningClusterAttributes` extended with 0x0008
  `SupportedWiFiBands`, 0x0009 `SupportedThreadFeatures`, 0x000A `ThreadVersion`;
  `PowerSourceConfigurationClusterAttributes` 0x0000 renamed `dummy` → `Sources`;
  `DiagnosticLogsClusterAttributes` is now an empty map `[:]` with a comment stating the cluster
  defines no attributes (as suggested — a fake `0x0000` name was worse than nothing). Also fixed the
  `Poweer Source` typo in the adjacent comment.

### B22. `[x]` Chunked reads confirm too early — a fast node's dump loses the stragglers (regression from B20)
- Found 2026-07-25 against **v1.9.0**, in a `getInfo()` log of a **Nanoleaf NL68 Essentials Lightstrip**
  (Thread **router**, `RoutingRole = 5`, mains powered). Introduced by the B20 fix — this is a
  follow-up defect of it, not a pre-existing bug.
- Where: `matterUtilitiesLib.groovy` `INFO_STATE_VALUES_WAIT`, in combination with
  `checkStateMachineConfirmation()` (`Matter_Advanced_Bridge.groovy:770`).
- Failure: the 0x0035 dump is missing **0037, 0038, 0039, 003A, 003B and 003E**. With `FFFB` skipped
  the cluster is 67 paths → chunks `0000-0013` / `0014-0027` / `0028-003B` / `003C-FFFD`; the missing
  attributes are the tail of chunk 3 plus one of chunk 4. Confirmation is armed on the last attribute
  of the last chunk (`FFFD`), but four reads are in flight at once and the replies **interleave** —
  this device answered chunk 4 while chunk 3 was still streaming. `logRequestedClusterAttrResult()`
  then cleared `state.tmp` and set `isInfo = false`, so the late replies were dropped on the floor.
- Root cause: `collate()` preserves the *send* order, but replies to concurrent Read Requests are not
  ordered. B20's reasoning ("`lastAttrInt` is still the last attribute to arrive") holds only for a
  slow, strictly serial responder such as the sleepy Nuki that B20 was verified against.
- Fix: treat the confirmation as "the burst has started", not "the burst is finished".
- Verify: `getInfo()` on a mains-powered Thread router → the 0x0035 dump has all 68 lines, `003E` last.
- Confidence: High — the missing set matches the chunk boundaries exactly.
- **VERIFIED on the dev hub (2026-07-25, 23:45 log, the same Nanoleaf NL68).** All 68 attributes now
  print, `0037`-`003B` and `003E` included, ending with `[0035_003E] ActiveNetworkFaultsList`. The
  interleaving that caused the bug is still plainly visible in the dump order (`0020` appears between
  `000A` and `000B`) — the settle phase absorbs it instead of truncating. C25's names are live in the
  same run, and `RoutingRole = 5` (Router) confirms the device class that exposed this.
- **Fix applied in the working copy (2026-07-25):**
  1. `gatherAttributesValuesInfo()` bumps `state['stateMachines']['infoRxCount']` on the same line that
     appends to `state.tmp`, so the counter tracks exactly what the dump will contain.
  2. New `INFO_STATE_VALUES_SETTLE` (8). `INFO_STATE_VALUES_WAIT` enters it on confirmation **only when
     `chunks > 1`** — single-chunk clusters print immediately, exactly as before this whole B20 series.
  3. The settle state prints once `infoRxCount` has not moved for `INFO_SETTLE_QUIET_TICKS = 2`
     consecutive ticks (~600 ms), and is bounded by `maxTicks + (chunks-1)*3` so a device that never
     goes quiet cannot stall the run.
  4. `INFO_STATE_VALUES` snapshots the chunk count into `state['stateMachines']['infoChunks']` —
     `lastReadChunks` is shared with `refresh()` and could otherwise be overwritten mid-wait.
  Both edited files parse-check clean.

### B23. `[x]` `readAttributeSafe` validates every cluster against the **Descriptor's** attribute list
- Found 2026-08-13 against **v1.9.0** on kkossev's Aqara Hub E1, while probing endpoint `0x6B` for a
  user label. Live log:
  ```
  readAttributeSafe(endpoint:6B, cluster:0039, attribute:0005) -> starting readSingleAttrStateMachine!
  readAttributeSafe(): state[fingerprint6B]['AttributeList'] does not contain attribute 5 (0x0005) !
  valid attributes are: [0000, 0001, 0002, 0003, FFF8, FFF9, FFFB, FFFC, FFFD]
  ```
  `0x0039:0x0005` (NodeLabel) **is** supported — the same discovery run printed
  `[0039_FFFB] AttributeList = [0001, 0002, 0003, 0005, 0007, ...]`. The list it actually checked
  (`0000-0003` + globals) is the **Descriptor** cluster's list: DeviceTypeList, ServerList, ClientList,
  PartsList.
- Where: `Libraries\Matter_State_Machines.groovy` `readSingleAttrStateMachine()` case 1 —
  `List<String> attributeList = state[fingerprintName]['AttributeList']`, read unconditionally.
- Root cause: `getStateClusterName()` (`Matter_Advanced_Bridge.groovy:852-861`) stores the Descriptor
  (`001D`) attribute list under the **plain key** `AttributeList` and every other cluster under
  `NNNN_FFFB`. `requestMatterClusterAttributesValues()` (`Matter_Advanced_Bridge.groovy:2803-2808`)
  picks the key correctly; this state machine never did.
- Failure scenario: `readAttributeSafe` is usable **only** for cluster `0x001D`. For any other cluster
  it compares against the wrong list — refusing valid attributes (as above) and, for ids that happen to
  collide with `0000-0003`, permitting reads the endpoint may not support. The cluster check just above
  it uses `ServerList` and is correct, which is why the failure looks so much like a real device answer.
- Impact beyond the command: this is a diagnostic dead end that can send an investigation down the
  wrong path — here it briefly looked like the E1 had stopped exposing `NodeLabel`.
- Fix applied in the working copy (2026-08-13): choose the key the same way
  `requestMatterClusterAttributesValues()` does —
  `String attrListKey = (data.cluster == 0x001D) ? 'AttributeList' : "${...}_FFFB"` — and use it in both
  warn messages so the log names the key it actually consulted. The null-list branch now also points at
  `readAttribute` as the unvalidated escape hatch. Library parse-checks clean.
- Verify: `utilities readAttributeSafe 0x6B 0x0039 0x0005` must now perform the read and log the
  NodeLabel value; `utilities readAttributeSafe 0x6B 0x0039 0x00FF` must still be refused, quoting
  `['0039_FFFB']`; and a Descriptor read (`... 0x001D 0x0003`) must keep working unchanged.
- Confidence: High — root cause is a direct key mismatch, confirmed against both storage sites.
- **VERIFIED on the dev hub (2026-08-13, 11:50–11:51 log, Aqara Hub E1, `matterStateMachinesLib` 1.2.1).**
  All three regression cases pass:
  1. `readAttributeSafe 0x6B 0x0039 0x0005` now performs the read —
     `sendToDevice (he rattrs [{"ep":"0x6B","cluster":"0x0039","attr":"0x0005"}])` →
     `checkStateMachineConfirmation: ... CONFIRMED!` → `NodeLabel = Aqara Water Leak Sensor`.
  2. `readAttributeSafe 0x6B 0x0039 0x00FF` is still refused, and the warn now names the right key
     *and* the right list:
     `state[fingerprint6B]['0039_FFFB'] does not contain attribute 255 (0x00FF) !` /
     `valid attributes are: [0001, 0002, 0003, 0005, 0007, ... FFFB, FFFC, FFFD]`.
  3. `readAttributeSafe 0x6B 0x001D 0x0003` unchanged — Descriptor `PartsList` read, CONFIRMED,
     value `[]`.
- Incidental confirmation from case 3: endpoint `0x6B`'s `PartsList` is **empty**, so it is a leaf
  bridged node with no sub-endpoints — independent corroboration that the Aggregator/`PartsList`
  machinery in `AQARA_AGGREGATOR_LABELS_IMPLEMENTATION_PLAN.md` cannot reach this device (TODO 1.1).
- Not a defect, seen in the same log: `attribute 'nodeLabel' is not declared in driver 'Generic
  Component Water Sensor' ... the event is discarded by the platform` is
  `warnUndeclaredAttributeOnce()` working as designed — see AGENTS.md §2.3.

### B24. `[x]` Discovery stalls when a large multi-path read gets no response — NOT a bridge defect
- Found 2026-08-13 on kkossev's Aqara Hub M3 (firmware 4.5.50, Matter spec 1.5.0.0, 18 bridged
  endpoints). Every bridged endpoint failed with
  `ERROR discovering bridged device #N fingerprintNN - timeout waiting for cluster 0x39 reading results`,
  ~35 s each. Discovery produced no `0x0039` data at all.
- **Not a driver regression.** The read path was untouched by the 1.9.1 work; a reboot of the M3
  changed nothing; the failure is deterministic.
- **NOT a bridge defect either — confirmed 2026-08-13.** kkossev confirmed that Hubitat devices `5377`
  and `4383` are **the same physical M3 bridge paired to two different Hubitat hubs**. (Corroborated by
  the logs: identical bridged devices with identical serials on both, and endpoint `0x50`
  "Air Conditioner" reporting the bridge's own `PartNumber AG041` / `SerialNumber 54ef44…535f` /
  `UniqueID lumi1.54ef44…535f`.) That same bridge **answers** the 19-path read for hub `4383` on all
  seven `0x0039` endpoints in 200-400 ms, and **ignores** it for hub `5377`. The bridge is therefore
  not the variable - the Hubitat hub or the network path between them is.
  Leading hypothesis: **the large response is lost in transit, not never sent.** A 19-path `0x0039`
  reply carries ~10 strings including a 38-character ProductURL and needs IPv6 fragmentation; a path
  that drops fragments (MTU mismatch, a switch or AP filtering IPv6 fragments, VLAN encapsulation
  overhead) makes the whole reply vanish silently, which is exactly the observed symptom. Small
  chunks fit in one datagram and get through. Note this does not fully explain the 20-path `0x0028`
  read on endpoint 0 succeeding on the failing hub, so it is a hypothesis, not a conclusion.
  **Next diagnostic if it recurs:** compare the two Hubitat hubs' platform versions, and their network
  placement - Ethernet vs WiFi, VLAN, IPv6 configuration, and any switch between hub and bridge.
- **Consequence for the fix: it is the right one, and more broadly useful than first thought.** Falling
  back to smaller batches is a network-robustness mitigation that helps any user on a marginal path,
  not an Aqara-specific workaround. Do **not** describe it to users as "the Aqara M3 needs small
  reads".
- Root cause, proven with `_TRACE_ALL_MESSAGES` (see C26 — without it the driver suppresses all
  inbound tracing during discovery and the log cannot distinguish silence from a bad reply):
  **the M3 sends no response whatsoever to a 19-path read of cluster `0x0039` on a bridged endpoint.**
  Not an error status, nothing. Yet the same hub answers:
  - a **single**-path `0x0039` read on that endpoint (76 ms),
  - a **9**-path `0x001D` read on that endpoint,
  - a **20**-path chunk plus a 5-path chunk for `0x0028` on endpoint 0.

  So it is neither a global path-count limit nor a bad attribute value — a bad value would still have
  produced a partial or error response. It is specific to a large batch on a *bridged* endpoint.
- Fix (`Libraries\Matter_State_Machines.groovy`): `discoverGlobalElementsStateMachine()` now re-issues
  the identical read in chunks of `SMALL_READ_CHUNK_SIZE` (8) on timeout, before failing the endpoint.
  The send was extracted into `sendGlobalElementsValueReads(data, chunkSize)` so the first attempt and
  the retry cannot drift. `sendChunkedAttributeReads()` gained an optional `chunkSize` argument that
  defaults to `READ_CHUNK_SIZE`, so no other caller changes behaviour. A sticky
  `state.stateMachines.smallReadChunks` flag makes every later endpoint start small immediately, so
  only the first endpoint of a misbehaving bridge ever pays the timeout.
- The B22 interleaving hazard does not bite here: `parseBridgedDeviceBasic()` writes values into
  `state[fingerprintName]` independently of `state.tmp`, which only feeds the pretty log dump. A
  premature confirmation can truncate a log line, not lose fingerprint data.
- **VERIFIED on the dev hub (2026-08-13, 15:15 log).** Endpoint `0x2E` timed out once, logged
  `no response to the full attribute read of cluster 0x0039 on endpoint 0x2E; retrying in 3 chunks of 8`,
  and every value arrived (`VendorName`, `ProductName`, `NodeLabel = Scene test`, `Reachable`,
  `UniqueID`, `SerialNumber`, `ProductLabel`). Endpoint `0x29` then went **straight** to 3 chunks of 8
  with no timeout and finished at `retry=1`, confirming the sticky flag.
- **Fallback correctly stays dormant (2026-08-13).** A second Aqara M3 ("Aqara M3 CN", Hubitat device
  `4383`) **answered the identical 19-path `0x0039` read with no retry** on endpoints `0x59` and `0x50`
  - values began arriving ~450 ms after the send, and no `retrying in ... chunks` warning appears
  anywhere in the run. Discovery completed: 18 endpoints, 44 subscriptions. So the fix costs nothing
  on bridges that do not need it.
- **Loose end:** the two runs report `RebootCount` 37 vs 200 and `UpTime` 104 days vs 15 minutes for
  what is one physical bridge. GeneralDiagnostics `0x0033` is not fabric-scoped, so those should
  match. Either the M3 keeps per-fabric diagnostics counters (non-conformant), or one hub was reading
  a cached value. Not worth chasing unless something else depends on `UpTime`/`RebootCount`.
- Verify elsewhere: a full `_DiscoverAll` on a bridge that never needed this (E1, Hue) must still send
  one full-size request per cluster and show no `retrying in ... chunks` warning.
- **Re-examined 2026-08-13 against an external (ChatGPT) analysis** that recommended
  `MAX_READ_PATHS_PER_REQUEST = 9`, on the grounds that 9 is "the maximum you can safely rely on".
  **That recommendation is refuted and was not implemented.** 9 is the Matter spec *floor* — the
  minimum a conformant server must support (`kMinSupportedPathsPerReadRequest` in connectedhomeip) —
  not a ceiling. The evidence above contradicts the premise twice over: this same M3 answered a
  **20-path** `0x0028` read on endpoint 0, and the **same physical bridge on a different Hubitat hub**
  answered the 19-path `0x0039` read on all seven endpoints in 200-400 ms. There is no 9-path M3
  limit; lowering `READ_CHUNK_SIZE` globally would slow every read on every bridge without addressing
  the root cause.
  Two facts from that analysis *were* verified and are now recorded in the code comments:
  (a) `SMALL_READ_CHUNK_SIZE = 8` sits below the spec floor of 9, so a conformant server can never
  refuse it for being too big — a justification the constant previously lacked; (b) the real limit
  **cannot** be discovered at runtime (`CapabilityMinima` reports only CASE sessions and subscriptions
  per fabric; `MaxPathsPerInvoke` `0x0016` applies to Invoke, not Read), which is why
  fallback-on-timeout is the only viable strategy. Do not re-litigate this without new hardware evidence.
- Confidence: High — root cause reproduced and the fix confirmed on hardware.

### B25. `[x]` Multi-path reads that bypassed the chunking safety net
- Found 2026-08-13 while verifying B24. `sendChunkedAttributeReads()` was the only chunk-aware read
  path, and three call sites did not use it or did not benefit from it:
  1. **`Matter_State_Machines.groovy` `DISCOVER_ALL_STATE_SUPPORTED_CLUSTERS_*`** sent 4 global paths
     (`FFFB`/`FFF8`/`FFF9`/`FFFC`) × every matched cluster in **one unchunked** `matter.readAttributes()`
     — 24+ paths on a 6-cluster endpoint, past the point where the M3 stops answering — with no
     small-chunk fallback. On timeout it proceeded with incomplete fingerprint data.
  2. **`state.stateMachines.smallReadChunks` was wiped** by `state.stateMachines = [:]` at the start of
     every discovery, so a bridge that had *proved* it needs small reads got full-size reads again on
     the next run, and `refresh()` / `componentRefresh()` never consulted the flag at all.
  3. **`componentRefresh()`** sent one path per subscription of an endpoint, unchunked, with a fixed
     6 s window.
- Fix: the quirk flag moved to `state.states.smallReadChunks` (survives discovery resets, cleared only
  by `resetStats()` via `initializeVars(fullInit = true)`), and `effectiveReadChunkSize()` /
  `latchSmallReadChunks()` were added next to the constants in the parent. `sendChunkedAttributeReads()`
  now applies `effectiveReadChunkSize()` whenever no explicit `chunkSize` is passed, which makes
  `refresh()`, `componentRefresh()`, `requestMatterClusterAttributesValues()` and the BasicInfo read
  adaptive in one place. Callers that compute a chunk count for their own timeout window
  (`refresh()`, `componentRefresh()`) use the same helper, or the window under-sizes once the quirk latches.
- The discovery cluster-globals read is now chunked, widens its wait budget by `(chunks - 1) * 3` ticks
  (the allowance `infoCollectStateMachine` already makes), and on timeout **re-issues only the missing
  paths** in chunks of 8 before failing. That retry is exact because `clusterDataExpected` is a true
  per-path ledger, unlike the single-attribute tripwire used elsewhere.
- **VERIFIED on hardware 2026-08-13 (17:20 log, device `5377` — the same hub that reproduced B24),
  with `_TRACE_ALL_MESSAGES` enabled** so absence of a reply is distinguishable from a filtered trace:
  - the chunked path is live — `st:26 - waiting for 4 cluster attributes from endpoint 0050
    (1 chunk(s))` carries the new chunk count, followed by the matching 4-path
    `he rattrs [...0xFFFB, 0xFFF8, 0xFFF9, 0xFFFC]` and `st:29 - all 4 cluster attributes received!`;
  - **no regression** — `st:26 - all parts discovered (total #18) !`, `the number of subscriptions is
    44`, and `*** END of the Matter Bridge and Devices discovery ***`. Both totals match the B24
    baseline exactly;
  - **no** `timeout waiting for cluster data! Missing: [...]` and **no** `retrying in ... chunks`
    anywhere in the run;
  - subscriptions work afterwards: thermostat, FP2 presence, button and battery reports all flow from
    17:20:57 onward, and a bridge ping at 17:30:38 returns in 78 ms.
- **What that run did NOT exercise**, so it stays unproven rather than disproven:
  - the **multi-chunk** cluster-globals read — every endpoint on this bridge matched few enough
    clusters to fit one chunk of 20, so `(chunks - 1) * 3` tick widening never applied;
  - the **missing-path retry** — nothing timed out, so `clusterDataRetried` never fired and the exact
    re-request of the missing ledger entries is still only parse-checked;
  - `refresh()` / `componentRefresh()` at chunk size 8 — the quirk never latched this run, so the
    adaptive size was 20 throughout.
  These are all *fallback* paths that fire only on failure; the primary risk this item carried was a
  regression in the normal path, and that is now excluded on the previously-failing hub.
- Confidence: High for the chunking and the no-regression claim (reproduced on the hub that used to
  fail). Medium for the timeout-retry fallback, which awaits a bridge that actually stops answering.

### B26. `[ ]` Component wildcard reads bypass chunking entirely — logged, not fixed
- Found 2026-08-13 during the B25 audit. Twelve component call sites read a whole cluster with
  `parent?.readAttribute(endpoint, cluster, -1)`, which reaches
  `matter.attributePath(endpoint, cluster, -1)` unchanged — the driver has no handling for `-1`, so it
  relies on the platform encoding it as a wildcard AttributeId.
- A wildcard is **one request path but an unbounded response**, so `READ_CHUNK_SIZE` cannot limit it.
  If B24's leading hypothesis is right — that the binding constraint is response size on a marginal
  network path, not request path count — these are exactly the reads that would fail, and they have no
  fallback at all.
- Sites: `Matter_Generic_Component_Air_Purifier.groovy:870-910` (**eight** back-to-back wildcard reads in
  one `refresh`, the worst case), `Matter_Generic_Component_Door_Lock` (`0x0101`),
  `Matter_Custom Component_Power_Energy.groovy` (`0x0090`, `0x0091`),
  `Matter_Generic_Component_Window_Shade.groovy`, `Matter_Generic_Component_Button.groovy`,
  `Matter_Generic_Component_Camera_AV_Stream.groovy`.
- **No evidence yet that any of these actually fail** — logged for tracking, deliberately not fixed.
  Converting a wildcard into concrete chunked paths (using the cached `NNNN_FFFB` AttributeList where
  one exists) is the obvious mitigation, but it is a larger change and should wait for a real report.
- Related: `requestAndCollectServerListAttributesList()` issues one read per cluster in a tight loop
  with no pacing — a different flavour of the same risk (concurrent in-flight reads rather than paths
  per read). Also unmeasured.

### C25. `[x]` Diagnostics clusters have no attribute-name maps — values print as `UNKNOWN` (0x0035, 0x0036, 0x0004, 0x0005)
- Found 2026-07-25 against **v1.9.0**, in the 23:27 log that verified B20. Direct consequence of that
  fix: now that the cluster's values actually arrive, the dump is 68 unlabelled numbers —
  `[0035_0000] UNKNOWN = 25`, `[0035_0007] UNKNOWN = [[[tag:3, value:85756], ...`, and so on.
- Where: `Libraries/matterLib.groovy` — `0x0035 : 'ThreadNetworkDiagnostics'` was in `MatterClusters`,
  but there was no attributes map and no `getAttributesMapByClusterId()` branch, so
  `getAttributeName()` fell through to `UNKNOWN`. Same shape of gap as C20/C22/C24.
- Fix: add `ThreadNetworkDiagnosticsClusterAttributes` (0x0000-0x003E, Core spec 11.13.6) and
  `if (cluster == '0035') { return ThreadNetworkDiagnosticsClusterAttributes }`. Pure data-map edit.
- Confidence: High — every name was cross-checked against the values in that log, and they agree:
  `0000 Channel = 25`, `0001 RoutingRole = 2` (SleepyEndDevice, which matches the ICD cluster found in
  C22), `0002 NetworkName = MyHome61`, `0007 NeighborTable` a 14-field struct, `0008 RouteTable` a
  10-field struct, `003B SecurityPolicy = [0:672, 1:143]`, `003D OperationalDatasetComponents` 12 bools.
- **Fixed and VERIFIED (2026-07-25, 23:45 log):** map + dispatch branch added; matterLib passes the
  full static-compile check, and all 68 0x0035 attributes print with their real names on the hub
  (`Channel`, `RoutingRole`, `NeighborTable`, `RxErrSecCount`, `SecurityPolicy`, ...).
- **Extended the same day** after a `getInfo()` log of a **Meross MS600** (Wi-Fi Root Node, 23:33)
  showed two more unnamed clusters:
  - **0x0036 WiFiNetworkDiagnostics** — new `WiFiNetworkDiagnosticsClusterAttributes` (0x0000-0x000C,
    Core spec 11.14.6) + dispatch branch. Cross-checked against that log: `0000 BSSID = KNEnTI8S`
    (6 base64 bytes), `0002 WiFiVersion = 3` (n), `0003 ChannelNumber = 1`, `0004 RSSI = -58` dBm.
  - **0x0004 Groups** — `[0004_0000] UNKNOWN = 128` was **not** a missing map: `GroupsClusterAttributes`
    already existed with the right name (`NameSupport`, 128 = bit 7 "group names supported"), it simply
    had no `if (cluster == '0004')` branch. Added. An audit of every `*ClusterAttributes` field against
    the `return` branches found exactly one more orphan, `ScenesClusterAttributes` (0x0005) — dispatched
    too. There are now **no** defined-but-unreachable attribute maps left in matterLib.

---

## Also observed — deliberately *not* filed as bugs

From the Nuki 4.0 `getInfo()` log of 2026-07-25 (see B20):

- **Duplicate `FFFB` read per cluster.** The collector reads `0xFFFB` alone, then re-reads the whole
  list *including* `0xFFFB`, producing eight `gatherAttributesValuesInfo: ... is already in the
  state.tmp` debug lines per run and one wasted path per request. Excluding `FFFB` (and arguably the
  echo-only `FFF8`/`FFF9`) from the second read would shorten every request — worth folding into B20.
  **Done (2026-07-25) as part of the B20 fix** — `FFFB` only; `FFF8`/`FFF9` are kept, they carry the
  command lists and belong in the dump.
- **0x003E dumps ~3.5 KB of credentials at `info` level.** `NOCs` and `TrustedRootCertificates` are
  printed in full. These are *public* operational certificates, not private keys, but they identify
  the user's fabrics and they dominate the log — users paste these on the forum. Truncating to a
  summary (fabric count / index) would be better.
- **Empty-string values are silently dropped from the dump.** `gatherAttributesValuesInfo()` at
  `Matter_Advanced_Bridge.groovy:1007` requires `descMap.value != ''`, so `[0028_0005] NodeLabel`
  never appears. There the lock genuinely *has* no NodeLabel — which is precisely the blind spot
  behind [TODO.md](TODO.md) item 1.1. Printing `(empty)` would make it visible.
- **Legitimate TLV Nulls logged as if broken.** `003C_0001`, `003C_0002` (commissioning window
  closed) and `0031_0007` (no connect error) arrive as `NULL:null` and produce
  `newParseCompatibilityPatch: descMap.attrId is null or descMap.value is null` followed by a
  `NOT PROCESSED` warn. Null is a valid Matter value here, not a parse failure. Folds into C23.
- **`RebootCount = 170`** on the Nuki. With a 13-day uptime this is historical, not current
  instability — device-side, nothing for the driver to do.
- **`parseDescriptorCluster: Bridge partsList: [0001]`** — the wording says "Bridge" for what is a
  directly-paired single device. Cosmetic, and an artifact of running MAB on a non-bridge node.

---

### C26. `[x]` `_DiscoverAll` suppresses ALL inbound message tracing, making discovery failures undiagnosable
- Where: `Matter_Advanced_Bridge.groovy:523` in `processParsedDescription()`:
  ```groovy
  if (!(((descMap.attrId in ['FFF8','FFF9','FFFA','FFFC','FFFD','00FE']) && DO_NOT_TRACE_FFFX) || state['states']['isDiscovery'] == true)) {
      logDebug "parse: descMap:${descMap}"
  }
  ```
  The `|| isDiscovery == true` arm drops the trace for **every** message while a discovery is running,
  not just the FFFx globals the first arm targets.
- Impact: when discovery stalls, the log cannot distinguish *nothing arrived* from *something arrived
  that was not a matching attribute report* — an error/status response, a wrong endpoint, a decode
  failure. Found 2026-08-13 while investigating the Aqara M3 stall (see TODO): the 19-path `0x0039`
  read on a bridged endpoint produced no `parseBridgedDeviceBasic:` lines and no way to tell why.
- Note `checkStateMachineConfirmation()` runs at `:516`, **before** both this trace suppression and
  the `isDeviceDisabled()` early return at `:518`. So a confirmation seen in the log is trustworthy
  even for a disabled child — useful when reasoning about what did arrive.
- Suggested fix: gate the discovery-time suppression on `traceEnable` rather than removing it, or
  keep suppressing only the FFFx globals during discovery. The intent was noise reduction, not
  blindness — a `_DiscoverAll` on an 18-endpoint bridge is exactly when the trace is most wanted.
- Workaround meanwhile: probe with `utilities readAttribute` / `readAttributeSafe` **outside**
  discovery, where `isDiscovery` is false and the trace prints.
- **FIXED 2026-08-13** (kkossev's suggestion): new `@Field static final Boolean _TRACE_ALL_MESSAGES`
  beside `DO_NOT_TRACE_FFFX`. When TRUE it bypasses **both** suppressions and logs every parsed
  message. Default FALSE; also needs `Enable debug logging`, since `logDebug` is gated by it. The
  existing behaviour is untouched when the flag is FALSE, so this is a diagnostic escape hatch, not a
  change of default logging.
- Confidence: High (read directly from the source). Severity: diagnosability only, no runtime effect.

## D. Packaging / release blockers

### D1. `[x]` The v1.9.0 beta bundle omits the Camera AV Stream child driver
- `Matter_Advanced_Bridge.groovy` registers parser 0x0551 and maps matching endpoints to
  `Matter Generic Component Camera AV Stream`, but `MatterAdvancedBridge_BETA.zip` contains neither
  `kkossev.MatterGenericComponentCameraAVStream.groovy` nor an install/update manifest entry for it.
- Effect: a 0x0551 endpoint cannot create its required custom child when installed from the beta bundle.
- Fix: add `Components\Matter_Generic_Component_Camera_AV_Stream.groovy` under the correct bundle name
  and add matching `install.txt`/`update.txt` entries; verify install and upgrade on a clean test hub.
- Confidence: High; ZIP contents were inspected directly.
- **Fixed (2026-07-25):** `MatterAdvancedBridge_BETA.zip` rebuilt with a 20th entry,
  `kkossev.MatterGenericComponentCameraAVStream.groovy` (31 403 bytes, byte-identical to the loose
  source including the B16 fix), placed after the other components and before the two manifests.
  `driver kkossev.MatterGenericComponentCameraAVStream.groovy` appended to **both** `install.txt` and
  `update.txt` (876 → 935 bytes each), preserving their LF-only line endings and no-trailing-newline
  format. The bundle now carries all 12 components.
- **Scope kept deliberately narrow — this is D1 only, not D2.** All 18 pre-existing entries were
  re-verified SHA-256-identical to the pre-edit archive, so the 11 bundled files that still differ
  from their loose sources were **not** synced and `_DEBUG` was **not** flipped; those remain D2's job
  at the release point. `packageManifest.json` and the production `MatterAdvancedBridge.zip` were not
  touched (they are v1.8.8, per the ground rules).
- Cross-checked after rebuilding: every `library`/`driver` line in the manifests resolves to a real ZIP
  entry, and every ZIP entry except the two `.txt` files is named in the manifests — no orphans in
  either direction. The old archive is recoverable from git (the ZIP is tracked).
- **Still needs the hub test from the original entry:** clean HPM install *and* upgrade from the beta
  bundle. Nothing about a 0x0551 camera endpoint creating its child has been exercised — no camera
  device is available (same limitation as B16).

### D2. `[?]` v1.9.0 release artifacts are not synchronized with the current loose sources
- `MatterAdvancedBridge_BETA.zip` contains parent v1.9.0 timeStamp `2026/07/25 7:01 PM`, while the loose
  parent is `2026/07/25 8:57 PM`; 11 bundled source entries differ from their current loose files.
- The production `packageManifest.json` and `MatterAdvancedBridge.zip` intentionally remain at v1.8.8
  until release. This is not a defect in the stable 1.8.8 package, but it is a hard release gate for 1.9.0.
- The loose parent also has `_DEBUG=true`; it must be false in the released source and rebuilt bundle.
- Verify at release: versions/timestamps/history, `_DEBUG=false`, manifest/date/notes, exact UTF-8
  loose-to-ZIP content equality, all 12 components present, and clean HPM install/update.

---

## Session log — 2026-07-25 (historical, superseded by the index above)

> Kept as the record of what was applied and verified in that session. **Do not read this as current
> status** — several items named here have since changed state. The index at the top of this file is
> authoritative.

**Most items from the original v1.8.8 review remain closed; the current source audit reopened C8 and
C10 because their documented fixes are not present/complete. B13 is fixed and device-verified.**

**The current v1.9.0 audit has open items B12, B15, B17-B19 plus the tooling item C8. B20 + B22 are
fixed and verified on both device classes (sleepy end device and mains-powered router); only B20's two
`_DiscoverAll` state-machine sites remain untested. C25 is verified in the same run. Every C item
except C8 is closed, and D1 is closed. D2 remains the release gate rather than a defect in the
stable v1.8.8 package — and note that D1's rebuild deliberately did *not* address any part of it.**

**A second batch — C20, C22, C23, C24 — was applied on 2026-07-25.** Three are pure data edits in
`Libraries\matterLib.groovy` (cluster/attribute name maps) and C23 is a log-level change in the parent.
All four are marked `[x]`: nothing here alters parsing or event delivery, only the names and levels the
driver logs. `matterLib.groovy` passes a **full** static compile locally (not just the parse check —
`getAttributesMapByClusterId()` is `@CompileStatic`, so the new cases are type-checked). No version
strings were bumped. ⚠️ `matterLib.groovy` is a **library**: paste it to the hub *before* the parent.

**A five-item mechanical pass was applied on 2026-07-25** (at kkossev's request, overriding the
one-bug-at-a-time ground rule): **B14** (`!isDiscoveryActive` in the duplicate filter), **B16**
(camera FFFC/FFFD hex parsing), **B21** (endpoint-0 `VendorName`/`ProductLabel` stored),
**C10** (Window Shade info string) and **C21** (Contact Sensor 2027→2026 dates). All four touched
files pass the local Groovy parse check. **C10 and C21 are closed** (text-only, nothing to test).
**B14, B16 and B21 remain `[ ]`** — they change runtime behavior and need the dev hub; B16 in
particular has no camera device available to test against. No `version()`/`timeStamp()`/`@Field`
version strings were bumped. Files touched: `Matter_Advanced_Bridge.groovy`,
`Components\Matter_Generic_Component_Camera_AV_Stream.groovy`,
`Components\Matter_Generic_Component_Window_Shade.groovy`,
`Components\Matter_Custom_Component_Contact_Sensor.groovy`.
Note this pass adds to the hub-verification queue that already holds B12's staged fix.

**B20/B21 and C22-C24 were merged in from the `BUGS2.md` staging file** (Nuki Smart Lock 4.0
`getInfo()` log, 2026-07-25) and renumbered from their original `N1…N5` ids: N1→B20, N4→B21,
N2→C22, N3→C23, N5→C24. That run covered **endpoint 0 only** — the lock endpoint 0x01
(DoorLock 0x0101) is not in the log, so nothing there was reviewed. `BUGS2.md` has been deleted.

**B12 was added later the same day** from a user log analysis against v1.9.0 — the `sendHubitatEvent()`
duplicate filter reads the parent's state instead of the child's. The one-line fix **is applied in the
working copy** and passes the local Groovy parse check, but it changes event-delivery behavior
(repeated identical reports now stop at the parent), so it stays `[ ]` until confirmed on the hub.
**B13 was added and closed the same day**, from a user log (`requestExtendedInfo(): serverList is null!`)
and then verified on the dev hub against the Zemismart M1 — see the entry. It touches `matterUtilitiesLib`
and the parent, so the library must be pasted to the hub first.
Applied in the same session, from the same log, and **not** tracked as bugs here because they are
log/cosmetic only: the `initialize()` warning printed the subscribe timer under an "unsubscribe" label;
`thermostatSetpoint` reused the `heatingSetpoint` descriptionText (duplicate-looking log line);
cooling-only thermostats never got a `thermostatSetpoint`; the thermostat setpoint-limit events had no
device name in their descriptionText; and the setpoint limits / `supportedThermostatModes` /
`numberOfButtons` are now skipped when unchanged (new helper `isUnchangedStaticAttribute()`).

The 2026-07-25 batch closed C3, C6, C9, C11, C12, C16, C18 and C19 in one pass (at kkossev's
explicit request, overriding the one-bug-at-a-time ground rule). It also marked C8 closed, but the
current duplicate-path audit reopened it. B1, B4 and C9 were found already
fixed by earlier work and were only re-verified against the current source. C17 was confirmed as a
genuine bug by an ALPSTUGA log the same day and is fixed in Air Purifier v1.2.4. Version strings,
`packageManifest.json` and the zip were **not** touched, per the ground rules.

**Device-verified on the dev hub (2026-07-25, IKEA ALPSTUGA on bridge 8111):** C17 end-to-end
(see the entry), C12 (child emits `Good`, a declared AirQualityEnum value), and B4 (cluster 040D
routed to the Air Quality Sensor child, dni `M3125-01`).

**Still needs a hub test** — the following changes are verified only by inspection:
- Window Shade `setLevel` from a dashboard **with a fade rate** (C19).
- `_DiscoverAll` end-to-end (C3 deleted a parent method; grep shows no callers).
- Power/Energy: cumulative `energy` event now reports kWh via `/1000000` where it previously reported
  Wh via `/1000` — check a real meter's magnitude and that history has no 1000× step (C6 + later edit).
- Any Matter button: single press → `pushed`, double → `doubleTapped`. B1 was closed on static
  reading; BUGS.md originally called a device test mandatory and it has not been done.

## Suggested fix order (advisory — the user picks the actual order)

- **Group 1 — safe mechanical guards/typos** (compile-and-go, no behavior risk):
  A1, A2, A3, A4/C5 (`[]`→`[:]`), B6, B7, B9, B11, C2, C4, C7, C10, C13, C15 (excl. method renames).
- **Group 2 — logic fixes needing care / a quick device check**:
  B1 (button FeatureMap — test with a real Matter button), B2 (fan speed), B3 (setColor level),
  B4 + B5 (mapMatterCategory routing — mind branch order), C1, C11, C12, C14, C19.
- **Group 3 — gated on the user's decision or device logs**:
  B8 (SwitchBot child fate), B10 (lock 'closed' seed), C3 (delete vs keep dead method), C6 (energy
  unit), C17 (float decode), C18 (wildcard vs disabled devices), C15 method renames.
- **Current v1.9.0 audit:** all the mechanical items are done — C10, C20, C21, C22, C23, C24, plus
  B14, B16, B20/B22, B21 and C25 (see the status notes above); B20+B22+C25 are hub-verified. What
  remains is device/design work: test B12/B14/B16/B17/B18/B19/B21 on the dev hub plus B20's
  `_DiscoverAll` half, decide B15, and complete D2 at the release point.
  D1 is done (the beta bundle now has all 12 components) but still owes a clean HPM install/upgrade
  test. C8 is local tooling.
- **Merged Nuki-log items:** B21 and C22/C24 were one-line/data-map edits (done with the C20 batch);
  C23 was a log-level change; B20 was the only one needing real work (chunked reads + timeout scaling)
  and it is now staged, awaiting a Thread-device `getInfo()` test.
  C8 is local tooling cleanup. B12 remains first in the existing hub-verification queue because its fix
  is already staged.

---

## Appendix: the merged Codex review (2026-07-04)

`BUGS_CODEX.md` was an **independent** defect analysis run by Codex before this file existed. It was
merged here and deleted on 2026-08-13, so `[Codex Xn]` references throughout this document resolve
against the table below rather than against a second file.

**Provenance of that review:** generated 2026-07-04 against parent `Matter_Advanced_Bridge.groovy`
v1.8.8, timestamp `2026/05/29 07:01 AM`. Its line numbers referred to that snapshot and are already
stale — always grep the quoted code before acting on any item.

**Its per-item text was deliberately not copied.** Every one of the 13 findings was independently
re-verified when this file was written, and each entry here is strictly richer than the Codex original:
more precise locations, the fix actually applied, and a confirmed status. Two findings changed severity
on re-verification, which is the main reason the IDs do not line up.

| Codex | → this file | Status | Note |
|---|---|---|---|
| A1 | A1 | `[x]` | NPE, endpoint with no supported clusters |
| A2 | A2 | `[x]` | NPE, missing `NNNN_FFFB` AttributeList |
| A3 | C3 | `[x]` | **Downgraded** — not a crash; the no-arg `createChildDevices()` is dead code |
| B1 | B2 | `[x]` | Air Purifier `setSpeed`/`cycleSpeed` Integer→String API |
| B2 | B1 | `[x]` | Button FeatureMap parsed as decimal — **worse than Codex reported**: `pushed` is lost too, not only `doubleTapped` |
| B3 | B4 | `[x]` | CO₂-only endpoints (0x040D) never mapped to a child |
| B4 | B5 | `[x]` | Energy-only endpoints (0x0091) never mapped to Power/Energy |
| B5 | B3 | `[x]` | `componentSetColor()` computes level but never applies it |
| C1 | C1 | `[x]` | RGBW-upgrade hint compares FFFB against 2-char strings |
| C2 | C6 | `[x]` | Periodic energy logged 1000× too small / mislabeled |
| C3 | B9 | `[x]` | **Upgraded** to severity B — humidity of exactly 0 % rejected as invalid |
| C4 | C2 | `[x]` | Undefined variables in log/description lines (noise only) |
| C5 | C8 | `[ ]` | `.hubitat\metadata.json` stale entry + duplicate ids — **the only Codex finding still open** |

### Its "Verified Non-Issues", re-checked 2026-08-13

- *"`packageManifest.json` version 1.8.8 matches the parent `version()`."* — **no longer true.** The
  parent is 1.9.1 and the manifest still says 1.8.8. That gap is deliberate for a BETA and is tracked
  by the D-series release gates, not by this appendix.
- *"The release bundle comparison must use UTF-8; with correct decoding the zipped parent matches the
  loose parent."* — still true, and now recorded where it gets used: `AGENTS.md` §1 and D1/D2.
- The third bullet was a note about an unrelated file elsewhere on the author's disk. Dropped as
  local-machine trivia.

## Verification checklist for fixers

- Parent + changed libraries + changed components must compile on the dev hub (libraries first!).
- After A1/A2: run `_DiscoverAll` on the dev bridge — discovery completes, "number of subscriptions"
  info event appears, no NPE in logs.
- After B1: physical single/double press on a Matter button → `pushed`/`doubleTapped` events.
- After B2: `setSpeed('medium')` → Matter write in debug logs and a `speed` report back.
- After B14: `_DiscoverAll` must emit one unchanged value per attribute and suppress only burst repeats.
- After B15: run safe reads with fingerprints present and minimized, plus one invalid attribute.
- After B16: camera FFFC/FFFD test values must decode as hexadecimal.
- After B17: unrelated traffic must not cancel a deliberately unanswered ping timeout.
- After B20/B22: `getInfo()` on a **mains-powered Thread router** (not only a sleepy end device) —
  the 0x0035 dump must contain all 68 lines. A sleepy node answers serially and hides B22 entirely.
- After B20: also run `_DiscoverAll` — that is what exercises the two chunked state-machine reads
  (`discoverGlobalElementsStateMachine`, `discoverAllStateMachine`) and `refresh()` on a bridge with
  more than 20 subscriptions, which was rewritten onto the shared helper.
- After B18/B19: exercise component initialization/preference changes before and after first reports.
- After B20: `getInfo()` on a Thread device's endpoint 0 must print the `[0035_xxxx]` values with no
  `timeout waiting for the attribute values` warn.
- After B21: log lines for endpoint 0 must read `Device#00 (Nuki Smart Lock)`, not `( Smart Lock)`.
- After C20/C22/C23/C24 (**paste `matterLib.groovy` before the parent**): a `getInfo()` dump shows
  `Cluster IcdManagement (0x0046)` with named attributes, no `UNKNOWN`/`dummy` for 0x0031/0x002E, real
  names for 0x0003/0x0202/0x040D, and no `NOT PROCESSED` warns — while an unhandled report *outside*
  info mode must still warn (check by triggering any normal report from an unsupported cluster).
- After B23 (**paste `Matter_State_Machines.groovy` as a Library first**): `utilities readAttributeSafe
  0x6B 0x0039 0x0005` performs the read instead of refusing it; a bogus attribute on the same cluster is
  still refused and the warn quotes `['0039_FFFB']`; a `0x001D` read is unchanged.
- After D1/D2: compare every ZIP entry to loose UTF-8 source and test clean HPM install/update.
- This is the only bug list — `BUGS_CODEX.md` no longer exists. Update this file's checkboxes.
