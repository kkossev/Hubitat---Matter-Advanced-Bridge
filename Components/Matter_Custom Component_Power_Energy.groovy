
/*
 *  'Matter Custom Component Power Energy' - component driver for Matter Advanced Bridge
 *
 *  https://community.hubitat.com/t/dynamic-capabilities-commands-and-attributes-for-drivers/98342
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
 * ver. 1.0.0  2025-04-06 kkossev  - first release
 * ver. 1.1.0  2025-01-10 kkossev  - added ping command and RTT monitoring via matterHealthStatusLib
 * ver. 1.1.1  2025-01-29 kkossev  - common libraries
 *
*/

import groovy.transform.Field

@Field static final String matterComponentPowerEnergyVersion = '1.1.1'
@Field static final String matterComponentPowerEnergyStamp   = '2025/01/29 10:41 PM'

metadata {
    definition(name: 'Matter Custom Component Power Energy', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Generic_Component_Energy.groovy') {
        capability 'Actuator'
        capability 'Switch'             // Commands:[off, on, refresh]
        capability 'PowerMeter'
        capability 'EnergyMeter'
        capability 'VoltageMeasurement'
        capability 'CurrentMeter'
        capability 'Refresh'

        attribute 'energyExported', 'number'
        attribute 'frequency', 'number'
        attribute 'powerFactor', 'number'
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
    //if (logEnable) { log.debug "${description}" }
    description.each { d ->
        if (d.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
        }
        else if (d.name == 'switch') {
            if (device.currentValue('switch') != d.value) {
                if (d.descriptionText && txtEnable) { log.info "${d.descriptionText} (value changed)" }
                sendEvent(d)
            }
            else {
                if (logEnable) { log.debug "${device.displayName} : ignored switch event '${d.value}' (no change)" }
            }
        }
        else if (d.name == 'unprocessed') {
            processUnprocessed(d)
        }
        else {
            if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
            sendEvent(d)
        }
    }
}

// Component command to turn device on
void on() {
    if (logEnable) { log.debug "${device.displayName} turning on ..." }
    parent?.componentOn(device)
}

// Component command to turn device off
void off() {
    if (logEnable) { log.debug "${device.displayName} turning off ..." }
    parent?.componentOff(device)
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
    log.warn "debug logging disabled for ${device.displayName} "
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void setState(String stateName, String stateValue) {
    if (logEnable) { log.debug "${device.displayName} setting state '${stateName}' to '${stateValue}'" }
    state[stateName] = stateValue
}

String getState(String stateName) {
    if (logEnable) { log.debug "${device.displayName} getting state '${stateName}'" }
    return state[stateName]
}

void processUnprocessed(Map description) {
    if (logEnable) { log.debug "${device.displayName} processing unprocessed: ${description}" }

    String inputString = description.value
    //if (logEnable) { log.debug "${device.displayName} inputString: ${inputString}" }
    inputString = inputString.replaceAll('\\[', '').replaceAll('\\]', '')
    String[] keyValuePairs = inputString.split(', ')
    Map<String, String> resultMap = [:]
    keyValuePairs.each { pair ->
        String[] parts = pair.split(':')
        resultMap[parts[0].trim()] = parts[1].trim()
    }
    
    Map descMap = resultMap

    if (logEnable) { log.debug "${device.displayName} descMap: ${descMap}" }
    //
    //if (descMap.cluster != '0101') { logWarn "processUnprocessed: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    String eventValue = descMap.value
    String descriptionText = "${device.displayName} ${descMap.cluster}:${descMap.attrId} value:${eventValue}"
    switch (descMap.cluster + '_' + descMap.attrId) {
        // 0090_FFFB clusterAttrList [0, 1, 2, 8-ActivePower, 11-RMSVoltage, 12-RMSCurrent, 14-Frequency, 17-PowerFactor,
        case '0090_0008': // attribute 'ActivePower'
            eventValue= Integer.parseInt(descMap.value, 16) / 1000
            descriptionText = "${device.displayName} ActivePower is ${eventValue} W (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'power', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000B': // attribute 'RMSVoltage'
            eventValue= Integer.parseInt(descMap.value, 16) / 1000
            descriptionText = "${device.displayName} RMSVoltage is ${eventValue} V (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'voltage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000C': // attribute 'RMSCurrent'
            eventValue= Integer.parseInt(descMap.value, 16) / 1000
            descriptionText = "${device.displayName} RMSCurrent is ${eventValue} A (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'amperage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000E': // attribute 'Frequency'
            eventValue= Integer.parseInt(descMap.value, 16) / 1000
            descriptionText = "${device.displayName} Frequency is ${eventValue} Hz (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'frequency', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_0011': // attribute 'PowerFactor'
            eventValue= Integer.parseInt(descMap.value, 16) / 100
            descriptionText = "${device.displayName} PowerFactor is ${eventValue} (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'powerFactor', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0091_0001': // attribute 'CumulativeEnergyImported'
            // PATCH !! 152100CF0E18  (not working!.... )
            descMap.value = descMap.value.substring(4, descMap.value.length() - 2)
            try { eventValue = Integer.parseInt(descMap.value, 16) / 1000 } catch (e) {eventValue = descMap.value; if (logEnable) { log.debug "Exception processing CumulativeEnergyImported descMap.value ${descMap.value}"} }
            descriptionText = "${device.displayName} energy is ${eventValue} Wh (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'energy', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0091_0002': // attribute 'CumulativeEnergyExported'
            // PATCH !! 1520000018
            descMap.value = descMap.value.substring(4, descMap.value.length() - 2)
            try { eventValue = Integer.parseInt(descMap.value, 16) / 1000 } catch (e) {eventValue = descMap.value; if (logEnable) { log.debug "Exception processing CumulativeEnergyExported descMap.value ${descMap.value}"} }
            descriptionText = "${device.displayName} CumulativeEnergyExported is ${eventValue} Wh (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'energyExported', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        default:
            if (logEnable) { log.warn "processUnprocessed: unexpected attrId:${descMap.attrId}" }
    }
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
