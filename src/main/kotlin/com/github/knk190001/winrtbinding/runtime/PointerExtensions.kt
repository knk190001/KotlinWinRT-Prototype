package com.github.knk190001.winrtbinding.runtime

import com.sun.jna.Pointer
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

val getValueFn = Pointer::class.functions.single {
    it.name == "getValue"
}.apply {
    isAccessible = true
}

inline fun <reified T> Pointer.getValue(offset: Long, currentValue: T): T? {
    //    Object getValue(long offset, Class<?> type, Object currentValue) {
    return getValueFn.call(offset, T::class.java, currentValue) as T?
}

val ptr:JNAPointer = Pointer.NULL