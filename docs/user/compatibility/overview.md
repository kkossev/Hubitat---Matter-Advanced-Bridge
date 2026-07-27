# Compatibility overview

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

Whether a device works through this package depends on three things, and they fail independently.

## The three questions

**1. Does your bridge expose the device at all?**

A Matter bridge only shares what its manufacturer decided to share. A device can work perfectly in
the Aqara or Smart Life app and still be invisible over Matter. Nothing on the Hubitat side can fix
that — check the [bridge pages](../bridges/aqara.md) and the
[compatibility matrix](matrix.md) for what each bridge has been seen to expose.

**2. Does the driver understand what it exposes?**

The bridge describes each device by the Matter clusters it reports. If the driver implements those
clusters, the device becomes a working Hubitat child device. See [Device types](device-types.md) for
the full list, and [Which driver do I get?](../drivers/index.md) for how the driver is chosen.

A device reporting nothing recognisable still gets a child device — with the
`Generic Component Switch` driver and the product name `Unknown`. That is a placeholder, not
support.

**3. Does Hubitat have somewhere to put it?**

The driver maps Matter attributes onto Hubitat capabilities. Where Hubitat has no equivalent
concept, the information has nowhere to go — this is why bridged devices arrive as several separate
children rather than one composite device.

## What "supported" means here

**Supported means the driver implements the cluster.** It does not promise your specific device
works, because question 1 is outside this project's control.

That is why the documentation separates the two. [Device types](device-types.md) and the first table
in the [compatibility matrix](matrix.md) are verified against the driver source and are current.
The bridge-by-bridge results in the second half of the matrix are test results, most from 2024, and
are labelled accordingly.

Claims about devices carry an evidence label:

| Label | Meaning |
|---|---|
| **Confirmed** | Tested working on a named hub, bridge, and device combination. |
| **Reported** | A user reported it working; not reproduced here. |
| **Implemented, unverified** | The code exists, but it has not passed a live-device test. |
| **Unsupported** | Known not to work, or intentionally not supported. |
| **Unknown** | No reliable evidence either way. Usually means nobody has tried. |

**Unknown is not a negative.** Most Unknown entries simply mean no one with that bridge and that
device has reported back.

## Where to look

| Question | Page |
|---|---|
| What kinds of device does this handle? | [Device types](device-types.md) |
| Is my exact device type supported, and does my bridge expose it? | [Compatibility matrix](matrix.md) |
| What is known about my specific bridge? | [Aqara](../bridges/aqara.md) · [Philips Hue](../bridges/philips-hue.md) · [SwitchBot](../bridges/switchbot.md) · [Tuya / Zemismart](../bridges/tuya-zemismart.md) · [Other bridges](../bridges/other-bridges.md) |
| Which driver will my device get, and can I change it? | [Which driver do I get?](../drivers/index.md) |
| It should work, but it does not | [Troubleshooting](../help/troubleshooting.md) |

## Adding to this

Compatibility information comes from people testing devices. If you have a result — working or not —
post it in the [community thread](../help/support-and-links.md) with the bridge, its firmware, the
device, and the driver version.
