# Which driver do I get?

Applies to: 1.8.8 | Last verified: — | Status: Historical

> **Stub — transcribed from source, not yet audited.** Written in phase 1d of the documentation
> migration from `mapMatterCategory()` in `Matter_Advanced_Bridge.groovy`. There was no wiki page to
> migrate. Verify against the current release before treating this as authoritative.

When the parent driver discovers a device behind your Matter bridge, it creates one Hubitat child
device per endpoint and picks a driver for it automatically.

## How the driver is chosen

The choice is made from the **clusters the endpoint reports** (its `ServerList`), *not* from the
Matter device type. Device types are consulted only as tie-breakers in two cases, noted below.

**The order matters.** The selection is a first-match-wins chain, so an endpoint that reports several
of these clusters gets the driver that appears earliest in the table. A colour bulb reports both
`0x0300` and `0x0006`, and lands on `Generic Component RGBW` because colour is checked first.

| # | Cluster | Cluster name | Driver | Namespace |
|---:|---|---|---|---|
| 1 | `0x0071` / `0x0072` | HEPA / Activated Carbon Filter Monitoring | [Air Purifier](air-purifier.md) | kkossev |
| 2 | `0x0300` | Color Control | `Generic Component RGBW` if the device type is Extended Color Light (`0x010D`), otherwise `Generic Component CT` | hubitat |
| 3 | `0x0008` | Level Control | `Generic Component Dimmer` | hubitat |
| 4 | `0x0045` | Boolean State | See "Boolean State" below | both |
| 5 | `0x005B` | Air Quality | [Air Purifier](air-purifier.md) | kkossev |
| 6 | `0x0101` | Door Lock | [Door Lock](door-lock.md) | kkossev |
| 7 | `0x0102` | Window Covering | [Window Shade](window-shade.md) | kkossev |
| 8 | `0x0201` | Thermostat | `Generic Component Thermostat` | hubitat |
| 9 | `0x0202` | Fan Control | `Generic Component Fan Control` | hubitat |
| 10 | `0x0400` | Illuminance Measurement | `Generic Component Omni Sensor` | hubitat |
| 11 | `0x0402` | Temperature Measurement | `Generic Component Omni Sensor` | hubitat |
| 12 | `0x0403` | Pressure Measurement | `Generic Component Pressure Sensor` | hubitat |
| 13 | `0x0405` | Relative Humidity Measurement | `Generic Component Omni Sensor` | hubitat |
| 14 | `0x0406` | Occupancy Sensing | [Motion Sensor](motion-sensor.md) | kkossev |
| 15 | `0x040D` | Carbon Dioxide Concentration | [Air Purifier](air-purifier.md) | kkossev |
| 16 | `0x042A` | Concentration Measurement | [Air Purifier](air-purifier.md) | kkossev |
| 17 | `0x0090` / `0x0091` | Electrical Power / Energy Measurement | [Power Energy](power-energy.md) | kkossev |
| 18 | `0x0006` | On/Off | [Switch](switch.md) | kkossev |
| 19 | `0x003B` | Switch (button) | [Button](button.md) | kkossev |
| 20 | `0x002F` | Power Source | [Battery](battery.md) | kkossev |
| 21 | `0x0551` | Camera AV Stream Management (Matter 1.3+) | [Camera AV Stream](camera-av-stream.md) | kkossev |
| — | *no match* | | `Generic Component Switch` | hubitat |

### Boolean State (`0x0045`)

This cluster covers both contact and water leak sensors, so the device type decides:

| Device type | Also reports | Driver |
|---|---|---|
| `0x0043` Water Leak Detector | — | `Generic Component Water Sensor` (hubitat) |
| `0x0015` Contact Sensor | `0x0080` Boolean State Configuration | [Contact Sensor](contact-sensor.md) (kkossev) — adds `sensitivityLevel` |
| `0x0015` Contact Sensor | — | `Generic Component Contact Sensor` (hubitat) |
| ambiguous / absent | `0x0080` | [Contact Sensor](contact-sensor.md) (kkossev) |
| ambiguous / absent | — | `Generic Component Contact Sensor` (hubitat) |

The custom driver is used when the device also reports `0x0080`, which is what allows the
sensitivity setting. Aqara P100 is the known example.

### Devices that fall through

An endpoint reporting none of the clusters above gets `Generic Component Switch` with a product name
of `Unknown`. That is a fallback, not a statement that the device works.

## Changing the driver manually

Some devices are ambiguous at discovery time and you may want a different driver than the one chosen.
Change the **Type** field on the child device in Hubitat, then click **Save Device**.

The documented case is a bulb that reports hue and saturation but was assigned
`Generic Component CT`. Switching it to `Generic Component RGBW` gives you full colour control; the
parent logs an info message when it detects this situation.

Changing a child to a driver that does not match its clusters will not make unsupported features
work — the parent only sends what the device actually reports.

## The drivers

**Custom drivers** shipped in this package, namespace `kkossev`:

[Air Purifier](air-purifier.md) ·
[Battery](battery.md) ·
[Button](button.md) ·
[Camera AV Stream](camera-av-stream.md) ·
[Contact Sensor](contact-sensor.md) ·
[Door Lock](door-lock.md) ·
[Motion Sensor](motion-sensor.md) ·
[Power Energy](power-energy.md) ·
[Signal](signal.md) ·
[Switch](switch.md) ·
[SwitchBot Button](switchbot-button.md) ·
[Window Shade](window-shade.md)

**Parent driver:** [Matter Advanced Bridge](matter-advanced-bridge.md)

**Hubitat stock drivers** used by this package: see [Hubitat stock drivers](stock-drivers.md).

> `Matter Custom Component Signal` and `Matter Generic Component SwitchBot Button` are not assigned
> by the cluster chain above. Their assignment path needs to be documented in phase 2.
