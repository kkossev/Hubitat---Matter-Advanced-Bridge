# SwitchBot

Applies to: 1.9.0 | Last verified: 2026-08-22 | Status: Current

Devices tested behind the **SwitchBot Hub 2** (since April 2024), the **SwitchBot Hub Mini
Matter Enabled** (since February 2025), and the **SwitchBot Hub 3** (since January 2026) acting as
Matter bridges. Only a handful of device types have been tried across these hubs, so most of this
bridge family is simply unknown rather than unsupported. SwitchBot identifies one further
Matter-capable hub, but no MAB test results have been recorded for it yet.

Labels: **Confirmed** — tested working. **Partial** — works with a known, specific limitation.
**Implemented, unverified** — discovered and read by MAB, but no one has reported testing that it
actually works. **Unknown** — not tested. See the [compatibility overview](../compatibility/overview.md).

## Matter-capable hubs

| Hub | SwitchBot Matter status | MAB evidence |
|---|---|---|
| SwitchBot Hub Mini Matter Enabled | Matter bridge | Confirmed — tested with MAB since [#247](https://community.hubitat.com/t/-/135252/247) (iEnam, 2025-02-11); reconfirmed with Bot and Curtain by [#447](https://community.hubitat.com/t/-/135252/447) (Radial, 2026-08-16) |
| SwitchBot Hub 2 | Matter bridge | Confirmed — tested with MAB since [#34](https://community.hubitat.com/t/-/135252/34) (2024-04-08) |
| SwitchBot Hub 3 | Matter bridge | Confirmed — tested with MAB since a separate thread, [Switchbot matter pairing](https://community.hubitat.com/t/switchbot-matter-pairing/160265) (jonharju, 2026-01-18), reconfirmed independently in [SwitchBot driver for Roller Shades?](https://community.hubitat.com/t/switchbot-driver-for-roller-shades/164680) (TheGerman, 2026-06-25–2026-07-05). Not previously recorded on this page — see the "SwitchBot Hub 3" section below. |
| SwitchBot AI Hub | Matter bridge | Unknown — not tested with MAB |

The original **SwitchBot Hub Mini** is a different product and does **not** support Matter. Do not
confuse it with the Matter Enabled version above.

One user reported that the SwitchBot app currently caps a Matter-bridged hub at 8 secondary
devices ([#180](https://community.hubitat.com/t/-/135252/180), iEnam, 2024-12-30) — not confirmed
as an official SwitchBot spec, but worth knowing if devices seem to be missing.

### Setup requirement for the Hub Mini Matter Enabled

Devices are not exposed to Matter automatically. In the SwitchBot app, open the Hub Mini's Matter
setup and explicitly add each device — Bot, Curtain, or otherwise — as a bridged device. A device
left out of that list stays invisible to MAB, the same way an unshared device stays invisible on
any other Matter bridge. Source: [community report, 2026-08-16](https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252/447).

### SwitchBot Hub 3

Evidence for this hub comes from two separate community threads, not the main MAB thread — neither
had been cross-referenced onto this page before.

**Use MAB, not the stock "Generic Matter Bridge" driver, for Blind Tilt behind a Hub 3.** In
[Switchbot matter pairing](https://community.hubitat.com/t/switchbot-matter-pairing/160265),
jonharju paired a Hub 3 and added three Blind Tilt devices; on Hubitat's stock driver, **Close**
(and Set Position to 100%) started moving, then stopped and hung
([post #4](https://community.hubitat.com/t/switchbot-matter-pairing/160265/4)). Switching the hub
and the Blind Tilts over to the Matter Advanced Bridge driver fixed it —
*"Open and Close now work as expected"*
([post #6](https://community.hubitat.com/t/switchbot-matter-pairing/160265/6)) — and a follow-up
confirmed the stock driver has the identical bug, concluding *"you need to install and use the
Advanced Matter Bridge with this unit"*
([post #8](https://community.hubitat.com/t/switchbot-matter-pairing/160265/8), 2026-01-28). The
same user separately confirmed the Hub 3's built-in temperature/humidity sensor
([post #9](https://community.hubitat.com/t/switchbot-matter-pairing/160265/9), 2026-02-25).

**Discovery can need a clean reinstall.** In
[SwitchBot driver for Roller Shades?](https://community.hubitat.com/t/switchbot-driver-for-roller-shades/164680),
TheGerman initially found MAB detected the Hub 3's temperature and humidity but created no child
devices for any of the four Blind Tilts, even though their fingerprints were being read correctly
([post #5](https://community.hubitat.com/t/switchbot-driver-for-roller-shades/164680/5)). After
uninstalling and reinstalling a few times, discovery picked them up correctly
([post #8](https://community.hubitat.com/t/switchbot-driver-for-roller-shades/164680/8),
2026-07-05). No root cause was identified; if a Hub 3's Blind Tilts don't appear after the first
**Discover All**, try removing and re-adding the bridge device before assuming they are
unsupported.

**The Hub 3 also bridges a built-in Motion Sensor and three built-in Buttons** — visible in
TheGerman's posted device fingerprint (`Hub 3 MotionSensor`, cluster `0x0406`; `Hub 3 Button 1/2/3`,
cluster `0x003B`) alongside the temperature/humidity sensors. Nobody has reported testing whether
motion or button events actually arrive — MAB reads and creates the child devices, but that is as
far as the evidence goes.

## Working

| Device type | Evidence | Notes |
|---|---|---|
| Temperature sensor (built-in) | Confirmed | Built into the Hub 2 [#34](https://community.hubitat.com/t/-/135252/34) and the Hub 3 ([topic 160265, post #9](https://community.hubitat.com/t/switchbot-matter-pairing/160265/9)) |
| Humidity sensor (built-in) | Confirmed | Built into the Hub 2 [#34](https://community.hubitat.com/t/-/135252/34) and the Hub 3 ([topic 160265, post #9](https://community.hubitat.com/t/switchbot-matter-pairing/160265/9)) |
| Motion sensor (built-in) | Implemented, unverified | Hub 3 only. Discovered by MAB (device fingerprint in [topic 164680, post #5](https://community.hubitat.com/t/switchbot-driver-for-roller-shades/164680/5)); no report yet that motion events actually arrive. |
| Buttons (built-in, ×3) | Implemented, unverified | Hub 3 only. Discovered by MAB, same fingerprint as above; no report yet that button events actually arrive. |
| Curtain motor | Confirmed | Behind Hub 2 since [#34](https://community.hubitat.com/t/-/135252/34) (2024-04-08) and Hub Mini Matter Enabled since [#447](https://community.hubitat.com/t/-/135252/447) (2026-08-16). Must be added as a bridged device in the SwitchBot app on the Hub Mini Matter Enabled — see setup note above. A status-reporting bug (stuck on "Opening"/"Closing", wrong switch state) was reported behind Hub 2 in [#331](https://community.hubitat.com/t/-/135252/331) (kwon2288, 2026-02-11) and fixed the next day in 1.7.6 ([#334](https://community.hubitat.com/t/-/135252/334)). |
| Blind Tilt | Confirmed | Behind Hub 3. Must use MAB, not Hubitat's stock driver — see the "SwitchBot Hub 3" section above for the Close/position bug the stock driver has. Discovery can require a clean reinstall of the bridge device. |
| Bot | Confirmed | Behind Hub 2 since [#225](https://community.hubitat.com/t/-/135252/225) (iEnam, 2025-01-21) — occasionally needs a second `on()` command, or works if the hub is pinged first — and behind Hub Mini Matter Enabled since [#447](https://community.hubitat.com/t/-/135252/447) (Radial, 2026-08-16). Must be added as a bridged device in the SwitchBot app on the Hub Mini Matter Enabled — see setup note above. |
| Standing Fan | Partial | Behind Hub 2 [#438](https://community.hubitat.com/t/-/135252/438) (iEnam, 2026-07-07). The fan speed selector works; the **Cycle Speed** command errors — open, unresolved as of 2026-08-21. |
| Meter Pro (temperature/humidity/CO2) | Partial | A standalone sensor, not the hub's built-in one. Temperature and humidity confirmed behind Hub Mini Matter Enabled [#314](https://community.hubitat.com/t/-/135252/314) (spookypitboss86, 2026-02-04). **CO2 is not exposed over Matter at all** — confirmed by inspecting the device's own Matter fingerprint [#321](https://community.hubitat.com/t/-/135252/321)–[#324](https://community.hubitat.com/t/-/135252/324) (2026-02-05); this is a gap in SwitchBot's Matter firmware, not something MAB can work around. A separate, earlier report [#163](https://community.hubitat.com/t/-/135252/163) (brianstk, 2024-12-15) found a Meter Pro behind Hub 2 reporting temperature incorrectly (fixed at 32) with humidity missing entirely — unclear if the same model/firmware as the 2026 report; no retest was posted. |
| Battery level reporting | Confirmed | |

## Known issues

- **SwitchBot door locks are not tested through MAB.** kkossev's expectation, stated in
  [#259](https://community.hubitat.com/t/-/135252/259) (2025-06-27), is that they work through
  Hubitat's own built-in "Generic Matter Bridge" driver instead of through this package.
- **A SwitchBot hub firmware update can silently stop MAB devices from responding.** Reported fix:
  open the bridge device and run **Initialize**
  ([#294](https://community.hubitat.com/t/-/135252/294), LearningHubitat, 2026-01-02).
- **Pairing or re-pairing a SwitchBot Hub 2 to Hubitat has been unreliable for some users** —
  several reports of a hub that would not pair or stopped working after months, eventually
  succeeding on a later retry or after a driver update
  ([#77](https://community.hubitat.com/t/-/135252/77),
  [#304](https://community.hubitat.com/t/-/135252/304),
  [#438](https://community.hubitat.com/t/-/135252/438)). No single root cause has been identified.

## Untested

Everything else. SwitchBot's range includes plugs, standalone locks, and more sensors, and none
has been tried through the bridge — they are **Unknown**, not unsupported. (Blind Tilt and the
Hub 3's built-in motion/button sensors are covered above — see the "SwitchBot Hub 3" section.)

If you have SwitchBot devices bridged into Hubitat, a report in the
[community thread](../help/support-and-links.md) would fill in the largest gap in this
documentation. Name the device, the hub firmware, and the driver version.

## A note on the SwitchBot Button driver

The package includes a `Matter Generic Component SwitchBot Button` driver, contributed in 2024.
**It is deprecated** — use the [Button](../drivers/button.md) driver instead.

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
- [SwitchBot device Matter compatibility](https://support.switch-bot.com/hc/en-us/articles/38979658026519-SwitchBot-Device-Matter-Compatibility)
- [SwitchBot hub comparison](https://support.switch-bot.com/hc/en-us/articles/13569889726743-Differences-Between-Hub-Mini-Hub-2-Hub-Mini-Matter-Enabled-and-Hub-3)
