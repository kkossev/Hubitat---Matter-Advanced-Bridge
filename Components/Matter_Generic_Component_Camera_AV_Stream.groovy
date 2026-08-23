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
  * ver. 1.0.1  2026-07-25 kkossev : bug fixes
  * ver. 1.0.2  2026-08-17 kkossev : fixed the 'No signature of method: parse()' error logs
  * ver. 1.1.0  2026-08-19 kkossev + Claude Opus 5 : Matter 1.5.1 update, verified against the Aqara G350 firmware 4.5.70.
  *                                  FeatureMap bit map corrected (was an early draft map - Snapshot was reported as Speaker, NightVision as TwoWayTalk);
  *                                  TwoWayTalkSupport enum corrected (0=NotSupported, 1=HalfDuplex, 2=FullDuplex - the G350 is FullDuplex);
  *                                  added the privacy modes (0x0013/0x0014/0x0015) with a Switch capability master control;
  *                                  NightVision is now settable; added mechanical PTZ (cluster 0x0552); vision occupancy (0x0406) -> motion;
  *                                  the capability structs are decoded into readable text instead of raw tag maps.
  *
  *             TODO: snapshot capture workflow (CaptureSnapshot 0x000C + CaptureSnapshotResponse 0x000D) - see cameraSnapshotDiagnostics()
  *             TODO: image controls if attributes 0x0022/0x0023/0x0024 appear in AttributeList (the G350 does not implement them)
  *             TODO: status light controls if attributes 0x0027/0x0028 appear in AttributeList (the G350 does not implement them)
  *
*/

import groovy.transform.Field
import hubitat.helper.HexUtils
import hubitat.matter.DataType

@Field static final String CAMERA_DRIVER_VERSION = '1.1.0'
@Field static final String CAMERA_DRIVER_STAMP   = '2026/08/22 9:34 PM'

// Matter cluster ids handled by this child driver
@Field static final String CLUSTER_AV_STREAM = '0551'   // CameraAvStreamManagement
@Field static final String CLUSTER_PTZ       = '0552'   // CameraAvSettingsUserLevelManagement (mechanical PTZ)
@Field static final String CLUSTER_OCCUPANCY = '0406'   // OccupancySensing (vision occupancy on the camera endpoint)

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
    0x0004  : 'MinViewportResolution',
    0x0005  : 'RateDistortionTradeOffPoints',
    0x0006  : 'MaxContentBufferSize',
    0x0007  : 'MicrophoneCapabilities',
    0x0008  : 'SpeakerCapabilities',
    0x0009  : 'TwoWayTalkSupport',
    0x000A  : 'SnapshotCapabilities',
    0x000B  : 'MaxNetworkBandwidth',
    0x000C  : 'CurrentFrameRate',
    0x000D  : 'HDRModeEnabled',
    0x000E  : 'SupportedStreamUsages',
    0x000F  : 'AllocatedVideoStreams',
    0x0010  : 'AllocatedAudioStreams',
    0x0011  : 'AllocatedSnapshotStreams',
    0x0012  : 'StreamUsagePriorities',
    0x0013  : 'SoftRecordingPrivacyModeEnabled',
    0x0014  : 'SoftLivestreamPrivacyModeEnabled',
    0x0015  : 'HardPrivacyModeOn',
    0x0016  : 'NightVision',
    0x0017  : 'NightVisionIllum',
    0x0018  : 'Viewport',
    0x0019  : 'SpeakerMuted',
    0x001A  : 'SpeakerVolumeLevel',
    0x001B  : 'SpeakerMaxLevel',
    0x001C  : 'SpeakerMinLevel',
    0x001D  : 'MicrophoneMuted',
    0x001E  : 'MicrophoneVolumeLevel',
    0x001F  : 'MicrophoneMaxLevel',
    0x0020  : 'MicrophoneMinLevel',
    0x0021  : 'MicrophoneAGCEnabled',
    0x0022  : 'ImageRotation',
    0x0023  : 'ImageFlipHorizontal',
    0x0024  : 'ImageFlipVertical',
    0x0025  : 'LocalVideoRecordingEnabled',
    0x0026  : 'LocalSnapshotRecordingEnabled',
    0x0027  : 'StatusLightEnabled',
    0x0028  : 'StatusLightBrightness',
    0x0029  : 'ImageRotationDiscreteAngles',
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
    0x0005  : 'VideoStreamModify',
    0x0006  : 'VideoStreamDeallocate',
    0x0007  : 'SnapshotStreamAllocate',
    0x0009  : 'SnapshotStreamModify',
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
// FeatureMap bit definitions (0x0551 FeatureMap) — Matter 1.5.1, ClusterRevision 2
//
// NOTE: versions up to 1.0.2 used an early draft bit map in which Snapshot was reported as
// 'Speaker' and NightVision as 'TwoWayTalk'. There is no TwoWayTalk feature bit - two-way talk
// is advertised by the TwoWayTalkSupport attribute (0x0009) only.
// The Aqara G350 reports 0x041F = [Audio, Video, Snapshot, Privacy, Speaker, NightVision].
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_FEATURE_BITS = [
    0   : 'Audio',              // ADO
    1   : 'Video',              // VDO
    2   : 'Snapshot',           // SNP
    3   : 'Privacy',            // PRIV
    4   : 'Speaker',            // SPKR
    5   : 'ImageControl',       // ICTL
    6   : 'Watermark',          // WMARK
    7   : 'OnScreenDisplay',    // OSD
    8   : 'LocalStorage',       // STOR
    9   : 'HighDynamicRange',   // HDR
    10  : 'NightVision'         // NV
]

@Field static final Integer CAMERA_FEATURE_AUDIO     = 0
@Field static final Integer CAMERA_FEATURE_VIDEO     = 1
@Field static final Integer CAMERA_FEATURE_SNAPSHOT  = 2
@Field static final Integer CAMERA_FEATURE_PRIVACY   = 3
@Field static final Integer CAMERA_FEATURE_SPEAKER   = 4
@Field static final Integer CAMERA_FEATURE_NIGHTVISION = 10

// --------------------------------------------------------------------------------------------
// TriStateAutoEnum — used by NightVision (0x0016) and NightVisionIllum (0x0017)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_NIGHT_VISION_ENUM = [
    0   : 'Off',
    1   : 'On',
    2   : 'Auto'
]

// --------------------------------------------------------------------------------------------
// TwoWayTalkSupportTypeEnum (0x0009 TwoWayTalkSupport attribute)
// Corrected in this version - the enum used to start at HalfDuplex, so the G350's FullDuplex
// (raw 2) was logged as 'Unknown(2)'.
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_TWO_WAY_TALK_ENUM = [
    0   : 'NotSupported',
    1   : 'HalfDuplex',
    2   : 'FullDuplex'
]

// --------------------------------------------------------------------------------------------
// StreamUsageEnum (0x000E SupportedStreamUsages, 0x0012 StreamUsagePriorities)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> CAMERA_STREAM_USAGE_ENUM = [
    0   : 'Internal',
    1   : 'Recording',
    2   : 'Analysis',
    3   : 'LiveView'
]

