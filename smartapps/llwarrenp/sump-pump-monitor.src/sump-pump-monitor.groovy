/*
 *  Sump Pump Monitor
 *
 *  Copyright 2014 Tim Slagle
 *  Copyright 2018 Warren Poschman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
def appVersion() {
    return "2.4"
}

/*
* Change Log:
* 2018-7-12  - (2.4) Added alert for non-responsive sensor for malfunction, power outage / breaker off, or similar
* 2018-6-26  - (2.3) Cleaned up logic and preferences to avoid unhandled errors, improved documentation
* 2018-6-23  - (2.2) Changed displayed time to use hub timezone instead of UTC
* 2018-6-8   - (2.1) Tweaked for GitHub and uploaded
* 2018-3-20  - (2.0) Reworked and added features
* 2014-10-15 - (1.0) Initial release by @tslagle13
*/

definition(
    name: "Sump Pump Monitor",
    namespace: "LLWarrenP",
    author: "Warren Poschman",
    description: "Checks to see whether your sump pump is running more than usual.  Connect a multi-sensor to the sump pump and it will alert you once every X hours if the pump is running twice or more in Y minutes.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-PipesLeaksAndFloods.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-PipesLeaksAndFloods@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-PipesLeaksAndFloods@2x.png")

def showLastDate(timestamp) {
	def lastDate = "Never"
	if (timestamp == null) {
		timestamp = 0
	}
    if (timestamp != 0) lastDate = new Date(timestamp).format("EEE, MMMMM d yyyy HH:mm:ss z", location.timeZone)
    return lastDate
}

preferences {
	section("Sump Pump Monitor v${appVersion()}\n\nLast Ran:\n   ${showLastDate(atomicState.lastActionTimeStamp)}\nLast Alert:\n   ${showLastDate(atomicState.lastAlertTimeStamp)}")
    section("Monitoring Settings") {
		input "multi", "capability.accelerationSensor", title: "Which acceleration sensor?", multiple: false, required: true
        paragraph "The sensor will detect the inrush current as an acceleration"
		input "frequency", "decimal", title: "Over what time interval (minutes)?", description: "Minutes", range: "1..*", defaultValue: 30, required: true
        input "heartbeat", "decimal", title: "Alert me if the sensor has not provided status in this many hours:", defaultValue: 0, range: "0..*", required: false
        paragraph "Set this to zero (0) to disable.  This alert is to detect if the sensor is offline or otherwise not reporting for the set number of hours such as in the case where a power failure has occurred or a breaker has tripped.  Ensure that the device's normal operation is to report at least this frequent."
	}
    section("Alert Settings"){
		input "alertfrequency", "decimal", title: "Alert how often (hours)?", description: "Hours", range: "0..*", defaultValue: 24, required: true
        paragraph "Since the pump may continue to fire at a steady rate, this suppresses excessive alerts after the initial alert"
		input "messageText", "text", title: "Custom Alert Text (optional)", required: false
		input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
		input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes","No"]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(multi, "acceleration.active", checkFrequency)
	// Look for any report from the device to ensure that it is powered up and reporting
    unschedule(heartbeatAlert)
	if (heartbeat?.toInteger() > 0) {
    	multi.each { sen ->
    		sen.capabilities.each { cap ->
        		cap.attributes.each { attr ->
            		subscribe(sen, attr.name, deviceHeartbeat)
	       		}
    		}
		}
    	runIn(heartbeat?.toInteger() * 3600, heartbeatAlert)
    }
}

def checkFrequency(evt) {
	log.debug("sump pump firing")
	def lastTime = state[frequencyPumpFired(evt)]

	// Pump has fired so reset hearbeat, but check frequency afterwards
	if (heartbeat?.toInteger() > 0) deviceHeartbeat(evt)
    
	// Pump has never fired before, it's the first time so just record the event
	if (lastTime == null) state[frequencyPumpFired(evt)] = now()
    // Pump has fired before but the last time it did so was outside the window of interest so just record the event
	else if ((now() - lastTime) >= (frequency * 60000)) state[frequencyPumpFired(evt)] = now()
    // Pump has fired before and its within our specified window of interest so we need to possibly send an alert
	else if ((now() - lastTime) <= (frequency * 60000)) {
		def timePassed = (now() - lastTime) / 60000
		def timePassedRound = Math.round(timePassed.toDouble()) + (unit ?: "")
        if (timePassedRound == null) then timePassedRound = 1
		if (alertfrequency == null) alertfrequency = 0

		// The pump has fired so record it
		state[frequencyPumpFired(evt)] = now()
		log.debug("sump pump ${multi} fired twice in the last ${timePassedRound} minutes")

		// Check to see the last time we sent out an alert either never or some time in the past
		def lastAlert = state[frequencyAlert(evt)]
		if (lastAlert == null) {
        	lastAlert = 0
        	state[frequencyAlert(evt)] = 0
        }

		// If the last time we sent an alert was greater than our window between alerts, it's time to send another alert
		if (((now() - lastAlert)) >= (alertfrequency * 3600000)) {
			log.debug "sump pump sending alerts"
			state[frequencyAlert(evt)] = now()

			def msg = messageText ?: "Warning: ${multi} has run twice in the last ${timePassedRound} minutes."

			if (!phone || pushAndPhone != "No") {
				log.debug "sending push"
				sendPush(msg)
			}

			if (phone) {
				log.debug "sending SMS"
				sendSms(phone, msg)
			}
            
		}
        // Otherwise, if it is inside the window we want to suppress the alert but we should log it to the debug log
		else log.debug "sump pump suppressing alert due to alert frequency"

	}

}

def deviceHeartbeat(evt) {
	// We got a message from the device so we can assume it is online / active so reset the alert
	unschedule(heartbeatAlert)
    runIn(heartbeat?.toInteger() * 3600, heartbeatAlert)
    log.debug "sump pump ${multi} heartbeat"
}

def heartbeatAlert() {
	log.debug "sump pump sending heartbeat alert"

	def msg = messageText ?: "Warning: ${multi} has not reported status in the last ${heartbeat} hours.  Check pump circuit for power and operation."

	if (!phone || pushAndPhone != "No") {
		log.debug "sending push"
		sendPush(msg)
	}

	if (phone) {
		log.debug "sending SMS"
		sendSms(phone, msg)
	}
}

private frequencyPumpFired(evt) {
	"lastActionTimeStamp"
}

private frequencyAlert(evt) {
	"lastAlertTimeStamp"
}

// END OF FILE