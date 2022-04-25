package de.devin.monity.httprouting

import de.devin.monity.bootLocation
import de.devin.monity.db.UserDB
import de.devin.monity.util.html.emailHTML
import de.devin.monity.util.htmlEmail
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.*

private val userEmailConfirmationMap = HashMap<UUID, UUID>()

fun Route.UserRoute() {
    post("/user/{action}") {
        //if (!authRoute(call)) return@post call.respondText("Insufficient authentication", status = HttpStatusCode.Unauthorized)
        val action = call.parameters["action"] ?: return@post call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        val auth = call.request.headers["authentication"]!!

        var success = false
        println(1)
        val user: UserData
        try {
           user = call.receiveOrNull() ?: return@post call.respondText("Invalid body given", status = HttpStatusCode.BadRequest)
        }catch (e: Exception) {
            e.printStackTrace()
            return@post
        }
        println(2)

        when (action) {
            "register" -> run {
                success = userRegister(user.username, user.password, user.email, auth)
            }
            "confirm" -> run {
                val id = UUID.fromString(call.request.queryParameters["id"] ?: return@post call.respondText("Missing parameter id", status = HttpStatusCode.BadRequest))
                val uuid = UUID.fromString(call.request.queryParameters["uuid"] ?: return@post call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest))
                if (id in userEmailConfirmationMap) {
                    if (userEmailConfirmationMap[id] != uuid) {
                        userEmailConfirmationMap.remove(id)
                        return@post call.respondText("ID does not match given UUID", status =  HttpStatusCode.BadRequest)
                    }

                    transaction {
                        UserDB.update({UserDB.uuid eq uuid.toString()}) {
                            it[confirmed] = true
                        }
                    }
                    userEmailConfirmationMap.remove(id)
                    return@post call.respondHtml {
                        head { title("Success") }
                        body { h1 { +"Thanks for registration" } }
                    }
                }
            }
        }

        call.respondText("$success", status = HttpStatusCode.OK)

    }

}

data class UserData(val username: String,
                    val password: String,
                    val email: String,
                    val uuid: String)

private fun userRegister(username: String, password: String, emailAddress: String, auth: String): Boolean {
    var userExists = true

    transaction {
        userExists = UserDB.select((UserDB.name eq username) or (UserDB.email eq emailAddress)).count() != 0L
    }

    if (userExists) return false

    val uuid = UUID.randomUUID()

    transaction {
        UserDB.insert {
            it[name] = username
            it[UserDB.password] = password
            it[salt]  = ""
            it[email] = emailAddress
            it[UserDB.uuid] = uuid.toString()
            it[confirmed] = false
        }
    }

    val id = UUID.randomUUID()
    val link = "http://127.0.0.1:8808/user/confirm?auth=$auth&id=$id&uuid=$uuid.to"

    val email = htmlEmail()
    email.addTo(emailAddress)
    email.subject = "Monity verification"
    val cid = email.embed(File("$bootLocation/../resources/Logo.png"), "logo")
    val cidWaves = email.embed(File("$bootLocation/../resources/waves.svg"), "background")

    email.setHtmlMsg(emailHTML(link, cid, cidWaves))
    email.send()


    userEmailConfirmationMap[id] = uuid
    return true
}