// --------------------------------------------------------------------------------------------
// Cluster 0x0552 CameraAvSettingsUserLevelManagement — mechanical PTZ (Matter 1.5.1, rev 1)
// --------------------------------------------------------------------------------------------
@Field static final Map<Integer, String> PTZ_ATTR_NAMES = [
    0x0000  : 'MPTZPosition',
    0x0001  : 'MaxPresets',
    0x0002  : 'MPTZPresets',
    0x0003  : 'DPTZStreams',
    0x0004  : 'ZoomMax',
    0x0005  : 'TiltMin',
    0x0006  : 'TiltMax',
    0x0007  : 'PanMin',
    0x0008  : 'PanMax',
    0x0009  : 'MovementState',
    0xFFF8  : 'GeneratedCommandList',
    0xFFF9  : 'AcceptedCommandList',
    0xFFFB  : 'AttributeList',
    0xFFFC  : 'FeatureMap',
    0xFFFD  : 'ClusterRevision'
]

@Field static final Map<Integer, String> PTZ_ACCEPTED_COMMANDS = [
    0x0000  : 'MPTZSetPosition',
    0x0001  : 'MPTZRelativeMove',
    0x0002  : 'MPTZMoveToPreset',
    0x0003  : 'MPTZSavePreset',
    0x0004  : 'MPTZRemovePreset',
    0x0005  : 'DPTZSetViewport',
    0x0006  : 'DPTZRelativeMove'
]

@Field static final Map<Integer, String> PTZ_FEATURE_BITS = [
    0   : 'DigitalPTZ',         // DPTZ
    1   : 'MechanicalPan',      // MPAN
    2   : 'MechanicalTilt',     // MTILT
    3   : 'MechanicalZoom',     // MZOOM
    4   : 'MechanicalPresets'   // MPRESETS
]

@Field static final Integer PTZ_FEATURE_DPTZ     = 0
@Field static final Integer PTZ_FEATURE_PAN      = 1
@Field static final Integer PTZ_FEATURE_TILT     = 2
@Field static final Integer PTZ_FEATURE_ZOOM     = 3
@Field static final Integer PTZ_FEATURE_PRESETS  = 4

// PhysicalMovementEnum (0x0009 MovementState)
@Field static final Map<Integer, String> PTZ_MOVEMENT_STATE_ENUM = [
    0   : 'Idle',
    1   : 'Moving'
]

// --------------------------------------------------------------------------------------------
// Matter TLV element type not exposed by hubitat.matter.DataType.
// Command fields that cmdField() cannot express (char_string, structs) are hand-built as raw TLV
// and sent with the invoke() overload that takes a TLV string - see ptzSavePreset().
// --------------------------------------------------------------------------------------------
@Field static final Integer MATTER_TLV_INT8 = 0x00

// A position report newer than this makes the post-move readback unnecessary
@Field static final Integer PTZ_POSITION_FRESH_MS = 4000

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
        capability 'Switch'         // master privacy control: off() enables both soft privacy modes, on() clears them
        capability 'MotionSensor'   // vision occupancy (cluster 0x0406 with the VIS feature bit) on the camera endpoint

        // Only frequently-changing user-facing attributes declared as Hubitat attributes.
        // AudioVolume capability provides: mute + volume (speaker). Switch provides: switch. MotionSensor provides: motion.
        // All other camera attributes stored in state.cameraAttr (same pattern as Door Lock state.lockAttr).
        attribute 'microphoneMuted',  'enum', ['muted', 'unmuted']
        attribute 'microphoneVolume', 'number'
        attribute 'nightVision',      'enum', ['Off', 'On', 'Auto']
        // Privacy modes (cluster 0x0551 attributes 0x0013/0x0014/0x0015)
        attribute 'softRecordingPrivacy',  'enum', ['enabled', 'disabled']
        attribute 'softLivestreamPrivacy', 'enum', ['enabled', 'disabled']
        attribute 'hardPrivacy',           'enum', ['enabled', 'disabled']   // read-only - the physical shutter
        // Mechanical PTZ (cluster 0x0552)
        attribute 'pan',           'number'
        attribute 'tilt',          'number'
        attribute 'zoom',          'number'
        attribute 'movementState', 'enum', ['Idle', 'Moving']

        command 'setSpeakerMuted',   [[name: 'Muted*',  type: 'ENUM',   constraints: ['true', 'false'], description: 'Mute or unmute speaker']]
        command 'setSpeakerVolume',  [[name: 'Level*',  type: 'NUMBER', description: 'Speaker volume (0-100)']]
        command 'setMicrophoneMuted',[[name: 'Muted*',  type: 'ENUM',   constraints: ['true', 'false'], description: 'Mute or unmute microphone']]
        command 'setMicrophoneVolume',[[name: 'Level*', type: 'NUMBER', description: 'Microphone volume (0-100)']]
        command 'setNightVision',    [[name: 'Mode*',   type: 'ENUM',   constraints: ['Off', 'On', 'Auto'], description: 'Night vision mode']]
        command 'setSoftRecordingPrivacy',  [[name: 'Enabled*', type: 'ENUM', constraints: ['true', 'false'], description: 'Stop the camera recording (soft privacy)']]
        command 'setSoftLivestreamPrivacy', [[name: 'Enabled*', type: 'ENUM', constraints: ['true', 'false'], description: 'Stop the camera live stream (soft privacy)']]
        command 'ptzSetPosition',  [[name: 'Pan',  type: 'NUMBER', description: 'Absolute pan  (leave empty to keep)'],
                                    [name: 'Tilt', type: 'NUMBER', description: 'Absolute tilt (leave empty to keep)'],
                                    [name: 'Zoom', type: 'NUMBER', description: 'Absolute zoom (leave empty to keep)']]
        command 'ptzRelativeMove', [[name: 'Pan delta',  type: 'NUMBER', description: 'Relative pan  (leave empty to keep)'],
                                    [name: 'Tilt delta', type: 'NUMBER', description: 'Relative tilt (leave empty to keep)'],
                                    [name: 'Zoom delta', type: 'NUMBER', description: 'Relative zoom (leave empty to keep)']]
        command 'ptzMoveToPreset', [[name: 'Preset ID*', type: 'NUMBER', description: 'Move to a saved preset']]
        command 'ptzSavePreset',   [[name: 'Preset ID*', type: 'NUMBER', description: 'Preset slot'], [name: 'Name', type: 'STRING', description: 'Preset name']]
        command 'ptzRemovePreset', [[name: 'Preset ID*', type: 'NUMBER', description: 'Preset to remove']]
        command 'cameraSnapshotDiagnostics', [[name: 'Read snapshot capabilities and log support status']]
        command 'getInfo', [[name: 'Read all camera attributes and log a summary']]

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

