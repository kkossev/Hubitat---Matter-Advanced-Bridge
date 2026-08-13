# Aqara

Applies to: 1.9.0 | Last verified: 2026-08-13 | Status: Current

This page combines the earlier Aqara compatibility record with every Aqara report in the public
[Matter Advanced Bridge community thread](https://community.hubitat.com/t/-/135252/1). The complete
public topic was reviewed through post [#439](https://community.hubitat.com/t/-/135252/439) on
2026-07-28: 436 available posts, with three deleted post-number gaps.

Labels: **Confirmed** — reported working with MAB. **Partial** — exported, but with a documented
limitation. **Implemented, unverified** — MAB gained the needed support after the report, but no
follow-up test was posted. **Unknown** — no MAB test was found. See the
[compatibility overview](../compatibility/overview.md).

## Matter-capable hubs

| Hub | Aqara Matter status | MAB evidence |
|---|---|---|
| Hub E1 | Matter bridge | **Confirmed** — U100 lock [#90](https://community.hubitat.com/t/-/135252/90), stable bridge connection [#224](https://community.hubitat.com/t/-/135252/224), and leak sensors [#266](https://community.hubitat.com/t/-/135252/266) |
| Hub M1S | Matter bridge | **Confirmed** — contact sensors and single/double wall switches [#21](https://community.hubitat.com/t/-/135252/21); contact-sensor model identified in [#129](https://community.hubitat.com/t/-/135252/129) |
| Hub M1S Gen 2 | Matter bridge | **Unknown** — the thread names M1S, not M1S Gen 2 |
| Hub M2 | Matter bridge via firmware update | **Confirmed** — FP2 signals used with MAB [#367](https://community.hubitat.com/t/-/135252/367)–[#368](https://community.hubitat.com/t/-/135252/368) |
| Camera Hub G3 | Matter bridge | **Confirmed** — several Aqara wall switches imported [#432](https://community.hubitat.com/t/-/135252/432)–[#439](https://community.hubitat.com/t/-/135252/439) |
| Hub M3 | Matter bridge and controller | **Confirmed** — multiple independent reports, including sensors [#92](https://community.hubitat.com/t/-/135252/92), FP1E [#137](https://community.hubitat.com/t/-/135252/137), and E1 TRV [#149](https://community.hubitat.com/t/-/135252/149) |
| Camera Hub G5 Pro | Matter bridge and controller | Unknown — not tested with MAB |
| Hub M100 | Matter bridge and controller | Unknown — not tested with MAB |
| Doorbell Camera Hub G410 | Matter bridge and controller | **Confirmed** — E1 TRV discovery, setpoint and temperature [#278](https://community.hubitat.com/t/-/135252/278)–[#287](https://community.hubitat.com/t/-/135252/287) |
| Hub M200 | Matter bridge and controller | Unknown — not tested with MAB |

The M3 firmware 4.1.17 discovery regression reported in [#122](https://community.hubitat.com/t/-/135252/122) was fixed and tested in MAB 1.1.2 [#123](https://community.hubitat.com/t/-/135252/123).

For camera and doorbell hubs, this table refers to their Matter-bridge function for Aqara child
devices. It does not mean their video streams are exposed to Hubitat through MAB.

The **Camera Hub G2H Pro** is deliberately omitted. Aqara's current
[compatibility guidance](https://us.aqara.com/en-ca/collections/smart-home-controller) explicitly
excludes it from Matter bridging, despite the original announcement of a future firmware update.


## Community-confirmed devices

| Aqara Zigbee device | Hub(s) | Result |
|---|---|---|
| Door and Window Sensor | M1S | **Confirmed** — several sensors worked [#21](https://community.hubitat.com/t/-/135252/21); later identified explicitly [#129](https://community.hubitat.com/t/-/135252/129) |
| Single and double wall switches | M1S | **Confirmed** [#21](https://community.hubitat.com/t/-/135252/21) |
| Wall switches | G3, E1 | **Confirmed** — imported as switches, one child per gang. Aqara labels are not imported (see [Limitations](#limitations-and-open-results)); manually assigned Hubitat labels survive rediscovery [#432](https://community.hubitat.com/t/-/135252/432)–[#439](https://community.hubitat.com/t/-/135252/439). A double-gang `Aqara Wall Switch EU` was reproduced through a Hub E1 on 2026-08-13. |
| Temperature/humidity sensor | M3 | **Confirmed** — temperature, humidity and battery were created [#92](https://community.hubitat.com/t/-/135252/92). A T1 also reported all three [#138](https://community.hubitat.com/t/-/135252/138). |
| Vibration Sensor | M3 | **Partial** — exported as motion; its battery endpoint was missing although Apple Home and Home Assistant showed it [#92](https://community.hubitat.com/t/-/135252/92). |
| FP1E presence sensor | M3 | **Confirmed** [#124](https://community.hubitat.com/t/-/135252/124), [#137](https://community.hubitat.com/t/-/135252/137), [#164](https://community.hubitat.com/t/-/135252/164) |
| Wireless Remote Switch H1, Double Rocker | M3 | **Partial** — battery works [#138](https://community.hubitat.com/t/-/135252/138); after Button support was added, Aqara exposed only single-click events [#299](https://community.hubitat.com/t/-/135252/299). |
| Thermostat E1 TRV | M3, G410 | **Confirmed** — tested through M3 [#149](https://community.hubitat.com/t/-/135252/149); setpoint and internal temperature work through G410, with slow temperature updates [#278](https://community.hubitat.com/t/-/135252/278)–[#287](https://community.hubitat.com/t/-/135252/287). |
| FP2 presence zones via Aqara **signals** | M3, M2 | **Confirmed, cloud-dependent** — M3 signals and zones [#237](https://community.hubitat.com/t/-/135252/237)–[#243](https://community.hubitat.com/t/-/135252/243); M2 workflow [#367](https://community.hubitat.com/t/-/135252/367)–[#368](https://community.hubitat.com/t/-/135252/368). This is Advanced Matter Bridging, not direct export of the FP2 device. |
| Ceiling Light T1 | M3 | **Confirmed** — remained controllable locally while FP2 signals stopped during an Internet outage [#240](https://community.hubitat.com/t/-/135252/240). |
| U100 lock | E1 | **Export confirmed** [#90](https://community.hubitat.com/t/-/135252/90). The report predates working lock commands; MAB has supported lock/unlock since 1.5.5, but the thread contains no U100 bridge retest. |
| Water Leak Sensor `SJCGQ11LM` | E1 | **Export confirmed; current mapping unverified** — initially appeared as a contact sensor [#266](https://community.hubitat.com/t/-/135252/266). MAB 1.6.0 later added automatic Water Leak Detector classification, with no posted follow-up test. |

## Earlier Aqara compatibility record

These results were carried over from the original 2024 Aqara testing notes. The complete community
thread did not provide enough detail to associate every item with a specific hub or to revalidate it
against current firmware.

| Device type | Earlier result | Tested model or note |
|---|---|---|
| Relays, on/off | Confirmed | Aqara Double Rocker H1 EU |
| Plugs, on/off | Confirmed | Aqara Smart Plug EU |
| Bulbs — on/off, level, colour temperature | Confirmed | Aqara LED Strip T1 |
| Bulbs — RGBW | Confirmed | Aqara LED Strip T1; colour mode was reported incorrectly in 2024 |
| Motion sensors | Confirmed | Aqara P1, Xiaomi |
| Temperature and humidity | Confirmed | Aqara T1 sensor, Aqara TVOC sensor |
| Light sensor | Confirmed | Aqara Light Detection Sensor T1 |
| Curtain motor | Confirmed | Aqara Curtain Motor |
| Battery level | Confirmed | Either as its own child device, or as part of an existing one |
| Aqara Cube T1 Pro | Confirmed | Exposed as six separate on/off devices |

## Limitations and open results

These are bridge-export limitations or unresolved device-specific behavior found in the thread.

| Device or feature | Current evidence |
|---|---|
| Atmospheric pressure from Aqara temperature/humidity sensors | **Not exposed by M3 or E1.** It was also absent through M3 in Home Assistant and Apple Home [#138](https://community.hubitat.com/t/-/135252/138)–[#142](https://community.hubitat.com/t/-/135252/142). MAB now supports Matter pressure reports, but cannot create data the bridge omits. |
| H1 Double Rocker multi-click/hold | Aqara exposes only single-click events over Matter [#299](https://community.hubitat.com/t/-/135252/299). |
| Aqara mini switch | **Unresolved:** roughly three-minute delay or missed actions through M3 on MAB 1.7.7 [#341](https://community.hubitat.com/t/-/135252/341)–[#342](https://community.hubitat.com/t/-/135252/342). No later retest was posted. |
| Aqara app device names | **Confirmed not exported over Matter.** Aqara hubs publish the *model* name in every Matter name field, so MAB has nothing to import. On a Hub E1 the `NodeLabel`, `ProductName` and `ProductLabel` of a bridged device all read e.g. `Aqara Water Leak Sensor`, and renaming the device in the Aqara Home app changes none of them. Verified on 2026-08-13 across three independent controllers: MAB, Apple Home, and a freshly commissioned Samsung SmartThings fabric. Apple Home may still display your chosen name — Aqara hubs also bridge to Apple over **HomeKit**, a separate protocol from Matter, and Apple additionally keeps its own name for each accessory; neither route is reachable from Hubitat. Give the Hubitat child device a label yourself; it survives rediscovery. |
| G3 wall-switch labels | Functional switches are exported, but their Aqara labels are not imported [#432](https://community.hubitat.com/t/-/135252/432)–[#433](https://community.hubitat.com/t/-/135252/433). Two causes, now separated: the app name is not exported at all (row above), and on a multi-gang switch the individual gang endpoints carry no name field of their own — only their parent bridged-device endpoint does. MAB does not yet inherit that parent name, so each gang becomes a generic `Switch`. |
| G3 unexplained Button children | **Unresolved:** eight extra Button children reported after discovery [#439](https://community.hubitat.com/t/-/135252/439). |
| FP2 signals | Depend on Aqara cloud connectivity even though ordinary bridged devices continue to work locally [#239](https://community.hubitat.com/t/-/135252/239)–[#243](https://community.hubitat.com/t/-/135252/243). |

## Driver support added after older tests

| Device type | Earlier result | Current MAB status |
|---|---|---|
| Door lock — U100 | Exported, but commands did not work in 2024 | Lock/unlock has worked since 1.5.5; lock codes remain experimental. See [Door Lock](../drivers/door-lock.md). |
| Buttons — H1, Xiaomi/Lumi | Events were not handled in 2024 | Button subscriptions were fixed in 1.5.6. Actual events still depend on what the Aqara hub exports. See [Button](../drivers/button.md). |
| Water leak sensors | Exported as contact sensors in 2025 | Automatic Water Leak Detector classification was added in 1.6.0. |
| PM 2.5 — TVOC sensor | Driver did not support it | `pm25` has been supported since 1.3.0. See [Air Purifier](../drivers/air-purifier.md). |
| Smart pet feeder | Motion only | It also depended on lock and button support; current behavior has not been retested. |

## Not supported, or untested

| Device type | Evidence |
|---|---|
| Smoke detectors — Aqara Smart Smoke Detector | Unsupported. Matter's Smoke/CO Alarm device type is not implemented. |
| Aqara dimmers | Unknown — not tested |
| Smart natural gas detector | Unknown — not tested |

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
- [Aqara Advanced Matter Bridging — current Matter Bridge hub list](https://www.aqara.com/en/explore/introducing-advanced-matter-bridging/)
- [Aqara's original Matter rollout announcement — Hub M2 and legacy hub updates](https://www.aqara.com/en/news/aqara-releases-details-around-its-first-matter-compatible-devices/)
- [Matter Advanced Bridge community topic](https://community.hubitat.com/t/-/135252/1)
