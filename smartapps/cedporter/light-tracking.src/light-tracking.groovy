/**
 *  Light Tracking
 *
 *  Copyright 2016 C. Edward Porter
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

definition(
    name: "Light Tracking",
    namespace: "cedporter",
    author: "C. Edward Porter",
    description: "light tracking",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

/* This is how the user chooses what smart appliances to track */
preferences {
    section("Track these lights..."){
    	input "switches", "capability.switch", required: true, multiple: true
    }
}

/* This is the routing information for the exposed REST API.
Haven't gotten far with this yet. Just tested it enough to call /switches
and get a JSON object listing the subscribed switches*/
mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
}

/* function handling the request for switches list */
def listSwitches(){
	String timeStamp = new Date();
	def resp = []
    //resp << [time: timeStamp]
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch"), device: state[it.getId()]]
    }
    return resp
}

/* Once app is installed, it calls initialize */
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

/* This creates the "state" which is basically a multidimensional map
that will contain all of the switches tracked. for now just tracking last on/last off
we'll want to hash this out more and devise a way to track time on for a longer period of time*/
def initialize() {
    Date initialOn = new Date()
    Date initialOff = new Date()
    /* Should execute daily at 0:01 */
    schedule("0 0 12 1/1 * ? *", clearDailyUsage)
    /* Should execute first day of the month at 0:01 */
    schedule("0 0 12 1 1/1 ? *", clearMonthlyUsage)
    /* Should execute first day of the year at 0:01 */
    schedule("0 0 12 1 1 ? *", clearYearlyUsage)
	subscribe(switches, "switch.on", switchDetectedHandler)
    subscribe(switches, "switch.off", switchDetectedHandler)
    switches.each {
    	log.debug "name: " + it.displayName
        log.debug "full name: " + it.id
        state[it.id] = [lastOn: now(), 
        	lastOff: now(), 
            dimmerSetting: 100, 
            lastOnTime: initialOn,
            lastOffTime: initialOff]
    }
    
}

/* This handles actual switch event */
def switchDetectedHandler(evt) {
	String timeStamp = new Date();
	log.debug "Switched: ${evt.descriptionText}"
    log.debug evt.deviceId
    if (evt.value == "on") {
    	state[evt.deviceId]['lastOn'] = now()
        state[evt.deviceId]['lastOnTime'] = timeStamp
        log.debug "switch turned on!"
        /*def params = [
            uri: "https://energywebapp.herokuapp.com/switchevent",
            body: [hubId: evt?.hub.id, deviceId: evt.displayName, name: evt.deviceId, timeOn: state[evt.deviceId]['lastOn']]
		]
        try {
            httpPostJson(params) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.    contentType}"
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }*/
    } else if (evt.value == "off") {
        log.debug "switch turned off!"
        state[evt.deviceId]['lastOff'] = now()
        state[evt.deviceId]['lastOffTime'] = timeStamp
        log.debug evt.date
        def duration = (state[evt.deviceId]['lastOff'] - state[evt.deviceId]['lastOn']) / 1000
        def switchState = evt.getDevice().currentValue("level")
        log.debug "Current level: ${switchState}"
        log.debug "Time left on: ${duration}"
        def params = [
            uri: "https://energywebapp.herokuapp.com/switchevent",
            body: [hubId: evt?.hub.id, 
            deviceName: evt.displayName, 
            deviceId: evt.deviceId,
            timeOn: state[evt.deviceId]['lastOnTime'], 
            timeOff: state[evt.deviceId]['lastOffTime'], 
            dimmerSetting: switchState, 
            duration: duration]
		]
        try {
            httpPostJson(params) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.    contentType}"
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }
    }
    
}