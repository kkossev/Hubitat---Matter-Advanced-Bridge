<!-- MIGRATED from wiki page `Matter-Advanced-Bridge-‐-Installation` at baseline c4000b7 on 2026-07-27.
     Mechanical import: links and images rewritten, text unchanged.
     NOT YET AUDITED against the current release. -->

# Matter Advanced Bridge - Installation
_(last updated 2024-03-16)_

The 'Matter Advanced Bridge' custom driver for Hubitat Elevation platform is now released.

The recommended installation method is via HPM - Browse by Tags 'Matter' or Search by Keywords 'Matter Advanced Bridge' : 
![image](../assets/images/getting-started-installation-01.png)



**Please use HPM version 1.9.2 or newer!**
The Bundle package is large and the installation on a C-7 hub may take up to 2 minutes, please be patient.

The driver can also be installed manually from GitHub as a Bundle Zip-ed package : 

https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/raw/main/MatterAdvancedBridge.zip 


**Save the ZIP file on your PC** and then use the HE Bindles -> Import function : 

![image](../assets/images/getting-started-installation-02.png)

---------------- 


![image](../assets/images/getting-started-installation-03.png)

-------------------

These are the manual steps that must be followed when bridging a Tuya Matter hub :

**Installation steps**: 

1. Add the Tuya (Zemismart M1) hub to your Smart Life account.
2. Add at least one Tuya Zigbee device via the hub.
3. Pair the Tuya Matter bridge hub to Hubitat C-8 as a standard Matter device.
4. The default 'Device' driver should be assigned automatically. 
If you click on the "Get Info" button of the 'Device' driver, you should see at least one fingerprint shown in HE live logs : 

![image](../assets/images/getting-started-installation-04.png)


5. Manually change the driver to 'Matter Advanced Bridge'
6. Click on the 'Save Preferences' button.

![image](../assets/images/getting-started-installation-05.png)


7. Click on the 'Initialize' button, then hit F5 to refresh the web page - it should look like this : 

![image](../assets/images/getting-started-installation-06.png)


8. Discovery

Discovering the Matter Bridge capabilities and all the bridged devices' capabilities are automated.  All you need to do is click on the "_Discover All" button and wait for the discovery process to finish : 

![image](../assets/images/getting-started-installation-07.png)


The final result of all these steps should be a State Variable named 'Subscriptions' filled in (first press the 'F5' key to refresh the browser) : 
[details="Subscriptions State Variable"]
![image](../assets/images/getting-started-installation-08.png)
[/details]


---------------

Depending on the number of bridged devices, the discovery process may take up to several minutes.


-------------
[next page](../configuration/commands-and-states.md)

([back to Matter Advanced Bridge main page](../index.md)
