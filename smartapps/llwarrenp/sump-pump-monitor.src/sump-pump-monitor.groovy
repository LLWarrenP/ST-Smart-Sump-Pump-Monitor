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
    return "2.1"
}

/*
* Change Log:
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
	if (timestamp == null) {
		timestamp = 0
	}
	def lastDate = new Date(timestamp).format("EEE, MMMMM d yyyy HH:mm:ss z")
	return lastDate
}

preferences {
	section("Sump Pump Monitor v${appVersion()}")
	section("Last Ran:\n   ${showLastDate(atomicState.lastActionTimeStamp)}")
	section("Last Alert:\n   ${showLastDate(atomicState.lastAlertTimeStamp)}")
    section("Sensor") {
		input "multi", "capability.accelerationSensor", title: "Which?", multiple: false, required: true
	}
    section("Monitoring Settings") {
		input "frequency", "decimal", title: "Window for pump running two or more times", description: "Minutes", required: true
	}
    section("Alert Settings"){
		input "alertfrequency", "decimal", title: "Alert how often?", description: "Hours", required: false
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
}


def checkFrequency(evt) {
	log.debug("running check sump")
	def lastTime = state[frequencyKeyAccelration(evt)]

	if (lastTime == null) {
		state[frequencyKeyAccelration(evt)] = now()
	}

	else if (now() - lastTime >= frequency * 60000) {
		state[frequencyKeyAccelration(evt)] = now()
	}

	else if (now() - lastTime <= frequency * 60000) {
		log.debug("Last time valid")
		def timePassed = now() - lastTime
		def timePassedFix = timePassed / 60000
		def timePassedRound = Math.round(timePassedFix.toDouble()) + (unit ?: "")
		state[frequencyKeyAccelration(evt)] = now()
		def msg = messageText ?: "Warning: ${multi} has run twice in the last ${timePassedRound} minutes."

		def lastAlert = state[frequencyAlert(evt)]

		if (lastAlert == null) {
			state[frequencyAlert(evt)] = 0
		}

		else if (now() - lastAlert >= alertfrequency * 3600000) {
			log.debug "sending alerts"
			state[frequencyAlert(evt)] = now()

			if (!phone || pushAndPhone != "No") {
				log.debug "sending push"
				sendPush(msg)
			}

			if (phone) {
				log.debug "sending SMS"
				sendSms(phone, msg)
			}
            
		}

		else {
        	log.debug "suppressing alert due to alert frequency"
        }

	}

}


private frequencyKeyAccelration(evt) {
	"lastActionTimeStamp"
}


private frequencyAlert(evt) {
	"lastAlertTimeStamp"
}