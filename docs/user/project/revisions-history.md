# Revision history

Applies to: 1.9.1 | Last verified: 2026-08-13 | Status: Current

User-visible changes, newest first. The driver file header carries the complete technical changelog,
including internal refactoring not listed here.

**The current released version is 1.8.8**, which is what Hubitat Package Manager installs.
**1.9.0 and later are BETA** — available for testing, not yet the released package.

## 1.9.1 — 2026-08-13 *(BETA)*

- Each child device's **Device Data** section now shows the bridged device's serial number and unique
  id, when the bridge reports them. These are whatever the bridge sends: on some bridges several
  children share the hub's own serial, so treat them as informational rather than a guaranteed
  per-device identifier.
- **Multi-gang devices are no longer indistinguishable.** A two-gang wall switch used to arrive as two
  children both called `Switch`, named after the bridge, because the individual gangs carry no name of
  their own over Matter. Each gang now inherits its parent device's name and identifiers and gets a
  `- 1` / `- 2` suffix, for example `Aqara Wall Switch EU - 1`. A label you set yourself is **never**
  overwritten — only a blank or generic one is replaced.
- Discovery no longer stalls when a bridge fails to answer a large attribute read. It retries in
  smaller batches and carries on, instead of timing out on every device in turn.
- Aqara device names: see [Aqara](../bridges/aqara.md) for what these hubs do and do not export.

## 1.9.0 — 2026-07-25 *(BETA)*

- Illuminance, temperature, pressure and humidity sensors no longer report a false `0` when the
  device says the measurement is unavailable.
- Illuminance readings are now rounded rather than truncated, and very bright readings such as
  direct sunlight are no longer discarded.
- New **Spammy attributes minimum reporting interval** preference, which throttles frequently
  reporting attributes at the Matter subscription instead of inside the driver. The old
  driver-side illuminance throttling and its preference are removed — see
  [Known issues](../help/known-issues.md).
- Battery reporting fixed for devices that report it on the root node, such as IKEA Thread devices.
  The bridge itself can now report battery too.
- The health check and ping jobs are scheduled automatically on a fresh installation. Previously
  nothing was scheduled until you pressed Save Preferences.
- Fixed endless `colorMode is CT` log messages on the bridge device.

## 1.8.x — 2026-02 to 2026-05

- **1.8.9** Aqara G350 video camera support *(BETA)*.
- **1.8.8** Lock code improvements; default timeouts doubled. **Current released version.**
- **1.8.7** Matter Lock Codes — first test version. Still experimental.
- **1.8.6** Fan control (cluster `0x0202`), tested on an Altitude Boca II ceiling fan. Thanks
  @sbohrer.
- **1.8.4** Refresh now reads attributes in chunks, to stay within Matter message size limits.
- **1.8.3** Thermostat fixes: Fahrenheit to Celsius conversion for heating setpoints, cooling
  setpoint support, and correct decoding of the running state. Thanks @Murv82.
- **1.8.2** Contact sensor sensitivity support (cluster `0x0080`) and the custom Contact Sensor
  driver — Aqara P100.
- **1.8.0** Pressure sensor support (cluster `0x0403`); Button driver improvements.

## 1.7.x — 2026-01 to 2026-03

- **1.7.8** Delayed illumination handling. Thanks @lgk.
- **1.7.7 – 1.7.6** Power/energy and window covering exception fixes.
- **1.7.4** Bridge reboot and uptime reporting; event filtering after a reboot or re-subscribe.
- **1.7.2** Contact, water, motion and lock state parsing fixes; battery percentage patch for the
  Zemismart M1.
- **1.7.1** Automatic best-name labelling for all child devices. Thanks @iEnam.
- **1.7.0** ALPSTUGA air quality monitor support (CO₂); new clean subscribe interval preferences.

## 1.6.0 — 2026-01-17

- Major refactoring of the Door Lock driver.
- Water leak sensors are now detected automatically instead of appearing as contact sensors.

## 1.5.x — 2025-04 to 2026-01

