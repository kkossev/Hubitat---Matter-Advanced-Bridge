# Using it with Matter devices

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

As a side effect of how it works, this package can be used not only with Matter **bridges**, but also
with Matter devices paired directly to a Hubitat hub.

That works because a Zigbee device presented by a Matter bridge looks very similar to a native Matter
device paired directly: the same clusters, read the same way.

## How

The steps are the same as for a bridge — see [Installation](installation.md). Pair the device to
Hubitat as a standard Matter device, change its **Type** to `Matter Advanced Bridge`, then
**Initialize** and run **_DiscoverAll**.

The device's endpoints become child devices, exactly as bridged devices do. A single-node device
reports its battery on the root node, and the driver passes that through to the child rather than
leaving it on the parent.

## Devices known to work

| Device | Evidence |
|---|---|
| Aqara Matter Contact Sensor P2 | Reported |
| Eve Energy (Europe) | Reported |
| Nanoleaf A19 Matter | Reported |
| Zemismart Matter Bulb | Reported |

This list is not exhaustive — it is simply what has been tried. A directly-paired Matter device that
reports one of the clusters in [Which driver do I get?](../drivers/index.md) has a good chance of
working.

![A directly-paired Matter device using this driver](../assets/images/getting-started-use-with-matter-devices-01.png)

## Should you?

Hubitat's own stock drivers handle most directly-paired Matter devices, and for a device they
support properly they are the simpler choice. This package is worth trying when the stock driver
does not expose something the device can actually do.

## Next steps

- [Which driver do I get?](../drivers/index.md)
- [Troubleshooting](../help/troubleshooting.md)
