package de.devin.monity.network.httprouting

import de.devin.monity.Monity
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.UtilRoute() {

    route("/util") {
        get("default_profile_picture") {
            return@get call.respondText(Monity.dataFolder.absolutePath + "/images/monity/default.png")
        }
    }
}
