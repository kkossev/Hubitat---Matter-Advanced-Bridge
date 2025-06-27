/* groovylint-disable CompileStatic, DuplicateNumberLiteral, DuplicateStringLiteral, ImplicitClosureParameter, LineLength */
library(
    base: 'driver',
    author: 'Krassimir Kossev',
    category: 'matter',
    description: 'Matter Utilities Library',
    name: 'matterUtilitiesLib',
    namespace: 'kkossev',
    importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Libraries/matterUtilitiesLib.groovy',
    version: '1.3.0',
    documentationLink: ''
)
/*
  *  Matter Utilities Library
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
  * ver. 1.0.0  2024-03-16 kkossev  - first release
  * ver. 1.1.0  2024-07-20 kkossev  - release 1.1.2
  * ver. 1.2.0  2024-10-11 kkossev  - added testParse()
  * ver. 1.3.0  2025-06-27 Claude Sonnet 4  - added custom decodeTLVToHex() and decodeTLV()
*/

import groovy.transform.Field

/* groovylint-disable-next-line ImplicitReturnStatement */
@Field static final String matterUtilitiesLibVersion = '1.3.0'
@Field static final String matterUtilitiesLibStamp   = '2025/06/27 1:02 PM'

metadata {
    // no capabilities
    // no attributes
    command 'utilities', [[name:'command', type: 'STRING', description: 'for advanced users: ? for help', constraints: ['STRING']]]
    if (_DEBUG == true) {
        command 'testParse', [[name: 'testParse', type: 'STRING', description: 'testParse', defaultValue : '']]
    }
// no preferences
}

@Field static final Map<String, String> UtilitiesMap = [
    'readAttribute': 'readAttribute',
    'readAttributeSafe': 'readAttributeSafe',
    'subscribeSingleAttribute': 'subscribeSingleAttribute',
    'unsubscribe': 'unsubscribe',
    'removeAllDevices': 'removeAllDevices',
    'removeAllSubscriptions': 'removeAllSubscriptions',
    'minimizeStateVariables': 'minimizeStateVariables',
    'resetStats': 'resetStats',
    'testMatter': 'testMatter',
    'decodeTLV': 'testDecodeTLV',
    'help': 'utilitiesHelp'
]

void utilitiesHelp(List<String> parameters) {
    logDebug "utilitiesHelp: ${parameters}"
    logInfo "utilitiesHelp: supported commands: ${UtilitiesMap.keySet()}"
}

void readAttributeSafe(List<String> parameters /*String endpointPar, String clusterPar, String attrIdPar*/) {
    if (parameters == null || parameters.size() != 3) {
        logInfo 'usage: readAttributeSafe endpoint cluster attribute'
        return
    }
    Integer endpointInt = safeNumberToInt(parameters[0])
    Integer clusterInt  = safeNumberToInt(parameters[1])
    Integer attrInt     = safeNumberToInt(parameters[2])
    String  endpointId  = HexUtils.integerToHexString(endpointInt, 1)
    String  clusterId   = HexUtils.integerToHexString(clusterInt, 2)
    String  attrId      = HexUtils.integerToHexString(attrInt, 2)
    logDebug "readAttributeSafe(endpoint:${endpointId}, cluster:${clusterId}, attribute:${attrId}) -> starting readSingeAttrStateMachine!"

    readSingeAttrStateMachine([action: START, endpoint: endpointInt, cluster: clusterInt, attribute: attrInt])
}

void readAttribute(List<String> parameters /*String endpointPar, String clusterPar, String attrIdPar*/) {
    if (parameters == null || parameters.size() != 3) {
        logInfo 'usage: readAttribute endpoint cluster attribute'
        return
    }
    Integer endpoint = safeNumberToInt(parameters[0])
    Integer cluster = safeNumberToInt(parameters[1])
    Integer attrId = safeNumberToInt(parameters[2])
    logDebug "readAttribute(endpoint:${endpoint}, cluster:${cluster}, attrId:${attrId})"
    readAttribute(endpoint, cluster, attrId)
}

