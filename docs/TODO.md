# Open User Requests — Matter Advanced Bridge

Improvement requests harvested from the community thread
([RELEASE] Matter Advanced Bridge (limited device support), topic **135252**, 436 posts) on
2026-07-25. **Only posts #418 → #439 were analyzed** (the range the user asked for — #418 is the
v1.8.8 release announcement, #439 is the last post; #437 was deleted). Posts #1–#417 have not
been mined yet, so this list is *not* a complete harvest of the thread.
Post links are `https://community.hubitat.com/t/-/135252/<post#>`.

> **This file is published.** It is the answer to "has this already been asked for?" — it records
> which community requests have been analysed, which were implemented, which are blocked and why, and
> which were deliberately declined. If you are about to raise a request, search here first.
>
> Community members are credited by their forum handle with a link to the originating post, exactly as
> the public thread shows them. If you would rather not be named here, say so in the thread and it
> will be removed.
>
> Device identifiers appearing as evidence (serial numbers, unique IDs) are the maintainer's own
> hardware and are **partially masked** — enough to keep the point being made, not enough to identify
> a unit. Some entries cite `docs/maintainer/**` working documents, the `plans/` design notes, or
> files under `Tests/`; those are **maintainer-local and deliberately not published**, so those names
> will not resolve in a clone. No item here depends on them.

This list complements [BUGS.md](BUGS.md) (reviewed defects; tracked and published since 2026-08-13).
Items here are **feature requests and unresolved user reports**, not reviewed bugs — each needs
its own analysis before implementation. Same ground rules as BUGS.md: one item at a time, no
`version()`/`timeStamp()` bumps until the user says so, mark `[x]` only after the user confirms a
hub test.

Verified against **v1.9.0** (`2026/07/25 5:17 PM`). Line numbers refer to that revision.

**Section 7 has a different source.** It is not from the thread: it holds the `TODO:` comments that
used to sit in the parent driver's header block, moved here on 2026-08-13 and re-verified against
**v1.9.1** at that date. Line numbers in section 7 refer to v1.9.1.

**Section 9 has a different source thread.** *Should Shelly Gen4 plug report power EVERY second?*
(topic **161449**, 20 posts, fully analyzed 2026-08-16) — separate from the main release thread
135252. Verified against **v1.9.2**.

---

## 1. Child device naming / labels

### 1.1 `[x]` Import user-set labels from aggregator-style bridges (Aqara G3 wall switches)
> **CLOSED 2026-08-13 — the "import the Aqara app name" half is NOT IMPLEMENTABLE** (three-controller
> evidence below). The *second* half of the original report — multi-gang switches landing as generic
> `Switch` children — is real, reproducible, and still actionable; it is carved out under
> **"Remaining actionable work"** at the end of this item. Item numbering is unchanged because
> AGENTS.md and item 2.1 both reference `1.1`.

