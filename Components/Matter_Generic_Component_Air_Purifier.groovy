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
 * ver. 1.1.0  2025-01-10 kkossev   - added ping command and RTT monitoring via matterHealthStatusLib
 * ver. 1.2.0  2025-01-18 kkossev   - added ALPSTUGA Air Quality Monitor support
 * ver. 1.2.1  2026-01-29 kkossev   - added common library matterCommonLib
 * ver. 1.2.2  2026-02-19 kkossev   - moved common methods to matterCommonLib
 * ver. 1.2.3  2026-05-24 kkossev   - featureMap bug fix
 * ver. 1.2.4  2026-07-25 kkossev   - bug fixes; removed parse(String); added HEPA/carbon filter monitoring (0x0071/0x0072); Resource Monitoring moved into child
 * ver. 1.2.5  2026-08-16 kkossev   - 'auto' is now reported; new 'filterDaysRemaining'; removed 'Child lock' and 'Set Indicator Status'; filter reset is refused when the device does not accept it
 *
*/

import groovy.transform.Field

@Field static final String matterComponentAirPurifierVersion = '1.2.5'
@Field static final String matterComponentAirPurifierStamp   = '2026/08/16 9:58 PM'

@Field static final Boolean _DEBUG_AIR_PURIFIER = false    // make it FALSE for production!
@Field static final Integer RESOURCE_MONITORING_COALESCE_MS = 250
@Field static final Long MATTER_EPOCH_OFFSET_S = 946684800L   // Matter epoch-s is seconds from 2000-01-01 UTC; Unix epoch-s = Matter epoch-s + this
@Field static final Integer DEFAULT_FILTER_LIFE_TIME_DAYS = 180
@Field static final String FILTER_DUE_CHECK_CRON = '0 7 0 * * ?'   // daily, shortly after midnight

