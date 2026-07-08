 /*
  *  'Matter Generic Component Camera AV Stream' - component driver for Matter Advanced Bridge
  *
  *  https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009
  *
  *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  *  in compliance with the License. You may obtain a copy of the License at:
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
  *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
  *  for the specific language governing permissions and limitations under the License.
  *
  * ver. 1.0.0  2026-05-30 kkossev + Claude Sonnet 4.6 : first version - Aqara G350 Matter Camera AV Stream Management cluster 0x0551 support
  *
  *             TODO: snapshot capture workflow (CaptureSnapshot 0x000C + CaptureSnapshotResponse 0x000D)
  *             TODO: privacy mode controls if attributes 0x0013/0x0014/0x0015 appear in AttributeList
  *             TODO: image controls if attributes 0x0022/0x0023/0x0024 appear in AttributeList
  *             TODO: status light controls if attributes 0x0027/0x0028 appear in AttributeList
  *
*/

import groovy.transform.Field
import hubitat.helper.HexUtils
import hubitat.matter.DataType

@Field static final String CAMERA_DRIVER_VERSION = '1.0.0'
@Field static final String CAMERA_DRIVER_STAMP   = '2026/05/30 12:00 AM'

@Field static final Boolean _DEBUG_CAMERA   = false         // set true only for development
@Field static final Boolean _DEFAULT_LOG_ENABLE = false     // disable on production

// --------------------------------------------------------------------------------------------
// Full attribute name map (for human-readable log output in child driver)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_ATTR_NAMES = [
    0x0000  : 'MaxConcurrentEncoders',
    0x0001  : 'MaxEncodedPixelRate',
    0x0002  : 'VideoSensorParams',
    0x0003  : 'NightVisionUsesInfrared',
    0x0004  : 'MinViewport',
    0x0005  : 'RateDistortionTradeOffPoints',
    0x0006  : 'MaxContentBufferSize',
    0x0007  : 'MicrophoneCapabilities',
    0x0008  : 'SpeakerCapabilities',
    0x0009  : 'TwoWayTalkSupport',
    0x000A  : 'SnapshotCapabilities',
    0x000B  : 'MaxNetworkBandwidth',
    0x000C  : 'CurrentFrameRate',
    0x000E  : 'SupportedStreamUsages',
    0x000F  : 'AllocatedVideoStreams',
    0x0010  : 'AllocatedAudioStreams',
    0x0011  : 'AllocatedSnapshotStreams',
    0x0012  : 'StreamUsagePriorities',
    0x0016  : 'NightVision',
    0x0018  : 'Viewport',
    0x0019  : 'SpeakerMuted',
    0x001A  : 'SpeakerVolumeLevel',
    0x001B  : 'SpeakerMaxLevel',
    0x001C  : 'SpeakerMinLevel',
    0x001D  : 'MicrophoneMuted',
    0x001E  : 'MicrophoneVolumeLevel',
    0x001F  : 'MicrophoneMaxLevel',
    0x0020  : 'MicrophoneMinLevel',
    // Global cluster attributes
    0xFFF8  : 'GeneratedCommandList',
    0xFFF9  : 'AcceptedCommandList',
    0xFFFB  : 'AttributeList',
    0xFFFC  : 'FeatureMap',
    0xFFFD  : 'ClusterRevision'
]

// --------------------------------------------------------------------------------------------
// Accepted commands (0x0551 AcceptedCommandList for G350)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_ACCEPTED_COMMANDS = [
    0x0000  : 'AudioStreamAllocate',
    0x0002  : 'AudioStreamDeallocate',
    0x0003  : 'VideoStreamAllocate',
    0x0006  : 'VideoStreamDeallocate',
    0x0007  : 'SnapshotStreamAllocate',
    0x000A  : 'SnapshotStreamDeallocate',
    0x000B  : 'SetStreamPriorities',
    0x000C  : 'CaptureSnapshot'
]

// --------------------------------------------------------------------------------------------
// Generated commands (0x0551 GeneratedCommandList for G350)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_GENERATED_COMMANDS = [
    0x0001  : 'AudioStreamAllocateResponse',
    0x0004  : 'VideoStreamAllocateResponse',
    0x0008  : 'SnapshotStreamAllocateResponse',
    0x000D  : 'CaptureSnapshotResponse'
]

