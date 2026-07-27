# Matter Generic Component Door Lock

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Matter door locks.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Door_Lock` (driver version 1.5.0)
- **Assigned to:** endpoints reporting cluster `0x0101` Door Lock — Matter device type Door Lock
  (`0x000A`)

**Locking and unlocking work directly from Hubitat.** Older forum posts and the previous version of
this page described a workaround routing lock commands through Apple Home, because locks could not
be controlled at all. That has not been necessary since driver 1.5.5 — see the
[revision history](../project/revisions-history.md).

## Capabilities

`Lock` · `LockCodes` · `Battery` · `Sensor` · `Actuator` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `lock` | `locked`, `unlocked`, `unlocked with timeout`, `unknown` | |
| `battery` | 0–100 | Most locks are battery powered. |
| `doorState` | string | If the lock has a door sensor. |
| `lockAlarm` | string | Alarms the lock raises — jammed, tampered, and similar. |
| `lastLockOperation` | string | What happened last. |
| `lastOperationSource` | string | How it happened: keypad, remote, manual, and so on. |
| `lastLockOperationError` | string | Why the last operation failed, if it did. |
| `lastUserChange`, `lastCodeName` | string | User and code activity. |
| `powerSourceStatus` | `active`, `standby`, `unavailable` | |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

`lastOperationSource` is the useful one for automations — it tells you whether the door was unlocked
from the keypad, from an app, or by hand.

Lock type, operating mode, and the supported operating modes are kept in the `lockAttr` state
variable rather than as attributes. **Get Info** summarises them in the log.

## Commands

| Command | What it does |
|---|---|
| **Lock** / **Unlock** | The basics. |
| **Unlock With Timeout** | Unlocks and re-locks automatically after the given number of seconds. |
| **Unbolt Door** | Retracts the latch without unlocking. Not available on all locks. |
| **Identify** | Makes the lock identify itself. |
| **Get Info** | Reads the lock's capabilities and writes a summary to the live logs and device data. **Run this first** — it tells you which features your lock actually supports. |
| **Clear Statistics** | Resets the event counters and the duplicate-event cache. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

### Lock codes

`Set Code`, `Delete Code`, `Get Codes`, and `Set Code Length` come from the `LockCodes` capability.

**Lock code management is experimental.** It was first added in 1.8.7 as a test version and has
not been confirmed working across locks. Check the state variables after running **Get Info** to see
whether your lock reports support for it, and keep the lock's own app available as a fallback. See
[Known issues](../help/known-issues.md).

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | On | Turns itself off automatically after 24 hours. |
| **Enable descriptionText logging** | On | One readable log line per change. |
| **Advanced Options** | Off | Reveals the two settings below. |
| **Ignore Compatibility Checks** | Off | Sends commands even when the lock does not advertise support for them. For testing what a lock will actually accept. |
| **Overwrite existing codes** | Off | Makes **Set Code** delete any existing credential in that slot first. Needed for slots created by Apple Home or another controller. |

## Bridge-specific notes

- The **Aqara U200** is the lock most of the recent work was done against.
- Locks are Confirmed on Aqara bridges and Unsupported on Tuya in the
  [compatibility matrix](../compatibility/matrix.md).

## Known limitations

- **Lock codes are experimental** — see above.
- Not every lock supports every command. **Unbolt Door** in particular depends on the hardware, and
  a lock that does not support it will refuse or ignore the command.
- The lock reports its state; the driver cannot force it. A lock that is jammed reports the failure
  through `lockAlarm` and `lastLockOperationError` rather than retrying.
- Credential types beyond PIN — RFID, fingerprint, face — are not exposed.

## See also

- [Which driver do I get?](index.md)
- [Known issues](../help/known-issues.md)