/**
 * Subscribes to or unsubscribes from a specific attribute in the Matter Advanced Bridge.
 *
 * @param addOrRemove The action to perform. Valid values are 'add', 'remove', or 'show'.
 * @param endpoint The endpoint of the attribute.
 * @param cluster The cluster of the attribute.
 * @param attrId The attribute ID.
 *
 * sends matter.subscribe command to the bridge!!
 */
void subscribeSingleAttribute(List<String> parameters /*String addOrRemove, String endpointPar, String clusterPar, String attrIdPar*/) {
    if (parameters == null || parameters.size() != 4) {
        logInfo 'usage: subscribeSingleAttribute addOrRemove endpoint cluster attribute'
        return
    }
    Integer endpoint = safeNumberToInt(parameters[1])
    Integer cluster = safeNumberToInt(parameters[2])
    Integer attrId = safeNumberToInt(parameters[3])
    String cmd = updateStateSubscriptionsList(addOrRemove, endpoint, cluster, attrId)
    if (cmd != null && cmd != '') {
        logDebug "subscribeSingleAttribute(): cmd = ${cmd}"
        sendToDevice(cmd)
    }
}

void unsubscribe(List<String> parameters) {
    logTrace "unsubscribe: ${parameters}"
    unsubscribe()
}

void removeAllSubscriptions(List<String> parameters) {
    logTrace "removeAllSubscriptions(${parameters}) ..."
    clearSubscriptionsState()
    unsubscribe()
    sendInfoEvent('all subsciptions are removed!', 're-discover the devices again ...')
}

void removeAllDevices(List<String> parameters) {
    logTrace "Removing all child devices ${parameters}"
    removeAllDevices()
}

boolean utilities(String commandLine=null) {
    //List<String> supportedCommandsList =  UtilitiesMap.keySet().collect { it.toLowerCase() }
    List<String> supportedCommandsList = UtilitiesMap.keySet()*.toLowerCase()
    List commandLineParsed = commandLine?.split(' ')
    if (commandLineParsed == null || commandLineParsed.size() == 0) {
        logInfo "utilities: command is null or empty! supportedCommandsList=${UtilitiesMap.keySet()}"
        return false
    }
    String cmd = commandLineParsed[0]?.toLowerCase()
    List<String> parameters = commandLineParsed.drop(1)

    logDebug "utilities: cmd=${cmd}, parameters=${parameters}, supportedCommandsList=${UtilitiesMap.keySet()}"
    // check if the cmd is in the supportedCommandsList
    if (cmd == null || !(cmd in supportedCommandsList)) {
        logInfo "utilities: the command <b>${(cmd ?: '')}</b> must be one of these : ${UtilitiesMap.keySet()}"
        return false
    }
    // find func name from the UtilitiesMap
    String func = UtilitiesMap.find { it.key.toLowerCase() == cmd }.value
    if (func == null) {
        logInfo "utilities: the command <b>${cmd}</b> is not supported!"
        return false
    }
    try {
        "${func}"(parameters)
    }
    catch (e) {
        logWarn "utilities: Exception '${e}' caught while processing <b>${func}</b>(${parameters})"
        return false
    }
    return true
}

