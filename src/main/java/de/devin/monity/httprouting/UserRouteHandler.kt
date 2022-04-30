package de.devin.monity.httprouting

import de.devin.monity.bootLocation
import de.devin.monity.db.UserDB
import de.devin.monity.util.Error
import de.devin.monity.util.htmlEmail
import filemanagment.util.logInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

private val userEmailConfirmationMap = HashMap<UUID, UserData>()

fun Route.UserRoute() {
    post("/header") {
        call.request.headers.forEach { s, strings -> run { println(s); println(strings.joinToString()) } }
        call.respondText { "ok" }
    }
    get ("/user/{action}") {
        val action = call.parameters["action"] ?: return@get call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        when (action) {
            "confirm" -> run {
                val id = UUID.fromString(call.request.queryParameters["id"] ?: return@get call.respondText("Missing parameter id", status = HttpStatusCode.BadRequest))
                val uuid = UUID.fromString(call.request.queryParameters["uuid"] ?: return@get call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest))

                val error = confirmUser(id, uuid)
                call.respond(error)
            }
        }
    }

    post("/user/{action}") {
        if (!authRoute(call)) return@post call.respondText("Insufficient authentication", status = HttpStatusCode.Unauthorized)
        val action = call.parameters["action"] ?: return@post call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        val auth = call.request.headers["authorization"]!!

        var error = Error.NONE
        val user: UserData
        try {
           user = call.receiveOrNull() ?: return@post call.respondText("Invalid body given", status = HttpStatusCode.BadRequest)
        }catch (e: Exception) {
            e.printStackTrace()
            return@post
        }
        when (action) {
            "register" -> run {
                error = userRegister(user.username, user.password, user.email, auth, user.salt)
            }
            "resend" -> run {
                error = resendEmail(user.email)
            }
            "login" -> run {
                error = login(user)
            }
        }

        call.respond(error)
    }
}

data class UserData(val username: String,
                    val password: String,
                    val salt: String,
                    val email: String,
                    val uuid: String)

private fun confirmUser(id: UUID, uuid: UUID): Error {

    //Nachdem ein Nutzer auf den Link in der E-Mail geklickt hat, wird diese methode ausgeführt.
    //Dabei wissen wir die ID der Registrierung sowie die UUID des zu registrierenden users.
    //Anhand dieser Daten kann nun der Account registriert werden, falls in der zwischenzeit niemand anders
    //den namen bzw. E-Mail registriert hat.

    if (!userEmailConfirmationMap.containsKey(id)) {
        return Error.INVALID_CONFIRMATION
    }

    //id und uuid passen nicht zusammen, unwahrscheinlich aber kann durch einen falschen Link vorkommen
    if (userEmailConfirmationMap[id]!!.uuid != uuid.toString()) {
        userEmailConfirmationMap.remove(id)
        return Error.INVALID_ID_UUID_COMBINATION
    }


    val user = userEmailConfirmationMap[id]!!

    //jemand hat sich den username weggeschnappt
    if (UserDB.hasUserName(user.username)) {
        userEmailConfirmationMap.remove(id)
        return Error.USERNAME_TAKEN
    }

    //jemand hat die email Adresse bereits registriert
    if (UserDB.hasEmail(user.email)) {
        userEmailConfirmationMap.remove(id)
        return Error.EMAIL_ALREADY_IN_USE
    }

    //nun den nutzer in die Datenbank eintragen
    UserDB.insert(user)


    logInfo("Confirmed user ${user.username}")
    userEmailConfirmationMap.remove(id)
    return Error.NONE
}

private fun login(user: UserData, auth: UUID): Error {

    if (!UserDB.hasUserName(user.username)) {
        return Error.USER_NOT_FOUND
    }

    val userSaved = UserDB.getByName(user.username)

    if (userSaved.password != user.password) {
        return Error.INVALID_PASSWORD
    }

    levelUpAuthKey(auth)

    return Error.NONE
}

private fun resendEmail(emailAddress: String): Error {
    //um eine E-Mail erneut schicken zu können, muss diese E-Mail mindestens einmal verwendet worden sein
    if (userEmailConfirmationMap.values.none { it.email == emailAddress }) {
        return Error.EMAIL_NOT_FOUND
    }

    val user = userEmailConfirmationMap.values.firstOrNull { it.email == emailAddress } ?: return Error.EMAIL_NOT_FOUND
    val id = userEmailConfirmationMap.keys.first { userEmailConfirmationMap[it]!!.email == emailAddress }

    val link = "http://127.0.0.1:8808/user/confirm?&id=$id&uuid=${user.uuid}"

    val email = htmlEmail()
    email.addTo(emailAddress)
    email.subject = "Monity verification"
    email.embed(File("$bootLocation/../resources/Logo.png"), "logo")
    email.embed(File("$bootLocation/../resources/waves.png"), "waves")

    var htmlLines = Files.  readString(Path.of("$bootLocation/../resources/email.html"))
    htmlLines = htmlLines.replace("placeholder:url", link)

    email.setHtmlMsg(htmlLines)
    email.send()
    return Error.NONE
}

private fun userRegister(username: String, password: String, emailAddress: String, auth: String, salt: String): Error {

    //Damit sich ein Nutzer registrieren kann, darf dieser nutzername sowohl als auch E-Mail
    // nicht bereits verwendet worden sein
    val existsUser = transaction {
        return@transaction UserDB.select((UserDB.name eq username)).count() != 0L
    }

    if (existsUser) return Error.USERNAME_TAKEN

    val existsEmail = transaction {
        return@transaction UserDB.select((UserDB.email eq emailAddress)).count() != 0L
    }
    if (existsEmail) return Error.EMAIL_ALREADY_IN_USE

    //Jeder user hat intern eine UUID, damit sind theoretisch dieselben nutzernamen möglich, dennoch sind diese
    //auch unique um verwechselung im GUI zu verhindern
    val uuid = UUID.randomUUID()

    val userData = UserData(username, password, salt, emailAddress, uuid.toString())

    //nun wird der Link erstellt, den man klicken soll, um seine anmeldung abzuschließen,
    //zu dieser uuid wird der user gespeichert der registriert werden soll mithilfe einer Map
    val id = UUID.randomUUID()
    val link = "http://127.0.0.1:8808/user/confirm?&id=$id&uuid=$uuid"


    //wichtig ist, der User soll jetzt noch nicht in die Datenbank gespeichert werden
    //da, wenn er bereits gespeichert wird der Name sowie E-Mail Adresse fest sind und blockiert werden
    //so lange die Registrierung aber nicht abgeschlossen ist, sollen diese noch frei bleiben.
    //Damit wird verhindert, dass namen unnötig blockiert werden

    val email = htmlEmail()
    email.addTo(emailAddress)
    email.subject = "Monity verification"
    email.embed(File("$bootLocation/../resources/Logo.png"), "logo")
    email.embed(File("$bootLocation/../resources/waves.png"), "waves")

    var htmlLines = Files.readString(Path.of("$bootLocation/../resources/email.html"))
    htmlLines = htmlLines.replace("placeholder:url", link)

    email.setHtmlMsg(htmlLines)
    email.send()

    userEmailConfirmationMap[id] = userData
    return Error.NONE
}