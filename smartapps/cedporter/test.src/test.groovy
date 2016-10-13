/**
 *  test
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
 
import groovy.time.*

definition(
    name: "test",
    namespace: "cedporter",
    author: "C. Edward Porter",
    description: "test",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
    section("Track these lights..."){
    	input "switches", "capability.switch", required: true, multiple: true
    }
}

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

def listSwitches(){
	def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
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
    Date initialOn = new Date()
    Date initialOff = new Date()
	subscribe(switches, "switch.on", switchDetectedHandler)
    subscribe(switches, "switch.off", switchDetectedHandler)
    switches.each {
    	log.debug "name: " + it.displayName
        log.debug "full name: " + it.id
        state[it.id] = [lastOn: now(), lastOff: now()]
    }
    
}

def switchDetectedHandler(evt){
	log.debug "Switched: ${evt.descriptionText}"
    log.debug evt.deviceId
    if (evt.value == "on") {
    	state[evt.deviceId]['lastOn'] = now()
        log.debug "switch turned on!"
    } else if (evt.value == "off") {
        log.debug "switch turned off!"
        state[evt.deviceId]['lastOff'] = now()
        log.debug evt.date
        def duration = (state[evt.deviceId]['lastOff'] - state[evt.deviceId]['lastOn']) / 1000
        log.debug "Time left on: ${duration}"
    }
    
}