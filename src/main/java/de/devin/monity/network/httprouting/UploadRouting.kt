package de.devin.monity.network.httprouting

import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.network.db.chat.MediaDB
import de.devin.monity.network.db.chat.MediaData
import de.devin.monity.network.db.user.DataOptions
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserSettingsDB
import de.devin.monity.util.FileManager
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.UserUpdatesProfileNotification
import de.devin.monity.util.validUUID
import filemanagment.util.logInfo
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

            val path = file.absolutePath.replace("/","\\").split("\\data")[1]

            val multipartData = call.receiveMultipart()



            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val fileBytes = part.streamProvider().readBytes()
                        file.writeBytes(fileBytes)
                    }
                }
            }
            DetailedUserDB.updateProfilePicture(uuid, path)


            if (UserSettingsDB.get(uuid).dataOptions != DataOptions.NONE) {
                for (contact in UserHandler.getOnlineUser(uuid).contacts) {
                    UserHandler.sendNotificationIfOnline(contact, UserUpdatesProfileNotification(uuid))
                }
            }


            call.respondText(DetailedUserDB.get(uuid).profileImageLocation, status = HttpStatusCode.OK)
        }

        post ("/chatFile") {
            if (!authRoute(this.call, AuthLevel.AUTH_LEVEL_USER)) return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
            val chatIDString = call.request.queryParameters["chatID"] ?: return@post call.respondText("Missing parameter uuid", status = HttpStatusCode.BadRequest)
            val fileName = call.request.queryParameters["fileName"] ?: return@post call.respondText("Missing parameter fileName", status = HttpStatusCode.BadRequest)
            var embedIDRaw = call.request.queryParameters["embedID"] ?: return@post call.respondText("Missing parameter embedID", status = HttpStatusCode.BadRequest)

            var newMediaRequest = false
            if (embedIDRaw == "na") {
                var randomID = UUID.randomUUID()
                while (MediaDB.has(randomID)) randomID = UUID.randomUUID()
                embedIDRaw = randomID.toString()
                newMediaRequest = true
            }

            if (!validUUID(chatIDString)) return@post call.respondText("Invalid parameter UUID", status = HttpStatusCode.BadRequest)
            if (!validUUID(embedIDRaw)) return@post call.respondText("Invalid parameter UUID", status = HttpStatusCode.BadRequest)

            val embedID = UUID.fromString(embedIDRaw)

            if (!newMediaRequest)
                if (!MediaDB.has(embedID)) return@post call.respondText("EmbedID does not exist", status = HttpStatusCode.NotFound)

            val uuid = UUID.fromString(chatIDString)

            val file = FileManager.getNewFileToUploadFile(uuid, embedID, fileName)

            logInfo(file.absolutePath)

            val path = file.absolutePath.replace("/","\\").split("\\data")[1]

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


            val json = JSONObject().put("path", path).put("embedID", embedID)

            return@post call.respondText(json.toString(), status = HttpStatusCode.OK)
        }
    }
}