// Hubitat platform 2.5.1.132+ transaction callbacks. The parent passes these Maps unchanged.
// Every custom component driver must implement this - without it the parent's dw.parse(descMap)
// throws MissingMethodException, which the platform logs as an error in THIS device's log.
void parse(Map descMap) {
    switch (descMap?.callbackType) {
        case 'Invoke':
            handleInvokeResponse(descMap)
            break
        default:
            logDebug "parse(Map): ignored callback: ${descMap}"
            break
    }
}

private void handleInvokeResponse(final Map descMap) {
    Integer invokeStatus = safeNumberToInt(descMap.status, null)
    Integer commandInt = safeNumberToInt(descMap.commandInt, null)

    if (invokeStatus == 0) {
        logDebug "Matter command completed: endpoint=${descMap.endpointInt} cluster=${descMap.clusterInt} command=${commandInt}"
    }
    else {
        logWarn "Matter command failed: status=${invokeStatus} endpoint=${descMap.endpointInt} cluster=${descMap.clusterInt} command=${commandInt}"
    }
}

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
    switch (descMap.cluster) {
        case CLUSTER_AV_STREAM:                                 // 0551
            processCameraAttributeReport(descMap)
            break
        case CLUSTER_PTZ:                                       // 0552
            processPtzAttributeReport(descMap)
            break
        case CLUSTER_OCCUPANCY:                                 // 0406 - vision occupancy on the camera endpoint
            processOccupancyReport(descMap)
            break
        default:
            logWarn "handleCameraMessage: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"
            break
    }
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
    String prefix = isInfoMode ? "[${CLUSTER_AV_STREAM}/${attrId}] " : ''
    String message = null
    boolean useDebugLog = false

    switch (attrId) {
        // ----- Global cluster metadata -----
        case 'FFFC': // FeatureMap
            Integer featureMap = safeHexToInt(rawVal)
            String decoded = decodeCameraFeatureMap(featureMap)
            state.cameraAttr['featureMap']    = decoded
            state.cameraAttr['featureMapRaw'] = featureMap
            message = "${prefix}FeatureMap=0x${HexUtils.integerToHexString(featureMap, 2)} ${decoded}"
            break
        case 'FFFD': // ClusterRevision
            Integer rev = safeHexToInt(rawVal)
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
        // ----- Privacy modes -----
        case '0013': // SoftRecordingPrivacyModeEnabled (RW)
        case '0014': // SoftLivestreamPrivacyModeEnabled (RW)
        case '0015': // HardPrivacyModeOn (RO - the physical shutter)
            String privEnabled = parseBooleanValue(rawVal)
            String privVal = (privEnabled == 'true') ? 'enabled' : 'disabled'
            String privAttr = (attrId == '0013') ? 'softRecordingPrivacy' : (attrId == '0014') ? 'softLivestreamPrivacy' : 'hardPrivacy'
            state.cameraAttr[privAttr] = privVal
            sendEvent(name: privAttr, value: privVal, descriptionText: "${device.displayName} ${privAttr} is ${privVal}", type: 'physical')
            if (attrId != '0015') { updatePrivacySwitchState() }     // hard privacy is not part of the master switch
            message = "${prefix}${attrName}=${privEnabled} (${privAttr}=${privVal})"
            break
        // ----- NightVision -----
        case '0016': // NightVision → nightVision attribute
        case '0017': // NightVisionIllum (same TriStateAutoEnum; not implemented by the G350)
            Integer nvVal = safeToInt(rawVal)
            String nvDecoded = decodeCameraEnum(nvVal, CAMERA_NIGHT_VISION_ENUM)
            if (attrId == '0016') {
                sendEvent(name: 'nightVision', value: nvDecoded, descriptionText: "${device.displayName} nightVision is ${nvDecoded}", type: 'physical')
            } else {
                state.cameraAttr['nightVisionIllum'] = nvDecoded
            }
            message = "${prefix}${attrName}=${nvDecoded} (raw=${rawVal})"
            break
        // ----- Capability discovery -----
        case '0009': // TwoWayTalkSupport
            Integer twtVal = safeToInt(rawVal)
            state.cameraAttr['twoWayTalkSupport'] = decodeCameraEnum(twtVal, CAMERA_TWO_WAY_TALK_ENUM)
            message = "${prefix}TwoWayTalkSupport=${state.cameraAttr['twoWayTalkSupport']} (raw=${rawVal})"
            break
        case '000A': // SnapshotCapabilities
            String snapDecoded = decodeSnapshotCapabilities(rawVal)
            state.cameraAttr['snapshotCapabilities'] = snapDecoded
            message = "${prefix}SnapshotCapabilities=${snapDecoded}"
            break
        // ----- Decoded capability structs -----
        case '0002': // VideoSensorParams
            String vsp = decodeVideoSensorParams(rawVal)
            state.cameraAttr['videoSensorParams'] = vsp
            message = "${prefix}VideoSensorParams=${vsp}"
            break
        case '0004': // MinViewportResolution
            String mvr = decodeResolution(rawVal)
            state.cameraAttr['minViewportResolution'] = mvr
            message = "${prefix}MinViewportResolution=${mvr}"
            break
        case '0005': // RateDistortionTradeOffPoints
            String rdt = decodeRateDistortionPoints(rawVal)
            state.cameraAttr['rateDistortionTradeOffPoints'] = rdt
            message = "${prefix}RateDistortionTradeOffPoints=${rdt}"
            useDebugLog = true
            break
        case '0007': // MicrophoneCapabilities
        case '0008': // SpeakerCapabilities
            String audioCaps = decodeAudioCapabilities(rawVal)
            state.cameraAttr[(attrId == '0007') ? 'microphoneCapabilities' : 'speakerCapabilities'] = audioCaps
            message = "${prefix}${attrName}=${audioCaps}"
            break
        case '000E': // SupportedStreamUsages
        case '0012': // StreamUsagePriorities
            List<String> usages = normalizeCameraUintList(rawVal).collect { decodeCameraEnum(it, CAMERA_STREAM_USAGE_ENUM) }
            state.cameraAttr[(attrId == '000E') ? 'supportedStreamUsages' : 'streamUsagePriorities'] = usages.toString()
            message = "${prefix}${attrName}=${usages}"
            break
        case '0018': // Viewport
            String vp = decodeViewport(rawVal)
            state.cameraAttr['viewport'] = vp
            message = "${prefix}Viewport=${vp}"
            useDebugLog = true
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
        emitCameraLine(message, isInfoMode, CLUSTER_AV_STREAM, attrId, 'Camera AV Stream Management', useDebugLog)
    }
}

/**
 *  Route one decoded line either into the getInfo() buffer or straight to the log.
 *  Shared by the 0x0551, 0x0552 and 0x0406 handlers so getInfo() can span several clusters.
 *  Expected-attribute keys are 'CLUSTER_ATTRID' so that the same attribute id in two
 *  different clusters is tracked separately.
 */