// --------------------------------------------------------------------------------------------
// FeatureMap bit definitions (0x0551 FeatureMap) — Matter 1.3 spec
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_FEATURE_BITS = [
    0   : 'Video',              // VID — video stream management
    1   : 'Audio',              // AUD — audio stream management
    2   : 'Speaker',            // SPK — speaker control
    3   : 'Privacy',            // PRI — privacy mode
    4   : 'NightVision',        // NV  — night vision
    5   : 'NightVisionIllum',   // NVI — night vision illumination control
    6   : 'Snapshot',           // SS  — snapshot capture
    7   : 'IRLEDs',             //      — IR LED control
    8   : 'VideoStreamCurrent', // VSC — current video stream attributes
    10  : 'TwoWayTalk'          // TWT — two-way talk
]

// --------------------------------------------------------------------------------------------
// NightVision enum (0x0016 NightVision attribute)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_NIGHT_VISION_ENUM = [
    0   : 'Off',
    1   : 'On',
    2   : 'Auto'
]

// --------------------------------------------------------------------------------------------
// TwoWayTalkSupport enum (0x0009 TwoWayTalkSupport attribute)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_TWO_WAY_TALK_ENUM = [
    0   : 'HalfDuplex',
    1   : 'FullDuplex'
]

// --------------------------------------------------------------------------------------------
// metadata
// --------------------------------------------------------------------------------------------
metadata {
    definition(
        name: 'Matter Generic Component Camera AV Stream',
        namespace: 'kkossev',
        author: 'Krassimir Kossev',
        importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Camera_AV_Stream.groovy'
    ) {
        capability 'Sensor'
        capability 'Refresh'
        capability 'AudioVolume'    // mute (ENUM ["unmuted","muted"]), volume (NUMBER, %) | mute(), unmute(), setVolume(level), volumeUp(), volumeDown()

        // Only frequently-changing user-facing attributes declared as Hubitat attributes.
        // AudioVolume capability provides: mute + volume (speaker).
        // All other camera attributes stored in state.cameraAttr (same pattern as Door Lock state.lockAttr).
        attribute 'microphoneMuted',  'enum', ['muted', 'unmuted']
        attribute 'microphoneVolume', 'number'
        attribute 'nightVision',      'enum', ['Off', 'On', 'Auto']

        command 'setSpeakerMuted',   [[name: 'Muted*',  type: 'ENUM',   constraints: ['true', 'false'], description: 'Mute or unmute speaker']]
        command 'setSpeakerVolume',  [[name: 'Level*',  type: 'NUMBER', description: 'Speaker volume (0-100)']]
        command 'setMicrophoneMuted',[[name: 'Muted*',  type: 'ENUM',   constraints: ['true', 'false'], description: 'Mute or unmute microphone']]
        command 'setMicrophoneVolume',[[name: 'Level*', type: 'NUMBER', description: 'Microphone volume (0-100)']]
        command 'cameraSnapshotDiagnostics', [[name: 'Read snapshot capabilities and log support status']]
        command 'getInfo', [[name: 'Read all CameraAvStreamManagement attributes and log a summary']]

        if (_DEBUG_CAMERA) {
            command 'testCameraRead', [[name: 'attrHex', type: 'STRING', description: 'Hex attribute ID to read (e.g. 001A)', defaultValue: '001A']]
        }
    }

    preferences {
        section {
            input name: 'helpInfo', type: 'hidden', title: fmtHelpInfo('Community Link')
            input name: 'logEnable',
                  type: 'bool',
                  title: '<b>Enable debug logging</b>',
                  required: false,
                  defaultValue: _DEBUG_CAMERA ?: false
            input name: 'txtEnable',
                  type: 'bool',
                  title: '<b>Enable descriptionText logging</b>',
                  required: false,
                  defaultValue: true
        }
    }
}

// --------------------------------------------------------------------------------------------
// Lifecycle
// --------------------------------------------------------------------------------------------
void installed() {
    logInfo "installed() driver version ${CAMERA_DRIVER_VERSION}"
    initCameraAttr()
}

void uninstalled() {
    logInfo 'uninstalled()'
}

