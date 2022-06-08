package de.devin.monity.network.httprouting

import de.devin.monity.Monity
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*


fun Route.UtilRoute() {

    route("/util") {
        get("default_profile_picture") {
            return@get call.respondText(Monity.dataFolder.absolutePath + "/images/monity/default.png")
        }
    }
}
