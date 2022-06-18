package de.devin.monity.util.notifications

import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

class UserStoppedTypingNotification(val typer: UUID, val chatID: UUID): Notification {

    override val from: UUID
        get() = typer
    override val name: String
        get() = "user:action:stopped:typing"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(typer))

        json.put("content", JSONObject().put("from", userJson).put("chat", chatID.toString()))
        return json
    }
}

class UserStartedTypingNotification(val typer: UUID, val chatID: UUID): Notification {

    override val from: UUID
        get() = typer
    override val name: String
        get() = "user:action:started:typing"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(typer))

        json.put("content", JSONObject().put("from", userJson).put("chat", chatID.toString()))
        return json
    }
}


