package de.devin.monity.util.html

import io.ktor.application.*
import io.ktor.html.*
import kotlinx.html.*
import name
import version


suspend fun respondHomePage(call: ApplicationCall) {
    call.respondHtml {
        head {
            title("Monity API")
            link {
                rel = "stylesheet"
                href = "https://use.typekit.net/rue5taw.css"
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
            }
        }
    }
}