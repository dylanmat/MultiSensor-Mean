import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.5"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-01"
@Field static final String APP_NAME_BASE = "MultiSensor Mean"
@Field static final String APP_NAME = "MultiSensorMeanApp"
@Field static final String APP_DISPLAY_NAME = "MultiSensor Mean App"
@Field static final String CHILD_APP_NAME = "MultiSensorMeanGroup"
@Field static final String APP_NAMESPACE = "multisensor.mean.app"
@Field static final String CHILD_APP_NAMESPACE = "multisensor.mean.group"
@Field static final String ICON_URL = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png"
@Field static final String ICON_URL_2X = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
@Field static final String ICON_URL_3X = "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%403x.png"

/**
 *  MultiSensor Mean App
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

definition(
    name: APP_NAME,
    namespace: APP_NAMESPACE,
    author: "OpenAI Assistant",
    description: "Create child devices that track the average of multiple environmental sensors.",
    category: "Convenience",
    iconUrl: ICON_URL,
    iconX2Url: ICON_URL_2X,
    iconX3Url: ICON_URL_3X
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: APP_DISPLAY_NAME, install: true, uninstall: true) {
        section("Application Details") {
            paragraph "Version: ${APP_VERSION}\nBranch: ${APP_BRANCH}\nLast Updated: ${APP_UPDATED}"
        }
        section("Child Devices") {
            paragraph "Create one or more averaged sensor devices. Each child device can average temperature, humidity, illuminance, and UV index across any set of compatible sensors."
            app(name: "childDevices", appName: CHILD_APP_NAME, namespace: CHILD_APP_NAMESPACE, title: "Add a new " + APP_NAME_BASE + " child", multiple: true)
        }
    }
}

def installed() {
    log.info "Installed ${APP_DISPLAY_NAME} v${APP_VERSION}"
    initialize()
}

def updated() {
    log.info "Updated ${APP_DISPLAY_NAME} v${APP_VERSION}"
    initialize()
}

def initialize() {
    log.debug "Initialization complete for ${APP_DISPLAY_NAME}"
}
