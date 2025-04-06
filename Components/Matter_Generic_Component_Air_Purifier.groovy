/* groovylint-disable BitwiseOperatorInConditional, CompileStatic, DuplicateStringLiteral, LineLength, PublicMethodsBeforeNonPublicMethods */
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
  * 
  *                                   TODO: add cluster 005B 'AirQuality' endpointId:"0C"
  *                                   TODO: add cluster 042A 'PM2.5ConcentrationMeasurement'  endpointId:"0C"
  *
  *                                   TODO: add cluster 0071 'HEPAFilterMonitoring' endpointId:"0B"
  *                                   TODO: add cluster 0202 'Window Covering' endpointId:"0B"
  *
*/

import groovy.transform.Field
import groovy.transform.CompileStatic

@Field static final String matterComponentAirPurifierVersion = '1.0.0'
@Field static final String matterComponentAirPurifierStamp   = '2024/10/10 11:31 PM'

@Field static final Boolean _DEBUG_AIR_PURIFIER = true

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

        command    'identify'
        command    'refreshAll'
        // Commands for devices.Ikea_E2006
        command 'setSpeed', [[name:'Fan speed*', type:'ENUM', description:'Select the desired fan speed', constraints:SUPPORTED_FAN_SPEEDS]]
        command 'toggle'
        command 'setIndicatorStatus', [[name:'Status*', type:'ENUM', description:'Select LED indicators status on the device', constraints:['on', 'off']]]
        
        attribute 'unprocessed', 'string'
        // Attributes for devices.Ikea_E2006
        attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
        attribute 'filterUsage', 'number'
        attribute 'pm25', 'number'
        attribute 'auto', 'enum', ['on', 'off']
        attribute 'indicatorStatus', 'enum', ['on', 'off']
        
        // Attributes for capability.HealthCheck
        attribute 'healthStatus', 'enum', ['offline', 'online', 'unknown']

        if (_DEBUG_LOCK) {
            command 'getInfo', [
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


/* groovylint-disable-next-line UnusedMethodParameter */
void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${description}" }
    description.each { d ->
        /*
        if (d.name == 'lock') {
            if (device.currentValue('lock') != d.value) {
                if (d.descriptionText && txtEnable) { log.info "${d.descriptionText} (value changed)" }
                sendEvent(d)
            }
            else {
                if (logEnable) { log.debug "${device.displayName} : ignored lock event '${d.value}' (no change)" }
            }
        }
        */
        /*else*/ if (d.name == 'unprocessed') {
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

// Component command to lock device
void lock() {
    if (settings?.onOffSwitch) {
        on()
    }
    else {
        if (logEnable) { log.debug "${device.displayName} locking ..." }
        parent?.componentLock(device)
    }
}

// Component command to unlock device
void unlock() {
    if (settings?.onOffSwitch) {
        off()
    }
    else {
        if (logEnable) { log.debug "${device.displayName} unlocking ..." }
        parent?.componentUnlock(device)
    }
}

void on() {
    if (logEnable) { log.debug "${device.displayName} turning on ..." }
    sendEvent(name: 'switch', value: 'on', type: 'digital')
    //parent?.componentOn(device)   // n/a
}

void off() {
    if (logEnable) { log.debug "${device.displayName} turning off ..." }
    sendEvent(name: 'switch', value: 'off', type: 'digital')
    sendEvent(name:'auto', value:'disabled', descriptionText:'Auto mode is disabled', type:'digital')
    sendEvent(name:'speed', value:'off', descriptionText:'Fan speed is off', type:'digital')
    //parent?.componentOff(device)  // n/a
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

// Component command to ping the device
void ping() {
    parent?.componentPing(device)
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
    /*
    state.warning = 'WARNING! Matter Locks lock and unlock commands are not supported by Hubitat. This driver is a placeholder for future compatibility.'
    log.warn "${device.displayName} ${state.warning}"
    state.working = 'What is working: lock status, battery level, refresh, identify'
    log.info "${device.displayName} ${state.working}"
    */
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
    // TODO!
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

void getInfo(String infoType, String endpoint) {
    parent?.componentGetInfo(device, infoType, endpoint)
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
        case '005B_0000': // attribute 'airQuality', 'enum', ['good', 'moderate', 'unhealthy for sensitive groups', 'unhealthy', 'hazardous']
            eventValue= AirQualityEnum[Integer.parseInt(descMap.value, 16)]
            descriptionText = "${device.displayName} AirQuality: ${eventValue} (raw:${descMap.value})"
            sendEvent(name: 'airQuality', value: eventValue, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        case '042A_0000': // attribute 'pm25', 'number'
            Integer pm25 = Integer.parseInt(descMap.value, 16) / 100000000
            log.trace "${device.displayName} PM2.5: ${pm25} μg/m³"
            descriptionText = "${device.displayName} PM2.5: ${pm25} μg/m³"
            sendEvent(name: 'pm25', value: pm25, descriptionText: descriptionText)
            if (txtEnable) { log.info "${descriptionText}" }
            break
        default:
            if (logEnable) { log.warn "processUnprocessed: unexpected attrId:${descMap.attrId}" }
    }
}

void refreshAll() {
    if (logEnable) { log.debug "${device.displayName} refreshAll:" }
    String id = (Integer.parseInt(device.getDataValue('id'), 16)).toString()
    parent?.readAttribute([id, '91', '0'])    // AirQuality    (Mandatory)
    parent?.readAttribute([id, '1066', '0'])  // PM2.5   (Mandatory)
}


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


