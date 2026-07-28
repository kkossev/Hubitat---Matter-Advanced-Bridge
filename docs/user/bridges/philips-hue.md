# Philips Hue

Applies to: 1.9.0 | Last verified: 2026-07-28 | Status: Current

Philips Hue supports Matter through both the square **Hue Bridge v2** and the **Hue Bridge Pro**.
The original MAB device results below were tested with a Hue Bridge v2 in 2024; newer entries are
labelled separately where the Hue bridge support and MAB code both exist but the combination has
not yet had a named live-device test.

Philips Hue says that new and existing Hue lights and accessories connected through either bridge
work with Matter. Its two explicit exceptions are the **Hue Play HDMI sync box** and the **rotary
dial action** on the Tap dial switch. Products connected only through Bluetooth are not bridged.
See [Philips Hue and Matter](https://www.philips-hue.com/en-gb/explore-hue/works-with/matter).

Labels: **Confirmed** — tested working with MAB. **Implemented, unverified** — Hue exposes the
required Matter device type and MAB implements it, but no current MAB test has been recorded.
**Unsupported** — explicitly excluded or not exposed. **Unknown** — there is not enough evidence.
See the [compatibility overview](../compatibility/overview.md).

## Working

| Hue device or function | Evidence | MAB result |
|---|---|---|
| White lights — bulbs, lamps, fixtures and lightstrips | Confirmed | On/off and level |
| White ambiance lights | Confirmed | On/off, level and colour temperature |
| White and color ambiance lights | Confirmed | On/off, level, colour temperature and RGB colour |
| Smart plug | Confirmed | On/off |
| Indoor and outdoor motion sensors — occupancy | Confirmed | Motion |
| Indoor and outdoor motion sensors — temperature | Confirmed | Temperature |

The light rows cover the corresponding Hue bulbs, lamps, ceiling and wall fixtures, and lightstrips.
Matter exposes their common light controls, not Hue-specific features such as gradients,
Entertainment areas, dynamic scenes, or effects.

Hue does not report colour mode back when a light is changed from another system, so Hubitat may
show a stale colour mode until the light reports again.

## Supported, needs a MAB test

These are no longer merely speculative. Signify's current
[Matter 1.3 certification for the Hue Bridge platform](https://csa-iot.org/csa_product/hue-bridge-matter-platform-2/)
includes the Switch, Occupancy Sensing, Illuminance Measurement, Temperature Measurement, Boolean
State, and Power Source clusters. Public
[Hue Bridge endpoint telemetry](https://matter-survey.org/de/device/philips-hue-bridge-4107-2)
also shows Generic Switch, Occupancy Sensor, Light Sensor, and Temperature Sensor endpoints.

| Hue device or function | Evidence | Expected MAB result |
|---|---|---|
| Indoor and outdoor motion sensors — ambient light | Implemented, unverified | A separate illuminance child using `Generic Component Omni Sensor` |
| Secure contact sensor | Implemented, unverified | Contact sensor; Hue Bridge firmware added Matter support for it in January 2024 |
| Dimmer switch — original and current models | Implemented, unverified | Button child; press, hold and release events depend on the switch FeatureMap |
| Smart button | Implemented, unverified | Button child |
| Hue Tap switch | Implemented, unverified | Button child |
| Tap dial switch — four buttons | Implemented, unverified | Button child; the rotary dial itself is not exposed through Matter |
| Wall switch module — battery model and 2026 wired control module | Implemented, unverified | Button child |
| Wired On/Off Switch — 2026, one- or two-channel | Implemented, unverified | One or two switch children; Hue documents each controlled load as a Matter light |
| Wired Dimmer Switch — 2026, one-channel | Implemented, unverified | Dimmer child; Hue documents the controlled load as a Matter light |
| Battery level for Hue switches and sensors | Unknown | The Hue platform is certified for Power Source, but no MAB result for accessory battery endpoints has been recorded |

The button rows are expected to work with the current
[Button](../drivers/button.md) driver. The old 2024 result predated the MAB 1.5.6 event-subscription
fix and is not evidence that current Hue buttons fail.

The Secure contact sensor is explicitly named in the
[Hue Bridge release notes](https://www.philips-hue.com/en-gb/support/release-notes/bridge):
Matter support was added in bridge software version `1962097030`, released 2024-01-04.

## Not exposed, limited, or unknown

| Hue device or feature | Evidence |
|---|---|
| Hue Play HDMI sync box | Unsupported — Philips Hue explicitly excludes it from Matter |
| Tap dial switch rotary dial | Unsupported — Philips Hue explicitly excludes the dial; the four buttons are separate |
| Hue scenes, Rooms, Zones, Entertainment areas and dynamic effects | Not exposed to MAB as child devices — the Hue app can export scenes to Apple Home, but that controller-specific workflow is not a discoverable MAB endpoint |
| Gradient segments | Unsupported — a gradient product is exposed as a light, without per-segment control |
| Hue Secure cameras, video doorbell and smart chime | Unknown — the current Hue Bridge Matter certification does not declare the Camera AV Stream Management cluster; generic “Matter” compatibility on a product page is not evidence that MAB will discover a camera endpoint |
| Friends of Hue switches and other third-party Zigbee accessories | Unknown — Hue's “all products” statement covers Philips Hue products; no complete Matter export list for third-party accessories was found |
| Third-party Zigbee lights paired to the Hue Bridge | Unknown — the bridge accepts some third-party lights, but Philips Hue does not publish a model-by-model Matter export guarantee |

## Research basis

- [Philips Hue and Matter](https://www.philips-hue.com/en-gb/explore-hue/works-with/matter) —
  current scope and the two explicit exclusions.
- [CSA Matter 1.3 certification](https://csa-iot.org/csa_product/hue-bridge-matter-platform-2/) —
  certified 2025-08-28 for Hue Bridge product IDs `0x0002` and `0x0003`.
- [Hue Bridge release notes](https://www.philips-hue.com/en-gb/support/release-notes/bridge) —
  Matter launch and Secure contact sensor support.
- [Hue in-wall solutions](https://www.philips-hue.com/en-gb/support/article/wall-switch-modules/000016) —
  the 2026 wired load switches are Matter-compatible as lights; control-only modules use Matter through the bridge.
- [Public Hue Bridge Matter telemetry](https://matter-survey.org/de/device/philips-hue-bridge-4107-2) —
  observed Generic Switch, Occupancy Sensor, Light Sensor, and Temperature Sensor endpoint shapes.

## See also

- [Compatibility matrix](../compatibility/matrix.md)
- [Which driver do I get?](../drivers/index.md)
