<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-Aqara` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->


### <b>Aqara E1 Hub as Matter Bridge</b>
*last updated 2024/11/29*

|Device Type                |         Status               |          Remarks                  |
|:--------------------------|:----------------------------:|----------------------------------:|
| Relays - On/Off           | working OK - tested          | Aqara Double Rocker H1 EU         |
| Plugs - On/Off            | working OK - tested          | Aqara Smart Plug EU               |
| Bulbs - On/Off            | working OK - tested          | Aqara LED Strip T1                |
| Bulbs - level control     | working OK - tested          | Aqara LED Strip T1                |
| Bulbs - CT control        | working OK - tested          | Aqara LED Strip T1                |
| Bulbs - RGBW control      | working OK - tested          | Aqara LED Strip T1 (*colorMode is wrong!)|
| Motion Sensors            | working OK - tested          | Aqara P1 Motion Sensor, Xiaomi    |
| Vibration Sensors         | working OK - tested          | Aqara Vibration Sensor (as motion sensor) |
| Temperature Sensor        | working OK - tested          | Aqara Temperature and Humidity Sensor T1, Aqara TVOC sensor   |
| Humidity Sensor           | working OK - tested          | Aqara Temperature and Humidity Sensor T1, Aqara TVOC sensor   |
| Light Sensor T1           | working OK - tested          |Aqara Light Detection Sensor T1    |
| Aqara Cube T1 Pro         | OK (exposed as 6 x OnOff)    | Aqara Cube T1 Pro                 |
| Battery Level reporting   | working OK - tested          | as individual Battery device or as part of an existing child device|
| Aqara Curtain Motor       | working OK - tested          | Aqara Curtain Motor               |
| Thermostats               | working OK - tested          |  Aqara thermostat E1 (TRV) -      |
| Presence sensor FP1E      | working OK                   | Aqara FP1E (the new 2024 model)   |
|:--------------------------|:----------------------------:|----------------------------------:|
| Aqara Door Lock           | Locks not supported in HE (yet)) |  Aqara U100                    |
| Smart Pet Feeder          | partially working (motion only) |  depends on lock and button implementations |
| Door and Window Sensor    | TODO - check it!             |Aqara Door and Window Sensor, old and T1   |
| Wireless Remote Switch    | Buttons not supported in HE (yet)  | Aqara Double Rocker H1 - CRASHES! |
| Wireless button           | Buttons not supported in HE (yet)  | Xiaomi/Lumi            - CRASHES! |
| Wired Remote Switch       | Buttons not supported in HE (yet)  | not implemented                   |
|:--------------------------|:----------------------------:|-----------------------------------:|
| Presence sensor FP1       | NOT working (not exposed via Matter)    | Aqara FP1              |
| Presence sensor FP2       | NOT working (not exposed via Matter)    | Aqara FP2              |
| PM 2.5                    | Not supported in HE (yet)     | Aqara TVOC sensor      |
| Atmospheric pressure      | NOT working (not exposed via Matter)    | Aqara TVOC sensor      |
| Light Detector T1         | NOT working (not exposed via Matter??)  | not tested             |
| Xiaomi(Lumi) Light Sensor | NOT working (not exposed via Matter  )  | Xiaomi(Lumi) Light Sensor|
| Smoke Detectors           | Not supported in HE (yet)   | Aqara Smart Smoke Detector|
| Water Leak Sensors       | NOT working (not exposed via Matter)   | Aqara Water Leak Sensor|
|:--------------------------|:---------------------------------------:|-----------------------:|
| Aqara Dimmers             | Unknown                |   not tested / not implemented          |
| Smart Natural Gas Detector| Unknown                |   not tested / not implemented          |
|:--------------------------|:----------------------:|----------------------------------------:|

---------------------
([back to Matter Advanced Bridge main page](../index.md)
