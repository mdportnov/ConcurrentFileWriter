package ru.mephi.filewriter

import com.fasterxml.jackson.databind.*
import freemarker.cache.*
import freemarker.core.HTMLOutputFormat
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

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
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        static("/") {
            resources("html")
            resources("files")
        }

        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/ws") {
            val thisConnection = Connection(this)
            connections += thisConnection
//            println("New client logged in as [${thisConnection.name}]")
            send("You've logged in as [${thisConnection.name}]")

            connections.forEach {
                if (it != thisConnection)
                    it.session.send("New client logged in as [${thisConnection.name}]")
            }

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
//                        println("Log: ${frame.readText()}")
                        when (val receivedText = frame.readText()) {
                            "OpenConnectionEvent" -> {
                                if (!fileDatabase.lockControl(thisConnection.name)) {
                                    thisConnection.sendMessage("Unable to connect")
                                } else thisConnection.sendMessage("Connected to file")
                            }
                            "CloseConnectionEvent" -> {
                                if (fileDatabase.unlockControl(thisConnection.name))
                                    thisConnection.sendMessage("Disconnected from file")
                                else
                                    thisConnection.sendMessage("Already disconnected from file")
                            }
                            "UpdateEvent" -> {
                                updateFile()
                                connections.forEach {
                                    if (it != thisConnection)
                                        it.sendMessage("UpdateEvent")
                                }
                            }
                            else -> {
                                thisConnection.sendMessage("Received data: $receivedText")
                                if (!fileDatabase.writeFile(receivedText, thisConnection.name)) {
                                    thisConnection.sendMessage("Unable to write. Mutex is locked.")
                                } else {
                                    thisConnection.sendMessage("File successfully saved.")
                                    connections.forEach {
                                        if (it != thisConnection)
                                            it.sendMessage("File updated")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        get("/file") {
            updateFile()
            call.respond(listOfLines)
        }
    }

}

private fun updateFile() {
    listOfLines.clear()
    val strList = fileDatabase.readFile()
    var idx = 0
    for (str: String in strList) {
        listOfLines.add(LineOfText(str, idx))
        idx += 1
    }
}
