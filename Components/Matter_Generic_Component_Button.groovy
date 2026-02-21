/*
  *  'Matter Generic Component Button' - component driver for Matter Advanced Bridge
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
  * ver. 1.0.0  2026-01-07 kkossev + GPT-5.2 : inital release
  * ver. 1.0.1  2026-01-10 kkossev - adding ping() and RTT
  * ver. 1.0.2  2026-01-11 kkossev + Claude Sonnet 4.5 - added 'generatePushedOn' preference for buttons that don't send multiPressComplete events
  * ver. 1.0.3  2026-01-25 kkossev + + GPT-5.2 : newParse=true fixes (evtId as Integer)
  * ver. 1.0.4  2026-01-29 kkossev  - common libraries
  * ver. 1.1.0  2026-02-21 kkossev  - (dev. branch) - added getInfo() command; code refactoring
  *
  *                                 TODO: featureMap in deviceFingerprintData is wrong!! 
*/

import groovy.transform.Field
import groovy.json.JsonSlurper

@Field static final String matterComponentButtonVersion = '1.1.0'
@Field static final String matterComponentButtonStamp   = '2026/02/21 9:30 AM'

@Field static final JsonSlurper jsonParser = new JsonSlurper()

metadata {
    definition(name: 'Matter Generic Component Button', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Button.groovy') {
        capability 'Refresh'
        capability 'PushableButton'
        capability 'HoldableButton'
        capability 'ReleasableButton'
        capability 'DoubleTapableButton'

        attribute 'numberOfButtons', 'number'
        attribute 'latched', 'number'   // TODO
        
        command   'getInfo', [[name: 'Check the live logs and the device data for additional information on this device']]
    }
}

preferences {
    section {
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging', required: false, defaultValue: true
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', required: false, defaultValue: true
    }
}

void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    //log.trace "parse(List<Map> description) called with: ${description}"
    logDebug "parse(List<Map> description) called with: ${description}"

    description.each { d ->
        switch (d.name) {
            case 'rtt':
                // Delegate to health status library
                parseRttEvent(d)
                break
            
            case 'handleInChildDriver':
                // All Switch cluster events and attributes are routed through handleUnprocessedMessageInChildDriver()
                handleUnprocessedMessageInChildDriver(d)
                break

            default:
                if (d.descriptionText) { logInfo "${d.descriptionText}" }
                sendEvent(d)
                break
        }
    }
}

// Handle unprocessed Matter cluster data forwarded from parent bridge
private void handleUnprocessedMessageInChildDriver(final Map d) {
    //logDebug "handleUnprocessedMessageInChildDriver  : ${d.value}"
    
    // Parse the descMap from JSON string
    Map descMap = d.value as Map
    if (descMap == null) {
        log.warn "${device.displayName} failed to parse unprocessed data: ${d.value}"
        return
    }

    // Handle Switch cluster (0x003B) attributes and events
    if (descMap.clusterInt == 0x003B) {
        // Handle events (evtId present)
        if (descMap.evtId != null || descMap.evtInt != null) {
            handleSwitchEvent(descMap)
        }
        // Handle attributes (attrId present)
        else if (descMap.attrId != null || descMap.attrInt != null) {
            handleSwitchAttribute(descMap)
        }
    }
    else {
        logDebug "unprocessed cluster ${descMap.clusterInt} not supported"
    }
}


