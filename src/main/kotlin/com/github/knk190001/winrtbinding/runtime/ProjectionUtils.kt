package com.github.knk190001.winrtbinding.runtime

import com.github.knk190001.winrtbinding.runtime.interfaces.IWinRTInterface
import com.github.knk190001.winrtbinding.runtime.interfaces.IWinRTObject
import com.sun.jna.*
import com.sun.jna.platform.win32.COM.IUnknown
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import com.sun.jna.Function as JnaFunction

val WinRT = JNAApiInterface.INSTANCE

interface JNAApiInterface : StdCallLibrary {
    fun RoActivateInstance(filter: HANDLE, pref: PointerByReference): HRESULT
    fun RoGetActivationFactory(
        activatableClassId: HANDLE,
        iid: Guid.REFIID,
        factory: PointerByReference
    ): HRESULT

    fun RoInitialize(initType: Int): HRESULT
    fun RoGetParameterizedTypeInstanceIID(
        nameElementCount: WinDef.UINT,
        nameElements: Pointer,
        metadataLocator: Pointer?,
        iid: Guid.GUID.ByReference,
        pExtra: Pointer?
    ): HRESULT

    fun WindowsCreateString(sourceString: WString, length: Int, string: WinNT.HANDLEByReference): HRESULT
    fun WindowsDeleteString(hstring: HANDLE?): Int

    fun WindowsGetStringRawBuffer(str: HANDLE, length: IntByReference): WString

    companion object {
        val INSTANCE = Native.load(
            "combase",
            JNAApiInterface::class.java
        ) as JNAApiInterface
    }
}

fun checkHR(hr: HRESULT) {
    if (hr.toInt() != 0) {
        throw Win32Exception(hr)
    }
}

fun String.toHandle(): HANDLE {
    val wString = WString(this)
    val handleByReference = WinNT.HANDLEByReference()
    val hr = JNAApiInterface.INSTANCE.WindowsCreateString(wString, this.length, handleByReference)
    return handleByReference.value
}

fun HANDLE.handleToString(): String {
    val ibr = IntByReference()
    val wstr = JNAApiInterface.INSTANCE.WindowsGetStringRawBuffer(this, ibr)
    return wstr.toString()
}

private val typeMapper = WinRTTypeMapper()

class WinRTTypeMapper : DefaultTypeMapper() {
    init {
        val booleanConverter: TypeConverter = object : TypeConverter {
            override fun toNative(value: Any, context: ToNativeContext): Any {
                return if (value as Boolean) {
                    1
                } else {
                    0
                }.toByte()
            }

            override fun fromNative(value: Any, context: FromNativeContext): Boolean {
                return (value as Byte).toInt() != 0
            }

            override fun nativeType(): Class<*>? {
                return Byte::class.javaPrimitiveType
            }

        }

        val stringConverter: TypeConverter = object : TypeConverter {
            override fun toNative(value: Any, context: ToNativeContext): HANDLE {
                val str = value as String
                return str.toHandle()
            }

            override fun fromNative(value: Any, context: FromNativeContext): String {
                val handle = value as HANDLE
                return handle.handleToString()
            }

            override fun nativeType(): Class<*> {
                return Pointer::class.java
            }

        }

        val boxedLongConverter: TypeConverter = object : TypeConverter {
            override fun toNative(value: Any, context: ToNativeContext): LongArray {
                val longs = value as Array<Long>
                return longs.toLongArray()
            }

            override fun fromNative(value: Any, context: FromNativeContext): Array<Long> {
                val array = value as LongArray
                return array.toTypedArray()
            }

            override fun nativeType(): Class<*>? {
                return LongArray::class.java
            }

        }
        val boxedFloatArrayConverter: TypeConverter = object : TypeConverter {
            override fun toNative(value: Any, context: ToNativeContext): FloatArray {
                val longs = value as Array<Float>
                return longs.toFloatArray()
            }

            override fun fromNative(value: Any, context: FromNativeContext): Array<Float> {
                val array = value as FloatArray
                return array.toTypedArray()
            }

            override fun nativeType(): Class<*>? {
                return FloatArray::class.java
            }

        }

        addTypeConverter(Boolean::class.javaPrimitiveType, booleanConverter)
        addTypeConverter(String::class.java, stringConverter)
        addTypeConverter(Array<Long>::class.java, boxedLongConverter)
        addTypeConverter(Array<Float>::class.java, boxedFloatArrayConverter)

    }
}

private val winRTOptions = mapOf<String, Any?>(
    Library.OPTION_TYPE_MAPPER to typeMapper
)

fun JnaFunction.invokeHR(params: Array<Any?>): HRESULT {
    return this.invoke(HRESULT::class.java, params, winRTOptions) as HRESULT
}

inline fun <A : IWinRTInterface, reified T : A> Array<A>.interfaceOfType(): T {
    //Loop through the array and return the first interface that matches the type
    this.forEach {
        if (it is T) {
            return it
        }
    }
    throw IllegalArgumentException("No interface of type ${T::class.java.name} found in the array")
}

inline fun <reified T : IWinRTInterface, reified R : T> Array<T?>.castToImpl(): Array<T?> {
    @Suppress("UNCHECKED_CAST")
    return this.map {
        if (it is IWinRTObject) {
            it.interfaces.interfaceOfType() as R
        } else {
            it as R
        }
    }.toTypedArray() as Array<T?>
}

inline fun <reified T : IWinRTInterface, reified R : T> T.castToImpl(): R {
    return if (this !is IWinRTObject) {
        this as R
    } else {
        this.interfaces.first {
            it is R
        } as R
    }
}

fun Guid.GUID.ByReference.getValue(): Guid.GUID {
    return this
}

fun Unknown.ByReference.setValue(unknown: Unknown) {
    this.pointer = unknown.pointer
}

typealias JNAPointer = com.sun.jna.Pointer

val iUnknownIID = IUnknown.IID_IUNKNOWN;