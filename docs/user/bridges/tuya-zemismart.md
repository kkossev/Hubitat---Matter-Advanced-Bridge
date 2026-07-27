<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-Tuya-(Zemismart-M1)` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->


### <b>Zemismart M1 Hub as Matter Bridge</b>   
*last updated 2024/02/25*
|Device Type                |         Status               |          Remarks                  |
|:--------------------------|:----------------------------:|----------------------------------:|
| Plugs - On/Off            | working OK - tested          |                                   |
| Relays/Switches - On/Off  | working OK - tested          |                                   |
| Bulbs - On/Off            | working OK - tested          |                                   |
| Bulbs - level control     | working OK - tested          |                                   |
| Bulbs - CT control        | working OK - tested          |                                   |
| Bulbs - RGBW control      | working OK - tested          |                                   |
| Motion Sensors            | working OK - tested          | Not all motion sensors are working! |
| mmWave Presence Sensors   | working OK - tested          | Moes/Linptech                     |
| Contact Sensors           | working OK - tested          | Generic Component Contact Sensor  |
| Water Leak Sensor         | working OK - tested          | as a contact / switch             |
| Tuya Valve on/off         | working OK - tested          | as a contact / switch             |
| Temperature Sensor        | working OK - tested          |                                   |
| Light Sensors             | working OK - tested          |  Tuya Light Sensor                |
| Zemismart Curtain Motor   | working OK - tested          | Zemismart Curtain Motor           |
| Fingerbot                 | working OK - tested          | as contact / switch               |
|:--------------------------|:----------------------------:|----------------------------------:|
| Wireless Remote Switch    | TODO  (not implemented yet)  | Tuya TS0044 Scene Switch - CRASHES! |
| Wireless button           | TODO  (not implemented yet)  | Tuya button          - CRASHES! |
| Battery Level reporting   | TODO  (not implemented yet)  | check !                           |
|:--------------------------|:----------------------------:|----------------------------------:|
| Tuya Gas Detector         | Unknown                |   not tested / not implemented          |
| Tuya Smoke Detector       | Unknown                |   not tested / not implemented          |
| Tuya Vibration Sensor     | Unknown                |   not tested                            |
| TRVs                      | Unknown                |   not tested                            |
| Light Door Lock           | Unknown                |   not tested                            |
|:--------------------------|:----------------------------:|----------------------------------:|
| Humidity Sensor           | NOT working (fixed 40%)      |  Tuya bridge bug?                 |
| Tuya Dimmers              | NOT working (not exposed via Matter) | Girier TS0110F, Tuya TS0601 |
| Thermostats               | NOT working (not exposed via Matter) |   AVATTO thermostat       |
|:--------------------------|:----------------------------:|----------------------------------:|
* mmWave sensors that are NOT shared via the Matter Gateway: 
* the big black radar w/ annoying Led;
* TS0601 _TZE200_ikvncluo 
* TS0601 _TZE204_kapvnnlk

---------------------

([back to Matter Advanced Bridge main page](../index.md)
