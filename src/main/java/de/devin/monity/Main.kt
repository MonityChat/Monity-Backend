package de.devin.monity

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.devin.monity.network.db.DetailedUserDB
import de.devin.monity.network.db.UserContactDB
import de.devin.monity.network.db.UserDB
import de.devin.monity.network.httprouting.*
import de.devin.monity.network.wsrouting.ActionHandler
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.network.wsrouting.WebSocketHandler.send
import de.devin.monity.util.Status
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.html.respondHomePage
import filemanagment.filemanagers.ConfigFileManager
import filemanagment.util.ConsoleColors
import filemanagment.util.logInfo
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URLDecoder

val bootLocation = LocationGetter().getLocation()
const val version = "1.2.7"
const val name = "Monity"

fun main() {
    Monity.boot()
}

object Monity {

    private val config = ConfigFileManager
    private lateinit var db: Database
    val dataFolder = File(LocationGetter().getLocation().absolutePath + "/../data")
    fun boot() {
        logInfo("Launching $name ${ConsoleColors.GREEN}v.$version")
        logInfo("Launching HTTP Server on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        runHTTPServer()
        logInfo("Server running on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        logInfo("Connecting to Database ${ConsoleColors.YELLOW}${config.getSQLHost()}:${config.getSQLPort()}/${config.getSQLDatabase()}")
        runDatabase()
        ActionHandler.loadDefaultActions()
    }

    private fun runHTTPServer() {
        val server = embeddedServer(Tomcat, port = config.getHTTPPort(), host = config.getHTTPHost()) {
            environment.monitor.subscribe(Routing.RoutingCallStarted) {
                handlePreRoute(it)
            }

            //CORS installation and configuration for internal cross routing
            install(CORS) {
                anyHost()
                header(HttpHeaders.ContentType)
                header(HttpHeaders.Authorization)
            }

            //Automatized De/Serialization from incoming outgoing HTTP objects
            install(ContentNegotiation) {
                gson()
            }

            //Websockets extensions
            install(WebSockets)

            //websocket routing
            routing {
                webSocket("/monity") {
                    try {
                        WebSocketHandler.handleIncomingRequest(this)
                        while (!WebSocketHandler.isValidConnection(this)) {} //Wait

                        //User successfully connected
                        val user = WebSocketHandler.getUserFrom(this)
                        user.setStatus(user.getProfile().preferredStatus)

                        for (frame in this.incoming) {
                            val returnPacket = WebSocketHandler.handleIncomingContent(frame, this)
                            send(returnPacket)
                        }
                    }catch (e: ClosedReceiveChannelException) {
                        val user = WebSocketHandler.getUserFrom(this)
                        logInfo("Closing websocket to User ${user.getUserName()}")
                        WebSocketHandler.closed(this)
                        user.setStatus(Status.OFFLINE)
                    }catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }

            routing {
                static("assets") {
                    staticRootFolder = dataFolder
                    files(".")
                }
                resources("html.home")
                get("/") {
                    respondHomePage(call)
                }
                AuthRoute()
                UserRoute()
                UtilRoute()
                UploadImage()
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
            DetailedUserDB.load()
            UserDB.load()
            UserContactDB.load()
        }
    }
}

//Must be in class because Java is not able to get a protected domain from static functions such as this would turn into
//when not in a class
class LocationGetter {
    fun getLocation(): File {
        val url = javaClass.protectionDomain.codeSource.location
        val jarFileLocation = File(url.path).parentFile
        val path = URLDecoder.decode(jarFileLocation.absolutePath, "UTF-8")
        return File(path)
    }
}