/* groovylint-disable CompileStatic, DuplicateNumberLiteral, DuplicateStringLiteral, LineLength */
library(
    base: 'driver',
    author: 'Krassimir Kossev',
    category: 'matter',
    description: 'Matter Health Status Library - Device Ping and RTT Monitoring',
    name: 'matterHealthStatusLib',
    namespace: 'kkossev',
    importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Libraries/matterHealthStatusLib.groovy',
    version: '1.0.0',
    documentationLink: ''
)
/*
  *  Matter Health Status Library
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
  * ver. 1.0.0  2025-01-10 kkossev - initial release
  *                                  - ping command and RTT attribute
  *                                  - parseRttEvent() method for child drivers
  *                                  - foundation for health monitoring features
*/

import groovy.transform.Field

@Field static final String matterHealthStatusLibVersion = '1.0.0'
@Field static final String matterHealthStatusLibStamp   = '2025/01/10 2:02 PM'
@Field static final int ROLLING_AVERAGE_N = 10

metadata {
    command 'ping'
    
    attribute 'rtt', 'NUMBER'  // Round-trip time in milliseconds
    
    // Future enhancements:
    // capability 'HealthCheck'
    // attribute 'healthStatus', 'ENUM', ['online', 'offline', 'unknown']
}

preferences {
    // Future enhancements:
    // input 'pingInterval', 'number', title: 'Auto-ping interval (minutes)', defaultValue: 0
    // input 'rttTimeout', 'number', title: 'Ping timeout (seconds)', defaultValue: 10
}

/**
 * Ping the device to measure round-trip time
 * Delegates to parent bridge to send Matter read request
 */
void ping() {
    if (settings?.logEnable) { log.debug "${device.displayName} ping: sending ping request to device..." }
    parent?.componentPing(device)
}

/**
 * Parse RTT event from parent bridge
 * Called from child driver's parse() method when d.name == 'rtt'
 * 
 * @param d Map containing event data [name: 'rtt', value: <milliseconds>, ...]
 * @return boolean true if event was handled, false otherwise
 */
boolean parseRttEvent(Map d) {
    if (d?.name != 'rtt') {
        return false
    }
    
    Integer rttValue = d.value as Integer
    
    if (rttValue < 0) {
        // Timeout condition
        if (settings?.logEnable) { log.warn "${device.displayName} Ping failed - no response from device" }
    } else {
        // Success - update statistics
        if (state.stats == null) { state.stats = [:] }
        
        state.stats['pingsOK'] = (state.stats['pingsOK'] ?: 0) + 1
        
        // Update min/max/average
        if (rttValue < safeToInt((state.stats['pingsMin'] ?: '999'))) { 
            state.stats['pingsMin'] = rttValue 
        }
        if (rttValue > safeToInt((state.stats['pingsMax'] ?: '0'))) { 
            state.stats['pingsMax'] = rttValue 
        }
        state.stats['pingsAvg'] = approxRollingAverage(safeToDouble(state.stats['pingsAvg']), safeToDouble(rttValue)) as int
        
        // Log with statistics
        String descriptionText = "${device.displayName} Round-trip time is ${rttValue} ms (min=${state.stats['pingsMin']} max=${state.stats['pingsMax']} average=${state.stats['pingsAvg']} (HE uptime: ${formatUptime()})"
        if (settings?.txtEnable) { log.info "${descriptionText}" }
        
        // Update description text in the event
        d.descriptionText = descriptionText
        d.unit = 'ms'
    }
    
    // Always send the RTT event
    sendEvent(d)
    
    return true
}

/**
 * Calculate rolling average
 * @param avg Current average
 * @param newSample New sample value
 * @return Updated rolling average
 */
private double approxRollingAverage(double avg, double newSample) {
    if (avg == null || avg == 0) { return newSample }
    return (avg * (ROLLING_AVERAGE_N - 1) + newSample) / ROLLING_AVERAGE_N
}

/**
 * Format hub uptime for display
 * Credits: @thebearmay
 * @return Formatted uptime string (e.g., "1d, 7h, 8m, 13s")
 */
private String formatUptime() {
    try {
        Long ut = location.hub.uptime.toLong()
        Integer days = Math.floor(ut/(3600*24)).toInteger()
        Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
        Integer min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
        Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()
        return "${days}d, ${hrs}h, ${min}m, ${sec}s"
    } catch (Exception e) {
        return 'unknown'
    }
}

/**
 * Safe conversion to Integer with default value
 */
private Integer safeToInt(val, Integer defaultVal=0) {
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
private Double safeToDouble(val, Double defaultVal=0.0) {
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
