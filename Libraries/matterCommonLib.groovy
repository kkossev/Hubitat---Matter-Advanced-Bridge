library(
    base: 'driver',
    author: 'Krassimir Kossev',
    category: 'matter',
    description: 'Matter Common Library',
    name: 'matterCommonLib',
    namespace: 'kkossev',
    importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Libraries/matterCommonLib.groovy',
    version: '1.0.0',
    documentationLink: ''
)
/*
  *  Matter Common Library
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
  * ver. 1.0.0  2026-01-24 kkossev  - first release
*/

import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.transform.CompileStatic

@Field static final String matterCommonLibVersion = '1.0.0'
@Field static final String matterCommonLibStamp   = '2026/01/24 12:37 AM'
metadata {
    // no capabilities
    // no attributes
    // no preferences
}

// Common methods can be added here for reuse in multiple drivers

static Integer safeNumberToInt(val, Integer defaultVal=0) {
    try {
        String stringVal = val == null ? null : val.toString()
        if (stringVal == null) { return defaultVal }
        return stringVal.startsWith('0x') ? safeHexToInt(stringVal.substring(2)) : safeToInt(stringVal)
    } catch (NumberFormatException e) {
        return defaultVal
    }
}

static Integer safeHexToInt(val, Integer defaultVal=0) {
    if (val == null) return defaultVal
    if (val instanceof Integer) return val
    if (val instanceof String) {
        try {
            return HexUtils.hexStringToInt(val)
        } catch (NumberFormatException e) {
            return defaultVal
        }
    }
    return defaultVal
}


/**
 * Safe conversion to Integer with default value
 */
static Integer safeToInt(Object val, Integer defaultVal=0) {
    if (val == null) { return defaultVal }
    if (val instanceof Integer) { return val }
    if (val instanceof String) {
        try { return val.toInteger() }
        catch (Exception e) { return defaultVal }
    }
    try { return val as Integer }
    catch (Exception e) { return defaultVal }
}

/**
 * Safe conversion to Double with default value
 */
static Double safeToDouble(Object val, Double defaultVal=0.0) {
    if (val == null) { return defaultVal }
    if (val instanceof Double) { return val }
    if (val instanceof Integer) { return val.toDouble() }
    if (val instanceof String) {
        try { return val.toDouble() }
        catch (Exception e) { return defaultVal }
    }
    try { return val as Double }
    catch (Exception e) { return defaultVal }
}

/**
 * Get the device number (endpoint) from device data
 * @return device number as Integer or null if invalid
 */
private Integer getDeviceNumber() {
    String id = device.getDataValue('id')
    if (!id) {
        logWarn "Device ID not found in data values"
        return null
    }
    
    Integer deviceNumber = HexUtils.hexStringToInt(id)
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) {
        logWarn "Invalid device number: ${deviceNumber} (from id: ${id})"
        return null
    }
    
    return deviceNumber
}

// for use by parent driver
void setState(String stateName, String stateValue) {
    logDebug "setting state '${stateName}' to '${stateValue}'"
    state[stateName] = stateValue
}

// for use by parent driver
String getState(String stateName) {
    logDebug "getting state '${stateName}'"
    return state[stateName]
}


// ============ Helper Methods for Fingerprint Data ============


/**
 * Get the complete fingerprint data stored in device data
 * @return Map containing fingerprint data or null if not found
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
 * @return List of cluster IDs as hex strings (e.g., ["03", "1D", "2F", "0102"])
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


void logInfo(msg)  { if (settings.txtEnable)   { log.info  "${device.displayName} " + msg } }
void logError(msg) { if (settings.txtEnable)   { log.error "${device.displayName} " + msg } }
void logDebug(msg) { if (settings.logEnable)   { log.debug "${device.displayName} " + msg } }
void logWarn(msg)  { if (settings.logEnable)   { log.warn  "${device.displayName} " + msg } }
void logTrace(msg) { if (settings.traceEnable) { log.trace "${device.displayName} " + msg } }


