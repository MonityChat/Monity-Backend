package de.devin.monity.util

import de.devin.monity.LocationGetter
import java.io.File
import java.util.Random
import java.util.UUID

object FileManager {

    fun getNewFileToUploadProfilePicture(uuid: UUID, fileType: String): File {
        val number = Random().nextInt(1000000)
        val file = File(LocationGetter().getLocation().absolutePath + "/../data/images/users/$uuid/profilepicture$number.$fileType")
        if (file.parentFile.listFiles() != null) {
            for (fileItem in file.parentFile.listFiles()) {
                fileItem.delete()
            }
        }

        if (file.exists()) {
            file.delete()
        }
        file.parentFile.mkdirs()
        return file
    }

    fun getNewFileToUploadFile(chatID: UUID, embedID: UUID, fileNameAndType: String): File {

        var fileNameSplit = fileNameAndType.split(".")
        val fileName = fileNameSplit[0]
        val fileNameType = fileNameSplit[1]

        var file = File(LocationGetter().getLocation().absolutePath + "/../data/images/chats/$chatID/$embedID/$fileName.$fileNameType")
        while (file.exists()) {
            file = File(LocationGetter().getLocation().absolutePath + "/../data/images/chats/$chatID/$embedID/${fileName + UUID.randomUUID()}.$fileNameType")
        }
        file.parentFile.mkdirs()
        return file
    }

}