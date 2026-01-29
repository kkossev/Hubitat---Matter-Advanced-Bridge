/*
 *  'Matter Generic Component Air Purifier' - component driver for Matter Advanced Bridge
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
 * For a big portion of this code all credits go to @dandanache for the 'IKEA Starkvind Air Purifier (E2006)' 'https://raw.githubusercontent.com/dan-danache/hubitat/master/ikea-zigbee-drivers/Ikea_E2006.groovy'
 *
 * ver. 1.0.0  2024-10-10 kkossev   - first version
 * ver. 1.1.0  2025-01-10 kkossev   - added ping command and RTT monitoring via matterHealthStatusLib
 * ver. 1.2.0  2025-01-18 kkossev   - added ALPSTUGA Air Quality Monitor support
 * ver. 1.2.1  2026-01-29 kkossev   - added common library matterCommonLib
 * 
 *                                   TODO: use safeToHex methods;  decodeIeee754Float method float value
 *                                   TODO: add cluster 0071 'HEPAFilterMonitoring' endpointId:"0B"
 *                                   TODO: add cluster 0202 'Window Covering' endpointId:"0B"
 *
*/

import groovy.transform.Field
import groovy.transform.CompileStatic
import hubitat.helper.HexUtils

@Field static final String matterComponentAirPurifierVersion = '1.2.1'
@Field static final String matterComponentAirPurifierStamp   = '2026/01/29 10:38 PM'

@Field static final Boolean _DEBUG_AIR_PURIFIER = false    // make it FALSE for production!

metadata {
    definition(name: 'Matter Generic Component Air Purifier', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Generic_Component_Air_Quality') {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'Actuator'
        capability 'AirQuality'
        capability 'FanControl'
        capability 'FilterStatus'
        capability 'Switch'
        capability 'HealthCheck'
        capability 'PowerSource'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'CarbonDioxideMeasurement'

        command    'identify'
        command    'getInfo'
        // Commands for devices.Ikea_E2006
        command 'setSpeed', [[name:'Fan speed*', type:'ENUM', description:'Select the desired fan speed', constraints:SUPPORTED_FAN_SPEEDS]]
        command 'toggle'
        command 'setIndicatorStatus', [[name:'Status*', type:'ENUM', description:'Select LED indicators status on the device', constraints:['on', 'off']]]
        
        // Attributes for devices.Ikea_E2006
        attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
        attribute 'filterUsage', 'number'
        attribute 'pm25', 'number'
        attribute 'auto', 'enum', ['on', 'off']
        attribute 'indicatorStatus', 'enum', ['on', 'off']
        

        if (_DEBUG_AIR_PURIFIER) {
            command 'getBridgeInfo', [
                    [name:'infoType', type: 'ENUM', description: 'Bridge Info Type', constraints: ['Basic', 'Extended']],   // if the parameter name is 'type' - shows a drop-down list of the available drivers!
                    [name:'endpoint', type: 'STRING', description: 'Endpoint', constraints: ['STRING']]
            ]
        }
    }
}

preferences {
    section {
	    input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
        input name: 'logEnable',
              type: 'bool',
              title: '<b>Enable debug logging</b>',
              required: false,
              defaultValue: true

        input name: 'txtEnable',
              type: 'bool',
              title: '<b>Enable descriptionText logging</b>',
              required: false,
              defaultValue: true

        // Inputs for devices.Ikea_E2006
        input(
            name: 'pm25ReportDelta', type: 'enum',
            title: 'Sensor report frequency',
            description: '<small>Adjust how often the device sends its PM 2.5 sensor data.</small>',
            options: [
                '01': 'Very High - report changes of +/- 1μg/m3',
                '02': 'High - report changes of +/- 2μg/m3',
                '03': 'Medium - report changes of +/- 3μg/m3',
                '05': 'Low - report changes of +/- 5μg/m3',
                '10': 'Very Low - report changes of +/- 10μg/m3'
            ],
            defaultValue: '03',
            required: true
        )
        input(
            name: 'co2ReportDelta', type: 'enum',
            title: 'CO₂ report frequency',
            description: '<small>Adjust how often the device sends its CO₂ sensor data.</small>',
            options: [
                '05': 'Very High - report changes of +/- 5 ppm',
                '10': 'High - report changes of +/- 10 ppm',
                '25': 'Medium - report changes of +/- 25 ppm',
                '50': 'Low - report changes of +/- 50 ppm',
                '100': 'Very Low - report changes of +/- 100 ppm'
            ],
            defaultValue: '10',
            required: true
        )
        input(
            name: 'filterLifeTime', type: 'enum',
            title: 'Filter life time',
            description: '<small>Configure time between filter changes (default 6 months).</small>',
            options: [
                 '90': '3 months',
                '180': '6 months',
                '270': '9 months',
                '360': '1 year'
            ],
            defaultValue: '180',
            required: true
        )
        input(
            name: 'childLock', type: 'bool',
            title: 'Child lock',
            description: '<small>Lock physical controls, safeguarding against accidental operation.</small>',
            defaultValue: false
        )
    }
}

