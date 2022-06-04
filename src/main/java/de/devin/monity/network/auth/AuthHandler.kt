package de.devin.monity.network.auth

import java.util.*
import kotlin.collections.HashMap

object AuthHandler {
    private val authenticationMap = HashMap<UUID, AuthLevel>()

    fun getLevel(key: UUID): AuthLevel {
        if (!isAuthenticated(key)) error("Key is invalid")
        return authenticationMap[key]!!
    }

    fun isAuthenticated(key: UUID): Boolean {
        return authenticationMap.contains(key)
    }

    fun addDefaultAuthKey(key: UUID) {
        authenticationMap[key] = AuthLevel.AUTH_LEVEL_NONE
    }

    fun levelUpAuthKey(key: UUID) {
        if (!authenticationMap.containsKey(key)) error("Authkey was not found")
        authenticationMap[key] = authenticationMap[key]!!.nextLevel ?: return
    }




}