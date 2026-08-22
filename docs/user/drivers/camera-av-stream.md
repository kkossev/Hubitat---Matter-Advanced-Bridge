# Matter Generic Component Camera AV Stream

Applies to: 1.9.3 | Last verified: 2026-08-22 | Status: Experimental

Audio and settings control for Matter cameras.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Camera_AV_Stream.groovy` (driver version 1.1.0)
- **Assigned to:** endpoints reporting cluster `0x0551` Camera AV Stream Management or `0x0552`
  Camera AV Settings User Level Management — Matter 1.5 and later

**This driver does not give you video.** Hubitat has no video capability, and Matter's camera
cluster covers device management rather than the stream itself. What you get is control over the
camera's audio, night vision, and similar settings, plus its status as attributes you can use in
rules.

It was written against the **Aqara G350** and is the newest, least tested driver in the package.

## Capabilities

`AudioVolume` · `Switch` · `MotionSensor` · `Sensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `mute` | `muted`, `unmuted` | The speaker. |
| `volume` | 0–100 | Speaker volume. |
| `microphoneMuted` | `muted`, `unmuted` | |
| `microphoneVolume` | 0–100 | |
| `nightVision` | `Off`, `On`, `Auto` | Settable with **Set Night Vision**. |
| `switch` | `on`, `off` | Master privacy control — see below. |
| `motion` | `active`, `inactive` | The camera's own vision occupancy, where it reports one. |
| `softRecordingPrivacy` | `enabled`, `disabled` | Recording stopped. |
| `softLivestreamPrivacy` | `enabled`, `disabled` | Live stream stopped. |
| `hardPrivacy` | `enabled`, `disabled` | Read-only — the physical shutter. |
| `pan` / `tilt` / `zoom` | number | Current mechanical position, on a camera with PTZ. |
| `movementState` | `Idle`, `Moving` | Whether the camera is currently moving. |
| `rtt` | number | Round-trip time in milliseconds, from the last **Ping**. |

The camera reports far more than this. Everything else is kept in the `cameraAttr` state variable
rather than as Hubitat attributes, since most of it never changes — **Get Info** writes a readable
summary to the logs.

## Commands

| Command | What it does |
|---|---|
| **Set Speaker Muted** / **Set Speaker Volume** | Mute or set the speaker, 0–100. |
| **Set Microphone Muted** / **Set Microphone Volume** | Mute or set the microphone. |
| **Mute** / **Unmute** / **Set Volume** / **Volume Up** / **Volume Down** | The standard Hubitat audio commands, acting on the speaker. |
| **Set Night Vision** | `Off`, `On` or `Auto`. |
| **On** / **Off** | The master privacy switch — see below. |
| **Set Soft Recording Privacy** / **Set Soft Livestream Privacy** | The two privacy modes individually. |
| **Ptz Set Position** | Move to an absolute pan, tilt and/or zoom. Leave a field empty to leave that axis alone. |
| **Ptz Relative Move** | Nudge by a delta on any axis. Leave a field empty to leave that axis alone. |
| **Ptz Save Preset** / **Ptz Move To Preset** / **Ptz Remove Preset** | Save, recall or delete a camera position. |
| **Camera Snapshot Diagnostics** | Reads the camera's snapshot capabilities and reports in the log whether snapshots are supported. Diagnostic only — it does not take a picture. |
| **Get Info** | Reads every camera attribute, across both camera clusters, and logs a summary. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

## Privacy and the on/off switch

Matter gives a camera three privacy states, and this driver exposes all of them:

| State | Meaning |
|---|---|
| `softRecordingPrivacy` | The camera stops recording. Writable. |
| `softLivestreamPrivacy` | The camera stops streaming. Writable. |
| `hardPrivacy` | The physical privacy state. **Read-only** — the camera reports it, nothing can set it remotely. |

The **On/Off** switch is a convenience wrapper over the two soft modes, so the camera behaves like an
ordinary switch in rules and on a dashboard:

- **Off** enables both soft privacy modes.
- **On** clears both.
- `switch` reads `on` only when neither mode is enabled.