metadata {
    definition(name: 'Matter Generic Component Air Purifier', namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: 'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/development/Components/Matter_Generic_Component_Air_Purifier.groovy') {
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
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'CarbonDioxideMeasurement'

        command    'identify'
        command    'getInfo'
        // Commands for devices.Ikea_E2006
        command 'setSpeed', [[name:'Fan speed*', type:'ENUM', description:'Select the desired fan speed', constraints:SUPPORTED_FAN_SPEEDS]]
        command 'toggle'
        command 'resetFilterCondition', [[name:'Filter*', type:'ENUM', description:'Reset the filter condition counter - use it after physically replacing the filter', constraints:['HEPA', 'activated carbon']]]
        
        // Attributes for devices.Ikea_E2006
        attribute 'airQuality', 'enum', ['Unknown', 'Good', 'Fair', 'Moderate', 'Poor', 'VeryPoor', 'ExtremelyPoor']   // matches AirQualityEnum (Matter spec 2.9.5.1)
        attribute 'filterUsage', 'number'                       // HEPA filter, cluster 0x0071 - percent USED (100 = spent)
        attribute 'carbonFilterStatus', 'enum', ['normal', 'replace']    // activated carbon filter, cluster 0x0072
        attribute 'carbonFilterUsage', 'number'                 // activated carbon filter, cluster 0x0072 - percent USED
        attribute 'filterInPlace', 'enum', ['present', 'not present']
        attribute 'carbonFilterInPlace', 'enum', ['present', 'not present']
        attribute 'filterLastChanged', 'number'                 // Matter epoch-s
        attribute 'carbonFilterLastChanged', 'number'           // Matter epoch-s
        attribute 'filterDaysRemaining', 'number'               // HEPA filter, estimated in the driver from filterLastChanged or filterUsage + the 'Filter life time' preference
        attribute 'pm25', 'number'
        attribute 'auto', 'enum', ['on', 'off']


        if (_DEBUG_AIR_PURIFIER) {
            command 'getBridgeInfo', [
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
            name: 'co2ReportDelta', type: 'enum',
            title: 'CO₂ report frequency',
            description: '<small>Adjust how often the device sends its CO₂ sensor data.</small>',
            options: [
                '05': 'Very High - report changes of +/- 5 ppm',
                '10': 'High - report changes of +/- 10 ppm',
                '25': 'Medium - report changes of +/- 25 ppm',
                '50': 'Low - report changes of +/- 50 ppm',
                '100': 'Very Low - report changes of +/- 100 ppm'
            ],
            defaultValue: '10',
            required: true
        )
        input(
            name: 'filterLifeTime', type: 'enum',
            title: 'Filter life time',
            description: '<small>Expected time between HEPA filter changes (default 6 months). Used by Hubitat to estimate <b>filterDaysRemaining</b>, from the filter reset date when the device reports one, otherwise from the reported filter condition - it is not sent to the device.</small>',
            options: [
                 '90': '3 months',
                '180': '6 months',
                '270': '9 months',
                '360': '1 year'
            ],
            defaultValue: '180',
            required: true
        )
    }
}

// Fields for devices.Ikea_E2006
@Field static final List<String> SUPPORTED_FAN_SPEEDS = [
    'auto', 'low', 'medium-low', 'medium', 'medium-high', 'high', 'off'
]



// Hubitat platform 2.5.1.132+ transaction callbacks. The parent passes these Maps unchanged.
void parse(Map descMap) {
    checkDriverVersion()
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

// parse commands from parent
void parse(List<Map> description) {
    checkDriverVersion()
    if (logEnable) { log.debug "${description}" }
    description.each { d ->
        if (d.name == 'rtt') {
            // Delegate to health status library
            parseRttEvent(d)
        }
        else if (d.name == 'handleInChildDriver') {
            if (!(d.value instanceof Map)) {
                logWarn "handleInChildDriver: expected Map, received ${d.value}"
                return
            }
            processMatterMap(d.value as Map, d.isRefresh == true, d.isDiscovery == true)
        }
        else {
            logDescriptionText(d.descriptionText)
            sendEvent(d)
            if (d.name == 'speed') { syncAutoAttribute(d.value as String) }
        }
    }
}

// The parent maps FanMode 5 (Auto) to speed 'auto' - mirror that into the 'auto' attribute.
// Only 'auto' counts: FanMode 6 is mapped to 'smart', which is not a legal FanControl speed at all (see docs/TODO.md 3.2).
private void syncAutoAttribute(final String speed) {
    if (speed == null) { return }
    String autoValue = speed == 'auto' ? 'on' : 'off'
    if (device.currentValue('auto') == autoValue) { return }
    String descriptionText = "${device.displayName} auto is ${autoValue}"
    sendEvent(name: 'auto', value: autoValue, descriptionText: descriptionText)
    logDescriptionText(descriptionText)
}

void identify() {
    if (logEnable) { log.debug "${device.displayName} identifying ..." }
    parent?.componentIdentify(device)
}

void on() {
    if (logEnable) { log.debug "${device.displayName} turning on ..." }
    parent?.componentOn(device)
}

void off() {
    if (logEnable) { log.debug "${device.displayName} turning off ..." }
    parent?.componentOff(device)
}

void toggle() {
    if (device.currentValue('switch', true) == 'on') { off() }
    else { on() }
}

void setSpeed(String speed) {
    if (logEnable) { log.debug "Setting speed to: ${speed}" }
    if (!(speed in ['off', 'low', 'medium-low', 'medium', 'medium-high', 'high', 'on', 'auto'])) {
        if (logEnable) { log.warn "Unknown speed: ${speed}" }
        return
    }
    parent?.componentSetSpeed(device, speed)
}

void cycleSpeed() {
    String curSpeed = device.currentValue('speed', true)
    if (logEnable) { log.debug "Current speed is: ${curSpeed}" }
    String newSpeed
    switch (curSpeed) {
        case 'high':
        case 'off':
            newSpeed = 'low'
            break
        case 'low':
            newSpeed = 'medium-low'
            break
        case 'medium-low':
            newSpeed = 'medium'
            break
        case 'medium':
            newSpeed = 'medium-high'
            break
        case 'medium-high':
            newSpeed = 'high'
            break
        default:
            if (logEnable) { log.warn "Unknown current speed: ${curSpeed}" }
            return
    }
    if (logEnable) { log.debug "Cycling speed to: ${newSpeed}" }
    parent?.componentSetSpeed(device, newSpeed)
}

// Resource Monitoring ResetCondition - cluster 0x0071 (HEPA) / 0x0072 (activated carbon)
void resetFilterCondition(String filter = 'HEPA') {
    String normalizedFilter = filter?.trim()?.toLowerCase()
    Integer clusterInt
    String filterName
    if (normalizedFilter == 'hepa') {
        clusterInt = 0x0071
        filterName = 'HEPA filter'
    }
    else if (normalizedFilter?.contains('carbon')) {
        clusterInt = 0x0072
        filterName = 'activated carbon filter'
    }
    else {
        logWarn "resetFilterCondition: unsupported filter '${filter}'"
        return
    }

    String clusterHex = hex4(clusterInt)
    if (!isClusterSupported(clusterHex)) {
        logWarn "resetFilterCondition: cluster 0x${clusterHex} is not in the ServerList ${getServerList()} - this device has no ${filterName}"
        return
    }
    // The cluster being present does not mean ResetCondition (command 0x0000) is implemented: an IKEA STARKVIND
    // behind a DIRIGERA bridge advertises 0x0071 with an EMPTY AcceptedCommandList, so the invoke was silently
    // swallowed and the counter never moved (confirmed on device 2026-08-16). The list is only known once
    // getInfo() has read 0xFFF9, so an unknown list is not treated as a refusal.
    Object acceptedCommands = getResourceMonitoringState(clusterInt)['acceptedCommands']
    if (acceptedCommands instanceof List && !('0x0000' in (acceptedCommands as List))) {
        logWarn "resetFilterCondition: this device does not accept the ResetCondition command on cluster 0x${clusterHex} " +
                "(AcceptedCommandList ${acceptedCommands}) - the ${filterName} counter has to be reset on the device itself"
        return
    }
    Integer endpointInt = getDeviceNumber()
    if (endpointInt == null) { return }

    List<Map<String, String>> cmdFields = []
    String cmd = matter.invoke(endpointInt, clusterInt, 0x0000, cmdFields)
    logInfo "resetting the ${filterName} condition"
    logDebug "resetFilterCondition: sending ResetCondition to endpoint ${endpointInt}, cluster 0x${clusterHex}: ${cmd}"
    parent?.sendToDevice(cmd)
}

// Called when the device is first created
void installed() {
    setStateDriverVersion(driverVersionAndTimeStamp())
    scheduleFilterDueCheck()
    logInfo 'driver installed'
}

// Called when the device is removed
void uninstalled() {
    logInfo 'driver uninstalled'
}

// Called when the settings are updated
void updated() {
    checkDriverVersion()
    logInfo 'driver configuration updated'
    if (logEnable) {
        log.debug settings
        runIn(14400, 'logsOff')
    }
    scheduleFilterDueCheck()
    updateFilterDaysRemaining()     // the 'Filter life time' preference may have just changed
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private void logsOff() {
    log.warn "debug logging disabled for ${device.displayName} "
    device.updateSetting('logEnable', [value: 'false', type: 'bool'] )
}


void refresh() {
    checkDriverVersion()
    updateFilterDaysRemaining()
    parent?.componentRefresh(this.device)
}

String driverVersionAndTimeStamp() {
    return "${matterComponentAirPurifierVersion} (${matterComponentAirPurifierStamp})".toString()
}

String getStateDriverVersion() {
    return state.driverVersion
}

void setStateDriverVersion(final String version) {
    state.driverVersion = version
}

void checkDriverVersion() {
    String currentVersion = driverVersionAndTimeStamp()
    String previousVersion = getStateDriverVersion()
    if (previousVersion == currentVersion) { return }

    logDebug "updating the driver from version ${previousVersion ?: 'not stored'} to ${currentVersion}"
    migrateDriverState(previousVersion, currentVersion)
    setStateDriverVersion(currentVersion)
    logInfo "driver updated from version ${previousVersion ?: 'not stored'} to ${currentVersion}"
}

// Reserved for non-destructive state migrations required by future child-driver versions.
private void migrateDriverState(final String previousVersion, final String currentVersion) {
    logTrace "migrateDriverState: migrating from ${previousVersion ?: 'not stored'} to ${currentVersion}"
    // 1.2.5 - 'Set Indicator Status' and 'Child lock' are gone: Matter has no such attribute, so neither ever reached the device.
    if (device.currentState('indicatorStatus') != null) {
        device.deleteCurrentState('indicatorStatus')
        logDebug 'migrateDriverState: removed the obsolete indicatorStatus attribute'
    }
    if (settings?.childLock != null) {
        device.removeSetting('childLock')
        logDebug 'migrateDriverState: removed the obsolete childLock preference'
    }
    // 1.2.5 - existing installs must pick up the daily filter-due job without a Save Preferences.
    scheduleFilterDueCheck()
    updateFilterDaysRemaining()
}

void processMatterMap(final Map descMap, final boolean isRefresh = false, final boolean isDiscovery = false) {
    Integer clusterInt = nativeMatterInt(descMap.clusterInt)
    Integer attrInt = nativeMatterInt(descMap.attrInt)
    if (clusterInt == null || attrInt == null) {
        logWarn "processMatterMap: missing or invalid native identifiers: clusterInt=${descMap.clusterInt} attrInt=${descMap.attrInt}"
        return
    }

    boolean isInfoMode = state.states?.isInfo == true
    String prefix = isInfoMode ? "[${hex4(clusterInt)}_${hex4(attrInt)}] " : ''
    Object value = descMap.value

    if (attrInt in [0xFFF8, 0xFFF9, 0xFFFB, 0xFFFC, 0xFFFD]) {
        processGlobalMatterAttribute(clusterInt, attrInt, value, isInfoMode, prefix)
        return
    }

    switch (clusterInt) {
        case 0x005B:
            processAirQualityAttribute(attrInt, value, isRefresh, isDiscovery)
            break
        case 0x0071:
        case 0x0072:
            processResourceMonitoringAttribute(clusterInt, attrInt, value, isInfoMode, prefix, isRefresh, isDiscovery)
            break
        case 0x040D:
        case 0x042A:
            processConcentrationAttribute(clusterInt, attrInt, value, isInfoMode, prefix, isRefresh, isDiscovery)
            break
        default:
            logWarn "processMatterMap: unsupported cluster 0x${hex4(clusterInt)} attribute 0x${hex4(attrInt)}"
            break
    }
}

private Integer nativeMatterInt(final Object value) {
    return value instanceof Number ? (value as Number).intValue() : null
}

private void processAirQualityAttribute(final Integer attrInt, final Object value, final boolean isRefresh,
                                        final boolean isDiscovery) {
    if (attrInt != 0x0000) {
        logWarn "processAirQualityAttribute: unsupported attribute 0x${hex4(attrInt)}"
        return
    }
    if (!(value instanceof Number)) {
        logWarn "processAirQualityAttribute: expected Number for AirQuality, received ${value}"
        return
    }

    Integer enumValue = (value as Number).intValue()
    String eventValue = AirQualityEnum[enumValue]
    if (eventValue == null) {
        logWarn "processAirQualityAttribute: unknown AirQuality value ${enumValue}"
        return
    }
    if (!isRefresh && !isDiscovery && state.lastAirQuality == eventValue) {
        logDebug "AirQuality unchanged: ${eventValue} - event suppressed"
        return
    }

    state.lastAirQuality = eventValue
    String descriptionText = "${device.displayName} AirQuality: ${eventValue} (raw:${enumValue})"
    emitReadingEvent([name: 'airQuality', value: eventValue, descriptionText: descriptionText], isRefresh, isDiscovery)
}

private void processResourceMonitoringAttribute(final Integer clusterInt, final Integer attrInt, final Object value,
                                                final boolean isInfoMode, final String prefix, final boolean isRefresh,
                                                final boolean isDiscovery) {
    String filterName = resourceFilterName(clusterInt)
    String attrName = ResourceMonitoringClusterAttributes[attrInt] ?: "attribute 0x${hex4(attrInt)}"
    if (value == null) {
        if (isInfoMode) { logInfo "${prefix}${filterName} ${attrName}: <i>not available</i>" }
        else { logDebug "${filterName} ${attrName} is not available" }
        return
    }

    switch (attrInt) {
        case 0x0000: // Condition
            Integer condition = resourceInteger(value, 0, 100, "${filterName} Condition")
            if (condition == null) { return }
            storeResourceMonitoringValue(clusterInt, 'condition', condition)
            if (isInfoMode) { logInfo "${prefix}${filterName} Condition: ${condition}%" }
            queueResourceUsage(clusterInt, isRefresh, isDiscovery)
            break
        case 0x0001: // DegradationDirection
            Integer direction = resourceInteger(value, 0, 1, "${filterName} DegradationDirection")
            if (direction == null) { return }
            storeResourceMonitoringValue(clusterInt, 'direction', direction)
            if (isInfoMode) {
                logInfo "${prefix}${filterName} DegradationDirection: ${ResourceMonitoringDegradationDirection[direction]}"
            }
            queueResourceUsage(clusterInt, isRefresh, isDiscovery)
            break
        case 0x0002: // ChangeIndication
            Integer indication = resourceInteger(value, 0, 2, "${filterName} ChangeIndication")
            if (indication == null) { return }
            processResourceChangeIndication(clusterInt, indication, isRefresh, isDiscovery)
            break
        case 0x0003: // InPlaceIndicator
            Boolean inPlace = resourceBoolean(value, "${filterName} InPlaceIndicator")
            if (inPlace == null) { return }
            processResourceInPlace(clusterInt, inPlace, isRefresh, isDiscovery)
            break
        case 0x0004: // LastChangedTime
            Long lastChanged = resourceNonNegativeLong(value, "${filterName} LastChangedTime")
            if (lastChanged == null) { return }
            processResourceLastChanged(clusterInt, lastChanged, isRefresh, isDiscovery)
            break
        case 0x0005: // ReplacementProductList
            if (!(value instanceof List)) {
                logWarn "processResourceMonitoringAttribute: expected List for ${filterName} ReplacementProductList, received ${value}"
                return
            }
            if (isInfoMode) { logInfo "${prefix}${filterName} ReplacementProductList: ${value}" }
            break
        default:
            logTrace "processResourceMonitoringAttribute: unsupported cluster 0x${hex4(clusterInt)} attribute 0x${hex4(attrInt)} value ${value}"
            break
    }
}

private void queueResourceUsage(final Integer clusterInt, final boolean isRefresh, final boolean isDiscovery) {
    Map filterState = getResourceMonitoringState(clusterInt)
    filterState.pendingRefresh = filterState.pendingRefresh == true || isRefresh
    filterState.pendingDiscovery = filterState.pendingDiscovery == true || isDiscovery
    setResourceMonitoringState(clusterInt, filterState)
    String callback = clusterInt == 0x0071 ? 'emitPendingHepaFilterUsage' : 'emitPendingCarbonFilterUsage'
    runInMillis(RESOURCE_MONITORING_COALESCE_MS, callback, [overwrite: true])
}

void emitPendingHepaFilterUsage() {
    emitPendingResourceUsage(0x0071)
}

void emitPendingCarbonFilterUsage() {
    emitPendingResourceUsage(0x0072)
}

private void emitPendingResourceUsage(final Integer clusterInt) {
    Map filterState = getResourceMonitoringState(clusterInt)
    boolean isRefresh = filterState.pendingRefresh == true
    boolean isDiscovery = filterState.pendingDiscovery == true
    filterState.pendingRefresh = false
    filterState.pendingDiscovery = false
    setResourceMonitoringState(clusterInt, filterState)

    Integer condition = filterState.condition instanceof Number ? (filterState.condition as Number).intValue() : null
    Integer direction = filterState.direction instanceof Number ? (filterState.direction as Number).intValue() : null
    if (condition == null || direction == null) {
        logDebug "${resourceFilterName(clusterInt)} usage is waiting for both Condition and DegradationDirection"
        return
    }

    Integer usage = direction == 0 ? condition : 100 - condition
    usage = Math.max(0, Math.min(100, usage))
    String eventName = clusterInt == 0x0071 ? 'filterUsage' : 'carbonFilterUsage'
    String directionName = ResourceMonitoringDegradationDirection[direction]
    String descriptionText = "${device.displayName} ${resourceFilterName(clusterInt)} usage: ${usage}% (Condition:${condition}% direction:${directionName})"
    emitResourceReading(clusterInt, 'lastUsage', eventName, usage, '%', descriptionText, isRefresh, isDiscovery)
    // The usage is passed in rather than read back: currentValue() does not reliably reflect the event just sent.
    if (clusterInt == 0x0071) { computeFilterDaysRemaining(null, usage) }
}

private void processResourceChangeIndication(final Integer clusterInt, final Integer indication,
                                             final boolean isRefresh, final boolean isDiscovery) {
    String eventValue = indication == 0 ? 'normal' : 'replace'
    String eventName = clusterInt == 0x0071 ? 'filterStatus' : 'carbonFilterStatus'
    String descriptionText = "${device.displayName} ${resourceFilterName(clusterInt)} status: ${eventValue} (ChangeIndication:${ResourceMonitoringChangeIndication[indication]})"
    emitResourceReading(clusterInt, 'lastStatus', eventName, eventValue, null, descriptionText, isRefresh, isDiscovery)
}

private void processResourceInPlace(final Integer clusterInt, final Boolean inPlace,
                                    final boolean isRefresh, final boolean isDiscovery) {
    String eventValue = inPlace ? 'present' : 'not present'
    String eventName = clusterInt == 0x0071 ? 'filterInPlace' : 'carbonFilterInPlace'
    String descriptionText = "${device.displayName} ${resourceFilterName(clusterInt)} is ${eventValue}"
    emitResourceReading(clusterInt, 'lastInPlace', eventName, eventValue, null, descriptionText, isRefresh, isDiscovery)
}

private void processResourceLastChanged(final Integer clusterInt, final Long lastChanged,
                                        final boolean isRefresh, final boolean isDiscovery) {
    String eventName = clusterInt == 0x0071 ? 'filterLastChanged' : 'carbonFilterLastChanged'
    String descriptionText = "${device.displayName} ${resourceFilterName(clusterInt)} last changed: ${lastChanged} epoch-s"
    emitResourceReading(clusterInt, 'lastChanged', eventName, lastChanged, null, descriptionText, isRefresh, isDiscovery)
    if (clusterInt == 0x0071) { computeFilterDaysRemaining(lastChanged, null) }
}

/*
 * HEPA filter change estimate.
 *
 * Matter's Resource Monitoring cluster has no 'filter life time' attribute - it reports Condition (a percentage,
 * surfaced as filterUsage) and, optionally, LastChangedTime. The 'Filter life time' preference supplies the
 * missing lifetime and the estimate is made here, in the driver; nothing is written to the device.
 *
 * Two sources, in order of preference:
 *   1. LastChangedTime (0x0004) - count the days elapsed since the filter was last reset.
 *   2. Condition (0x0000, surfaced as filterUsage) - scale the lifetime by the percentage still left.
 * Most bridged purifiers only offer the second: an IKEA STARKVIND behind a DIRIGERA bridge advertises an
 * 0x0071 AttributeList of [0x0000, 0x0001, 0x0002] and never reports LastChangedTime (confirmed 2026-08-16).
 */
void scheduleFilterDueCheck() {
    // Neither source moves on its own between reports - LastChangedTime only changes on a filter reset - so the
    // remaining days have to be recomputed on a timer.
    unschedule('updateFilterDaysRemaining')
    schedule(FILTER_DUE_CHECK_CRON, 'updateFilterDaysRemaining')
    logDebug "scheduleFilterDueCheck: the filter due check is scheduled daily (${FILTER_DUE_CHECK_CRON})"
}

// Public and deliberately parameterless - this is the schedule() target, so it must resolve by name alone.
void updateFilterDaysRemaining() {
    computeFilterDaysRemaining(null, null)
}

private void computeFilterDaysRemaining(final Long lastChangedPar, final Integer usagePar) {
    Integer lifeTimeDays = safeToInt(settings?.filterLifeTime, DEFAULT_FILTER_LIFE_TIME_DAYS)
    if (lifeTimeDays <= 0) { lifeTimeDays = DEFAULT_FILTER_LIFE_TIME_DAYS }

    Integer daysRemaining
    String basis

    Long lastChanged = lastChangedPar
    if (lastChanged == null) {
        Object currentValue = device.currentValue('filterLastChanged')
        lastChanged = currentValue instanceof Number ? (currentValue as Number).longValue() : null
    }

    if (lastChanged != null && lastChanged > 0) {
        // Preferred: count the days elapsed since the device's own LastChangedTime (0x0004).
        Long nowSeconds = (now() / 1000L) as Long
        Long elapsedSeconds = nowSeconds - (lastChanged + MATTER_EPOCH_OFFSET_S)
        if (elapsedSeconds < 0) {
            logWarn "updateFilterDaysRemaining: LastChangedTime ${lastChanged} epoch-s is in the future - check the device clock"
            elapsedSeconds = 0
        }
        Integer elapsedDays = (elapsedSeconds / 86400L) as Integer
        daysRemaining = Math.max(0, lifeTimeDays - elapsedDays)
        basis = "${elapsedDays} of ${lifeTimeDays} days used"
    }
    else {
        // Fallback: scale the configured lifetime by the filter life still left. Confirmed 2026-08-16 on an
        // IKEA STARKVIND behind a DIRIGERA bridge - its 0x0071 AttributeList is [0x0000, 0x0001, 0x0002] only,
        // so LastChangedTime is never reported and the elapsed-time path above can never run there.
        // Note the explicit null test: a usage of 0 is legitimate and would be swallowed by an Elvis operator.
        Integer usage = usagePar
        if (usage == null) {
            Object usageValue = device.currentValue('filterUsage')
            usage = usageValue instanceof Number ? (usageValue as Number).intValue() : null
        }
        if (usage == null) {
            logDebug 'updateFilterDaysRemaining: the device reports neither LastChangedTime nor Condition - no estimate is possible'
            return
        }
        usage = Math.max(0, Math.min(100, usage))
        daysRemaining = Math.round(lifeTimeDays * (100 - usage) / 100.0d) as Integer
        basis = "${usage}% of a ${lifeTimeDays} day filter life used"
    }

    if (device.currentValue('filterDaysRemaining') == daysRemaining) {
        logDebug "filterDaysRemaining unchanged: ${daysRemaining} - event suppressed"
        return
    }
    String descriptionText = daysRemaining > 0
        ? "${device.displayName} HEPA filter is due in ${daysRemaining} days (${basis})"
        : "${device.displayName} HEPA filter is due for replacement (${basis})"
    sendEvent(name: 'filterDaysRemaining', value: daysRemaining, unit: 'days', descriptionText: descriptionText)
    logDescriptionText(descriptionText)
}

private void emitResourceReading(final Integer clusterInt, final String stateKey, final String eventName,
                                 final Object value, final String unit, final String descriptionText,
                                 final boolean isRefresh, final boolean isDiscovery) {
    Map filterState = getResourceMonitoringState(clusterInt)
    if (!isRefresh && !isDiscovery && filterState[stateKey] == value) {
        logDebug "${eventName} unchanged: ${value} - event suppressed"
        return
    }
    filterState[stateKey] = value
    setResourceMonitoringState(clusterInt, filterState)

    Map event = [name: eventName, value: value, descriptionText: descriptionText]
    if (unit != null) { event.unit = unit }
    emitReadingEvent(event, isRefresh, isDiscovery)
}

private Map getResourceMonitoringState(final Integer clusterInt) {
    Object allState = state.resourceMonitoring
    Object filterState = allState instanceof Map ? (allState as Map)[hex4(clusterInt)] : null
    return filterState instanceof Map ? new LinkedHashMap(filterState as Map) : [:]
}

private void setResourceMonitoringState(final Integer clusterInt, final Map filterState) {
    Map allState = state.resourceMonitoring instanceof Map ? new LinkedHashMap(state.resourceMonitoring as Map) : [:]
    allState[hex4(clusterInt)] = new LinkedHashMap(filterState)
    state.resourceMonitoring = allState
}

private void storeResourceMonitoringValue(final Integer clusterInt, final String key, final Object value) {
    Map filterState = getResourceMonitoringState(clusterInt)
    filterState[key] = value
    setResourceMonitoringState(clusterInt, filterState)
}

private Integer resourceInteger(final Object value, final Integer minimum, final Integer maximum, final String label) {
    if (!(value instanceof Number)) {
        logWarn "${label}: expected Number, received ${value}"
        return null
    }
    Double numericValue = (value as Number).doubleValue()
    if (Double.isNaN(numericValue) || Double.isInfinite(numericValue) || numericValue != Math.rint(numericValue) ||
        numericValue < minimum || numericValue > maximum) {
        logWarn "${label}: invalid value ${value}; expected an integer in ${minimum}..${maximum}"
        return null
    }
    return (value as Number).intValue()
}

private Long resourceNonNegativeLong(final Object value, final String label) {
    if (!(value instanceof Number)) {
        logWarn "${label}: expected Number, received ${value}"
        return null
    }
    Double numericValue = (value as Number).doubleValue()
    if (Double.isNaN(numericValue) || Double.isInfinite(numericValue) || numericValue != Math.rint(numericValue) || numericValue < 0) {
        logWarn "${label}: invalid non-negative integer value ${value}"
        return null
    }
    return (value as Number).longValue()
}

private Boolean resourceBoolean(final Object value, final String label) {
    if (value instanceof Boolean) { return value as Boolean }
    Integer numericValue = resourceInteger(value, 0, 1, label)
    return numericValue == null ? null : numericValue == 1
}

private String resourceFilterName(final Integer clusterInt) {
    return clusterInt == 0x0071 ? 'HEPA filter' : 'activated carbon filter'
}

private void processConcentrationAttribute(final Integer clusterInt, final Integer attrInt, final Object value,
                                           final boolean isInfoMode, final String prefix, final boolean isRefresh,
                                           final boolean isDiscovery) {
    switch (attrInt) {
        case 0x0000:
            processConcentrationMeasuredValue(clusterInt, value, isRefresh, isDiscovery)
            break
        case 0x0001:
        case 0x0002:
        case 0x0007:
            logNativeConcentrationInfoValue(clusterInt, attrInt, value, isInfoMode, prefix)
            break
        case 0x0008:
        case 0x0009:
        case 0x000A:
            logNativeConcentrationEnum(clusterInt, attrInt, value, isInfoMode, prefix)
            break
        default:
            logWarn "processConcentrationAttribute: unsupported cluster 0x${hex4(clusterInt)} attribute 0x${hex4(attrInt)}"
            break
    }
}

private void processConcentrationMeasuredValue(final Integer clusterInt, final Object value, final boolean isRefresh,
                                               final boolean isDiscovery) {
    String measurementName = clusterInt == 0x040D ? 'CO\u2082' : 'PM2.5'
    String unit = clusterInt == 0x040D ? 'ppm' : '\u00B5g/m\u00B3'
    if (value == null) {
        logDebug "${measurementName} is not available"
        return
    }
    if (!(value instanceof Number)) {
        logWarn "processConcentrationMeasuredValue: expected Number for ${measurementName}, received ${value}"
        return
    }

    Double measuredValue = (value as Number).doubleValue()
    if (Double.isNaN(measuredValue) || Double.isInfinite(measuredValue)) {
        logWarn "processConcentrationMeasuredValue: invalid ${measurementName} value ${measuredValue}"
        return
    }
    Integer roundedValue = Math.round(measuredValue) as Integer
    Integer threshold = clusterInt == 0x040D
        ? safeToInt(settings.co2ReportDelta, 10)
        : safeToInt(settings.pm25ReportDelta, 3)
    String stateName = clusterInt == 0x040D ? 'lastCO2' : 'lastPM25'
    Integer lastValue = state[stateName] instanceof Number ? (state[stateName] as Number).intValue() : null

    logDebug "${measurementName} native: ${measuredValue}; rounded: ${roundedValue} ${unit}"
    if (!isRefresh && !isDiscovery && lastValue != null && Math.abs(roundedValue - lastValue) < threshold) {
        logDebug "${measurementName} change ${roundedValue - lastValue} ${unit} below threshold ${threshold} - event suppressed"
        return
    }

    state[stateName] = roundedValue
    String eventName = clusterInt == 0x040D ? 'carbonDioxide' : 'pm25'
    String descriptionText = "${device.displayName} ${measurementName}: ${roundedValue} ${unit}"
    emitReadingEvent([name: eventName, value: roundedValue, unit: unit, descriptionText: descriptionText], isRefresh, isDiscovery)
}

private void emitReadingEvent(final Map event, final boolean isRefresh, final boolean isDiscovery) {
    if (isRefresh) {
        event.descriptionText = "${event.descriptionText} [refresh]".toString()
        event.isStateChange = true
        event.isRefresh = true
    }
    if (isDiscovery) {
        event.descriptionText = "${event.descriptionText} [discovery]".toString()
        event.isStateChange = true
        event.isDiscovery = true
    }
    sendEvent(event)
    logDescriptionText(event.descriptionText)
}

private void logDescriptionText(final Object descriptionText) {
    if (descriptionText == null) { return }
    String message = descriptionText.toString()
    String devicePrefix = "${device.displayName} ".toString()
    if (message.startsWith(devicePrefix)) {
        message = message.substring(devicePrefix.length())
    }
    logInfo message
}

private void logNativeConcentrationInfoValue(final Integer clusterInt, final Integer attrInt, final Object value,
                                             final boolean isInfoMode, final String prefix) {
    String clusterName = clusterInt == 0x040D ? 'CO\u2082' : 'PM2.5'
    String unit = clusterInt == 0x040D ? 'ppm' : '\u00B5g/m\u00B3'
    String attrName = ConcentrationMeasurementClusterAttributes[attrInt] ?: "attribute 0x${hex4(attrInt)}"
    if (value == null) {
        if (isInfoMode) { logInfo "${prefix}${clusterName} ${attrName}: <i>not available</i>" }
        return
    }
    if (!(value instanceof Number)) {
        logWarn "logNativeConcentrationInfoValue: expected Number for 0x${hex4(clusterInt)}/0x${hex4(attrInt)}, received ${value}"
        return
    }
    if (isInfoMode) {
        logInfo "${prefix}${clusterName} ${attrName}: ${(value as Number).doubleValue()} ${unit}"
    }
}

private void logNativeConcentrationEnum(final Integer clusterInt, final Integer attrInt, final Object value,
                                        final boolean isInfoMode, final String prefix) {
    if (!(value instanceof Number)) {
        logWarn "logNativeConcentrationEnum: expected Number for 0x${hex4(clusterInt)}/0x${hex4(attrInt)}, received ${value}"
        return
    }
    if (!isInfoMode) { return }

    Integer enumValue = (value as Number).intValue()
    String decoded
    switch (attrInt) {
        case 0x0008: decoded = decodeMeasurementUnit(enumValue); break
        case 0x0009: decoded = decodeMeasurementMedium(enumValue); break
        case 0x000A: decoded = decodeLevelValue(enumValue); break
    }
    String clusterName = clusterInt == 0x040D ? 'CO\u2082' : 'PM2.5'
    String attrName = ConcentrationMeasurementClusterAttributes[attrInt] ?: "attribute 0x${hex4(attrInt)}"
    logInfo "${prefix}${clusterName} ${attrName}: ${decoded}"
}

private void processGlobalMatterAttribute(final Integer clusterInt, final Integer attrInt, final Object value,
                                          final boolean isInfoMode, final String prefix) {
    if (!(clusterInt in [0x005B, 0x0071, 0x0072, 0x040D, 0x042A])) {
        logWarn "processGlobalMatterAttribute: unsupported cluster 0x${hex4(clusterInt)}"
        return
    }
    boolean isResourceMonitoring = clusterInt in [0x0071, 0x0072]
    String clusterName = clusterInt == 0x005B
        ? 'Air Quality'
        : (isResourceMonitoring ? resourceFilterName(clusterInt) : (clusterInt == 0x040D ? 'CO\u2082' : 'PM2.5'))
    Map<Integer, String> attributes = clusterInt == 0x005B
        ? AirQualityClusterAttributes
        : (isResourceMonitoring ? ResourceMonitoringClusterAttributes : ConcentrationMeasurementClusterAttributes)
    String attrName = attributes[attrInt] ?: "attribute 0x${hex4(attrInt)}"

    switch (attrInt) {
        case 0xFFFC:
        case 0xFFFD:
            Integer normalized = compatibilityGlobalInt(value, attrInt)
            if (normalized == null) {
                logWarn "processGlobalMatterAttribute: expected Number or compatibility HEX String for 0x${hex4(clusterInt)}/0x${hex4(attrInt)}, received ${value}"
                return
            }
            if (!isInfoMode) { return }
            if (attrInt == 0xFFFC) {
                if (isResourceMonitoring) {
                    logInfo "${prefix}${clusterName} ${attrName}: 0x${hex4(normalized)} (${normalized})"
                }
                else {
                    String decoded = clusterInt == 0x005B
                        ? decodeAirQualityFeatureMap(normalized)
                        : decodeConcentrationMeasurementFeatureMap(normalized)
                    logInfo "${prefix}${clusterName} ${attrName}: 0x${hex4(normalized)} (${normalized}) - Features: ${decoded}"
                }
            }
            else {
                logInfo "${prefix}${clusterName} ${attrName}: ${normalized}"
            }
            break
        case 0xFFF8:
        case 0xFFF9:
        case 0xFFFB:
            if (!(value instanceof List)) {
                logWarn "processGlobalMatterAttribute: expected List for 0x${hex4(clusterInt)}/0x${hex4(attrInt)}, received ${value}"
                return
            }
            List<String> formatted = formatCompatibilityIdentifierList(value as List)
            if (formatted == null) { return }
            // Remember which commands a Resource Monitoring cluster actually accepts - resetFilterCondition()
            // refuses to send ResetCondition to a device that does not list it.
            if (attrInt == 0xFFF9 && clusterInt in [0x0071, 0x0072]) {
                storeResourceMonitoringValue(clusterInt, 'acceptedCommands', formatted)
            }
            if (isInfoMode) { logInfo "${prefix}${clusterName} ${attrName}: ${formatted}" }
            break
    }
}

// Temporary compatibility boundary for scalar global values converted to HEX by newParseCompatibilityPatch().
private Integer compatibilityGlobalInt(final Object value, final Integer attrInt) {
    if (value instanceof Number) { return (value as Number).intValue() }
    if (attrInt in [0xFFFC, 0xFFFD] && value instanceof String && value ==~ /^[0-9A-Fa-f]+$/) {
        return safeHexToInt(value, null)
    }
    return null
}

// Temporary display boundary for global lists converted to HEX Strings by newParseCompatibilityPatch().
private List<String> formatCompatibilityIdentifierList(final List values) {
    List<String> formatted = []
    for (Object item : values) {
        Integer identifier
        if (item instanceof Number) {
            identifier = (item as Number).intValue()
        }
        else if (item instanceof String && item ==~ /^[0-9A-Fa-f]+$/) {
            identifier = safeHexToInt(item, null)
        }
        if (identifier == null) {
            logWarn "formatCompatibilityIdentifierList: unsupported list item ${item}"
            return null
        }
        formatted.add("0x${hex4(identifier)}".toString())
    }
    return formatted
}


void getInfo() {
    checkDriverVersion()
    // fingerprintData and the endpoint data value are persisted HEX boundaries, not callback fields.
    List<String> persistedServerList = getServerList()
    if (persistedServerList.isEmpty()) {
        logWarn "getInfo: ServerList is empty or not available"
        return
    }

    List<Integer> serverClusters = []
    for (Object persistedCluster : persistedServerList) {
        Integer clusterInt = safeHexToInt(persistedCluster, null)
        if (clusterInt == null) {
            logWarn "getInfo: invalid persisted ServerList value ${persistedCluster}"
            continue
        }
        serverClusters.add(clusterInt)
    }
    if (serverClusters.isEmpty()) {
        logWarn "getInfo: ServerList contains no valid cluster identifiers"
        return
    }

    String endpointHex = device.getDataValue('id')
    Integer endpoint = safeHexToInt(endpointHex, null)
    if (endpoint == null) {
        logWarn "getInfo: invalid persisted endpoint ${endpointHex}"
        return
    }

    logInfo "getInfo: Device supports clusters: ${serverClusters.collect { '0x' + hex4(it) }}"

    // Set state flags for info mode
    if (state.states == null) { state.states = [:] }
    if (state.lastTx == null) { state.lastTx = [:] }
    state.states.isInfo = true
    state.lastTx.infoTime = now()
    
    // Schedule job to turn off info mode after 10 seconds
    runIn(10, 'clearInfoMode')
    
    // Read ALL attributes from each supported cluster
    // Cluster 0x0006 - OnOff / Switch
    if (0x0006 in serverClusters) {
        logInfo "getInfo: reading all OnOff cluster attributes"
        parent?.readAttribute(endpoint, 0x0006, -1)
    }
    
    // Cluster 0x005B - Air Quality
    if (0x005B in serverClusters) {
        logInfo "getInfo: reading all Air Quality cluster attributes"
        parent?.readAttribute(endpoint, 0x005B, -1)
    }

    // Clusters 0x0071 / 0x0072 - Resource Monitoring
    if (0x0071 in serverClusters) {
        logInfo "getInfo: reading all HEPA Filter Monitoring cluster attributes"
        parent?.readAttribute(endpoint, 0x0071, -1)
    }
    if (0x0072 in serverClusters) {
        logInfo "getInfo: reading all Activated Carbon Filter Monitoring cluster attributes"
        parent?.readAttribute(endpoint, 0x0072, -1)
    }
    
    // Cluster 0x0402 - Temperature Measurement
    if (0x0402 in serverClusters) {
        logInfo "getInfo: reading all Temperature Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x0402, -1)
    }
    
    // Cluster 0x0405 - Relative Humidity Measurement
    if (0x0405 in serverClusters) {
        logInfo "getInfo: reading all Relative Humidity Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x0405, -1)
    }
    
    // Cluster 0x040D - Carbon Dioxide Concentration Measurement
    if (0x040D in serverClusters) {
        logInfo "getInfo: reading all CO₂ Concentration Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x040D, -1)
    }
    
    // Cluster 0x042A - PM2.5 Concentration Measurement
    if (0x042A in serverClusters) {
        logInfo "getInfo: reading all PM2.5 Concentration Measurement cluster attributes"
        parent?.readAttribute(endpoint, 0x042A, -1)
    }
    
    logInfo "getInfo: completed - check live logs and device data for results"
}

// Clear info mode flag (called by scheduled job)
void clearInfoMode() {
    if (state.states == null) { state.states = [:] }
    state.states.isInfo = false
    logDebug "clearInfoMode: info mode disabled"
}

// ============ FeatureMap Decoders ============

/**
 * Decode Air Quality cluster FeatureMap bitmap
 * Bit 0 (0x01): FAIR - Fair Air Quality
 * Bit 1 (0x02): MOD - Moderate Air Quality
 * Bit 2 (0x04): VPOOR - Very Poor Air Quality
 * Bit 3 (0x08): XPOOR - Extremely Poor Air Quality
 */
String decodeAirQualityFeatureMap(Integer featureMap) {
    List<String> features = []
    if (featureMap & 0x01) { features.add('Fair') }
    if (featureMap & 0x02) { features.add('Moderate') }
    if (featureMap & 0x04) { features.add('VeryPoor') }
    if (featureMap & 0x08) { features.add('ExtremelyPoor') }
    return features.isEmpty() ? 'None' : features.join(', ')
}

/**
 * Decode Concentration Measurement cluster FeatureMap bitmap
 * Applies to both CO₂ (0x040D) and PM2.5 (0x042A) clusters
 * Bit 0 (0x01): MEA - NumericMeasurement
 * Bit 1 (0x02): LEV - LevelIndication
 * Bit 2 (0x04): MED - MediumLevel
 * Bit 3 (0x08): CRI - CriticalLevel
 * Bit 4 (0x10): PEA - PeakMeasurement
 * Bit 5 (0x20): AVG - AverageMeasurement
 */
String decodeConcentrationMeasurementFeatureMap(Integer featureMap) {
    List<String> features = []
    if (featureMap & 0x01) { features.add('NumericMeasurement') }
    if (featureMap & 0x02) { features.add('LevelIndication') }
    if (featureMap & 0x04) { features.add('MediumLevel') }
    if (featureMap & 0x08) { features.add('CriticalLevel') }
    if (featureMap & 0x10) { features.add('PeakMeasurement') }
    if (featureMap & 0x20) { features.add('AverageMeasurement') }
    return features.isEmpty() ? 'None' : features.join(', ')
}

/**
 * Decode MeasurementUnitEnum
 * Per Matter spec Table 94
 */
String decodeMeasurementUnit(Integer value) {
    switch (value) {
        case 0: return 'PPM (parts per million)'
        case 1: return 'PPB (parts per billion)'
        case 2: return 'PPT (parts per trillion)'
        case 3: return 'mg/m³ (milligrams per cubic meter)'
        case 4: return 'μg/m³ (micrograms per cubic meter)'
        case 5: return 'ng/m³ (nanograms per cubic meter)'
        case 6: return 'pm/m³ (particles per cubic meter)'
        case 7: return 'Bq/m³ (becquerels per cubic meter)'
        default: return "Unknown (${value})"
    }
}

/**
 * Decode MeasurementMediumEnum
 * Per Matter spec Table 95
 */
String decodeMeasurementMedium(Integer value) {
    switch (value) {
        case 0: return 'Air'
        case 1: return 'Water'
        case 2: return 'Soil'
        default: return "Unknown (${value})"
    }
}

/**
 * Decode LevelValueEnum
 * Per Matter spec Table 96
 */
String decodeLevelValue(Integer value) {
    switch (value) {
        case 0: return 'Unknown'
        case 1: return 'Low'
        case 2: return 'Medium'
        case 3: return 'High'
        case 4: return 'Critical'
        default: return "Unknown (${value})"
    }
}


// ============ Matter Air Quality Cluster Attributes Map ============
// Air Quality cluster (0x005B) attributes per Matter spec
@Field static final Map<Integer, String> AirQualityClusterAttributes = [
    0x0000  : 'AirQuality',         // AirQualityEnum, R V, M
    0xFFF8  : 'GeneratedCommandList',// list, R V, M
    0xFFF9  : 'AcceptedCommandList', // list, R V, M
    0xFFFB  : 'AttributeList',       // list, R V, M
    0xFFFC  : 'FeatureMap',          // FeatureMap, R V, M
    0xFFFD  : 'ClusterRevision'      // uint16, R V, M
]

// Resource Monitoring clusters (0x0071 HEPA / 0x0072 activated carbon)
@Field static final Map<Integer, String> ResourceMonitoringClusterAttributes = [
    0x0000  : 'Condition',
    0x0001  : 'DegradationDirection',
    0x0002  : 'ChangeIndication',
    0x0003  : 'InPlaceIndicator',
    0x0004  : 'LastChangedTime',
    0x0005  : 'ReplacementProductList',
    0xFFF8  : 'GeneratedCommandList',
    0xFFF9  : 'AcceptedCommandList',
    0xFFFB  : 'AttributeList',
    0xFFFC  : 'FeatureMap',
    0xFFFD  : 'ClusterRevision'
]

@Field static final Map<Integer, String> ResourceMonitoringDegradationDirection = [
    0x00    : 'Up',
    0x01    : 'Down'
]

@Field static final Map<Integer, String> ResourceMonitoringChangeIndication = [
    0x00    : 'OK',
    0x01    : 'Warning',
    0x02    : 'Critical'
]

// ============ Matter Concentration Measurement Cluster Attributes Map ============
// Carbon Dioxide (0x040D) and PM2.5 (0x042A) Concentration Measurement clusters
// Both share the same attribute structure per Matter spec
@Field static final Map<Integer, String> ConcentrationMeasurementClusterAttributes = [
    0x0000  : 'MeasuredValue',       // single (IEEE754), R V, M - Current concentration
    0x0001  : 'MinMeasuredValue',    // single (IEEE754), R V, M - Minimum measurable value
    0x0002  : 'MaxMeasuredValue',    // single (IEEE754), R V, M - Maximum measurable value
    0x0007  : 'Uncertainty',         // single (IEEE754), R V, O - Measurement uncertainty
    0x0008  : 'MeasurementUnit',     // MeasurementUnitEnum, R V, M - Unit of measurement
    0x0009  : 'MeasurementMedium',   // MeasurementMediumEnum, R V, O - Medium being measured
    0x000A  : 'LevelValue',          // LevelValueEnum, R V, M - Concentration level
    0xFFF8  : 'GeneratedCommandList',// list, R V, M
    0xFFF9  : 'AcceptedCommandList', // list, R V, M
    0xFFFB  : 'AttributeList',       // list, R V, M
    0xFFFC  : 'FeatureMap',          // FeatureMap, R V, M
    0xFFFD  : 'ClusterRevision'      // uint16, R V, M
]

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
@Field static final String WIKI   = 'Documentation:'
@Field static final String COMM_LINK =   "https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252/1"
@Field static final String GITHUB_LINK = "https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/blob/main/docs/user/drivers/air-purifier.md"
// credits @jtp10181
String fmtHelpInfo(String str) {
	String info = "${DRIVER} v${parent?.version()}<br> ${COMPONENT} v${matterComponentAirPurifierVersion}"
	String prefLink = "<a href='${GITHUB_LINK}' target='_blank'>${WIKI}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid green; border-radius: 6px; color: green;'"
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

#include kkossev.matterCommonLib
#include kkossev.matterHealthStatusLib