// Fields for devices.Ikea_E2006
@Field static final List<String> SUPPORTED_FAN_SPEEDS = [
    'auto', 'low', 'medium-low', 'medium', 'medium-high', 'high', 'off'
]


void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${description}" }
    description.each { d ->
        if (d.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
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

void identify() {
    if (logEnable) { log.debug "${device.displayName} identifying ..." }
    parent?.componentIdentify(device)
}

void on() {
    if (logEnable) { log.debug "${device.displayName} turning on ..." }
    parent?.componentOn(device)
}

void off() {
    if (logEnable) { log.debug "${device.displayName} turning off ..." }
    parent?.componentOff(device)
}

void toggle() {
    if (device.currentValue('switch', true) == 'on') { off() }
    else { on() }
}

void setSpeed(String speed) {
    if (logEnable) { log.debug "Setting speed to: ${speed}" }
    Integer newSpeed = 0x00
    switch (speed) {
        case 'on':
        case 'auto':
            newSpeed = 1
            break
        case 'low':
            newSpeed = 10
            break
        case 'medium-low':
            newSpeed = 20
            break
        case 'medium':
            newSpeed = 30
            break
        case 'medium-high':
            newSpeed = 40
            break
        case 'high':
            newSpeed = 50
            break
        case 'off':
            newSpeed = 0
            break
        default:
            if (logEnable) { log.warn "Unknown speed: ${speed}" }
            return
    }
    parent?.componentSetSpeed(device, newSpeed)
}

void cycleSpeed() {
    String curSpeed = device.currentValue('speed', true)
    if (logEnable) { log.debug "Current speed is: ${curSpeed}" }
    Integer newSpeed = 0x00
    switch (curSpeed) {
        case 'high':
        case 'off':
            newSpeed = 10
            break
        case 'low':
            newSpeed = 20
            break
        case 'medium-low':
            newSpeed = 30
            break
        case 'medium':
            newSpeed = 40
            break
        case 'medium-high':
            newSpeed = 50
            break
        default:
            if (logEnable) { log.warn "Unknown current speed: ${curSpeed}" }
            return
    }
    if (logEnable) { log.debug "Cycling speed to: ${newSpeed}" }
    parent?.componentSetSpeed(device, newSpeed)
}

void setIndicatorStatus(String status) {
    if (logEnable) { log.debug "Setting status indicator to: ${status}" }
    sendEvent(name:'indicatorStatus', value:status, descriptionText:"Indicator status turned ${status}", type:'digital')
    // TODO!
}

private Integer lerp(Integer ylo, Integer yhi, BigDecimal xlo, BigDecimal xhi, Integer cur) {
    return Math.round(((cur - xlo) / (xhi - xlo)) * (yhi - ylo) + ylo)
}

private List pm25Aqi(Integer pm25) { // See: https://en.wikipedia.org/wiki/Air_quality_index#United_States
    if (pm25 <=  12.1) return [lerp(  0,  50,   0.0,  12.0, pm25), 'good', 'green']
    if (pm25 <=  35.5) return [lerp( 51, 100,  12.1,  35.4, pm25), 'moderate', 'gold']
    if (pm25 <=  55.5) return [lerp(101, 150,  35.5,  55.4, pm25), 'unhealthy for sensitive groups', 'darkorange']
    if (pm25 <= 150.5) return [lerp(151, 200,  55.5, 150.4, pm25), 'unhealthy', 'red']
    if (pm25 <= 250.5) return [lerp(201, 300, 150.5, 250.4, pm25), 'very unhealthy', 'purple']
    if (pm25 <= 350.5) return [lerp(301, 400, 250.5, 350.4, pm25), 'hazardous', 'maroon']
    if (pm25 <= 500.5) return [lerp(401, 500, 350.5, 500.4, pm25), 'hazardous', 'maroon']
    return [500, 'hazardous', 'maroon']
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
        runIn(14400, 'logsOff')
    }
    // TODO!
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName} "
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}


