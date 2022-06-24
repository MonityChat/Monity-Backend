package de.devin.monity

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.devin.monity.network.db.chat.*
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.network.db.user.UserSettingsDB
import de.devin.monity.network.httprouting.authRoute
import de.devin.monity.network.httprouting.uploadImage
import de.devin.monity.network.httprouting.userRoute
import de.devin.monity.network.httprouting.handlePreRoute
import de.devin.monity.network.wsrouting.ActionHandler
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.network.wsrouting.WebSocketHandler.close
import de.devin.monity.network.wsrouting.WebSocketHandler.closeConnection
import de.devin.monity.network.wsrouting.WebSocketHandler.send
import de.devin.monity.network.wsrouting.WebSocketHandler.sendAndClose
import de.devin.monity.util.html.respondHomePage
import de.devin.monity.filemanagment.filemanagers.ConfigFileManager
import de.devin.monity.util.*
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
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URLDecoder

val bootLocation = LocationGetter().getLocation()
const val version = "1.3.6"
const val name = "Monity"

fun main() {
    Monity.boot()
}

/**
 * Monity main class, loading its boot function will execute all required methods to start the monity backend
 */
object Monity {

    private val config = ConfigFileManager
    private lateinit var db: Database
    private val dataFolder = File(LocationGetter().getLocation().absolutePath + "/../data")

    /**
     * Will launch all required services
     * These contain:
     *
     * - HTTP Server
     * - WebSocket Server
     * - Database
     *
     * - Validation Service
     * @see SocketValidationTask
     * - ActionHandler
     * @see ActionHandler+
     * - TypingManager
     * @see TypingManager
     */
    fun boot() {
        logInfo("Data: ${dataFolder.absolutePath}")
        logInfo("Launching $name ${ConsoleColors.GREEN}v.$version")
        logInfo("Launching HTTP Server on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")

        WebSocketHandler.startValidationService()
        WebSocketHandler.startActivityService()

        logInfo("Connecting to Database ${ConsoleColors.YELLOW}${config.getSQLHost()}:${config.getSQLPort()}/${config.getSQLDatabase()}")
        runDatabase()

        runHTTPServer()

        logInfo("Server running on port ${ConsoleColors.YELLOW}${config.getHTTPPort()}")

        setAllUsersToOffline()
        ActionHandler.loadDefaultActions()
        TypingManager.loadTimer()
    }

    /**
     * Will launch HTTP Server which can answer to HTTP request on the given port and URL.
     * Will also be able to listen to WebSockets and handle their request.
     * @see WebSocketHandler
     */
    private fun runHTTPServer() {
        embeddedServer(CIO, port = config.getHTTPPort(), host = config.getHTTPHost()) {
            this.environment.monitor.subscribe(Routing.RoutingCallStarted) {
                handlePreRoute(it)
            }

            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
            }

            install(ContentNegotiation) {
                gson()
            }

            install(WebSockets) {
                pingPeriodMillis = 5000
                timeoutMillis = 10000
            }

            install(Routing)

            routing {
                webSocket("/monity") {
                    try {
                        //Sobald sich ein Nutzer verbindet
                        WebSocketHandler.handleIncomingRequest(this)

                        //Sobald ein Nutzer Text über den Socket schickt
                        for (frame in incoming) {
                            if (!WebSocketHandler.isValidConnection(this)) {
                                sendAndClose(Error.UNAUTHORIZED)
                                return@webSocket
                            } else {
                                try {
                                    val returnPacket = WebSocketHandler.handleIncomingContent(frame, this)
                                    send(returnPacket)
                                } catch (e: java.lang.Exception) {
                                    //if anything goes wrong while executing the given action, it will return an error to the user and close the connection.
                                    e.printStackTrace()
                                    send(Error.THERE_WAS_AN_UNHANDLED_INTERNAL_EXCEPTION)
                                    val message = e.message ?: "Unknown"
                                    this.close(CloseReason.Codes.INTERNAL_ERROR, Error.THERE_WAS_AN_UNHANDLED_INTERNAL_EXCEPTION, message)
                                }
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
                authRoute()
                userRoute()
                uploadImage()
            }
        }.start(wait = false)
    }

    /**
     * Will connect to the database and load all tables
     */
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

private fun setAllUsersToOffline() {
    for (user in UserDB.getAllUsers()) {
        DetailedUserDB.setStatus(user, Status.OFFLINE)
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