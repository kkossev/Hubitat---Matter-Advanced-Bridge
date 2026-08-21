# Matter Custom Component Signal

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Experimental

A small driver that turns a motion report into a button press, for use with **Aqara** Matter
bridges.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Custom_Component_Signal.groovy` (driver version 1.1.2)
- **Assigned to:** nothing automatically — **you assign this one by hand**

Nothing in the discovery process ever chooses this driver. Assign it to an existing child device by
changing its **Type** in Hubitat and clicking **Save Device**.

## What it is for

When the device reports motion as `active`, this driver sends **both** a `motion` event and a
`pushed` button event. That makes a device usable as a trigger in apps that expect a button, rather
than only as a motion sensor.

The `inactive` report is deliberately ignored: a button press has no "un-press", so only the leading
edge produces an event.

## Capabilities

`MotionSensor` · `PushableButton` · `Sensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `motion` | `active`, `inactive` | Passed through from the device. |
| `pushed` | `1` | Sent whenever motion goes `active`. |
| `numberOfButtons` | `1` | Fixed. |
| `currentPosition` | string | Reported by the device where it sends one. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

## Commands

| Command | What it does |
|---|---|
| **Push** | Sends a `pushed` event without the device doing anything. For testing rules. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per event. |

## Bridge-specific notes

Intended for devices behind an **Aqara** Matter bridge.

> **Which Aqara devices?** Still not confirmed. The community thread documents Aqara **Signals**
> (see [Aqara Signals](../bridges/aqara-signals.md)) as a mechanism, but every case found there —
> FP2 presence zones — is state-based (active/inactive) and works with an ordinary Motion Sensor,
> not this driver. This driver exists for a *momentary* report (motion `active` with no meaningful
> `inactive`). If you are using it successfully, please say which device in the
> [community thread](../help/support-and-links.md) so this page can name it.

## Known limitations

- **Assign it deliberately.** It is not offered by discovery, and assigning it to a device that does
  not report motion will produce nothing.
- One button only. Multi-button devices should use the [Button](button.md) driver.
- No timeout or debounce. Every `active` report is another button press, so a sensor that reports
  repeatedly during sustained motion will push repeatedly.

## See also

- [Which driver do I get?](index.md)
- [Button](button.md)
