# Preferences

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

The settings on the **parent** Matter Advanced Bridge device page. Click **Save Preferences** after
changing any of them.

Child devices have their own, much shorter preference lists — usually just the two logging
switches. See the individual [driver pages](../drivers/index.md).

![The Preferences tab, with Advanced Options switched on](../assets/images/configuration-preferences-01.png)

The two links at the top — **Wiki page** in the first tile and **Community Link** in the green box —
both show the installed driver version underneath. That is the quickest way to check which version
you are running.

**Defaults apply to new installations.** A device that was set up under an older release keeps
whatever it had; the screenshot above shows 1x and Minimize State Variables off, both of which
differ from the current defaults. If a default below matters to you, check the actual value on your
own device rather than assuming.

## Basic preferences

These are always visible.

| Preference | Default | What it does |
|---|---|---|
| **Enable descriptionText logging** | On | Logs a readable line for each event, such as a switch turning on. Safe to leave on. |
| **Enable debug logging** | On | Detailed logging for diagnosing problems. **Switches itself off automatically after 24 hours.** |
| **Advanced Options** | Off | Reveals the advanced preferences below. They are set to sensible values already — you do not need to open this section for normal use. |

## Preferences added by Hubitat

Two more appear at the bottom of the tab. They come from the Hubitat platform and behave the same
way on every device, not just this one:

- **Default Current State** — which state the device tile shows by default. `healthStatus` is a
  sensible choice for a bridge.
- **Show on Home page** — whether the device appears on the Home page and counts towards the quick
  status bar totals.

## Advanced preferences

Visible only when **Advanced Options** is on.

### Health check

| Preference | Default | What it does |
|---|---|---|
| **Healthcheck Method** | Periodic polling | How the driver decides whether the bridge is still there. `Periodic polling` pings the bridge on a timer. `Activity check` only watches for incoming traffic and does not ping. `Disabled` turns the check off — and **removes the `healthStatus` state from the device page**. |
| **Healthcheck Interval** | Every 15 Mins | How often that check runs. The choices are every minute (not recommended), 15 or 30 minutes, or 1, 4, or 12 hours. Three consecutive failures set `healthStatus` to `offline`. |

### Discovery and subscriptions

| Preference | Default | What it does |
|---|---|---|
| **Discovery timeout scale** | 2x | Multiplies the timeouts and delays used during discovery. Increase it if discovery stalls or finishes incomplete on a slow bridge, or one with battery devices behind it. Choices are 1x, 2x, and 3x. |
| **Clean subscribe minimum reporting interval** | 1 second | The fastest rate at which the bridge is allowed to report a subscribed attribute. 1 second is also the enforced floor. |
| **Clean subscribe maximum reporting interval** | 600 seconds | The longest the bridge may go without reporting a subscribed attribute, even if nothing changed. Effectively a keep-alive. |
| **Spammy attributes minimum reporting interval** | 0 | `0` keeps every attribute in one subscription. Set a value in seconds to move attributes marked as spammy — ones that report very frequently — into a second, slower subscription. **Changing this re-subscribes automatically**, a second or so after you save. |

### Maintenance

| Preference | Default | What it does |
|---|---|---|
| **Enable trace logging** | Off | Very detailed logging, well beyond debug. Switches itself off automatically — see the note below. |
| **Minimize State Variables** | On | Prunes the State Variables to keep them small. Switching it from off to on prunes them immediately. |

## Notes

**Debug logging is on by default** and turns itself off 24 hours after you last save preferences.
Leaving it on permanently will fill your logs; if you have finished diagnosing something, turn it
off rather than waiting.

**Trace logging turns off after 2 hours**, despite the on-screen description saying 30 minutes. The
code is the authority here — the description text has not kept up.

**The "1x" discovery timeout scale is labelled "(default)" but is not the driver's default** — that
is 2x. The label is left over from when 1x was the default, so do not read it as a description of
what you are currently running.

## Next steps

- [Commands and states](commands-and-states.md)
- [Troubleshooting](../help/troubleshooting.md)
