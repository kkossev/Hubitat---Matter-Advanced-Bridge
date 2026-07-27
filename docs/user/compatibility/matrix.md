<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-hubs-and-devices-compatibility-matrix` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->

*last updated 2024/03/13*

|Matter Clusters| Matter Device Type <br>ID                    |Tuya|Aqara|Philips Hue|Switch Bot|HE Child Device Driver | Remarks |
|:----------:|:--------------------:|:-----:|:-----:|:-----:|:-----:|:--------------:|:--------------:|
|    --        | 4.   <b>Lighting Device Types</b>             |       |       |       |     |                |         |
|    06        | 4.1. On/Off Light<br>(ID: 0x0100)             |  Yes  |  Yes  |  Yes  |  ?  |'Matter Generic Component Switch' |       |
|  06, 08      | 4.2. Dimmable Light <br>(ID: 0x0101)          |  Yes  |  Yes  |  Yes  |  ?  |'Generic Component Dimmer'        |       |
| 06, 08, 0300 | 4.3. Color Temperature Light <br>(ID: 0x010C) |  Yes  |  Yes  |  Yes  |  ?  |'Generic Component CT'        |      |
| 06, 08, 0300 | 4.4. Extended Color Light <br>(ID: 0x010D)    |  Yes  |  Yes  |  Yes  |  ?  |'Generic Component RGBW'       |      |
|    --        | 5. <b>Smart Plugs/Outlets/Actuators</b>       |       |       |       |     |      ---                  |                  |
|    06        | 5.1 On/Off Plug-in Unit <br>(ID: 0x010A)      |  Yes  |  Yes  |  Yes  |  ?  |'Matter Generic Component Switch'  |      |
|  06, 08      | 5.2 Dimmable Plug-in Unit <br>(ID: 0x010B)    |   ?   |   ?   |   ?   |  ?  |'Generic Component Dimmer'        |       |
|    --        | 6. <b>Switches and Control Device Types</b>   |       |       |       |     |      ---         |                  |
|    06        | 6.1  On/Off Light Switch <br>(ID: 0x0103)     |  Yes  |  Yes  |  Yes  |  ?  |'Matter Generic Component Switch'  | Obsolete? |
|  06, 08      | 6.2      Dimmer Switch <br>(ID: 0x0104)       |   ?   |   ?   |   ?   |  ?  |'Generic Component Dimmer'  | Tuya dimmers are NOT exporeted! |
| 06, 08, 0300 | 6.3   Color Dimmer Switch <br>(ID: 0x0105)    |   ?   |   ?   |   ?   |  ?  |'Generic Component RGBW'               |                          |
|06, 08, 0300,<br>0400,0406| 6.4     Control Bridge <br>(ID: 0x0840)       |   ?   |   ?   |   ?   |  ?  |                      |                          |
|    3B        | 6.6    Generic Switch <br>(ID: 0x000F)        |  TODO |  TODO |   ?   |  ?  | TODO         | Clarify the Tuya/Aqara devices that use cluster 0x3B|  
|    --        | 7.     <b>Sensor Device Types</b>             |       |       |       |     |      --         |                          |
|    45        | 7.1.   Contact Sensor <br>(ID: 0x0015)        |  Yes  |  Yes  |   ?   |  ?  |'Generic Component Contact Sensor'| Confirm Aqara Contact sensors are working!|
|    45        | n/a  Water Leak Sensors<br>(ID:n/a)           |  Yes  |  No   |   ?   |  ?  |'Generic Component Contact Sensor'| as a Contact Sensor! | 
|   0400       | 7.2    Light Sensor <br>(ID: 0x0106)          |  Yes  | Yes   |   ?   |  ?  | Generic Component Omni Sensor'|Aqara T1 Light Sensor |
|   0406       | 7.3    Occupancy Sensor <br>(ID: 0x0107)      |  Yes  |  Yes  |   ?   |  ?  |'Matter Generic Component Motion Sensor'|     |
|   0402       | 7.4    Temperature Sensor <br>(ID: 0x0302)    |  Yes  |  Yes  |  Yes  | Yes |'Generic Component Omni Sensor'|      |
|   0403       | 7.5      Pressure Sensor<br>(ID: 0x0305 )     |  No |  No |   ?   |  ?  |               |                  |
|   0405       | 7.7       Humidity Sensor <br>(ID: 0x0307)    |  No  |  Yes  |   ?   | Yes |'Generic Component Omni Sensor'|       |
|06, 08, 0300, <br>0400, 0406| 7.8        On/Off Sensor <br>(ID: 0x0850) |   ?   |   ?   |   ?   |  ?  |               | not clear what is this  |
|5C, 0405, 0402, <br>040C      | 7.9      Smoke CO Alarm <br>(ID: 0x0076)|   No   |   No  |   ?   |  ?  |    not available        |   No support in this driver   |
|    --        | 8       <b>Closure Device Types</b>           |       |       |       |     |   --           |           |
|   0101       | 8.1        Door Lock <br>(ID: 0x000A)         |  No   | Yes*  |   ?   |  ?  |'Generic Component Lock'|        |
|   0102       | 8.3      Window Covering <br>(ID: 0x0202)     |  Yes  |   ?   |  --   | Yes |'Matter Generic Component Window Shade'|      |
|    --        |  9      <b>HVAC Device Types</b>              |       |       |       |     |    --           |              |
|   0201       | 9.2        Thermostat <br>(ID: 0x0301)        |  No?  |  Yes  |  No   |  ?  |'Generic Component Thermostat'|       |
|   0202       | 9.3           Fan <br>(ID: 0x002B)            |  No   |  No   |  No   |  ?  |   not available        |   No support in this driver   |
|    2C        | 9.4       Air Purifier  <br>(ID: 0x002D)      |  No   |  No   |  No   |  ?  |   not available        |   No support in this driver   |
|    5B        | 9.5     Air Quality Sensor <br>(ID: 0x002C)   |  No   |  No   |  No   |  ?  |   not available        |   No support in this driver   |
|    n/a       |        <b>Other Devices<b>                    |       |       |       |     |   --          |      |
|    2F        | --     Battry Reporting                       |  TODO   |  TODO   |  TODO   |  ?  |'Matter Generic Component Battery'| TODO - check what the problem is - HIGH priority  |
|    n/a       |        <b>Other Functions<b>                  |       |       |       |     |    --           |      |
|    ---       |         Composite devices                     |   NO  |   NO  |   NO  |  NO  | No support in this driver | TODO - low priority    |
|    ---       |         Groups                                |   NO  |   NO  |   NO  |  NO  | not exported by Matter Bridges?|      |
|    ---       |         Scenes                                |   NO  |   NO  |   NO  |  NO  | not exported by Matter Bridges?|      |



Table Legend:

* "Yes" means supported by both the Bridge and the driver, and tested and confirmed to be working.

* "NO"  means not supported by the particular brand Bridge of by the driver.

* "No?" means the tested device did not show up as bridged by the particular brand Bridge, but there is a chance other devices may be expored.

* "--" or "n/a" means not applicable for the particular brand bridge.

* "?"   means unknown - not tested.


--------------------------------
([back to Matter Advanced Bridge main page](../index.md)
