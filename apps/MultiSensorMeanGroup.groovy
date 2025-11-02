import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.5"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-01"
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
        }
        section("Status") {
            paragraph "Version: ${APP_VERSION}\nBranch: ${APP_BRANCH}\nLast Updated: ${APP_UPDATED}"
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
            subscribe(device, "temperature", "handleDeviceEvent")
            subscribe(device, "humidity", "handleDeviceEvent")
            subscribe(device, "illuminance", "handleDeviceEvent")
            subscribe(device, "ultravioletIndex", "handleDeviceEvent")
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
    if (!child || !monitoredDevices) {
        return
    }

    Map<String, Map<String, Object>> aggregates = [
        temperature: [values: [], unit: null],
        humidity: [values: [], unit: null],
        illuminance: [values: [], unit: null],
        ultravioletIndex: [values: [], unit: null]
    ]

    monitoredDevices.each { device ->
        aggregates.each { attr, data ->
            def currentState = device.currentState(attr)
            if (currentState?.value != null) {
                BigDecimal numericValue = safeToBigDecimal(currentState.value)
                if (numericValue != null) {
                    data.values << numericValue
                    data.unit = currentState.unit ?: data.unit
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
