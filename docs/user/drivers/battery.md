# Matter Generic Component Battery

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Battery reporting for a bridged device.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Battery.groovy`
- **Assigned to:** endpoints reporting cluster `0x002F` Power Source, where no other supported
  cluster claimed the endpoint first

**This is usually a second device.** A battery-powered sensor typically produces one child for the
sensor and another for its battery, because the bridge exposes them as separate endpoints. That is
expected, not a fault — see [Known issues](../help/known-issues.md).

## Capabilities

`Sensor` · `Battery` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `battery` | 0–100 | Percentage remaining. |
| `batteryVoltage` | number | Volts. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

Matter reports battery percentage in half-percent units and voltage in millivolts; the driver
converts both, so `battery` and `batteryVoltage` read the way you would expect.

The driver also declares `batStatus`, `batOrder`, `batDescription`, `batTimeRemaining`,
`batChargeLevel`, `batReplacementNeeded`, `batReplaceability`, `batReplacementDescription`, and
`batQuantity`. Only percentage and voltage are subscribed for continuous updates; the rest appear
only if the device reports them, typically during discovery.

## Commands

| Command | What it does |
|---|---|
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time to this device and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per change. |

## Bridge-specific notes

- **Zemismart M1 Hub** reports its own battery percentage in a form that needs correcting. The
  driver applies that patch automatically, for that model only.
- **Single-node Matter devices** — a sensor paired directly rather than through a bridge — report
  battery on their root node. Since 1.9.0 the driver passes that through to the device's own child
  rather than leaving it on the parent. See
  [Using it with Matter devices](../getting-started/use-with-matter-devices.md).
- A **bridge** that reports its own battery publishes it on the parent device instead. See
  [Commands and states](../configuration/commands-and-states.md).

## Known limitations

- **Battery reporting is infrequent by design.** Devices report when the level changes materially,
  not on a schedule, so a fresh reading may be hours old. **Refresh** asks for a current value.
- Charge state and replacement indicators are published only when the device sends them. Most
  bridged battery devices send percentage and nothing else.
- Battery reporting across bridges is largely untested — it is Unknown for every bridge in the
  [compatibility matrix](../compatibility/matrix.md). Reports welcome.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
