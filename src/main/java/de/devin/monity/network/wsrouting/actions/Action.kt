package de.devin.monity.network.wsrouting.actions

import org.json.JSONObject
import java.util.*

data class Parameter(val key: String)

interface Action {

    val name: String
    val parameters: List<Parameter>

    fun execute(sender: UUID, parameters: List<Parameter>): JSONObject

}