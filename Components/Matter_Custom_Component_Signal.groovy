/* groovylint-disable CompileStatic, DuplicateStringLiteral, LineLength, PublicMethodsBeforeNonPublicMethods */
/*
 *  'Matter Custom Component Signal' - component driver for Matter Advanced Bridge
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
 * ver. 1.0.0  2025-05-24 kkosev     - first version
 * ver. 1.1.0  2025-01-10 kkossev    - added ping command and RTT monitoring via matterHealthStatusLib; removed unused setState/getState methods
 * ver. 1.1.1  2025-01-29 kkossev    - common libraries
 *
*/

import groovy.transform.Field

@Field static final String matterComponentSignalVersion = '1.1.1'
@Field static final String matterComponentSignalStamp   = '2025/01/29 10:40 PM'

metadata {
    definition(name: 'Matter Custom Component Signal', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Custom_Component_Signal.groovy') {
        capability 'Refresh'
        capability 'PushableButton'
        capability 'MotionSensor'
        capability 'Sensor'
        attribute 'currentPosition', 'string'
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

void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${description}" }
    description.each { d ->
        if (d.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
        }
        else if (d.name == 'motion') {
            sendEvent(d)
            if (d.value == 'active') {
                sendEvent(name: 'pushed', value: 1, descriptionText: 'button was pushed', isStateChange: true, type: 'physical')
                if (txtEnable) { log.info "${device.displayName} button was pushed" }
            }
            else {
                if (logEnable) { log.debug "${device.displayName} : ignored event '${d.value}'" }
            }
        }
        else {
            if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
            sendEvent(d)
        }
    }
}


void push(Number bn = 1) {
    sendEvent(name: 'pushed', value: bn, descriptionText: 'button was pushed', isStateChange: true, type: 'digital')
    if (txtEnable) { log.info "${device.displayName} button ${bn} was pushed" }
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
    sendEvent(name: 'numberOfButtons', value: 1)
    if (logEnable) {
        log.debug settings
        runIn(86400, 'logsOff')
    }
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName} "
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}

void refresh() {
    parent?.componentRefresh(this.device)
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
