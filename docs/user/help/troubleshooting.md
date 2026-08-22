# Troubleshooting

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Work through the symptom that matches yours. Most problems are one of three things: the bridge is
not reachable, the subscriptions have been lost, or the device was never discovered in the first
place.

## First, check these

Open the parent device page and look at the [Current States](../configuration/commands-and-states.md):

| What you see | What it means |
|---|---|
| `healthStatus: online`, recent `rtt` | The hub can talk to the bridge. The problem is further down. |
| `healthStatus: offline` | Three consecutive checks failed. Start with [The bridge is offline](#the-bridge-is-offline). |
| `networkStatus: offline` | The Hubitat platform itself has lost the bridge. This forces `healthStatus` offline too. |
| A high `initializeCtr` | Repeated re-initialisation, which usually means an unstable connection. |

Then click **Ping**. If `rtt` updates with a number, the bridge is answering right now. If it shows
`timeout`, it is not — the driver waits 15 seconds before giving up.

## The bridge is offline

**Likely causes:** the bridge lost power or network, its IP address changed, or it was reset.

1. Check the bridge in its own manufacturer's app. If that app cannot see it either, the problem is
   not on the Hubitat side.
2. Compare the `ipAddress` state with the address the bridge actually has now. Matter uses mDNS, so
   a changed address is usually picked up automatically — but not always.
3. Once the bridge is back, click **Ping**. If `rtt` returns a number, the connection is restored.

**Power-cycling a Matter bridge or device no longer requires Re Subscribe.** Subscriptions survive a
restart on current Hubitat platform versions. If updates really have stopped after a restart, treat
it as the next symptom.

## Child devices have stopped updating

The bridge is online, but a device's state is stale — a contact sensor stuck `closed`, a switch that
never changes.

1. Click **Refresh** on the parent. This re-reads everything and forces an event even if the value
   has not changed. Log lines from it carry a `[refresh]` tag.
2. If Refresh brings the correct value in but normal updates still do not arrive, the subscription
   is the problem: click **Re Subscribe**.
3. If Refresh logs `no attributes to refresh!`, nothing has been discovered yet — run
   **_DiscoverAll**.
4. If the device still accepts commands and reports `networkStatus: online`, yet its state never
   follows — a lock that locks and unlocks on demand but whose `lock` attribute never moves — the
   endpoint information and subscriptions are out of step. **_DiscoverAll** rebuilds both and keeps
   the existing child devices, so dashboards and rules survive it. This is the fix when Refresh and
   Re Subscribe have both failed, and it is worth trying before removing anything.

## Discovery finds nothing, or stops early

**Expected logs:** discovery always pings the bridge first. If you see
`the Matter Bridge did not respond after N attempts - discovery aborted!`, the bridge never
answered, and nothing else in the log matters. If you see
`the hub reports networkStatus 'offline' for this bridge - the discovery will most probably fail`,
fix that first.

Otherwise, repeated `timeout waiting for the attribute value (retry=N)` lines mean the bridge is
answering too slowly:

1. Switch on **Advanced Options** in [Preferences](../configuration/preferences.md) and raise
   **Discovery timeout scale** to 3x. This is what it is for — slow bridges, and bridges with
   battery-powered devices behind them.
2. Run **_DiscoverAll** again. Discovery is safe to repeat; it does not duplicate child devices.
3. Discovery can take several minutes on a bridge with many devices. Let it finish before deciding
   it has failed.

## A device behind the bridge did not appear

1. **Is it paired to the bridge itself?** Check in the manufacturer's app first. The bridge only
   shares what it has.
2. **Was it added after your last discovery?** Run **_DiscoverAll** again.
3. **Compare `deviceCount` with `endpointsCount`.** `endpointsCount` is what the bridge offers;
   `deviceCount` is what became a Hubitat device. A gap is normal — not every Matter device type is
   supported.
4. If a child was created but named `Unknown` and given a `Generic Component Switch` driver, the
   device reported no cluster this driver recognises. See
   [Which driver do I get?](../drivers/index.md).

One physical device can arrive as several child devices. A temperature and humidity sensor may
produce separate temperature, humidity, and battery children — that is how the bridge presents it,
not a fault.

## A device works, but with the wrong driver

Change the **Type** field on the child device and click **Save Device**. The documented case is a
colour bulb that came up as `Generic Component CT`: switching it to `Generic Component RGBW` gives
full colour control. [Which driver do I get?](../drivers/index.md) explains how the choice is made.

## The logs are flooded

- **Debug logging is on by default** and turns itself off 24 hours after you last saved
  preferences. Turn it off in [Preferences](../configuration/preferences.md) when you have finished
  diagnosing.
- **Trace logging** is far noisier still, and is only for a specific investigation.
- If one chatty device dominates the log, set **Spammy attributes minimum reporting interval** to a
  positive value. That moves frequently-reporting attributes into a slower subscription.

## Nothing above helped

**Load All Defaults** resets every preference, state, and scheduled job, then restarts the periodic
jobs. Child devices are **not** deleted. You must run **_DiscoverAll** afterwards.

Do not start here. It discards your settings, and if the real problem is an unreachable bridge you
will simply be back where you started, minus your configuration.

Avoid the **Utilities** entries `removeAllDevices` and `removeAllSubscriptions` unless you have
decided to rebuild from scratch. `removeAllDevices` deletes every child device — along with its
name, room assignment, and every automation that referred to it. There is no confirmation prompt.

## Asking for help

Post in the [community thread](support-and-links.md) with:

1. **Driver version** — shown under the links at the top of the Preferences tab.
2. **Hubitat platform version** and hub model.
3. **Bridge make, model, and firmware** — `productName` and `softwareVersionString` on the parent
   device page.
4. **`deviceCount` and `endpointsCount`**, and which device is affected.
5. **Logs with debug logging on**, covering the moment the problem happens. Turn debug on, reproduce
   the problem, then copy the log — a log from after the fact rarely shows the cause.
6. What you have already tried from this page.

Check [Known issues](known-issues.md) first — some behaviour is a documented limitation of the
Hubitat Matter platform rather than a fault in this driver.
