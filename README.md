# MultiSensor-Mean

## Overview
MultiSensor Mean Test is a Hubitat parent/child application that creates virtual child devices representing the mean values of illuminance, temperature, humidity, and ultraviolet index across one or more physical sensors. Each child device continuously reflects the average of its selected devices based on the configured update mode.

The parent app hosts any number of averaging groups. Every group is defined through the bundled child app and is backed by the included virtual device driver so the aggregated values can participate in Rule Machine, dashboards, or any other Hubitat automations.

## Files
- `apps/MultiSensorMeanApp.groovy` – parent application that hosts the shared settings and manages child group apps.
- `apps/MultiSensorMeanGroup.groovy` – child app used to configure each averaged sensor group and create its corresponding child device.
- `drivers/MultiSensorMeanChild.groovy` – driver backing each averaged child device.

## Installation
1. Import the parent app, child app, and driver code into your Hubitat hub (Apps Code for the parent/child apps and Drivers Code for the driver).
2. Install the parent app (`MultiSensor Mean Test`) from the Hubitat Apps list.
3. From within the parent app UI, tap **Add a new MultiSensor Mean child** to create a **MultiSensor Mean Test Group** child app for each set of sensors you want to average.
4. When prompted, grant the required device permissions so the app can read the selected sensor attributes.

## Configuration
When you open a **MultiSensor Mean Test Group** child app you can:
- Provide a custom label for the averaged child device. This becomes both the child app name and the published child device label.
- Select any combination of devices that expose temperature, humidity, illuminance, or ultraviolet index. Attributes that are missing from a given device are ignored automatically.
- Choose the averaging cadence:
  - **Real-time (event driven):** averages update immediately whenever one of the selected devices reports a new value for a tracked attribute.
  - **Scheduled refresh:** averages are recalculated on a timer that you define between 1 and 60 minutes to reduce hub load.

The created child device automatically updates its own temperature, humidity, illuminance, and ultraviolet index attributes with the averaged values. You can use these values anywhere a regular Hubitat device attribute is accepted. The driver also includes a **Clear Averages** command that resets the stored attribute values if you want to start fresh.

## Version Metadata
- **App Name:** MultiSenor Mean Test
- **Version:** 0.0.5
- **Branch:** work
- **Last Updated (PST/PDT):** 2025-11-01

## Changelog
- **0.0.5** – Ensure real-time subscriptions and averaging only operate on attributes each device actually supports, and expose the averaged attribute list with `@Field` so Hubitat accepts the declaration.
- **0.0.4** – Added default labels for the parent and child apps and preserved custom names so they survive installs and updates.
- **0.0.3** – Defined SmartApp icons and the explicit parent reference used when creating child group apps.
- **0.0.2** – Expanded installation/configuration guidance and version metadata; incremented app version constants.
- **0.0.1** – Initial release providing averaged multi-sensor child devices with configurable update cadence.
