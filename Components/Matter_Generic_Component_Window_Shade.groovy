/*
 *  ''Matter Generic Component Window Shade' - component driver for Matter Advanced Bridge
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
 * This library is inspired by @w35l3y work on Tuya device driver (Edge project).
 * For a big portions of code all credits go to Jonathan Bradshaw.
 *
 *
 * ver. 1.0.0  2024-03-16 kkossev - first release
 * ver. 1.1.0  2025-01-12 kkossev - (dev.branch) added capabilities 'Switch' and 'SwitchLevel'
 * ver. 1.2.0  2025-01-10 kkossev - added ping command and RTT monitoring via matterHealthStatusLib
 * ver. 1.2.1  2025-01-10 kkossev - bugfix: changed OPEN/CLOSED to Hubitat standard (100/0) for correct Dashboard display; invertPosition preference default to true
 * ver. 1.2.2  2025-01-29 kkossev - common libraries
 * ver. 1.2.3  2026-02-11 kkossev - (dev. branch) getInfo()
 * ver. 1.2.4  2026-02-19 kkossev - (dev. branch) moved common methods to matterCommonLib
 *
 *                                   TODO:
*/

import groovy.transform.Field

@Field static final String matterComponentWindowShadeVersion = '1.2.4'
@Field static final String matterComponentWindowShadeStamp   = '2026/02/19 4:49 PM'

@Field static final Boolean _DEBUG = false

@Field static final Integer OPEN   = 100    // Hubitat standard: Open = 100%
@Field static final Integer CLOSED = 0      // Hubitat standard: Closed = 0%
@Field static final Integer POSITION_DELTA = 5
@Field static final Integer MAX_TRAVEL_TIME = 15
@Field static final Boolean SIMULATE_LEVEL = true

metadata {
    definition(name: 'Matter Generic Component Window Shade', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Window_Shade.groovy', singleThreaded: true) {
        capability 'Actuator'
        capability 'WindowShade'    // Attributes: position - NUMBER, unit:% windowShade - ENUM ["opening", "partially open", "closed", "open", "closing", "unknown"]
                                    // Commands: close(); open(); setPosition(position) position required (NUMBER) - Shade position (0 to 100);
                                    //           startPositionChange(direction): direction required (ENUM) - Direction for position change request ["open", "close"]
                                    //            stopPositionChange()
        capability 'Refresh'
        capability 'Battery'
        capability 'Switch'
        capability 'SwitchLevel'        
        attribute 'targetPosition', 'number'            // ZemiSmart M1 is updating this attribute, not the position :(
        attribute 'operationalStatus', 'number'         // 'enum', ['unknown', 'open', 'closed', 'opening', 'closing', 'partially open']

        attribute 'batteryVoltage', 'number'
        attribute 'batStatus', 'string'             // Aqara E1 blinds
        attribute 'batOrder', 'string'              // Aqara E1 blinds
        attribute 'batDescription', 'string'        // Aqara E1 blinds
        attribute 'batTimeRemaining', 'string'
        attribute 'batChargeLevel', 'string'            // Aqara E1 blinds
        attribute 'batReplacementNeeded', 'string'      // Aqara E1 blinds
        attribute 'batReplaceability', 'string'
        attribute 'batReplacementDescription', 'string'
        attribute 'batQuantity', 'string'

        command   'initialize', [[name: 'initialize all attributes']]
        command   'getInfo', [[name: 'Check the live logs and the device data for additional infoormation on this device']]

        if (_DEBUG) {
            command 'parseTest', [[name: 'parseTest', type: 'STRING', description: 'parseTest', defaultValue : '']]
        }        
    }
}

