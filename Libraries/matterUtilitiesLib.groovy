library(
    base: 'driver',
    author: 'Krassimir Kossev',
    category: 'matter',
    description: 'Matter Utilities Library',
    name: 'matterUtilitiesLib',
    namespace: 'kkossev',
    importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Libraries/matterUtilitiesLib.groovy',
    version: '1.3.4',
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
  * ver. 1.3.0  2025-06-28 Claude Sonnet 4  - added custom decodeTLVToHex() and decodeTLV()
  * ver. 1.3.1  2026-01-06 GPT-5.2 - added discoveryTimeoutScale
  * ver. 1.3.2  2026-01-26 GPT-5.2 - Minimal fix: make decodeTLVContainer() handle nested containers; reduced warning level logging
  * ver. 1.3.3  2026-02-18 kkossev   -(dev.branch)
  * ver. 1.3.4  2026-07-25 kkossev   -(dev.branch) - bug fixes;
  * 
*/

import groovy.transform.Field

/* groovylint-disable-next-line ImplicitReturnStatement */
@Field static final String matterUtilitiesLibVersion = '1.3.4'
@Field static final String matterUtilitiesLibStamp   = '2026/07/25 8:57 AM'

metadata {
    // no capabilities
    // no attributes
    command 'utilities', [[name:'command', type: 'STRING', description: 'for advanced users: ? for help', constraints: ['STRING']]]
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
    logDebug "readAttributeSafe(endpoint:${endpointId}, cluster:${clusterId}, attribute:${attrId}) -> starting readSingleAttrStateMachine!"

    readSingleAttrStateMachine([action: START, endpoint: endpointInt, cluster: clusterInt, attribute: attrInt])
}

void readAttribute(List<String> parameters) {
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
    String addOrRemove = parameters[0]
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
    sendInfoEvent('all subscriptions are removed!', 're-discover the devices again ...')
}

void removeAllDevices(List<String> parameters) {
    logInfo "Removing all child devices ${parameters}"
    removeAllDevices()
}

boolean utilities(String commandLine=null) {
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

// NOTE: the former 'timePar' and 'fast' parameters are gone - the collector is now driven by the replies,
// not by a fixed schedule, so there is nothing left to tune per call. See infoCollectStateMachine() below.
void collectBasicInfo(Integer endpoint = 0) {
    // Descriptor first (it fills in the ServerList), then the attribute lists of every ServerList cluster,
    // then the BasicInformation / BridgedDeviceBasicInformation cluster - resolved once the ServerList is known.
    startInfoCollect(endpoint, ['001D', 'SERVERLIST', 'BASICINFO'], 'Basic Bridge Discovery finished')
}

void requestExtendedInfo(Integer endpoint = 0) {
    List<String> serverList = state[getFingerprintName(endpoint)]?.ServerList
    if (serverList == null) {
        logWarn 'requestExtendedInfo(): serverList is null!'
        return
    }
    List<String> queue = []
    serverList.each { String cluster ->
        Integer clusterInt = safeHexToInt(cluster, -1)
        if (clusterInt < 0) { return }
        if (endpoint != 0 && (clusterInt in [0x2E, 0x41])) {
            logWarn "requestExtendedInfo(): skipping endpoint ${endpoint}, cluster:${clusterInt} (0x${cluster}) - KNOWN TO CAUSE Zemismart M1 to crash !"
            return
        }
        queue.add(cluster)
    }
    startInfoCollect(endpoint, queue, 'Extended Bridge Discovery finished')
}

/*
 * ---------------------------------------------------------------------------------------------------------
 *  getInfo() collector state machine
 *
 *  Replaces the original fixed runIn() schedule, which waited 3*scale seconds before reading the values and
 *  12*scale seconds before printing them, for EVERY cluster - a 'Basic' run took ~91 seconds at scale 2, of
 *  which about 2 seconds was actual Matter traffic. Every step now advances as soon as the reply is seen by
 *  checkStateMachineConfirmation(); the old delays survive only as the timeout safety net.
 *
 *  The queue holds 4-char cluster hex strings plus two tokens:
 *      'SERVERLIST' - read the AttributeList (0xFFFB) of every cluster in the endpoint's ServerList
 *      'BASICINFO'  - resolved at run time to 0028 (the bridge) or 0039 (a bridged device), which can only
 *                     be decided after the Descriptor has actually been read.
 *
 *  NOTE: state.states['isInfo'], state.states['cluster'] and state.tmp are owned by
 *  requestMatterClusterAttributesList() / logRequestedClusterAttrResult() - this machine must not set them.
 * ---------------------------------------------------------------------------------------------------------
 */
@Field static final Integer INFO_COLLECT_PERIOD    = 300    // milliseconds between the state machine ticks
// Per-step timeout, multiplied by discoveryTimeoutScale : ~5s at 1x, ~10s at 2x, ~15s at 3x.
// Keep it modest - a 'Basic' run has 5 waiting steps, so this is the worst case divided by 5. A bridge that
// answers a single attribute read in more than 5 seconds is unreachable for practical purposes anyway
// (compare MAX_PING_MILISECONDS = 15000 in the parent driver).
@Field static final Integer INFO_COLLECT_MAX_TICKS = 17

@Field static final Integer INFO_STATE_IDLE             = 0
@Field static final Integer INFO_STATE_NEXT             = 1
@Field static final Integer INFO_STATE_ATTR_LIST        = 2
@Field static final Integer INFO_STATE_ATTR_LIST_WAIT   = 3
@Field static final Integer INFO_STATE_VALUES           = 4
@Field static final Integer INFO_STATE_VALUES_WAIT      = 5
@Field static final Integer INFO_STATE_SERVER_LIST      = 6
@Field static final Integer INFO_STATE_SERVER_LIST_WAIT = 7
@Field static final Integer INFO_STATE_END              = 99

void startInfoCollect(Integer endpoint, List<String> queue, String finishedText) {
    if (state['stateMachines'] == null) { state['stateMachines'] = [:] }
    if (state['states'] == null) { state['states'] = [:] }
    if (queue == null || queue.isEmpty()) {
        logWarn 'startInfoCollect(): nothing to collect!'
        return
    }
    state['states']['isPing'] = false
    state['stateMachines']['infoEndpoint'] = endpoint
    state['stateMachines']['infoQueue'] = queue
    state['stateMachines']['infoIndex'] = 0
    state['stateMachines']['infoFinished'] = finishedText
    state['stateMachines']['infoState'] = INFO_STATE_NEXT
    state['stateMachines']['infoRetry'] = 0
    logDebug "startInfoCollect(): endpoint=${endpoint} queue=${queue}"
    unschedule('infoCollectStateMachine')
    runInMillis(INFO_COLLECT_PERIOD, 'infoCollectStateMachine', [overwrite: true])
}

void infoCollectStateMachine() {
    if (state['stateMachines'] == null) { state['stateMachines'] = [:] }
    Integer st = safeToInt(state['stateMachines']['infoState'], INFO_STATE_IDLE)
    Integer retry = safeToInt(state['stateMachines']['infoRetry'], 0)
    Integer endpoint = safeToInt(state['stateMachines']['infoEndpoint'], 0)
    Integer index = safeToInt(state['stateMachines']['infoIndex'], 0)
    List<String> queue = (state['stateMachines']['infoQueue'] ?: []) as List
    Integer maxTicks = INFO_COLLECT_MAX_TICKS * getDiscoveryTimeoutScale()
    String entry = (index >= 0 && index < queue.size()) ? queue[index] : null
    Integer entryCluster = (entry != null) ? safeHexToInt(entry, -1) : -1
    boolean confirmed = (state['stateMachines']['Confirmation'] == true)
    logTrace "infoCollectStateMachine: st:${st} retry:${retry} index:${index} entry:${entry}"

    switch (st) {
        case INFO_STATE_NEXT :
            if (entry == null) { st = INFO_STATE_END; break }
            if (entry == 'SERVERLIST') { st = INFO_STATE_SERVER_LIST; break }
            if (entry == 'BASICINFO') {
                // the ServerList is known only now, after the Descriptor has been read
                String wanted = (endpoint == 0) ? '0028' : '0039'
                List<String> knownServerList = state[getFingerprintName(endpoint)]?.ServerList ?: []
                if (!(wanted in knownServerList)) {
                    logWarn "collectBasicInfo(): cluster 0x${wanted} is <b>not in the ServerList</b> of endpoint ${endpoint} !"
                    state['stateMachines']['infoIndex'] = index + 1
                    break       // stays in INFO_STATE_NEXT
                }
                queue[index] = wanted
                state['stateMachines']['infoQueue'] = queue
            }
            st = INFO_STATE_ATTR_LIST
            break
        case INFO_STATE_ATTR_LIST :
            if (entryCluster < 0) {
                logWarn "infoCollectStateMachine: invalid cluster '${entry}' - skipped"
                state['stateMachines']['infoIndex'] = index + 1
                st = INFO_STATE_NEXT
                break
            }
            state['stateMachines']['toBeConfirmed'] = [endpoint, entryCluster, 0xFFFB]
            state['stateMachines']['Confirmation'] = false
            requestMatterClusterAttributesList([endpoint: endpoint, cluster: entryCluster])
            retry = 0; st = INFO_STATE_ATTR_LIST_WAIT
            break
        case INFO_STATE_ATTR_LIST_WAIT :
            if (confirmed) {
                st = INFO_STATE_VALUES
            }
            else {
                retry++
                if (retry > maxTicks) {
                    logWarn "infoCollectStateMachine: timeout waiting for the AttributeList of cluster ${entry} (endpoint ${endpoint})"
                    st = INFO_STATE_VALUES      // try the values anyway - an older AttributeList may still be in the state
                }
            }
            break
        case INFO_STATE_VALUES :
            state['stateMachines']['Confirmation'] = false
            Integer lastAttr = requestMatterClusterAttributesValues([endpoint: endpoint, cluster: entryCluster])
            if (lastAttr == null) {
                logWarn "infoCollectStateMachine: no attributes to read for cluster ${entry} (endpoint ${endpoint})"
                logRequestedClusterAttrResult([endpoint: endpoint, cluster: entryCluster])
                state['stateMachines']['infoIndex'] = index + 1
                retry = 0; st = INFO_STATE_NEXT
                break
            }
            state['stateMachines']['toBeConfirmed'] = [endpoint, entryCluster, lastAttr]
            retry = 0; st = INFO_STATE_VALUES_WAIT
            break
        case INFO_STATE_VALUES_WAIT :
            if (!confirmed) {
                retry++
                if (retry <= maxTicks) { break }
                logWarn "infoCollectStateMachine: timeout waiting for the attribute values of cluster ${entry} (endpoint ${endpoint}) - logging what was received"
            }
            logRequestedClusterAttrResult([endpoint: endpoint, cluster: entryCluster])
            state['stateMachines']['infoIndex'] = index + 1
            retry = 0; st = INFO_STATE_NEXT
            break
        case INFO_STATE_SERVER_LIST :
            List<String> burstList = state[getFingerprintName(endpoint)]?.ServerList ?: []
            if (burstList.isEmpty()) {
                logWarn "infoCollectStateMachine: the ServerList of endpoint ${endpoint} is empty - skipping the attribute lists"
                state['stateMachines']['infoIndex'] = index + 1
                st = INFO_STATE_NEXT
                break
            }
            // requestAndCollectServerListAttributesList() reads 0xFFFB of each cluster, in the ServerList order
            state['stateMachines']['toBeConfirmed'] = [endpoint, safeHexToInt(burstList.last(), 0), 0xFFFB]
            state['stateMachines']['Confirmation'] = false
            requestAndCollectServerListAttributesList([endpointPar: endpoint.toString()])
            retry = 0; st = INFO_STATE_SERVER_LIST_WAIT
            break
        case INFO_STATE_SERVER_LIST_WAIT :
            if (!confirmed) {
                retry++
                if (retry <= maxTicks) { break }
                logWarn "infoCollectStateMachine: timeout waiting for the ServerList attribute lists (endpoint ${endpoint})"
            }
            state['stateMachines']['infoIndex'] = index + 1
            retry = 0; st = INFO_STATE_NEXT
            break
        case INFO_STATE_END :
            state['states']['isInfo'] = false
            sendInfoEvent(state['stateMachines']['infoFinished'] ?: 'Bridge Discovery finished')
            st = INFO_STATE_IDLE
            break
        default :
            state['states']['isInfo'] = false
            st = INFO_STATE_IDLE
            break
    }

    state['stateMachines']['infoState'] = st
    state['stateMachines']['infoRetry'] = retry
    if (st != INFO_STATE_IDLE) {
        runInMillis(INFO_COLLECT_PERIOD, 'infoCollectStateMachine', [overwrite: true])
    }
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
            logDebug "minimizeStateVariables(): removed stateKey=${stateKey}"
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
    sendEvent([name: 'initializeCtr', value: state.stats['initializeCtr'], descriptionText: "${device.displayName} statistics were reset!", type: 'digital', isStateChange: true])
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