void updated() {
    logInfo "updated() driver version ${CAMERA_DRIVER_VERSION}"
    if (settings.logEnable) { runIn(86400, logsOff) }
}

private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName}"
    device.updateSetting('logEnable', [value: 'false', type: 'bool'])
}

// --------------------------------------------------------------------------------------------
// parse() — receives events forwarded from parent driver
// --------------------------------------------------------------------------------------------
void parse(String description) { log.warn 'parse(String description) not implemented' }

void parse(List<Map> parsedEvents) {
    parsedEvents.each { d ->
        if (d.name == 'rtt') {
            parseRttEvent(d)
        } else if (d.name in ['handleInChildDriver', 'unprocessed']) {
            handleCameraMessage(d)
        } else {
            if (d.descriptionText) { logInfo "${d.descriptionText}" }
            sendEvent(d)
        }
    }
}

// --------------------------------------------------------------------------------------------
// handleCameraMessage — entry point for cluster 0x0551 reports
// --------------------------------------------------------------------------------------------
void handleCameraMessage(Map description) {
    Map descMap = [:]
    try {
        descMap = description.value as Map
    } catch (e) {
        logWarn "handleCameraMessage: exception ${e} while parsing description.value = ${description.value}"
        return
    }
    logDebug "handleCameraMessage: descMap = ${descMap}"
    if (descMap.cluster != '0551') {
        logWarn "handleCameraMessage: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"
        return
    }
    processCameraAttributeReport(descMap)
}

