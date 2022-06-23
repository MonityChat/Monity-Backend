package de.devin.monity.util

import de.devin.monity.LocationGetter
import java.io.File
import java.util.Random
import java.util.UUID

/**
 * Util class which helps to manage where user uploaded files are stored on the system drive
 * This becomes most useful when uploading new files
 * @see UploadRouting.kt
 */
object FileManager {

    /**
     * Will search a place where a user profile picture will be stored
     * @param uuid the user who uploads the new profile picture
     * @param fileType the filetype of the profile picture
     * @return a path where the new profile picture will be stored
     */
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

    /**
     * Will search for a place where to store a uploaded file
     * @param chatID the chat where the file was uploaded to
     * @param embedID the embedID which links to the path
     * @param fileNameAndType the name and type of the class. For example image.png
     * @return a path where the file will be uploaded to
     */
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