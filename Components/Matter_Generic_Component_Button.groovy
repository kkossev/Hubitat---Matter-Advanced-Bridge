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
  *
*/

import groovy.transform.Field
import groovy.json.JsonSlurper

@Field static final String matterComponentButtonVersion = '1.0.3'
@Field static final String matterComponentButtonStamp   = '2026/01/25 8:30 PM'

@Field static final JsonSlurper jsonParser = new JsonSlurper()

metadata {
    definition(name: 'Matter Generic Component Button', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Components/Matter_Generic_Component_Button.groovy') {
        capability 'Refresh'
        capability 'PushableButton'
        capability 'HoldableButton'
        capability 'ReleasableButton'
        capability 'DoubleTapableButton'
    }
}

preferences {
    section {
        input name: 'logEnable', type: 'bool', title: 'Enable debug logging', required: false, defaultValue: true
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', required: false, defaultValue: true
        input name: 'generatePushedOn', type: 'enum', title: 'Generate "pushed" event on:', 
            options: [
                'multiPressComplete': 'Multi-Press Complete (default - standard Matter behavior)',
                'shortRelease': 'Short Release (use if button doesn\'t send multi-press events)'
            ], 
            required: false, 
            defaultValue: 'multiPressComplete',
            description: 'Some buttons (e.g., Hue dimmer switch) only send shortRelease, never multiPressComplete. Choose which event should trigger the "pushed" event.'
    }
}

void parse(String description) { log.warn 'parse(String description) not implemented' }

// parse commands from parent
void parse(List<Map> description) {
    logDebug "${description}"

    description.each { d ->
        switch (d.name) {
            case 'rtt':
                // Delegate to health status library
                parseRttEvent(d)
                break
            
            case 'unprocessed':
                // All Switch cluster events and attributes are routed through handleUnprocessed()
                handleUnprocessed(d)
                break

            default:
                if (d.descriptionText) { logInfo "${d.descriptionText}" }
                sendEvent(d)
                break
        }
    }
}

// Handle unprocessed Matter cluster data forwarded from parent bridge
private void handleUnprocessed(final Map d) {
    logDebug "handleUnprocessed: ${d.value}"
    
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
        logDebug "unprocessed cluster ${descMap.cluster} not supported"
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
    Object evtIdRaw = (descMap.evtId != null) ? descMap.evtId : descMap.evtInt
    Integer evtIdInt = (evtIdRaw instanceof Number) ? ((Number)evtIdRaw).intValue() : safeHexToInt(evtIdRaw?.toString(), null)
    String evtIdStr = (evtIdRaw != null) ? evtIdRaw.toString() : 'null'

    // Build a consistent payload map across newParse/oldParse.
    Map payload = extractEventPayload(descMap)

    // Map Matter Switch events to handler methods
    Map eventData = [name: 'unknown', value: payload ?: descMap.value, descriptionText: "${device.displayName} event ${evtIdStr}"]

    switch (evtIdInt) {
        case 1:
            eventData.name = 'initialPress'
            eventData.descriptionText = "${device.displayName} initialPress"
            handleInitialPressEvent(eventData)
            break
        case 2:
            eventData.name = 'longPress'
            eventData.descriptionText = "${device.displayName} longPress"
            handleHeldEvent(eventData)
            break
        case 3:
            eventData.name = 'shortRelease'
            eventData.descriptionText = "${device.displayName} shortRelease"
            handleShortReleaseEvent(eventData)
            break
        case 4:
            eventData.name = 'longRelease'
            eventData.descriptionText = "${device.displayName} longRelease"
            handleLongReleaseEvent(eventData)
            break
        case 5:
            logDebug 'multiPressOngoing event processed'
            break
        case 6:
            eventData.name = 'multiPressComplete'
            eventData.descriptionText = "${device.displayName} multiPressComplete"
            handleMultiPressComplete(eventData)
            break
        default:
            logDebug "unhandled Switch event evtId=${evtIdStr} (normalized=${evtIdInt})"
            break
    }
}

// Extract event payload from descMap.values (oldParse) or descMap.value (newParse)
private Map extractEventPayload(Map descMap) {
    Map payload = [:]

    Map valuesMap = null
    if (descMap.values instanceof Map) {
        valuesMap = (Map)descMap.values
    }
    else if (descMap.value instanceof Map) {
        valuesMap = (Map)descMap.value
    }

    if (valuesMap == null) { return payload }

    try {
        def findTagValue = { Map m, String tag ->
            if (m == null) { return null }
            if (m.containsKey(tag)) { return m[tag] }
            // Defensive: some Hubitat map implementations can throw or behave oddly
            // when accessed with non-string keys; fall back to scanning keys.
            for (def entry in m.entrySet()) {
                if (entry?.key != null && entry.key.toString() == tag) {
                    return entry.value
                }
            }
            return null
        }

        // oldParse: valuesMap['0'] -> [type:7, value:1]
        // newParse: valuesMap['0'] -> 1
        def tag0 = findTagValue(valuesMap, '0')
        if (tag0 != null) {
            def position = (tag0 instanceof Map) ? ((Map)tag0)['value'] : tag0
            Integer pos = safeToInt(position)
            if (pos != null) { payload.position = pos }
        }

        def tag1 = findTagValue(valuesMap, '1')
        if (tag1 != null) {
            def pressCount = (tag1 instanceof Map) ? ((Map)tag1)['value'] : tag1
            Integer pc = safeToInt(pressCount)
            if (pc != null) { payload.pressCount = pc }
        }
    } catch (Exception e) {
        logDebug "extractEventPayload error: ${e}"
    }

    return payload
}