@redpaw has several Aqara wall switches, all labelled in the Aqara app, but every one of them
lands in Hubitat as plain `Switch`. kkossev acknowledged in-thread that this "has been on my TODO
list for quite a long time" — multi-gang devices are exposed as an `Aggregator` with a nested
`PartsList`, and the user-set names live somewhere other than the functional endpoint's
`NodeLabel`.
- Posts: [#432](https://community.hubitat.com/t/-/135252/432) (redpaw), [#433](https://community.hubitat.com/t/-/135252/433) (kkossev)
- Code: the child label is taken from `d.NodeLabel` **only** —
  `Matter_Advanced_Bridge.groovy:4207` (`createChildDevice()`); the name comes from
  `product_name`/`ProductName` at `:4176` and `:4206`.
- `TagList` (0x001D:0x0004) **is** decoded and stored since the plan was written
  (`Matter_Advanced_Bridge.groovy:1370` `normalizeTagList()`, `:1444-1453`) but is never consulted
  when naming a child.
- `FixedLabel` (0x0040) and `UserLabel` (0x0041) are declared in `Libraries\matterLib.groovy:79-80`
  and `:511-517` but are never read, subscribed, or parsed.
- Existing design doc: `AGGREGATOR_LABELS_PLAN.md` — written against
  1.8.8, unimplemented; re-verify it against 1.9.0 before starting.
  A second, much larger one exists in
  `AQARA_AGGREGATOR_LABELS_IMPLEMENTATION_PLAN.md`;
  see the scope note at its top before working from it.
- **2026-08-13 analysis (kkossev's own Aqara Hub E1, water leak sensor at endpoint `0x6B`).** The hub
  reports `NodeLabel` == `ProductName` == `ProductLabel` == `Aqara Water Leak Sensor`, while the device
  *does* carry a custom name in the Aqara Home app. What this establishes:
  - `NodeLabel` is **already imported** — `createChildDevice()` passes `label: d.NodeLabel ?: ''`. The
    gap is not a missing read; the E1 is exporting the model name in that field.
  - `0039_FFFB` on that endpoint lists every standard attribute, and
    `discoverGlobalElementsStateMachine()` reads **all** ids the device reports — there is no hardcoded
    subset in the read path, so no other name-bearing field is hiding inside cluster `0x0039`. The
    whitelist in `parseBridgedDeviceBasic()` decides only what is *stored*.
  - This endpoint is a **flat bridged node**, not the G3 nested-Aggregator case — `0x0039` sits directly
    on `0x6B`. Aggregator/`PartsList`/`TagList` work cannot help it.
  - The remaining candidate is the Matter-standard **`UserLabel` cluster `0x0041` / `FixedLabel`
    `0x0040`**, attribute `0x0000 LabelList` — never read, parsed or subscribed by MAB.
  - ⚠️ **Hazard:** `requestExtendedInfo()` deliberately skips cluster `0x41` on any non-zero endpoint —
    `Libraries\matterUtilitiesLib.groovy:189`, *"KNOWN TO CAUSE Zemismart M1 to crash"*. Any discovery
    read of `0x0041` must be guarded, not unconditional.
- **PROBE RESULT (2026-08-13, Aqara Hub E1, endpoint `0x6B`) — negative.**
  `utilities readAttributeSafe 0x6B 0x0041 0x0000` and `... 0x0040 0x0000` were both refused:

  ```
  readAttributeSafe(): state[fingerprint6B]['ServerList'] does not contain cluster 65 (0x0041) !
  readAttributeSafe(): state[fingerprint6B]['ServerList'] does not contain cluster 64 (0x0040) !
  valid clusters are: [001D, 0039, 0003, 0045, 002F]
  ```

  The endpoint exposes only Descriptor, BridgedDeviceBasicInformation, Identify, BooleanState and
  PowerSource. **Neither `UserLabel` (`0x0041`) nor `FixedLabel` (`0x0040`) exists on the bridged
  endpoint**, so there is no Matter-standard user-label field for MAB to read, and no `0x0041`
  discovery read (and therefore no Zemismart M1 crash risk) needs to be added. Branch A of the plan is
  dead for this hub — the only field that could carry the name is `NodeLabel`, which the E1 fills with
  the model name.
- ## SETTLED (2026-08-13): the Aqara Hub E1 does not export app-assigned names. Not implementable.
  **Rename test.** The leak sensor was renamed to **`ZZTEST Leak`** in the Aqara Home app, then a full
  `_DiscoverAll` was run. The hub still reports the model name on every field:

  ```
  [0039_0003] ProductName  = Aqara Water Leak Sensor
  [0039_0005] NodeLabel    = Aqara Water Leak Sensor
  [0039_000E] ProductLabel = Aqara Water Leak Sensor
  ```

  Same for the wall switch at `0x35`: `ProductName` = `NodeLabel` = `ProductLabel` =
  `Aqara Wall Switch EU`. **Case (a) confirmed** — the E1 never publishes the app name, at rename time
  or otherwise. No MAB change can recover it. The earlier Apple Home screenshot
  (`E1 Water Leak Sensor`, a string the wire has never carried in these logs) is therefore Apple's own
  Home-database entry, not a live read. Close this as *not implementable for the E1*; only a
  hub-firmware change on Aqara's side could alter it.
  Counter-example worth noting: the **bridge's own** `0x0028 NodeLabel` is `Aqara Hub` while its
  `ProductName` is `Aqara Hub E1` — so the E1 does populate `NodeLabel` distinctly for the root node,
  and simply mirrors the model name for every bridged device.
- **INDEPENDENT CONFIRMATION (2026-08-13): Samsung SmartThings, fresh fabric — the custom name is not
  there either.** kkossev commissioned the same E1 hub onto SmartThings as a brand-new Matter fabric.
  The app-assigned names are absent there too. This is the decisive cross-controller test and it closes
  the last remaining loophole: the earlier hypothesis was that the E1 publishes the name *once at
  share/commission time* and lets it go stale, which would have made a re-share a valid workaround. A
  freshly commissioned third controller disproves that — the name is **never** published, at any point
  in the lifecycle. Three independent controllers now agree (Hubitat/MAB, Apple Home, SmartThings), so
  this is a property of the Aqara hub firmware and not of any controller's implementation.
- **Nuance found on the M3 (2026-08-13): Aqara scenes DO carry the user's name.** The M3 bridges Aqara
  *scenes/automations* as `On/Off Plug-in Unit` endpoints (PartNumber `MVD-SCN`), and there
  `NodeLabel` = `ProductName` = `ProductLabel` = **`Scene test`** / **`Entertainment`** - the names the
  user gave those scenes in the Aqara app. This does **not** overturn the conclusion above: for a
  scene there is no model name, so the only string available *is* the user's. For physical bridged
  devices the rename test on the E1 was decisive - the model name is reported and a rename changes
  nothing. Worth remembering when writing user-facing text: "Aqara does not export your device names"
  is true of devices, while scene children do come across with sensible labels.
- **Why Apple Home still shows a name — most likely HomeKit, not Matter (kkossev's hypothesis,
  2026-08-13).** The Hub E1 is a **HomeKit** bridge that long predates its Matter support, and Aqara's
  HAP bridging *does* pass the app-assigned name (HomeKit's Accessory Information `Name`
  characteristic). Matter and HAP are separate stacks on the same hub, and Aqara evidently populates
  the name on one and not the other. That single assumption explains every observation: Apple (HomeKit)
  shows `E1 Water Leak Sensor`; MAB and SmartThings (Matter only) see the model name.
  A competing explanation remains possible — that Apple is on Matter and captured the name at pairing
  time, before a firmware change made the hub always report the model name — and Apple's accessory page
  did show a *Connected Services* entry, which leans Matter. **Distinguishing test:** the sensor is
  currently renamed `ZZTEST Leak` in the Aqara app. If Apple Home now shows `ZZTEST Leak`, the name is
  propagating live over some protocol (HomeKit); if it still shows `E1 Water Leak Sensor`, Apple simply
  cached it and no live source exists on either stack.
  **Either way this does not change the outcome for MAB** — Hubitat has no HomeKit controller, so a
  name that exists only over HAP is unreachable regardless. Recorded because it changes the
  *explanation* published in `docs/user/bridges/aqara.md`, not the conclusion.
  **No further investigation is warranted.** Only an Aqara firmware change could alter this; the
  remaining lever would be reporting it to Aqara.
- ## NEW FINDING — the nested structure is reproducible on the E1, and it IS worth fixing
  The same log shows the multi-gang topology that TODO 1.1 was originally about, on kkossev's own
  hardware — no G3 needed to develop against:

  ```
  ep 0x01  DeviceTypeList [000E] Aggregator   ServerList [0003, 001D]        PartsList [0035, 0036, 0037, 006B]
  ep 0x35  DeviceTypeList [0013] Bridged Node ServerList [001D, 0039]        PartsList [0036, 0037]   NodeLabel = 'Aqara Wall Switch EU'
  ep 0x36  DeviceTypeList [0100] On/Off Light ServerList [001D, 0003, 0006]  PartsList []            NodeLabel = null   <- no 0x0039 at all
  ep 0x37  DeviceTypeList [0100] On/Off Light ServerList [001D, 0003, 0006]  PartsList []            NodeLabel = null   <- no 0x0039 at all
  ep 0x6B  DeviceTypeList [0013, 0043, 0011]  ServerList [001D, 0039, 0003, 0045, 002F]  PartsList []  (flat leaf)
  ```

  The two gang endpoints `0x36`/`0x37` carry **no `0x0039` cluster**, so `fingerprintToData()` produces
  `NodeLabel: null, ProductLabel: null` and the bridge-ProductName fallback fires
  (`using bridge ProductName 'Aqara Hub E1' for endpoint 36`). Both children are created from
  `mapMatterCategory()`'s generic `product_name: Switch`. Their **parent bridged node `0x35`** is the
  only endpoint holding a name, and `0x35` itself gets no child (`fingerprint35 has no supported
  clusters; skipping`).
  So the plan's §4.2 topology work is **not** dead — it is the correct fix for this shape, with one
  corrected expectation: the inherited value is the **model** name, not a user name. `Aqara Wall Switch
  EU - 1` / `- 2` beats two identical `Switch` children, and that is the whole realistic benefit.
  This also fed **TODO 2.1** (spurious Button children): the parent-bridged-node-with-PartsList shape
  is exactly what would produce children the user cannot account for. 2.1 was closed 2026-08-16 with
  no evidence ever supplied — and the inheritance work below is precisely what would have fixed its
  most likely cause.
- Also visible: `0x35` reported `Reachable = false` (the wall switch was offline during this run) —
  MAB stores `Reachable` but does not act on it. Possible separate item.

#### Remaining actionable work `[x]` — inherit the parent bridged node's name for multi-gang devices
> **IMPLEMENTED AND CONFIRMED ON THE HUB, 2026-08-13, driver 1.9.1.** See the note at the end of
> this subsection.
The user-name half is closed; **this half is not.** A double-gang wall switch still produces two
children both labelled `Switch`, which is the concrete complaint in
[#432](https://community.hubitat.com/t/-/135252/432).

- **Shape** (reproducible on kkossev's own E1 — no G3 required, see the endpoint dump above):
  the gang endpoints `0x36`/`0x37` have **no `0x0039` cluster**, so `NodeLabel`/`ProductLabel` are
  `null`; their parent **Bridged Node `0x35`** (`DeviceTypeList [0013]`, `PartsList [0036, 0037]`)
  holds the only name, and gets no child of its own.
- **Fix**: build a reverse parent index from the endpoints' `PartsList`, and when a functional endpoint
  has no name of its own, inherit the nearest ancestor's `NodeLabel` plus a component suffix.
- **Correct the plan before coding**: `AQARA_AGGREGATOR_LABELS_IMPLEMENTATION_PLAN.md` §4.1 says to
  inherit from the **Aggregator** (`000E`, endpoint `0x01`). That is **wrong for this hardware** — the
  name is on a **Bridged Node** (`0013`) that happens to have a `PartsList`. Implementing §4.1 as
  written would find nothing. The `UserLabel`/`FixedLabel` avenue is also dead (probed absent).
- **Realistic benefit, state it honestly**: the inherited string is the **model** name, so the outcome
  is `Aqara Wall Switch EU - 1` / `- 2` rather than two identical `Switch` children. Worth doing; not
  the user's own name, which does not exist on the wire.
- Ordering constraint: children are created in `discoverAllStateMachine` state 26, after the whole
  PartsList loop (states 21-24) has finished, so the topology is available in time — but only if it is
  built before state 26, not per-endpoint inside the loop.
- Do not relabel an existing child that the user has named; see the resolved sub-issue below.

**What was implemented (2026-08-13, driver 1.9.1):**
- New helpers next to `fingerprintToData()`: `fingerprintForEndpoint()`, `fingerprintHasIdentity()`,
  `findIdentityParentEndpoint()`, `computeComponentSuffix()`, `isReplaceableChildLabel()`.
- The parent search picks the nearest ancestor whose fingerprint actually holds `0x0039` data. This
  is what makes the two-parent case work: `0x36` is listed in **both** `0x01` (Aggregator) and `0x35`
  (Bridged Node) `PartsList`s, and the Aggregator drops out because it has no `0x0039` — no device
  type test needed, so the plan's §4.1 `000E` rule is *not* used. Ties break on the smallest
  `PartsList` then the endpoint string; cycles and nesting are handled.
- `fingerprintToData()` inherits `NodeLabel`/`ProductName`/`ProductLabel`/`VendorName`/`SerialNumber`/
  `UniqueID` and computes `suggestedLabel`, **before** the existing bridge-ProductName fallback, so a
  gang is no longer named after the bridge.
- `createChildDevice()` applies `suggestedLabel` at creation and relabels an existing child only when
  `isReplaceableChildLabel()` allows it. Adds a `parentEndpoint` Device Data value.
- **Verified by a Groovy harness** that extracts the real helper source out of the driver file (so it
  cannot drift) and runs it against the transcribed E1 topology: 17/17 pass, covering the two-parent
  case, the flat leaf, suffix numbering, all five label-policy branches, single-part parents, a
  `PartsList` cycle, nested unnamed parents, and an orphan endpoint.

**CONFIRMED on the dev hub (2026-08-13).** `M3150-36` Device Info shows
`Device label = Aqara Wall Switch EU - 1` with `Device name = Generic Component Switch`, and its
Device Data carries `Parent Endpoint 35`, `Serial Number 54ef44…85cf`,
`Unique Id lumi.54ef44…85cf`. Endpoint `0x36` has no `0x0039` cluster of its own, so every one of
those values proves the inheritance path, including the two-parent disambiguation that had to reject
Aggregator `0x01` in favour of Bridged Node `0x35`.

> ⚠️ **Gotcha worth remembering.** The child's **Device Data** section also lists `Label`, `Name` and
> `Is Component`. Those are **Hubitat's own creation-time snapshot** — the driver never writes them,
> and `setLabel()` does not refresh them. The Device Data `Label` row kept reading
> `Aqara Wall Switch EU` while the real label was already `Aqara Wall Switch EU - 1`. When checking a
> label, read the **Device Info → Device label** field, never the Device Data row.

**Hardening applied after the confirmation:** the `dw.setLabel()` call is now wrapped in try/catch.
It runs inside `createChildDevice()` on the `_DiscoverAll` path, so an unguarded platform refusal
would have thrown up into `DISCOVER_ALL_STATE_SUPPORTED_CLUSTERS_NEXT_DEVICE` and aborted discovery
for every remaining endpoint. A failure now logs a warning and discovery continues.

**Still to spot-check on the hub:** after `_DiscoverAll`, `M3150-36`/`M3150-37` should be labelled
`Aqara Wall Switch EU - 1` / `- 2`, gain `serialNumber = 54ef44…85cf`,
`uniqueId = lumi.54ef44…85cf` and `parentEndpoint = 35`, and the log should show
`inherited identity from parent endpoint 35`. The leak sensor `M3150-6B` must be **unchanged**
(flat leaf, no suffix, keeps its own serial). Then rename one gang by hand, re-run `_DiscoverAll`,
and confirm the manual label survives.
- **Resolved sub-issue (do not re-open):** redpaw first reported that children lose their labels on
  re-discovery ([#432](https://community.hubitat.com/t/-/135252/432)); after retesting he confirmed labels **do** persist and the loss only
  happened when he deleted and recreated a child with a different driver
  ([#439](https://community.hubitat.com/t/-/135252/439)).

### 1.2 `[x]` Expose each child's Matter SerialNumber / UniqueID in its Device Data
@Bogie: *"Is there any way to see the actual serial number of each child device? Is it part of the
code listed in the 'Device Data' section under 'Device Info'?"*
- Post: [#440](https://community.hubitat.com/t/-/135252/440) (Bogie, 2026-08-11). **Newer than the
  #418–#439 harvest** — that is why it was missing from this list; the thread should be re-mined from
  #440 onward.
- **Answer today: no.** A child's Device Data holds only `id`, `fingerprintName`, `product_name`,
  `deviceType`, `deviceTypeName` and the `fingerprintData` JSON blob. No serial number anywhere.
- The data **is** already read off the wire on every `_DiscoverAll` — kkossev's E1 log shows
  `[0039_000F] SerialNumber = 158d00…55d2` and `[0039_0012] UniqueID = lumi.158d00…55d2` — but
  `parseBridgedDeviceBasic()` drops both: its whitelist keeps only
  `VendorName, ProductName, NodeLabel, SoftwareVersionString, Reachable, ProductLabel`. Because
  `copyEntireFingerprintToChild()` copies that same map, the values never reach `fingerprintData`
  either.
- Fix: add both to the *storage* list but **not** to the event list — mirror the split
  `parseBasicInformationCluster()` already uses, otherwise every discovery emits two more
  `attribute ... is not declared in driver` warnings. Then carry them through `fingerprintToData()`
  and write them with `updateDataValue` in `createChildDevice()`, outside the `dw == null` branch so
  existing children are upgraded too.
- Both are optional Matter attributes — write them only when the bridge actually reports them, and
  skip nulls (HE stores a null as the literal string `'null'`).
- Privacy: the plan's §7.3 says keep identifiers out of routine logs. That is already moot —
  `logRequestedClusterAttrResult()` prints the serial in full during every discovery.
- Design origin: `AQARA_AGGREGATOR_LABELS_IMPLEMENTATION_PLAN.md` §2.2/§7.1/§7.2 proposed exactly this
  (as `matterSerialNumber`/`matterUniqueId`). Existing Device Data keys carry no `matter` prefix, so
  plain `serialNumber`/`uniqueId` is more consistent.
- **VERIFIED IN PART on the dev hub (2026-08-13, Aqara Hub E1, driver 1.9.1).** The water leak child
  (`M3150-6B`) Device Data now shows `Serial Number = 158d00…55d2` and
  `Unique Id = lumi.158d00…55d2` (Hubitat prettifies the camelCase keys for display). Both values
  also appear inside the `fingerprintData` JSON, as expected now that they reach `state` -
  harmless duplication, and it means they survive `minimizeStateVariables` by two routes.
- **VERIFIED COMPLETE on the dev hub (2026-08-13, Aqara Hub E1, driver 1.9.1).** All three checks pass:
  1. *Positive* - `M3150-6B` Device Data shows `Serial Number = 158d00…55d2` and
     `Unique Id = lumi.158d00…55d2`.
  2. *Null guard* - the gang children `M3150-36` / `M3150-37` show **neither** key. The log confirms the
     map carried `SerialNumber:null, UniqueID:null` into `createChildDevice()` and nothing was written,
     so no literal `'null'` string was stored.
  3. *Event suppression* - `parseBridgedDeviceBasic: SerialNumber = ...` and `UniqueID = ...` appear as
     info lines with **no** accompanying `not declared in driver` warning. The only such warnings left
     are the pre-existing six (`vendorName`, `productName`, `nodeLabel`, `softwareVersionString`,
     `productLabel`, `reachable`, plus `batteryVoltage`), unchanged.
  Child `fingerprintData` grew from 813 to 878 bytes for endpoint `0x6B` - the two new keys.
- **CAVEAT (2026-08-13, Aqara M3 CN hub): the reported serial is NOT always device-unique.** Endpoint
  `0x50` ("Air Conditioner", a hub-hosted virtual/IR device) reports
  `SerialNumber = 54ef44…535f` and `UniqueID = lumi1.54ef44…535f` - which are the **M3 hub's own**
  identifiers, straight out of its `0x0028` BasicInformation (`SerialNumber = 54ef44…535f`,
  `UniqueID = lumi1.54ef44…535f`). Other endpoints on the same hub do report distinct values
  (scenes gave `1082…6832` / `AL.6147…6986`), so it is per-endpoint behaviour, not
  hub-wide.
  This is the bridge's data and MAB reports it faithfully - not a driver defect. But it means the
  feature cannot be described to users as "a unique id per device": on some bridges several children
  will show the hub's serial. Say "the identifiers the bridge reports, when it reports them" in any
  forum reply, and do not build de-duplication or device-matching logic on these values.
- **Follow-on insight for the multi-gang work (item 1.1 "Remaining actionable work").** The wall switch
  *does* have identifiers - endpoint `0x35` reports `SerialNumber = 54ef44…85cf` and
  `UniqueID = lumi.54ef44…85cf` - but `0x35` gets no child device, so they are stored in
  `state.fingerprint35` and surface nowhere. The gang children `0x36`/`0x37` have no `0x0039` and
  therefore no identifiers at all. The same parent-inheritance change that would give them a useful
  label should also copy the parent's `serialNumber`/`uniqueId` down, so a multi-gang device is
  identifiable too. Note both gangs would then share one serial, which is correct - it is one physical
  device.
- Not included: Descriptor `EndpointUniqueID` (`0x001D:0x0005`). The plan lists it, but it is absent
  from these Aqara endpoints' `001D_FFFB` and nobody has asked for it — leave it until a bridge that
  exposes it turns up.

### 1.3 `[ ]` Follow-ups revealed by the Philips Hue regression run (2026-08-13)
A full `_DiscoverAll` on the Hue bridge (31 endpoints, 108 subscriptions) completed cleanly and is a
good **cross-vendor regression test for the parent-identity inheritance of item 1.1** — the Hue
dimmer switch has the same shape as the Aqara multi-gang switch:

```
ep 0x20  Power Source + Bridged Node   ServerList [001D, 0039, 002F, 002E]  PartsList [0021..0024]
         NodeLabel 'Hue dimmer switch 2'   UniqueID f803f6c9...  (no SerialNumber - Hue omits 0x000F)
ep 0x21-0x24  Generic Switch (000F)    ServerList [003B, 001D, 0003]        PartsList []   no 0x0039
```

**Result: the four button children kept their user labels** (`Button Hue On (Matter)` etc.) and no
`relabelling` line appeared. `isReplaceableChildLabel()` refused correctly — not blank, not generic,
not equal to the inherited base `Hue dimmer switch 2`. This is the regression that mattered most: a
bridge-wide rename of every multi-endpoint device's children did **not** happen.

Two follow-ups this run exposed:

1. **Hue publishes `TagList`, so the component suffix could be semantic.** The four buttons carry
   `Switches.On '1'`, `Switches.Up '2'`, `Switches.Down '3'`, `Position.Bottom '4'`. The current
   suffix is the 1-based `PartsList` index, so they would read `- 1 .. - 4` where
   `- On`, `- Up`, `- Down`, `- Bottom` is available and far better. `normalizeTagList()` already
   decodes this. That is the plan's §5.2, now with real data to build against. Aqara's endpoints have
   **no** `TagList`, so the index must remain the fallback.
2. **Descriptor `0x0005` prints as `UNKNOWN`.** Hue exposes it — `[001D_0005] UNKNOWN =
   41daf9…4a41` — because `DescriptorClusterAttributes` in
   `Libraries/matterLib.groovy` stops at `0x0004 TagList`. Adding `0x0005 : 'EndpointUniqueID'` is a
   one-line fix and is exactly what the plan's §10.2 asked for. Whether to store it is a separate
   question; naming it correctly in the logs is free.

## 2. Discovery / endpoint classification

### 2.1 `[x]` Spurious "Button" child devices from an Aqara G3 bridge — **CLOSED, NO EVIDENCE**
- Jira: `HUB-64` (Done, 2026-08-16)

> **CLOSED 2026-08-16 — no evidence ever supplied; two hypotheses recorded below.** The completion
> gate was never met: no `_DiscoverAll` log or endpoint fingerprint from the G3 arrived, redpaw did
> not follow up on post #439, and no G3 is available to test. This is **not** a failed reproduction
> — nobody attempted one, because there is no hardware. Nothing in the classification logic was
> changed. **Reopen on** any `_DiscoverAll` debug log or `state` fingerprint dump from driver 1.9.1
> or later showing unexplained Button children.
>
> **Hypothesis A — the 8 Buttons are correct and the real complaint is naming.** Matter models each
> gang of a multi-gang remote as its own Generic Switch (0x000F) endpoint, and some Aqara firmware
> adds a composite endpoint on top, so two H1 double-rockers can legitimately yield six to eight
> endpoints. "Cannot match them to physical devices" is the expected result when every child is
> named `Button` and carries the model name in `NodeLabel`. Under A the work belongs to item **1.1**,
> which already owns it, and 2.1 has nothing of its own.
>
> **A is also already addressed by shipped work.** 1.1's "Remaining actionable work" — parent
> bridged-node name inheritance — was implemented and hub-confirmed on 2026-08-13 in driver
> **1.9.1**, so a gang endpoint with no `0x0039` of its own now inherits the nearest named ancestor's
> `NodeLabel` plus a component suffix (`Aqara Wall Switch EU - 1` / `- 2`) instead of a row of
> identical children. redpaw reported against 1.8.8/1.9.0, a month before that landed. Unaccountable
> children on **1.9.1 or later** are new evidence, not this item.
>
> **Hypothesis B — the `003B` branch is missing the device-type check its siblings have** (the code
> evidence below). Weaker than it looks: `0006` OnOff is tested *before* `003B`, so ordinary
> wall-switch endpoints never reach the Button branch.
>
> **The deciding evidence is already instrumented** — `mapMatterCategory()` logs `ServerList` and
> `DeviceType` for every endpoint (`Matter_Advanced_Bridge.groovy:3711`) whenever debug logging is
> on. A device type of 0x000F on those endpoints means A; anything else means B. Nothing needs to be
> built to make this investigable; only a reporter willing to send the log is missing.

After a clean re-discovery @redpaw got **8 `Button` children he cannot match to any physical
device** (he owns two H1 wireless switches, which he identified separately from the logs). Last
post in the thread, still unanswered.
- Post: [#439](https://community.hubitat.com/t/-/135252/439) (redpaw, 2026-07-07)
- Code: `mapMatterCategory()` maps **any** endpoint whose `ServerList` contains cluster `003B` to a
  Button child, with kkossev's own marker `// Switch / Button - TODO !` —
  `Matter_Advanced_Bridge.groovy:3528-3529`. There is no `DeviceTypeList` check for
  0x000F *Generic Switch*, unlike the 0x0045 branch which does distinguish
  contact vs. water leak by device type (`:3467-3485`).
- Compounding: the final fallback at `:3538` turns every unrecognized endpoint into a
  `Generic Component Switch` named `Unknown`, so nothing is ever skipped.
- Needs: redpaw's `_DiscoverAll` debug log / `state` fingerprints for the G3, to see whether the 8
  endpoints are aggregator/bridged-node containers, unused gang endpoints, or real
  `003B` endpoints of the H1 switches.
- Related to 1.1 — the same aggregator/nested-endpoint tree is the likely source.

## 3. Fan control (cluster 0x0202)

### 3.1 `[ ]` SwitchBot Standing Fan: `Cycle Speed` throws an error — **VERIFY ON DEVICE**
@iEnam re-paired his SwitchBot Hub 2 and reports that the fan speed selector works, but the
`Cycle Speed` command produces an error. Explicit request: "please review when you can". The
screenshots in the post are images — **ask iEnam for the exact log text** before fixing.
- Post: [#438](https://community.hubitat.com/t/-/135252/438) (iEnam, 2026-07-07)
- Which child driver is involved matters: `mapMatterCategory()` checks `005B` (Air Quality →
  *Matter Generic Component Air Purifier*, `:3486`) **before** `0202` (→ stock
  *Generic Component Fan Control*, `:3498`), so a fan that also exposes air-quality clusters gets
  the Air Purifier child and a different `cycleSpeed()` implementation.
- Three defects sit on that path (see 3.2, 3.3, 3.4) — any of them could be what he is seeing.

### 3.2 `[ ]` `parseFanControl()` emits `speed = 'smart'`, which is not a legal Hubitat value
FanMode 6 is mapped to `'smart'` (`Matter_Advanced_Bridge.groovy:2083`), but the Hubitat
`FanControl` capability only accepts `low, medium-low, medium, medium-high, high, on, off, auto`
(the same list the driver itself documents at `:449-450` and that the Air Purifier child declares in
`Components\Matter_Generic_Component_Air_Purifier.groovy:147-149`). The child rejects the event.
- Options: drop mode 6 from the mapping, or map it to `'auto'` and keep the raw mode in a separate
  attribute.

### 3.3 `[ ]` Air Purifier child `cycleSpeed()` cycles labels the parent collapses together
The child steps `off → low → medium-low → medium → medium-high → high → low`
(`Components\Matter_Generic_Component_Air_Purifier.groovy:224-251`), but `componentSetSpeed()`
maps `medium-low` and `medium` both to Matter FanMode 2, and `medium-high` and `high` both to
FanMode 3 (`Matter_Advanced_Bridge.groovy:3820-3833`). Two of every four cycle steps therefore
write the mode the fan is already in, and the reported `speed` snaps back — the cycle looks stuck.
- BUGS **B2** fixed the `setSpeed` pass-through but left `cycleSpeed()`'s label set unaligned with
  the parent's mapping.
- **The same method has no `'auto'` case.** The `switch` handles `off/low/medium-low/medium/`
  `medium-high/high` and nothing else, so a current speed of `auto` falls to the default branch,
  logs `Unknown current speed: auto` and does nothing at all. Found 2026-08-16 while testing on an
  IKEA STARKVIND, whose `supportedFanSpeeds` is `[off, low, medium, high, auto]` — a user who leaves
  the purifier in auto and presses **Cycle Speed** gets silence. Not hypothetical on that hardware.
- Note the parent's own `componentCycleSpeed()` (`:3844-3859`, used by the stock Fan Control child)
  already does this correctly — it restricts the cycle to `supportedFanSpeeds`. Consider making the
  Air Purifier child delegate to it instead of cycling locally. **That single change closes both
  defects above**: the parent's version cycles only the speeds the device reported, so the collapsed
  labels never get written and `auto` stops being an unhandled state.

### 3.4 `[x]` `componentSetSpeed()` is missing the deviceNumber guard its siblings have
`componentOpen()`/`componentClose()` validate `deviceNumber` before use
(`Matter_Advanced_Bridge.groovy:3797`, `:3808`); `componentSetSpeed()` did not,
so a child with a missing/invalid `id` data value surfaced as a raw
`HexUtils.hexStringToInt(null)` NPE rather than a warn.
- **Fixed in 1.9.2.** `componentSetSpeed()` now null-checks the `id` data value *before* converting
  it (its siblings convert first and validate second, so they retain the latent NPE), then applies
  the same `deviceNumber` range check. It also gained a ServerList guard: writing FanMode to an
  endpoint with no `0202` cluster used to be silently accepted by the bridge — an IKEA VINDSTYRKA
  answered with `success:true` and never reported a FanMode (confirmed on device 2026-08-16).
- **Bearing on 3.1:** the child's `cycleSpeed()` calls `componentSetSpeed()`, so the NPE fixed here
  was one of the three candidate causes of iEnam's error, and it now surfaces as a named warning
  instead of a stack trace. This does **not** make Cycle Speed work — it only tells you which cause
  you are looking at. The new ServerList guard is diagnostic in the same way: a silent no-op could
  never have produced the error he reported. **3.2** (the illegal `'smart'` value, still unfixed)
  remains the strongest hypothesis for a visible error. Ask iEnam to retest on 1.9.2 and send the
  exact log line.
- Low risk, mechanical.

## 4. Coexistence with the Hubitat stock "Generic Matter Bridge"

### 4.1 `[ ]` Adopt / migrate children created by the stock Matter bridge driver
kkossev's own in-thread commitment, prompted by @redpaw's duplicated devices (one set from the
stock bridge, one from MAB, plus Aqara/Alexa authorizations): "Making the Matter Advanced Bridge
package compatible with the HE stock 'Generic Matter Bridge' driver now doesn't sound
impossible! … the first tests for an automatic change of the child devices driver types were
successful."
- Posts: [#434](https://community.hubitat.com/t/-/135252/434), [#436](https://community.hubitat.com/t/-/135252/436) (redpaw); [#435](https://community.hubitat.com/t/-/135252/435) (kkossev)
- Blocker documented in `Tests/Matter_Advanced_Bridge_Stock_Child_Adoption_Handover.md` (maintainer-local)
  (2026-07-06): MAB builds child DNIs as `${device.id}-${endpoint}` while the stock bridge uses
  `${device.deviceNetworkId}-${endpoint}`; adoption means searching for the stock-compatible DNI in
  `createChildDevice()`, `sendHubitatEvent()`, `copyEntireFingerprintToChild()`,
  `updateChildFingerprintData()` and `discoverAllStateMachine()`.
- Experiment driver: `Tests\Jailbreak_the_Children.groovy`.
- **PARTLY IMPLEMENTED IN 1.9.0** (commit `52f2e77`, verified against the shipped code 2026-08-16;
  checkbox left open pending your hub confirmation). The DNI half of the blocker above is done:
  `stockChildDni()` / `legacyMabChildDni()` / `findChildByEndpoint()` /
  `childDniForEndpoint()` (`Matter_Advanced_Bridge.groovy:807-834`) create new children with the
  stock `${device.deviceNetworkId}-${endpoint}` form while still resolving the legacy
  `${device.id}-${endpoint}` one, and every hard-coded lookup listed above was converted —
  `isDeviceDisabled()`, `getDeviceDisplayName()`, `getDw()`, `sendHubitatEvent()`,
  `parseColorControl()` and `createChildDevice()`.
  **Still open:** the *driver-type* migration kkossev demoed in post #435. `createChildDevice()`
  reuses whatever child it finds at the adopted DNI without changing its `typeName`, so a child
  left behind by the stock bridge keeps its stock component driver instead of being swapped to the
  MAB equivalent. The one exception is the existing CT→RGBW upgrade path in `parseColorControl()`.

## 5. Logging / UX

### 5.1 `[ ]` Make an unreachable bridge obvious instead of "Queue full"
When @Pat-C's DIRIGERA stopped responding, his only symptom was a `Queue full` error from the
platform; he had to ask what it meant, and kkossev explained it comes from the bridge not answering
the periodic `ping()`. A warn after N consecutive ping timeouts naming the likely cause ("bridge not
responding — check the bridge hub") would close these cases without a forum round-trip.
- Posts: [#426](https://community.hubitat.com/t/-/135252/426) (Pat-C), [#429](https://community.hubitat.com/t/-/135252/429) (kkossev), [#430](https://community.hubitat.com/t/-/135252/430) (Pat-C — resolved by
  power-cycling the IKEA hub for an hour)
- `Queue full` itself is a platform message, not driver text (no occurrence in this repo).
- Low priority / cosmetic.

### 5.2 `[x]` Do not start `_DiscoverAll` while the Matter bridge is offline

Jira: HUB-125 (Done)

@stueyhughes started discovery after his Aqara G410 had stopped responding. Discovery removed the
current subscriptions and then failed in `BRIDGE_GLOBAL_ELEMENTS_WAIT`, making recovery more
disruptive. kkossev explicitly proposed blocking discovery while the bridge is offline.

- Posts: [#3](https://community.hubitat.com/t/-/162116/3) (failure and logs),
  [#4](https://community.hubitat.com/t/-/162116/4) (diagnosis and commitment)
- Status: **DONE**. `_DiscoverAll()` now starts at a `DISCOVER_ALL_STATE_PING_BRIDGE` state that
  pings the bridge (up to 3 attempts, ~5s each) before `DISCOVER_ALL_STATE_INIT` clears subscriptions
  or state; on no response it aborts to `DISCOVER_ALL_STATE_ERROR` with existing children and
  subscriptions untouched. Shipped in v1.9.0 (commit `c65897f`, 2026-07-25), still present in v1.9.2.

### 5.3 `[ ]` Aqara U400 remains online and commandable but stops reporting lock state

@jbasen reports that MAB 1.8.8 can still lock/unlock the U400 and reports `networkStatus: online`,
but physical and commanded state changes no longer update the lock attribute. Disabling/re-enabling
Matter, `Re Subscribe`, hub reboot, lock battery removal, and current U400 firmware 3.1.1.0 did not
restore feedback; Apple Home continues to receive it.

- Posts: [#1](https://community.hubitat.com/t/-/165667/1)–[#6](https://community.hubitat.com/t/-/165667/6)
- Status: **NEEDS_EVIDENCE** — likely subscription/routing state, but no MAB debug capture of a
  physical lock/unlock or subscription callback is posted yet.
- Next evidence: MAB 1.9.x retest, child and parent debug logs around `Re Subscribe`, a physical
  unlock, and a commanded unlock; capture `SubscriptionResult` and the raw Door Lock report.

### 5.4 `[?]` Aqara Signal endpoints reported less reliable than Soft Sensor endpoints — **VERIFY ON DEVICE**

- **Reported** privately on 2026-08-15 for Aqara FP1 and FP2 presence devices. Equivalent Aqara
  Soft Sensor endpoints were described as reliably following state changes while Matter Signal
  endpoints intermittently did not. Private correspondence, screenshots, and identity are not
  reproduced here.
- The report does not yet distinguish a missed bridge report, a subscription lapse, endpoint
  classification, event deduplication, or a display-only difference. Do not change parsing from
  screenshots alone.
- **Required evidence:** synchronized Hubitat event histories for the Signal and Soft Sensor
  children, MAB debug logs covering the same transitions, endpoint/cluster fingerprints, and a
  precise statement of which presence transitions were missing or delayed.
- **Verification:** repeat controlled occupied/unoccupied transitions on FP1 and FP2 and compare
  both children over the same time window, including after rediscovery or resubscription.

## 6. Documentation

### 6.1 `[ ]` Add the Rachio Smart Lighting Controller to the supported-devices list
@mjarends reports it paired and worked immediately with MAB. Worth listing in the wiki / top post as
a confirmed-working bridge.
- Post: [#431](https://community.hubitat.com/t/-/135252/431) (mjarends, 2026-06-18)

### 6.2 `[ ]` Document that a Matter bridge exposes only its *non-Matter* devices
@Pat-C bought IKEA Thread devices, paired them to DIRIGERA, and expected `_DiscoverAll` to find
them. kkossev explained that true Matter-over-Thread devices must be paired to HE directly with a
code generated by the IKEA app — the DIRIGERA bridge only exposes its Zigbee/legacy devices. This is
a recurring first-question; one paragraph in the wiki would answer it.
- Posts: [#420](https://community.hubitat.com/t/-/135252/420)–[#424](https://community.hubitat.com/t/-/135252/424) (Pat-C, kkossev)

### 6.3 `[ ]` Documentation links still point at the superseded GitHub Wiki
Noticed 2026-08-16 while preparing the 1.9.1 HPM release. Not from the thread.
- `AGENTS.md:10` lists `Wiki: …/wiki` as *the* documentation link for the project, and does not
  mention `docs/user/` at all — so an agent reading the canonical guide is sent to the frozen wiki.
  Should point at [`docs/user/index.md`](user/index.md), with the wiki demoted to a legacy note if
  it is kept.
- **Already correct, do not "fix":** the parent driver's `GITHUB_LINK`
  (`Matter_Advanced_Bridge.groovy:86`) and the three component drivers that define their own
  (`Air_Purifier:1080`, `Camera_AV_Stream:614`, `Window_Shade:696`) all resolve to `docs/user/**`.
  The wiki mentions in `README.md:18`, `docs/user/index.md:23` and
  `docs/user/help/support-and-links.md:23` are deliberate "superseded, kept so old forum links keep
  working" notes.
- The remaining component drivers have no documentation link of their own — worth deciding whether
  they should get one now that `docs/user/drivers/` covers all thirteen.

---

## 7. Carried over from the parent driver's header (moved 2026-08-13)

These four `TODO:` comments sat in the `Matter_Advanced_Bridge.groovy` header block above
`version()`. They were re-verified against v1.9.1 on 2026-08-13 and removed from the driver — this is
now their only home. A fifth entry was an empty `TODO:` line and was simply dropped.

### 7.1 `[ ]` Use the `SubscriptionResult` callback to tell when a subscription's report burst is done
- Original: *"use subscriptionResult - subscriptionId: XXXXXX to determine when subscription
  attribute/event reports have completed."*
- **Partially done already.** `SubscriptionResult` is no longer ignored: `parse()` logs it
  (`Matter_Advanced_Bridge.groovy:540`), routes it to Door Lock children
  (`routeSubscriptionResultToDoorLockChildren()`, `:1194`), and uses it to release the deferred
  second "spammy" subscription instead of a fixed timer (`:3317`, with
  `SPAMMY_SUBSCRIBE_FALLBACK_DELAY` as the fallback only).
- **What remains:** the place that actually needs a "reports have completed" signal still guesses.
  `shouldFilterNoisyPostSubscribeEvent()` (`:1717`) drops post-subscribe events on a **wall-clock
  heuristic** — 30 s or 10 s since `state.lastTx.subscribeTime`, chosen by hub uptime. That is what
  this TODO proposed replacing. Caveat: per the comment at `:1190`, a `SubscriptionResult` describes
  the node-level subscription and carries no endpoint, so it can mark the burst boundary but cannot
  attribute reports to endpoints.
- Worth doing only if the time heuristic is actually mis-filtering something. No user report yet.

### 7.2 `[ ]` Use event `timestamp` / `priority` to filter duplicate and out-of-order events
- Original: *"use events timestamp / priority as a filtering criteria for duplicated events and
  out-of-order events ? (may not ne needed anymore after callbackType:SubscribeResult processing is
  implemented)"*
- **Not done.** Matter event reports do carry both fields — see the sample descMap documented at
  `Matter_Advanced_Bridge.groovy:712`, `timestamp:29456912, priority:2` — but nothing reads them.
  Duplicate suppression today is `isBurstDuplicate()` plus the Door Lock child's own `eventSerial`
  filter; ordering is not checked at all.
- **The TODO's own hedge is now testable.** It guessed this might become unnecessary once
  `SubscriptionResult` processing landed — which it has (7.1). Resolve 7.1 first, then decide whether
  7.2 still buys anything. They should not be worked independently.

### 7.3 `[ ]` Composite grouping — several endpoints' attributes on one child device (@iEnam)
- Original: *"Composite grouping of different attributes of a child device @iEnam"*
- **Not done, and deliberately deferred more than once.** Both
  `AGGREGATOR_LABELS_PLAN.md` (§"Do not merge multiple endpoints…")
  and `AQARA_AGGREGATOR_LABELS_IMPLEMENTATION_PLAN.md`
  explicitly place it out of scope.
- It is already **published as a limitation**, so any change here is user-visible and must update
  `docs/user/compatibility/matrix.md` ("Composite devices — several sensors as one Hubitat device |
  Not supported"), `docs/user/compatibility/device-types.md`, and `docs/user/help/known-issues.md`.
- The largest item in this file. One Hubitat device fed by several Matter endpoints changes child
  creation, DNI allocation, subscription routing and event dispatch at once. Needs its own plan
  before any code.

### 7.4 `[ ]` Thermostat `supportedThermostatModes` is not initialised at discovery, and its fallback is wrong
- Original: *"thermostat component - supported modes JSON initialization after discovery"*
- **Partially done.** `parseThermostat()` case `001B` derives the mode list from
  `ControlSequenceOfOperation` and emits it (`:2451`–`:2464`). Confirmed working on the hub
  (2026-08-13 log): the E1 TRV resolved to `[off, heat]` and the Air Conditioner to `[off, cool]`.
- **What remains — and it hides a real defect.** The list is only populated when a `0x001B` report
  arrives. `initializeThermostat()` (`:4418`–`:4421`) is the fallback and **hardcodes
  `["off", "heat"]`**, so a cooling-only endpoint that never reports `0x001B` would be told it
  supports heating and not cooling. It is also invoked lazily from `componentSetThermostatMode()`
  (`:4352`) — i.e. at first *use*, not after discovery, which is precisely what the TODO asked for.
- Fix direction: seed the attribute from the endpoint's stored `0201_FFFB` / `ControlSequenceOfOperation`
  during discovery, and make `initializeThermostat()` derive its default instead of hardcoding one.
- Verify on a cool-only device (the Air Conditioner on endpoint `0x50` of the M3) that
  `supportedThermostatModes` is correct **before** any thermostat command is issued.

### 7.5 `[ ]` Button child never emits `supportedButtonValues`
- Original: a `TODO:` in the Button child's header — *"send supportedButtonValues attribute event
  depending on the featureMap (e.g. if MSM supported, send `supportedButtonValues =
  'pushed, held, released, double'`) to help Hubitat apps know which button events to expect"*
  (`Components\Matter_Generic_Component_Button.groovy:26`). Moved here 2026-08-16 when **HUB-77**
  ("add Matter button handling") was closed as implemented — this is the one part of that request
  still outstanding, so it must not be lost with the ticket.
- Same shape as 7.4: the capability-level list an app reads to know what to expect is never
  initialised. Apps must discover a button's abilities by observing events.
- The data is already available — `getFeatureMap()` reads `003B_FFFB`/`003B_FFFC` and was made
  reliable by BUGS **B1** (`safeHexToInt` with a numeric `0x1F` default). The bits map directly:
  MSM → `doubleTapped`, MSL → `held`, and `released` follows the release handling the driver already
  implements.
- Note the two remaining header `TODO:`s alongside it — `featureMap` stored in decimal rather than
  hex in `deviceFingerprintData` (readability only), and a proposed `getFeatureMap(cluster)` helper in
  the commonLib. Both cosmetic; neither blocks this.
- Verify on a real button whose FeatureMap is *not* `0x1F` (a device without MSM), so a wrong bit test
  shows up as a missing value rather than being masked by the default.

---

## 8. Air Purifier / filter monitoring

### 8.1 `[ ]` `filterDaysRemaining` covers the HEPA filter only
Added in child v1.2.5 (2026-08-16) for cluster `0x0071`. The activated carbon filter (`0x0072`)
reports the same `Condition` / `ChangeIndication` / `LastChangedTime` set and gets
`carbonFilterUsage` / `carbonFilterStatus` / `carbonFilterLastChanged`, but no
`carbonFilterDaysRemaining` — a device with both filters gets a due-date estimate for one of them.
- Deliberate scope call when the attribute was added, not an oversight.
- Needs a decision before coding: `filterLifeTime` is a **single** preference, and the two filters do
  not share a service interval in practice. A second preference is the honest fix; reusing the one
  value for both would produce a confidently wrong number for the carbon filter.
- No user has asked for it. The test unit (STARKVIND behind DIRIGERA) has no `0x0072` at all, so this
  cannot be verified locally — wait for a device that has both.

## 9. Shelly Gen4 Matter power reporting (thread 161449)

Source: *Should Shelly Gen4 plug report power EVERY second?*, topic **161449**, 20 posts, analyzed
2026-08-16. Post links are `https://community.hubitat.com/t/-/161449/<post#>`. Not part of the main
release thread (135252).

### 9.1 `[ ]` `deviceTypeUpdated()` is a no-op stub — the driver-switch state repair kkossev agreed to never landed
@TArman kept losing working power readings whenever hopping between the stock "Generic Matter
Outlet" and MAB while chasing the reporting-rate issue below. @hubitrep suggested (post
[#13](https://community.hubitat.com/t/-/161449/13)) that the platform's `deviceTypeUpdated()`
callback — called whenever a device's driver type changes — could repair state automatically, and
kkossev agreed (post [#14](https://community.hubitat.com/t/-/161449/14)) it "can be very useful in
this case."
- Code: `Matter_Advanced_Bridge.groovy:580-583` — `deviceTypeUpdated()` exists but only does
  `log.warn "${device.displayName} driver change detected"`. It does not call `updated()`,
  `ensureNewParseFlag()`, or trigger a subscribe, so switching a device *into* MAB still leaves it
  inert until the user manually opens Preferences and hits Save.
- Partially overtaken by events: since v1.9.0 `newParse` is unconditionally forced true and the
  toggle preference was removed entirely (`ensureNewParseFlag()`,
  `Matter_Advanced_Bridge.groovy:587-596`), so the *specific* failure mode in the thread (stock
  driver leaving `newParse` stuck off) can no longer reproduce the same way. The broader gap —
  nothing runs `updated()`/re-subscribe automatically on a driver-type switch — is still open.
- **VERIFY ON DEVICE**: reproduce a stock-driver ↔ MAB switch on current 1.9.2 and confirm whether
  the child still needs a manual Save Preferences before it reports again.

### 9.2 `[?]` Readings "off by common multipliers" for the Power Energy child — **VERIFY ON DEVICE**

Jira: HUB-76 (Inbox)

@TArman reported (post [#8](https://community.hubitat.com/t/-/161449/8)) that "Matter Custom
Component Power Energy" readings were off by common multipliers; kkossev replied "I see where the
problem is... do not use it for this device for now" (post
[#9](https://community.hubitat.com/t/-/161449/9)).
- The very next release, `ver. 1.7.7` (2026-02-14), lists "bugfix: Power/Energy processing
  exceptions" in the driver header — one week after the thread's last post.
- Current code (`Components\Matter_Custom Component_Power_Energy.groovy:212-274`) divides
  RMSVoltage/RMSCurrent/ActivePower/Frequency by `1000` (mV/mA/mW/mHz → V/A/W/Hz) and PowerFactor by
  `10000` (`-10000..10000` → `-1.0..1.0`), which matches the Matter Electrical Power Measurement
  cluster spec units.
- No explicit changelog line ties 1.7.7 to *this specific* multiplier report, so treat as
  **plausibly fixed, unconfirmed on Shelly hardware** rather than a hard close — flip to `[ ]` if a
  Shelly Gen4 user reports it again.

### 9.3 `[ ]` Every-1-second power reports flooding the hub — mitigation shipped, opt-in — **VERIFY ON DEVICE**

Jira: HUB-117 (Verify on Device)

@daniel.winks confirmed (post [#18](https://community.hubitat.com/t/-/161449/18)) Shelly Gen4 power
devices report at 1 Hz over both WebSocket and Matter with no on-device configuration option, and
suggested averaging N reports before each `sendEvent()` to cut hub load. kkossev noted (post
[#17](https://community.hubitat.com/t/-/161449/17)) that HE-side filtering "would be a patch only"
since driver-call overhead remains either way.
- `SupportedMatterClusters[0x0090].subscriptions` marks `ActivePower`/`RMSVoltage`/`RMSCurrent`/
  `Frequency`/`PowerFactor` as `isSpammy: true` (`Matter_Advanced_Bridge.groovy:293-301`), and the
  `spammyAttributesMinInterval` preference (added **v1.9.0**, 2026-07-25 — five months after this
  thread) routes `isSpammy` attributes into a second, additive `matter.subscribe()` with a longer
  minimum interval (AGENTS.md §2.5).
- This throttles at the Matter reporting-engine layer (the device itself sends less often), which is
  a better fix than daniel.winks' driver-side averaging idea — no wasted `sendEvent()` calls at all,
  vs. still waking the driver every second and only averaging afterward.
- Already documented: `docs/user/drivers/power-energy.md:60-65` ("Known limitations" names Power/
  Voltage/Current/Frequency/PowerFactor as frequently-reporting and points at the preference by
  name), plus `docs/user/configuration/preferences.md:60`, `docs/user/drivers/air-purifier.md:70`,
  and `docs/user/help/troubleshooting.md:95`. No documentation gap — retracts the "nothing points a
  Shelly owner at this preference" note from the first pass of this analysis.
- **Why this is still open despite shipping:** AGENTS.md §2.5 flags it as unverified whether the HE
  Matter client keeps the *primary* subscription alive alongside a second `matter.subscribe()`. If
  the primary dies whenever `spammyAttributesMinInterval` > 0, that breaks every attribute on the
  bridge, not just the throttled ones. Verification step: set the preference above 0 on a bridge with
  both spammy and non-spammy attributes, then confirm the non-spammy ones still report.
- Never retested with the originating reporter's Shelly Gen4 (@TArman).
- Remaining, deliberate: the preference **defaults to `0` = off** (per the v1.9.0 CHANGELOG entry,
  called out explicitly as an upgrade-behavior change) — an affected user still has to find and set
  it themselves rather than getting it automatically. That is a default-value product decision, not
  a bug or a documentation miss; leaving it here only as a design note, not an action item.
- Scope note: only `0x0090` power attributes carry `isSpammy`; `0x0091` cumulative energy does not
  (almost certainly correct — cumulative energy is not a 1 Hz reporter).

## Already covered elsewhere (do not duplicate)

- Aqara G350 camera control (speaker mute / volume) — demoed by kkossev in
  [#419](https://community.hubitat.com/t/-/135252/419); already shipped in the dev branch
  (`ver. 1.8.9`, `Matter Generic Component Camera AV Stream`).
- Everything announced in [#418](https://community.hubitat.com/t/-/135252/418) (the v1.8.0→1.8.8 change list) is in the driver
  header changelog — not a request.
- Air Purifier `setSpeed`/`cycleSpeed` Integer-vs-String → BUGS **B2** (closed; the residual
  `cycleSpeed` label mismatch is item **3.3** here).
- SwitchBot **Button** child being dead since 1.8.0 → BUGS **B8** (closed). Unrelated to the
  SwitchBot **fan** report in 3.1.
- Matter pairing failures, Thread "healing times", multi-fabric desync
  ([#422](https://community.hubitat.com/t/-/135252/422)–[#428](https://community.hubitat.com/t/-/135252/428), Pat-C / GuyMan / hubitrep) → platform and Matter-fabric issues, not
  addressable in the driver. kkossev's reference article is linked in
  [#425](https://community.hubitat.com/t/-/135252/425).
- Hot-path / structural refactors → `Matter_Advanced_Bridge_OPTIMIZATION_PLAN.md`,
  `Libraries/Matter_State_Machines_OPTIMIZATION_PLAN.md` and
  `MATTERLIB_ATTRIBUTE_MAP_REFACTOR.md`. All three had their
  status re-verified against the code on 2026-08-13 — read the header before picking anything up.
- The remaining `plans/` documents (documentation migration, the Phase A/B runtime+persistence
  migration, and the child-driver process guide) are **not** user requests and are indexed only in
  `docs/maintainer/README.md`. They are deliberately not listed here.
