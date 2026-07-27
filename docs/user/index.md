# Matter Advanced Bridge

A Hubitat Elevation driver package for Matter bridges. The parent driver discovers the devices
behind a Matter bridge, creates one Hubitat child device per endpoint, subscribes to the supported
Matter attributes and events, and routes reports to the children.

Supported bridges include Zemismart M1, Aqara M3 and E1, SwitchBot Hub 2, IKEA DIRIGERA, Philips Hue,
and the Home Assistant Matter bridge.

- **Author:** Krassimir Kossev (kkossev)
- **License:** Apache 2.0
- **Current release:** 1.8.8
- **Community thread:**
  <https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009>

---

## Documentation is being migrated

This directory is the new home for user documentation. It is **not complete yet.**

**For current documentation, use the Wiki:**
<https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki>

The Wiki remains the authoritative user documentation until this migration finishes. It will not be
deleted; when the migration completes, its pages will link here.

Migration status is tracked in `docs/maintainer/plans/DOCUMENTATION_MIGRATION_PLAN.md`, which is a
local maintainer document and is not part of this repository.

## Planned structure

| Section | Contents |
|---|---|
| Getting started | Installation, using Matter devices |
| Configuration | Preferences, commands and states |
| Drivers | One page per driver, plus the device-type to driver assignment table |
| Compatibility | Overview, device types, hub and device matrix |
| Matter bridges | Aqara, Philips Hue, SwitchBot, Tuya / Zemismart, others |
| Help | Troubleshooting, known issues, support and links |
| Project | Revision history |

## A note on accuracy

Pages carry a status line:

```text
Applies to: <version> | Last verified: YYYY-MM-DD | Status: Current | Experimental | Historical
```

`Historical` means the content was migrated from the Wiki but has not yet been re-verified against
the current release. Much of the Wiki content dates from March 2024, so treat anything marked
`Historical` as a starting point rather than a current statement of behaviour.

Compatibility claims use explicit evidence labels — **Confirmed**, **Reported**, **Implemented,
unverified**, **Unsupported**, **Unknown**, **Historical** — so that a tested device is
distinguishable from one that merely ought to work.


---

<!-- MIGRATED from wiki page `Home` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->

## Imported wiki content: `Home`

*Pending merge in phase 2. Preserved verbatim.*

# [Welcome to the Hubitat---Matter-Advanced-Bridge wiki!](index.md)

## [RELEASE] Matter Advanced Bridge (limited device support)

A Hubitat community-created Matter Advanced Bridge package has now been released.

It brings some of the Tuya, Aqara, Hue, Switchbot, (and probably others) Zigbee devices to HE using the Matter Bridge protocol.

Note that not all devices supported by these hubs will be available in HE using this driver. So, consider this an enthusiast's experimental project exploring new technologies and alternative ways to bring IoT devices from different brands to Hubitat.