// --------------------------------------------------------------------------------------------
// processCameraAttributeReport — decode individual attributes
// --------------------------------------------------------------------------------------------
void processCameraAttributeReport(Map descMap) {
    initCameraAttr()
    String attrId  = descMap.attrId ?: ''
    Object rawVal  = descMap.value
    Integer attrInt = descMap.attrInt != null ? (descMap.attrInt as Integer) : safeHexToInt(attrId)
    String attrName = CAMERA_ATTR_NAMES[attrInt] ?: "0x${attrId}"
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${attrId}] " : ''
    String message = null
    boolean useDebugLog = false

    switch (attrId) {
        // ----- Global cluster metadata -----
        case 'FFFC': // FeatureMap
            Integer featureMap = safeToInt(rawVal)
            String decoded = decodeCameraFeatureMap(featureMap)
            state.cameraAttr['featureMap']    = decoded
            state.cameraAttr['featureMapRaw'] = featureMap
            message = "${prefix}FeatureMap=0x${HexUtils.integerToHexString(featureMap, 2)} ${decoded}"
            break
        case 'FFFD': // ClusterRevision
            Integer rev = safeToInt(rawVal)
            state.cameraAttr['clusterRevision'] = rev
            message = "${prefix}ClusterRevision=${rev}"
            break
        case 'FFF9': // AcceptedCommandList
            List<String> acceptedNames = decodeCameraCommandList(rawVal, CAMERA_ACCEPTED_COMMANDS)
            state.cameraAttr['acceptedCommandIds'] = normalizeCameraUintList(rawVal)
            state.cameraAttr['acceptedCommands']   = acceptedNames.toString()
            message = "${prefix}AcceptedCommands=${acceptedNames}"
            break
        case 'FFF8': // GeneratedCommandList
            List<String> generatedNames = decodeCameraCommandList(rawVal, CAMERA_GENERATED_COMMANDS)
            state.cameraAttr['generatedCommandIds'] = normalizeCameraUintList(rawVal)
            state.cameraAttr['generatedCommands']   = generatedNames.toString()
            message = "${prefix}GeneratedCommands=${generatedNames}"
            break
        case 'FFFB': // AttributeList
            state.cameraAttr['attributeList'] = rawVal?.toString()
            message = "${prefix}AttributeList=${rawVal}"
            useDebugLog = true
            break
        // ----- Speaker controls -----
        case '0019': // SpeakerMuted (BOOL) → AudioVolume 'mute' attribute
            String speakerMutedStr = parseBooleanValue(rawVal)
            state.cameraAttr['speakerMuted'] = speakerMutedStr
            String muteVal = (speakerMutedStr == 'true') ? 'muted' : 'unmuted'
            sendEvent(name: 'mute', value: muteVal, descriptionText: "${device.displayName} mute is ${muteVal}", type: 'physical')
            message = "${prefix}SpeakerMuted=${speakerMutedStr} (mute=${muteVal})"
            break
        case '001A': // SpeakerVolumeLevel → AudioVolume 'volume' attribute
            Integer speakerVol = safeToInt(rawVal)
            sendEvent(name: 'volume', value: speakerVol, unit: '%', descriptionText: "${device.displayName} volume is ${speakerVol}%", type: 'physical')
            message = "${prefix}SpeakerVolumeLevel=${speakerVol}"
            break
        case '001B': // SpeakerMaxLevel
            state.cameraAttr['speakerMaxLevel'] = safeToInt(rawVal)
            message = "${prefix}SpeakerMaxLevel=${rawVal}"
            useDebugLog = true
            break
        case '001C': // SpeakerMinLevel
            state.cameraAttr['speakerMinLevel'] = safeToInt(rawVal)
            message = "${prefix}SpeakerMinLevel=${rawVal}"
            useDebugLog = true
            break
        // ----- Microphone controls -----
        case '001D': // MicrophoneMuted → microphoneMuted attribute
            String micMutedStr = parseBooleanValue(rawVal)
            String micMuteVal = (micMutedStr == 'true') ? 'muted' : 'unmuted'
            sendEvent(name: 'microphoneMuted', value: micMuteVal, descriptionText: "${device.displayName} microphoneMuted is ${micMuteVal}", type: 'physical')
            message = "${prefix}MicrophoneMuted=${micMuteVal}"
            break
        case '001E': // MicrophoneVolumeLevel → microphoneVolume attribute
            Integer micVol = safeToInt(rawVal)
            sendEvent(name: 'microphoneVolume', value: micVol, descriptionText: "${device.displayName} microphoneVolume is ${micVol}", type: 'physical')
            message = "${prefix}MicrophoneVolumeLevel=${micVol}"
            break
        case '001F': // MicrophoneMaxLevel
            state.cameraAttr['microphoneMaxLevel'] = safeToInt(rawVal)
            message = "${prefix}MicrophoneMaxLevel=${rawVal}"
            useDebugLog = true
            break
        case '0020': // MicrophoneMinLevel
            state.cameraAttr['microphoneMinLevel'] = safeToInt(rawVal)
            message = "${prefix}MicrophoneMinLevel=${rawVal}"
            useDebugLog = true
            break
        // ----- NightVision -----
        case '0016': // NightVision → nightVision attribute
            Integer nvVal = safeToInt(rawVal)
            String nvDecoded = decodeCameraEnum(nvVal, CAMERA_NIGHT_VISION_ENUM)
            sendEvent(name: 'nightVision', value: nvDecoded, descriptionText: "${device.displayName} nightVision is ${nvDecoded}", type: 'physical')
            message = "${prefix}NightVision=${nvDecoded} (raw=${rawVal})"
            break
        // ----- Capability discovery -----
        case '0009': // TwoWayTalkSupport
            Integer twtVal = safeToInt(rawVal)
            state.cameraAttr['twoWayTalkSupport'] = decodeCameraEnum(twtVal, CAMERA_TWO_WAY_TALK_ENUM)
            message = "${prefix}TwoWayTalkSupport=${state.cameraAttr['twoWayTalkSupport']} (raw=${rawVal})"
            break
        case '000A': // SnapshotCapabilities
            state.cameraAttr['snapshotCapabilities'] = rawVal?.toString()
            message = "${prefix}SnapshotCapabilities=${rawVal}"
            break
        case '0011': // AllocatedSnapshotStreams
            state.cameraAttr['allocatedSnapshotStreams'] = rawVal?.toString()
            message = "${prefix}AllocatedSnapshotStreams=${rawVal}"
            useDebugLog = true
            break
        default:
            // Store all other attributes in state.cameraAttr for diagnostics
            state.cameraAttr[attrName] = rawVal
            message = "${prefix}${attrName}=${rawVal} (stored in state.cameraAttr)"
            useDebugLog = true
            break
    }

    if (message != null) {
        if (isInfoMode) {
            state.states.infoBuffer = (state.states?.infoBuffer ?: []) + [message]
            List<String> remaining = state.states?.infoExpectedAttrs ?: []
            remaining.remove(attrId)
            state.states.infoExpectedAttrs = remaining
            if (!remaining.isEmpty()) { return }
            flushInfoBuffer()
            return
        } else {
            if (useDebugLog) { logDebug "Camera AV Stream Management: ${message}" }
            else             { logInfo  "Camera AV Stream Management: ${message}" }
        }
    }
}

