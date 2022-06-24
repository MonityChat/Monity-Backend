package de.devin.monity.network.httprouting

import de.devin.monity.bootLocation
import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.db.user.*
import de.devin.monity.util.Error
import de.devin.monity.util.htmlEmail
import de.devin.monity.util.logInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

private val userEmailConfirmationMap = HashMap<UUID, UserData>()
private val userEmailResetMap = HashMap<UUID, UserData>()

const val domain = "http://localhost:3000"
const val domainAPI = "http://localhost:8808"

/**
 * The routing which handles every available user action.
 */
fun Route.userRoute() {
    get("/user/{action}") {
        val action = call.parameters["action"] ?: return@get call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        when (action) {
            "confirm" -> run {
                val id = UUID.fromString(
                    call.request.queryParameters["id"] ?: return@get call.respondText(
                        "Missing parameter id",
                        status = HttpStatusCode.BadRequest
                    )
                )
                val uuid = UUID.fromString(
                    call.request.queryParameters["uuid"] ?: return@get call.respondText(
                        "Missing parameter uuid",
                        status = HttpStatusCode.BadRequest
                    )
                )

                val error = confirmUser(id, uuid)
                call.respondRedirect("$domain/success?error=$error")
                
            }
            "salt" -> run {
                val username = call.request.queryParameters["username"]
                    ?: return@get call.respondText("Missing parameter username", status = HttpStatusCode.BadRequest)

                val hasUserName = UserDB.hasUserName(username)
                val hasUserEmail = UserDB.hasEmail(username)

                if (!hasUserEmail && !hasUserName) return@get call.respondText(
                    "User not found",
                    status = HttpStatusCode.NotFound
                )

                val user = if (hasUserName)  {
                    UserDB.getByName(username)
                } else {
                    UserDB.getByEmail(username)
                }

                call.respond(Salt(user.salt))
            }

            "exists" -> run {
                val username = call.request.queryParameters["username"]
                    ?: return@get call.respondText("Missing parameter username", status = HttpStatusCode.BadRequest)

                if (!UserDB.hasEmail(username) && !UserDB.hasUserName(username)) {
                    call.respondText("User not found", status = HttpStatusCode.NotFound)
                }
                call.respond("OK")
            }
            "resetPasswordRedirect" -> run {
                val id = call.request.queryParameters["id"] ?: return@get call.respondText("Parameter ID missing", status = HttpStatusCode.BadRequest)
                call.respondRedirect("$domain/reset-password?id=$id")
            }
        }
    }

    post("/user/{action}") {
        if (!authRoute(call)) return@post call.respondText(
            "Insufficient authentication",
            status = HttpStatusCode.Unauthorized
        )
        val action =
            call.parameters["action"] ?: return@post call.respondText("Missing ", status = HttpStatusCode.BadRequest)

        val auth = call.request.headers["authorization"]!!

        var error = Error.NONE
        val user: UserData
        try {
            user = call.receiveOrNull() ?: return@post call.respondText(
                "Invalid body given",
                status = HttpStatusCode.BadRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@post
        }


        when (action) {
            "register" -> run {
                error = userRegister(user.username, user.password, user.email, user.salt)
            }
            "resend" -> run {
                error = resendEmail(user.email)
            }
            "login" -> run {
                error = login(user, UUID.fromString(auth))
            }
            "reset" -> run {
                error = resetPassword(user)
            }
            "resetConfirm" -> run {
                val id = call.request.queryParameters["id"] ?: return@post call.respondText("Parameter ID missing", status = HttpStatusCode.BadRequest)
                error = resetPasswordConfirm(UUID.fromString(id), user)
            }
        }

        call.respond(error)
    }
}


/**
 * Userdata contains all IMPORTANT and BASIC information about every user.
 * @param username of the user
 * @param password of the user
 * @param salt of the user
 * @param email of the user
 * @param uuid of the user
 */
data class UserData(
    val username: String,
    val password: String,
    val salt: String,
    val email: String,
    val uuid: UUID
)

/**
 * A container class to store a salt directly as a data class
 * A salt is used to hash a password securely
 * @param salt the salt
 */
data class Salt(val salt: String)

private fun resetPasswordConfirm(id: UUID, newUser: UserData): Error {

    if (!userEmailResetMap.containsKey(id)) {
        return Error.INVALID_RESET_REQUEST
    }

    val user = userEmailResetMap[id]
    UserDB.updatePasswordAndSalt(user!!.uuid, newUser.password, newUser.salt)

    return Error.NONE

}

private fun resetPassword(user: UserData): Error {

    if (!UserDB.hasEmail(user.email)) {
        return Error.EMAIL_NOT_FOUND
    }

    var id = UUID.randomUUID()

    while (userEmailResetMap.containsKey(id)) id = UUID.randomUUID()



    val link = "$domainAPI/user/resetPasswordRedirect?&id=$id"

    val email = htmlEmail()
    email.addTo(user.email)
    email.subject = "Monity verification"
    email.embed(File("$bootLocation/../resources/Logo.png"), "logo")
    email.embed(File("$bootLocation/../resources/waves.png"), "waves")

    var htmlLines = Files.readString(Path.of("$bootLocation/../resources/email.html"))
    htmlLines = htmlLines.replace("placeholder:url", link)
    htmlLines = htmlLines.replace("placeholder:title", "Your password reset has been acknowledged.")
    htmlLines = htmlLines.replace("placeholder:content", "To reset your password click the button below.")
    htmlLines = htmlLines.replace("placeholder:button", "Reset password")


    email.setHtmlMsg(htmlLines)
    try {
        email.send()
    } catch (e: Exception) {
        return Error.EMAIL_NOT_FOUND
    }

    val userData = UserDB.getByUserOrEmail(user.email)
    userEmailResetMap[id] = userData

    return Error.NONE
}

private fun confirmUser(id: UUID, uuid: UUID): Error {

    //Nachdem ein Nutzer auf den Link in der E-Mail geklickt hat, wird diese methode ausgeführt.
    //Dabei wissen wir die ID der Registrierung sowie die UUID des zu registrierenden users.
    //Anhand dieser Daten kann nun der Account registriert werden, falls in der zwischenzeit niemand anders
    //den namen bzw. E-Mail registriert hat.

    if (!userEmailConfirmationMap.containsKey(id)) {
        return Error.INVALID_CONFIRMATION
    }

    //id und uuid passen nicht zusammen, unwahrscheinlich aber kann durch einen falschen Link vorkommen
    if (userEmailConfirmationMap[id]!!.uuid != uuid) {
        userEmailConfirmationMap.remove(id)
        return Error.INVALID_ID_UUID_COMBINATION
    }


    val user = userEmailConfirmationMap[id]!!

    //jemand hat sich den username weggeschnappt
    if (UserDB.hasUserName(user.username)) {
        userEmailConfirmationMap.remove(id)
        return Error.USERNAME_ALREADY_IN_USE
    }

    //jemand hat die email Adresse bereits registriert
    if (UserDB.hasEmail(user.email)) {
        userEmailConfirmationMap.remove(id)
        return Error.EMAIL_ALREADY_IN_USE
    }

    //nun den nutzer in die Datenbank eintragen
    UserDB.insert(user)
    DetailedUserDB.insert(createDefaultUser(user.username, (user.uuid)))
    UserSettingsDB.insert(UserSettings(user.uuid, FriendRequestLevel.ALL, DataOptions.ALL))


    userEmailConfirmationMap.remove(id)
    return Error.NONE
}

private fun login(user: UserData, auth: UUID): Error {


    val userName = user.username.ifEmpty { null } //schauen ob der nutzername mit angeben wurde
    val email = user.email.ifEmpty { null }       //schauen ob die email mit angeben wurde

    if (userName == null && email == null) {
        return Error.INVALID_LOGIN_REQUEST  // falls keine Email und kein Nutzername angegeben abbrechen
    }

    //Überprüfen ob Email oder Username existieirt
    if (userName != null && !UserDB.hasUserName(userName)) {
        return Error.USER_NOT_FOUND
    }

    if (email != null && !UserDB.hasEmail(email)) {
        return Error.EMAIL_NOT_FOUND
    }


    //Den nutzername anhand des Nutzers und Email
    val userSaved: UserData = if (userName == null)
        UserDB.getByEmail(email!!)
    else
        UserDB.getByName(userName)

    //Password überprüfung
    if (userSaved.password != user.password) {
        return Error.INVALID_PASSWORD
    }

    //Authentifizierung hochstufen
    AuthHandler.levelUpAuthKey(auth)

    return Error.NONE
}

private fun resendEmail(emailAddress: String): Error {
    //um eine E-Mail erneut schicken zu können, muss diese E-Mail mindestens einmal verwendet worden sein
    if (userEmailConfirmationMap.values.none { it.email == emailAddress }) {
        return Error.EMAIL_NOT_FOUND
    }

    val user = userEmailConfirmationMap.values.firstOrNull { it.email == emailAddress } ?: return Error.EMAIL_NOT_FOUND
    val id = userEmailConfirmationMap.keys.first { userEmailConfirmationMap[it]!!.email == emailAddress }

    val link = "$domainAPI/user/confirm?&id=$id&uuid=${user.uuid}"

    val email = htmlEmail()
    email.addTo(emailAddress)
    email.subject = "Monity verification"
    email.embed(File("$bootLocation/../resources/Logo.png"), "logo")
    email.embed(File("$bootLocation/../resources/waves.png"), "waves")

    var htmlLines = Files.readString(Path.of("$bootLocation/../resources/email.html"))
    htmlLines = htmlLines.replace("placeholder:url", link)
    htmlLines = htmlLines.replace("placeholder:title", "Thank you for creating a MONITY account!")
    htmlLines = htmlLines.replace(
        "placeholder:content",
        "Thank you for creating a Monity account.\nTo complete your registration click the link below."
    )
    htmlLines = htmlLines.replace("placeholder:button", "Confirm Email")


    email.setHtmlMsg(htmlLines)
    logInfo("Sending verification email to $emailAddress")
    email.send()
    return Error.NONE
}

private fun userRegister(username: String, password: String, emailAddress: String, salt: String): Error {

    //Damit sich ein Nutzer registrieren kann, darf dieser nutzername sowohl als auch E-Mail
    // nicht bereits verwendet worden sein
    val existsUser = UserDB.hasUserName(username)
    if (existsUser) return Error.USERNAME_ALREADY_IN_USE

    val existsEmail = UserDB.hasEmail(emailAddress)
    if (existsEmail) return Error.EMAIL_ALREADY_IN_USE

    //Jeder user hat intern eine UUID, damit sind theoretisch dieselben nutzernamen möglich, dennoch sind diese
    //auch unique um verwechselung im GUI zu verhindern
    var uuid = UUID.randomUUID()
    while (UserDB.has(uuid)) uuid = UUID.randomUUID()

    val userData = UserData(username, password, salt, emailAddress, uuid)

    //nun wird der Link erstellt, den man klicken soll, um seine anmeldung abzuschließen,
    //zu dieser uuid wird der user gespeichert der registriert werden soll mithilfe einer Map
    var id = UUID.randomUUID()
    while(userEmailConfirmationMap.containsKey(uuid)) id = UUID.randomUUID()

    val link = "$domainAPI/user/confirm?&id=$id&uuid=$uuid"

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
    htmlLines = htmlLines.replace(
        "placeholder:content",
        "Thank you for creating a Monity account.\nTo complete your registration click the link below."
    )
    htmlLines = htmlLines.replace("placeholder:button", "Confirm Email")

    email.setHtmlMsg(htmlLines)
    try {
        logInfo("Send registration email to: $emailAddress")
        email.send()
    } catch (e: Exception) {
        return Error.EMAIL_NOT_FOUND
    }


    userEmailConfirmationMap[id] = userData
    return Error.NONE
}