// Handle Switch cluster events
private void handleSwitchEvent(Map descMap) {

    switch (descMap.evtId) {
        
        case 0x0000:    // SwitchLatched Event (Conformance: M)
            // This event SHALL be generated, when the latching switch is moved to a new position. It MAY have been delayed by debouncing within the switch.
            // data: ID:0 name: NewPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // This field SHALL indicate the new value of the CurrentPosition attribute, i.e. after the move.
            def value =  descMap.value[0]
            sendButtonEvent('latched', toButtonNumber(value, safeToInt(device.currentValue('numberOfButtons'))))
            logInfo "SwitchLatched event: button ${value} latched (currentPosition=${value})"
            break

        case 0x0001:    // InitialPress Event (Conformance: M)
            // This event SHALL be generated, when the momentary switch starts to be pressed (after debouncing).
            // data: ID:0 name: NewPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // This field SHALL indicate the new value of the CurrentPosition attribute, i.e. while pressed.
            //
            // [callbackType:Event, endpointInt:1, clusterInt:59, evtId:1, timestamp:221965458, timestampType:0, priority:1, eventSerial:458823, data:[1:STRUCT:[0:UINT:1]], value:[0:1], cluster:003B, endpoint:01]
            def value =  descMap.value[0]
            logDebug "InitialPress event ${descMap.evtId} ->  CurrentPosition new value: ${value}"
            state.lastEvent = 'initialPress'
            break
        case 0x0002:    // LongPress Event (Conformance: M)
            // This event SHALL be generated, when the momentary switch has been pressed for a "long" time (this time interval is manufacturer determined (e.g. since it depends on the switch physics)).
            // data: ID:0 name: NewPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // This field SHALL indicate the new value of the CurrentPosition attribute, i.e. while pressed.
            def value =  descMap.value[0]
            logDebug "LongPress event ${descMap.evtId} ->  CurrentPosition new value: ${value}"
            sendButtonEvent('held', toButtonNumber(value, safeToInt(device.currentValue('numberOfButtons'))))
            state.lastEvent = 'longPress'
            break
        case 0x0003:    // ShortRelease Event (Conformance: M)
            // This event SHALL be generated, when the momentary switch has been released (after debouncing).
            // If the server supports the Momentary Switch LongPress (MSL) feature, this event SHALL be generated when the switch is released if no LongPress event had been generated since the previous InitialPress event.
            // • If the server does not support the Momentary Switch LongPress (MSL) feature, this event SHALL be generated when the switch is released - even when the switch was pressed for a long time.
            // • Also see Section 1.13.7, “Sequence of generated events”.
            // data: ID:0 name: NewPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // This field SHALL indicate the previous value of the CurrentPosition attribute, i.e. just prior to release.
            //
            // [callbackType:Event, endpointInt:1, clusterInt:59, evtId:3, timestamp:222905792, timestampType:0, priority:1, eventSerial:458839, data:[3:STRUCT:[0:UINT:1]], value:[0:1], cluster:003B, endpoint:01]
            /*
            eventData.name = 'shortRelease'
            eventData.descriptionText = "${device.displayName} shortRelease"
            //handleShortReleaseEvent(eventData)
            */
            Integer featureMap = matter.convertHexToInt(device.getDataValue('featureMap'))
            Integer buttonNumber = toButtonNumber(safeToInt(descMap.value[0]), safeToInt(device.currentValue('numberOfButtons')))
            if ((featureMap & 0x08) != 0) { // MomentarySwitchLongPress (MSL) supported, so shortRelease is only generated for non-long presses
                if (state.lastEvent == 'longPress') {
                    logDebug "ignored shortRelease event (previous longPress and MSL supported)"
                    return
                }
                logDebug "shortRelease event <i>accepted</i> (MSL supported, so this is a non-long press, and the last event != longPress -> we send a pushed event in Hubitat for shortRelease)"
                sendButtonEvent('pushed', buttonNumber)
                return
            }
            else { // MSL not supported, so all releases generate shortRelease
                logDebug "shortRelease event processed (MSL not supported, so this could be a long press as well. However, we do not send released events for short presses in Hubitat)"
                return
                sendButtonEvent('released', buttonNumber)
            }
            //state.lastEvent = 'shortRelease'
            break
        case 0x0004:    // LongRelease Event (Conformance: M)
            // This event SHALL be generated, when the momentary switch has been released (after debouncing) and after having been pressed for a long time, 
            //  i.e. this event SHALL be generated when the switch is released if a LongPress event has been generated since the previous InitialPress event.
            //  Also see Section 1.13.7, “Sequence of generated events”.
            // data: ID:0 name: NewPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // This field SHALL indicate the previous value of the CurrentPosition attribute, i.e. just prior to release.
            logDebug "LongRelease event processed"
            sendButtonEvent('released', toButtonNumber(safeToInt(descMap.value[0]), safeToInt(device.currentValue('numberOfButtons'))))
            state.lastEvent = 'longRelease'
            break
        case 0x0005:    // MultiPressOngoing Event (Conformance: M)
            //This event SHALL be generated to indicate how many times the momentary switch has been pressed in a multi-press sequence, during that sequence. See Multi Press Details below.
            logDebug 'multiPressOngoing event processed'
            state.lastEvent = 'multiPressOngoing'
            break
        case 0x0006:    // MultiPressComplete
            // This event SHALL be generated to indicate how many times the momentary switch has been pressed in a multi-press sequence, after it has been detected that the sequence has ended. 
            // See Multi Press Details.
            // data: ID:0 name: PreviousPosition type:uint8 Constraint: 0 to NumberOfPositions-1 Conformance: M
            // data: ID:1 name: TotalNumberOfPressesCounted type:uint8 Constraint: 1 to MultiPressMax Conformance: M
            // The PreviousPosition field SHALL indicate the previous value of the CurrentPosition attribute, i.e. just prior to release.
            // The TotalNumberOfPressesCounted field SHALL contain:
            // • a value of 1 when there was one press in a multi-press sequence (and the sequence has ended), i.e. there was no double press (or more),
            // • a value of 2 when there were exactly two presses in a multi-press sequence (and the sequence has ended),
            // • a value of 3 when there were exactly three presses in a multi-press sequence (and the sequence has ended),
            // • a value of N when there were exactly N presses in a multi-press sequence (and the sequence has ended).
            //
            // [callbackType:Event, endpointInt:62, clusterInt:59, evtId:6, timestamp:1771500329745, timestampType:1, priority:1, eventSerial:2228462, 
            // data:[6:STRUCT:[0:UINT:1, 1:UINT:1]], value:[0:1, 1:1], 
            // cluster:003B, endpoint:3E]
            Integer PreviousPosition = safeToInt(descMap.value[0])
            Integer TotalNumberOfPressesCounted = safeToInt(descMap.value[1])
            logDebug "multiPressComplete event processed, PreviousPosition=${PreviousPosition}, TotalNumberOfPressesCounted=${TotalNumberOfPressesCounted}"
            if (TotalNumberOfPressesCounted == 1) {
                logDebug "Button ${toButtonNumber(PreviousPosition, safeToInt(device.currentValue('numberOfButtons')))} was pushed (multiPressComplete with count=1)"
                sendButtonEvent('pushed', toButtonNumber(PreviousPosition, safeToInt(device.currentValue('numberOfButtons'))))
            }
            else if (TotalNumberOfPressesCounted == 2) {
                logDebug "Button ${toButtonNumber(PreviousPosition, safeToInt(device.currentValue('numberOfButtons')))} was double-tapped (multiPressComplete with count=2)"
                sendButtonEvent('doubleTapped', toButtonNumber(PreviousPosition, safeToInt(device.currentValue('numberOfButtons'))))
            }
            else {
                logDebug "Button ${toButtonNumber(PreviousPosition, safeToInt(device.currentValue('numberOfButtons')))} was pressed ${TotalNumberOfPressesCounted} times (multiPressComplete with count=${TotalNumberOfPressesCounted})"
            }
            state.lastEvent = 'multiPressComplete'
            break
        default:
            logDebug "unhandled Switch event evtId=${evtIdStr} (normalized=${evtIdInt})"
            break
    }
}


