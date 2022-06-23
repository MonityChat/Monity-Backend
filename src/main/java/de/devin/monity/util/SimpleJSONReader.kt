package de.devin.monity.util

import org.json.JSONObject


/**
 * Simple JSON Reader class which has helpful functions
 * @param input input json string
 */
class SimpleJSONReader(input: String) {

    /**
     * If the given string is a valid JSON
     */
    var valid = false
        private set

    /**
     * The JSONObject when the string is valid
     * If the string is not valid the JSONObject will be empty
     */
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