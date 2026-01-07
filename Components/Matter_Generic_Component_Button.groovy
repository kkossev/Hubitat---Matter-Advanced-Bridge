/* groovylint-disable CompileStatic, DuplicateStringLiteral, LineLength, PublicMethodsBeforeNonPublicMethods */
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
    * ver. 1.0.0  2026-01-07 GPT-5.2  - inital release
  *
*/

import groovy.transform.Field
import groovy.json.JsonSlurper

@Field static final String matterComponentButtonVersion = '1.0.0'
@Field static final String matterComponentButtonStamp   = '2026/01/07 10:56 PM'

@Field static final JsonSlurper jsonParser = new JsonSlurper()

metadata {
    definition(name: 'Matter Generic Component Button', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Generic_Component_Button.groovy') {
        capability 'Refresh'
        capability 'PushableButton'
        capability 'HoldableButton'
        capability 'ReleasableButton'
        capability 'DoubleTapableButton'

        // Switch cluster (0x003B) attributes forwarded by parent
        attribute 'numberOfPositions', 'string'
        attribute 'currentPosition', 'string'
        attribute 'multiPressMax', 'string'

        // Switch cluster (0x003B) events forwarded by parent
        attribute 'initialPress', 'string'
        attribute 'longPress', 'string'
        attribute 'shortRelease', 'string'
        attribute 'longRelease', 'string'
        attribute 'multiPressOngoing', 'string'
        attribute 'multiPressComplete', 'string'
        attribute 'switchLatched', 'string'

        // Unprocessed Matter cluster data from parent
        attribute 'unprocessed', 'string'
    }
}

preferences {
    section {
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging', required: false, defaultValue: true
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', required: false, defaultValue: true
        input name: 'forceSingleButton', type: 'bool', title: 'Force single button (numberOfButtons = 1)', required: false, defaultValue: false
    }
}

/* groovylint-disable-next-line UnusedMethodParameter */
void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    if (logEnable) { log.debug "${device.displayName} ${description}" }

    description.each { d ->
        switch (d.name) {
            case 'unprocessed':
                handleUnprocessed(d)
                break
            case 'numberOfPositions':
                handleNumberOfPositions(d)
                break
            case 'currentPosition':
                handleCurrentPosition(d)
                break

            case 'initialPress':
                handleInitialPressEvent(d)
                break
            case 'longPress':
                handleHeldEvent(d)
                break
            case 'shortRelease':
                handleShortReleaseEvent(d)
                break
            case 'longRelease':
                handleLongReleaseEvent(d)
                break

            case 'multiPressComplete':
                handleMultiPressComplete(d)
                break

            default:
                if (d.descriptionText && txtEnable) { log.info "${d.descriptionText}" }
                sendEvent(d)
                break
        }
    }
}

// Handle unprocessed Matter cluster data forwarded from parent bridge
private void handleUnprocessed(final Map d) {
    if (logEnable) { log.debug "${device.displayName} handleUnprocessed: ${d.value}" }
    
    // Parse the descMap from JSON string
    Map descMap = parseDescMapJson(d.value)
    if (descMap == null) {
        log.warn "${device.displayName} failed to parse unprocessed data: ${d.value}"
        return
    }

    // Handle Switch cluster (0x003B) attributes and events
    if (descMap.cluster == '003B' || descMap.clusterInt == 59) {
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
        if (logEnable) { log.debug "${device.displayName} unprocessed cluster ${descMap.cluster} not supported" }
    }
}

// Parse descMap from JSON string
private Map parseDescMapJson(String jsonStr) {
    if (jsonStr == null || jsonStr == 'null' || jsonStr.trim() == '') { return null }
    
    try {
        Object parsed = jsonParser.parseText(jsonStr)
        if (parsed instanceof Map) {
            return (Map)parsed
        }
    } catch (Exception e) {
        log.warn "${device.displayName} parseDescMapJson error: ${e}"
        return null
    }
    
    return null
}

// Handle Switch cluster events
private void handleSwitchEvent(Map descMap) {
    String evtId = descMap.evtId ?: (descMap.evtInt ? HexUtils.integerToHexString(descMap.evtInt as Integer, 1) : null)
    
    // Get the event values from descMap.values if available
    Map payload = extractEventPayload(descMap)
    String payloadJson = (payload && !payload.isEmpty()) ? groovy.json.JsonOutput.toJson(payload) : null
    
    // Map Matter Switch events to handler methods
    Map eventData = [name: 'unknown', value: payloadJson ?: descMap.value, descriptionText: "${device.displayName} event ${evtId}"]
    
    switch (evtId) {
        case '0001':
        case '1':
            eventData.name = 'initialPress'
            eventData.descriptionText = "${device.displayName} initialPress"
            handleInitialPressEvent(eventData)
            break
        case '0002':
        case '2':
            eventData.name = 'longPress'
            eventData.descriptionText = "${device.displayName} longPress"
            handleHeldEvent(eventData)
            break
        case '0003':
        case '3':
            eventData.name = 'shortRelease'
            eventData.descriptionText = "${device.displayName} shortRelease"
            handleShortReleaseEvent(eventData)
            break
        case '0004':
        case '4':
            eventData.name = 'longRelease'
            eventData.descriptionText = "${device.displayName} longRelease"
            handleLongReleaseEvent(eventData)
            break
        case '0005':
        case '5':
            eventData.name = 'multiPressOngoing'
            eventData.descriptionText = "${device.displayName} multiPressOngoing"
            sendEvent(eventData)
            break
        case '0006':
        case '6':
            eventData.name = 'multiPressComplete'
            eventData.descriptionText = "${device.displayName} multiPressComplete"
            handleMultiPressComplete(eventData)
            break
        default:
            if (logEnable) { log.debug "${device.displayName} unhandled Switch event ${evtId}" }
            break
    }
}

