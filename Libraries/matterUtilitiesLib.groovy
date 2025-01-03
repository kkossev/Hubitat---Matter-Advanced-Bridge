/* groovylint-disable CompileStatic, DuplicateNumberLiteral, DuplicateStringLiteral, ImplicitClosureParameter, LineLength */
library(
    base: 'driver',
    author: 'Krassimir Kossev',
    category: 'matter',
    description: 'Matter Utilities Library',
    name: 'matterUtilitiesLib',
    namespace: 'kkossev',
    importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Libraries/matterUtilitiesLib.groovy',
    version: '1.2.0',
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
  * ver. 1.2.0  2024-10-11 kkossev  - (dev.branch) added testParse()
*/

import groovy.transform.Field

/* groovylint-disable-next-line ImplicitReturnStatement */
@Field static final String matterUtilitiesLibVersion = '1.2.0'
@Field static final String matterUtilitiesLibStamp   = '2024/10/11 9:46 PM'

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
