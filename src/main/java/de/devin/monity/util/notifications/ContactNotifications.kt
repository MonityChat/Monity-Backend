package de.devin.monity.util.notifications

import de.devin.monity.network.db.user.UserProfile
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

class ContactAddRequestNotification(val user: UserProfile): Notification {

    override val from: UUID = user.uuid
    override val name: String = "friend:request:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(user)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}

class ContactAddNotification(val user: UserProfile): Notification {

    override val from: UUID = user.uuid
    override val name: String = "friend:accept:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(user)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}