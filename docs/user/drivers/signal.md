# Matter Custom Component Signal

Applies to: 1.9.0 | Last verified: 2026-08-22 | Status: Experimental

Adds a button press to a motion-reporting endpoint, for Aqara Signals that should fire a rule
rather than hold a state.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Custom_Component_Signal.groovy` (driver version 1.1.3)
- **Assigned to:** nothing automatically — you assign this one by hand

## What it is for

An Aqara [Signal](#aqara-signals) is discovered like any other motion-reporting endpoint, and
discovery gives it the stock [Motion Sensor](motion-sensor.md) driver. For most Signals that is the
correct driver: the ones documented so far are presence zones, which carry a state that goes active
and later inactive again.

This driver covers the other case — a Signal that means "something just happened", where a rule
needs a momentary trigger rather than a state to watch. On every `active` report it sends a `motion`
event **and** a `pushed` button event, so the device can drive apps that expect a button.

The `inactive` report is deliberately ignored. A button press has no release, so only the leading
edge produces an event.

To use it, change the child device's **Type** in Hubitat and click **Save Device**.

## Capabilities

`MotionSensor` · `PushableButton` · `Sensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `motion` | `active`, `inactive` | Passed through from the bridge unchanged. |
| `pushed` | `1` | Sent whenever `motion` goes `active`. |
| `numberOfButtons` | `1` | Set when the driver is saved. |
| `currentPosition` | string | Declared, but nothing in this driver sets it. A value the parent forwards under this name is passed straight through. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

## Commands

| Command | What it does |
|---|---|
| **Push** | Sends a `pushed` event without the device doing anything. For testing rules. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per event. |

## Known limitations

- **Assign it deliberately.** Discovery never selects it, and on an endpoint that does not report
  motion it produces nothing at all.
- One button only. Multi-button devices belong on the [Button](button.md) driver.
- No debounce or timeout. Every `active` report is another button press, so a sensor that re-reports
  during sustained motion pushes repeatedly.