// TODO - refactor or remove?
void collectBasicInfo(Integer endpoint = 0, Integer timePar = 1, boolean fast = false) {
    Integer time = timePar
    // first thing to do is to read the Bridge (ep=0) Descriptor Cluster (0x001D) attribute 0XFFFB and store the ServerList in state.bridgeDescriptor['ServerList']
    // also, the DeviceTypeList ClientList and PartsList are stored in state.bridgeDescriptor
    requestAndCollectAttributesValues(endpoint, cluster = 0x001D, time, fast)  // Descriptor Cluster - DeviceTypeList, ServerList, ClientList, PartsList

    // next - fill in all the ServerList clusters attributes list in the fingerprint
    time += fast ? SHORT_TIMEOUT : LONG_TIMEOUT
    scheduleRequestAndCollectServerListAttributesList(endpoint.toString(), time, fast)

    // collect the BasicInformation Cluster attributes
    time += fast ? SHORT_TIMEOUT : LONG_TIMEOUT
    String fingerprintName = getFingerprintName(endpoint)
    if (state[fingerprintName] == null) {
        logWarn "collectBasicInfo(): state.${fingerprintName} is null !"
        state[fingerprintName]
        return
    }
    List<String> serverList = state[fingerprintName]['ServerList']
    logDebug "collectBasicInfo(): endpoint=${endpoint}, fingerprintName=${fingerprintName}, serverList=${serverList} "

    if (endpoint == 0) {
        /* groovylint-disable-next-line ConstantIfExpression */
        if ('28' in serverList) {
            requestAndCollectAttributesValues(endpoint, cluster = 0x0028, time, fast) // Basic Information Cluster
            time += fast ? SHORT_TIMEOUT : LONG_TIMEOUT
        }
        else {
            logWarn "collectBasicInfo(): BasicInformationCluster 0x0028 endpoint:${endpoint} is <b>not in the ServerList !</b>"
        }
    }
    else {
        if ('39' in serverList) {
            requestAndCollectAttributesValues(endpoint, cluster = 0x0039, time, fast) // Bridged Device Basic Information Cluster
            time += fast ? SHORT_TIMEOUT : LONG_TIMEOUT
        }
        else {
            logWarn "collectBasicInfo(): BridgedDeviceBasicInformationCluster 0x0039 endpoint:${endpoint} is <b>not in the ServerList !</b>"
        }
    }
    runIn(time as int, 'delayedInfoEvent', [overwrite: true, data: [info: 'Basic Bridge Discovery finished', descriptionText: '']])
}

// TODO - refactor or remove?
void requestExtendedInfo(Integer endpoint = 0, Integer timePar = 15, boolean fast = false) {
    Integer time = timePar
    List<String> serverList = state[getFingerprintName(endpoint)]?.ServerList
    logWarn "requestExtendedInfo(): serverList:${serverList} endpoint=${endpoint} getFingerprintName = ${getFingerprintName(endpoint)}"
    if (serverList == null) {
        logWarn 'getInfo(): serverList is null!'
        return
    }
    serverList.each { cluster ->
        Integer clusterInt = HexUtils.hexStringToInt(cluster)
        if (endpoint != 0 && (clusterInt in [0x2E, 0x41])) {
            logWarn "requestExtendedInfo(): skipping endpoint ${endpoint}, cluster:${clusterInt} (0x${cluster}) - KNOWN TO CAUSE Zemismart M1 to crash !"
            return
        }
        logDebug "requestExtendedInfo(): endpointInt:${endpoint} (0x${HexUtils.integerToHexString(safeToInt(endpoint), 1)}),  clusterInt:${clusterInt} (0x${cluster}),  time:${time}"
        /* groovylint-disable-next-line ParameterReassignment */
        requestAndCollectAttributesValues(endpoint, clusterInt, time, fast = false)
        time += fast ? SHORT_TIMEOUT : LONG_TIMEOUT
    }

    runIn(time, 'delayedInfoEvent', [overwrite: true, data: [info: 'Extended Bridge Discovery finished', descriptionText: '']])
    logDebug "requestExtendedInfo(): jobs scheduled for total time: ${time} seconds"
}

void minimizeStateVariables(List<String> parameters) {
    logInfo "minimizeStateVariables(${parameters}) ..."
    List<String> stateKeys = state.keySet().collect { it }
    state.each { fingerprintName, fingerprintMap ->
        if (fingerprintName.startsWith('fingerprint')) {
            stateKeys.add(fingerprintName)
        }
    }
    stateKeys.each { stateKey ->
        if (stateKey.startsWith('fingerprint')) {
            state.remove(stateKey)
            logWarn "minimizeStateVariables(): removed stateKey=${stateKey}"
        }
    }
    state.remove('tmp')
    state.remove('stateMachines')
    stateKeys = null
}

