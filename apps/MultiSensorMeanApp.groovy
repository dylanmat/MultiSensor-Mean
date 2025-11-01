import groovy.transform.Field

@Field static final String APP_VERSION = "0.0.5"
@Field static final String APP_BRANCH = "work"
@Field static final String APP_UPDATED = "2025-11-01"
@Field static final String APP_NAME_BASE = "MultiSensor Mean"

String getAppName() {
    return APP_BRANCH == "main" ? APP_NAME_BASE : "${APP_NAME_BASE} Test"
}

/**
 *  MultiSensor Mean (branch-aware)
 *  Version: ${APP_VERSION}
 *  Branch: ${APP_BRANCH}
 *  Last Updated: ${APP_UPDATED}
 */

definition(
    name: appName,
    namespace: "multisensor.mean",
    author: "OpenAI Assistant",
    description: "Create child devices that track the average of multiple environmental sensors.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: appName, install: true, uninstall: true) {
        section("Application Details") {
            paragraph "Version: ${APP_VERSION}\nBranch: ${APP_BRANCH}\nLast Updated: ${APP_UPDATED}"
        }
        section("Child Devices") {
            paragraph "Create one or more averaged sensor devices. Each child device can average temperature, humidity, illuminance, and UV index across any set of compatible sensors."
            app(name: "childDevices", appName: "${appName} Group", namespace: "multisensor.mean", title: "Add a new MultiSensor Mean child", multiple: true)
        }
    }
}

def installed() {
    log.info "Installed ${appName} v${APP_VERSION}"
    initialize()
}

def updated() {
    log.info "Updated ${appName} v${APP_VERSION}"
    initialize()
}

def initialize() {
    log.debug "Initialization complete for ${appName}"
}
