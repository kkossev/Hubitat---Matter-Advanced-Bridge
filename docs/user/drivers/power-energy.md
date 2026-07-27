# Matter Custom Component Power Energy

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Energy-monitoring plugs and sockets.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Custom Component_Power_Energy.groovy`
- **Assigned to:** endpoints reporting cluster `0x0090` Electrical Power Measurement or `0x0091`
  Electrical Energy Measurement

Measurement is checked before On/Off in the assignment chain, so a metering plug gets this driver
rather than the plain [Switch](switch.md) — and keeps its on/off control.

## Capabilities

`Actuator` · `Switch` · `PowerMeter` · `EnergyMeter` · `VoltageMeasurement` · `CurrentMeter` ·
`Refresh`

## Attributes

| Attribute | Unit | From |
|---|---|---|
| `switch` | `on`, `off` | Cluster `0x0006`. |
| `power` | W | `ActivePower`. |
| `energy` | kWh | `CumulativeEnergyImported`. |
| `energyExported` | kWh | `CumulativeEnergyExported`, for devices that measure both directions. |
| `voltage` | V | `RMSVoltage`. |
| `amperage` | A | `RMSCurrent`. |
| `frequency` | Hz | |
| `powerFactor` | 0–1 | |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

Matter reports these in thousandths — milliwatts, millivolts, milliamps — and energy in milliwatt
hours. The driver converts everything, so the values read in normal units.

A device publishes only what it measures. Many plugs report power and energy and nothing else.

## Commands

| Command | What it does |
|---|---|
| **On** / **Off** | Switches the outlet. |
| **Get Info** | Writes device details to the live logs and device data, including which measurement attributes the device actually supports. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per change. |

## Bridge-specific notes

None recorded. Per-bridge results are in the
[compatibility matrix](../compatibility/matrix.md).

## Known limitations

- **These attributes report often.** Power, voltage, current, frequency and power factor are all
  marked as frequently reporting. If they fill your logs or your event history, set **Spammy
  attributes minimum reporting interval** in [Preferences](../configuration/preferences.md) on the
  parent device to move them to a slower subscription.
- **`energy` is a cumulative total from the device**, not a figure this driver accumulates. It
  cannot be reset from Hubitat, and how the device handles its own reset is the manufacturer's
  business.
- Only the cumulative energy attributes are used. Matter's periodic energy attributes are not
  implemented.
- A plug that measures power on a different endpoint from its switch will appear as two child
  devices.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
