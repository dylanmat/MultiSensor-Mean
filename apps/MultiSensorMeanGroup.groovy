import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.6"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-02"
@Field static final String APP_NAME_BASE = "MultiSensor Mean"
@Field static final String GROUP_APP_NAME = "MultiSensorMeanGroup"
@Field static final String GROUP_APP_DISPLAY_NAME = "MultiSensor Mean Group"
@Field static final String APP_NAMESPACE = "multisensor.mean.group"
@Field static final String PARENT_APP_NAMESPACE = "multisensor.mean.app"
@Field static final String PARENT_APP_NAME = "MultiSensorMeanApp"
@Field static final String ICON_URL = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png"
@Field static final String ICON_URL_2X = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
@Field static final String ICON_URL_3X = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%403x.png"

/**
 *  MultiSensor Mean Group
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

definition(
    name: GROUP_APP_NAME,
    namespace: APP_NAMESPACE,
    author: "OpenAI Assistant",
    description: "Group configuration for MultiSensor Mean child devices.",
    parent: PARENT_APP_NAMESPACE + ":" + PARENT_APP_NAME,
    iconUrl: ICON_URL,
    iconX2Url: ICON_URL_2X,
    iconX3Url: ICON_URL_3X
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: GROUP_APP_DISPLAY_NAME, install: true, uninstall: true) {
        section("Group Configuration") {
            label title: "Child device name", required: true, defaultValue: state?.childLabel ?: "${APP_NAME_BASE} Average"
            input "monitoredDevices", "capability.sensor", title: "Select devices to average", multiple: true, required: true, submitOnChange: true
            input "updateMode", "enum", title: "Update mode", options: [["realtime":"Real-time (event driven)"], ["scheduled":"Scheduled refresh"]], required: true, submitOnChange: true, defaultValue: state?.updateMode ?: "realtime"
            if (updateMode == "scheduled") {
                input "refreshMinutes", "number", title: "Refresh interval (minutes)", range: "1..60", required: true, defaultValue: state?.refreshMinutes ?: 5
            }
            if (monitoredDevices) {
                List<String> availableAttributes = availableAveragingAttributes()
                if (availableAttributes) {
                    Map attributeOptions = availableAttributes.collectEntries { [(it): attributeDisplayName(it)] }
                    input "selectedAttributes", "enum", title: "Attributes to include", options: attributeOptions, multiple: true, required: false, submitOnChange: true, defaultValue: state?.selectedAttributes ?: availableAttributes
                } else {
                    paragraph "No supported averaging attributes were found on the selected devices."
                }
            }
        }
        section("Status") {
            paragraph "Version: ${APP_VERSION}\nBranch: ${APP_BRANCH}\nLast Updated: ${APP_UPDATED}"
            List<String> summaryAttributes = resolvedSelectedAttributes(false)
            if (summaryAttributes) {
                paragraph attributeSummaryDescription(summaryAttributes)
            }
            String deviceStatusSummary = buildDeviceAttributeSummaryText(collectDeviceAttributeDetails(monitoredDevices))
            if (deviceStatusSummary) {
                paragraph "Devices:\n${deviceStatusSummary}"
            }
        }
    }
}

def installed() {
    log.info "Installed ${GROUP_APP_DISPLAY_NAME} v${APP_VERSION}"
    rememberChildLabel()
    initialize()
}

def updated() {
    log.info "Updated ${GROUP_APP_DISPLAY_NAME} v${APP_VERSION}"
    unschedule()
    unsubscribe()
    rememberChildLabel()
    initialize()
}

def uninstalled() {
    removeChildDevice()
}

def initialize() {
    state.childLabel = app.getLabel() ?: state.childLabel
    state.updateMode = updateMode ?: state.updateMode ?: "realtime"
    if (refreshMinutes) {
        state.refreshMinutes = refreshMinutes
    }
    state.selectedAttributes = resolvedSelectedAttributes(true)
    ensureChildDevice()
    configureAutomation()
    updateAverages()
}

private void configureAutomation() {
    unsubscribe()
    unschedule()

    if (!monitoredDevices) {
        return
    }

    if (state.updateMode == "realtime") {
        monitoredDevices.each { device ->
            attributesForDevice(device).each { attribute ->
                subscribe(device, attribute, "handleDeviceEvent")
            }
        }
    } else if (state.updateMode == "scheduled" && (refreshMinutes ?: state.refreshMinutes)) {
        Integer minutes = (refreshMinutes ?: state.refreshMinutes ?: 5) as Integer
        minutes = Math.max(1, Math.min(60, minutes))
        state.refreshMinutes = minutes
        runIn(minutes * 60, "runScheduledUpdate", [overwrite: true])
    }
}

def handleDeviceEvent(evt) {
    updateAverages()
}

def runScheduledUpdate() {
    updateAverages()
    Integer minutes = (state.refreshMinutes ?: 5) as Integer
    minutes = Math.max(1, Math.min(60, minutes))
    runIn(minutes * 60, "runScheduledUpdate", [overwrite: true])
}

def updateAverages() {
    def child = getChildDevice(childDeviceNetworkId())
    if (!child) {
        log.warn "Child device missing for ${app.label}; attempting to recreate."
        child = ensureChildDevice()
    }
    if (!child) {
        return
    }

    if (!monitoredDevices) {
        child.sendEvent(name: "averagingSummary", value: "No devices configured")
        child.sendEvent(name: "deviceAttributeSummary", value: "No devices configured")
        clearUnusedChildAttributes([], child)
        state.lastAveragedAttributes = []
        state.lastDeviceCounts = [:]
        state.lastDeviceAttributeDetails = []
        return
    }

    List<Map<String, Object>> deviceDetails = collectDeviceAttributeDetails(monitoredDevices)
    String deviceSummary = buildDeviceAttributeSummaryText(deviceDetails)
    child.sendEvent(name: "deviceAttributeSummary", value: deviceSummary ?: "No devices configured")

    List<String> attributesToAverage = resolvedSelectedAttributes(true)
    if (!attributesToAverage) {
        child.sendEvent(name: "averagingSummary", value: "No attributes configured")
        clearUnusedChildAttributes([], child)
        state.lastDeviceCounts = [:]
        state.lastAveragedAttributes = []
        state.lastDeviceAttributeDetails = deviceDetails.collect { detail ->
            List<String> attrs = []
            def rawAttrs = detail.attributes
            if (rawAttrs instanceof Collection) {
                rawAttrs.each { attr ->
                    if (attr) {
                        attrs << attr.toString()
                    }
                }
            }
            [id: detail.id, name: detail.name, attributes: attrs]
        }
        return
    }

    Map<String, Map<String, Object>> aggregates = [:]
    Map<String, Integer> deviceCounts = [:].withDefault { 0 }

    monitoredDevices.each { device ->
        attributesToAverage.each { String attribute ->
            if (deviceSupportsAttribute(device, attribute)) {
                def currentState = device.currentState(attribute)
                if (currentState?.value != null) {
                    BigDecimal numericValue = safeToBigDecimal(currentState.value)
                    if (numericValue != null) {
                        Map<String, Object> data = aggregates[attribute]
                        if (!data) {
                            data = [values: [], unit: null]
                            aggregates[attribute] = data
                        }
                        data.values << numericValue
                        data.unit = currentState.unit ?: data.unit
                        deviceCounts[attribute] = deviceCounts[attribute] + 1
                    }
                }
            }
        }
    }

    aggregates.each { attr, data ->
        if (data.values) {
            BigDecimal average = calculateAverage(data.values)
            String unit = data.unit
            child.sendEvent(name: attr, value: formatValue(average, attr), unit: unit)
        }
    }

    attributesToAverage.each { attr ->
        if (!aggregates.containsKey(attr) || !(aggregates[attr]?.values)) {
            child.sendEvent(name: attr, value: null)
        }
    }

    clearUnusedChildAttributes(attributesToAverage, child)

    String summary = buildAveragingSummary(attributesToAverage, deviceCounts)
    child.sendEvent(name: "averagingSummary", value: summary ?: "No attributes averaged")
    state.lastAveragedAttributes = attributesToAverage
    state.lastDeviceCounts = attributesToAverage.collectEntries { attr ->
        [(attr): (deviceCounts[attr] ?: 0)]
    }
    state.lastDeviceAttributeDetails = deviceDetails.collect { detail ->
        List<String> attrs = []
        def rawAttrs = detail.attributes
        if (rawAttrs instanceof Collection) {
            rawAttrs.each { attr ->
                if (attr) {
                    attrs << attr.toString()
                }
            }
        }
        [id: detail.id, name: detail.name, attributes: attrs]
    }
}

@Field static final List<String> AVERAGED_ATTRIBUTES = [
    "temperature",
    "humidity",
    "illuminance",
    "ultravioletIndex"
]

private List<String> availableAveragingAttributes() {
    if (!monitoredDevices) {
        return []
    }

    Set<String> available = [] as Set
    monitoredDevices.each { device ->
        available.addAll(supportedAveragingAttributes(device))
    }
    AVERAGED_ATTRIBUTES.findAll { available.contains(it) }
}

private List<String> attributesForDevice(device) {
    if (!device) {
        return []
    }
    List<String> attributes = []
    resolvedSelectedAttributes(true).each { attr ->
        if (deviceSupportsAttribute(device, attr)) {
            attributes << attr
        }
    }
    attributes
}

private boolean deviceSupportsAttribute(device, String attributeName) {
    if (!device || !attributeName) {
        return false
    }

    try {
        def supported = device?.supportedAttributes?.collect { it?.name }?.findAll { it }
        if (supported && supported.contains(attributeName)) {
            return true
        }
    } catch (Exception ignored) {
    }

    boolean hasAttribute = false
    try {
        hasAttribute = device?.hasAttribute(attributeName)
    } catch (Exception ignored) {
        hasAttribute = false
    }
    return hasAttribute
}

private List<String> supportedAveragingAttributes(device) {
    if (!device) {
        return []
    }
    AVERAGED_ATTRIBUTES.findAll { attribute -> deviceSupportsAttribute(device, attribute) }
}

private List<String> resolvedSelectedAttributes(boolean useStateFallback) {
    List<String> available = availableAveragingAttributes()
    List<String> configured = []

    def rawSelected = settings?.selectedAttributes
    if (rawSelected instanceof Collection) {
        configured.addAll(rawSelected.collect { it.toString() })
    } else if (rawSelected) {
        configured << rawSelected.toString()
    } else if (useStateFallback && state?.selectedAttributes instanceof Collection) {
        configured.addAll(state.selectedAttributes.collect { it.toString() })
    }

    configured = configured.findAll { available.contains(it) }
    if (!configured && available) {
        configured = available
    }

    if (useStateFallback) {
        state.selectedAttributes = configured
    }
    configured
}

private String attributeDisplayName(String attribute) {
    switch (attribute) {
        case "temperature":
            return "Temperature"
        case "humidity":
            return "Humidity"
        case "illuminance":
            return "Illuminance"
        case "ultravioletIndex":
            return "UV Index"
        default:
            return attribute?.capitalize()
    }
}

private String attributeSummaryDescription(List<String> attributes) {
    Map<String, Integer> storedCounts = [:]
    if (state?.lastDeviceCounts instanceof Map) {
        state.lastDeviceCounts.each { key, value ->
            if (key && value != null) {
                storedCounts[key.toString()] = (value as Integer)
            }
        }
    }

    String summary = attributes.collect { attr ->
        String label = attributeDisplayName(attr)
        Integer count = storedCounts.containsKey(attr) ? storedCounts[attr] : (monitoredDevices?.count { deviceSupportsAttribute(it, attr) } ?: 0)
        "${label}: ${count} device${count == 1 ? '' : 's'}"
    }.join("\n")
    return summary ?: "No attributes configured"
}

private List<Map<String, Object>> collectDeviceAttributeDetails(Collection devices) {
    List<Map<String, Object>> details = []
    if (!devices) {
        return details
    }

    Integer index = 0
    devices.each { device ->
        details << [
            id: (device?.id?.toString() ?: "${index}"),
            name: deviceDisplayName(device, index),
            attributes: supportedAveragingAttributes(device)
        ]
        index++
    }
    details
}

private String buildDeviceAttributeSummaryText(List<Map<String, Object>> deviceDetails) {
    if (!deviceDetails) {
        return null
    }

    deviceDetails.collect { Map<String, Object> detail ->
        List<String> attributes = []
        def rawAttributes = detail.attributes
        if (rawAttributes instanceof Collection) {
            rawAttributes.each { attr ->
                if (attr) {
                    attributes << attributeDisplayName(attr.toString())
                }
            }
        }
        String attributeText = attributes ? attributes.join(", ") : "No supported attributes"
        "${detail.name}: ${attributeText}"
    }.join("\n")
}

private String deviceDisplayName(device, Integer index = null) {
    String label = device?.displayName ?: device?.name
    if (!label) {
        String fallbackId = device?.id?.toString()
        if (fallbackId) {
            label = "Device ${fallbackId}"
        } else if (index != null) {
            label = "Device ${index + 1}"
        } else {
            label = "Device"
        }
    }
    label
}

private String buildAveragingSummary(List<String> attributes, Map<String, Integer> deviceCounts) {
    if (!attributes) {
        return null
    }
    attributes.collect { attr ->
        Integer count = deviceCounts[attr] ?: 0
        String label = attributeDisplayName(attr)
        "${label}: ${count} device${count == 1 ? '' : 's'}"
    }.join(", ")
}

private void clearUnusedChildAttributes(List<String> activeAttributes, child) {
    List<String> inactiveAttributes = AVERAGED_ATTRIBUTES.findAll { !(activeAttributes?.contains(it)) }
    inactiveAttributes.each { attribute ->
        child.sendEvent(name: attribute, value: null)
    }
}

private BigDecimal calculateAverage(List<BigDecimal> values) {
    if (!values) {
        return null
    }
    BigDecimal sum = values.inject(BigDecimal.ZERO) { BigDecimal total, BigDecimal value -> total + value }
    BigDecimal average = sum.divide(new BigDecimal(values.size()), 4, BigDecimal.ROUND_HALF_UP)
    return average
}

private Object formatValue(BigDecimal value, String attribute) {
    if (value == null) {
        return null
    }
    switch (attribute) {
        case "temperature":
            return value.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
        case "humidity":
            return value.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
        case "illuminance":
            return value.setScale(0, BigDecimal.ROUND_HALF_UP).toInteger()
        case "ultravioletIndex":
            return value.setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
        default:
            return value.toDouble()
    }
}

private BigDecimal safeToBigDecimal(Object value) {
    try {
        return new BigDecimal(value.toString())
    } catch (Exception ex) {
        log.debug "Unable to convert value '${value}' to BigDecimal: ${ex.message}"
        return null
    }
}

private String childDeviceNetworkId() {
    return "MSM-${app.id}"
}

private getChildDeviceLabel() {
    return app.getLabel() ?: state.childLabel ?: "${APP_NAME_BASE} Average ${app.id}"
}

private ensureChildDevice() {
    String dni = childDeviceNetworkId()
    def child = getChildDevice(dni)
    String label = getChildDeviceLabel()
    if (!child) {
        try {
            child = addChildDevice("multisensor.mean", "MultiSensor Mean Child Device", dni, [name: label, label: label, isComponent: true])
            log.info "Created child device '${label}' (${dni})"
        } catch (Exception ex) {
            log.error "Unable to create child device: ${ex.message}", ex
        }
    } else if (child.label != label) {
        child.label = label
    }
    state.childLabel = label
    return child
}

private void rememberChildLabel() {
    String currentLabel = app?.getLabel()
    if (currentLabel) {
        state.childLabel = currentLabel
    }
}

private void removeChildDevice() {
    String dni = childDeviceNetworkId()
    def child = getChildDevice(dni)
    if (child) {
        deleteChildDevice(dni)
        log.info "Deleted child device ${dni}"
    }
}