// Extract event payload from descMap.values
private Map extractEventPayload(Map descMap) {
    Map payload = [:]
    
    // Try to extract from descMap.values if it exists (now properly parsed as Map from JSON)
    if (descMap.values != null) {
        try {
            // JSON always uses string keys, so check for "0" and "1"
            def valuesMap = descMap.values
            
            // Extract position (tag 0) - JSON keys are strings
            def tag0 = valuesMap['0']
            if (tag0 != null) {
                def position = (tag0 instanceof Map) ? tag0['value'] : tag0
                if (position != null) {
                    Integer pos = safeToInt(position)
                    if (pos != null) { payload.position = pos }
                }
            }
            
            // Extract pressCount (tag 1) if present - JSON keys are strings
            def tag1 = valuesMap['1']
            if (tag1 != null) {
                def pressCount = (tag1 instanceof Map) ? tag1['value'] : tag1
                if (pressCount != null) {
                    Integer pc = safeToInt(pressCount)
                    if (pc != null) { payload.pressCount = pc }
                }
            }
        } catch (Exception e) {
            if (logEnable) { log.debug "${device.displayName} extractEventPayload error: ${e.message}" }
        }
    }
    
    return payload
}

// Handle Switch cluster attributes
private void handleSwitchAttribute(Map descMap) {
    String attrId = descMap.attrId ?: (descMap.attrInt ? HexUtils.integerToHexString(descMap.attrInt as Integer, 1) : null)
    String value = descMap.value
    
    Map attrData = [name: 'unknown', value: value, descriptionText: "${device.displayName} attribute ${attrId} = ${value}"]
    
    switch (attrId) {
        case '0000':
        case '0':
            attrData.name = 'numberOfPositions'
            attrData.descriptionText = "${device.displayName} numberOfPositions is ${value}"
            handleNumberOfPositions(attrData)
            break
        case '0001':
        case '1':
            attrData.name = 'currentPosition'
            attrData.descriptionText = "${device.displayName} currentPosition is ${value}"
            handleCurrentPosition(attrData)
            break
        case '0002':
        case '2':
            attrData.name = 'multiPressMax'
            attrData.descriptionText = "${device.displayName} multiPressMax is ${value}"
            sendEvent(attrData)
            break
        default:
            if (logEnable) { log.debug "${device.displayName} unhandled Switch attribute ${attrId}" }
            break
    }
}

// Record event attribute but do not emit Hubitat capability events here.
// For correct semantics:
// - single click should produce only 'pushed'
// - long press should produce 'held' then 'released' (no extra 'pushed')
// We therefore emit 'pushed' based on multiPressComplete pressCount==1.
private void handleInitialPressEvent(final Map d) {
    sendEvent(d)
}

private void handleHeldEvent(final Map d) {
    sendEvent(d)

    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    state.lastHeldButton = buttonNumber
    state.lastHeldMs = now()
    // Used to suppress multiPressComplete-derived pushed/doubleTapped after a hold.
    state.justHeldUntilMs = now() + 2000L
    sendButtonEvent('held', buttonNumber)
}

// For Hubitat, a normal 'pushed' action does not have a corresponding 'released'.
// We still record the event attribute, but do not emit the capability event.
private void handleShortReleaseEvent(final Map d) {
    sendEvent(d)
}

// Emit 'released' only after a prior 'held' (longPress) for the same button.
private void handleLongReleaseEvent(final Map d) {
    sendEvent(d)

    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    Integer lastHeldButton = (state.lastHeldButton instanceof Number) ? (state.lastHeldButton as Number).intValue() : null
    if (lastHeldButton == null || lastHeldButton != buttonNumber) {
        if (logEnable) { log.debug "${device.displayName} ignored longRelease for button ${buttonNumber} (not held)" }
        return
    }

    state.lastHeldButton = null
    state.lastHeldMs = null
    state.justHeldUntilMs = now() + 2000L
    sendButtonEvent('released', buttonNumber)
}