/**
 * Decode IEEE754 single-precision float from hex string
 * @param hexValue Hex string (e.g., "40A00000")
 * @return Rounded integer value
 */
Integer decodeIeee754Float(String hexValue) {
    // Minimal version: expects valid String input (hex or decimal)
    if (hexValue == null) return null
    if (hexValue =~ /^\d+\.\d+$/) {
        return Math.round(Float.parseFloat(hexValue))
    } else {
        Integer bits = Integer.parseUnsignedInt(hexValue, 16)
        return Math.round(Float.intBitsToFloat(bits))
    }
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

// ============ Helper Methods for Fingerprint Data ============

/**
 * Get parsed fingerprint data from device
 * @return Map containing fingerprint data or null if not available
 */
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

/**
 * Get ServerList from fingerprint data
 * @return List of cluster IDs as hex strings (e.g., ["0006", "005B", "0402", "0405", "040D", "042A"])
 */
List<String> getServerList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getServerList: fingerprint data not available"
        return []
    }
    
    return fingerprint['ServerList'] ?: []
}

/**
 * Check if a specific cluster is supported by this device
 * @param clusterHex Cluster ID as hex string (e.g., "005B" for Air Quality)
 * @return true if cluster is in ServerList
 */
boolean isClusterSupported(String clusterHex) {
    List<String> serverList = getServerList()
    return serverList.contains(clusterHex?.toUpperCase())
}

// Custom parsing for description strings with array values
// NOTE: This workaround may not be needed once complex structure parsing is fixed generally
Map patchParseDescriptionMap(String inputString) {
    Map<String, Object> resultMap = [:]
    
    // Remove outer brackets only
    String cleaned = inputString.replaceAll('^\\[', '').replaceAll('\\]$', '')
    
    // Split by ", " but preserve array values
    List<String> parts = []
    int bracketDepth = 0
    StringBuilder current = new StringBuilder()
    
    for (int i = 0; i < cleaned.length(); i++) {
        char c = cleaned.charAt(i)
        if (c == '[') {
            bracketDepth++
            current.append(c)
        } else if (c == ']') {
            bracketDepth--
            current.append(c)
        } else if (c == ',' && bracketDepth == 0 && i + 1 < cleaned.length() && cleaned.charAt(i + 1) == ' ') {
            parts.add(current.toString())
            current = new StringBuilder()
            i++ // skip the space after comma
        } else {
            current.append(c)
        }
    }
    if (current.length() > 0) {
        parts.add(current.toString())
    }
    
    // Now parse each key:value pair
    parts.each { pair ->
        String[] kvParts = pair.split(':', 2)
        if (kvParts.size() == 2) {
            String key = kvParts[0].trim()
            String value = kvParts[1].trim()
            // Check if value is an array
            if (value.startsWith('[') && value.endsWith(']')) {
                // Parse array value
                String arrayContent = value.substring(1, value.length() - 1)
                resultMap[key] = arrayContent.split(',\\s*').collect { it.trim() }
            } else {
                resultMap[key] = value
            }
        }
    }
    
    return resultMap
}