> **On the Aqara G350, turning the switch off physically closes the camera shutter.** Matter treats
> the soft modes as a software-only stop, but this camera ties them to the shutter: writing the two
> soft modes makes `hardPrivacy` follow about a second later. That is the camera's own behaviour,
> not something the driver does — but it means **Off** is a stronger action than it sounds, and the
> shutter has to physically re-open when you switch back on.

## Pan, tilt and zoom

On a camera that implements cluster `0x0552`, the PTZ commands drive the motor directly. Two things
worth knowing:

- **The limits are the camera's, not 0–100.** Values are clamped to what the camera reports. The
  G350, for example, allows pan −170…170 but tilt only −15…24, and zoom tops out at 81. If a value
  is clamped the log says so. If the driver has not read the limits yet, the command is refused —
  run **Refresh** or **Get Info** first.
- **Only the axes you fill in are sent.** Leaving *Tilt* empty in **Ptz Set Position** means tilt is
  not touched, rather than being reset to a cached value.

Digital PTZ (panning within the video frame rather than moving the camera) is not implemented.

### Presets

**Ptz Save Preset** stores the camera's current pan, tilt and zoom under a numbered slot with a name
you choose, and **Ptz Move To Preset** recalls it. The camera decides how many slots it has — the
G350 allows five.

Preset numbers start at **1**, not 0. Moving to or removing a preset the camera has not saved is
refused with a message in the log rather than being sent and silently ignored, because a camera
given a preset it does not know may simply not answer at all.

After a save or a remove, the current preset list is visible in the `cameraAttr` state variable.

Two things the G350 does that are worth knowing before you build rules on this:

- **Moving the camera sets off its own motion detection.** The camera's vision occupancy sees the
  image change and reports `motion active`. If you automate on `motion`, expect a hit every time
  the camera pans.
- **The other axes can drift slightly.** A pan-only move on the G350 also nudged the reported tilt
  by a couple of units. The driver sends only the axis you asked for, so this is the camera's own
  mechanics, not a command touching tilt.

## Preferences

| Preference | Default | What it does |
|---|---|---|
| **Enable debug logging** | Off | Turns itself off automatically after 24 hours if switched on. |
| **Enable descriptionText logging** | On | One readable log line per change. |

## Bridge-specific notes

The **Aqara G350** is the only camera this has been developed against. Cameras are new to Matter and
implementations vary, so treat any other model as untested.

## Known limitations

**Experimental.** Expect gaps, and report what you find in the
[community thread](../help/support-and-links.md).

- **No video, and no snapshots.** Snapshot capture is not implemented — the diagnostics command only
  reports whether the camera claims to support it. Live view uses Matter's WebRTC clusters
  (`0x0553`/`0x0554`) and recorded clips use Push AV (`0x0555`); both need a media stack Hubitat
  drivers cannot run, so neither will be supported here.
- **No two-way talk**, even on a camera that supports it. The camera advertises its capability in
  `TwoWayTalkSupport` and **Get Info** reports it — the G350 says `FullDuplex` — but the audio
  itself travels over the same WebRTC transport as the video, so there is nothing here to speak
  through. The attribute tells you what the camera can do with a different controller, not what this
  driver can do.
- **No digital PTZ**, and no zone management, chime, HDR, image control, local storage or status
  light controls, even where a camera exposes them.
- Requires a camera that implements Matter's camera clusters, which arrived in Matter 1.5. Most
  cameras sold today do not, and a bridge must also choose to expose them.
- Everything here was developed against the Aqara G350 on firmware 4.5.70. Earlier G350 firmware
  exposes far less — privacy and PTZ in particular arrived in 4.5.70.
- If the camera stops responding to streaming from *any* controller, power-cycle the camera itself.
  Its transport subsystem can wedge independently of its Matter state, and neither a hub restart nor
  a driver change clears it.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
- [Compatibility matrix](../compatibility/matrix.md)
- [Aqara bridge](../bridges/aqara.md)