// --------------------------------------------------------------------------------------------
// Write command implementations — direct Matter sends via parent?.sendToDevice()
// Pattern: Lock driver's lock() / matterClearCredential() etc.
// --------------------------------------------------------------------------------------------

// AudioVolume capability commands — delegate to speaker write logic
void mute()                          { setSpeakerMuted('true')  }
void unmute()                        { setSpeakerMuted('false') }
void setVolume(BigDecimal volumelevel) { setSpeakerVolume(volumelevel) }
void volumeUp()   { setSpeakerVolume(((device.currentValue('volume') ?: 50) as Integer) + 10) }
void volumeDown() { setSpeakerVolume(((device.currentValue('volume') ?: 50) as Integer) - 10) }

void setSpeakerMuted(String mutedParam) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    String muted = parseBooleanValue(mutedParam)
    logInfo "setSpeakerMuted: ${muted} (experimental Matter camera write)"
    Integer boolType = (muted == 'true') ? DataType.BOOLEAN_TRUE : DataType.BOOLEAN_FALSE
    List<Map<String, String>> reqs = [matter.attributeWriteRequest(deviceNumber, 0x0551, 0x0019, boolType, '')]
    parent?.sendToDevice(matter.writeAttributes(reqs))
    //runIn(2, 'readSpeakerMutedAttr')
}

void setSpeakerVolume(BigDecimal level) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    Integer min = state.cameraAttr?.speakerMinLevel != null ? (state.cameraAttr.speakerMinLevel as Integer) : 0
    Integer max = state.cameraAttr?.speakerMaxLevel != null ? (state.cameraAttr.speakerMaxLevel as Integer) : 100
    Integer clamped = Math.max(min, Math.min(max, level as Integer))
    logInfo "setSpeakerVolume: ${clamped} (range ${min}..${max}, experimental Matter camera write)"
    List<Map<String, String>> reqs = [matter.attributeWriteRequest(deviceNumber, 0x0551, 0x001A, DataType.UINT8, HexUtils.integerToHexString(clamped, 1))]
    parent?.sendToDevice(matter.writeAttributes(reqs))
    //runIn(2, 'readSpeakerVolumeAttr')
}

void setMicrophoneMuted(String mutedParam) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    String muted = parseBooleanValue(mutedParam)
    logInfo "setMicrophoneMuted: ${(muted == 'true') ? 'muted' : 'unmuted'} (experimental Matter camera write)"
    Integer boolType = (muted == 'true') ? DataType.BOOLEAN_TRUE : DataType.BOOLEAN_FALSE
    List<Map<String, String>> reqs = [matter.attributeWriteRequest(deviceNumber, 0x0551, 0x001D, boolType, '')]
    parent?.sendToDevice(matter.writeAttributes(reqs))
    //runIn(2, 'readMicrophoneMutedAttr')
}

void setMicrophoneVolume(BigDecimal level) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    Integer min = state.cameraAttr?.microphoneMinLevel != null ? (state.cameraAttr.microphoneMinLevel as Integer) : 0
    Integer max = state.cameraAttr?.microphoneMaxLevel != null ? (state.cameraAttr.microphoneMaxLevel as Integer) : 100
    Integer clamped = Math.max(min, Math.min(max, level as Integer))
    logInfo "setMicrophoneVolume: ${clamped} (range ${min}..${max}, experimental Matter camera write)"
    List<Map<String, String>> reqs = [matter.attributeWriteRequest(deviceNumber, 0x0551, 0x001E, DataType.UINT8, HexUtils.integerToHexString(clamped, 1))]
    parent?.sendToDevice(matter.writeAttributes(reqs))
    //runIn(2, 'readMicrophoneVolumeAttr')
}

// --------------------------------------------------------------------------------------------
// Readback helpers — called via runIn() after writes
// --------------------------------------------------------------------------------------------
private void readSpeakerMutedAttr() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0551, 0x0019)]))
}

