package de.devin.monity.network.httprouting

import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.auth.AuthLevel
import filemanagment.util.logError
import filemanagment.util.logInfo
import filemanagment.util.logWarning
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.util.*


data class Authorization(val uuid: UUID, val authLevel: AuthLevel)

/**
 * Called before any routing method is called.
 */
fun handlePreRoute(call: RoutingApplicationCall) {
    logInfo("Route called: ${call.request.path()} by ${call.request.host()}")

    if (call.request.headers["authorization"] == null) {
        logWarning("API call without authentication from ${call.request.origin.remoteHost}")
    }
}

fun Route.AuthRoute() {
    route("/auth") {
        get {
            var uuid = UUID.randomUUID()
            while (AuthHandler.isAuthenticated(uuid)) uuid = UUID.randomUUID()
            call.respond(Authorization(uuid, AuthLevel.AUTH_LEVEL_NONE))
            logInfo("Authenticated UUID: $uuid")
            AuthHandler.addDefaultAuthKey(uuid)
        }
    }
}

fun authRoute(call: ApplicationCall): Boolean {
    val authentication = call.request.headers["authorization"] ?: run { logError("Auth required call without auth header @${call.request.path()} from ${call.request.origin.remoteHost}"); return false }// No authentication header

    val uuid: UUID
    try {
         uuid = UUID.fromString(authentication)
    }catch (e: Exception) {
        return false
    }

    return AuthHandler.isAuthenticated(uuid)
}