- A Signal brings Aqara's cloud dependency with it, whichever driver the child device uses. See
  [Cloud dependency](#cloud-dependency).

## Aqara Signals

Aqara publishes no detailed specification for Signals. What follows was established by testing
against M2 and M3 hubs.

### What a Signal is

A Signal is an Aqara Home condition — "if this is true" — exported to Matter as its own bridged
device. It gives a Matter identity to Aqara behavior that has no Matter device type of its own; an
FP2 presence zone is the standard example.

The feature is **Scene and Signal Sync**, reached from **Profile → Third-party ecosystems → Matter**
in the Aqara Home app, and marked "Limited Trial" as of January 2025. **Scenes** export Aqara
automations outward; **Signals** export conditions — an FP2 zone's presence, a device's on/off state
— so another ecosystem can read them as Matter devices.

![Aqara app: Connect to Ecosystems, with Matter selected](../assets/images/bridges-aqara-signals-01.png)

![Aqara app: Matter page, Scene and Signal Sync](../assets/images/bridges-aqara-signals-02.png)

### Creating a Signal

1. In the Aqara Home app, open the automation editor and build the condition you want to expose,
   for example `Presence (zone) Oven` with "all conditions are met".
2. Save it with a descriptive name. That name becomes the Matter `ProductName` / `NodeLabel` the
   bridge publishes and MAB imports, for example `Aqara FP2 Presence Oven Zone`.
3. Toggle **Add to Matter**.

![Aqara app: naming a Signal and enabling Add to Matter](../assets/images/bridges-aqara-signals-03.png)

### How a Signal reaches Hubitat

A Signal appears in the hub's `BridgedDeviceBasicInformation` cluster (`0x0039`) like any other
bridged device, with `ProductName` taken from the name given above — `Aqara FP2 Presence Signal`
and `Aqara FP2 Absence Signal` for a presence/absence pair.

![Aqara M3 hub log: BridgedDeviceBasicInformation ProductName for two FP2 Signals](../assets/images/bridges-aqara-signals-04.png)

No driver change is needed for this. MAB's existing NodeLabel import picks the name up, and the
child device works as a plain sensor with active/inactive state on the stock Motion Sensor driver.
Per-zone Signals arrive as separate child devices.

![Hubitat device list: FP2 Presence/Absence Signal child devices, including a per-zone Signal](../assets/images/bridges-aqara-signals-05.png)

### Cloud dependency

**A Signal's Running Method is Cloud, not Hub**, unlike an ordinary Aqara Automation, which runs
locally. It is fixed when the Signal is created and cannot be changed from MAB or Hubitat.

![Aqara app: a Signal's Running Method is Cloud, contrasted with an Automation's Running Method of Hub](../assets/images/bridges-aqara-signals-06.jpeg)

The effect is observable rather than theoretical. During a WAN outage, a Signal-derived FP2 presence
child stopped updating entirely, while an Aqara Ceiling Light T1 bridged directly from the same M3
hub carried on responding to Hubitat. The local Matter connection between hub and Hubitat keeps
working; what breaks is the Signal's own path through Aqara's cloud.

The hub is required in any case. An FP2's zones are not exported over Matter by the sensor itself,
only by an M2 or M3 hub.

**Automating on a Signal-derived device means depending on the Aqara hub's Internet connectivity,
not only on your local network.** A directly-bridged Matter device on the same hub is unaffected.

### Discovery and removal

A newly created Signal does not become a Hubitat child device on its own:

1. Run **Discover All** on the MAB bridge device.
2. Wait for discovery to finish.
3. Refresh the bridge device's page (**F5**). An already-open browser session does not update by
   itself.

Existing children are preserved across a rediscovery; only genuinely new devices are added. Deleting
a Signal on the Aqara side does **not** delete its Hubitat child device — remove that one yourself.

If a Signal created after initial setup does not appear, a discovery run followed by the page
refresh is the fix. Removing and re-adding the bridge device is not necessary.

## Aqara Soft Sensors

A Soft Sensor is a different mechanism from a Signal, not a second name for the same thing, and the
difference determines how each one behaves once bridged.

### What a Soft Sensor is

A [Presence Soft Sensor](https://forum.aqara.com/t/what-is-the-presence-soft-sensor-and-how-to-set-it-up-for-better-automations/284575)
fuses several devices — cameras, locks, contact, motion and presence sensors — into a single
room-level presence state, computed on the hub itself, with a configurable "No Presence" delay set
when the sensor is created. It requires a Hub M3 on firmware 4.5.40 or later with Aqara Home 6.1.1
or later.

### How a Soft Sensor reaches Hubitat

A Soft Sensor arrives as a standard Matter Occupancy Sensor, so discovery assigns the stock
[Motion Sensor](motion-sensor.md) driver and no classification change is needed. A working example
imports with this fingerprint:

| Field | Value |
|---|---|
| `deviceType` | `0013` Bridged Node, `0107` Occupancy Sensor |
| `deviceTypeName` | `Occupancy Sensor` |
| `VendorName` | `Aqara` |
| `ProductName` / `ProductLabel` / `NodeLabel` | `Soft Human Presence Sensor` |
| `ServerList` | `001D` Descriptor, `0003` Identify, `0039` Bridged Device Basic Information, `0406` Occupancy Sensing |
| `UniqueID` | `virtual.696241…8040` |

The `virtual.` prefix on `UniqueID` is the hub's own marking: this endpoint is synthesized on the
hub rather than passed through from one physical radio. That matches a fused, locally computed state
rather than a mirrored condition.

### Signal and Soft Sensor compared

| | Signal | Soft Sensor |
|---|---|---|
| Computed where | Aqara's cloud — Running Method is `Cloud` | On the hub |
| Built from | One Aqara Home condition | Several devices, fused into one state |
| Hub requirement | M2 and M3 confirmed | Hub M3 only |
| Matter identity | Depends on the condition; presence zones arrive as motion-reporting endpoints | Occupancy Sensor (`0107`) |
| Cloud round trip in the reporting path | Yes, and it has been observed failing during a WAN outage | Not by design — the state is computed locally |

A Signal's state change has to complete a round trip through Aqara's cloud before Matter sees it; a
Soft Sensor's does not. Where a Soft Sensor is available, expect it to be the more reliable of the
two for presence. The difference is in Aqara's architecture, not in the bridging: MAB imports both
correctly through the same generic driver, and neither needs a driver or parsing change.

## See also

- [Which driver do I get?](index.md)
- [Button](button.md)
- [Motion Sensor](motion-sensor.md)
- [Aqara bridge](../bridges/aqara.md) — per-device compatibility reports and their sources
- [Compatibility matrix](../compatibility/matrix.md)
- [Aqara Advanced Matter Bridging — current Matter Bridge hub list](https://www.aqara.com/en/explore/introducing-advanced-matter-bridging/)
- [Aqara — Presence Soft Sensor explainer](https://forum.aqara.com/t/what-is-the-presence-soft-sensor-and-how-to-set-it-up-for-better-automations/284575)