void resetStats(List<String> parameters) {
    logInfo "resetStats(${parameters}) ..."
    state.stats = [:]
    // stats : {duplicatedCtr=0, pingsMax=288, rxCtr=264, pingsMin=80, pingsAvg=135, txCtr=51, pingsOK=6, pingsFail=1, initializeCtr=5}
    state.stats = [initializeCtr: 0, rxCtr: 0, txCtr: 0, duplicatedCtr: 0, pingsOK: 0, pingsFail: 0, pingsMin: 0, pingsMax: 0, pingsAvg: 0]
    sendMatterEvent([name: 'initializeCtr', value: state.stats['initializeCtr'], descriptionText: "${device.displayName} statistics were reset!", type: 'digital'])
}

void testParse(String par) {
    log.trace '------------------------------------------------------'
    log.warn "testParse - <b>START</b> (${par})"
    parse(par)
    log.warn "testParse -   <b>END</b> (${par})"
    log.trace '------------------------------------------------------'
}

void testMatter(String parameters) {
    log.warn "testMatter(${parameters})"
    /*
    String configureCmd = matter.configure()
    log.debug "testMatter(): configureCmd=${configureCmd}"
    sendToDevice(configureCmd)
    */

    List<Map<String, String>> eventPaths = []
    /*
//    eventPaths.add(matter.eventPath('1E', 0x003B, 0x00))
    eventPaths.add(matter.eventPath('24', 0x003B, 0x01))
//    eventPaths.add(matter.eventPath('1E', 0x003B, 0x02))
    eventPaths.add(matter.eventPath('24', 0x003B, 0x03))
//    eventPaths.add(matter.eventPath('1E', 0x003B, 0x04))
    eventPaths.add(matter.eventPath('24', 0x003B, 0x05))
    eventPaths.add(matter.eventPath('24', 0x003B, 0x06))
*/
    eventPaths.add(matter.eventPath('21', 0x003B, -1))
    //def xxx = matter.cleanSubscribe(0, 0xFFFF, eventPaths)
    def xxx = matter.subscribe(0, 0xFFFF, eventPaths)
    log.warn "testMatter(): sending : ${xxx}"
    //def xxx = 'he cleanSubscribe 0x00 0xFFFF [{"ep":"0xFFFFFFFF","cluster":"0xFFFFFFFF","evt":"0xFFFFFFFF", "priority": "1", "pri": "1"}]'
    sendToDevice(xxx)
}

/**
 * Decodes Matter TLV (Tag-Length-Value) encoded data
 * @param tlvHex The hex string containing TLV encoded data
 * @return List of decoded values
 */