// Handle Switch cluster attributes
private void handleSwitchAttribute(Map descMap) {
    logDebug "handleSwitchAttribute  : ${descMap}"

    // Declare variables for conditional logging
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${descMap.attrId}] " : ""
    String message = null
    boolean useDebugLog = false  // Flag to use logDebug instead of logInfo when isInfoMode is false


    String value = descMap.value
    Map attrData = [name: 'unknown', value: value, descriptionText: "${device.displayName} attribute ${attrIdStr ?: attrIdRaw} = ${value}"]

    switch (descMap.attrInt) {
        case 0x0000: // numberOfPositions
            // This attribute SHALL indicate the maximum number of positions the switch has. Any kind of switch has a minimum of 2 positions. Also see Multi Position Details for the case NumberOfPositions>2.
            attrData.name = 'numberOfPositions'
            attrData.descriptionText = "${device.displayName} numberOfPositions is ${value}"
            handleNumberOfPositions(attrData)
            break
        case 0x0001: // currentPosition
            // This attribute SHALL indicate the position of the switch. The valid range is zero to NumberOfPositions-1. CurrentPosition value 0 SHALL be assigned to the default position of the switch: 
            // for example the "open" state of a rocker switch, or the "idle" state of a push button switch.
            message = "${prefix}currentPosition attribute is ${value}"
            useDebugLog = true
            break
        case 0x0002: // multiPressMax
            // This attribute SHALL indicate how many consecutive presses can be detected and reported by a momentary switch which supports multi-press 
            // (e.g. it will report the value 3 if it can detect single press, double press and triple press, but not quad press and beyond).
            device.updateDataValue('multiPressMax', value.toString())
            logDebug "multiPressMax is ${value}"
            useDebugLog = true
            break

        case 0xFFFC: // FeatureMap
            Integer featureMap = safeToInt(descMap.value)
            log.trace "FeatureMap raw value: ${descMap.value} hex=0x${matter.integerTo8bitUnsignedHex(featureMap)}"
            device.updateDataValue('featureMap', matter.integerTo8bitUnsignedHex(featureMap).toString())
            String featuresText = decodeFeatureMap(featureMap)
            // FeatureMapRaw is stored in fingerprintData as '003B_FFFC', but it is wrong !!! - TODO !!
            message = "${prefix}FeatureMap: ${featuresText} (0x${matter.integerTo8bitUnsignedHex(featureMap)})"
            break
        case 0xFFFB: // AttributeList
            // AttributeList is stored in fingerprintData, no need to store separately
            message = "${prefix}AttributeList: ${descMap.value}"
            useDebugLog = true
            break
        case 0xFFFD: // ClusterRevision
            Integer revision = safeToInt(descMap.value)
            message = "${prefix}ClusterRevision: ${revision}"
            break
        case 0xFFF8: // GeneratedCommandList (events supported) - stored in fingerprintData only
            message = "${prefix}GeneratedCommandList: ${descMap.value}"
            useDebugLog = true
            break
        case 0xFFF9: // AcceptedCommandList - stored in fingerprintData only
            message = "${prefix}AcceptedCommandList: ${descMap.value}"
            useDebugLog = true
            break
        case 0xFFFA: // EventList - stored in fingerprintData only
            message = "${prefix}EventList: ${descMap.value}"
            useDebugLog = true
            break
        default:
            logDebug "unhandled Switch attribute attrId=${descMap.attrId}"
            break
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
    if ((featureMap & 0x01) != 0) { features.add('LatchingSwitch (LS)') }               // Switch is latching. This feature is for a switch that maintains its position after being pressed (or turned).
    if ((featureMap & 0x02) != 0) { features.add('MomentarySwitch (MS)') }              // Switch is momentary (not latching). This feature is for a switch that does not maintain its position after being pressed (or turned). 
                                                                                        // After releasing, it goes back to its idle position.
    if ((featureMap & 0x04) != 0) { features.add('MomentarySwitchRelease (MSR)') }      // Switch supports release. This feature is for a momentary switch that can distinguish and report release events. 
                                                                                        // When this feature flag MSR is present, MS SHALL be present as well.
    if ((featureMap & 0x08) != 0) { features.add('MomentarySwitchLongPress (MSL)') }    // Switch supports long press. This feature is for a momentary switch that can distinguish and report long presses from short presses. 
                                                                                        // When this feature flag MSL is present, MS and MSR SHALL be present as well.
    if ((featureMap & 0x10) != 0) { features.add('MomentarySwitchMultiPress (MSM)') }   // Switch supports multi-press. This feature is for a momentary switch that can distinguish and report double press and potentially multiple presses with more events, such as triple press, etc. 
                                                                                        // When this feature flag MSM is present, MS and MSR SHALL be present as well.
    return features.isEmpty() ? 'None' : features.join(', ')
}



