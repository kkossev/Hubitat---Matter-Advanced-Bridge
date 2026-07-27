# Other bridges

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Bridges outside the four with their own pages —
[Aqara](aqara.md), [Philips Hue](philips-hue.md), [SwitchBot](switchbot.md), and
[Tuya / Zemismart](tuya-zemismart.md).

## IKEA DIRIGERA

**In use with this driver.** The DIRIGERA hub works as a Matter bridge and is one of the hubs the
driver is developed against — the screenshots in
[Commands and states](../configuration/commands-and-states.md) are from a DIRIGERA.

Device-by-device results have not been catalogued yet. Reports welcome.

## Other brands

These were identified in 2024 as having, or planning, a Matter bridge. **None has been tested with
this driver**, and the list has not been revisited since — some may have shipped, changed, or been
discontinued.

| Brand | Product |
|---|---|
| Mediola | [Matter bridge](https://matter-bridge.com/en/faq-en) for Homematic IP |
| Ubisys | [Gateway G1](https://smarthome-store.de/gateway-g1.html) |
| Schneider Electric | [Wiser gateway](https://matter-smarthome.de/en/development/what-ifa-2022-taught-us-about-matter/) |
| Bosch | [Smart Home controller](https://www.digitalzimmer.de/artikel/news/bosch-smart-home-neuer-controller-auch-fuer-matter/) |
| Third Reality | [Matter bridge MZ1](https://www.3reality.com/matter-solution) |

## Trying an untested bridge

There is nothing bridge-specific in the discovery process, so an unlisted bridge is worth trying —
it either exposes its devices over Matter or it does not, and discovery will tell you within a few
minutes. Follow [Installation](../getting-started/installation.md) as normal.

If it works, or if it does not, please report it in the
[community thread](../help/support-and-links.md) with the bridge model, its firmware, and what
appeared. That is how these pages get written.

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Using it with Matter devices](../getting-started/use-with-matter-devices.md) — for Matter devices
  paired directly, without a bridge
