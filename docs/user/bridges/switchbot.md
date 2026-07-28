# SwitchBot

Applies to: 1.9.0 | Last verified: 2026-07-28 | Status: Current

Devices tested behind the **SwitchBot Hub 2** acting as a Matter bridge. Only a few have been tried,
so most of this bridge is simply unknown rather than unsupported. SwitchBot identifies three other
Matter-capable hubs, but no MAB test results have been recorded for them yet.

Labels: **Confirmed** — tested working. **Unknown** — not tested. See the
[compatibility overview](../compatibility/overview.md).

## Matter-capable hubs

| Hub | SwitchBot Matter status | MAB evidence |
|---|---|---|
| SwitchBot Hub Mini Matter Enabled | Matter bridge | Unknown — not tested with MAB |
| SwitchBot Hub 2 | Matter bridge | Confirmed — tested with MAB |
| SwitchBot Hub 3 | Matter bridge | Unknown — not tested with MAB |
| SwitchBot AI Hub | Matter bridge | Unknown — not tested with MAB |

The original **SwitchBot Hub Mini** is a different product and does **not** support Matter.


## Working

| Device type | Evidence | Notes |
|---|---|---|
| Temperature sensor | Confirmed | Built into the Hub 2 |
| Humidity sensor | Confirmed | Built into the Hub 2 |
| Curtain motor | Confirmed | SwitchBot Curtain Motor |
| Battery level reporting | Confirmed | |

## Untested

Everything else. SwitchBot's range includes locks, buttons, plugs, blind tilt motors, and sensors,
and none has been tried through the bridge — they are **Unknown**, not unsupported.

If you have SwitchBot devices bridged into Hubitat, a report in the
[community thread](../help/support-and-links.md) would fill in the largest gap in this
documentation. Name the device, the hub firmware, and the driver version.

## A note on the SwitchBot Button driver

The package includes a `Matter Generic Component SwitchBot Button` driver, contributed in 2024.
**It is deprecated** — use the [Button](../drivers/button.md) driver instead.

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
- [SwitchBot device Matter compatibility](https://support.switch-bot.com/hc/en-us/articles/38979658026519-SwitchBot-Device-Matter-Compatibility)
- [SwitchBot hub comparison](https://support.switch-bot.com/hc/en-us/articles/13569889726743-Differences-Between-Hub-Mini-Hub-2-Hub-Mini-Matter-Enabled-and-Hub-3)
