# Tuya / Zemismart

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Devices tested behind the **Zemismart M1 Hub** acting as a Matter bridge. The same Tuya platform
powers the Zemismart M6, MOES, GIRIER, and other rebadged gateways, so results generally carry
across — but firmware differs, so treat them as a guide.

The device results date from 2024; where the driver has since changed, the entry says so.

Labels: **Confirmed** — tested working. **Unsupported** — the bridge does not expose it, or it does
not work. **Unknown** — not tested. See the [compatibility overview](../compatibility/overview.md).

## Working

| Device type | Evidence | Notes |
|---|---|---|
| Plugs, relays, switches — on/off | Confirmed | |
| Bulbs — on/off, level, colour temperature, RGBW | Confirmed | |
| Motion sensors | Confirmed | Not every motion sensor is exposed — see below. |
| mmWave presence sensors | Confirmed | Moes, Linptech |
| Contact sensors | Confirmed | |
| Temperature sensors | Confirmed | |
| Light sensors | Confirmed | Tuya light sensor |
| Curtain motor | Confirmed | Zemismart Curtain Motor. See [Window Shade](../drivers/window-shade.md) — the position settings exist for these motors. |
| Tuya valve | Confirmed | Appears as a switch |
| Fingerbot | Confirmed | Appears as a switch |

## Needs re-testing

| Device type | 2024 result | Now |
|---|---|---|
| Water leak sensor | Worked, but appeared as a contact sensor | Water leak detectors have been identified correctly since driver 1.6.0, and get Hubitat's water sensor driver. |
| Wireless remote switches and buttons — Tuya TS0044 scene switch, Tuya button | "Not implemented yet" | Button support was fixed in driver 1.5.6. See [Button](../drivers/button.md). |
| Battery level reporting | "Not implemented yet" | A [Battery](../drivers/battery.md) driver exists. Battery is untested on every bridge and is the first thing worth checking. |

## Known problems

**These are 2024 findings and have not been re-tested.** Tuya gateway firmware updates frequently,
and what the bridge shares over Matter can change with it.

| Device type | Evidence | Detail |
|---|---|---|
| Humidity sensors | Unsupported | Reported a fixed 40% in 2024, believed to be a bridge firmware bug. Worth re-testing on current firmware. |
| Tuya dimmers | Unsupported | Not exposed over Matter — GIRIER TS0110F, Tuya TS0601 |
| Thermostats | Unsupported | Not exposed over Matter — AVATTO thermostat |

## Devices the bridge does not share

As of 2024, some mmWave sensors were not exposed over Matter at all. Not re-tested since:

- the large black radar with the LED indicator
- TS0601 `_TZE200_ikvncluo`
- TS0601 `_TZE204_kapvnnlk`

## Untested

| Device type | Evidence |
|---|---|
| Gas detector, smoke detector | Unknown |
| Vibration sensor | Unknown |
| TRVs | Unknown |
| Door lock | Unknown — locks are supported by the driver since 1.5.5, but none has been tried on this bridge |

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
