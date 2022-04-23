import de.devin.monity.httprouting.AuthRoute
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
import java.io.File
import java.net.URLDecoder

val bootLocation = LocationGetter().getLocation()
const val version = "1.0.0"
const val name = "Monity"

fun main() {
    Monity().boot()
}

class Monity() {

    private val config = ConfigFileManager()

    fun boot() {
        logInfo("Launching $name ${ConsoleColors.GREEN}v.$version")
        logInfo("Launching HTTP Server on port ${ConsoleColors.YELLOW}${config.getPort()}")
        runHTTPServer()
        logInfo("Server running on port ${ConsoleColors.YELLOW}${config.getPort()}")
    }

    private fun runHTTPServer() {
        embeddedServer(Tomcat, port = config.getPort(), host = config.getHost()) {
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

            }

        }.start(wait = false)
    }
}

class LocationGetter() {
    fun getLocation(): File {
        val url = javaClass.protectionDomain.codeSource.location;
        val jarFileLocation = File(url.path).parentFile;
        val path = URLDecoder.decode(jarFileLocation.absolutePath, "UTF-8");
        return File(path);
    }
}