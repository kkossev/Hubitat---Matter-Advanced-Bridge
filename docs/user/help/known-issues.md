# Known issues

Applies to: 1.9.2 | Last verified: 2026-07-27 | Status: Current

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
| **Lock Codes (PIN codes and users) are experimental.** Locking and unlocking work; code management was first added in 1.8.7 and is still labelled a test version. | 1.8.7 and later | Implemented, unverified | Use the lock's own app to manage codes. |

## Reporting something not listed here

Post in the [community thread](support-and-links.md) with the information listed under
[asking for help](troubleshooting.md#asking-for-help). A report naming the bridge, its firmware, and
the driver version is worth far more than one without.
