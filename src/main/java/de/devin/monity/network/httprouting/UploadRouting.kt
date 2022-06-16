package de.devin.monity.network.httprouting

import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.network.db.chat.MediaDB
import de.devin.monity.network.db.chat.MediaData
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.util.FileManager
import de.devin.monity.util.validUUID
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.json.JSONObject
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

        post ("/chatFile") {
            if (!authRoute(this.call, AuthLevel.AUTH_LEVEL_USER)) return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
            val chatIDString = call.request.queryParameters["chatID"] ?: return@post call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest)
            val fileType = call.request.queryParameters["fileType"] ?: return@post call.respondText("Missing parameter fileType", status = HttpStatusCode.BadRequest)
            val fileName = call.request.queryParameters["fileName"] ?: return@post call.respondText("Missing parameter fileName", status = HttpStatusCode.BadRequest)
            var embedIDRaw = call.request.queryParameters["embedID"] ?: return@post call.respondText("Missing parameter embedID", status = HttpStatusCode.BadRequest)


            if (embedIDRaw.isEmpty()) {
                var randomID = UUID.randomUUID()
                while (MediaDB.has(randomID)) randomID = UUID.randomUUID()
                embedIDRaw = randomID.toString()
            }

            if (!validUUID(chatIDString)) return@post call.respondText("Invalid parameter UUID", status = HttpStatusCode.BadRequest)
            if (!validUUID(embedIDRaw)) return@post call.respondText("Invalid parameter UUID", status = HttpStatusCode.BadRequest)

            val embedID = UUID.fromString(embedIDRaw)

            if (!MediaDB.has(embedID)) return@post call.respondText("EmbedID does not exist", status = HttpStatusCode.NotFound)

            val uuid = UUID.fromString(chatIDString)

            val file = FileManager.getNewFileToUploadFile(uuid, embedID, fileType, fileName)

            val path = file.absolutePath.split("\\/images.*")[0]
            val multipartData = call.receiveMultipart()

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val fileBytes = part.streamProvider().readBytes()
                        file.writeBytes(fileBytes)
                    }
                }
            }

            val uploadedMedia = MediaData(embedID, path)
            MediaDB.insertSingle(uploadedMedia)

            val json = JSONObject().put("path", path).put("embedid", embedID)

            call.respondText(json.toString(), status = HttpStatusCode.OK)
        }
    }

}