void processUnprocessed(Map description) {
    logDebug "processing unprocessed: ${description}"

    String inputString = description.value
    logTrace "inputString: ${inputString}"
    
    // Parse the input string to handle array values like value:[00, FFF8, FFF9]
    Map descMap = patchParseDescriptionMap(inputString)
    logDebug "descMap: ${descMap}"
    
    String eventValue = descMap.value
    String descriptionText = "${device.displayName} ${descMap.cluster}:${descMap.attrId} value:${eventValue}"
    
    // Check if this is info mode for detailed logging
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${descMap.cluster}_${descMap.attrId}] " : ""
    
    switch (descMap.cluster + '_' + descMap.attrId) {
        case '005B_0000': // attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
            eventValue= AirQualityEnum[Integer.parseInt(descMap.value, 16)]
            // Check if value changed from last report
            String lastAirQuality = state.lastAirQuality
            if (lastAirQuality == eventValue) {
                if (logEnable) { log.debug "${device.displayName} AirQuality unchanged: ${eventValue} - event suppressed" }
                return
            }
            state.lastAirQuality = eventValue
            descriptionText = "${device.displayName} AirQuality: ${eventValue} (raw:${descMap.value})"
            sendEvent(name: 'airQuality', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '042A_0000': // attribute 'pm25', 'number'
            Integer pm25Int
            if (descMap.value instanceof Number) {
                pm25Int = Math.round(descMap.value as Float)
            } else if (descMap.value instanceof String) {
                pm25Int = decodeIeee754Float(descMap.value)
            } else {
                logWarn "Unexpected type for PM2.5 value: value=${descMap.value}, not Number or String"
                return
            }
            if (logEnable) { log.debug "${device.displayName} PM2.5 raw: ${descMap.value}" }
            if (logEnable) { log.debug "${device.displayName} PM2.5 decoded: ${pm25Int} μg/m³" }
            // Check threshold from preference
            Integer threshold = (settings.pm25ReportDelta ?: '03') as Integer
            Integer lastPM25 = state.lastPM25 != null ? state.lastPM25 as Integer : null
            if (lastPM25 != null && Math.abs(pm25Int - lastPM25) < threshold) {
                if (logEnable) { log.debug "${device.displayName} PM2.5 change ${pm25Int - lastPM25} μg/m³ below threshold ${threshold} - event suppressed" }
                return
            }
            state.lastPM25 = pm25Int
            descriptionText = "${device.displayName} PM2.5: ${pm25Int} μg/m³"
            sendEvent(name: 'pm25', value: pm25Int, unit: 'μg/m³', descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '040D_0000': // CO₂ Concentration Measurement
            Integer co2
            if (descMap.value instanceof Number) {
                co2 = Math.round(descMap.value as Float)
            } else if (descMap.value instanceof String) {
                co2 = decodeIeee754Float(descMap.value)
            } else {
                logWarn "Unexpected type for CO₂ value: value=${descMap.value}, not Number or String"
                return
            }
            if (logEnable) { log.debug "${device.displayName} CO₂ raw: ${descMap.value}" }
            if (logEnable) { log.debug "${device.displayName} CO₂ decoded: ${co2} ppm" }
            // Check threshold from preference
            Integer threshold = (settings.co2ReportDelta ?: '10') as Integer
            Integer lastCO2 = state.lastCO2 != null ? state.lastCO2 as Integer : null
            if (lastCO2 != null && Math.abs(co2 - lastCO2) < threshold) {
                if (logEnable) { log.debug "${device.displayName} CO₂ change ${co2 - lastCO2} ppm below threshold ${threshold} - event suppressed" }
                return
            }
            state.lastCO2 = co2
            descriptionText = "${device.displayName} CO₂: ${co2} ppm"
            sendEvent(name: 'carbonDioxide', value: co2, unit: 'ppm', descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        
        // Additional Concentration Measurement cluster attributes (CO₂ and PM2.5)
        case '040D_0001': // CO₂ MinMeasuredValue (IEEE754 float)
        case '042A_0001': // PM2.5 MinMeasuredValue (IEEE754 float)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                String unit = descMap.cluster == '040D' ? 'ppm' : 'μg/m³'
                Integer decoded
                if (descMap.value instanceof Number) {
                    decoded = Math.round(descMap.value as Float)
                } else if (descMap.value instanceof String) {
                    decoded = decodeIeee754Float(descMap.value)
                } else {
                    logWarn "Unexpected type for MinMeasuredValue: value=${descMap.value}, not Number or String"
                    return
                }
                String attrName = ConcentrationMeasurementClusterAttributes[0x0001]
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded} ${unit} (raw: ${descMap.value})"
            }
            break
        
        case '040D_0002': // CO₂ MaxMeasuredValue (IEEE754 float)
        case '042A_0002': // PM2.5 MaxMeasuredValue (IEEE754 float)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                String unit = descMap.cluster == '040D' ? 'ppm' : 'μg/m³'
                Integer decoded
                if (descMap.value instanceof Number) {
                    decoded = Math.round(descMap.value as Float)
                } else if (descMap.value instanceof String) {
                    decoded = decodeIeee754Float(descMap.value)
                } else {
                    logWarn "Unexpected type for MaxMeasuredValue: value=${descMap.value}, not Number or String"
                    return
                }
                String attrName = ConcentrationMeasurementClusterAttributes[0x0002]
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded} ${unit} (raw: ${descMap.value})"
            }
            break
        
        case '040D_0007': // CO₂ Uncertainty (IEEE754 float)
        case '042A_0007': // PM2.5 Uncertainty (IEEE754 float)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                String unit = descMap.cluster == '040D' ? 'ppm' : 'μg/m³'
                Integer decoded
                if (descMap.value instanceof Number) {
                    decoded = Math.round(descMap.value as Float)
                } else if (descMap.value instanceof String) {
                    decoded = decodeIeee754Float(descMap.value)
                } else {
                    logWarn "Unexpected type for Uncertainty: value=${descMap.value}, not Number or String"
                    return
                }
                String attrName = ConcentrationMeasurementClusterAttributes[0x0007]
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded} ${unit} (raw: ${descMap.value})"
            }
            break
        
        case '040D_0008': // CO₂ MeasurementUnit (enum)
        case '042A_0008': // PM2.5 MeasurementUnit (enum)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                Integer attrIdInt = Integer.parseInt(descMap.attrId, 16)
                String attrName = ConcentrationMeasurementClusterAttributes[attrIdInt]
                Integer enumValue = Integer.parseInt(descMap.value, 16)
                String decoded = decodeMeasurementUnit(enumValue)
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded}"
            }
            break
        
        case '040D_0009': // CO₂ MeasurementMedium (enum)
        case '042A_0009': // PM2.5 MeasurementMedium (enum)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                Integer attrIdInt = Integer.parseInt(descMap.attrId, 16)
                String attrName = ConcentrationMeasurementClusterAttributes[attrIdInt]
                Integer enumValue = Integer.parseInt(descMap.value, 16)
                String decoded = decodeMeasurementMedium(enumValue)
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded}"
            }
            break
        
        case '040D_000A': // CO₂ LevelValue (enum)
        case '042A_000A': // PM2.5 LevelValue (enum)
            if (isInfoMode) {
                String clusterName = descMap.cluster == '040D' ? 'CO₂' : 'PM2.5'
                Integer attrIdInt = Integer.parseInt(descMap.attrId, 16)
                String attrName = ConcentrationMeasurementClusterAttributes[attrIdInt]
                Integer enumValue = Integer.parseInt(descMap.value, 16)
                String decoded = decodeLevelValue(enumValue)
                logInfo "${prefix}${clusterName} ${attrName}: ${decoded}"
            }
            break
        
        // Global cluster attributes (apply to all clusters: 005B, 040D, 042A, etc.)
        case '005B_FFFC': // FeatureMap for Air Quality
        case '040D_FFFC': // FeatureMap for CO₂
        case '042A_FFFC': // FeatureMap for PM2.5
            if (isInfoMode) {
                String clusterName = descMap.cluster == '005B' ? 'Air Quality' : (descMap.cluster == '040D' ? 'CO₂' : 'PM2.5')
                Integer featureMap = descMap.value ? Integer.parseInt(descMap.value, 16) : 0
                String attrName = descMap.cluster == '005B' ? AirQualityClusterAttributes[0xFFFC] : ConcentrationMeasurementClusterAttributes[0xFFFC]
                String decoded = descMap.cluster == '005B' ? 
                    decodeAirQualityFeatureMap(featureMap) : 
                    decodeConcentrationMeasurementFeatureMap(featureMap)
                logInfo "${prefix}${clusterName} ${attrName}: 0x${descMap.value} (${featureMap}) - Features: ${decoded}"
            }
            break
        
        case '005B_FFFD': // ClusterRevision for Air Quality
        case '040D_FFFD': // ClusterRevision for CO₂  
        case '042A_FFFD': // ClusterRevision for PM2.5
            if (isInfoMode) {
                String clusterName = descMap.cluster == '005B' ? 'Air Quality' : (descMap.cluster == '040D' ? 'CO₂' : 'PM2.5')
                Integer revision = descMap.value ? Integer.parseInt(descMap.value, 16) : 0
                String attrName = descMap.cluster == '005B' ? AirQualityClusterAttributes[0xFFFD] : ConcentrationMeasurementClusterAttributes[0xFFFD]
                logInfo "${prefix}${clusterName} ${attrName}: ${revision}"
            }
            break
        
        case '005B_FFFB': // AttributeList for Air Quality
        case '040D_FFFB': // AttributeList for CO₂
        case '042A_FFFB': // AttributeList for PM2.5
            if (isInfoMode) {
                String clusterName = descMap.cluster == '005B' ? 'Air Quality' : (descMap.cluster == '040D' ? 'CO₂' : 'PM2.5')
                String attrName = descMap.cluster == '005B' ? AirQualityClusterAttributes[0xFFFB] : ConcentrationMeasurementClusterAttributes[0xFFFB]
                logInfo "${prefix}${clusterName} ${attrName}: ${descMap.value}"
            }
            break
        
        case '005B_FFF9': // AcceptedCommandList for Air Quality
        case '040D_FFF9': // AcceptedCommandList for CO₂
        case '042A_FFF9': // AcceptedCommandList for PM2.5
            if (isInfoMode) {
                String clusterName = descMap.cluster == '005B' ? 'Air Quality' : (descMap.cluster == '040D' ? 'CO₂' : 'PM2.5')
                String attrName = descMap.cluster == '005B' ? AirQualityClusterAttributes[0xFFF9] : ConcentrationMeasurementClusterAttributes[0xFFF9]
                logInfo "${prefix}${clusterName} ${attrName}: ${descMap.value ?: '[]'}"
            }
            break
        
        case '005B_FFF8': // GeneratedCommandList for Air Quality
        case '040D_FFF8': // GeneratedCommandList for CO₂
        case '042A_FFF8': // GeneratedCommandList for PM2.5
            if (isInfoMode) {
                String clusterName = descMap.cluster == '005B' ? 'Air Quality' : (descMap.cluster == '040D' ? 'CO₂' : 'PM2.5')
                String attrName = descMap.cluster == '005B' ? AirQualityClusterAttributes[0xFFF8] : ConcentrationMeasurementClusterAttributes[0xFFF8]
                logInfo "${prefix}${clusterName} ${attrName}: ${descMap.value ?: '[]'}"
            }
            break
        
        // Note: Temperature (0402) and Humidity (0405) are handled by the parent driver
        // and sent as standard events, not as 'unprocessed'
        default:
            logWarn "processUnprocessed: unexpected cluster:${descMap.cluster} attrId:${descMap.attrId}"
    }
}

