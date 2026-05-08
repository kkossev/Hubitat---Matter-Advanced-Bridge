/*
 *  'Matter Custom Component Contact Sensor' - component driver for Matter Advanced Bridge
 *
 *  https://community.hubitat.com/t/dynamic-capabilities-commands-and-attributes-for-drivers/98342
 *  https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ver. 1.0.0  2026-04-30 kkossev  - first release; adds sensitivityLevel attribute for devices with cluster 0x0080 (BooleanStateConfiguration), e.g. Aqara P100
 *
*/

import groovy.transform.Field

@Field static final String matterComponentContactSensorVersion = '1.0.0'
@Field static final String matterComponentContactSensorStamp   = '2026/04/30 8:30 PM'

metadata {
    definition(name: 'Matter Custom Component Contact Sensor', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Custom_Component_Contact_Sensor.groovy') {
        capability 'Sensor'
        capability 'ContactSensor'
        capability 'Refresh'

        attribute 'sensitivityLevel',          'number'    // cluster 0x0080 attr 0x0000 (R/W) - current sensitivity level index
        attribute 'supportedSensitivityLevels', 'number'    // cluster 0x0080 attr 0x0001 (R)   - total number of levels
        attribute 'defaultSensitivityLevel',    'number'    // cluster 0x0080 attr 0x0002 (R)   - factory default level index
    }
}

preferences {
    section {
        input name: 'sensitivityLevelPref',
              type: 'number',
              title: 'Sensitivity Level (raw index)',
              description: 'Valid range: 0 to (supportedSensitivityLevels - 1). Check device state for supported range and default. Leave blank to not change.',
              required: false, defaultValue: null
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', required: false, defaultValue: true
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging',           required: false, defaultValue: false
    }
}

void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${device.displayName} ${description}" }
    description.each { d ->
        if (d.name == 'rtt') {
            parseRttEvent(d)
        } else {
            if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
            sendEvent(d)
            if (d.name == 'sensitivityLevel') {
                device.updateSetting('sensitivityLevelPref', [value: d.value as Integer, type: 'number'])
            }
        }
    }
}

void installed() {
    log.info "${device.displayName} driver installed"
}

void updated() {
    log.info "${device.displayName} driver configuration updated"
    if (logEnable) {
        log.debug settings
        runIn(86400, 'logsOff')
    }
    if (settings.sensitivityLevelPref != null) {
        Integer newLevel = settings.sensitivityLevelPref as Integer
        Integer currentLevel = device.currentValue('sensitivityLevel') as Integer
        if (newLevel != currentLevel) {
            log.info "${device.displayName} requesting sensitivity level change: ${currentLevel} -> ${newLevel}"
            parent?.componentSetSensitivityLevel(device, newLevel)
        } else {
            if (logEnable) { log.debug "${device.displayName} sensitivityLevelPref ${newLevel} matches current level, no write needed" }
        }
    }
}

void uninstalled() {
    log.info "${device.displayName} driver uninstalled"
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'])
}

void refresh() {
    parent?.componentRefresh(this.device)
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
