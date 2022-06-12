package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.util.toJSON
import filemanagment.util.logInfo
import org.json.JSONObject
import java.util.*

class UserSelfDataAction: Action {

    override val name: String = "data:get:self"
    override val parameters: List<Parameter> = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        logInfo("here")
        val profile = DetailedUserDB.get(sender)
        return toJSON(profile)
    }
}