package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.user.DataOptions
import de.devin.monity.network.db.user.FriendRequestLevel
import de.devin.monity.network.db.user.UserSettingsDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*
import de.devin.monity.util.Error


/**
 * Returns the settings of the user
 */
class UserSettingsGetAction: Action {

    override val name: String
        get() = "settings:get:self"
    override val parameters: List<Parameter>
        get() = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        return toJSON(UserSettingsDB.get(sender))
    }
}


/**
 * Changes the settings of the user
 */
class UserSettingsChangeAction: Action {

    override val name: String
        get() = "settings:change:self"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("setting"), Parameter("value"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val setting = request.getString("setting")
        val value = request.getString("value")

        when (setting) {
            "dataOption" -> {
                UserSettingsDB.setUserDataOptions(sender, DataOptions.valueOf(value))
            }
            "friendRequestLevel" -> {
                UserSettingsDB.setUserRequestLevel(sender, FriendRequestLevel.valueOf(value))
            }
            else -> {
                return Error.INVALID_SETTING.toJson()
            }
        }

        return Error.NONE.toJson()
    }
}
