package de.devin.monity.httprouting

import de.devin.monity.db.UserDB
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import org.apache.catalina.User
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.HashMap

private val userEmailConfirmationMap = HashMap<String, String>()

fun Route.UserRoute() {
    post("/user/{action}") {
        //if (!authRoute(call)) return@post call.respondText("Insufficient authentication", status = HttpStatusCode.Unauthorized)
        val action = call.parameters["action"] ?: return@post call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        val auth = call.request.headers["authentication"]!!

        val userName = call.request.queryParameters["username"] ?: return@post call.respondText("Missing parameter username", status = HttpStatusCode.BadRequest)
        val password = call.request.queryParameters["password"] ?: return@post call.respondText("Missing parameter password", status = HttpStatusCode.BadRequest)

        var success = false

        when (action) {
            "register" -> run {
                val email = call.request.queryParameters["email"] ?: return@post call.respondText("Missing parameter email", status = HttpStatusCode.BadRequest)
                success = userRegister(userName, password, email, auth)
            }
            "confirm" -> run {
                val id = call.request.queryParameters["id"] ?: return@post call.respondText("Missing parameter id", status = HttpStatusCode.BadRequest)
                val uuid = call.request.queryParameters["uuid"] ?: return@post call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest)

                if (id in userEmailConfirmationMap) {
                    transaction {
                        UserDB.update({UserDB.uuid eq uuid}) {
                            it[confirmed] = true
                        }
                    }
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

private fun userRegister(username: String, password: String, emailAddress: String, auth: String): Boolean {

    var userExists = true

    transaction {
        userExists = UserDB.select((UserDB.name eq username) or (UserDB.email eq emailAddress)).count() != 0L
    }

    if (userExists) return false

    val uuid = UUID.randomUUID().toString()

    transaction {
        UserDB.insert {
            it[name] = username
            it[UserDB.password] = password
            it[salt]  = ""
            it[email] = emailAddress
            it[UserDB.uuid] = uuid
            it[confirmed] = false
        }
    }

    val id = UUID.randomUUID().toString()
    val link = "http://127.0.0.1:8808/user/confirm?auth=$auth&id=$id&uuid=$uuid"

    val email = SimpleEmail()
    email.hostName = "mail.gmx.net"
    email.setSmtpPort(465)
    email.setAuthenticator(DefaultAuthenticator("monitychat@gmx.de", "kopsa8-cubsaW-myhnyj"))
    email.isSSLOnConnect = true
    email.addTo(emailAddress)
    email.setFrom("monitychat@gmx.de")
    email.subject = "Monity confirmation"
    email.setMsg("To verify your monity account click the following link:\n$link")
    email.send()

    userEmailConfirmationMap[id] = uuid

    return true
}