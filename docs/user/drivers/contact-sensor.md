# Matter Custom Component Contact Sensor

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Door and window contact sensors **that also support adjustable sensitivity**. A contact sensor
without that support gets Hubitat's stock `Generic Component Contact Sensor` instead, and works just
as well — this driver exists only to add the sensitivity setting.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Custom_Component_Contact_Sensor.groovy` (driver version 1.0.1)
- **Assigned to:** endpoints reporting cluster `0x0045` Boolean State **and** cluster `0x0080`
  Boolean State Configuration — Matter device type Contact Sensor (`0x0015`)

The Aqara P100 is the known example. See [Which driver do I get?](index.md) for the full rule,
including how water leak sensors are told apart from contact sensors.

## Capabilities

`Sensor` · `ContactSensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `contact` | `open`, `closed` | From cluster `0x0045`. |
| `sensitivityLevel` | number | The level the device is currently using. |
| `supportedSensitivityLevels` | number | How many levels the device has. Valid levels are `0` to this value minus one. |
| `defaultSensitivityLevel` | number | The factory default level. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

The levels are raw indexes, not units. What each one means — and whether higher is more or less
sensitive — is the device manufacturer's decision, so check the sensor's own documentation.

## Commands

| Command | What it does |
|---|---|
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time to this device and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Sensitivity Level (raw index)** | Blank | Writes a new sensitivity level to the device. Leave blank to change nothing. |
| **Enable descriptionText logging** | On | One readable log line per state change. |
| **Enable debug logging** | Off | Turns itself off automatically after 24 hours if switched on. |

**Setting the sensitivity actually writes to the device**, unlike most preferences in this package.
Check `supportedSensitivityLevels` on the device page first: a value outside `0` to
`supportedSensitivityLevels - 1` is rejected with a warning in the log rather than sent. Saving a
value that matches the current level sends nothing.

The preference is kept in step with the device: when the sensor reports its level, the preference is
updated to match, so it always shows what the device is really using.

## Bridge-specific notes

None recorded. Contact sensors are Confirmed on Tuya and Aqara bridges — see the
[compatibility matrix](../compatibility/matrix.md).

## Known limitations

- **Sensitivity is the only extra setting.** Cluster `0x0080` also covers alarm configuration, which
  is not implemented.
- The write is sent as soon as you save, but confirmation comes only when the device reports a
  changed `sensitivityLevel`. If the value on the device page does not follow, the sensor did not
  accept it.
- Battery, if the sensor reports it, arrives as a separate child device with the
  [Battery](battery.md) driver.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