private void emitCameraLine(String message, boolean isInfoMode, String clusterId, String attrId, String logPrefix, boolean useDebugLog) {
    if (isInfoMode) {
        state.states.infoBuffer = (state.states?.infoBuffer ?: []) + [message]
        List<String> remaining = state.states?.infoExpectedAttrs ?: []
        remaining.remove("${clusterId}_${normalizeAttrKey(attrId)}".toString())
        state.states.infoExpectedAttrs = remaining
        if (!remaining.isEmpty()) { return }
        flushInfoBuffer()
        return
    }
    if (useDebugLog) { logDebug "${logPrefix}: ${message}" }
    else             { logInfo  "${logPrefix}: ${message}" }
}

// --------------------------------------------------------------------------------------------
// processPtzAttributeReport — decode cluster 0x0552 (mechanical PTZ) attributes
// --------------------------------------------------------------------------------------------
void processPtzAttributeReport(Map descMap) {
    initCameraAttr()
    String attrId = descMap.attrId ?: ''
    Object rawVal = descMap.value
    Integer attrInt = descMap.attrInt != null ? (descMap.attrInt as Integer) : safeHexToInt(attrId)
    String attrName = PTZ_ATTR_NAMES[attrInt] ?: "0x${attrId}"
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${CLUSTER_PTZ}/${attrId}] " : ''
    String message = null
    boolean useDebugLog = false

    switch (attrId) {
        case 'FFFC': // FeatureMap
            Integer featureMap = safeHexToInt(rawVal)
            String decoded = decodeBitmap(featureMap, PTZ_FEATURE_BITS)
            state.cameraAttr['ptzFeatureMap']    = decoded
            state.cameraAttr['ptzFeatureMapRaw'] = featureMap
            message = "${prefix}FeatureMap=0x${HexUtils.integerToHexString(featureMap, 2)} ${decoded}"
            break
        case 'FFFD': // ClusterRevision
            Integer ptzRev = safeHexToInt(rawVal)
            state.cameraAttr['ptzClusterRevision'] = ptzRev
            message = "${prefix}ClusterRevision=${ptzRev}"
            useDebugLog = true
            break
        case 'FFF9': // AcceptedCommandList
            List<String> names = decodeCameraCommandList(rawVal, PTZ_ACCEPTED_COMMANDS)
            state.cameraAttr['ptzAcceptedCommandIds'] = normalizeCameraUintList(rawVal)
            state.cameraAttr['ptzAcceptedCommands']   = names.toString()
            message = "${prefix}AcceptedCommands=${names}"
            break
        case 'FFF8': // GeneratedCommandList
            state.cameraAttr['ptzGeneratedCommands'] = rawVal?.toString()
            message = "${prefix}GeneratedCommands=${rawVal}"
            useDebugLog = true
            break
        case 'FFFB': // AttributeList
            state.cameraAttr['ptzAttributeList'] = rawVal?.toString()
            message = "${prefix}AttributeList=${rawVal}"
            useDebugLog = true
            break
        case '0000': // MPTZPosition (MPTZStruct: 0=Pan, 1=Tilt, 2=Zoom - all optional)
            Integer panVal  = safeStructInt(rawVal, 0)
            Integer tiltVal = safeStructInt(rawVal, 1)
            Integer zoomVal = safeStructInt(rawVal, 2)
            if (panVal  != null) { sendEvent(name: 'pan',  value: panVal,  descriptionText: "${device.displayName} pan is ${panVal}",   type: 'physical') }
            if (tiltVal != null) { sendEvent(name: 'tilt', value: tiltVal, descriptionText: "${device.displayName} tilt is ${tiltVal}", type: 'physical') }
            if (zoomVal != null) { sendEvent(name: 'zoom', value: zoomVal, descriptionText: "${device.displayName} zoom is ${zoomVal}", type: 'physical') }
            state.cameraAttr['ptzPositionRxMs'] = now()     // lets readPtzPosition() skip a redundant read
            message = "${prefix}MPTZPosition: pan=${panVal} tilt=${tiltVal} zoom=${zoomVal}"
            break
        case '0009': // MovementState (PhysicalMovementEnum)
            Integer moveVal = safeToInt(rawVal)
            String moveDecoded = decodeCameraEnum(moveVal, PTZ_MOVEMENT_STATE_ENUM)
            sendEvent(name: 'movementState', value: moveDecoded, descriptionText: "${device.displayName} movementState is ${moveDecoded}", type: 'physical')
            message = "${prefix}MovementState=${moveDecoded} (raw=${rawVal})"
            // the position attribute is not always reported at the end of a move - read it back once the camera settles
            if (moveDecoded == 'Idle') { runIn(2, 'readPtzPosition') }
            break
        case '0002': // MPTZPresets
            state.cameraAttr['ptzPresets']   = decodePtzPresets(rawVal)
            state.cameraAttr['ptzPresetIds'] = extractPresetIds(rawVal)   // used to reject a move to a preset that does not exist
            state.cameraAttr['ptzPresetsRxMs'] = now()                    // lets readPtzPresets() skip a redundant read
            message = "${prefix}MPTZPresets=${state.cameraAttr['ptzPresets']}"
            break
        case '0001': // MaxPresets
        case '0004': // ZoomMax
        case '0005': // TiltMin
        case '0006': // TiltMax
        case '0007': // PanMin
        case '0008': // PanMax
            state.cameraAttr[lowerFirst(attrName)] = safeToInt(rawVal)
            message = "${prefix}${attrName}=${rawVal}"
            useDebugLog = true
            break
        default:
            state.cameraAttr[attrName] = rawVal
            message = "${prefix}${attrName}=${rawVal} (stored in state.cameraAttr)"
            useDebugLog = true
            break
    }

    if (message != null) {
        emitCameraLine(message, isInfoMode, CLUSTER_PTZ, attrId, 'Camera PTZ', useDebugLog)
    }
}

/**
 *  Re-read MPTZPosition after a move, but only if the camera did not already report it.
 *  The G350 does report the final position just before MovementState returns to Idle, so the
 *  unconditional read this used to do was a redundant Matter round-trip and a duplicate log line
 *  on every move. Cameras that stay quiet still get the readback.
 */
void readPtzPosition() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    Long lastRx = state.cameraAttr?.ptzPositionRxMs as Long
    if (lastRx != null && (now() - lastRx) < PTZ_POSITION_FRESH_MS) {
        logDebug "readPtzPosition: the camera already reported its position ${now() - lastRx}ms ago - skipping the read"
        return
    }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0552, 0x0000)]))
}

