package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.DetailedUserDB
import de.devin.monity.network.db.UserProfile
import de.devin.monity.util.Status
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

class ProfileUpdateAction: Action {

    override val name: String
        get() = "profile:update"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("description"), Parameter("status"), Parameter("shortStatus"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val description = request.getString("description")
        val status = Status.valueOf(request.getString("status"))
        val shortStatus = request.getString("shortStatus")

        val oldProfile = DetailedUserDB.get(sender)
        val newProfile = UserProfile(oldProfile.userName, oldProfile.profileImageLocation, sender, description, status, shortStatus, oldProfile.lastSeen, status)

        DetailedUserDB.updateProfile(sender, newProfile)
        return toJSON(newProfile)
    }
}