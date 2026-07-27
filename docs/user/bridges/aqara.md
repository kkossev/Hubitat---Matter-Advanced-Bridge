# Aqara

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Devices tested behind the **Aqara E1 Hub** acting as a Matter bridge. The device results date from
2024; where the driver has since changed, the entry says so.

Labels: **Confirmed** — tested working. **Unsupported** — the bridge does not expose it, or it does
not work. **Unknown** — not tested. See the [compatibility overview](../compatibility/overview.md).

## Working

| Device type | Evidence | Tested with |
|---|---|---|
| Relays, on/off | Confirmed | Aqara Double Rocker H1 EU |
| Plugs, on/off | Confirmed | Aqara Smart Plug EU |
| Bulbs — on/off, level, colour temperature | Confirmed | Aqara LED Strip T1 |
| Bulbs — RGBW | Confirmed | Aqara LED Strip T1. Colour mode was reported incorrectly by this device in 2024. |
| Motion sensors | Confirmed | Aqara P1, Xiaomi |
| Vibration sensors | Confirmed | Aqara Vibration Sensor — appears as a **motion** sensor |
| Presence sensor | Confirmed | Aqara FP1E (2024 model) |
| Temperature and humidity | Confirmed | Aqara T1 sensor, Aqara TVOC sensor |
| Light sensor | Confirmed | Aqara Light Detection Sensor T1 |
| Curtain motor | Confirmed | Aqara Curtain Motor |
| Thermostat | Confirmed | Aqara E1 TRV |
| Battery level | Confirmed | Either as its own child device, or as part of an existing one |
| Aqara Cube T1 Pro | Confirmed | Exposed as six separate on/off devices |

## Needs re-testing

The driver has gained support for these since the bridge was last tested. The old result describes
what the **driver** could do at the time, not what the bridge exposes.

| Device type | 2024 result | Now |
|---|---|---|
| Door lock — Aqara U100 | "Locks not supported in HE" | Locks have worked since driver 1.5.5; lock codes are experimental. See [Door Lock](../drivers/door-lock.md). |
| Wireless remote switches and buttons — Aqara Double Rocker H1, Xiaomi/Lumi | "Buttons not supported in HE" | Button support was fixed in driver 1.5.6. See [Button](../drivers/button.md). |
| PM 2.5 — Aqara TVOC sensor | "Not supported in HE" | `pm25` has been supported since driver 1.3.0. See [Air Purifier](../drivers/air-purifier.md). |
| Smart pet feeder | Partially working — motion only | It depended on lock and button support, both of which now exist. |
| Door and window sensor — Aqara, and T1 | Never tested | Contact sensors are supported, including sensitivity on models that report it. |

## Not exposed by the bridge

Limitations of the Aqara bridge rather than of the driver. Nothing on the Hubitat side can change
them.

**These are 2024 findings and have not been re-tested.** Aqara has released bridge firmware since,
and a device listed here may have been added in the meantime. If one of them now appears in Hubitat,
please say so in the [community thread](../help/support-and-links.md).

| Device type | Evidence |
|---|---|
| Presence sensors FP1 and FP2 | Unsupported — not exposed over Matter |
| Water leak sensors | Unsupported — not exposed over Matter |
| Atmospheric pressure — Aqara TVOC sensor | Unsupported — not exposed over Matter |
| Light Detector T1, and the Xiaomi/Lumi light sensor | Unsupported — not exposed over Matter |

## Not supported, or untested

| Device type | Evidence |
|---|---|
| Smoke detectors — Aqara Smart Smoke Detector | Unsupported. Matter's Smoke/CO Alarm device type is not implemented. |
| Aqara dimmers | Unknown — not tested |
| Smart natural gas detector | Unknown — not tested |

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
