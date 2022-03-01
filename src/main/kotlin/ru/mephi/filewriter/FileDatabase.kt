package ru.mephi.filewriter

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class FileDatabase(private val filePath: String) {
    private val mutex = Mutex()
    private var currentConnection: String = ""
    private val file = File(filePath)

    fun lockControl(connectionName: String): Boolean {
        if (mutex.isLocked) {
            return false
        }
        CoroutineScope(Dispatchers.IO).launch {
            mutex.lock()
            println("mutex.isLocked: " + mutex.isLocked + " by " + connectionName)
            currentConnection = connectionName
        }
        return true
    }

    fun unlockControl(connectionName: String): Boolean {
        return if (mutex.isLocked && connectionName == currentConnection) {
            currentConnection = ""
            mutex.unlock()
            println("mutex.isLocked: " + mutex.isLocked + " by " + connectionName)
            mutex.isLocked
        } else
            mutex.isLocked
    }

    fun readFile(): List<String> {
        val file = File(File(".").absolutePath.removeSuffix(".") + filePath)
        return file.readText().split("\n")
    }

    fun writeFile(text: String, name: String): Boolean {
        if (name == currentConnection)
            mutex.unlock()

        if (mutex.isLocked)
            return false

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            mutex.withLock {
                println("mutex is locked by $currentConnection")
                file.writeText(text)
                delay(5000)
                println("mutex is unlocked $currentConnection")
            }
        }
        return true
    }
}