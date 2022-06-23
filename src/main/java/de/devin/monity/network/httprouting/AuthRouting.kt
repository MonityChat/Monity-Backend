package de.devin.monity.network.httprouting

import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.util.ConsoleColors
import de.devin.monity.util.logError
import de.devin.monity.util.logInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*


/**
 * Authorization is so that a websocket can show that it is authorized to execute the wanted actions.
 * A basic key can be received any time. Only by login in into an account will upgrade the key so other actions can be executed.
 * @see authRoute
 * @see AuthHandler.levelUpAuthKey
 */
data class Authorization(val uuid: UUID, val authLevel: AuthLevel)

/**
 * Called before any routing method is called.
 */
fun handlePreRoute(call: RoutingApplicationCall) {
    if (!call.request.path().endsWith(".png"))
        logInfo("Route called: ${ConsoleColors.PURPLE}${call.request.path()} ${ConsoleColors.RESET}by ${ConsoleColors.GREEN}${call.request.host()}")
}


/**
 * The route to receive a auth key
 */
fun Route.authRoute() {
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

/**
 * This function provides an easy way of looking if an HTTP request is really allowed to perform what it wants to perform.
 * It will check if the necessary headers are given and also if the authorization key is on a high enough level
 * @param call the call
 * @return true if the call is allowed false otherwise
 */
fun authRoute(call: ApplicationCall): Boolean {
    return authRoute(call, AuthLevel.AUTH_LEVEL_NONE)
}


/**
 *
 * This function provides an easy way of looking if an HTTP request is really allowed to perform what it wants to perform.
 * It will check if the necessary headers are given and also if the authorization key is on a high enough level
 * @param call the call
 * @param level the required level
 */
fun authRoute(call: ApplicationCall, level: AuthLevel): Boolean {
    val authentication = call.request.headers["authorization"] ?: run { logError("Auth required call without auth header @${call.request.path()} from ${call.request.origin.remoteHost}"); return false }// No authentication header

    val uuid: UUID
    try {
        uuid = UUID.fromString(authentication)
    }catch (e: Exception) {
        return false
    }

    return AuthHandler.isAuthenticated(uuid) && AuthHandler.getLevel(uuid).weight >= level.weight
}
