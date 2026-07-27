# Matter Advanced Bridge

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

The parent driver. You assign this one by hand, to the Matter bridge itself; every other driver in
the package is assigned automatically to the child devices it creates.

- **Namespace:** `kkossev`
- **Source:** `Matter_Advanced_Bridge.groovy`
- **Assigned to:** the bridge device, manually — see [Installation](../getting-started/installation.md)

## What it does

- Discovers the bridge, its endpoints, and the devices behind it.
- Creates one Hubitat child device per endpoint, choosing a driver from the clusters that endpoint
  reports. See [Which driver do I get?](index.md).
- Subscribes to the supported Matter attributes and events, and routes every incoming report to the
  right child.
- Sends commands from the children back out to the bridge.
- Checks that the bridge is still reachable, and reports `healthStatus`.

The children never talk to the bridge themselves. Everything passes through this driver, which is
why its logs are the place to look when a child device misbehaves.

## Capabilities

`Actuator` · `Sensor` · `Refresh` · `Health Check` · `Battery`

`Battery` is there because a bridge may expose a Power Source cluster on its root node. Most
mains-powered bridges never report it.

**`Initialize` is deliberately not declared.** It is commented out in the source so that a hub reboot
or driver update does not silently re-subscribe every attribute — see
[Commands and states](../configuration/commands-and-states.md).

## Attributes

`Status` · `healthStatus` · `rtt` · `deviceCount` · `endpointsCount` · `initializeCtr` ·
`nodeLabel` · `productName` · `softwareVersionString` · `rebootCount` · `upTime` ·
`totalOperationalHours` · `reachable` · `battery` · `batteryVoltage`

Not all of them appear — a bridge only publishes what it reports about itself.
[Commands and states](../configuration/commands-and-states.md) explains each one, and covers the
`ipAddress` and `networkStatus` states that Hubitat adds.

## Commands

`_DiscoverAll` · `Get Info` · `Identify` · `Load All Defaults` · `Ping` · `Refresh` ·
`Re Subscribe` · `Utilities`

Documented in full, including every `Utilities` entry, in
[Commands and states](../configuration/commands-and-states.md).

## Preferences

Three basic settings — descriptionText logging, debug logging, and an **Advanced Options** switch —
plus nine advanced ones covering the health check, discovery timeouts, and subscription intervals.
See [Preferences](../configuration/preferences.md).

## Bridge-specific notes

- The driver runs single-threaded, so commands and incoming reports are processed in order.
- Discovery always pings the bridge first and stops if there is no answer, rather than working
  through a long sequence that cannot succeed.
- Some bridges need longer than the defaults allow. **Discovery timeout scale** exists for that.
- The Zemismart M1 Hub reports its battery percentage in a form the driver has to correct; it
  applies that patch automatically, for that model only.

## Known limitations

- A device type the driver does not recognise still becomes a child device, with the
  `Generic Component Switch` driver and the product name `Unknown`.
- Each endpoint becomes its own child device. A multi-sensor arrives as several devices rather than
  one — see [Known issues](../help/known-issues.md).
- The driver can only expose what the bridge chooses to share. A device missing from Hubitat is
  usually missing from the bridge's Matter output, not lost by this driver.

## See also

- [Installation](../getting-started/installation.md)
- [Which driver do I get?](index.md)
- [Troubleshooting](../help/troubleshooting.md)