private void readSpeakerVolumeAttr() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0551, 0x001A)]))
}

private void readMicrophoneMutedAttr() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0551, 0x001D)]))
}

private void readMicrophoneVolumeAttr() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0551, 0x001E)]))
}

// --------------------------------------------------------------------------------------------
// cameraSnapshotDiagnostics — read snapshot capability attrs and log support summary
// --------------------------------------------------------------------------------------------
void cameraSnapshotDiagnostics() {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    logInfo 'cameraSnapshotDiagnostics: reading snapshot capability attributes...'
    List<Map<String, String>> paths = [0x000A, 0x0011, 0xFFF9, 0xFFF8].collect { Integer attr ->
        matter.attributePath(deviceNumber, 0x0551, attr)
    }
    parent?.sendToDevice(matter.readAttributes(paths))
    runIn(5, 'logSnapshotDiagnostics')
}

void logSnapshotDiagnostics() {
    List accepted  = state.cameraAttr?.acceptedCommandIds  ?: []
    List generated = state.cameraAttr?.generatedCommandIds ?: []
    logInfo '--- Camera Snapshot Diagnostics ---'
    logInfo "  SnapshotStreamAllocate  (0x0007): ${(7  in accepted)  ? 'supported'  : 'NOT supported'}"
    logInfo "  CaptureSnapshot         (0x000C): ${(12 in accepted)  ? 'supported'  : 'NOT supported'}"
    logInfo "  SnapshotStreamAllocateResponse (0x0008): ${(8  in generated) ? 'generated' : 'NOT generated'}"
    logInfo "  CaptureSnapshotResponse        (0x000D): ${(13 in generated) ? 'generated' : 'NOT generated'}"
    logInfo "  AllocatedSnapshotStreams: ${state.cameraAttr?.allocatedSnapshotStreams ?: '(not read)'}"
    logInfo "  SnapshotCapabilities:    ${state.cameraAttr?.snapshotCapabilities    ?: '(not read)'}"
    logInfo '-----------------------------------'
}

// --------------------------------------------------------------------------------------------
// Debug test command
// --------------------------------------------------------------------------------------------
void testCameraRead(String attrHexStr) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    Integer attrId = safeHexToInt(attrHexStr)
    logDebug "testCameraRead: reading ep=${HexUtils.integerToHexString(deviceNumber, 1)} cluster=0551 attr=${attrHexStr}"
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(deviceNumber, 0x0551, attrId)]))
}

// --------------------------------------------------------------------------------------------
// refresh
// --------------------------------------------------------------------------------------------
void refresh() {
    logInfo 'refresh(): requesting subscribed camera attributes from parent...'
    parent?.componentRefresh(this.device)
}

// --------------------------------------------------------------------------------------------
// getInfo — read all camera attributes and log a human-readable summary
// --------------------------------------------------------------------------------------------
void getInfo() {
    if (!isClusterSupported('0551')) {
        logWarn "getInfo: CameraAvStreamManagement cluster (0x0551) is not supported by this device"
        logInfo "getInfo: ServerList contains: ${getServerList()}"
        return
    }
    List<String> expectedAttrs = getCameraAttributeList()
    logInfo "getInfo: reading all supported CameraAvStreamManagement attributes: ${expectedAttrs}"
    if (state.states == null) { state.states = [:] }
    if (state.lastTx  == null) { state.lastTx  = [:] }
    state.states.isInfo = true
    state.states.infoBuffer = []
    state.states.infoExpectedAttrs = expectedAttrs.collect { it }
    state.lastTx.infoTime = now()
    runIn(10, 'clearInfoMode')
    String endpointHex = device.getDataValue('id') ?: '02'
    Integer endpoint = HexUtils.hexStringToInt(endpointHex)
    parent?.readAttribute(endpoint, 0x0551, -1)    // read all CameraAvStreamManagement attributes
}

// Flush accumulated getInfo buffer to a single logInfo entry
void flushInfoBuffer() {
    List<String> buf = state.states?.infoBuffer ?: []
    if (buf) {
        buf.sort { String line -> line.substring(1, 5) }   // sort by [XXXX] attrId prefix
        logInfo "getInfo:<br>" + buf.join('<br>')
    }
    if (state.states == null) { state.states = [:] }
    state.states.infoBuffer = []
    state.states.infoExpectedAttrs = null
    state.states.isInfo = false
    unschedule('clearInfoMode')
}