// --------------------------------------------------------------------------------------------
// processOccupancyReport — cluster 0x0406 on the camera endpoint (vision occupancy)
// A Matter 1.5 camera carries OccupancySensing with the VIS feature bit alongside 0x0551.
// --------------------------------------------------------------------------------------------
void processOccupancyReport(Map descMap) {
    initCameraAttr()
    String attrId = descMap.attrId ?: ''
    Object rawVal = descMap.value
    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${CLUSTER_OCCUPANCY}/${attrId}] " : ''
    String message = null

    if (attrId == '0000') {      // Occupancy bitmap - bit 0 is 'occupied'
        Integer occ = safeToInt(rawVal)
        String motionVal = ((occ & 0x01) != 0) ? 'active' : 'inactive'
        sendEvent(name: 'motion', value: motionVal, descriptionText: "${device.displayName} motion is ${motionVal}", type: 'physical')
        message = "${prefix}Occupancy=${occ} (motion=${motionVal})"
    }
    else {
        state.cameraAttr["occupancy_${attrId}"] = rawVal
        message = "${prefix}0x${attrId}=${rawVal} (stored in state.cameraAttr)"
    }

    if (message != null) {
        emitCameraLine(message, isInfoMode, CLUSTER_OCCUPANCY, attrId, 'Camera Occupancy', attrId != '0000')
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
// Privacy modes (0x0013 SoftRecording, 0x0014 SoftLivestream, 0x0015 HardPrivacy - read only)
//
// The Switch capability is the master control, mapped the same way SmartThings maps it:
//    off() -> both soft privacy modes enabled  (camera stops recording and streaming)
//    on()  -> both soft privacy modes cleared
// HardPrivacyModeOn reflects the physical shutter and is never written.
// --------------------------------------------------------------------------------------------
void on()  { setPrivacyModes(false) }
void off() { setPrivacyModes(true)  }

private void setPrivacyModes(boolean privacyEnabled) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    boolean canRecording  = hasCameraAttr(0x0013)
    boolean canLivestream = hasCameraAttr(0x0014)
    if (!canRecording && !canLivestream) {
        logWarn "on()/off(): this camera does not implement the soft privacy attributes (0x0013/0x0014) - ignoring"
        return
    }
    logInfo "${privacyEnabled ? 'off()' : 'on()'}: ${privacyEnabled ? 'enabling' : 'disabling'} the soft privacy modes"
    Integer boolType = privacyEnabled ? DataType.BOOLEAN_TRUE : DataType.BOOLEAN_FALSE
    List<Map<String, String>> reqs = []
    if (canRecording)  { reqs << matter.attributeWriteRequest(deviceNumber, 0x0551, 0x0013, boolType, '') }
    if (canLivestream) { reqs << matter.attributeWriteRequest(deviceNumber, 0x0551, 0x0014, boolType, '') }
    parent?.sendToDevice(matter.writeAttributes(reqs))
}

void setSoftRecordingPrivacy(String enabledParam)  { writePrivacyAttr(0x0013, enabledParam, 'SoftRecordingPrivacyModeEnabled') }
void setSoftLivestreamPrivacy(String enabledParam) { writePrivacyAttr(0x0014, enabledParam, 'SoftLivestreamPrivacyModeEnabled') }

private void writePrivacyAttr(Integer attrId, String enabledParam, String attrName) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasCameraAttr(attrId)) {
        logWarn "${attrName}: this camera does not implement attribute 0x${HexUtils.integerToHexString(attrId, 2)} - ignoring"
        return
    }
    String enabled = parseBooleanValue(enabledParam)
    logInfo "${attrName}: ${enabled}"
    Integer boolType = (enabled == 'true') ? DataType.BOOLEAN_TRUE : DataType.BOOLEAN_FALSE
    parent?.sendToDevice(matter.writeAttributes([matter.attributeWriteRequest(deviceNumber, 0x0551, attrId, boolType, '')]))
}

/**
 *  Derive the Switch state from the two soft privacy modes: 'on' when neither is enabled.
 *  Reads state.cameraAttr rather than device.currentValue() - sendEvent() is asynchronous, so a
 *  currentValue() read in the same pass would still return the previous value.
 */
private void updatePrivacySwitchState() {
    String rec  = state.cameraAttr?.softRecordingPrivacy
    String live = state.cameraAttr?.softLivestreamPrivacy
    // if the camera implements only one of the two, base the switch on whichever is known
    if (rec == null && live == null) { return }
    boolean privacyOn = (rec == 'enabled') || (live == 'enabled')
    String switchVal = privacyOn ? 'off' : 'on'
    if (device.currentValue('switch') != switchVal) {
        sendEvent(name: 'switch', value: switchVal, descriptionText: "${device.displayName} switch is ${switchVal} (privacy ${privacyOn ? 'enabled' : 'disabled'})", type: 'physical')
    }
}

// --------------------------------------------------------------------------------------------
// NightVision (0x0016) — writable TriStateAutoEnum
// --------------------------------------------------------------------------------------------
void setNightVision(String mode) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasCameraAttr(0x0016)) {
        logWarn 'setNightVision: this camera does not implement NightVision (0x0016) - ignoring'
        return
    }
    Integer modeVal = CAMERA_NIGHT_VISION_ENUM.find { k, v -> v.equalsIgnoreCase(mode) }?.key
    if (modeVal == null) {
        logWarn "setNightVision: unknown mode '${mode}' - expected one of ${CAMERA_NIGHT_VISION_ENUM.values()}"
        return
    }
    logInfo "setNightVision: ${mode}"
    parent?.sendToDevice(matter.writeAttributes([matter.attributeWriteRequest(deviceNumber, 0x0551, 0x0016, DataType.UINT8, HexUtils.integerToHexString(modeVal, 1))]))
}

// --------------------------------------------------------------------------------------------
// Mechanical PTZ commands (cluster 0x0552)
//
// Every field of MPTZSetPosition and MPTZRelativeMove is optional: only the axes the caller
// actually supplied are sent, so a single-axis move never disturbs the other two.
// --------------------------------------------------------------------------------------------
void ptzSetPosition(BigDecimal pan = null, BigDecimal tilt = null, BigDecimal zoom = null) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasPtzCommand(0x0000)) { logWarn 'ptzSetPosition: MPTZSetPosition is not supported by this camera'; return }

    List<Map<String, String>> cmdFields = []
    if (pan != null) {
        if (!hasPtzFeature(PTZ_FEATURE_PAN)) { logWarn 'ptzSetPosition: this camera has no mechanical pan'; return }
        Integer p = clampPtz(pan as Integer, 'panMin', 'panMax', 'pan')
        if (p == null) { return }
        cmdFields << matter.cmdField(DataType.INT16, 0x00, encodeInt16LE(p))
    }
    if (tilt != null) {
        if (!hasPtzFeature(PTZ_FEATURE_TILT)) { logWarn 'ptzSetPosition: this camera has no mechanical tilt'; return }
        Integer t = clampPtz(tilt as Integer, 'tiltMin', 'tiltMax', 'tilt')
        if (t == null) { return }
        cmdFields << matter.cmdField(DataType.INT16, 0x01, encodeInt16LE(t))
    }
    if (zoom != null) {
        if (!hasPtzFeature(PTZ_FEATURE_ZOOM)) { logWarn 'ptzSetPosition: this camera has no mechanical zoom'; return }
        Integer z = clampPtz(zoom as Integer, null, 'zoomMax', 'zoom')
        if (z == null) { return }
        cmdFields << matter.cmdField(DataType.UINT8, 0x02, HexUtils.integerToHexString(z, 1))
    }
    if (cmdFields.isEmpty()) { logWarn 'ptzSetPosition: nothing to do - supply at least one of pan, tilt or zoom'; return }

    logInfo "ptzSetPosition: pan=${pan} tilt=${tilt} zoom=${zoom}"
    parent?.sendToDevice(matter.invoke(deviceNumber, 0x0552, 0x0000, cmdFields))
}

