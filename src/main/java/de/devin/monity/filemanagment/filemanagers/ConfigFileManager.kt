package de.devin.monity.filemanagment.filemanagers

import de.devin.monity.filemanagment.FileManager


/**
 * Loads the configuration file config.yml
 */
object ConfigFileManager: FileManager("config.yml", "config.yml") {

    fun getHTTPHost(): String = configuration.getString("host")

    fun getHTTPPort(): Int = configuration.getInt("port")

    fun getSQLUser(): String = configuration.getString("sql_user")

    fun getSQLPassword(): String = configuration.getString("sql_password")

    fun getSQLHost(): String = configuration.getString("sql_host")

    fun getSQLPort(): Int = configuration.getInt("sql_port")

    fun getSQLDatabase(): String = configuration.getString("sql_db")

    fun getEmailUser(): String = configuration.getString("email_user")

    fun getEmailPassword(): String = configuration.getString("email_password")

    fun getEmailSMTPPort(): Int = configuration.getInt("email_smtp_port")
    
    fun getEmailHostName(): String = configuration.getString("email_hostname")
}