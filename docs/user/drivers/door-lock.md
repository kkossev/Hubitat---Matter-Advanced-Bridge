# Matter Generic Component Door Lock

Applies to: 1.9.3 | Last verified: 2026-08-22 | Status: Current

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
| `lock` | `locked`, `unlocked`, `unlocked with timeout`, `unknown` | Seeded as `unknown` when the child device is created, until the lock first reports. |
| `battery` | 0–100 | Most locks are battery powered. Reported by the parent, from the Power Source cluster. |
| `doorState` | `Open`, `Closed`, `Jammed`, `ForcedOpen`, `Ajar`, `UnspecifiedError` | Only on a lock with a door sensor, and only from a Door State Change event. |
| `lockAlarm` | `LockJammed`, `LockFactoryReset`, `LockRadioPowerCycled`, `WrongCodeEntryLimit`, `FrontEscutcheonRemoved`, `DoorForcedOpen`, `DoorAjar`, `ForcedUser` | Alarms the lock raises. Also logged as a warning. |
| `lastLockOperation` | `Lock`, `Unlock`, `Unlatch`, `NonAccessUserEvent`, `ForcedUserEvent` | What happened last. |
| `lastOperationSource` | `Manual`, `Keypad`, `Button`, `Remote`, `ProprietaryRemote`, `Auto`, `Schedule`, `RFID`, `Biometric`, `Aliro (UWB/Apple Watch)`, `Unspecified` | How it happened. |
| `lastLockOperationError` | `InvalidCredential`, `DisabledUserDenied`, `Restricted`, `InsufficientBattery`, `Unspecified` | Why the last operation failed, if it did. |
| `lastUserChange` | string | The last user, credential or schedule change the lock reported, as operation plus data type — `Add PIN`, `Clear PIN`, and so on. |
| `lastCodeName` | string | The name of the code that operated the lock, when the lock identifies a user. |
| `lockCodes` | JSON | The codes this driver knows about. Hubitat's Lock Code Manager reads it. |
| `codeLength` | number | The lock's maximum PIN length once it has been read. Seeded as 4. |
| `maxCodes` | number | Total users the lock supports once it has been read. Seeded as 10. |
| `codeChanged` | `added`, `changed`, `deleted`, `failed`, plus the driver's own `setting` and `deleting` progress values | What Lock Code Manager watches. |
| `powerSourceStatus` | `active`, `standby`, `unavailable` | |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

`lastOperationSource` is the useful one for automations — it tells you whether the door was unlocked
from the keypad, from an app, or by hand.

### State variables

The lock reports far more than the attributes above. The rest is kept in state variables, because it
is configuration that rarely changes:

- **`lockAttr`** mirrors around thirty five Door Lock cluster attributes exactly as the lock reports
  them — lock type, actuator enabled, operating mode and supported operating modes, PIN length
  limits, user and credential counts, auto relock time, wrong code entry limit, and the rest of the
  lock's configuration. **Get Info** writes a readable summary of the same values to the live logs.
- **`info`** says whether the lock reports support for PIN codes and user management, and shows the
  FeatureMap value that conclusion came from. It is rewritten every time the device's preferences
  are saved, so the order that refreshes it is **Get Info** first, then **Save Preferences**.
- **`stats`** counts events received, duplicates suppressed, and out-of-order events. **Clear
  Statistics** resets it.

## Commands

| Command | What it does |
|---|---|
| **Lock** / **Unlock** | The basics. |
| **Unlock With Timeout** | Unlocks and re-locks automatically after the given number of seconds, 1 to 65535. The lock then reports as `unlocked with timeout`. Refused if the lock does not list the command as accepted. |
| **Unbolt Door** | Retracts the latch without unlocking. Needs the lock's Unbolt feature, and is refused if the lock does not advertise it. Hubitat has no unlatched state, so the lock reads as `unlocked` afterwards. |
| **Identify** | Makes the lock identify itself, if the endpoint has the Identify cluster. |
| **Get Info** | Reads every Door Lock attribute the lock advertises and writes a summary to the live logs and device data. **Run this first** — it tells you which features your lock actually supports. |
| **Clear Statistics** | Resets the event counters and the duplicate-event cache. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

### Lock codes

`Set Code`, `Delete Code`, `Get Codes`, and `Set Code Length` come from the `LockCodes` capability,
which is what lets Hubitat's Lock Code Manager drive the lock.