void ptzRelativeMove(BigDecimal panDelta = null, BigDecimal tiltDelta = null, BigDecimal zoomDelta = null) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasPtzCommand(0x0001)) { logWarn 'ptzRelativeMove: MPTZRelativeMove is not supported by this camera'; return }

    List<Map<String, String>> cmdFields = []
    if (panDelta != null) {
        if (!hasPtzFeature(PTZ_FEATURE_PAN)) { logWarn 'ptzRelativeMove: this camera has no mechanical pan'; return }
        cmdFields << matter.cmdField(DataType.INT16, 0x00, encodeInt16LE(panDelta as Integer))
    }
    if (tiltDelta != null) {
        if (!hasPtzFeature(PTZ_FEATURE_TILT)) { logWarn 'ptzRelativeMove: this camera has no mechanical tilt'; return }
        cmdFields << matter.cmdField(DataType.INT16, 0x01, encodeInt16LE(tiltDelta as Integer))
    }
    if (zoomDelta != null) {
        if (!hasPtzFeature(PTZ_FEATURE_ZOOM)) { logWarn 'ptzRelativeMove: this camera has no mechanical zoom'; return }
        // MATTER_TLV_INT8 - hubitat.matter.DataType has no INT8 constant
        cmdFields << matter.cmdField(MATTER_TLV_INT8, 0x02, encodeInt8(zoomDelta as Integer))
    }
    if (cmdFields.isEmpty()) { logWarn 'ptzRelativeMove: nothing to do - supply at least one delta'; return }

    logInfo "ptzRelativeMove: panDelta=${panDelta} tiltDelta=${tiltDelta} zoomDelta=${zoomDelta}"
    parent?.sendToDevice(matter.invoke(deviceNumber, 0x0552, 0x0001, cmdFields))
}

void ptzMoveToPreset(BigDecimal presetId) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasPtzCommand(0x0002)) { logWarn 'ptzMoveToPreset: MPTZMoveToPreset is not supported by this camera'; return }
    Integer id = validatePresetId(presetId, true)
    if (id == null) { return }
    logInfo "ptzMoveToPreset: preset ${id}"
    parent?.sendToDevice(matter.invoke(deviceNumber, 0x0552, 0x0002, [matter.cmdField(DataType.UINT8, 0x00, HexUtils.integerToHexString(id, 1))]))
}

void ptzSavePreset(BigDecimal presetId, String name = null) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasPtzCommand(0x0003)) { logWarn 'ptzSavePreset: MPTZSavePreset is not supported by this camera'; return }
    Integer id = validatePresetId(presetId)
    if (id == null) { return }
    String presetName = (name ?: "Preset ${id}").take(32)
    logInfo "ptzSavePreset: preset ${id} '${presetName}'"

    // matter.cmdField() cannot express a char_string field, so the payload is hand-built as raw TLV
    // and passed to the invoke() overload that takes a TLV string - the same approach the Door Lock
    // component driver uses for SetCredential and friends.
    //
    // MPTZSavePreset (0x0552 / 0x03):
    //   15                              anonymous structure
    //     24 00 <id>                    tag 0  PresetID    uint8        (0x20 context | 0x04 uint8)
    //     2C 01 <len> <utf8 bytes>      tag 1  Name        char_string  (0x20 context | 0x0C utf8-1)
    //   18                              end of structure
    String nameHex = encodeUtf8Hex(presetName)
    Integer nameLen = (nameHex.length() / 2) as Integer
    String tlv = '15' +
                 '2400' + HexUtils.integerToHexString(id, 1) +
                 '2C01' + String.format('%02X', nameLen) + nameHex +
                 '18'
    logDebug "ptzSavePreset: tlv=${tlv}"
    parent?.sendToDevice(matter.invoke(deviceNumber, 0x0552, 0x0003, 2000, tlv))
    runIn(3, 'readPtzPresets')
}

void ptzRemovePreset(BigDecimal presetId) {
    Integer deviceNumber = getDeviceNumber()
    if (deviceNumber == null) { return }
    if (!hasPtzCommand(0x0004)) { logWarn 'ptzRemovePreset: MPTZRemovePreset is not supported by this camera'; return }
    Integer id = validatePresetId(presetId, true)
    if (id == null) { return }
    logInfo "ptzRemovePreset: preset ${id}"
    parent?.sendToDevice(matter.invoke(deviceNumber, 0x0552, 0x0004, [matter.cmdField(DataType.UINT8, 0x00, HexUtils.integerToHexString(id, 1))]))
    runIn(3, 'readPtzPresets')
}

/** Re-read MPTZPresets after a save/remove, unless the camera already reported the new table. */
void readPtzPresets() {
    Integer dn = getDeviceNumber(); if (dn == null) { return }
    Long lastRx = state.cameraAttr?.ptzPresetsRxMs as Long
    if (lastRx != null && (now() - lastRx) < PTZ_POSITION_FRESH_MS) {
        logDebug "readPtzPresets: the camera already reported its presets ${now() - lastRx}ms ago - skipping the read"
        return
    }
    parent?.sendToDevice(matter.readAttributes([matter.attributePath(dn, 0x0552, 0x0002)]))
}

/** Clamp a PTZ value to the camera-reported limits. Returns null (and warns) if the limits are unknown. */
private Integer clampPtz(Integer value, String minKey, String maxKey, String label) {
    Integer minVal = (minKey == null) ? 0 : (state.cameraAttr?.get(minKey) as Integer)
    Integer maxVal = state.cameraAttr?.get(maxKey) as Integer
    if (maxVal == null || (minKey != null && minVal == null)) {
        logWarn "ptz: the ${label} limits are not known yet - run Refresh or Get Info first, then retry"
        return null
    }
    Integer clamped = Math.max(minVal, Math.min(maxVal, value))
    if (clamped != value) { logInfo "ptz: ${label} ${value} clamped to ${clamped} (range ${minVal}..${maxVal})" }
    return clamped
}

/**
 *  Validate a preset ID. Matter defines PresetID as 1-based (min="1" on MPTZSavePreset,
 *  MPTZMoveToPreset, MPTZRemovePreset and MPTZPresetStruct), so 0 is never valid.
 *
 *  mustExist=true additionally refuses a preset the camera has not saved. Moving to a
 *  non-existent preset otherwise just fails silently on the device, with no InvokeResponse.
 */
