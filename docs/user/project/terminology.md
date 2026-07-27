# Terminology

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Words used consistently throughout this documentation. Several of them are used loosely elsewhere,
which is the reason for the page.

## The package

**Matter Advanced Bridge** — this driver package. Referred to as "the package" or "the driver" here;
never abbreviated to MAB in user documentation.

**Parent driver** — the `Matter Advanced Bridge` driver itself, assigned to the bridge device. It
does the discovery, holds the subscriptions, and routes every message. See
[the parent driver page](../drivers/matter-advanced-bridge.md).

**Component driver**, or **child driver** — the driver on each device the bridge exposes. Some ship
with this package, some are Hubitat's own. Both are covered under
[Drivers](../drivers/index.md).

**Child device** — the Hubitat device created for one endpoint behind the bridge.

## Matter

**Matter bridge** — a hub that exposes devices from another ecosystem, usually Zigbee, over Matter.
An Aqara or Zemismart hub, for example.

**Bridged device** — a device behind such a bridge. It is not itself a Matter device; the bridge
speaks Matter on its behalf.

**Native Matter device** — a device that speaks Matter itself and can pair directly to a hub, with
no bridge involved. See [Using it with Matter devices](../getting-started/use-with-matter-devices.md).

**Endpoint** — one addressable function within a Matter node. One physical device can present
several endpoints, which is why a multi-sensor becomes several Hubitat child devices. Endpoint 0 is
always the node itself.

**Cluster** — a group of related functionality on an endpoint, identified by a number such as
`0x0006` On/Off. **Which clusters an endpoint reports is what decides its driver.**

**Attribute** — a value within a cluster, such as the on/off state. **Command** — an action, such as
turning something on. **Event** — something that happened, such as a button press. Matter treats
these as three distinct things, and buttons need events rather than attributes.

**Device type** — Matter's own classification of an endpoint, such as `0x0100` On/Off Light. Used
here only as a tie-breaker; the clusters decide the driver.

**Discovery** — working out what a bridge exposes and creating child devices for it. Run with
`_DiscoverAll`.

**Subscription** — the standing request that makes a bridge report changes as they happen.
**Re-subscription** — sending that request again, with the **Re Subscribe** command.

**Refresh** — reading current values once, on demand, rather than waiting for a report.

## Hubitat

**Hubitat Elevation** — the hub platform. "Hubitat" alone is acceptable; "HE" is used in the driver
logs but not in this documentation.

**Capability** — Hubitat's model of what a device can do, such as `Switch` or `MotionSensor`. A
driver declares capabilities; apps and dashboards use them.

**Stock driver** — a driver shipped with Hubitat rather than by this package. See
[Hubitat stock drivers](../drivers/stock-drivers.md).

## Support language

Used precisely, and defined in full in the
[compatibility overview](../compatibility/overview.md):

| Term | Meaning |
|---|---|
| **Confirmed** | Tested working on a named hub, bridge, and device. |
| **Reported** | A user reported it; not reproduced by the maintainer. |
| **Implemented, unverified** | The code exists; no live-device test. |
| **Unsupported** | Known not to work, or intentionally not supported. |
| **Unknown** | No reliable evidence. Usually nobody has tried. |
| **Experimental** | Present and usable, but incomplete and subject to change. |

**Supported** on its own means *the driver implements the relevant cluster*. It is not a promise
that a specific device works, because that also depends on the bridge.

## Spelling

**SwitchBot**, not Switchbot or Swithbot. **Zemismart**. **Aqara**. **Philips Hue**, or Hue.
**DIRIGERA**. Product names follow the manufacturer's own spelling, except when quoting a source
that got it wrong.

## See also

- [Compatibility overview](../compatibility/overview.md)
- [Which driver do I get?](../drivers/index.md)