// Clear info mode flag (timeout fallback — flushes any buffered lines)
void clearInfoMode() {
    flushInfoBuffer()
    logDebug "clearInfoMode: info mode disabled"
}

List<String> getCameraAttributeList() {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getCameraAttributeList: fingerprint data not available"
        return []
    }
    return fingerprint['0551_FFFB'] ?: []
}

// --------------------------------------------------------------------------------------------
// Helper methods
// --------------------------------------------------------------------------------------------

private void initCameraAttr() {
    if (state.cameraAttr == null) { state.cameraAttr = [:] }
}

/** Decode FeatureMap integer → "[Video, Audio, Speaker, Snapshot, TwoWayTalk]" style string */
private String decodeCameraFeatureMap(Integer featureMap) {
    List<String> names = []
    int knowBitMask = 0
    CAMERA_FEATURE_BITS.each { Integer bit, String name ->
        knowBitMask |= (1 << bit)
        if ((featureMap >> bit) & 1) { names << name }
    }
    // Log any unknown bits
    int unknownBits = featureMap & ~knowBitMask
    if (unknownBits) {
        logDebug "decodeCameraFeatureMap: unknown feature bits=0x${HexUtils.integerToHexString(unknownBits, 2)} in FeatureMap=0x${HexUtils.integerToHexString(featureMap, 2)}"
        names << "Unknown(0x${HexUtils.integerToHexString(unknownBits, 2)})"
    }
    return names ? "[${names.join(', ')}]" : '[none]'
}

/** Decode a list of command IDs to command names */
private List<String> decodeCameraCommandList(Object value, Map<Integer, String> commandMap) {
    List<Integer> ids = normalizeCameraUintList(value)
    return ids.collect { Integer id -> commandMap[id] ?: "Unknown(0x${HexUtils.integerToHexString(id, 4)})" }
}

/** Normalize a Matter ARRAY value to List<Integer>. Handles both newParse (List/Integer) and legacy (hex string) forms. */
private List<Integer> normalizeCameraUintList(Object value) {
    if (value == null) { return [] }
    if (value instanceof List) { return value.collect { safeHexToInt(it) } }
    if (value instanceof Integer) { return [value as Integer] }
    // Could be a comma-separated string from legacy parse
    String s = value.toString().trim()
    if (s.startsWith('[') && s.endsWith(']')) { s = s[1..-2] }
    return s.split(',').collect { safeHexToInt(it.trim()) }.findAll { it >= 0 }
}

/** Safe enum lookup with "Unknown(n)" fallback */
private String decodeCameraEnum(Integer value, Map<Integer, String> enumMap) {
    return enumMap[value] ?: "Unknown(${value})"
}

/** Normalize a boolean value (from Matter BOOL attr or string) to 'true'/'false' string */
private String parseBooleanValue(Object value) {
    if (value == null) { return 'false' }
    String s = value.toString().trim().toLowerCase()
    return (s in ['true', '1', '01', 'on', 'yes']) ? 'true' : 'false'
}

// fmtHelpInfo — inline helper (not in any shared library)
@Field static final String CAMERA_DRIVER_NAME = 'Matter Advanced Bridge'
@Field static final String CAMERA_COMPONENT   = 'Matter Generic Component Camera AV Stream'
@Field static final String CAMERA_WIKI        = 'Get help on GitHub Wiki page:'
@Field static final String CAMERA_COMM_LINK   = 'https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009'
@Field static final String CAMERA_GITHUB_LINK = 'https://github.com/kkossev/Hubitat/wiki/Matter-Advanced-Bridge'

String fmtHelpInfo(String str) {
    String info = "${CAMERA_DRIVER_NAME} v${parent?.version()}<br> ${CAMERA_COMPONENT} v${CAMERA_DRIVER_VERSION}"
    String prefLink = "<a href='${CAMERA_GITHUB_LINK}' target='_blank'>${CAMERA_WIKI}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid green; border-radius: 6px; color: green;'"
    String topLink  = "<a ${topStyle} href='${CAMERA_COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
        "<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
