package de.devin.monity.httprouting

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*


fun Route.UserRoute() {
    post("/user/{action}") {
        if (!authRoute(call)) return@post call.respondText("Insufficient authentication", status = HttpStatusCode.Unauthorized)
        val action = call.parameters["action"] ?: return@post call.respondText("Missing ", status = HttpStatusCode.BadRequest)
    }
}