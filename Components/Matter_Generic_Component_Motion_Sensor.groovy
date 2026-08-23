    /*
 *  'Matter Generic Component Motion Sensor' - component driver for Matter Advanced Bridge
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
 * ver. 1.1.0  2025-01-10 kkossev  - added ping command and RTT monitoring via matterHealthStatusLib
 * ver. 1.1.1  2025-01-29 kkossev  - common libraries
 * ver. 1.1.2  2026-08-16 kkossev  - bug fixes
 * ver. 1.1.3  2026-08-17 kkossev  - fixed the 'No signature of method: parse()' error logs
 *
*/

import groovy.transform.Field

@Field static final String matterComponentMotionVersion = '1.1.3'
@Field static final String matterComponentMotionStamp   = '2026/08/22 9:34 PM'

metadata {
    definition(name: 'Matter Generic Component Motion Sensor', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Motion_Sensor.groovy') {
        capability 'Sensor'
        capability 'MotionSensor'
        capability 'Refresh'

        command 'setMotion', [[name: 'setMotion', type: 'ENUM', constraints: ['No selection', 'active', 'inactive'], description: 'Force motion active/inactive (for tests)']]
    }
}

preferences {
    section {
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', required: false, defaultValue: true
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging', required: false, defaultValue: false
        input name: 'invertMotion', type: 'bool', title: 'Invert Motion', description: '<i>Some motion sensors (mmWave radars) report active when no motion detected. Enable this option to invert the motion state.</i>', required: false, defaultValue: false
    }
}


// Hubitat platform 2.5.1.132+ transaction callbacks. The parent passes these Maps unchanged.
// Every custom component driver must implement this - without it the parent's dw.parse(descMap)
// throws MissingMethodException, which the platform logs as an error in THIS device's log.
void parse(Map descMap) {
    switch (descMap?.callbackType) {
        case 'Invoke':
            handleInvokeResponse(descMap)
            break
        default:
            logDebug "parse(Map): ignored callback: ${descMap}"
            break
    }
}

private void handleInvokeResponse(final Map descMap) {
    Integer invokeStatus = safeNumberToInt(descMap.status, null)
    Integer commandInt = safeNumberToInt(descMap.commandInt, null)

    if (invokeStatus == 0) {
        logDebug "Matter command completed: endpoint=${descMap.endpointInt} cluster=${descMap.clusterInt} command=${commandInt}"
    }
    else {
        logWarn "Matter command failed: status=${invokeStatus} endpoint=${descMap.endpointInt} cluster=${descMap.clusterInt} command=${commandInt}"
    }
}

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${device.displayName} ${description}" }
    description.each { d ->
        if (d.name == 'motion') {
            if (invertMotion) {
                if (d.value == 'active') {
                    d.value = 'inactive'
                    d.descriptionText = d.descriptionText.replace('active', 'inactive')
                }
                else {
                    d.value = 'active'
                    d.descriptionText = d.descriptionText.replace('inactive', 'active')
                }
            }
        }
        if (d.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
        }
        else {
            if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
            sendEvent(d)
        }
    }
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
}

// for tests
void setMotion( String mode ) {
    switch (mode) {
        case 'active' :
            sendEvent([name:'motion', value:'active', type: 'digital', descriptionText: 'motion set to active', isStateChange:true])
            if (settings?.txtEnable) { log.info "${device.displayName} motion set to active" }
            break
        case 'inactive' :
            sendEvent([name:'motion', value:'inactive', type: 'digital', descriptionText: 'motion set to inactive', isStateChange:true])
            if (settings?.txtEnable) { log.info "${device.displayName} motion set to inactive" }
            break
        default :
            if (settings?.logEnable) { log.warn "${device.displayName} please select motion action" }
            break
    }
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
    if ((state.invertMotion ?: false) != settings?.invertMotion) {
        state.invertMotion = settings?.invertMotion
        if (logEnable) { log.debug "${device.displayName} invertMotion: ${settings?.invertMotion}" }
        String currentMotion = device.currentValue('motion')
        String motion = currentMotion == 'active' ? 'inactive' : (currentMotion == 'inactive' ? 'active' : null)
        if (motion != null) {
            sendEvent([name:'motion', value:motion, type: 'digital', descriptionText: "motion state inverted to ${motion}", isStateChange:true])
        }
        else {
            if (logEnable) { log.debug "${device.displayName} no motion state reported yet - waiting for the next report" }
        }
    }
    else {
        if (logEnable) { log.debug "${device.displayName} invertMotion: no change" }
    }
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}

void refresh() {
    parent?.componentRefresh(device)
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
