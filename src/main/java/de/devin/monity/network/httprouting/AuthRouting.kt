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


data class Authorization(val uuid: UUID, val authLevel: AuthLevel)

/**
 * Called before any routing method is called.
 */
fun handlePreRoute(call: RoutingApplicationCall) {
    if (!call.request.path().endsWith(".png"))
        logInfo("Route called: ${ConsoleColors.PURPLE}${call.request.path()} ${ConsoleColors.RESET}by ${ConsoleColors.GREEN}${call.request.host()}")
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
    return authRoute(call, AuthLevel.AUTH_LEVEL_NONE)
}


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
