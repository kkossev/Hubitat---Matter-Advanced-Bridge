<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-known-issues` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->


_last updated 03/11/2024_

## Known Matter Advanced Bridge driver issues

* Performance may not be optimal because of the huge state variables. Optimizing the speed/performance will be the project's second stage after the BETA release.
* As the bridged devices are created in Hubitat as child devices, these can not be shared via Hubitat Mesh and can not be used in the HE Swap app.
* Locks can not be controlled.
* Button events are not received.
* No composite devices (a temperature and humidity sensor will create 3 different child devices - T, H, and battery device).
-----------------------------

## Known Hubitat Matter platform limitations and issues : 

* initialize() method is called on every Matter error - resulting in multiple subscriptions to one and the same attribute.
* unsubscribe() resets all Matter connections - results in re-subscribing all other Matter devices connected to the same HE hub
* Hubitat is not notified when a Matter device has been reset (powered off/on) - the subscriptions (automatic reporting) are lost!
* Matter interface status (operational/failure) can not be checked by the driver - it is unknown whether the bridge is online or offline before sending a ping command.
* Subscription to Matter events does not work - this prevents buttons/scene switches supprot in Hubitat. 
* There is no decoding of the Maatter complex data structures - Hue bridge rooms as example can not be imported.
* Hubitat parser for some matter messages throws exception java.lang.ArrayIndexOutOfBoundsException - the effect of this problem is unknown.
* Matter writeAttribute commands do not work [link](https://community.hubitat.com/t/writing-matter-attributes-what-am-i-doing-wrong/135001/1)


----------------------------

[next page](support-and-links.md)

([back to Matter Advanced Bridge main page](../index.md))
