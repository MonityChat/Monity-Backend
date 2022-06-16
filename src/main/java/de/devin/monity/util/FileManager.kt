package de.devin.monity.util

import de.devin.monity.LocationGetter
import java.io.File
import java.util.UUID

object FileManager {

    fun getNewFileToUploadProfilePicture(uuid: UUID, fileType: String): File {
        val file = File(LocationGetter().getLocation().absolutePath + "/../data/images/users/$uuid/profilepicture.$fileType")
        if (file.exists()) {
            file.delete()
        }
        file.parentFile.mkdirs()
        return file
    }

    fun getNewFileToUploadFile(chatID: UUID, embedID: UUID, fileType: String, fileName: String): File {

        var file = File(LocationGetter().getLocation().absolutePath + "/../data/images/chats/$chatID/$embedID/$fileName.$fileType")
        while (file.exists()) {
            file = File(LocationGetter().getLocation().absolutePath + "/../data/images/chats/$chatID/$embedID/${fileName + UUID.randomUUID()}.$fileType")
        }
        file.parentFile.mkdirs()
        return file
    }

}