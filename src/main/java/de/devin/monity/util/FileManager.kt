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


}