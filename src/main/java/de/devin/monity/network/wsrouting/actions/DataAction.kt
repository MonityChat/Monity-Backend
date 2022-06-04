package de.devin.monity.network.wsrouting.actions

import org.json.JSONObject
import java.util.*

class UserDataAction: Action {

    override val name: String = "data:get:user"
    override val parameters: List<Parameter> = listOf()

    override fun execute(sender: UUID, parameters: List<Parameter>): JSONObject {
        return JSONObject()
    }
}