# ST-Smart-Sump-Pump-Monitor
Checks to see whether your sump pump is running more than usual.  If you connect a multi-sensor to the sump pump and it will alert once every X hours if the pump is running twice or more in Y minutes.  Also provides a history of when the pump last ran (in the settings tab).

# Required Devices
1. A multi-switch that can monitor energy usage and report a change as acceleration.  The Zooz Power Switch / Zooz Smart Plug (ZEN15) is an example of such a device.

**Note**
The multi-switch should be set up to be always on (this can be done in ST Smart Lighting) to avoid the chance that it is shut off and does not fire at all.  Additionally, the multi-switch will have to be configured to trigger based on the power usage of the pump when running and not trigger when it is in the idle state.  For example, set the multi-switch to report idle at < 5 W and active at anything > 25W where the power consumed while pumping is expected to be in the 600-800 W range.

# Installation

The SmartApp is installed as a custom app in the SmartThings mobile app via the "My Apps" section.  Therefore, the app must
first be installed into your SmartThings SmartApps repository.  For multiple sump pump installations, simply install the app twice.

## Installation via GitHub Integration
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My SmartApps" section in the navigation bar.
3. Click on "Settings"
4. Click "Add New Repository"
5. Enter "LLWarrenP" as the namespace
6. Enter "ST-Smart-Sump-Pump-Monitor" as the repository
7. Hit "Save"
8. Select "Update from Repo" and select "ST-Smart-Sump-Pump-Monitor"
9. Select "smartapps/sump-pump-monitor.src/sump-pump-monitor.groovy"
10. Check "Publish" and hit "Execute Update"

## Manual Installation
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My SmartApps" section in the navigation bar.
3. On your SmartApps page, click on the "+ Create New SmartApp" button on the right.
4. On the "New SmartApp" page, Select the Tab "From Code", Copy the "sump-pump-monitor.groovy" source code from GitHub and paste it into the IDE editor window.
5. Click the blue "Create" button at the bottom of the page. An IDE editor window containing the SmartApp code should now open.
6. Click the blue "Save" button above the editor window.
7. Click the "Publish" button next to it and select "For Me". You have now self-published your Smart App.

# App Settings
**Window for pump running two or more times:** The window that you want to be alerted on if the sump pump fires more than once
during the window.  Alerts are sent out at this same interval to avoid excessive alerting.  For instance, if you select "2",
an alert will be sent out if the sump pump fires twice over any two hour period and then every two hours if it continues to fire.


# App Logging
There is no user logging other than the alert.  However, the app will log any sent or suppressed alerts to the debug log.
