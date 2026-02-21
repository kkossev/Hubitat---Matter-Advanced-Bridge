/*
  *  'Matter Generic Component Battery' - component driver for Matter Advanced Bridge
  *
  *  https://community.hubitat.com/t/dynamic-capabilities-commands-and-attributes-for-drivers/98342
  *  https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009
  *
  *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
  *  in compliance with the License. You may obtain a copy of the License at:
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
  *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
  *  for the specific language governing permissions and limitations under the License.
  *
  * ver. 1.0.0  2024-03-16 kkossev  - first release
  * ver. 1.1.0  2025-01-10 kkossev  - added ping command and RTT monitoring via matterHealthStatusLib and matterCommonLib
*/

import groovy.transform.Field

@Field static final String matterComponentBatteryVersion = '1.1.0'
@Field static final String matterComponentBatteryStamp   = '2025/01/10 9:23 AM'

metadata {
    definition(name: 'Matter Generic Component Battery', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Battery.groovy') {
        capability 'Sensor'
        capability 'Battery'
        capability 'Refresh'
        attribute  'batteryVoltage', 'number'
        attribute  'batStatus', 'string'
        attribute  'batOrder', 'string'
        attribute  'batDescription', 'string'
        attribute  'batTimeRemaining', 'string'
        attribute  'batChargeLevel', 'string'
        attribute  'batReplacementNeeded', 'string'
        attribute  'batReplaceability', 'string'
        attribute  'batReplacementDescription', 'string'
        attribute  'batQuantity', 'string'
    }
}

preferences {
    section {
        input name: 'logEnable',
              type: 'bool',
              title: 'Enable debug logging',
              required: false,
              defaultValue: true

        input name: 'txtEnable',
              type: 'bool',
              title: 'Enable descriptionText logging',
              required: false,
              defaultValue: true
    }
}

/* groovylint-disable-next-line UnusedMethodParameter */
void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${device.displayName} ${description}" }
    description.each { d ->
        if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
        sendEvent(d)
    }
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
}

// Called when the device is removed
void uninstalled() {
    log.info "${device.displayName} driver uninstalled"
}

// Called when the settings are updated
void updated() {
    log.info "${device.displayName} driver configuration updated"
    if (logEnable) {
        log.debug settings
        runIn(86400, 'logsOff')
    }
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}

void refresh() {
    parent?.componentRefresh(this.device)
}


#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