The package is available for installation via Hubtat Package Manager ([HPM](https://community.hubitat.com/t/release-hubitat-package-manager-hpm-hubitatcommunity/94471)).

Please follow the [instructions](index.md) on installing, configuring, and using the Matter Advanced Bridge published as GitHub Wiki pages.

Many thanks to all who participated in the alpha-testing and helped with this project!

-------------------------


| Brand | Features | Links |
|----------|----------|----------|
|# **Zemismart M1**<br>![image](assets/images/index-01.png) | [WiKi](bridges/tuya-zemismart.md)| [Zemismart .com](https://www.zemismart.com/products/m1?DIST=RkNBHVU=)<br>[Amazon .com](https://amzn.to/4eOIkl1)<br>|
|# **Zemismart M6** <br> ![image](assets/images/index-02.png) | [WiKi](bridges/tuya-zemismart.md)| [Zemismart .com](https://www.zemismart.com/products/m6?DIST=RkNBHVU=)<br>[Amazon .com](https://amzn.to/3A03MVh)|
|# **MOES**<br> ![image](assets/images/index-03.png) | [WiKi](bridges/tuya-zemismart.md) | [Moeshouse .com](https://moeshouse.com/products/tuya-zigbee-matter-thread-gateway-smart-home-bridge-matter-hub?ref=zxlmpoay&variant=47548663923003)<br>[AliExpress](https://s.click.aliexpress.com/e/_DdlgBxP)<br>[Amazon .de](https://amzn.to/3YwslS3) |
|# **Tuya other brands** <br> ![image](assets/images/index-04.png) | [WiKi](bridges/tuya-zemismart.md) | [AliExpress](https://s.click.aliexpress.com/e/_DDxf0i1)<br>[AliExpress](https://s.click.aliexpress.com/e/_DkUF6Lx)<br>[AliExpress (GIRIER)](https://s.click.aliexpress.com/e/_DdgepGZ)<br>[Amazon .de](https://amzn.to/3NxzTyw)<br>[Amazon .de](https://amzn.to/409clHT) |
| | | |
|#  **Aqara Hub M100** <br> ![image](assets/images/index-05.png)|  [WiKi](bridges/aqara.md)| [Amazon](https://geni.us/XQ4q4W)|
|#  **Aqara Hub E1** <br> ![image](assets/images/index-06.png)| [WiKi](bridges/aqara.md) |[Amazon .com](https://amzn.to/3YwttVN)<br>[Amazon .co.uk](https://amzn.to/48agOMk) <br>[Amazon .de](https://amzn.to/3Yd9iM3)|
|#  **Aqara M2** <br> ![image](assets/images/index-07.png)|[WiKi](bridges/aqara.md) |[Amazon .com](https://amzn.to/4f6iDwc)<br>[Amazon .co.uk](https://amzn.to/3Nvqjwk) <br>[Amazon .de](https://amzn.to/4f3CJaZ)|
|#  **Aqara M3** <br> ![image](assets/images/index-08.png)|[WiKi](bridges/aqara.md) |[Amazon .com](https://amzn.to/3BMrBAd)  <br>[Amazon .co.uk](https://amzn.to/3BLthKy) <br>[Amazon .de](https://amzn.to/3NxoezU)|
|# **Aqara G3 **<br> ![image](assets/images/index-09.png)|[WiKi](bridges/aqara.md) | [Amazon .com](https://amzn.to/3Y9A6x0)<br>[Amazon .co.uk](https://amzn.to/3BJwZUM)<br>[Amazon .de](https://amzn.to/3Ys5TKP)|
| | | |
|# **BOSCH**<br>![image](assets/images/index-10.png) | | [Amazon .de](https://amzn.to/3Uf4Lb7)|
| | | | 
|# **Philips Hue**<br> ![image](assets/images/index-11.png)|[WiKi](bridges/philips-hue.md) | <br>[Amazon .com](https://amzn.to/40f8clX)<br>[Amazon .co.uk](https://amzn.to/3Y8MkFX)<br>[Amazon .de](https://amzn.to/3NzT9eI)|
| | | |
|# **SwitchBot Hub 2**<br> ![image](assets/images/index-12.png) | | [Amazon .com](https://amzn.to/48eubLz)<br> [Amazon .co.uk](https://amzn.to/3BSzTXt) <br> [Amazon .de](https://amzn.to/4dRoy7d)|
|# **Sonoff iHost** <br> ![image](assets/images/index-13.png) |[WiKi](bridges/switchbot.md) | [itead .cc](https://itead.cc/product/sonoff-ihost-smart-home-hub/ref/221/)<br>[Amazon .com](https://amzn.to/3YeO00G) <br>[Amazon .co.uk](https://amzn.to/3Ys2jQK)<br> [Amazon .de](https://amzn.to/3Y8rIhb) <br>|
| | | |
|# Ikea DIRIGERA<br>![image](assets/images/index-14.png) | | [Ikea .com](https://www.ikea.com/us/en/p/dirigera-hub-for-smart-products-white-smart-50503414/)|






---

<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-Home` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->

## Imported wiki content: `Matter-Advanced-Bridge-‐-Home`

*Pending merge in phase 2. Preserved verbatim.*


* ### [Installation](getting-started/installation.md)

* ### [Commands and States](configuration/commands-and-states.md)

* ### [Preferences](configuration/preferences.md)

* ### [Device Types supported](compatibility/device-types.md)
  * #### [Door Locks (W.I.P)](drivers/door-lock.md)

* ### [Hubs and devices compatibility matrix](compatibility/matrix.md)
  * ### [Tuya (Zemismart M1)](bridges/tuya-zemismart.md)
  * ### [Aqara](bridges/aqara.md)
  * ### [Philips Hue](bridges/philips-hue.md)
  * ### [Switchbot Hub2](bridges/switchbot.md)
  * ### [Other Matter Bridges](bridges/other-bridges.md)
  * ### [use with Matter devices](getting-started/use-with-matter-devices.md)

* ### [Troubleshooting](help/troubleshooting.md)

* ### [Known issues](help/known-issues.md)

* ### [Links](help/support-and-links.md)


--------

#### [Revisions history](project/revisions-history.md)
#### [TODO list](https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki/Matter-Advanced-Bridge-%E2%80%90-TODO-list)
--------

[next page](configuration/commands-and-states.md) 

[back to Matter Advanced Bridge main page](index.md)