preferences {
    section {
	    input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
        input name: 'txtEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', required: false, defaultValue: true
        input name: 'logEnable', type: 'bool', title: '<b>Enable debug logging</b>',           required: false, defaultValue: false
        input name: 'maxTravelTime', type: 'number', title: '<b>Maximum travel time</b>', description: '<i>The maximum time to fully open or close (Seconds)</i>', required: false, defaultValue: MAX_TRAVEL_TIME
        input name: 'deltaPosition', type: 'number', title: '<b>Position delta</b>', description: '<i>The maximum error step reaching the target position</i>', required: false, defaultValue: POSITION_DELTA
        input name: 'substituteOpenClose', type: 'bool', title: '<b>Substitute Open/Close w/ setPosition</b>', description: '<i>Non-standard Zemismart motors</i>', required: false, defaultValue: false
        input name: 'invertPosition', type: 'bool', title: '<b>Reverse Position Reports</b>', description: '<i>Non-standard Zemismart motors</i>', required: false, defaultValue: true
        input name: 'targetAsCurrentPosition', type: 'bool', title: '<b>Reverse Target and Current Position</b>', description: '<i>Non-standard Zemismart motors</i>', required: false, defaultValue: false
    }
}

int getDelta() { return settings?.deltaPosition != null ? settings?.deltaPosition as int : POSITION_DELTA }
int getFullyOpen()   { return  OPEN }
int getFullyClosed() { return CLOSED }
boolean isFullyOpen(int position)   { return Math.abs(position - getFullyOpen()) < getDelta() }
boolean isFullyClosed(int position) { return Math.abs(position - getFullyClosed()) < getDelta() }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "parse: ${description}" }
    description.each { d ->
        if (d?.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
        }
        else if (d?.name == 'position') {
            processCurrentPositionBridgeEvent(d)
        }
        else if (d?.name == 'targetPosition') {
            processTargetPositionBridgeEvent(d)
        }
        else if (d?.name == 'operationalStatus') {
            processOperationalStatusBridgeEvent(d)
        }
        else if (d.name in  ['unprocessed', 'handleInChildDriver']) {
            handleUnprocessedMessageInChildDriver(d)
        }
        else {
            if (d?.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
            log.trace "parse: ${d}"
            sendEvent(d)
        }
    }
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
    if (descMap.cluster != '0102') { logWarn "handleUnprocessedMessageInChildDriver: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }

    // Check if this is an event (has evtId) or an attribute (has attrId)
    if (descMap.evtId != null) {
        //processWindowCoveringEvent(descMap)
        logDebug "handleUnprocessedMessageInChildDriver: TODO: received event report - evtId: ${descMap.evtId}, value: ${descMap.value}"
        return
    }
    // else - process attribute report
    processWindowCoveringAttributeReport(descMap)
}

// 5.3.3 Window Covering Cluster 0x0102 (258)
@Field static final Map<Integer, String> WindowCoveringClusterAttributes = [
    0x0000  : 'Type',                           // Tuya - 00
    0x0001  : 'PhysicalClosedLimitLift',
    0x0002  : 'PhysicalClosedLimitTilt',
    0x0003  : 'CurrentPositionLift',            // Tuya - 00
    0x0004  : 'CurrentPositionTilt',
    0x0005  : 'NumberOfActuationsLift',
    0x0006  : 'NumberOfActuationsTilt',
    0x0007  : 'ConfigStatus',                   // Tuya - 04
    0x0008  : 'CurrentPositionLiftPercentage',  // Tuya - 00
    0x0009  : 'CurrentPositionTiltPercentage',
    0x000A  : 'OperationalStatus',              // Tuya - 00
    0x000B  : 'TargetPositionLiftPercent100ths',    // Tuya - 1170 (must be subtracted from 100 ?)
    0x000C  : 'TargetPositionTiltPercent100ths',
    0x000D  : 'EndProductType',                 // Tuya - 00
    0x000E  : 'CurrentPositionLiftPercent100ths',   // Tuya - 1A2C (must be subtracted from 100 ?)
    0x000F  : 'CurrentPositionTiltPercent100ths',
    0x0010  : 'InstalledOpenLimitLift',         // Tuya - 00
    0x0011  : 'InstalledClosedLimitLift',       // Tuya - FFFF
    0x0012  : 'InstalledOpenLimitTilt',
    0x0013  : 'InstalledClosedLimitTilt',
    0x0014  : 'VelocityLift',
    0x0015  : 'AccelerationTimeLift',
    0x0016  : 'DecelerationTimeLift',
    0x0017  : 'Mode',                           // Tuya - 00
    0x0018  : 'IntermediateSetpointsLift',
    0x0019  : 'IntermediateSetpointsTilt',
    0x001A  : 'SafetyStatus'
]

@Field static final Map<Integer, String> WindowCoveringType = [
    0x00 : 'Roller Shade',
    0x01 : 'Motorized Drapery',
    0x02 : 'Awning',
    0x03 : 'Shutter',
    0x04 : 'Tilt Blind',
    0x05 : 'Projector Screen',
    0x06 : 'Other'
]

@Field static final Map<Integer, String> WindowCoveringConfigStatus = [
    0x00 : 'No Errors',
    0x01 : 'Open Limit Error',
    0x02 : 'Closed Limit Error',
    0x03 : 'Open and Closed Limit Error',
    0x04 : 'Position Feedback Error',
    0x05 : 'Motor Error',
    0x06 : 'Configuration Error',
    0x07 : 'Unknown Error'
]

@Field static final Map<Integer, String> WindowCoveringOperationalStatus = [
    0x00 : 'Stopped',
    0x01 : 'Opening',
    0x02 : 'Closing',
    0x03 : 'Partially Open',
    0x04 : 'Unknown'
]

@Field static final Map<Integer, String> WindowCoveringMode = [
    0x00 : 'Normal',
    0x01 : 'Inverted'
]



void processWindowCoveringAttributeReport(Map descMap) {
    
    String eventValue = descMap.value
    String descriptionText = "${device.displayName} ${descMap.cluster}:${descMap.attrId} value:${eventValue}"
    
    // Declare variables for conditional logging
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${descMap.attrId}] " : ""
    String message = null
    boolean useDebugLog = false  // Flag to use logDebug instead of logInfo when isInfoMode is false
   
    switch (descMap.attrId) {
        case '0000': // Type
            String typeText = WindowCoveringType[safeHexToInt(descMap.value)] ?: "Unknown (${descMap.value})"
            message = "${prefix}Type: ${typeText} (raw:${descMap.value})"
            useDebugLog = true
            break
        case '0003': // CurrentPositionLift
            message = "${prefix}CurrentPositionLift: ${descMap.value}"
            break
        case '0004': // CurrentPositionTilt
            message = "${prefix}CurrentPositionTilt: ${descMap.value}"
            break
        case '0005': // NumberOfActuationsLift
            message = "${prefix}NumberOfActuationsLift: ${descMap.value}"
            break
        case '0007': // ConfigStatus
            String configStatusText = WindowCoveringConfigStatus[safeHexToInt(descMap.value)] ?: "Unknown (${descMap.value})"
            message = "${prefix}ConfigStatus: ${configStatusText} (raw:${descMap.value})"
            useDebugLog = true
            break
        case '0008': // CurrentPositionLiftPercentage
            message = "${prefix}CurrentPositionLiftPercentage: ${descMap.value}%"
            break
        case '0009': // CurrentPositionTiltPercentage
            message = "${prefix}CurrentPositionTiltPercentage: ${descMap.value}%"
            break
        case '000A': // OperationalStatus
            String operationalStatusText = WindowCoveringOperationalStatus[safeHexToInt(descMap.value)] ?: "Unknown (${descMap.value})"
            message = "${prefix}OperationalStatus: ${operationalStatusText} (raw:${descMap.value})"
            break
        case '000B': // TargetPositionLiftPercent100ths
            message = "${prefix}TargetPositionLiftPercent100ths: ${descMap.value} (scaled:${safeToInt(descMap.value) / 100}%)"
            break
        case '000C': // TargetPositionTiltPercent100ths
            message = "${prefix}TargetPositionTiltPercent100ths: ${descMap.value} (scaled:${safeToInt(descMap.value) / 100}%)"
            break
        case '000D': // EndProductType
            message = "${prefix}EndProductType: ${descMap.value}"
            break
        case '000E': // CurrentPositionLiftPercent100ths
            message = "${prefix}CurrentPositionLiftPercent100ths: ${descMap.value} (scaled:${safeToInt(descMap.value) / 100}%)"
            break
        case '0010': // InstalledOpenLimitLift
            message = "${prefix}InstalledOpenLimitLift: ${descMap.value}"
            break
        case '0011': // InstalledClosedLimitLift
            message = "${prefix}InstalledClosedLimitLift: ${descMap.value}"
            break
        case '0017': // Mode
            String modeText = WindowCoveringMode[safeHexToInt(descMap.value)] ?: "Unknown (${descMap.value})"
            message = "${prefix}Mode: ${modeText} (raw:${descMap.value})"
            useDebugLog = true
            break

        case 'FFFC': // FeatureMap
            Integer featureMap = safeHexToInt(descMap.value)
            String featuresText = decodeFeatureMap(featureMap)
            // FeatureMapRaw is stored in fingerprintData as '0102_FFFC'
            message = "${prefix}FeatureMap: ${featuresText} (0x${descMap.value})"
            break
        case 'FFFB': // AttributeList
            // AttributeList is stored in fingerprintData, no need to store separately
            message = "${prefix}AttributeList: ${descMap.value}"
            useDebugLog = true
            // device.updateDataValue('AttributeList', ...) - removed, now in fingerprintData
            break
        case 'FFFD': // ClusterRevision
            Integer revision = safeHexToInt(descMap.value)
            message = "${prefix}ClusterRevision: ${revision}"
            break
        case 'FFF8': // GeneratedCommandList (events supported) - stored in fingerprintData only
            message = "${prefix}GeneratedCommandList: ${descMap.value}"
            useDebugLog = true
            // Data is in fingerprintData['0102_FFF8'], no duplicate storage needed
            break
        case 'FFF9': // AcceptedCommandList - stored in fingerprintData only
            message = "${prefix}AcceptedCommandList: ${descMap.value}"
            useDebugLog = true
            // Data is in fingerprintData['0102_FFF9'], no duplicate storage needed
            break



        default:
            message = "${prefix}Unhandled attribute ${descMap.attrId}: ${descMap.value}"
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

String decodeFeatureMap(Integer featureMap) {
    List<String> features = []
    if ((featureMap & 0x00000001) != 0) { features << 'Lift' }
    if ((featureMap & 0x00000002) != 0) { features << 'Tilt' }
    if ((featureMap & 0x00000004) != 0) { features << 'LiftAndTilt' }
    if ((featureMap & 0x00000008) != 0) { features << 'PositionAwareLift' }
    if ((featureMap & 0x00000010) != 0) { features << 'PositionAwareTilt' }
    if ((featureMap & 0x00000020) != 0) { features << 'PositionAwareLiftAndTilt' }
    if ((featureMap & 0x00000040) != 0) { features << 'RemoteControlLift' }
    if ((featureMap & 0x00000080) != 0) { features << 'RemoteControlTilt' }
    if ((featureMap & 0x00000100) != 0) { features << 'RemoteControlLiftAndTilt' }
    if ((featureMap & 0x00000200) != 0) { features << 'RemoteControlPositionAwareLift' }
    if ((featureMap & 0x00000400) != 0) { features << 'RemoteControlPositionAwareTilt' }
    if ((featureMap & 0x00000800) != 0) { features << 'RemoteControlPositionAwareLiftAndTilt' }
    return features.isEmpty() ? "None (raw: ${String.format('0x%08X', featureMap)})" : features.join(', ')
}


int invertPositionIfNeeded(int position) {
    int value =  (settings?.invertPosition ?: false) ? (100 - position) as Integer : position
    if (value < 0)   { value = 0 }
    if (value > 100) { value = 100 }
    return value
}

void processCurrentPositionBridgeEvent(final Map d) {
    Map map = new HashMap(d)
    //stopOperationTimeoutTimer()
    if (settings?.targetAsCurrentPosition == true) {
        map.name = 'targetPosition'
        if (logEnable) { log.debug "${device.displayName} processCurrentPositionBridgeEvent: targetAsCurrentPosition is true -> <b>processing as targetPosition ${map.value} !</b>" }
        processTargetPosition(map)
    }
    else {
        if (logEnable) { log.debug "${device.displayName} processCurrentPositionBridgeEvent: currentPosition reported is ${map.value}" }
        processCurrentPosition(map)
    }
}

void processCurrentPosition(final Map d) {
    Map map = new HashMap(d)
    stopOperationTimeoutTimer()
    // we may have the currentPosition reported inverted !
    map.value = invertPositionIfNeeded(d.value as int)
    if (logEnable) { log.debug "${device.displayName} processCurrentPosition: ${map.value} (was ${d.value})" }
    map.name = 'position'
    map.unit = '%'
    map.descriptionText = "${device.displayName} position is ${map.value}%"
    if (map.isRefresh) {
        map.descriptionText += ' [refresh]'
    }
    if (txtEnable) { log.info "${map.descriptionText}" }
    sendEvent(map)
    if (SIMULATE_LEVEL == true) {
        sendEvent(name: 'level', value: map.value, descriptionText: "${device.displayName} level is ${map.value}%", unit: '%', type: 'digital')
    }
    updateWindowShadeStatus(map.value as int, device.currentValue('targetPosition') as int, /*isFinal =*/ true, /*isDigital =*/ false)
}

void updateWindowShadeStatus(int currentPositionPar, int targetPositionPar, Boolean isFinal, Boolean isDigital) {
    String value = 'unknown'
    String descriptionText = 'unknown'
    String type = isDigital ? 'digital' : 'physical'
    //log.trace "updateWindowShadeStatus: currentPositionPar = ${currentPositionPar}, targetPositionPar = ${targetPositionPar}"
    Integer currentPosition = currentPositionPar as int
    Integer targetPosition = targetPositionPar as int

    if (isFinal == true) {
        if (isFullyClosed(currentPosition)) {
            value = 'closed'
        }
        else if (isFullyOpen(currentPosition)) {
            value = 'open'
        }
        else {
            value = 'partially open'
        }
    }
    else {
        if (targetPosition < currentPosition) {
            //value =  'opening'
            value =  'closing'  // changed 2026-02-11
        }
        else if (targetPosition > currentPosition) {
            //value = 'closing'
            value = 'opening'   // changed 2026-02-11
        }
        else {
            //value = 'stopping'
            if (isFullyClosed(currentPosition)) {
                value = 'closed'
            }
            else if (isFullyOpen(currentPosition)) {
                value = 'open'
            }            
        }
    }
    descriptionText = "${device.displayName} windowShade is ${value} [${type}]"
    sendEvent(name: 'windowShade', value: value, descriptionText: descriptionText, type: type)
    if (logEnable) { log.debug "${device.displayName} updateWindowShadeStatus: isFinal: ${isFinal}, substituteOpenClose: ${settings?.substituteOpenClose}, targetPosition: ${targetPosition}, currentPosition: ${currentPosition}, windowShade: ${device.currentValue('windowShade')}" }
    if (txtEnable) { log.info "${descriptionText}" }
    if (SIMULATE_LEVEL == true) {
        sendEvent(name: 'switch', value: value == 'open' ? 'on' : 'off', descriptionText: "${device.displayName} switch is ${value == 'open' ? 'on' : 'off'}", type: 'digital')
    }
}

void sendWindowShadeEvent(String value, String descriptionText) {
    sendEvent(name: 'windowShade', value: value, descriptionText: descriptionText)
    if (txtEnable) { log.info "${device.displayName} windowShade is ${value}" }
    if (SIMULATE_LEVEL == true) {
        sendEvent(name: 'switch', value: value == 'open' ? 'on' : 'off', descriptionText: "${device.displayName} switch is ${value == 'open' ? 'on' : 'off'}", type: 'digital')
    }
}

void processTargetPositionBridgeEvent(final Map d) {
    Map map = new HashMap(d)
    stopOperationTimeoutTimer()
    if (logEnable) { log.debug "${device.displayName} processTargetPositionBridgeEvent: ${d}" }
    if (settings?.targetAsCurrentPosition) {
        if (logEnable) { log.debug "${device.displayName} processTargetPositionBridgeEvent: targetAsCurrentPosition is true" }
        map.name = 'position'
        processCurrentPosition(map)
        return
    }
    processTargetPosition(map)
}

void processTargetPosition(final Map d) {
    //log.trace "processTargetPosition: value: ${d.value}"
    Map map = new HashMap(d)
    map.value = invertPositionIfNeeded(safeToInt(d.value))
    map.descriptionText = "${device.displayName} targetPosition is ${map.value}%"
    if (map.isRefresh) {
        map.descriptionText += ' [refresh]'
    }
    map.name = 'targetPosition'
    map.unit = '%'
    if (logEnable) { log.debug "${device.displayName} processTargetPosition: ${map.value} (was ${d.value})" }
    if (txtEnable) { log.info "${map.descriptionText}" }
    //
    sendEvent(map)
    if (!map.isRefresh) {
        // skip upddating the windowShade status on targetPosition refresh
        updateWindowShadeStatus(device.currentValue('position') as int, map.value as int, /*isFinal =*/ false, /*isDigital =*/ false)
    }
}

void processOperationalStatusBridgeEvent(Map d) {
    stopOperationTimeoutTimer()
    if (logEnable) { log.debug "${device.displayName} processOperationalStatusBridgeEvent: ${d}" }
    if (d.descriptionText && txtEnable) { log.info "${device.displayName} ${d.descriptionText}" }
    sendEvent(d)
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
    initialize()
}

void initialize() {
    sendEvent(name: 'position', value: 0, descriptionText: "${device.displayName} initializing position to 0%", unit: '%', type: 'digital')
    sendEvent(name: 'targetPosition', value: 0, descriptionText: "${device.displayName} initializing targetPosition to 0%", unit: '%', type: 'digital')
    sendEvent(name: 'windowShade', value: 'open', descriptionText: "${device.displayName} initializeing windowShade to open", type: 'digital')
    sendEvent(name: 'level', value: 0, descriptionText: "${device.displayName} initializing level to 0%", unit: '%', type: 'digital')
    sendEvent(name: 'switch', value: 'on', descriptionText: "${device.displayName} initializing switch to on", type: 'digital')
}

void on() { open() }
void off() { close() }  

// Component command to open device
void open() {
    if (txtEnable) { log.info "${device.displayName} opening" }
    sendEvent(name: 'targetPosition', value: OPEN, descriptionText: "targetPosition set to ${OPEN}", type: 'digital')
    if (settings?.substituteOpenClose == false) {
        parent?.componentOpen(device)
    }
    else {
        setPosition(getFullyOpen())
    }
    startOperationTimeoutTimer()
    sendWindowShadeEvent('opening', "${device.displayName} windowShade is opening")
}

// Component command to close device
void close() {
    if (logEnable) { log.debug "${device.displayName} closing [digital]" }
    sendEvent(name: 'targetPosition', value: CLOSED, descriptionText: "targetPosition set to ${CLOSED}", type: 'digital')
    if (settings?.substituteOpenClose == false) {
        if (logEnable) { log.debug "${device.displayName} sending componentClose() command to the parent" }
        parent?.componentClose(device)
    }
    else {
        if (logEnable) { log.debug "${device.displayName} sending componentSetPosition(${getFullyClosed()}) command to the parent" }
        setPosition(getFullyClosed())
    }
    startOperationTimeoutTimer()
    sendWindowShadeEvent('closing', "${device.displayName} windowShade is closing [digital]")
}

void setLevel(BigDecimal targetPosition) { setPosition(targetPosition) }

// Component command to set position of device
void setPosition(BigDecimal targetPosition) {
    if (txtEnable) { log.info "${device.displayName} setting target position ${targetPosition}% (current position is ${device.currentValue('position')})" }
    sendEvent(name: 'targetPosition', value: targetPosition as int, descriptionText: "targetPosition set to ${targetPosition}", type: 'digital')
    updateWindowShadeStatus(device.currentValue('position') as int, targetPosition as int, isFinal = false, isDigital = true)
    BigDecimal componentTargetPosition = invertPositionIfNeeded(targetPosition as int)
    if (logEnable) { log.debug "inverted componentTargetPosition: ${componentTargetPosition}" }
    parent?.componentSetPosition(device, componentTargetPosition)
    startOperationTimeoutTimer()
}

// Component command to start position change of device
void startPositionChange(String change) {
    if (logEnable) { log.debug "${device.displayName} startPositionChange ${change}" }
    if (change == 'open') {
        open()
    }
    else {
        close()
    }
}

// Component command to start position change of device
void stopPositionChange() {
    if (logEnable) { log.debug "${device.displayName} stopPositionChange" }
    parent?.componentStopPositionChange(device)
}

// Component command to refresh the device
void refresh() {
    if (txtEnable) { log.info "${device.displayName} refreshing ..." }
    state.standardOpenClose = 'OPEN = 0% CLOSED = 100%'
    state.driverVersion = matterComponentWindowShadeVersion + ' (' + matterComponentWindowShadeStamp + ')'
    parent?.componentRefresh(device)
}

// Called when the device is removed
void uninstalled() {
    log.info "${device.displayName} driver uninstalled"
}

// Called when the settings are updated
void updated() {
    if (txtEnable) { log.info "${device.displayName} driver configuration updated" }
    if (logEnable) {
        log.debug settings
        runIn(86400, 'logsOff')
    }
    if ((state.substituteOpenClose ?: false) != settings?.substituteOpenClose) {
        state.substituteOpenClose = settings?.substituteOpenClose
        if (logEnable) { log.debug "${device.displayName} substituteOpenClose: ${settings?.substituteOpenClose}" }
    }
    else {
        if (logEnable) { log.debug "${device.displayName} invertMotion: no change" }
    }
    //
    if ((state.invertPosition ?: false) != settings?.invertPosition) {
        state.invertPosition = settings?.invertPosition
        if (logEnable) { log.debug "${device.displayName} invertPosition: ${settings?.invertPosition}" }
    }
    else {
        if (logEnable) { log.debug "${device.displayName} invertPosition: no change" }
    }
}

BigDecimal scale(int value, int fromLow, int fromHigh, int toLow, int toHigh) {
    return  BigDecimal.valueOf(toHigh - toLow) *  BigDecimal.valueOf(value - fromLow) /  BigDecimal.valueOf(fromHigh - fromLow) + toLow
}

void startOperationTimeoutTimer() {
    int travelTime = Math.abs(device.currentValue('position') - device.currentValue('targetPosition'))
    Integer scaledTimerValue = scale(travelTime, 0, 100, 1, settings?.maxTravelTime as int) + 1.5
    if (logEnable) { log.debug "${device.displayName} startOperationTimeoutTimer: ${scaledTimerValue} seconds" }
    runIn(scaledTimerValue, 'operationTimeoutTimer', [overwrite: true])
}

void stopOperationTimeoutTimer() {
    if (logEnable) { log.debug "${device.displayName} stopOperationTimeoutTimer" }
    unschedule('operationTimeoutTimer')
}

void operationTimeoutTimer() {
    if (logEnable) { log.warn "${device.displayName} operationTimeout!" }
    updateWindowShadeStatus(device.currentValue('position') as int, device.currentValue('targetPosition') as int, /*isFinal =*/ true, /*isDigital =*/ true)
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}

// -------------




/**
 * Get the Window Covering cluster AttributeList (0x0102_FFFB)
 * @return List of attribute IDs as hex strings (e.g., ["00", "01", "02", ...])
 */
List<String> getWindowCoveringAttributeList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getWindowCoveringAttributeList: fingerprint data not available"
        return []
    }
    
    return fingerprint['0102_FFFB'] ?: []
}