private void handleNumberOfPositions(final Map d) {
    Integer positions = safeToInt(d.value)
    if (positions == null || positions < 1) {
        if (logEnable) { log.debug "${device.displayName} ignored numberOfPositions value '${d.value}'" }
        return
    }
    sendEvent(d)
    Integer buttons = (forceSingleButton == true) ? 1 : positions
    sendEvent(name: 'numberOfButtons', value: buttons, isStateChange: true)
    if (txtEnable) { log.info "${device.displayName} numberOfButtons is ${buttons}" }
}

private void handleCurrentPosition(final Map d) {
    String prev = device.currentValue('currentPosition')?.toString()
    String next = d.value?.toString()
    if (prev == next) {
        if (logEnable) { log.debug "${device.displayName} : ignored currentPosition '${next}' (no change)" }
        return
    }

    sendEvent(d)

    // Don't generate pushed events from currentPosition if this is a momentary button device
    // that reports proper Switch events. The currentPosition is just a state flag (0/1).
    // Rely on multiPressComplete events instead for devices that support them.
    if (logEnable) { log.debug "${device.displayName} currentPosition changed to ${next} (not generating button event)" }
}

private void handleMultiPressComplete(final Map d) {
    sendEvent(d)

    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer pressCount = (payload?.pressCount != null) ? safeToInt(payload.pressCount) : null

    if (pressCount == null) {
        if (logEnable) { log.debug "${device.displayName} multiPressComplete missing pressCount (value='${d.value}')" }
        return
    }
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    // If we just had a longPress/longRelease sequence, do not derive pushed/doubleTapped from multiPressComplete.
    Long untilMs = (state.justHeldUntilMs instanceof Number) ? (state.justHeldUntilMs as Number).longValue() : null
    if (untilMs != null && now() <= untilMs) {
        if (logEnable) { log.debug "${device.displayName} ignored multiPressComplete pressCount=${pressCount} (recent hold)" }
        return
    }

    if (pressCount == 2) {
        sendButtonEvent('doubleTapped', buttonNumber)
        return
    }

    if (pressCount == 1) {
        sendButtonEvent('pushed', buttonNumber)
        return
    }

    if (logEnable) { log.debug "${device.displayName} ignored multiPressComplete pressCount=${pressCount}" }
}

private Integer toButtonNumber(final Integer pos, final Integer numberOfButtons) {
    if (forceSingleButton == true) { return 1 }
    if (pos == null) { return 1 }
    if (numberOfButtons == null || numberOfButtons <= 1) { return 1 }

    // Heuristic:
    // - If pos is within 1..N, treat it as 1-based and use as-is.
    // - Else if within 0..N-1, treat it as 0-based and add 1.
    if (pos >= 1 && pos <= numberOfButtons) { return pos }
    if (pos >= 0 && pos < numberOfButtons) { return pos + 1 }
    return (pos < 1) ? 1 : pos
}

private void sendButtonEvent(final String action, final Integer buttonNumber) {
    if (buttonNumber == null || buttonNumber < 1) {
        if (logEnable) { log.debug "${device.displayName} ignored ${action} event for invalid buttonNumber=${buttonNumber}" }
        return
    }

    String desc = "${device.displayName} button ${buttonNumber} was ${action}"
    sendEvent(name: action, value: buttonNumber, descriptionText: desc, isStateChange: true, type: 'physical')
    if (txtEnable) { log.info desc }
}

private Map decodePayload(final Object rawValue) {
    if (rawValue == null) { return [:] }
    String s = rawValue.toString().trim()
    if (!s) { return [:] }

    if (s.startsWith('{') && s.endsWith('}')) {
        try {
            Object parsed = jsonParser.parseText(s)
            if (parsed instanceof Map) { return (Map)parsed }
        } catch (Exception ignored) {
            return [:]
        }
    }

    // Fallback: treat single numeric-ish values as a position
    Integer v = safeToInt(s)
    if (v != null) {
        return [position: v]
    }

    return [:]
}

private Integer safeToInt(final Object value) {
    if (value == null) { return null }
    if (value instanceof Number) { return (value as Number).intValue() }

    String s = value.toString().trim()
    if (!s) { return null }

    try {
        if (s.startsWith('0x') || s.startsWith('0X')) {
            return Integer.parseInt(s.substring(2), 16)
        }
        if (s ==~ /^[0-9]+$/) {
            return Integer.parseInt(s, 10)
        }
        if (s ==~ /^[0-9a-fA-F]+$/) {
            return Integer.parseInt(s, 16)
        }
    } catch (Exception ignored) {
        return null
    }

    return null
}

// Component command to ping the device
void ping() {
    parent?.componentPing(device)
}

// Called when the device is first created
void installed() {
    log.info "${device.displayName} driver installed"
}

// Called when the settings are updated
void updated() {
    log.info "${device.displayName} driver configuration updated"
    if (device.currentValue('numberOfButtons') == null) {
        sendEvent(name: 'numberOfButtons', value: 1, isStateChange: true)
    }
    if (logEnable) {
        log.debug settings
        runIn(86400, 'logsOff')
    }
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'])
}

void refresh() {
    parent?.componentRefresh(this.device)
}