void getInfo() {
    // Get ServerList to see what clusters are supported
    List<String> serverList = getServerList()
    if (serverList.isEmpty()) {
        logWarn "getInfo: ServerList is empty or not available"
        return
    }
    
    logInfo "getInfo: Device supports clusters: ${serverList}"
    
    // Set state flags for info mode
    if (state.states == null) { state.states = [:] }
    if (state.lastTx == null) { state.lastTx = [:] }
    state.states.isInfo = true
    state.lastTx.infoTime = now()
    
    // Schedule job to turn off info mode after 10 seconds
    runIn(10, 'clearInfoMode')
    
    // Get the endpoint ID
    String endpointHex = device.getDataValue('id') ?: '1'
    Integer endpoint = HexUtils.hexStringToInt(endpointHex)
    
    // Read ALL attributes from each supported cluster
    // Cluster 0x0006 - OnOff / Switch
    if (isClusterSupported('0006')) {
        logInfo "getInfo: reading all OnOff cluster attributes"
        parent?.readAttribute(endpoint, 0x0006, -1)
    }
    
    // Cluster 0x005B - Air Quality
    if (isClusterSupported('005B')) {
        logInfo "getInfo: reading all Air Quality cluster attributes"
        parent?.readAttribute(endpoint, 0x005B, -1)
    }
    
    // Cluster 0x0402 - Temperature Measurement
    if (isClusterSupported('0402')) {
        logInfo "getInfo: reading all Temperature Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x0402, -1)
    }
    
    // Cluster 0x0405 - Relative Humidity Measurement
    if (isClusterSupported('0405')) {
        logInfo "getInfo: reading all Relative Humidity Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x0405, -1)
    }
    
    // Cluster 0x040D - Carbon Dioxide Concentration Measurement
    if (isClusterSupported('040D')) {
        logInfo "getInfo: reading all CO₂ Concentration Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x040D, -1)
    }
    
    // Cluster 0x042A - PM2.5 Concentration Measurement
    if (isClusterSupported('042A')) {
        logInfo "getInfo: reading all PM2.5 Concentration Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x042A, -1)
    }
    
    logInfo "getInfo: completed - check live logs and device data for results"
}

