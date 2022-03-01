package ru.mephi.filewriter

import freemarker.cache.*
import freemarker.core.HTMLOutputFormat
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

const val globalFilePath = "src/main/resources/files/file.txt"
val fileDatabase = FileDatabase(globalFilePath)
val listOfLines = mutableListOf<LineOfText>()

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        module()
    }.start()
}

fun Connection.sendMessage(msg: String) {
    CoroutineScope(Dispatchers.IO).launch {
        session.send("[${name}]: " + msg)
        println("Log: [${name}]: " + msg)
    }
}

fun Application.module() {
    install(WebSockets)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        outputFormat = HTMLOutputFormat.INSTANCE
    }

    routing {
        static("/static") {
            resources("files")
        }
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/ws") {
            val thisConnection = Connection(this)
            connections += thisConnection
            println("New client logged in as [${thisConnection.name}]")
            send("You've logged in as [${thisConnection.name}]")

            connections.forEach {
//                if (it != thisConnection)
                it.session.send("New client logged in as [${thisConnection.name}]")
            }

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        when (val receivedText = frame.readText()) {
                            "OpenConnection" -> {
                                if (!fileDatabase.lockControl(thisConnection.name)) {
                                    thisConnection.sendMessage("Unable to connect")
                                } else thisConnection.sendMessage("Connected to file")
                            }
                            "CloseConnection" -> {
                                if (fileDatabase.unlockControl(thisConnection.name))
                                    thisConnection.sendMessage("Disconnected from file")
                                else
                                    thisConnection.sendMessage("Already disconnected from file")
                            }
                            else -> {
                                thisConnection.sendMessage("Received data: $receivedText")
                                if (!fileDatabase.writeFile(receivedText, thisConnection.name)) {
                                    thisConnection.sendMessage("Unable to write. Mutex is locked.")
                                } else thisConnection.sendMessage("File successfully saved.")
                            }
                        }
                    }
                }
            }
        }
        get("/") {
            listOfLines.clear()
            val strList = fileDatabase.readFile()
            var idx = 0
            for (str: String in strList) {
                listOfLines.add(LineOfText(str, idx))
                idx += 1
            }
            call.respond(FreeMarkerContent("index.ftl", mapOf("entries" to listOfLines), ""))
        }
    }
}
