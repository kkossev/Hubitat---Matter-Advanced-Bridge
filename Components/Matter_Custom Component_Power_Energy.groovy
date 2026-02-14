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
 * ver. 1.1.2  2026-02-14 kkossev - (dev. branch) getInfo(); bugfix: Power/Energy processing exceptions;
*
*/

import groovy.transform.Field

@Field static final String matterComponentPowerEnergyVersion = '1.1.2'
@Field static final String matterComponentPowerEnergyStamp   = '2026/02/14 10:26 AM'

metadata {
    definition(name: 'Matter Custom Component Power Energy', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Generic_Component_Energy.groovy') {
        capability 'Actuator'
        capability 'Switch'             // Commands:[off, on, refresh]
        capability 'PowerMeter'
        capability 'EnergyMeter'
        capability 'VoltageMeasurement'
        capability 'CurrentMeter'
        capability 'Refresh'

        command   'getInfo', [[name: 'Check the live logs and the device data for additional infoormation on this device']]

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
        else if (d.name in  ['unprocessed', 'handleInChildDriver'])  {
            handleUnprocessedMessageInChildDriver(d)
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


void handleUnprocessedMessageInChildDriver(Map description) {
    logDebug "handleUnprocessedMessageInChildDriver: description = ${description}"
    Map descMap =[:]
    try {
        descMap = description.value as Map
    }
    catch (e) {
        logWarn "handleUnprocessedMessageInChildDriver: exception ${e} while parsing description.value = ${description.value}"
        return
    }
    logDebug "handleUnprocessedMessageInChildDriver: parsed descMap = ${descMap}"

    // Check if this is an event (has evtId) or an attribute (has attrId)
    if (descMap.evtId != null) {
        //processWindowCoveringEvent(descMap)
        logDebug "handleUnprocessedMessageInChildDriver: TODO: received event report - evtId: ${descMap.evtId}, value: ${descMap.value}"
        return
    }
    // else - process attribute report
    processPowerEnergyAttributeReport(descMap)
}



void processPowerEnergyAttributeReport(Map descMap) {
    if (logEnable) { log.debug "${device.displayName} processPowerEnergyAttributeReport: ${descMap}" }

    String eventValue = descMap.value
    String descriptionText = "${device.displayName} ${descMap.cluster}:${descMap.attrId} value:${eventValue}"
    
    // Declare variables for conditional logging
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${descMap.cluster}_${descMap.attrId}] " : ""
    String message = null
    boolean useDebugLog = false  // Flag to use logDebug instead of logInfo when isInfoMode is false


    switch (descMap.cluster + '_' + descMap.attrId) {
            // 0090_FFFB clusterAttrList [0, 1, 2, 8-ActivePower, 11-RMSVoltage, 12-RMSCurrent, 14-Frequency, 17-PowerFactor,
        case '0090_0000': // attribute 'PowerMode'
            // [callbackType:Report, endpointInt:2, clusterInt:144, attrInt:0, data:[0:UINT:2], value:2, cluster:0090, endpoint:02, attrId:0000]
            Integer powerMode = safeHexToInt(descMap.value)
            String powerModeText = decodePowerMode(powerMode)
            message = "${prefix}PowerMode: ${powerModeText} (0x${descMap.value})"
            break
        case '0090_0001': // attribute 'NumberOfMeasurementTypes'
            // :[callbackType:Report, endpointInt:2, clusterInt:144, attrInt:1, data:[1:UINT:3], value:3, cluster:0090, endpoint:02, attrId:0001]
            Integer numberOfMeasurementTypes = safeHexToInt(descMap.value)
            descriptionText = "${device.displayName} NumberOfMeasurementTypes is ${numberOfMeasurementTypes} (raw:0x${descMap.value})"
            message = "${prefix}NumberOfMeasurementTypes: ${numberOfMeasurementTypes}"
            break
        case '0090_0002': // attribute 'Accuracy'
            // Array of MeasurementAccuracyStruct
            List<Map> accuracyList = parseMeasurementAccuracyArray(descMap.data)
            if (accuracyList && !accuracyList.isEmpty()) {
                message = "${prefix}Accuracy measurements (${accuracyList.size()} types)"
                useDebugLog = true
                // Log each measurement type separately for better readability
                accuracyList.each { acc ->
                    String typeText = decodeMeasurementType(acc.measurementType ?: 0)
                    String rangeText = formatMeasurementRange(acc, typeText)
                    String accuracyText = formatAccuracyRanges(acc.accuracyRanges)
                    String detailMsg = "${prefix}  - ${typeText}: ${rangeText}${accuracyText}"
                    if (isInfoMode) {
                        logInfo detailMsg
                    } else {
                        logDebug detailMsg
                    }
                }
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse Accuracy array for 0090_0002: ${descMap.data}"
            }
            break
        case '0090_0003': // attribute 'Ranges' (optional)
            // MeasurementRangeStruct array
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_0004': // attribute 'Voltage'
            // :[callbackType:Report, endpointInt:2, clusterInt:144, attrInt:4, data:[4:INT:0], value:0, cluster:0090, endpoint:02, attrId:0004]
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} Voltage is ${eventValue} V (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'voltage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_0005': // attribute 'ActiveCurrent'
            // [callbackType:Report, endpointInt:2, clusterInt:144, attrInt:5, data:[5:INT:0], value:0, cluster:0090, endpoint:02, attrId:0005
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} ActiveCurrent is ${eventValue} A (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'amperage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_0006': // attribute 'ReactiveCurrent' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_0008': // attribute 'ActivePower'
            // descMap:[callbackType:Report, endpointInt:2, clusterInt:144, attrInt:8, data:[8:INT:0], value:0, cluster:0090, endpoint:02, attrId:0008]
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} ActivePower is ${eventValue} W (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'power', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_0009': // attribute 'ReactivePower' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_000A': // attribute 'ApparentPower' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_000B': // attribute 'RMSVoltage'
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} RMSVoltage is ${eventValue} V (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'voltage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000C': // attribute 'RMSCurrent'
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} RMSCurrent is ${eventValue} A (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'amperage', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000D': // attribute 'RMSPower' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_000E': // attribute 'Frequency'
            eventValue= (safeHexToInt(descMap.value) / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} Frequency is ${eventValue} Hz (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'frequency', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_000F': // attribute 'HarmonicCurrents' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_0010': // attribute 'HarmonicPhases' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break
        case '0090_0011': // attribute 'PowerFactor'
            eventValue= (safeHexToInt(descMap.value) / 10000).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
            descriptionText = "${device.displayName} PowerFactor is ${eventValue} (raw:attrId:${descMap.attrId}:${descMap.value})"
            sendEvent(name: 'powerFactor', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '0090_0012': // attribute 'NeutralCurrent' (optional)
            //
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break


        case '0091_0000': // attribute 'Accuracy'
            // MeasurementAccuracyStruct: [measurementType, measured, minMeasuredValue, maxMeasuredValue, accuracyRanges[]]
            Map accuracyData = parseMeasurementAccuracyStruct(descMap.data)
            if (accuracyData.measurementType != null) {
                String measurementTypeText = decodeMeasurementType(accuracyData.measurementType)
                descriptionText = "${device.displayName} Accuracy: ${measurementTypeText}"
                if (accuracyData.measured != null) {
                    descriptionText += ", measured=${accuracyData.measured}"
                }
                if (accuracyData.minMeasuredValue != null && accuracyData.maxMeasuredValue != null) {
                    descriptionText += ", range=[${accuracyData.minMeasuredValue} to ${accuracyData.maxMeasuredValue}]"
                }
                message = "${prefix}Accuracy: ${measurementTypeText} (measured=${accuracyData.measured})"
                useDebugLog = true
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse MeasurementAccuracyStruct for 0091_0000: ${descMap.data}"
            }
            break
        case '0091_0001': // attribute 'CumulativeEnergyImported'
            // EnergyMeasurementStruct: [energy (mWh), startTimestamp, endTimestamp, ...]
            Map energyData = parseEnergyMeasurementStruct(descMap.data)
            if (energyData.energy != null) {
                BigDecimal energyWh = (energyData.energy / 1000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                BigDecimal energyKWh = (energyData.energy / 1000000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                descriptionText = "${device.displayName} CumulativeEnergyImported is ${energyKWh} kWh"
                sendEvent(name: 'energy', value: energyKWh, unit: 'kWh', descriptionText: descriptionText)
                if (txtEnable) { log.info "${descriptionText}" }
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse EnergyMeasurementStruct for 0091_0001: ${descMap.data}"
            }
            break
        case '0091_0002': // attribute 'CumulativeEnergyExported'
            // EnergyMeasurementStruct: [energy (mWh), startTimestamp, endTimestamp, ...]
            Map energyData = parseEnergyMeasurementStruct(descMap.data)
            if (energyData.energy != null) {
                BigDecimal energyWh = (energyData.energy / 1000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                BigDecimal energyKWh = (energyData.energy / 1000000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                descriptionText = "${device.displayName} CumulativeEnergyExported is ${energyKWh} kWh"
                sendEvent(name: 'energyExported', value: energyKWh, unit: 'kWh', descriptionText: descriptionText)
                if (txtEnable) { log.info "${descriptionText}" }
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse EnergyMeasurementStruct for 0091_0002: ${descMap.data}"
            }
            break
        
        case '0091_0003': // attribute 'PeriodicEnergyImported'
            // EnergyMeasurementStruct: [energy (mWh), startTimestamp, endTimestamp, ...]
            Map energyData = parseEnergyMeasurementStruct(descMap.data)
            if (energyData.energy != null) {
                BigDecimal energyWh = (energyData.energy / 1000000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                descriptionText = "${device.displayName} PeriodicEnergyImported is ${energyWh} Wh"
                if (energyData.startTime && energyData.endTime) {
                    Integer periodSeconds = energyData.endTime - energyData.startTime
                    descriptionText += " (period: ${periodSeconds}s)"
                }
                message = "${prefix}PeriodicEnergyImported: ${energyWh} Wh"
                //if (txtEnable) { log.info "${descriptionText}" }
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse EnergyMeasurementStruct for 0091_0003: ${descMap.data}"
            }
            break
            
        case '0091_0004': // attribute 'PeriodicEnergyExported'
            // EnergyMeasurementStruct: [energy (mWh), startTimestamp, endTimestamp, ...]
            Map energyData = parseEnergyMeasurementStruct(descMap.data)
            if (energyData.energy != null) {
                BigDecimal energyWh = (energyData.energy / 1000000).toBigDecimal().setScale(3, BigDecimal.ROUND_HALF_UP)
                descriptionText = "${device.displayName} PeriodicEnergyExported is ${energyWh} Wh"
                if (energyData.startTime && energyData.endTime) {
                    Integer periodSeconds = energyData.endTime - energyData.startTime
                    descriptionText += " (period: ${periodSeconds}s)"
                }
                message = "${prefix}PeriodicEnergyExported: ${energyWh} Wh"
                //if (txtEnable) { log.info "${descriptionText}" }
            } else {
                log.warn "processPowerEnergyAttributeReport: failed to parse EnergyMeasurementStruct for 0091_0004: ${descMap.data}"
            }
            break
        
        case '0091_0005':
            //  Power Measurement unprocessed cluster 0091 attribute 0005 (to be re-processed in the child driver!) (value:[callbackType:Report, endpointInt:2, clusterInt:145, attrInt:5, data:[5:STRUCT:[3:UINT:0, 0:UINT:0, 1:UINT:0, 2:UINT:0]], cluster:0091, endpoint:02, attrId:0005])
            log.warn "processPowerEnergyAttributeReport: UNPROCESSED cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}"
            break

        case '0090_FFFC': // FeatureMap
            Integer featureMap = safeHexToInt(descMap.value)
            String featuresText = decodeFeatureMap_0090(featureMap)
            message = "${prefix}FeatureMap: ${featuresText} (0x${descMap.value})"
            break
        case '0091_FFFC':
            Integer featureMap = safeHexToInt(descMap.value)
            String featuresText = decodeFeatureMap_0091(featureMap)
            message = "${prefix}FeatureMap: ${featuresText} (0x${descMap.value})"
            break
        case '0090_FFFB': // AttributeList
        case '0091_FFFB':
            message = "${prefix}AttributeList: ${descMap.value}"
            useDebugLog = true
            break
        case '0090_FFFD': // ClusterRevision
        case '0091_FFFD':
            Integer revision = safeHexToInt(descMap.value)
            message = "${prefix}ClusterRevision: ${revision}"
            break
        case '0090_FFF8': // GeneratedCommandList (events supported) - stored in fingerprintData only
        case '0091_FFF8': 
            message = "${prefix}GeneratedCommandList: ${descMap.value}"
            useDebugLog = true
            break
        case '0090_FFF9': // AcceptedCommandList - stored in fingerprintData only
        case '0091_FFF9':
            message = "${prefix}AcceptedCommandList: ${descMap.value}"
            useDebugLog = true
            break
        case '0090_FFFA': // EventList - stored in fingerprintData only
        case '0091_FFFA':
            message = "${prefix}EventList: ${descMap.value}"
            useDebugLog = true
            break

        default:
            /*if (logEnable) {*/ log.warn "processPowerEnergyAttributeReport: unexpected cluster_attrId:[${descMap.cluster}_${descMap.attrId}] data:${descMap.data}" // }
    }
    // Conditional logging after the switch
    if (message != null) {
        if (isInfoMode) {
            logInfo message
        } else {
            if (useDebugLog) {
                logDebug message
            } else {
                logInfo message
            }
        }
    }     
}

// PowerModeEnum for attribute 0x0000 in cluster 0x0090
String decodePowerMode(Integer powerMode) {
    switch (powerMode) {
        case 0: return 'Unknown'
        case 1: return 'DC'
        case 2: return 'AC'
        default: return "Unknown(0x${Integer.toHexString(powerMode)})"
    }
}

// Parse EnergyMeasurementStruct from cluster 0x0091
// Structure: [0:INT:energy_mwh, 1:UINT:startTimestamp, 2:UINT:endTimestamp, 3:UINT:startSystime, 4:UINT:endSystime, 5:INT:apparentEnergy, 6:INT:reactiveEnergy]
Map parseEnergyMeasurementStruct(Map data) {
    Map result = [:]
    try {
        // Data is in format like: [4:STRUCT:[1:UINT:824370960, 2:UINT:824371020, 0:INT:0]]
        // First, get the struct data (value of the attribute ID key)
        def structData = data.values().find { it instanceof Map }
        if (!structData) {
            logDebug "parseEnergyMeasurementStruct: no struct data found in ${data}"
            return result
        }
        
        // Parse the struct fields by their tag IDs
        structData.each { key, value ->
            String[] parts = key.split(':')
            if (parts.length >= 2) {
                Integer tag = parts[0] as Integer
                switch (tag) {
                    case 0: // Energy in mWh
                        result.energy = value as Long
                        break
                    case 1: // StartTimestamp (epoch_s)
                        result.startTime = value as Long
                        break
                    case 2: // EndTimestamp (epoch_s)
                        result.endTime = value as Long
                        break
                    case 3: // StartSystime (systime_ms)
                        result.startSystime = value as Long
                        break
                    case 4: // EndSystime (systime_ms)
                        result.endSystime = value as Long
                        break
                    case 5: // ApparentEnergy (mVAh)
                        result.apparentEnergy = value as Long
                        break
                    case 6: // ReactiveEnergy (mVARh)
                        result.reactiveEnergy = value as Long
                        break
                }
            }
        }
    } catch (Exception e) {
        logWarn "parseEnergyMeasurementStruct: exception ${e} parsing ${data}"
    }
    return result
}

// Parse MeasurementAccuracyStruct from cluster 0x0091 attribute 0x0000
// Structure: [0:UINT:measurementType, 1:BOOL:measured, 2:INT:minMeasuredValue, 3:INT:maxMeasuredValue, 4:ARRAY-STRUCT:accuracyRanges]
Map parseMeasurementAccuracyStruct(Map data) {
    Map result = [:]
    try {
        // Get the struct data
        def structData = data.values().find { it instanceof Map }
        if (!structData) {
            logDebug "parseMeasurementAccuracyStruct: no struct data found in ${data}"
            return result
        }
        
        // Parse the struct fields by their tag IDs
        structData.each { key, value ->
            String[] parts = key.split(':')
            if (parts.length >= 2) {
                Integer tag = parts[0] as Integer
                switch (tag) {
                    case 0: // MeasurementType (MeasurementTypeEnum)
                        result.measurementType = value as Integer
                        break
                    case 1: // Measured (boolean)
                        result.measured = value as Boolean
                        break
                    case 2: // MinMeasuredValue (int64s)
                        result.minMeasuredValue = value as Long
                        break
                    case 3: // MaxMeasuredValue (int64s)
                        result.maxMeasuredValue = value as Long
                        break
                    case 4: // AccuracyRanges (array of MeasurementAccuracyRangeStruct)
                        // This is complex, just note it exists
                        result.hasAccuracyRanges = true
                        break
                }
            }
        }
    } catch (Exception e) {
        logWarn "parseMeasurementAccuracyStruct: exception ${e} parsing ${data}"
    }
    return result
}

// Parse array of MeasurementAccuracyStruct from cluster 0x0090 attribute 0x0002
List<Map> parseMeasurementAccuracyArray(Map data) {
    List<Map> result = []
    try {
        logDebug "parseMeasurementAccuracyArray: parsing data with ${data.size()} keys"
        // Find the array-struct in the data
        def arrayData = data.values().find { it instanceof List || it instanceof ArrayList }
        if (!arrayData) {
            logDebug "parseMeasurementAccuracyArray: no array data found in ${data}"
            return result
        }
        
        logDebug "parseMeasurementAccuracyArray: found array with ${arrayData.size()} elements"
        
        // Each element in the array is a MeasurementAccuracyStruct
        arrayData.each { structElement ->
            if (structElement instanceof Map) {
                Map accuracy = [:]
                List<Map> accuracyRanges = []
                
                structElement.each { key, value ->
                    String[] parts = key.split(':')
                    if (parts.length >= 2) {
                        Integer tag = parts[0] as Integer
                        switch (tag) {
                            case 0: // MeasurementType
                                accuracy.measurementType = value as Integer
                                break
                            case 1: // Measured
                                accuracy.measured = value as Boolean
                                break
                            case 2: // MinMeasuredValue
                                accuracy.minMeasuredValue = value as Long
                                break
                            case 3: // MaxMeasuredValue
                                accuracy.maxMeasuredValue = value as Long
                                break
                            case 4: // AccuracyRanges array
                                if (value instanceof List) {
                                    value.each { rangeElement ->
                                        if (rangeElement instanceof Map) {
                                            Map range = [:]
                                            rangeElement.each { rKey, rValue ->
                                                String[] rParts = rKey.split(':')
                                                if (rParts.length >= 2) {
                                                    Integer rTag = rParts[0] as Integer
                                                    switch (rTag) {
                                                        case 0: range.rangeMin = rValue as Long; break
                                                        case 1: range.rangeMax = rValue as Long; break
                                                        case 2: range.percentMax = rValue as Integer; break
                                                        case 3: range.percentMin = rValue as Integer; break
                                                        case 4: range.percentTypical = rValue as Integer; break
                                                    }
                                                }
                                            }
                                            if (range) accuracyRanges.add(range)
                                        }
                                    }
                                }
                                break
                        }
                    }
                }
                accuracy.accuracyRanges = accuracyRanges
                result.add(accuracy)
                logDebug "parseMeasurementAccuracyArray: parsed accuracy: type=${accuracy.measurementType}, min=${accuracy.minMeasuredValue}, max=${accuracy.maxMeasuredValue}, ranges=${accuracyRanges.size()}"
            }
        }
    } catch (Exception e) {
        logWarn "parseMeasurementAccuracyArray: exception ${e} parsing ${data}"
    }
    logDebug "parseMeasurementAccuracyArray: returning ${result.size()} accuracy measurements"
    return result
}

// Format measurement range with units
String formatMeasurementRange(Map accuracy, String typeText) {
    if (!accuracy.minMeasuredValue && !accuracy.maxMeasuredValue) return ''
    
    Long min = accuracy.minMeasuredValue ?: 0
    Long max = accuracy.maxMeasuredValue ?: 0
    
    // Convert based on measurement type and add units
    String unit = ''
    BigDecimal minVal = min
    BigDecimal maxVal = max
    
    switch (accuracy.measurementType) {
        case 0x05: // ActivePower (mW)
        case 0x06: // ReactivePower
        case 0x07: // ApparentPower
        case 0x0A: // RMSPower
            minVal = (min / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            maxVal = (max / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            unit = ' W'
            break
        case 0x01: // Voltage (mV)
        case 0x08: // RMSVoltage
            minVal = (min / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            maxVal = (max / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            unit = ' V'
            break
        case 0x02: // ActiveCurrent (mA)
        case 0x03: // ReactiveCurrent
        case 0x04: // ApparentCurrent
        case 0x09: // RMSCurrent
        case 0x0D: // NeutralCurrent
            minVal = (min / 1000).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
            maxVal = (max / 1000).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
            unit = ' A'
            break
        case 0x0B: // Frequency (mHz or 0.001 Hz)
            minVal = (min / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            maxVal = (max / 1000).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
            unit = ' Hz'
            break
        case 0x0C: // PowerFactor (-10000 to 10000 = -1.0 to 1.0)
            minVal = (min / 10000).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
            maxVal = (max / 10000).toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP)
            unit = ''
            break
    }
    
    return "${minVal} to ${maxVal}${unit}"
}

// Format accuracy ranges as percentage string
String formatAccuracyRanges(List<Map> ranges) {
    if (!ranges || ranges.isEmpty()) return ''
    
    // Get the first range (typically there's one representative range)
    Map range = ranges[0]
    if (!range) return ''
    
    Integer min = range.percentMin ?: 0
    Integer typ = range.percentTypical ?: 0  
    Integer max = range.percentMax ?: 0
    
    // Convert from basis points (0.01%) to percentages
    BigDecimal minPct = (min / 100).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
    BigDecimal typPct = (typ / 100).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
    BigDecimal maxPct = (max / 100).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
    
    return " (±${minPct}-${maxPct}%, typ ${typPct}%)"
}

// MeasurementTypeEnum - used in Accuracy and Ranges structures
String decodeMeasurementType(Integer measurementType) {
    switch (measurementType) {
        case 0x00: return 'Unspecified'
        case 0x01: return 'Voltage'
        case 0x02: return 'ActiveCurrent'
        case 0x03: return 'ReactiveCurrent'
        case 0x04: return 'ApparentCurrent'
        case 0x05: return 'ActivePower'
        case 0x06: return 'ReactivePower'
        case 0x07: return 'ApparentPower'
        case 0x08: return 'RMSVoltage'
        case 0x09: return 'RMSCurrent'
        case 0x0A: return 'RMSPower'
        case 0x0B: return 'Frequency'
        case 0x0C: return 'PowerFactor'
        case 0x0D: return 'NeutralCurrent'
        case 0x0E: return 'ElectricalEnergy'
        case 0x0F: return 'ReactiveEnergy'
        case 0x10: return 'ApparentEnergy'
        case 0x11: return 'SoilMoisture'
        default: return "Unknown(0x${Integer.toHexString(measurementType)})"
    }
}

String decodeFeatureMap_0090(Integer featureMap) {
    List<String> features = []
    if ((featureMap & 0x01) != 0) { features.add('DirectCurrent') }
    if ((featureMap & 0x02) != 0) { features.add('AlternatingCurrent') }
    if ((featureMap & 0x04) != 0) { features.add('PolyphasePower') }
    if ((featureMap & 0x08) != 0) { features.add('Harmonics') }
    if ((featureMap & 0x10) != 0) { features.add('PowerQuality') }
    return features.join(', ')
}

String decodeFeatureMap_0091(Integer featureMap) {
    List<String> features = []
    if ((featureMap & 0x01) != 0) { features.add('ImportedEnergy') }
    if ((featureMap & 0x02) != 0) { features.add('ExportedEnergy') }
    if ((featureMap & 0x04) != 0) { features.add('CumulativeEnergy') }
    if ((featureMap & 0x08) != 0) { features.add('PeriodicEnergy') }
    if ((featureMap & 0x10) != 0) { features.add('ApparentEnergy') }
    if ((featureMap & 0x20) != 0) { features.add('ReactiveEnergy') }
    return features.join(', ')
}  

// Command to get all supported Power / Energy attributes (for info/debugging)
void getInfo() {
    // Check if ElectricalPowerMeasurement cluster is supported
    if (!isClusterSupported('0090')) {
        logWarn "getInfo: ElectricalPowerMeasurement cluster (0x0090) is not supported by this device"
        logInfo "getInfo: ServerList contains: ${getServerList()}"
        return
    }
    logInfo "getInfo: reading all supported ElectricalPowerMeasurement attributes: ${getElectricalPowerMeasurementAttributeList()}"
    
    // Set state flags for info mode
    if (state.states == null) { state.states = [:] }
    if (state.lastTx == null) { state.lastTx = [:] }
    state.states.isInfo = true
    state.lastTx.infoTime = now()
    state.states?.debugState = settings?.logEnable ?: 'false'  // save the debugState in state to restore it after collecting the info logs
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )  // temporarily switch off debug logging to avoid log clutter while collecting info logs
    
    // Schedule job to turn off info mode after 10 seconds
    runIn(10, 'clearInfoMode')
    
    String endpointHex = device.getDataValue('id') ?: '1'
    Integer endpoint = HexUtils.hexStringToInt(endpointHex)
    parent?.readAttribute(endpoint, 0x0090, -1)      // 0x0090 ElectricalPowerMeasurement cluster - read all attributes

    // Check if ElectricalEnergyMeasurement cluster 0x0091 is supported
    if (isClusterSupported('0091')) {
        logInfo "getInfo: reading all supported ElectricalEnergyMeasurement attributes: ${getElectricalEnergyMeasurementAttributeList()}"
        parent?.readAttribute(endpoint, 0x0091, -1)      // 0x0091 ElectricalEnergyMeasurement cluster - read all attributes
    }
    else {
        logWarn "getInfo: ElectricalEnergyMeasurement cluster (0x0091) is not supported by this device"
    }
}

// Clear info mode flag (called by scheduled job)
void clearInfoMode() {
    if (state.states == null) { state.states = [:] }
    state.states.isInfo = false
    device.updateSetting('logEnable', [value: state.states?.debugState ?: 'false', type: 'bool'] )  // restore debug logging to the value before info mode was enabled
    logDebug "clearInfoMode: info mode disabled"
}


Map getFingerprintData() {
    String fingerprintJson = device.getDataValue('fingerprintData')
    if (!fingerprintJson) {
        logDebug "getFingerprintData: fingerprintData not found in device data"
        return null
    }
    
    try {
        return new groovy.json.JsonSlurper().parseText(fingerprintJson)
    } catch (Exception e) {
        logWarn "getFingerprintData: failed to parse fingerprintData: ${e.message}"
        return null
    }
}

List<String> getServerList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getServerList: fingerprint data not available"
        return []
    }
    
    return fingerprint['ServerList'] ?: []
}


boolean isClusterSupported(String clusterHex) {
    List<String> serverList = getServerList()
    return serverList.contains(clusterHex?.toUpperCase())
}

List<String> getElectricalPowerMeasurementAttributeList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getElectricalPowerMeasurementAttributeList: fingerprint data not available"
        return []
    }
    return fingerprint['0090_FFFB'] ?: []
}


List<String> getElectricalEnergyMeasurementAttributeList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getElectricalEnergyMeasurementAttributeList: fingerprint data not available"
        return []
    }
    return fingerprint['0091_FFFB'] ?: []
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
