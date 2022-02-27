package com.jetbrains.handson.website

import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import models.LineOfText
import java.io.File

class MyExtendedFile(val filePath: String) {

    private val mutex = Mutex()

    fun readFile(): List<String> {
        val file = File(filePath)
        return file.readText().split("\n")
    }

    fun writeFile(text: String): Boolean {
        val file = File(filePath)

        if (mutex.isLocked){
            println("isBusy")
            return false
        }

        GlobalScope.launch(Dispatchers.IO) {
            mutex.withLock {
                println("mutex is locked")
                file.writeText(text)
                delay(10000)
                println("mutex is unlocked")
            }
        }
        println("wasWritten")
        return true
    }


}