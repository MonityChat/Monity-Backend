package de.devin.monity.httprouting

import io.ktor.application.*
import io.ktor.routing.*


fun Route.User() {
    post("/{action}") {
        val action = call.parameters
    }
}