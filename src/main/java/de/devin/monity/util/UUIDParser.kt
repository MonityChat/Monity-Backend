package de.devin.monity.util

import com.google.gson.Gson
import org.json.JSONObject
import java.util.*

fun validUUID(input: String): Boolean {
    return try {
        UUID.fromString(input)
        true
    } catch (e: Exception) {
        false
    }
}

fun toJSONString(any: Any): String {
    return Gson().toJson(any)
}

fun toJSON(any: Any): JSONObject {
    return JSONObject(Gson().toJson(any))
}