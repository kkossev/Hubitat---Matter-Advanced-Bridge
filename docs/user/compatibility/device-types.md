# Device types

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

What kinds of device this package understands. **Supported here means the driver implements the
Matter cluster involved** — whether your particular device works also depends on what your bridge
exposes. For device-by-device experience, see the
[compatibility matrix](matrix.md) and the individual [bridge pages](../bridges/aqara.md).

Which driver a device ends up with is decided by the clusters it reports, not by its Matter device
type. [Which driver do I get?](../drivers/index.md) has the full assignment table.

## Lights and switches

| Device type | Matter cluster | Driver |
|---|---|---|
| Switches, plugs, relays | `0x0006` On/Off | [Switch](../drivers/switch.md) |
| Dimmers and dimmable bulbs | `0x0008` Level Control | `Generic Component Dimmer` |
| Colour temperature bulbs | `0x0300` Color Control | `Generic Component CT` |
| RGBW / extended colour bulbs | `0x0300` Color Control | `Generic Component RGBW` |

## Sensors

| Device type | Matter cluster | Driver |
|---|---|---|
| Contact sensors | `0x0045` Boolean State | [Contact Sensor](../drivers/contact-sensor.md) or `Generic Component Contact Sensor` |
| Water leak sensors | `0x0045` + device type `0x0043` | `Generic Component Water Sensor` |
| Motion / occupancy sensors | `0x0406` Occupancy Sensing | [Motion Sensor](../drivers/motion-sensor.md) |
| Temperature sensors | `0x0402` | `Generic Component Omni Sensor` |
| Humidity sensors | `0x0405` | `Generic Component Omni Sensor` |
| Illuminance sensors | `0x0400` | `Generic Component Omni Sensor` |
| Pressure sensors | `0x0403` | `Generic Component Pressure Sensor` |
| Buttons and scene switches | `0x003B` Switch | [Button](../drivers/button.md) |
| Battery reporting | `0x002F` Power Source | [Battery](../drivers/battery.md) |

Contact sensors that also report `0x0080` Boolean State Configuration get the custom driver, which
adds a `sensitivityLevel` setting. Aqara P100 is the known example.

## Climate and air

| Device type | Matter cluster | Driver |
|---|---|---|
| Thermostats | `0x0201` | `Generic Component Thermostat` |
| Fans | `0x0202` Fan Control | `Generic Component Fan Control` |
| Air quality sensors | `0x005B` Air Quality | [Air Purifier](../drivers/air-purifier.md) |
| CO₂ sensors | `0x040D` | [Air Purifier](../drivers/air-purifier.md) |
| Other concentration sensors | `0x042A` | [Air Purifier](../drivers/air-purifier.md) |
| Air purifiers with filter monitoring | `0x0071` HEPA, `0x0072` Activated Carbon | [Air Purifier](../drivers/air-purifier.md) |

## Everything else

| Device type | Matter cluster | Driver | Notes |
|---|---|---|---|
| Window coverings, blinds, curtain motors | `0x0102` Window Covering | [Window Shade](../drivers/window-shade.md) | |
| Door locks | `0x0101` Door Lock | [Door Lock](../drivers/door-lock.md) | Locking and unlocking work. PIN code and user management is experimental — see [Known issues](../help/known-issues.md). |
| Energy monitoring | `0x0090` Power, `0x0091` Energy | [Power Energy](../drivers/power-energy.md) | |
| Cameras | `0x0551` Camera AV Stream | [Camera AV Stream](../drivers/camera-av-stream.md) | Matter 1.3 and later. Experimental. |

## What is not supported

- **A device reporting none of the clusters above** becomes a child device with the
  `Generic Component Switch` driver and a product name of `Unknown`. That is a fallback, not a claim
  that it works.
- **Composite devices.** A device with several sensors arrives as several child devices — one per
  endpoint — rather than one device with several attributes.
- Support for a cluster does not mean every attribute and command in it is implemented. The driver
  subscribes to the attributes that map onto Hubitat capabilities.
