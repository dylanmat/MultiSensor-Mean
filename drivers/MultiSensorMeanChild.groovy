import groovy.transform.Field

@Field static final String APP_VERSION = "0.1.1"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-21"
@Field static final String APP_NAME_BASE = "MultiSensor Average"
@Field static final String DEVICE_NAME = "MultiSensor Average Child"

/**
 *  MultiSensor Average Child
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

metadata {
    definition(name: DEVICE_NAME, namespace: "dylanm.ma", author: "OpenAI Assistant") {
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "IlluminanceMeasurement"
        capability "UltravioletIndex"

        attribute "temperature", "number"
        attribute "humidity", "number"
        attribute "illuminance", "number"
        attribute "ultravioletIndex", "number"
        attribute "averagingSummary", "string"
        attribute "deviceAttributeSummary", "string"

        command "clearAverages"
        command "publishAveragedEvent"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "attributeLogEnable", type: "bool", title: "Log attribute changes to info", defaultValue: false
    }
}

void installed() {
    logInfo "Installed ${DEVICE_NAME}"
}

void updated() {
    logInfo "Updated ${DEVICE_NAME}"
    if (logEnable) {
        runIn(1800, "logsOff")
    }
}

void clearAverages() {
    logInfo "Clearing averaged attribute values"
    ["temperature", "humidity", "illuminance", "ultravioletIndex"].each { attribute ->
        publishAveragedEvent(attribute, null)
    }
    publishAveragedEvent("averagingSummary", "No attributes configured")
    publishAveragedEvent("deviceAttributeSummary", "No devices configured")
}

void logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
    log.info "Debug logging disabled"
}

void logDebug(String message) {
    if (logEnable) {
        log.debug message
    }
}

void logInfo(String message) {
    log.info message
}

void publishAveragedEvent(String attributeName, Object value, String unit = null) {
    if (!attributeName) {
        return
    }

    String previous = device.currentValue(attributeName)?.toString()
    String incoming = value?.toString()
    Map eventDetails = [name: attributeName, value: value]
    if (unit) {
        eventDetails.unit = unit
    }
    sendEvent(eventDetails)
    if (attributeLogEnable && previous != incoming) {
        String unitSuffix = unit ? " ${unit}" : ""
        logInfo "${attributeName} is now ${value}${unitSuffix}"
    }
}

