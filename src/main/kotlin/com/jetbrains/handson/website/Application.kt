package com.jetbrains.handson.website

import freemarker.cache.*
import freemarker.core.HTMLOutputFormat
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.features.websocket.*
import io.ktor.freemarker.*
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders.Connection
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.network.sockets.*
import io.ktor.request.receiveParameters
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import models.LineOfText
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

const val globalFilePath = "src\\main\\resources\\files\\file2.txt"
val file = MyExtendedFile(globalFilePath)
val listOfLines = mutableListOf<LineOfText>()

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        module()
    }.start(wait = true)
}

// Not working
fun startClient() {
    val client = HttpClient {
        install(io.ktor.client.features.websocket.WebSockets)
    }

    runBlocking {
        client.webSocket(port = 8080, host = "localhost", path = "/chat") {
            val incomingMessages = launch {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    println(frame.readText())
                }
            }
        }
    }
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        outputFormat = HTMLOutputFormat.INSTANCE
    }

    val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())

    routing {
        static("/static") {
            resources("files")
        }
        get("/") {

            listOfLines.clear()
            val strList = file.readFile()
            var idx = 0
            for (str: String in strList) {
                listOfLines.add(LineOfText(str, idx))
                idx += 1
            }

            call.respond(FreeMarkerContent("index.ftl", mapOf("entries" to listOfLines), ""))
        }
        post("/commitChanges") {
            val params = call.receiveParameters()
            val headline = params["text"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            println(headline)

            val wasWritten = file.writeFile(headline)
            if (wasWritten) call.respond(message = "applied successful")
            else call.respond(message = "writing is unavailable, try later")
        }
        // Not working
        webSocket("/connection") {
            val c = Connection(this)
            connections += c

            c.session.send("Hi world")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                connections.forEach { it.session.send("looser") }
            }
        }
    }
}