private void handleNumberOfPositions(final Map d) {
    Integer positions = safeToInt(d.value)
    if (positions == null || positions < 1) {
        logDebug "ignored numberOfPositions value '${d.value}'"
        return
    }
    // Store the Matter device's total numberOfPositions for reference
    device.updateDataValue('numberOfPositions', d.value.toString())
    // Each child device represents a single button position, so numberOfButtons is always 1
    sendEvent(name: 'numberOfButtons', value: 1, isStateChange: true)
    logInfo "numberOfButtons is 1 (Matter device has ${positions} positions)"
}


private Integer toButtonNumber(final Integer pos, final Integer numberOfButtons) {
    if (pos == null) { return 1 }
    if (numberOfButtons == null || numberOfButtons <= 1) { return 1 }

    // Heuristic:
    // - If pos is within 1..N, treat it as 1-based and use as-is.
    // - Else if within 0..N-1, treat it as 0-based and add 1.
    if (pos >= 1 && pos <= numberOfButtons) { return pos }
    if (pos >= 0 && pos < numberOfButtons) { return pos + 1 }
    return (pos < 1) ? 1 : pos
}

private void sendButtonEvent(final String action, final Integer buttonNumber, final String eventType = 'physical') {
    if (buttonNumber == null || buttonNumber < 1) {
        logDebug "ignored ${action} event for invalid buttonNumber=${buttonNumber}"
        return
    }

    String desc = "${device.displayName} button ${buttonNumber} was ${action}"
    if (eventType == 'digital') { desc += ' [digital]' }
    else if (eventType == 'physical') { desc += ' [physical]' }
    sendEvent(name: action, value: buttonNumber, descriptionText: desc, isStateChange: true, type: eventType)
    logInfo desc
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
}