/**
 * Check if a specific attribute is supported by the Window Covering cluster
 * @param attrHex Attribute ID as hex string (e.g., "00" for CurrentPositionLift)
 * @return true if attribute is in Window Covering AttributeList
 */
boolean isWindowCoveringAttributeSupported(String attrHex) {
    List<String> attrList = getWindowCoveringAttributeList()
    return attrList.contains(attrHex?.toUpperCase())
}



// Command to get all supported WindowCovering attributes (for info/debugging)
void getInfo() {
    // Check if WindowCovering cluster is supported
    if (!isClusterSupported('0102')) {
        logWarn "getInfo: WindowCovering cluster (0x0102) is not supported by this device"
        logInfo "getInfo: ServerList contains: ${getServerList()}"
        return
    }
    logInfo "getInfo: reading all supported WindowCovering attributes: ${getWindowCoveringAttributeList()}"
    
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
    parent?.readAttribute(endpoint, 0x0102, -1)      // 0x0102 WindowCovering cluster - read all attributes
    // battery info is processed in parent driver !
    // parent?.readAttribute(endpoint, 0x002F, -1)       // 0x002F Power Source cluster - read all attributes
}

// Clear info mode flag (called by scheduled job)
void clearInfoMode() {
    if (state.states == null) { state.states = [:] }
    state.states.isInfo = false
    device.updateSetting('logEnable', [value: state.states?.debugState ?: 'false', type: 'bool'] )  // restore debug logging to the value before info mode was enabled
    logDebug "clearInfoMode: info mode disabled"
}



