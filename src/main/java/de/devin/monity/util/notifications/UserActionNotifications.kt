package de.devin.monity.util.notifications

import de.devin.monity.network.db.user.*
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*


/**
 * Occurs when the user stopped typing
 * @param typer the user who stopped
 * @param chatID in which chat he stopped
 */
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

/**
 * Occurs when a user started typing
 * @param typer the user who started typing
 * @param chatID in which chat he started
 */
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

/**
 * Occurs when a user went online
 * @param user the user who is now online
 */
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


/**
 * Occurs when a user went offline
 * @param user the user who went offline
 */
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


/**
 * Occurs when a user updated his profile
 * @param updater the user who updated his profile
 */
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