**Lock code management works.** Codes set and deleted from Hubitat are registered on the lock and
are usable in Rule Machine, tested on the Aqara U200 and U400. Operations by RFID tag and by UWB
(Aliro — an Apple Watch or phone) are reported alongside keypad PINs.

This changed in **1.9.0**, and forum posts older than that describe a version where it did not work
properly. If you are reading one, check its date.

**Whether your lock supports it at all is decided by the lock, not the driver.** Read the `info`
state variable. Four behaviours are worth knowing:

- **Set Code and Delete Code are refused outright** when the lock's FeatureMap does not advertise
  both PIN credentials and user management. **Ignore Compatibility Checks** forces them through.
- **A lock can claim more than it does.** The Nuki 4.0 lists `SetCredential`, `SetUser`, `GetUser`
  and the rest in its accepted commands while reporting a FeatureMap of `0x00`, and anything beyond
  lock, unlock and unbolt either goes unanswered or drops its Matter session. The FeatureMap check
  is what protects you from that, which is why forcing commands through is an advanced option.
- **Get Codes does not query the lock.** It re-sends the codes the driver has already stored, so
  that Lock Code Manager picks them up. Reading the full user list back out of a Matter lock is not
  implemented.
- **Set Code Length only changes the Hubitat attribute.** A Matter lock's minimum and maximum PIN
  lengths are read-only, so nothing is written to the lock. It accepts 4 to 8.

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | Off | Turns itself off automatically 24 hours after you switch it on. |
| **Enable descriptionText logging** | On | One readable log line per change. |
| **Advanced Options** | Off | Reveals the two settings below. |
| **Ignore Compatibility Checks** | Off | Sends commands even when the lock does not advertise support for them. For testing what a lock will actually accept. It applies to **Unlock With Timeout**, **Unbolt Door**, and the lock code commands. |
| **Overwrite existing codes** | Off | Makes **Set Code** delete any existing credential in that slot first. Needed for slots created by Apple Home or another controller. |

## Locks known to work

| Lock | How it connects | What works |
|---|---|---|
| **Aqara U200** | Paired to Hubitat as a Matter device, with this package assigned as its driver | Lock, unlock, and code management, including RFID and UWB operations |
| **Aqara U400** | The same | The same |
| **Nuki 4.0** | The same | Lock, unlock and unbolt only — see the FeatureMap warning above |
| **Aqara U100** | Bridged through an Aqara hub | Export confirmed in 2024, before lock control worked; not retested since |

Most of the lock and lock code work was done against the U200 and the U400, which reach Hubitat as
directly-paired Matter devices rather than through a bridge — see
[Using it with Matter devices](../getting-started/use-with-matter-devices.md). Hubitat's own stock
driver also covers these two locks, code management included, so this driver is the choice when you
want what it exposes on top: the operation source, the alarms, the error reasons, and the full
cluster dump from **Get Info**.

The [compatibility matrix](../compatibility/matrix.md) tracks *bridges*, and its Door Lock row is
still a 2024 result: **Unsupported** on Tuya / Zemismart, and a bridged U100 on Aqara exported but
never retested since lock control started working in 1.5.5.

## Known limitations

- **Whether codes can be managed at all is the lock's decision** — see the FeatureMap note above.
- **A code the driver has not seen before is registered with a placeholder PIN.** When the lock
  reports an unlock by a user the driver does not know about — a keypad code added in the lock's own
  app, for instance — the driver adds it to `lockCodes` as `Code <slot>` with a random six-digit
  PIN, so that Lock Code Manager can track the slot. **That PIN is not the real one.** Delete the
  code and re-add it from Hubitat if the stored value has to be correct.
- Not every lock supports every command. **Unbolt Door** in particular depends on the hardware, and
  a lock that does not support it will refuse or ignore the command.
- The lock reports its state; the driver cannot force it. A lock that is jammed reports the failure
  through `lockAlarm` and `lastLockOperationError` rather than retrying.
- **Only PIN credentials can be created and deleted.** RFID, fingerprint, face and Aliro
  credentials are enrolled in the lock's own app. They are reported correctly once they exist — an
  RFID tag or an Apple Watch unlock arrives with the right `lastOperationSource` — but Hubitat
  cannot add or remove them.

## See also

- [Which driver do I get?](index.md)
- [Using it with Matter devices](../getting-started/use-with-matter-devices.md)
- [Device types](../compatibility/device-types.md)
- [Compatibility matrix](../compatibility/matrix.md)
- [Aqara bridge](../bridges/aqara.md)
- [Known issues](../help/known-issues.md)