List<Integer> decodeTLV(String tlvHex) {
    if (!tlvHex) {
        logWarn "decodeTLV: empty or null input"
        return []
    }
    
    logTrace "decodeTLV: parsing TLV data: ${tlvHex}"
    List<Integer> decodedValues = []
    
    try {
        // Convert hex string to bytes
        byte[] bytes = HexUtils.hexStringToByteArray(tlvHex)
        int pos = 0
        
        while (pos < bytes.length) {
            // Try multiple parsing approaches for robustness
            
            int controlByte = bytes[pos] & 0xFF
            //logTrace "decodeTLV: processing byte 0x${HexUtils.integerToHexString(controlByte, 1)} at position ${pos}"
            
            // Check for common end markers first
            if (controlByte == 0x18) {
                //logTrace "decodeTLV: found end marker 0x18"
                break
            }
            
            // Try parsing as simple attribute ID format first (common in attribute lists)
            if (controlByte == 0x24) {
                // This appears to be unsigned 16-bit values in the format we're seeing
                pos++ // skip control byte
                if (pos + 1 < bytes.length) {
                    int value = ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
                    decodedValues.add(value)
                    //logTrace "decodeTLV: decoded as 16-bit attribute: ${value} (0x${HexUtils.integerToHexString(value, 2)})"
                    pos += 2
                } else {
                    logWarn "decodeTLV: insufficient data for 16-bit value"
                    break
                }
            } else if (controlByte == 0x25) {
                // Another 16-bit format
                pos++ // skip control byte
                if (pos + 1 < bytes.length) {
                    int value = ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
                    decodedValues.add(value)
                    //logTrace "decodeTLV: decoded as 16-bit attribute: ${value} (0x${HexUtils.integerToHexString(value, 2)})"
                    pos += 2
                } else {
                    logWarn "decodeTLV: insufficient data for 16-bit value"
                    break
                }
            } else if (controlByte == 0x04) {
                // 8-bit unsigned value
                pos++
                if (pos < bytes.length) {
                    int value = bytes[pos] & 0xFF
                    decodedValues.add(value)
                    //logTrace "decodeTLV: decoded as 8-bit value: ${value}"
                    pos++
                } else {
                    logWarn "decodeTLV: insufficient data for 8-bit value"
                    break
                }
            } else if (controlByte == 0x05) {
                // 16-bit unsigned value (little endian)
                pos++
                if (pos + 1 < bytes.length) {
                    int value = ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
                    decodedValues.add(value)
                    //logTrace "decodeTLV: decoded as 16-bit value: ${value}"
                    pos += 2
                } else {
                    logWarn "decodeTLV: insufficient data for 16-bit value"
                    break
                }
            } else {
                // Extract element type and see if we can handle it
                int elementType = (controlByte >> 5) & 0x07
                int tagControl = (controlByte >> 3) & 0x03
                int lengthControl = controlByte & 0x07
                
                //logTrace "decodeTLV: controlByte=0x${HexUtils.integerToHexString(controlByte, 1)}, elementType=${elementType}, tagControl=${tagControl}, lengthControl=${lengthControl}"
                
                pos++ // advance past control byte
                
                // Skip tag field based on tag control
                if (tagControl == 1) {
                    // Context-specific tag (1 byte)
                    if (pos < bytes.length) {
                        int contextTag = bytes[pos] & 0xFF
                        //logTrace "decodeTLV: context tag: ${contextTag}"
                        pos++
                    } else {
                        logWarn "decodeTLV: unexpected end reading context tag"
                        break
                    }
                } else if (tagControl == 2) {
                    // Common profile tag (2 bytes)
                    if (pos + 1 < bytes.length) {
                        int tagValue = ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
                        //logTrace "decodeTLV: common profile tag: ${tagValue}"
                        pos += 2
                    } else {
                        logWarn "decodeTLV: unexpected end reading common profile tag"
                        break
                    }
                } else if (tagControl == 3) {
                    // Fully qualified tag (6 bytes) 
                    if (pos + 5 < bytes.length) {
                        //logTrace "decodeTLV: fully qualified tag (6 bytes)"
                        pos += 6
                    } else {
                        logWarn "decodeTLV: unexpected end reading fully qualified tag"
                        break
                    }
                }
                
                // Process value based on element type
                if (elementType == 0 || elementType == 1) {
                    // Signed or unsigned integer
                    int valueLength = getLengthFromControl(lengthControl)
                    if (valueLength > 0 && pos + valueLength <= bytes.length) {
                        int value = elementType == 0 ? readSignedInt(bytes, pos, valueLength) : readUnsignedInt(bytes, pos, valueLength)
                        decodedValues.add(value)
                        //logTrace "decodeTLV: decoded ${elementType == 0 ? 'signed' : 'unsigned'} int (${valueLength} bytes): ${value}"
                        pos += valueLength
                    } else {
                        //logTrace "decodeTLV: skipping element type ${elementType}, pos=${pos}, remaining=${bytes.length - pos}"
                        pos++ // skip one byte and continue
                    }
                } else {
                    // Skip other element types for now
                    //logTrace "decodeTLV: skipping element type ${elementType}"
                    if (lengthControl <= 3) {
                        int skipLength = getLengthFromControl(lengthControl)
                        pos += skipLength
                    } else {
                        pos++ // skip one byte
                    }
                }
            }
        }
        
        logTrace "decodeTLV: successfully decoded ${decodedValues.size()} values: ${decodedValues}"
        return decodedValues
        
    } catch (Exception e) {
        logWarn "decodeTLV: error decoding TLV data '${tlvHex}': ${e}"
        return []
    }
}

