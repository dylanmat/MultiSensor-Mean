import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.5"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-01"
@Field static final String APP_NAME_BASE = "MultiSenor Mean"
@Field static final String APP_NAME = APP_BRANCH == "main" ? APP_NAME_BASE : "${APP_NAME_BASE} Test"

/**
 *  MultiSensor Mean Child Device
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

metadata {
    definition(name: "MultiSensor Mean Child Device", namespace: "multisensor.mean", author: "OpenAI Assistant") {
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "IlluminanceMeasurement"
        capability "UltravioletIndex"

        attribute "temperature", "number"
        attribute "humidity", "number"
        attribute "illuminance", "number"
        attribute "ultravioletIndex", "number"

        command "clearAverages"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}

void installed() {
    logInfo "Installed ${APP_NAME} child device"
}

void updated() {
    logInfo "Updated ${APP_NAME} child device"
    if (logEnable) {
        runIn(1800, "logsOff")
    }
}

void clearAverages() {
    logInfo "Clearing averaged attribute values"
    ["temperature", "humidity", "illuminance", "ultravioletIndex"].each { attribute ->
        sendEvent(name: attribute, value: null)
    }
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
