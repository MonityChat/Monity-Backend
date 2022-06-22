package de.devin.monity.util.html

import kotlinx.html.*
import de.devin.monity.name
import de.devin.monity.version
import io.ktor.server.application.*
import io.ktor.server.html.*


suspend fun respondHomePage(call: ApplicationCall) {
    call.respondHtml {
        head {
            title("Monity API")
            link {
                rel = "stylesheet"
                href = "https://use.typekit.net/rue5taw.css"
            }
            link {
                rel = "icon"
                href = "icon.png"
            }
        }
        body {
            style {
                unsafe {
                    raw("""
                        .verticalcenter {
                            display: block;
                            margin-left: auto;
                            margin-right: auto;
                            width: 50%; 
                        }
                        .imglogo {
                            width: 700px;
                            height: auto;
                        }   
                        .logofont {
                            font-family: josefin-sans, sans-serif;
                            font-weight: 700;
                            font-style: normal;
                            text-align: center;
                            color: white;
                        }
                        html {
                            background-image: url('waves.svg');
                            background-repeat: no-repeat;
                            background-size: cover;
                        }
                    """.trimIndent())
                }
            }
            div {
                img(src = "Logo.png", classes = "imglogo verticalcenter")
                h1 (classes = "verticalcenter logofont") {
                    +"$name Backend API v.$version".uppercase()
                }
                h2 (classes = "verticalcenter logofont") {
                    +"Developed by: Mike Benz, Devin Fritz, Simon Ritter".uppercase()
                }
                h2 (classes = "verticalcenter logofont") {
                    +"More information: ".uppercase()
                    a {
                        href = "https://github.com/MonityChat/Monity-Backend/wiki"
                        +"$name Github"
                    }
                }
            }
        }
    }
}