package de.devin.monity.network.httprouting

import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.util.FileManager
import de.devin.monity.util.validUUID
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.util.*


fun Route.UploadImage() {
    route("/upload") {
        post("/profileImage") {
            if (!authRoute(this.call, AuthLevel.AUTH_LEVEL_USER)) return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
            val uuidString = call.request.queryParameters["uuid"] ?: return@post call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest)
            if (!validUUID(uuidString)) return@post call.respondText("Invalid parameter UUID", status = HttpStatusCode.BadRequest)

            val fileType = "png"

            val uuid = UUID.fromString(uuidString)
            val file = FileManager.getNewFileToUploadProfilePicture(uuid, fileType)

            val multipartData = call.receiveMultipart()

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val fileBytes = part.streamProvider().readBytes()
                        file.writeBytes(fileBytes)
                    }
                }
            }
            DetailedUserDB.updateProfilePicture(uuid)

            call.respondText(DetailedUserDB.get(uuid).profileImageLocation, status = HttpStatusCode.OK)
        }
    }
}