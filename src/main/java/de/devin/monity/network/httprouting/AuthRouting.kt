package de.devin.monity.network.httprouting

import de.devin.monity.util.AuthLevel
import filemanagment.util.logError
import filemanagment.util.logInfo
import filemanagment.util.logWarning
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.util.*
import kotlin.collections.HashMap


private val authenticationMap = HashMap<String, AuthLevel>()

/**
 * Called before any routing method is called.
 */
fun handlePreRoute(call: RoutingApplicationCall) {
    logInfo("Route called: ${call.request.path()} by ${call.request.host()}")

    if (call.request.headers["authentication"] == null) {
        logWarning("API call without authentication from ${call.request.origin.remoteHost}")
    }
}

fun levelUpAuthKey(authKey: UUID) {
    if (authenticationMap[authKey.toString()] == null) error("Authkey must be included in map")
    val currentLevel = authenticationMap[authKey.toString()]!!
    val nextLevel = currentLevel.nextLevel

    if (nextLevel != null) authenticationMap[authKey.toString()] = nextLevel
}

fun Route.AuthRoute() {
    route("/auth") {
        get {
            val uuid = UUID.randomUUID()
            call.respond(Authorization(uuid, AuthLevel.AUTH_LEVEL_NONE))
            authenticationMap[uuid.toString()] = AuthLevel.AUTH_LEVEL_NONE
        }
    }
}

data class Authorization(val uuid: UUID, val authLevel: AuthLevel)

fun authRoute(call: ApplicationCall): Boolean {
    val authentication = call.request.headers["authorization"] ?: run { logError("Auth required call without auth header @${call.request.path()} from ${call.request.origin.remoteHost}"); return false }// No authentication header
    return authenticationMap.contains(authentication)
}

