# Matter Generic Component Air Purifier

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Air purifiers, and standalone air quality sensors.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Air_Purifier.groovy`
- **Assigned to:** endpoints reporting filter monitoring `0x0071` / `0x0072`, air quality `0x005B`,
  CO₂ `0x040D`, or concentration measurement `0x042A`

Filter monitoring is checked first in the assignment chain, so a purifier gets this driver rather
than a fan or sensor driver. An air quality monitor with no filters gets it too — it simply reports
the sensor half and leaves the purifier controls idle.

Much of this driver is owed to @dandanache's IKEA Starkvind (E2006) Zigbee driver, and the IKEA
Starkvind is the device it was built against.

## Capabilities

`AirQuality` · `FanControl` · `FilterStatus` · `Switch` · `CarbonDioxideMeasurement` ·
`TemperatureMeasurement` · `RelativeHumidityMeasurement` · `PowerSource` · `Sensor` · `Actuator` ·
`Configuration` · `HealthCheck` · `Refresh`

A given device fills in only the parts it actually has.

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `airQuality` | `Good`, `Fair`, `Moderate`, `Poor`, `VeryPoor`, `ExtremelyPoor`, `Unknown` | The device's own assessment, from the Matter air quality scale. |
| `pm25` | number | Particulate matter, µg/m³. |
| `carbonDioxide` | number | ppm. |
| `temperature`, `humidity` | number | If the device measures them. |
| `switch` | `on`, `off` | |
| `speed` | see below | Current fan speed. |
| `auto` | `on`, `off` | Whether the device is running in automatic mode. |
| `filterStatus` | `normal`, `replace` | HEPA filter. |
| `filterUsage` | 0–100 | **Percent used** — 100 means spent, not full. |
| `carbonFilterStatus`, `carbonFilterUsage` | as above | Activated carbon filter. |
| `filterInPlace`, `carbonFilterInPlace` | `present`, `not present` | Whether a filter is fitted. |
| `filterLastChanged`, `carbonFilterLastChanged` | number | When the filter was last reset, as a Matter timestamp. |
| `indicatorStatus` | `on`, `off` | The device's LED indicators. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

## Commands

| Command | What it does |
|---|---|
| **On** / **Off** / **Toggle** | Switches the purifier. |
| **Set Speed** | `auto`, `low`, `medium-low`, `medium`, `medium-high`, `high`, `off`. |
| **Set Indicator Status** | Turns the device's LED indicators on or off. |
| **Reset Filter Condition** | Resets the HEPA or activated carbon filter counter. **Use it after physically replacing the filter.** If the device has no such filter, the command is refused with a warning in the log. |
| **Identify** | Makes the device identify itself, if it supports that. |
| **Get Info** | Writes device details to the live logs and device data. |
| **Configure** / **Refresh** | Re-read the device's attributes. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Sensor report frequency** | Medium — ±3 µg/m³ | How much the PM 2.5 reading must change before the device reports it. Very High (±1) through Very Low (±10). |
| **CO₂ report frequency** | High — ±10 ppm | The same for CO₂. Very High (±5) through Very Low (±100). |
| **Filter life time** | 6 months | Time between filter changes: 3, 6, 9 months, or 1 year. |
| **Child lock** | Off | Locks the physical controls on the device, against accidental operation. |
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per change. |

The two report-frequency settings are the ones worth adjusting. They trade responsiveness against
traffic: a very high frequency on an air quality sensor produces a lot of events. If the logs are
still busier than you want, the parent's **Spammy attributes minimum reporting interval** in
[Preferences](../configuration/preferences.md) throttles at the subscription instead.

**Child lock and filter life time are written to the device**, not just stored in Hubitat.

Two more settings on this tab — **Default Current State** and **Enable command retry logic** — come
from the Hubitat platform rather than from this driver.

## Bridge-specific notes

- The **IKEA Starkvind E2006** is the reference device for this driver.
- The **ALPSTUGA** air quality monitor is supported for CO₂ reporting, added in 1.7.0.
- Air quality is Unsupported or Unknown for every bridge in the
  [compatibility matrix](../compatibility/matrix.md) — those results predate this driver, so they
  are stale rather than negative.

## Known limitations

- **`filterUsage` counts up, not down.** 100 means the filter is spent. A dashboard tile showing
  "100% filter" is telling you to replace it.
- Filter reset writes to the device. If the counter does not move afterwards, the device did not
  accept it.
- Air quality is reported on Matter's six-step scale, not as a numeric index. Where a device also
  publishes `pm25`, that is the more precise figure.
- The driver was developed against a small number of devices. A purifier with controls beyond speed,
  auto mode, and the LED indicator will not expose them here.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
