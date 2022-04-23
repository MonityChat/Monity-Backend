package filemanagment.filemanagers

import filemanagment.FileManager

class ConfigFileManager: FileManager("config.yml", "config.yml") {

    fun getHost(): String = configuration.getString("host")
    fun getPort(): Int = configuration.getInt("port")

}