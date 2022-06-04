package de.devin.monity.util

import org.json.JSONObject

class SimpleJSONReader(input: String) {

    var valid = false
        private set

    var json: JSONObject
        private set

    init {
        try {
            json = JSONObject(input)
            valid = true
        } catch (e: Exception) {
            valid = false
            json = JSONObject()
        }
    }

}