// Clear info mode flag (called by scheduled job)
void clearInfoMode() {
    if (state.states == null) { state.states = [:] }
    state.states.isInfo = false
    logDebug "clearInfoMode: info mode disabled"
}

// ============ FeatureMap Decoders ============

/**
 * Decode Air Quality cluster FeatureMap bitmap
 * Bit 0 (0x01): FAIR - Fair Air Quality
 * Bit 1 (0x02): MOD - Moderate Air Quality
 * Bit 2 (0x04): VPOOR - Very Poor Air Quality
 * Bit 3 (0x08): XPOOR - Extremely Poor Air Quality
 */
String decodeAirQualityFeatureMap(Integer featureMap) {
    List<String> features = []
    if (featureMap & 0x01) { features.add('Fair') }
    if (featureMap & 0x02) { features.add('Moderate') }
    if (featureMap & 0x04) { features.add('VeryPoor') }
    if (featureMap & 0x08) { features.add('ExtremelyPoor') }
    return features.isEmpty() ? 'None' : features.join(', ')
}

/**
 * Decode Concentration Measurement cluster FeatureMap bitmap
 * Applies to both CO₂ (0x040D) and PM2.5 (0x042A) clusters
 * Bit 0 (0x01): MEA - NumericMeasurement
 * Bit 1 (0x02): LEV - LevelIndication
 * Bit 2 (0x04): MED - MediumLevel
 * Bit 3 (0x08): CRI - CriticalLevel
 * Bit 4 (0x10): PEA - PeakMeasurement
 * Bit 5 (0x20): AVG - AverageMeasurement
 */
