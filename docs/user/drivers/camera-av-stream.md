# Matter Generic Component Camera AV Stream

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Experimental

Audio and settings control for Matter cameras.

- **Namespace:** `kkossev`
- **Source:** `Components/Matter_Generic_Component_Camera_AV_Stream.groovy` (driver version 1.0.1)
- **Assigned to:** endpoints reporting cluster `0x0551` Camera AV Stream Management — Matter 1.3 and
  later

**This driver does not give you video.** Hubitat has no video capability, and Matter's camera
cluster covers device management rather than the stream itself. What you get is control over the
camera's audio, night vision, and similar settings, plus its status as attributes you can use in
rules.

It was written against the **Aqara G350** and is the newest, least tested driver in the package.

## Capabilities

`AudioVolume` · `Sensor` · `Refresh`

## Attributes

| Attribute | Values | Notes |
|---|---|---|
| `mute` | `muted`, `unmuted` | The speaker. |
| `volume` | 0–100 | Speaker volume. |
| `microphoneMuted` | `muted`, `unmuted` | |
| `microphoneVolume` | 0–100 | |
| `nightVision` | `Off`, `On`, `Auto` | Read-only — reported by the camera, not settable here. |
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
| **Camera Snapshot Diagnostics** | Reads the camera's snapshot capabilities and reports in the log whether snapshots are supported. Diagnostic only — it does not take a picture. |
| **Get Info** | Reads every camera attribute and logs a summary. |
| **Refresh** | Re-reads this device's subscribed attributes from the bridge. |
| **Ping** | Measures the round-trip time and updates `rtt`. |

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
  reports whether the camera claims to support it.
- **Night vision is read-only.** The camera reports its mode; the driver cannot change it.
- Privacy mode, image controls, and status light controls are not implemented, even where the camera
  exposes them.
- Requires a camera that implements Matter 1.3's camera cluster. Most cameras sold today do not, and
  a bridge must also choose to expose it.

## See also

- [Which driver do I get?](index.md)
- [Device types](../compatibility/device-types.md)