// Called when the settings are updated
void updated() {
    logInfo 'driver configuration updated'
    if (device.currentValue('numberOfButtons') == null) {
        sendEvent(name: 'numberOfButtons', value: 1, isStateChange: true)
    }
    logDebug "settings: ${settings}"
    if (logEnable) {
        runIn(14400, 'logsOff')
    }
}

private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'])
}

// -------------


/**
 * Get the Switch cluster AttributeList (0x003B_FFFB)
 * @return List of attribute IDs as hex strings (e.g., ["00", "01", "02", ...])
 */
List<String> getSwitchAttributeList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getSwitchAttributeList: fingerprint data not available"
        return []
    }
    
    return fingerprint['003B_FFFB'] ?: []
}

// Command to get all supported Switch cluster attributes (for info/debugging)
void getInfo() {
    // Check if Switch cluster is supported
    if (!isClusterSupported('003B')) {
        logWarn "getInfo: Switch cluster (0x003B) is not supported by this device"
        logInfo "getInfo: ServerList contains: ${getServerList()}"
        return
    }
    logInfo "getInfo: reading all supported Switch cluster attributes: ${getSwitchAttributeList()}"
    
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
    parent?.readAttribute(endpoint, 0x003B, -1)      // 0x003B Switch cluster - read all attributes
}

// Clear info mode flag (called by scheduled job)
void clearInfoMode() {
    if (state.states == null) { state.states = [:] }
    state.states.isInfo = false
    device.updateSetting('logEnable', [value: state.states?.debugState ?: 'false', type: 'bool'] )  // restore debug logging to the value before info mode was enabled
    logDebug "clearInfoMode: info mode disabled"
}

void refresh() {
    parent?.componentRefresh(this.device)
}

// Capability command: PushableButton
void push(BigDecimal buttonNumber) {
    Integer btn = buttonNumber?.intValue() ?: 1
    sendButtonEvent('pushed', btn, 'digital')
}

// Capability command: HoldableButton
void hold(BigDecimal buttonNumber) {
    Integer btn = buttonNumber?.intValue() ?: 1
    sendButtonEvent('held', btn, 'digital')
}

// Capability command: ReleasableButton
void release(BigDecimal buttonNumber) {
    Integer btn = buttonNumber?.intValue() ?: 1
    sendButtonEvent('released', btn, 'digital')
}

// Capability command: DoubleTapableButton
void doubleTap(BigDecimal buttonNumber) {
    Integer btn = buttonNumber?.intValue() ?: 1
    sendButtonEvent('doubleTapped', btn, 'digital')
}

// --------- common matter libraries included below --------

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
