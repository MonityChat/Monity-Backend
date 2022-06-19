package de.devin.monity.util.notifications

import de.devin.monity.network.db.user.*
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

        val userJson = toJSON(DetailedUserDB.get(typer))

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

        val userJson = toJSON(DetailedUserDB.get(typer))

        json.put("content", JSONObject().put("from", userJson).put("chat", chatID.toString()))
        return json
    }
}

class UserWentOnlineNotification(val user: UUID): Notification {

    override val from: UUID
        get() = user
    override val name: String
        get() = "user:action:online"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val settings = UserSettingsDB.get(user)

        val profile = when(settings.dataOptions) {
            DataOptions.NONE -> {
                createAnonymousUser(UserDB.get(user).username, user)
            }
            else -> {
                DetailedUserDB.get(user)
            }
        }

        val userJson = toJSON(profile)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}

class UserWentOfflineNotification(val user: UUID): Notification {

    override val from: UUID
        get() = user
    override val name: String
        get() = "user:action:offline"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val settings = UserSettingsDB.get(user)

        val profile = when(settings.dataOptions) {
            DataOptions.NONE -> {
                createAnonymousUser(UserDB.get(user).username, user)
            }
            else -> {
                DetailedUserDB.get(user)
            }
        }

        val userJson = toJSON(profile)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}

class UserUpdatesProfileNotification(val updater: UUID): Notification {

    override val from: UUID
        get() = updater
    override val name: String
        get() = "user:action:update:profile"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(DetailedUserDB.get(updater))

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}