String decodeConcentrationMeasurementFeatureMap(Integer featureMap) {
    List<String> features = []
    if (featureMap & 0x01) { features.add('NumericMeasurement') }
    if (featureMap & 0x02) { features.add('LevelIndication') }
    if (featureMap & 0x04) { features.add('MediumLevel') }
    if (featureMap & 0x08) { features.add('CriticalLevel') }
    if (featureMap & 0x10) { features.add('PeakMeasurement') }
    if (featureMap & 0x20) { features.add('AverageMeasurement') }
    return features.isEmpty() ? 'None' : features.join(', ')
}

/**
 * Decode MeasurementUnitEnum
 * Per Matter spec Table 94
 */
String decodeMeasurementUnit(Integer value) {
    switch (value) {
        case 0: return 'PPM (parts per million)'
        case 1: return 'PPB (parts per billion)'
        case 2: return 'PPT (parts per trillion)'
        case 3: return 'mg/m³ (milligrams per cubic meter)'
        case 4: return 'μg/m³ (micrograms per cubic meter)'
        case 5: return 'ng/m³ (nanograms per cubic meter)'
        case 6: return 'pm/m³ (particles per cubic meter)'
        case 7: return 'Bq/m³ (becquerels per cubic meter)'
        default: return "Unknown (${value})"
    }
}