@Field static final String DRIVER = 'Matter Advanced Bridge'
@Field static final String COMPONENT = 'Matter Generic Component Window Shade'
@Field static final String WIKI   = 'Get help on GitHub Wiki page:'
@Field static final String COMM_LINK =   "https://community.hubitat.com/t/project-nearing-beta-release-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009"
@Field static final String GITHUB_LINK = "https://github.com/kkossev/Hubitat/wiki/Matter-Advanced-Bridge-%E2%80%90-Window-Covering"
// credits @jtp10181
String fmtHelpInfo(String str) {
	String info = "${DRIVER} v${parent?.version()}<br> ${COMPONENT} v${matterComponentWindowShadeVersion}"
	String prefLink = "<a href='${GITHUB_LINK}' target='_blank'>${WIKI}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid green; border-radius: 6px; color: green;'"
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

void parseTest(description) {
    log.warn "parseTest: ${description}"
    //String str = "name:position, value:0, descriptionText:Bridge#4266 Device#32 (tuya CURTAIN) position is is reported as 0 (to be re-processed in the child driver!) [refresh], unit:null, type:physical, isStateChange:true, isRefresh:true"
    String str = description
    // Split the string into key-value pairs
    List<String> pairs = str.split(', ')
    Map map = [:]
    pairs.each { pair ->
        // Split each pair into a key and a value
        List<String> keyValue = pair.split(':')
        String key = keyValue[0]
        String value = keyValue[1..-1].join(':') // Join the rest of the elements in case the value contains colons
        // Try to convert the value to a boolean or integer if possible
        if (value == 'true' || value == 'false' || value == true || value == false) {
            value = Boolean.parseBoolean(value)
        } else if (value.isInteger()) {
            value = Integer.parseInt(value)
        } else if (value == 'null') {
            value = null
        }
        // Add the key-value pair to the map
        map[key] = value
    }
    log.debug map
    parse([map])
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib