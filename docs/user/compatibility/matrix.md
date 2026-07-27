# Compatibility matrix

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Two different questions get confused here, so this page answers them separately:

1. **Does the driver support this Matter device type?** Verified against the driver source,
   2026-07-27. The table below is current.
2. **Does my bridge actually expose this device type?** That depends on the bridge, and can only be
   answered by testing. Those results are further down, and most of them date from 2024.

## Driver support by Matter device type

| Matter device type | ID | Clusters | Driver |
|---|---|---|---|
| **Lighting** | | | |
| On/Off Light | `0x0100` | `06` | [Switch](../drivers/switch.md) |
| Dimmable Light | `0x0101` | `06`, `08` | `Generic Component Dimmer` |
| Colour Temperature Light | `0x010C` | `06`, `08`, `0300` | `Generic Component CT` |
| Extended Colour Light | `0x010D` | `06`, `08`, `0300` | `Generic Component RGBW` |
| **Plugs and outlets** | | | |
| On/Off Plug-in Unit | `0x010A` | `06` | [Switch](../drivers/switch.md) |
| Dimmable Plug-in Unit | `0x010B` | `06`, `08` | `Generic Component Dimmer` |
| **Switches and controls** | | | |
| On/Off Light Switch | `0x0103` | `06` | [Switch](../drivers/switch.md) |
| Dimmer Switch | `0x0104` | `06`, `08` | `Generic Component Dimmer` |
| Colour Dimmer Switch | `0x0105` | `06`, `08`, `0300` | `Generic Component RGBW` |
| Generic Switch (buttons, scene switches) | `0x000F` | `3B` | [Button](../drivers/button.md) |
| **Sensors** | | | |
| Contact Sensor | `0x0015` | `45`, optionally `0080` | [Contact Sensor](../drivers/contact-sensor.md) or `Generic Component Contact Sensor` |
| Water Leak Detector | `0x0043` | `45` | `Generic Component Water Sensor` |
| Light Sensor | `0x0106` | `0400` | `Generic Component Omni Sensor` |
| Occupancy Sensor | `0x0107` | `0406` | [Motion Sensor](../drivers/motion-sensor.md) |
| Temperature Sensor | `0x0302` | `0402` | `Generic Component Omni Sensor` |
| Humidity Sensor | `0x0307` | `0405` | `Generic Component Omni Sensor` |
| Pressure Sensor | `0x0305` | `0403` | `Generic Component Pressure Sensor` |
| Air Quality Sensor | `0x002C` | `5B`, `040D`, `042A` | [Air Purifier](../drivers/air-purifier.md) |
| Smoke / CO Alarm | `0x0076` | `5C` | **Not supported** |
| **Closures** | | | |
| Door Lock | `0x000A` | `0101` | [Door Lock](../drivers/door-lock.md) |
| Window Covering | `0x0202` | `0102` | [Window Shade](../drivers/window-shade.md) |
| **HVAC** | | | |
| Thermostat | `0x0301` | `0201` | `Generic Component Thermostat` |
| Fan | `0x002B` | `0202` | `Generic Component Fan Control` |
| Air Purifier | `0x002D` | `0071`, `0072` | [Air Purifier](../drivers/air-purifier.md) |
| **Other** | | | |
| Camera | — | `0551` | [Camera AV Stream](../drivers/camera-av-stream.md) — experimental, Matter 1.3+ |
| Battery reporting | — | `2F` | [Battery](../drivers/battery.md) |

Not supported, and not planned as part of the device-type mapping:

| Feature | Status |
|---|---|
| Composite devices — several sensors as one Hubitat device | Not supported. Each endpoint becomes its own child device. |
| Groups | Not exposed by the Matter bridges tested. |
| Scenes | Not exposed by the Matter bridges tested. |

## Bridge experience

**These results are from 2024 and have not been re-tested.** Treat them as a starting point, not as
a current statement — several bridges have had firmware updates since, and the driver has gained
support for device types that were unsupported when this testing was done.

Labels: **Confirmed** — tested working. **Unsupported** — the bridge does not expose it, or it did
not work. **Unknown** — never tested.

| Device type | Tuya / Zemismart | Aqara | Philips Hue | SwitchBot |
|---|---|---|---|---|
| On/Off Light | Confirmed | Confirmed | Confirmed | Unknown |
| Dimmable Light | Confirmed | Confirmed | Confirmed | Unknown |
| Colour Temperature Light | Confirmed | Confirmed | Confirmed | Unknown |
| Extended Colour Light | Confirmed | Confirmed | Confirmed | Unknown |
| On/Off Plug-in Unit | Confirmed | Confirmed | Confirmed | Unknown |
| Dimmable Plug-in Unit | Unknown | Unknown | Unknown | Unknown |
| On/Off Light Switch | Confirmed | Confirmed | Confirmed | Unknown |
| Dimmer Switch | Unknown — Tuya dimmers were not exported | Unknown | Unknown | Unknown |
| Contact Sensor | Confirmed | Confirmed | Unknown | Unknown |
| Water Leak Detector | Confirmed | Unsupported | Unknown | Unknown |
| Light Sensor | Confirmed | Confirmed — Aqara T1 | Unknown | Unknown |
| Occupancy Sensor | Confirmed | Confirmed | Unknown | Unknown |
| Temperature Sensor | Confirmed | Confirmed | Confirmed | Confirmed |
| Humidity Sensor | Unsupported | Confirmed | Unknown | Confirmed |
| Pressure Sensor | Unsupported | Unsupported | Unknown | Unknown |
| Door Lock | Unsupported | Confirmed | Unknown | Unknown |
| Window Covering | Confirmed | Unknown | n/a | Confirmed |
| Thermostat | Not seen — no thermostat appeared | Confirmed | Unsupported | Unknown |
| Generic Switch (buttons) | Unknown | Unknown | Unknown | Unknown |
| Battery reporting | Unknown | Unknown | Unknown | Unknown |

Per-bridge notes, including which specific devices were tried, are on the bridge pages:
[Aqara](../bridges/aqara.md) ·
[Philips Hue](../bridges/philips-hue.md) ·
[SwitchBot](../bridges/switchbot.md) ·
[Tuya / Zemismart](../bridges/tuya-zemismart.md) ·
[Other bridges](../bridges/other-bridges.md)

## Contributing a result

If you have a device working — or not working — on a bridge marked Unknown above, say so in the
[community thread](../help/support-and-links.md). Name the bridge, its firmware, the device, and the
driver version, and it can be recorded here.