/**
 * Decode MeasurementMediumEnum
 * Per Matter spec Table 95
 */
String decodeMeasurementMedium(Integer value) {
    switch (value) {
        case 0: return 'Air'
        case 1: return 'Water'
        case 2: return 'Soil'
        default: return "Unknown (${value})"
    }
}

/**
 * Decode LevelValueEnum
 * Per Matter spec Table 96
 */
String decodeLevelValue(Integer value) {
    switch (value) {
        case 0: return 'Unknown'
        case 1: return 'Low'
        case 2: return 'Medium'
        case 3: return 'High'
        case 4: return 'Critical'
        default: return "Unknown (${value})"
    }
}


// ============ Matter Air Quality Cluster Attributes Map ============
// Air Quality cluster (0x005B) attributes per Matter spec
@Field static final Map<Integer, String> AirQualityClusterAttributes = [
    0x0000  : 'AirQuality',         // AirQualityEnum, R V, M
    0xFFF8  : 'GeneratedCommandList',// list, R V, M
    0xFFF9  : 'AcceptedCommandList', // list, R V, M
    0xFFFB  : 'AttributeList',       // list, R V, M
    0xFFFC  : 'FeatureMap',          // FeatureMap, R V, M
    0xFFFD  : 'ClusterRevision'      // uint16, R V, M
]

// ============ Matter Concentration Measurement Cluster Attributes Map ============
// Carbon Dioxide (0x040D) and PM2.5 (0x042A) Concentration Measurement clusters
// Both share the same attribute structure per Matter spec
@Field static final Map<Integer, String> ConcentrationMeasurementClusterAttributes = [
    0x0000  : 'MeasuredValue',       // single (IEEE754), R V, M - Current concentration
    0x0001  : 'MinMeasuredValue',    // single (IEEE754), R V, M - Minimum measurable value
    0x0002  : 'MaxMeasuredValue',    // single (IEEE754), R V, M - Maximum measurable value
    0x0007  : 'Uncertainty',         // single (IEEE754), R V, O - Measurement uncertainty
    0x0008  : 'MeasurementUnit',     // MeasurementUnitEnum, R V, M - Unit of measurement
    0x0009  : 'MeasurementMedium',   // MeasurementMediumEnum, R V, O - Medium being measured
    0x000A  : 'LevelValue',          // LevelValueEnum, R V, M - Concentration level
    0xFFF8  : 'GeneratedCommandList',// list, R V, M
    0xFFF9  : 'AcceptedCommandList', // list, R V, M
    0xFFFB  : 'AttributeList',       // list, R V, M
    0xFFFC  : 'FeatureMap',          // FeatureMap, R V, M
    0xFFFD  : 'ClusterRevision'      // uint16, R V, M
]

// 2.9.5.1. AirQualityEnum Type
@Field static final Map<Integer, String> AirQualityEnum = [
    0x00    : 'Unknown',        // The air quality is unknown M
    0x01    : 'Good',           // The air quality is good M
    0x02    : 'Fair',           // The air quality is fair FAIR
    0x03    : 'Moderate',       // The air quality is moderate MOD
    0x04    : 'Poor',           // The air quality is poor POOR
    0x05    : 'VeryPoor',       // The air quality is very poor VPOOR
    0x06    : 'ExtremelyPoor'   // The air quality is extremely poor XPOOR
]


@Field static final String DRIVER = 'Matter Advanced Bridge'
@Field static final String COMPONENT = 'Matter Generic Component Air Purifier'
@Field static final String WIKI   = 'Get help on GitHub Wiki page:'
@Field static final String COMM_LINK =   "https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252/1"
@Field static final String GITHUB_LINK = "https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki/Matter-Advanced-Bridge-%E2%80%90-Air-Purifier"
// credits @jtp10181
String fmtHelpInfo(String str) {
	String info = "${DRIVER} v${parent?.version()}<br> ${COMPONENT} v${matterComponentAirPurifierVersion}"
	String prefLink = "<a href='${GITHUB_LINK}' target='_blank'>${WIKI}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid green; border-radius: 6px; color: green;'"
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
