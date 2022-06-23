package de.devin.monity

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.devin.monity.network.db.chat.*
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.network.db.user.UserSettingsDB
import de.devin.monity.network.httprouting.*
import de.devin.monity.network.wsrouting.ActionHandler
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.network.wsrouting.WebSocketHandler.close
import de.devin.monity.network.wsrouting.WebSocketHandler.closeConnection
import de.devin.monity.network.wsrouting.WebSocketHandler.send
import de.devin.monity.util.TypingManager
import de.devin.monity.util.html.respondHomePage
import filemanagment.filemanagers.ConfigFileManager
import filemanagment.util.ConsoleColors
import filemanagment.util.logInfo
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URLDecoder
import de.devin.monity.util.Error
import io.ktor.websocket.*

val bootLocation = LocationGetter().getLocation()
const val version = "1.3.3"
const val name = "Monity"

fun main() {
    Monity.boot()
}

object Monity {

    private val config = ConfigFileManager
    private lateinit var db: Database
    val dataFolder = File(LocationGetter().getLocation().absolutePath + "/../data")
    fun boot() {
        logInfo("Data: ${dataFolder.absolutePath}")
        logInfo("Launching $name ${ConsoleColors.GREEN}v.$version")
        logInfo("Launching HTTP Server on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        runHTTPServer()
        logInfo("Server running on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")
        logInfo("Connecting to Database ${ConsoleColors.YELLOW}${config.getSQLHost()}:${config.getSQLPort()}/${config.getSQLDatabase()}")
        runDatabase()
        ActionHandler.loadDefaultActions()
        TypingManager.loadTimer()

    }

    private fun runHTTPServer() {
        embeddedServer(CIO, port = config.getHTTPPort(), host = config.getHTTPHost()) {
            this.environment.monitor.subscribe(Routing.RoutingCallStarted) {
                handlePreRoute(it)
            }
            //CORS installation and configuration for internal cross routing
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
            }

            //Automatized De/Serialization from incoming outgoing HTTP objects
            install(ContentNegotiation) {
                gson()
            }

            //Websockets extensions
            install(WebSockets) {
                pingPeriodMillis = 5000
                timeoutMillis = 10000
            }
            install(Routing)


            //websocket routing
            routing {
                webSocket("/monity") {
                    try {
                        logInfo("User trying to connect.")
                        WebSocketHandler.handleIncomingRequest(this)
                        while (!WebSocketHandler.isValidConnection(this)) {} //Wait

                        //User successfully connected
                        val user = WebSocketHandler.getUserFrom(this)
                        WebSocketHandler.executeLoginActions(user.uuid)

                        //everytime the user sends something over the websocket
                        for (frame in incoming) {
                            try {
                                val returnPacket = WebSocketHandler.handleIncomingContent(frame, this)
                                send(returnPacket)
                            } catch (e: java.lang.Exception) {
                                //if anything goes wrong while executing the given action, it will return an error to the user and close the connection.
                                send(Error.THERE_WAS_AN_UNHANDLED_INTERNAL_EXCEPTION)
                                val message = e.message ?: "Unknown"
                                this.close(CloseReason.Codes.INTERNAL_ERROR, Error.THERE_WAS_AN_UNHANDLED_INTERNAL_EXCEPTION, message)
                            }
                        }
                    }catch (e: ClosedReceiveChannelException) {
                        closeConnection(this)
                    }catch (e: Throwable) {
                        closeConnection(this)
                        e.printStackTrace()
                    } finally {
                        closeConnection(this)
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
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 1000
            maxLifetime = 60000
        }

        db = Database.connect(HikariDataSource(hikariConfig))
        transaction {
            DetailedUserDB.load()
            UserDB.load()
            UserContactDB.load()
            ChatDB.load()
            GroupDB.load()
            GroupMemberDB.load()
            GroupProfileDB.load()
            GroupSettingDB.load()
            MediaDB.load()
            MessageDB.load()
            UserSettingsDB.load()
            ReactionDB.load()
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