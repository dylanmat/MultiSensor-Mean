# MultiSensor Average

## Overview
MultiSensor Average is a Hubitat parent/child application that creates virtual child devices representing the mean values of illuminance, temperature, humidity, and ultraviolet index across one or more physical sensors. Each child device continuously reflects the average of its selected devices based on the configured update mode, and now surfaces a summary describing which attributes are included, how many physical devices contributed to each average, and which devices (plus their supported attributes) participate in the group.

The parent app hosts any number of averaging groups. Every group is defined through the bundled child app and is backed by the included virtual device driver so the aggregated values can participate in Rule Machine, dashboards, or any other Hubitat automations.

## Files
- `apps/MultiSensorMeanApp.groovy` – parent application that hosts the shared settings and manages child group apps.
- `apps/MultiSensorMeanGroup.groovy` – child app used to configure each averaged sensor group and create its corresponding child device.
- `drivers/MultiSensorMeanChild.groovy` – driver backing each averaged child device.

## Installation
1. Import the parent app, child app, and driver code into your Hubitat hub (Apps Code for the parent/child apps and Drivers Code for the driver).
2. Install the parent app (`MultiSensor Average`) from the Hubitat Apps list.
3. From within the parent app UI, tap **Add a new MultiSensor Average child** to create a **MultiSensor Average Child** app for each set of sensors you want to average. Each child app automatically provisions a **MultiSensor Average Child** device using the bundled driver.
4. When prompted, grant the required device permissions so the app can read the selected sensor attributes.

## Configuration
When you open a **MultiSensor Average Child** app you can:
- Provide a custom label for the averaged child device. This becomes both the child app name and the published child device label.
- Select any combination of devices that expose temperature, humidity, illuminance, or ultraviolet index. Attributes that are missing from a given device are ignored automatically.
- Decide which of the supported attributes should be included in the averaged child device. The group app limits subscriptions and calculations to your selections, and the child device only keeps the chosen attributes populated.
- Choose the averaging cadence:
  - **Real-time (event driven):** averages update immediately whenever one of the selected devices reports a new value for a tracked attribute.
  - **Scheduled refresh:** averages are recalculated on a timer that you define between 1 and 60 minutes to reduce hub load.

The created child device automatically updates its own temperature, humidity, illuminance, and ultraviolet index attributes with the averaged values. You can use these values anywhere a regular Hubitat device attribute is accepted. The driver also includes a **Clear Averages** command that resets the stored attribute values if you want to start fresh, an `averagingSummary` attribute that lists the enabled attributes along with the device counts that fed into each average so you can verify coverage at a glance, and a `deviceAttributeSummary` attribute that spells out each participating device together with the attributes it contributes.

## Version Metadata
- **App Name:** MultiSensor Average
- **Version:** 0.1.1
- **Branch:** work
- **Last Updated (PST/PDT):** 2025-11-21

## Changelog
- **0.1.1** – Added a per-device option to log averaged attribute changes to the info log and routed all averaged events through the driver so it can honor logging preferences.
- **0.1.0** – Renamed the apps and driver to MultiSensor Average, updated namespaces to `dylanm.ma`/`dylanm.ma.child`, and refreshed defaults to create the new child app and device names.
- **0.0.6** – Allow each group to pick the averaged attributes, surface a per-attribute device count summary both in the app UI and on the child device driver, and publish a device/attribute roster on the child device so you can confirm participation details instantly.
- **0.0.5** – Ensure real-time subscriptions and averaging only operate on attributes each device actually supports, and expose the averaged attribute list with `@Field` so Hubitat accepts the declaration.
- **0.0.4** – Added default labels for the parent and child apps and preserved custom names so they survive installs and updates.
- **0.0.3** – Defined SmartApp icons and the explicit parent reference used when creating child group apps.
- **0.0.2** – Expanded installation/configuration guidance and version metadata; incremented app version constants.
- **0.0.1** – Initial release providing averaged multi-sensor child devices with configurable update cadence.