/**
 * Decodes TLV data and converts to hex strings (commonly used for attribute IDs)
 * @param tlvHex The hex string containing TLV encoded data
 * @return List of hex strings
 */
List<String> decodeTLVToHex(String tlvHex) {
    List<Integer> values = decodeTLV(tlvHex)
    return values.collect { HexUtils.integerToHexString(it, it > 255 ? 2 : 1).toUpperCase() }
}

void testDecodeTLV(List<String> parameters) {
    if (parameters == null || parameters.size() != 1) {
        logInfo 'usage: decodeTLV <tlvHexString>'
        logInfo 'example: decodeTLV 041D041E041F042804300431043304360437043C043E043F18'
        return
    }
    
    String tlvHex = parameters[0]
    logInfo "testDecodeTLV: decoding TLV hex string: ${tlvHex}"
    
    List<Integer> decodedInts = decodeTLV(tlvHex)
    List<String> decodedHex = decodeTLVToHex(tlvHex)
    
    logInfo "testDecodeTLV: decoded integers: ${decodedInts}"
    logInfo "testDecodeTLV: decoded hex strings: ${decodedHex}"
    
    // If this looks like an attribute list, show the attribute names too
    if (decodedHex.size() > 0) {
        logInfo "testDecodeTLV: attribute names (if applicable):"
        decodedHex.each { hexValue ->
            Integer attrInt = HexUtils.hexStringToInt(hexValue)
            String attrName = GlobalElementsAttributes[attrInt] ?: "Unknown"
            logInfo "  0x${hexValue} (${attrInt}) = ${attrName}"
        }
    }
}

/**
 * Helper method to get value length from length control field
 */
private int getLengthFromControl(int lengthControl) {
    switch (lengthControl) {
        case 0: return 1  // 1 byte
        case 1: return 2  // 2 bytes  
        case 2: return 4  // 4 bytes
        case 3: return 8  // 8 bytes
        default: return 0  // Variable or special length
    }
}

/**
 * Helper method to read signed integer values from TLV data
 */
private int readSignedInt(byte[] bytes, int pos, int length) {
    if (length == 1) {
        int value = bytes[pos] & 0xFF
        return (value & 0x80) ? value - 256 : value
    } else if (length == 2) {
        int value = ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
        return (value & 0x8000) ? value - 65536 : value
    } else if (length == 4) {
        return ((bytes[pos+3] & 0xFF) << 24) | ((bytes[pos+2] & 0xFF) << 16) | 
               ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
    }
    return 0
}

/**
 * Helper method to read unsigned integer values from TLV data
 */
private int readUnsignedInt(byte[] bytes, int pos, int length) {
    if (length == 1) {
        return bytes[pos] & 0xFF
    } else if (length == 2) {
        return ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
    } else if (length == 4) {
        return ((bytes[pos+3] & 0xFF) << 24) | ((bytes[pos+2] & 0xFF) << 16) | 
               ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
    }
    return 0
}

/**
 * Helper method to get string length from TLV data
 */
private int getStringLength(byte[] bytes, int pos, int lengthControl) {
    if (lengthControl == 0) {
        // 1-byte length
        if (pos < bytes.length) {
            return bytes[pos] & 0xFF
        }
    } else if (lengthControl == 1) {
        // 2-byte length
        if (pos + 1 < bytes.length) {
            return ((bytes[pos+1] & 0xFF) << 8) | (bytes[pos] & 0xFF)
        }
    }
    return -1
}

/**
 * Helper method to get how many bytes the string length field takes
 */
private int getStringLengthBytes(int lengthControl) {
    return lengthControl == 0 ? 1 : (lengthControl == 1 ? 2 : 0)
}
