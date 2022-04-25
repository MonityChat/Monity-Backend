package de.devin.monity.util.html

import kotlinx.html.*
import kotlinx.html.stream.appendHTML

fun emailHTML(url: String, logoCid: String, bgCid: String): String {
    val html = StringBuilder().appendHTML().html {
        head {
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
                            font-weight: 300;
                            font-style: normal;
                            text-align: center;
                        }
                        .waves {
                            .background-image: cid:$bgCid
                        }
                    """.trimIndent())
                }
            }
            div (classes = "waves"){
                img(src = "cid:$logoCid", classes = "imglogo verticalcenter")
                h2 (classes = "verticalcenter logofont") {
                    +"Thank you for creating a MONITY account. To complete your verification click the link below"
                }
                h2 (classes = "verticalcenter logofont") {
                    a {
                        href = url
                        +"Confirmation"
                    }
                }
            }
        }
    }
    return html.toString()

}