import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.5"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-01"
@Field static final String APP_NAME_BASE = "MultiSensor Mean"
@Field static final String DEVICE_NAME = "${APP_NAME_BASE} Child Device"

/**
 *  ${DEVICE_NAME}
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

metadata {
    definition(name: DEVICE_NAME, namespace: "multisensor.mean", author: "OpenAI Assistant") {
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
