package de.devin.monity.filemanagment

import de.devin.monity.bootLocation
import org.simpleyaml.configuration.file.YamlFile
import java.io.File
import java.nio.file.Files


/**
 * Util class to load files and read their content
 * Can also copy content from an internal resource to the given path
 * @param fileName name of file
 * @param resourceLocation location of the internal resource
 * @param initLocation where the file will be located
 */
abstract class FileManager(private val fileName: String, private val resourceLocation: String, private val initLocation: File = File("$bootLocation/$fileName")) {

    lateinit var configuration: YamlFile

    init {
        load()
    }
    private fun load() {
        if (!initLocation.exists()) {
            initLocation.parentFile.mkdirs()
            try {
                val inputStream = this::class.java.classLoader.getResourceAsStream(resourceLocation) ?: error("Can't load file ${initLocation.absolutePath} due no resource location was found. Resource location: $resourceLocation")
                Files.copy(inputStream, initLocation.toPath())
            } catch (e: Exception) {
                error("Error while loading file $fileName: ${e.message}")
            }
        }
        configuration = YamlFile.loadConfiguration(initLocation)
    }
}