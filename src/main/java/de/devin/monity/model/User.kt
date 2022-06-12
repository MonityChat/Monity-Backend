package de.devin.monity.model

import de.devin.monity.network.db.user.UserProfile
import de.devin.monity.network.httprouting.UserData
import io.ktor.http.cio.websocket.*
import java.util.UUID


/**
 * This serves as a directly usable model of a user.
 */
open class User(private val userData: UserData, private val userProfile: UserProfile, val socketSession: DefaultWebSocketSession) {

    private val contacts: List<User> = listOf()

    fun getUserName(): String {
        return userData.username
    }

    fun getProfile(): UserProfile {
        return userProfile
    }

    fun getEmail(): String {
        return userData.email
    }

    fun getUUID(): UUID {
        return UUID.fromString(userData.uuid)
    }


}