// Handle Switch cluster attributes
private void handleSwitchAttribute(Map descMap) {
    Object attrIdRaw = (descMap.attrId != null) ? descMap.attrId : descMap.attrInt
    Integer attrIdInt = null
    String attrIdStr = null

    if (descMap.attrInt != null) {
        // newParse=true often provides attrInt as a Number (Long/Integer)
        attrIdInt = safeToInt(descMap.attrInt, null)
        attrIdStr = (attrIdInt != null) ? HexUtils.integerToHexString(attrIdInt as Integer, 2) : null
    }
    else {
        // oldParse provides attrId as a hex string (e.g. '0001')
        attrIdStr = (descMap.attrId != null) ? descMap.attrId.toString() : null
        attrIdInt = safeHexToInt(attrIdStr, null)
    }

    // Fallback: if we have only a string and it wasn't parsed above
    if (attrIdInt == null && attrIdStr != null) {
        attrIdInt = safeHexToInt(attrIdStr, null)
    }

    String value = descMap.value
    Map attrData = [name: 'unknown', value: value, descriptionText: "${device.displayName} attribute ${attrIdStr ?: attrIdRaw} = ${value}"]

    switch (attrIdInt) {
        case 0:
            attrData.name = 'numberOfPositions'
            attrData.descriptionText = "${device.displayName} numberOfPositions is ${value}"
            handleNumberOfPositions(attrData)
            break
        case 1:
            attrData.name = 'currentPosition'
            attrData.descriptionText = "${device.displayName} currentPosition is ${value}"
            handleCurrentPosition(attrData)
            break
        case 2:
            device.updateDataValue('multiPressMax', value.toString())
            logDebug "multiPressMax is ${value}"
            break
        default:
            logDebug "unhandled Switch attribute attrId=${attrIdStr ?: attrIdRaw} (normalized=${attrIdInt})"
            break
    }
}

// Process initial press event (does not generate a capability event)
// For correct semantics:
// - single click should produce only 'pushed'
// - long press should produce 'held' then 'released' (no extra 'pushed')
// We therefore emit 'pushed' based on multiPressComplete pressCount==1.
private void handleInitialPressEvent(final Map d) {
    logDebug 'initialPress event processed'
}

private void handleHeldEvent(final Map d) {
    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    state.lastHeldButton = buttonNumber
    // Used to suppress multiPressComplete-derived pushed/doubleTapped after a hold.
    state.justHeldUntilMs = now() + 2000L
    sendButtonEvent('held', buttonNumber)
}

// For Hubitat, a normal 'pushed' action does not have a corresponding 'released'.
// Process the event but do not emit the capability event.
private void handleShortReleaseEvent(final Map d) {
    logDebug 'shortRelease event processed'
    
    // Check if user configured to generate 'pushed' on shortRelease instead of multiPressComplete
    if (settings?.generatePushedOn == 'shortRelease') {
        Map payload = decodePayload(d.value)
        Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
        Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
        Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1
        
        sendButtonEvent('pushed', buttonNumber)
        logDebug "generated 'pushed' event on shortRelease (user preference)"
    }
}

// Emit 'released' only after a prior 'held' (longPress) for the same button.
private void handleLongReleaseEvent(final Map d) {
    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    Integer lastHeldButton = (state.lastHeldButton instanceof Number) ? (state.lastHeldButton as Number).intValue() : null
    if (lastHeldButton == null || lastHeldButton != buttonNumber) {
        logDebug "ignored longRelease for button ${buttonNumber} (not held)"
        return
    }

    state.lastHeldButton = null
    state.justHeldUntilMs = now() + 2000L
    sendButtonEvent('released', buttonNumber)
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

private void handleCurrentPosition(final Map d) {
    // Process currentPosition for debugging but don't create an attribute
    // This is useful for troubleshooting but doesn't clutter Current States
    logDebug "currentPosition changed to ${d.value}"
}

private void handleMultiPressComplete(final Map d) {
    // Skip if user configured to generate pushed on shortRelease instead
    if (settings?.generatePushedOn == 'shortRelease') {
        logDebug 'ignored multiPressComplete (using shortRelease preference)'
        return
    }
    
    Map payload = decodePayload(d.value)
    Integer pos = (payload?.position != null) ? safeToInt(payload.position) : null
    Integer pressCount = (payload?.pressCount != null) ? safeToInt(payload.pressCount) : null

    if (pressCount == null) {
        logDebug "multiPressComplete missing pressCount (value='${d.value}')"
        return
    }
    Integer buttons = safeToInt(device.currentValue('numberOfButtons'))
    Integer buttonNumber = (pos != null) ? toButtonNumber(pos, buttons) : 1

    // If we just had a longPress/longRelease sequence, do not derive pushed/doubleTapped from multiPressComplete.
    Long untilMs = (state.justHeldUntilMs instanceof Number) ? (state.justHeldUntilMs as Number).longValue() : null
    if (untilMs != null && now() <= untilMs) {
        logDebug "ignored multiPressComplete pressCount=${pressCount} (recent hold)"
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

    logDebug "ignored multiPressComplete pressCount=${pressCount}"
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

private Map decodePayload(final Object rawValue) {
    if (rawValue == null) { return [:] }

    if (rawValue instanceof Map) {
        return (Map)rawValue
    }

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
