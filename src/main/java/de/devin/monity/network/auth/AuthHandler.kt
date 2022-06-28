package de.devin.monity.network.auth

import java.util.*
import kotlin.collections.HashMap

object AuthHandler {
    private val authenticationMap = HashMap<UUID, AuthLevel>()
    private val authUserMap = HashMap<UUID, UUID>()
    fun getLevel(key: UUID): AuthLevel {
        if (!isAuthenticated(key)) error("Key is invalid")
        return authenticationMap[key]!!
    }

    fun bindUserToAuth(user: UUID, auth: UUID) {
        if (authUserMap.containsKey(auth)) return
        authUserMap[auth] = user
    }

    fun isUserBoundToAuth(user: UUID): Boolean {
        return authUserMap.keys.any { authUserMap[it] == user }
    }

    fun removeAuthBoundToUser(user: UUID) {
        val uuid = authUserMap.keys.first { authUserMap[it] == user }
        authUserMap.remove(uuid)
    }

    fun isAuthInUse(auth: UUID): Boolean {
        return authUserMap.containsKey(auth)
    }

    fun getUserForAuth(auth: UUID): UUID {
        return authUserMap[auth]!!
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