# Known issues

Applies to: 1.9.3 | Last verified: 2026-08-22 | Status: Current

For a problem you are having right now, start with [Troubleshooting](troubleshooting.md).

> **Most of the old known issues are gone.** The Hubitat Matter platform has evolved a great deal
> since 2024, and the limitations listed on this page back then — including the ones that stopped
> locks and buttons from working — have since been resolved. If you find an older forum post
> describing a Matter limitation, check its date before assuming it still applies.

Each entry below carries an evidence label: **Confirmed** (reproduced), **Reported** (user report,
not reproduced here), **Implemented, unverified** (the code is there, no live-device test), or
**Unknown** (no reliable evidence either way).

## Current limitations

| Issue | Affects | Evidence | Workaround |
|---|---|---|---|
| **No composite devices.** A temperature and humidity sensor arrives as separate temperature, humidity, and battery children, because that is how the bridge exposes it. | All versions | Confirmed | None. Grouping attributes into one child device is on the TODO list. |
| **Error entries in a child device's log on every command.** A shade, switch or metering child logs `MissingMethodException: No signature of method: ... parse()` each time it is commanded. | 1.9.0 and 1.9.1 | Reported | Update to 1.9.2, where it is fixed. The command itself always succeeded and the device responded normally, so the entry was log noise. |
| **Not every lock allows its PIN codes to be managed — the lock's FeatureMap decides.** Code management itself works, and is tested on the Aqara U200 and U400. A lock whose FeatureMap does not advertise PIN credentials and user management is refused, even when it lists the credential commands as accepted; the Nuki 4.0 does exactly that. | 1.9.0 and later | Confirmed | On a lock that does not support it, manage codes in the lock's own app. **Ignore Compatibility Checks** forces the commands through if you want to see what the lock does with them. |
| **A device can stay online and commandable but stop reporting its state.** Seen on an Aqara U400: lock and unlock kept working and `networkStatus` stayed `online`, but the `lock` attribute stopped following both physical and commanded changes. | 1.8.8 | Confirmed | Run **_DiscoverAll** on the parent. It rebuilds endpoint information and subscriptions and keeps the existing child devices. Refresh, Re Subscribe, a hub reboot, and re-pairing Matter all failed first. 1.9.0 makes the likely cause less likely, by refusing to start discovery against an unreachable bridge. |

## Reporting something not listed here

Post in the [community thread](support-and-links.md) with the information listed under
[asking for help](troubleshooting.md#asking-for-help). A report naming the bridge, its firmware, and
the driver version is worth far more than one without.
