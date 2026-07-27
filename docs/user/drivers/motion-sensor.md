# Matter Generic Component Motion Sensor

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Motion and presence sensors — PIR, mmWave radar, and the occupancy part of a multi-sensor.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Motion_Sensor.groovy` (driver version 1.1.2)
- **Assigned to:** endpoints reporting cluster `0x0406` Occupancy Sensing — Matter device type
  Occupancy Sensor (`0x0107`)

A multi-sensor that also measures temperature, humidity, or light reports those on other endpoints,
which become separate child devices.

## Capabilities

`Sensor` · `MotionSensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `motion` | `active`, `inactive` | From cluster `0x0406` Occupancy. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

Vibration sensors on some bridges are presented as occupancy sensors, and so appear here as motion.

## Commands

| Command | What it does |
|---|---|
| **Set Motion** | Forces `motion` to `active` or `inactive`. For testing only — see below. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time to this device and updates `rtt`. |

**Set Motion does not touch the device.** It only changes what Hubitat believes, so you can test a
rule without walking in front of the sensor. The next real report from the device overwrites it.

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable descriptionText logging** | On | One readable log line per state change. |
| **Enable debug logging** | Off | Turns itself off automatically after 24 hours if switched on. |
| **Invert Motion** | Off | Swaps `active` and `inactive`. |

**Invert Motion** exists for sensors — mmWave radars in particular — that report occupancy the wrong
way round, showing active when the room is empty. Switching it also flips the current state
immediately, so the device page does not sit on a stale value until the next report.

Note that debug logging defaults to **off** here, unlike the parent driver.

## Bridge-specific notes

None recorded. Per-bridge results are in the
[compatibility matrix](../compatibility/matrix.md) — occupancy sensors are Confirmed on Tuya and
Aqara bridges.

## Known limitations

- **Motion is reported, not configured.** Sensitivity, blind time, and detection distance stay in
  the sensor's own app; Matter's Occupancy Sensing cluster does not expose them and neither does
  this driver.
- **No timeout of its own.** The device decides when motion becomes inactive. A sensor with a long
  built-in cool-down stays `active` for that whole period, and nothing in Hubitat can shorten it.
- Occupancy is read as a simple active/inactive state. Where a sensor distinguishes occupancy types
  — PIR, ultrasonic, radar — that detail is not published.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
