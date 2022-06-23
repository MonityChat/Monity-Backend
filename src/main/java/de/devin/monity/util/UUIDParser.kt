package de.devin.monity.util

import com.google.gson.Gson
import org.json.JSONObject
import java.util.*


/**
 * Checks if the given String is a Valid UUID String
 * @see UUID
 * @param input input string
 * @return whether the given input is a valid UUID string
 */
fun validUUID(input: String): Boolean {
    return try {
        UUID.fromString(input)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Parses the given object into a JSON and converts the JSON into a string
 * @param any any object
 * @return the json string
 */
fun toJSONString(any: Any): String {
    return toJSON(any).toString()
}


/**
 * Parses the given object into a JSONObject
 * @see JSONObject
 * @param any any object
 * @return JSONObject from the given object
 */
fun toJSON(any: Any): JSONObject {
    return JSONObject(Gson().toJson(any))
}