- **1.5.6** Button events fixed — the subscription problem that had prevented buttons from working.
- **1.5.5** **Matter locks now work.** Locking and unlocking, not just status.
- **1.5.4** New Button driver; new discovery timeout scale preference.
- **1.5.3** Workaround for a Hubitat TLV decoding bug.
- **1.5.2** New Signal driver.
- **1.5.0** New Power Energy driver.

## 1.4.0 — 2024-12-26

- Compatibility with Hubitat platform 2.4.0.x.

## 1.3.0 — 2024-10-10

- Air Purifier driver and air quality sensor support (cluster `0x005B`).

## 1.2.x — 2024-10

- **1.2.2** SwitchBot Button driver. Thanks @ymerj. *(Deprecated — use the Button driver.)*
- **1.2.1** Thermostat fixes and basic Matter event decoding.
- **1.2.0** Thermostat support; adopted the platform's `cleanSubscribe`.

## 1.1.0 — 2024-07-20

- Door Lock component driver, contributed by @dds82.
- Identify command.

## 1.0.0 — 2024-03-16

First public release.

---

## Before the public release

The 0.x history below covers alpha and beta testing, from the first version in December 2023 to the
move to this GitHub repository in March 2024. It is kept for the record.

* ver. 0.0.0  2023-12-29 kkossev  - Inital version;
* ver. 0.0.1  2024-01-05 kkossev  - Linter; Discovery OK; published for alpha- testing.
* ver. 0.0.2  2024-01-07 kkossev  - Refresh() reads the subscribed attributes; added command 'Device Label'; VendorName, ProductName, Reachable for child devices; show the device label in the event logs if set; added a test command 'setSwitch' on/off/toggle + device#;
* ver. 0.0.3  2024-01-11 kkossev — added Child devices; added deviceCount; added the Motion Sensor and Window Shade component drivers; added matterLib.groovy; Hubitat Bundle package; added logTrace() and logError().
* ver. 0.0.4  2024-01-14 kkossev — added the Switch component driver; WindowCovering position and commands; trace logging switched off automatically; duplicate On/Off events filtered; disabled devices skipped; added initializeCtr; added removeAllSubscriptions(); 'Invert Motion' option @iEnam.
* ver. 0.0.5  2024-01-20 — added endpointsCount; subscribe to PartsList; discovery fixes; debug off by default; temperature sensor fix.
* ver. 0.0.6  2024-01-27 — the _DiscoverAll state machine replaced the manual discovery buttons.
* ver. 0.0.7  2024-01-28 — many discovery and event fixes; bulbs assigned 'Generic Component Dimmer'; setLevel implemented; Celsius to Fahrenheit conversion.
* ver. 0.1.0  2024-02-03 — Contact Sensor processing; Thermostat cluster decoding; Battery component; vibration sensors handled as motion sensors.
* ver. 0.2.0  2024-02-04 — parsing refactored to a lookup map; DoorLock cluster decoding; lock and unlock commands (untested).
* ver. 0.2.1  2024-02-07 — child device naming pattern; level change and colour temperature commands; 'Generic Component CT' for bulbs. Device label fixes @fanmanrules.
* ver. 0.2.3  2024-02-11 — RGBW hue and saturation, setColor; healthStatus offline fix.
* ver. 0.3.0  2024-02-13 — read all supported clusters during discovery; RGBW bulbs assigned 'Generic Component RGBW'.
* ver. 0.4.0  2024-02-18 — major refactoring of attribute subscriptions; the bundle published on HPM.
* ver. 0.4.1  2024-02-20 — illuminance support (Aqara T1 Light Sensor); FeatureMap stored per cluster.
* ver. 0.4.3  2024-02-26 — added the utilities() command; state cleanup when Minimize State Variables is on.
* ver. 0.4.4  2024-03-02 — refresh() for component devices; WindowCovering fixes @Steve9123456789.
* ver. 0.4.5  2024-03-03 — Battery / PowerSource cluster processing enabled.
* ver. 0.5.0  2024-03-09 — Window Covering driver refactoring, battery attributes, new options.
* ver. 0.5.1  2024-03-10 — Help/Documentation links added to the driver.
* ver. 0.6.0  2024-03-13 — moved to this GitHub repository.
