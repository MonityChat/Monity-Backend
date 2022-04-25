package de.devin.monity

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.devin.monity.db.UserDB
import de.devin.monity.httprouting.AuthRoute
import de.devin.monity.httprouting.UserRoute
import de.devin.monity.httprouting.handlePreRoute
import de.devin.monity.util.html.respondHomePage
import filemanagment.filemanagers.ConfigFileManager
import filemanagment.util.ConsoleColors
import filemanagment.util.logInfo
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URLDecoder

val bootLocation = LocationGetter().getLocation()
const val version = "1.0.0"
const val name = "Monity"

fun main() {
    Monity().boot()
}

class Monity {

    private val config = ConfigFileManager
    private lateinit var db: Database

    fun boot() {
        logInfo("Launching $name ${ConsoleColors.GREEN}v.$version")
        logInfo("Launching HTTP Server on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        runHTTPServer()
        logInfo("Server running on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        logInfo("Connecting to Database ${ConsoleColors.YELLOW}${config.getSQLHost()}:${config.getSQLPort()}/${config.getSQLDatabase()}")
        runDatabase()
    }

    private fun runHTTPServer() {
        val server = embeddedServer(Tomcat, port = config.getHTTPPort(), host = config.getHTTPHost()) {
            environment.monitor.subscribe(Routing.RoutingCallStarted) {
                handlePreRoute(it)
            }

            install(ContentNegotiation) {
                gson()
            }

            routing {
                resources("html.home")
                get("/") {
                    respondHomePage(call)
                }
                AuthRoute()
                UserRoute()
            }

        }.start(wait = false)
    }

    private fun runDatabase() {
        val hikariConfig = HikariConfig().apply {
            username = config.getSQLUser()
            password = config.getSQLPassword()
            jdbcUrl = "jdbc:mariadb://${config.getSQLHost()}:${config.getSQLPort()}/${config.getSQLDatabase()}"
            driverClassName = "org.mariadb.jdbc.Driver"
            maximumPoolSize = 10
        }
        db = Database.connect(HikariDataSource(hikariConfig))
        transaction {
            UserDB.load()
        }
    }
}

class LocationGetter {
    fun getLocation(): File {
        val url = javaClass.protectionDomain.codeSource.location
        val jarFileLocation = File(url.path).parentFile
        val path = URLDecoder.decode(jarFileLocation.absolutePath, "UTF-8")
        return File(path)
    }
}