# Matter Generic Component Button

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Buttons, scene switches, and remotes.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Button.groovy` (driver version 1.1.1)
- **Assigned to:** endpoints reporting cluster `0x003B` Switch — Matter device type Generic Switch
  (`0x000F`)

This driver also replaces the deprecated [SwitchBot Button](switchbot-button.md).

## Capabilities

`PushableButton` · `HoldableButton` · `ReleasableButton` · `DoubleTapableButton` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `pushed` | button number | A single press. |
| `held` | button number | The button was pressed and held. |
| `released` | button number | Released after being held. |
| `doubleTapped` | button number | Two presses in quick succession. |
| `numberOfButtons` | number | How many buttons the device has, from the Matter cluster. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

## What your device sends

Matter devices differ in which button events they support, and the driver adapts to each one. It
reads the device's FeatureMap and follows it rather than guessing:

| Your device supports | What happens |
|---|---|
| **Multi-press (MSM)** | The driver waits for the end of the press sequence, so it can tell one press from two. One press sends `pushed`, two sends `doubleTapped`. |
| **Long press (MSL)**, no multi-press | A short release sends `pushed`; a long press sends `held`, and letting go sends `released`. |
| **Neither** | Every release sends `pushed`. |

This is why a double tap on one remote produces `doubleTapped` and on another produces two `pushed`
events — it depends on what the device itself reports.

Where a device sends multi-press events without declaring multi-press support, the FeatureMap wins
and those events are ignored, to avoid firing a rule twice for one press.

## Commands

| Command | What it does |
|---|---|
| **Get Info** | Writes details about the device to the live logs and its device data. Useful when button events are not behaving. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time to this device and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. Useful here: every button event is logged with the reasoning behind it. |
| **Enable descriptionText logging** | On | One readable log line per button event. |

## Bridge-specific notes

Button events depend on the bridge passing Matter *events* through, not just attributes. Support was
fixed in driver 1.5.6 — see the [revision history](../project/revisions-history.md). Per-bridge
results are Unknown in the [compatibility matrix](../compatibility/matrix.md); reports are welcome.

## Known limitations

- **Three or more presses are not reported to Hubitat.** A triple press is written to the log with
  its count, but only single and double press produce events, because Hubitat has no attribute for
  longer sequences.
- **`supportedButtonValues` is not published.** Hubitat apps cannot tell in advance which events
  this device will produce, so a rule may offer choices your button never sends.
- The `latched` attribute is declared but not implemented — latching switches are not supported.
- Button numbering follows the device's own position numbering. On a multi-button remote, which
  physical button is number 1 is the manufacturer's decision.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