private Integer validatePresetId(BigDecimal presetId, boolean mustExist = false) {
    if (presetId == null) { logWarn 'ptz: a preset ID is required'; return null }
    Integer id = presetId as Integer
    Integer maxPresets = state.cameraAttr?.maxPresets as Integer
    if (id < 1 || (maxPresets != null && id > maxPresets)) {
        logWarn "ptz: preset ID ${id} is out of range - Matter preset IDs start at 1 (valid: 1..${maxPresets ?: '?'})"
        return null
    }
    if (mustExist) {
        List savedIds = state.cameraAttr?.ptzPresetIds
        if (savedIds != null) {      // null = never read, so do not second-guess the camera
            if (savedIds.isEmpty()) {
                logWarn "ptz: the camera has no saved presets - use Ptz Save Preset first"
                return null
            }
            if (!savedIds.collect { safeToInt(it) }.contains(id)) {
                logWarn "ptz: preset ${id} is not saved on the camera (saved: ${savedIds}) - use Ptz Save Preset first"
                return null
            }
        }
    }
    return id
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
    parent?.componentRefresh(device)
}

// --------------------------------------------------------------------------------------------
// getInfo — read all camera attributes and log a human-readable summary
// --------------------------------------------------------------------------------------------
void getInfo() {
    if (!isClusterSupported(CLUSTER_AV_STREAM)) {
        logWarn "getInfo: CameraAvStreamManagement cluster (0x0551) is not supported by this device"
        logInfo "getInfo: ServerList contains: ${getServerList()}"
        return
    }
    // Expected keys are 'CLUSTER_ATTRID' so that the same attribute id in two clusters is tracked separately.
    List<String> expectedAttrs = []
    getClusterAttributeList(CLUSTER_AV_STREAM).each { expectedAttrs << "${CLUSTER_AV_STREAM}_${normalizeAttrKey(it)}".toString() }
    boolean hasPtz = isClusterSupported(CLUSTER_PTZ)
    if (hasPtz) {
        getClusterAttributeList(CLUSTER_PTZ).each { expectedAttrs << "${CLUSTER_PTZ}_${normalizeAttrKey(it)}".toString() }
    }
    logInfo "getInfo: reading all supported camera attributes (0x0551${hasPtz ? ' + 0x0552' : ''}): ${expectedAttrs.size()} attribute(s)"
    if (state.states == null) { state.states = [:] }
    if (state.lastTx  == null) { state.lastTx  = [:] }
    state.states.isInfo = true
    state.states.infoBuffer = []
    state.states.infoExpectedAttrs = expectedAttrs
    state.lastTx.infoTime = now()
    runIn(15, 'clearInfoMode')
    String endpointHex = device.getDataValue('id') ?: '02'
    Integer endpoint = HexUtils.hexStringToInt(endpointHex)
    parent?.readAttribute(endpoint, 0x0551, -1)    // read all CameraAvStreamManagement attributes
    if (hasPtz) {
        runIn(3, 'getInfoPtz')                     // stagger the second cluster so the reads do not collide
    }
}

void getInfoPtz() {
    String endpointHex = device.getDataValue('id') ?: '02'
    Integer endpoint = HexUtils.hexStringToInt(endpointHex)
    parent?.readAttribute(endpoint, 0x0552, -1)    // read all PTZ attributes
}

