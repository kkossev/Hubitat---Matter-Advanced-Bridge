# Philips Hue

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Devices tested behind the **Philips Hue Hub** acting as a Matter bridge. The device results date
from 2024; where the driver has since changed, the entry says so.

Labels: **Confirmed** — tested working. **Unsupported** — the bridge does not expose it, or it does
not work. **Unknown** — not tested. See the [compatibility overview](../compatibility/overview.md).

## Working

| Device type | Evidence | Notes |
|---|---|---|
| Bulbs — on/off, level, colour temperature, RGBW | Confirmed | All bulbs tested |
| Plugs | Confirmed | |
| Motion sensor | Confirmed | Hue motion sensor |
| Temperature sensor | Confirmed | The temperature part of the Hue motion sensor |

Hue does not report colour mode back when a bulb is changed from another system, so Hubitat may
show a stale colour mode until the bulb reports again.

## Needs re-testing

| Device type | 2024 result | Now |
|---|---|---|
| Hue Dimmer v2 | "Not implemented yet" | Button support was fixed in driver 1.5.6. See [Button](../drivers/button.md). |
| Battery level reporting | "Not implemented yet" | A [Battery](../drivers/battery.md) driver exists. |
| Contact sensors | Expected to work, never tested | Contact sensors are supported. |

## Not exposed by the bridge

**These are 2024 findings and have not been re-tested.** Hue bridge firmware has moved on since, and
what a bridge shares over Matter can change with it.

| Feature | Evidence |
|---|---|
| Hue scenes | Unsupported — not exposed over Matter |
| Hue groups | Unsupported — not exposed over Matter |

Scenes and groups stay in the Hue app. Hubitat sees the individual bulbs, so the equivalent has to
be built with Hubitat groups and scenes.

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
