# Matter Generic Component Switch

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

The driver for switches, plugs, relays, and any other on/off device behind the bridge. It is also
the fallback for devices the package does not recognise.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Switch.groovy` (driver version 1.1.2)
- **Assigned to:** endpoints reporting cluster `0x0006` On/Off — Matter device types On/Off Light
  (`0x0100`), On/Off Plug-in Unit (`0x010A`), and On/Off Light Switch (`0x0103`)

A device that also reports a colour or level cluster gets a bulb driver instead; on/off is checked
late in the chain. See [Which driver do I get?](index.md).

## Capabilities

`Actuator` · `Switch` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `switch` | `on`, `off` | From cluster `0x0006`. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

Duplicate reports are filtered: if the bridge reports `on` and the device is already `on`, no event
is sent. With debug logging on, those appear as `ignored switch event`.

## Commands

| Command | What it does |
|---|---|
| **On** | Turns the device on. |
| **Off** | Turns the device off. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time to this device and updates `rtt`. |

Commands go to the parent driver, which sends them to the bridge. This driver never talks to the
device directly.

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per state change. |

## Bridge-specific notes

None recorded. On/Off is the most consistently implemented Matter cluster, and there is nothing
bridge-specific in this driver's code. Per-bridge results are in the
[compatibility matrix](../compatibility/matrix.md).

## Known limitations

- **A device with this driver and the product name `Unknown` is not necessarily a switch.** This is
  the fallback for an endpoint reporting nothing the package recognises, and its On and Off buttons
  will probably do nothing useful. See [Which driver do I get?](index.md).
- The state shown is what the bridge last reported. A device that does not report back after a
  command leaves Hubitat showing the previous state — use **Refresh**.
- No power monitoring. A smart plug that measures power exposes it on a separate endpoint, which
  becomes its own child device with the [Power Energy](power-energy.md) driver.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