// Flush accumulated getInfo buffer to a single logInfo entry
void flushInfoBuffer() {
    List<String> buf = state.states?.infoBuffer ?: []
    if (buf) {
        // sort by the '[cluster/attrId]' prefix so the clusters group together and the attributes stay in order
        buf.sort { String line ->
            int close = line.indexOf(']')
            return (close > 1) ? line.substring(1, close) : line
        }
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
    return getClusterAttributeList(CLUSTER_AV_STREAM)
}

/** The endpoint's AttributeList (FFFB) for one cluster, from the parent's fingerprint data. */
List<String> getClusterAttributeList(String clusterId) {
    Map fingerprint = getFingerprintData()
    if (fingerprint == null) {
        logDebug "getClusterAttributeList: fingerprint data not available"
        return []
    }
    return fingerprint["${clusterId}_FFFB"] ?: []
}

// --------------------------------------------------------------------------------------------
// Helper methods
// --------------------------------------------------------------------------------------------

private void initCameraAttr() {
    if (state.cameraAttr == null) { state.cameraAttr = [:] }
}

/** Two's-complement little-endian hex for a signed 16-bit value (e.g. -8 -> 'F8FF'). */
private String encodeInt16LE(Integer value) {
    int v = (value as int) & 0xFFFF
    return String.format('%02X%02X', v & 0xFF, (v >> 8) & 0xFF)
}

/** Two's-complement hex for a signed 8-bit value (e.g. -3 -> 'FD'). */
private String encodeInt8(Integer value) {
    return String.format('%02X', (value as int) & 0xFF)
}

/**
 *  The bare UTF-8 hex of a string, with no TLV length prefix - the platform adds the control byte
 *  and the length itself, the same convention Matter Advanced Device uses for TLV types 0x0C..0x13.
 */
private String encodeUtf8Hex(String s) {
    StringBuilder sb = new StringBuilder()
    s.getBytes('UTF-8').each { byte b -> sb.append(String.format('%02X', b & 0xFF)) }
    return sb.toString()
}

// --------------------------------------------------------------------------------------------
// Feature / attribute / command detection
//
// The rule adopted from the Matter camera work: the FeatureMap tells us which feature families
// exist, but the AttributeList and the AcceptedCommandList tell us what this particular device
// actually implements. Never gate a write on the FeatureMap alone.
// --------------------------------------------------------------------------------------------

/**
 *  True if the endpoint's 0551 AttributeList contains this attribute id.
 *  The parent stores the AttributeList as variable-width hex ('00', '13', '4000'), so the
 *  comparison is numeric rather than on the string form.
 */
private boolean hasCameraAttr(Integer attrId) {
    List<String> attrList = getCameraAttributeList()
    if (!attrList) { return true }      // unknown fingerprint - do not block the user, let the device refuse
    return attrList.any { safeHexToInt(it?.toString()) == attrId }
}

/** Canonical 4-digit uppercase hex key, so 'D', '0D' and '000D' all compare equal. */
private String normalizeAttrKey(Object attrId) {
    Integer v = safeHexToInt(attrId?.toString())
    return (v == null || v < 0) ? attrId?.toString()?.toUpperCase() : HexUtils.integerToHexString(v, 2).toUpperCase()
}

/** True if the 0552 AcceptedCommandList contains this command id. */
private boolean hasPtzCommand(Integer commandId) {
    List accepted = state.cameraAttr?.ptzAcceptedCommandIds ?: []
    if (!accepted) { return true }      // not read yet - let the device refuse rather than blocking
    return accepted.collect { safeToInt(it) }.contains(commandId)
}

/** True if the 0552 FeatureMap has this bit set. */
private boolean hasPtzFeature(Integer bit) {
    Integer featureMap = state.cameraAttr?.ptzFeatureMapRaw as Integer
    if (featureMap == null) { return true }     // not read yet
    return ((featureMap >> bit) & 1) == 1
}

/** True if the 0551 FeatureMap has this bit set. */
private boolean hasCameraFeature(Integer bit) {
    Integer featureMap = state.cameraAttr?.featureMapRaw as Integer
    if (featureMap == null) { return true }
    return ((featureMap >> bit) & 1) == 1
}

// --------------------------------------------------------------------------------------------
// Struct decoding helpers
//
// Matter struct values arrive from the parent in one of two shapes:
//   a) a flat Map keyed by field id     - e.g. [0:1920, 1:1080, 2:120, 3:120]
//   b) a List of [tag: n, value: v] Maps - e.g. [[tag:0, value:640], [tag:1, value:480]]
// safeStructField() accepts both.
// --------------------------------------------------------------------------------------------

/** Extract one field of a Matter struct by its field id, whichever shape the value arrived in. */
private Object safeStructField(Object struct, Integer fieldId) {
    if (struct == null) { return null }
    if (struct instanceof Map) {
        Map m = struct as Map
        for (Object key : m.keySet()) {
            if (safeToInt(key) == fieldId) { return m[key] }
        }
        return null
    }
    if (struct instanceof List) {
        for (Object element : (struct as List)) {
            if (element instanceof Map) {
                Map em = element as Map
                if (em.containsKey('tag') && safeToInt(em['tag']) == fieldId) { return em['value'] }
            }
        }
    }
    return null
}

private Integer safeStructInt(Object struct, Integer fieldId) {
    Object v = safeStructField(struct, fieldId)
    return (v == null) ? null : safeToInt(v)
}

/** VideoResolutionStruct: 0=Width, 1=Height -> '640x480' */
private String decodeResolution(Object struct) {
    Integer w = safeStructInt(struct, 0)
    Integer h = safeStructInt(struct, 1)
    if (w == null || h == null) { return struct?.toString() }
    return "${w}x${h}"
}

/** VideoSensorParamsStruct: 0=SensorWidth, 1=SensorHeight, 2=MaxFPS, 3=MaxHDRFPS */
private String decodeVideoSensorParams(Object struct) {
    Integer w = safeStructInt(struct, 0)
    Integer h = safeStructInt(struct, 1)
    Integer maxFps = safeStructInt(struct, 2)
    Integer maxHdrFps = safeStructInt(struct, 3)
    if (w == null || h == null) { return struct?.toString() }
    String out = "${w}x${h} @${maxFps ?: '?'}fps"
    if (maxHdrFps != null) { out += " (HDR @${maxHdrFps}fps)" }
    return out
}

/** AudioCapabilitiesStruct: 0=MaxNumberOfChannels, 1=SupportedCodecs, 2=SupportedSampleRates, 3=SupportedBitDepths */
private String decodeAudioCapabilities(Object struct) {
    Integer channels = safeStructInt(struct, 0)
    Object codecs = safeStructField(struct, 1)
    Object rates  = safeStructField(struct, 2)
    Object depths = safeStructField(struct, 3)
    if (channels == null && codecs == null) { return struct?.toString() }
    return "${channels ?: '?'} channel(s), codecs=${codecs}, sampleRates=${rates}, bitDepths=${depths}"
}

/** ViewportStruct: 0=X1, 1=Y1, 2=X2, 3=Y2 */
private String decodeViewport(Object struct) {
    Integer x1 = safeStructInt(struct, 0)
    Integer y1 = safeStructInt(struct, 1)
    Integer x2 = safeStructInt(struct, 2)
    Integer y2 = safeStructInt(struct, 3)
    if (x1 == null) { return struct?.toString() }
    return "(${x1},${y1})-(${x2},${y2})"
}

/** SnapshotCapabilitiesStruct[]: 0=Resolution, 1=MaxFrameRate, 2=ImageCodec, 3=RequiresEncodedPixels, 4=RequiresHardwareEncoder */
private String decodeSnapshotCapabilities(Object value) {
    if (!(value instanceof List)) { return value?.toString() }
    List entries = value as List
    List<String> out = entries.collect { Object entry ->
        String res = decodeResolution(safeStructField(entry, 0))
        Integer fps = safeStructInt(entry, 1)
        Integer codec = safeStructInt(entry, 2)
        Object needsPixels = safeStructField(entry, 3)
        Object needsHw = safeStructField(entry, 4)
        String s = "${res} @${fps ?: '?'}fps codec=${codec == 0 ? 'JPEG' : codec}"
        if (needsPixels?.toString() == 'true') { s += ' +encodedPixels' }
        if (needsHw?.toString() == 'true')     { s += ' +hwEncoder' }
        return s
    }
    return out.join('; ')
}

/** RateDistortionTradeOffPointsStruct[]: 0=Codec, 1=Resolution, 2=MinBitRate */
private String decodeRateDistortionPoints(Object value) {
    if (!(value instanceof List)) { return value?.toString() }
    List entries = value as List
    if (entries.isEmpty()) { return '(none)' }
    return entries.collect { Object entry ->
        Integer codec = safeStructInt(entry, 0)
        String res = decodeResolution(safeStructField(entry, 1))
        Integer bitRate = safeStructInt(entry, 2)
        return "${res} codec=${codec == 0 ? 'H264' : codec} minBitRate=${bitRate}"
    }.join('; ')
}

/** The PresetIDs the camera actually has saved, for validating a move/remove. */
private List<Integer> extractPresetIds(Object value) {
    if (!(value instanceof List)) { return null }
    return (value as List).collect { safeStructInt(it, 0) }.findAll { it != null }
}

/** MPTZPresetStruct[]: 0=PresetID, 1=Name, 2=Settings */
private String decodePtzPresets(Object value) {
    if (!(value instanceof List)) { return value?.toString() }
    List entries = value as List
    if (entries.isEmpty()) { return '(none saved)' }
    return entries.collect { Object entry ->
        Integer id = safeStructInt(entry, 0)
        Object nm = safeStructField(entry, 1)
        return "${id}:${nm ?: ''}"
    }.join(', ')
}

/** Decode an integer bitmap into a '[Name, Name]' string using a bit->name map. */
private String decodeBitmap(Integer bitmap, Map<Integer, String> bitNames) {
    if (bitmap == null) { return '[none]' }
    List<String> names = []
    int knownMask = 0
    bitNames.each { Integer bit, String name ->
        knownMask |= (1 << bit)
        if ((bitmap >> bit) & 1) { names << name }
    }
    int unknownBits = bitmap & ~knownMask
    if (unknownBits) { names << "Unknown(0x${HexUtils.integerToHexString(unknownBits, 2)})" }
    return names ? "[${names.join(', ')}]" : '[none]'
}

/** 'MaxPresets' -> 'maxPresets' (state.cameraAttr keys are camelCase) */
private String lowerFirst(String s) {
    if (!s) { return s }
    return s.substring(0, 1).toLowerCase() + s.substring(1)
}

/** Decode the 0x0551 FeatureMap → "[Audio, Video, Snapshot, Privacy, Speaker, NightVision]" style string */
private String decodeCameraFeatureMap(Integer featureMap) {
    return decodeBitmap(featureMap, CAMERA_FEATURE_BITS)
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
@Field static final String CAMERA_WIKI        = 'Documentation:'
@Field static final String CAMERA_COMM_LINK   = 'https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252'
@Field static final String CAMERA_GITHUB_LINK = 'https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/blob/main/docs/user/drivers/camera-av-stream.md'

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
