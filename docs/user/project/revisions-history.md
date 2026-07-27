<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-revisions-history` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->

([back to Matter Advanced Bridge main page](../index.md))


### Revisions history : 

 * ver. 0.0.0  2023-12-29 kkossev  - Inital version;
 * ver. 0.0.1  2024-01-05 kkossev  - Linter; Discovery OK; published for alpha- testing.
 * ver. 0.0.2  2024-01-07 kkossev  - Refresh() reads the subscribed attributes; added command 'Device Label'; VendorName, ProductName, Reachable for child devices; show the device label in the event logs if set;   added a test command 'setSwitch' on/off/toggle + device#;
* ver. 0.0.3  2024-01-11 kkossev  
  * added Child devices 
  * added deviceCount, 
  * added 'Matter_Generic_Component_Motion_Sensor.groovy', 
  * added 'Matter_Generic_Component_Window_Shade.groovy'
  * added matterLib.groovy'
  * Hubitat Bundle package;
  * A1 Bridge Discovery now uses the short version; 
  * added logTrace() and logError() methods; 
  * setSwitch and setLabel commands are visible in _DEBUG mode only
 * ver. 0.0.4  2024-01-14 kkossev 
   * added 'Matter Generic Component Switch' component driver;
   * cluster 0x0102 (WindowCovering) attributes decoding - position, targetPosition, windowShade; 
   * added cluster 0x0102 commands processing;
   * logTrace is  switched off after 30 minutes;
   * filtered duplicated On/Off events in the Switch component driver;
   * disabled devices are not processed to avoid spamming the debug logs; 
   * added initializeCtr attribute;
   * Default Bridge healthCheck method is set to periodic polling every 1 hour; 
   * added new removeAllSubscriptions() command; 
   * added 'Invert Motion' option to the Motion Sensor component driver @iEnam
* ver. 0.0.5  2024-01-20 : 
  *  added endpointsCount; 
  * subscribe to [endpoint:00, cluster:001D, attrId:0003 - PartsList = the number of the parts list entries]; 
  * refactoring: parseGlobalElements(); 
  * discovery process bugs fixes; 
  * debug is false by default; 
  * changed the steps sequence (first create devices, last subscribe to the attributes); 
  * temperature sensor omni component bug fix;
* ver. 0.0.6  2024-01-27 
  * DiscoverAll() button (state machine) replaces all old manual discovery buttons; 
  * removed setLabel and setSwitch test command;
* ver. 0.0.7  2024-01-28 
  * code cleanup;
  *  bug fix -do not send events to the bridge device if the child device does not exist; 
  * discoverAll debug options bug fix; 
  * deviceCount, endpointsCount, nodeLabelbug fixes;
  * refresh() button on the Bridge device fixed; 
  * multiple subscribe entries bug fix;
  * Bulbs are assigned 'Generic Component Dimmer'; 
  * cluster 08 partial processing - componentSetLevel() implementation; 
  * added subscribing to more than one attribute per endpoint; 
  * Celsius to Fahrenheit conversion for temperature sensors
 * ver. 0.1.0  2024-02-03
   * versions are renamed to start from major ver. 0.x.x;
   * added Contact Sensor processing (works with Zemismart only, doesn't work with Aqara?); 
   * added Thermostat cluster 0x0201 (attributes decoding only);
   *  added Generic Component Battery
   * vibration sensors are processed as motion sensors; 
   *  nodeLabel null checks; 
   * rounded the humidity to the nearest integer value; 
   *  added a compatibility table matrix for Aqara devices on the top post;
* ver : 0.1.1 2023/02/03 12:44 PM  :
   *  PowerSource cluster processing was temporarily removed.
 * ver. 0.2.0  2024-02-04  (dev.branch) : 
   *  refactored the matter messages parsing method using a lookup map;
   * bugfix: duplicated attrList entries; 
   * bugfix: deviceCount and initializeCtr not updated; 
   * bugfix : healhCheck schedued job was lost on resubscribe() 
   * added cluster 0x0101 DoorLock decoding; lock and unlock commands (not tested!)
* ver. 0.2.1  2024-02-07  (dev.branch)
  *  added temperature and humidity valid values checking; 
  * change: When creating new child devices, the Device Name is set using a pattern 'Bridge #4407 Device#08 (Humidity Sensor)' as an example, Device Label is left empty;
  *  bugfix: device labels in logs @fanmanrules;
  *  implemented componentStartLevelChange(), componentStopLevelChange(), componentSetColorTemperature; 
  * use 'Generic Component CT' driver instead of dimmer for bulbs; 
  * added colorTemperature and colorName for CT bulbs;
  * @CompileStatic experiments...
* ver. 0.2.2  2024-02-10 
  * bugfix: null pointers checks exceptions; 
  * increased the discovery timeouts (the number of the retries); 
  * all states are cleared at the start of teh discovery process;  
  * bugfix: CT/RGB bulbs reinitialization;
* ver. 0.2.3  2024-02-11 
  * lock/unlock commands disabled (not working for now); 
  * RGBW bulbs: hue, saturation; setColor, colorMode in CT mode;  
  * healthStatus offline when polling was not working;
* ver. 0.2.4  2024-02-11  
  * bugfix: setLevel duration and setColorTemperature level parameters were not working; 
  * ignored duplicated events on main driver level;
 * ver. 0.2.5  2024-02-12 
   * bugfix: exception processing while checking for duplicate events.
 * ver. 0.3.0  2024-02-13 
   * added reading of all supported clusters 0xFFFB attribute during DeviceDiscovery for each child device; 
   * subscribing to 0x0300 attributes; 
   * colorMode and colorName fixes; 
   * setColor turns the bulb on; 
   * RGBW bulbs is be assigned 'Generic Component RGBW' driver;
 * ver. 0.4.0  2024-02-18 - (dev.branch) : 
   * a major refactoring of the attributes subscription process;
   * refactored the refresh() command for all child devices to use the same subscription list;
   * added ERROR info messages during the discovery process;
   *  increased timeouts; 
   * added a compatibility matrix table for Tuya-Aqara-Hue-SwitchBot Matter bridges on the top post; 
   * created a MVP list and published it on the top post;
   * The bundle is made available on HPM; 
* ver. 0.4.1  2024-02-20 
  *  added illuminance cluster support (Aqara T1 Light Sensor); 
  * bugfix: colorName was sent wrongly in the event description for CT bulbs;
    * note: PhilipsHue does not report colorMode back when changed from another system!; 
    * note: Aqara LED Strip T1 colorMode reporting is wrong!
  * the FeatureMap of each supported cluster is stored in the state figngerprint variable;
* ver. 0.4.2  2024-02-25 
  *  fixed the illuminance lux reading conversion;  
  * invertMotion changes the motion state immediately; 
  * added a list of known issues and limitations on the top post - for both HE system and the driver;
* ver. 0.4.3  2024-02-26 
  *  added utilities() command; 
  * loose checks for the OnOff commands; 
  * states cleanup (remove fingerprintXX, leave Subscriptions) when minimizeStateVariables advanced option is enabled;
 * ver. 0.4.4  2024-03-02 kkossev  - (dev.branch) 
  * added refresh() for component devices;
  *  global refresh() from the parent device registers events for all child devices!;
  *  added clearStats command; 
  * SwitchBot/Zemismart WindowCovering - bug fixes @Steve9123456789
* ver. 0.4.5  2024-03-03 kkossev  - (dev.branch) 
  * WindowCovering refresh() bug patch; 
  * commented out the WindowCovering ping() command (capability 'Health Check' - not supported yet); 
  * enabled Battery / PowerSource cluster (0x002F) processing!
* ver. 0.4.6  2024-03-04 kkossev  - (dev.branch) 
  * WindowCovering unit fix; hopefully also WindowCovering close() fix; 
  * added and verified the importUrl for all libraries and component drivers;
* ver. 0.5.0  2024-03-09 kkossev  - (dev.branch) 
  * WindowCovering driver refactoring; 
  * WindowCovering: added battery attributes; 
  * WindowCovering: added a bunch of new options; 
  * Minimize State Variables by default is true;
  * Documented the WindowCovering settings - ../drivers/window-shade.md
* ver. 0.5.1  2024-03-10 
  * Help/Documentation button in the driver linked to GitHub Wiki page and HE Community thread;
* ver. 0.5.2  2024-03-11  (dev.branch) : 
  * fixed an exception in the Window Shade driver; 
  * added parseTest(map as string) _DEBUg command in the 'Matter Generic Component Window Shade' driver; 
  * battery attributes name changes;
  * removed the _DiscoverAll options;
* ver. 0.5.4  2024-03-12 kkossev  - (dev.branch) TODO list cleanup
* ver. 0.6.0  2024-03013 kkossev - moved to Github new repository https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/tree/main 


-----------

([back to Matter Advanced Bridge main page](../index.md))
