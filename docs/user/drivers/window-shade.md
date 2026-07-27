# Matter Generic Component Window Shade

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Curtain motors, roller blinds, and shades.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Window_Shade.groovy` (driver version 1.2.x)
- **Assigned to:** endpoints reporting cluster `0x0102` Window Covering — Matter device type Window
  Covering (`0x0202`)

This driver has more settings than any other in the package, because curtain motors — Zemismart's in
particular — vary in how they report position.

## Capabilities

`Actuator` · `WindowShade` · `Switch` · `SwitchLevel` · `Battery` · `Refresh`

`Switch` and `SwitchLevel` are there so the shade can be used in apps and dashboards that expect a
switch: on opens, off closes, and level sets position.

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `windowShade` | `open`, `closed`, `opening`, `closing`, `partially open`, `unknown` | |
| `position` | 0–100 | **100 is fully open, 0 is closed** — the Hubitat convention. |
| `targetPosition` | 0–100 | Where the motor is heading. Some Zemismart motors update only this, not `position`. |
| `operationalStatus` | number | Whether the motor is moving, and in which direction. |
| `battery`, `batteryVoltage` | number | For battery-powered motors. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

Aqara E1 blinds report extra battery detail — `batStatus`, `batChargeLevel`,
`batReplacementNeeded`, and several more. They are published as-is when the device sends them.

## Commands

| Command | What it does |
|---|---|
| **Open** / **Close** | Fully open or close. |
| **Set Position** | Move to a position, 0–100. |
| **Start Position Change** / **Stop Position Change** | Begin moving in a direction, and stop. |
| **On** / **Off** / **Set Level** | The `Switch` and `SwitchLevel` equivalents of open, close, and set position. |
| **Initialize** | Re-reads every attribute. Use after changing the settings below. |
| **Get Info** | Writes device details to the live logs and device data. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Maximum travel time** | 15 s | How long a full open or close takes. Used to estimate progress while moving. |
| **Position delta** | 5 | How close to the target counts as arrived. |
| **Reverse Position Reports** | **On** | Inverts reported position — 0 becomes 100. On by default, because that is what the common Zemismart motors need. |
| **Substitute Open/Close w/ setPosition** | Off | Sends a position instead of an open or close command, for motors that ignore open and close. |
| **Reverse Target and Current Position** | Off | Swaps the two, for motors that report movement in `targetPosition` and leave `position` unchanged. |
| **Enable descriptionText logging** | On | One readable log line per change. |
| **Enable debug logging** | Off | Turns itself off automatically after 24 hours if switched on. |

**If your shade shows open when it is closed, turn Reverse Position Reports off.** It defaults to on
for the Zemismart motors most people have, so a standards-compliant motor may need it off. Toggling
it re-reads the position immediately.

The other two reversal settings are for specific non-standard motors, and should be left off unless
your shade misbehaves in exactly the way they describe.

## Bridge-specific notes

- **Zemismart** motors are the reason three of these preferences exist. Start with the defaults; if
  position is wrong or stuck, work through the reversal settings one at a time, clicking
  **Initialize** after each change.
- **Aqara E1** blinds report the extended battery attributes listed above.
- Window coverings are Confirmed on Tuya and SwitchBot bridges in the
  [compatibility matrix](../compatibility/matrix.md).

## Known limitations

- **Tilt is not supported.** Venetian blinds that can tilt as well as raise expose that separately,
  and this driver does not implement it.
- Movement progress between the start and the end of a travel is estimated from **Maximum travel
  time**, not reported by the motor. If the estimate is wrong, correct that setting.
- A motor that reports neither `position` nor `targetPosition` while moving will jump straight from
  the old position to the new one.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
