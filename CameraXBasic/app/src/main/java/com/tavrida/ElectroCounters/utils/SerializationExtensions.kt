package com.tavrida.ElectroCounters.utils

import java.io.*

fun Any.writeTo(file: String) = this.writeTo(File(file))

fun Any.writeTo(file: File) = FileOutputStream(file).use { ObjectOutputStream(it).use { it.writeObject(this) } }

fun <T> File.readAs(): T = FileInputStream(this).use { ObjectInputStream(it).use { it.readObject() as T } }
