# Hubitat stock drivers

Applies to: 1.8.8 | Last verified: — | Status: Historical

> **Stub — transcribed from source, not yet audited.** Written in phase 1d of the documentation
> migration from `mapMatterCategory()` in `Matter_Advanced_Bridge.groovy`. There was no wiki page to
> migrate.

Not every child device uses a driver from this package. For ten common device kinds the parent
assigns a **Hubitat stock component driver** instead, because the built-in driver already does the
job.

**These drivers are written and maintained by Hubitat, not by this project.** If one of them
misbehaves — a missing attribute, a wrong unit, a dashboard template problem — that is a Hubitat
issue and reporting it in this project's community thread will not get it fixed. Report it to
Hubitat. What *is* in scope here is the parent sending the wrong data to the child, or picking the
wrong driver in the first place.

## Which stock drivers are used

| Driver | Assigned when the endpoint reports | Purpose |
|---|---|---|
| `Generic Component RGBW` | `0x0300` Color Control **and** device type `0x010D` Extended Color Light | Full-colour bulbs |
| `Generic Component CT` | `0x0300` Color Control without the Extended Color device type | Colour-temperature bulbs |
| `Generic Component Dimmer` | `0x0008` Level Control | Dimmers and dimmable bulbs |
| `Generic Component Water Sensor` | `0x0045` Boolean State with device type `0x0043` | Water leak detectors |
| `Generic Component Contact Sensor` | `0x0045` Boolean State with device type `0x0015`, without `0x0080` | Contact sensors |
| `Generic Component Thermostat` | `0x0201` Thermostat | Thermostats |
| `Generic Component Fan Control` | `0x0202` Fan Control | Fans |
| `Generic Component Omni Sensor` | `0x0400` Illuminance, `0x0402` Temperature, or `0x0405` Humidity | Multi-purpose sensor |
| `Generic Component Pressure Sensor` | `0x0403` Pressure Measurement | Pressure sensors |
| `Generic Component Switch` | `0x0006` fallback, and for any endpoint matching nothing else | Switches and unknown devices |

`Generic Component Omni Sensor` covers three separate clusters, so one Omni Sensor child may carry
illuminance, temperature, and humidity together.

`Generic Component Switch` appears twice in that table for different reasons: as a real match, and as
the final fallback for an endpoint whose clusters are not recognised. A child on this driver with a
product name of `Unknown` is the fallback case.

## Where they come from

Stock component drivers ship with the Hubitat platform. There is nothing to install and nothing in
this package's HPM bundle for them. See Hubitat's own driver documentation at
<https://docs2.hubitat.com/> for their capabilities and attributes.

## Changing a stock driver to a custom one

You can switch a child to any driver via the **Type** field on the device page, but the parent will
still only send what the endpoint actually reports. See
[Which driver do I get?](index.md#changing-the-driver-manually).
