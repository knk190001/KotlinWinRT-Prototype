package com.github.knk190001.winrtbinding.generator

import com.github.knk190001.winrtbinding.generator.model.entities.*
import com.sun.jna.platform.win32.Guid.GUID
import memeid.UUID


object GuidGenerator {
    fun getSignature(typeReference: SparseTypeReference, lookup: LookUpProjectable): String {
        if (typeReference.isTypeOf("System", "Object")) {
            return "cinterface(IInspectable)"
        }
        if (typeReference.isTypeOf("System", "String")) {
            return "string"
        }
        //TODO: GetGuidSignature
        //TODO: IsValueType
        //TODO: Struct Entity
        //TODO: Enum Entity

        if (typeReference.genericParameters != null) {
            val typeParameters = typeReference.genericParameters.map { it.type }
                .map { getSignature(it!!, lookup) }
                .joinToString(";")

            val entity = lookup(typeReference)

            return when (entity) {
                is SparseInterface -> "pinterface(${entity.guid.guidToSignatureFormat()};$typeParameters)"
                else -> throw IllegalArgumentException("Non interface type reference")
            }
        }
        val sInterface = lookup(typeReference)
        return sInterface.guid.guidToSignatureFormat()
    }

    fun getSignature2(typeReference: SparseTypeReference, lookup: LookUp): String {
        val tr = typeReference.normalize()
        if (tr.isTypeOf("System", "Object")) {
            return "cinterface(IInspectable)"
        }
        if (tr.isTypeOf("System", "String")) {
            return "string"
        }
        if (tr.isValueType(lookup)) {
            when (tr.name) {
                "Boolean" -> return "b1"
                "Byte" -> return "u1"
                "Char" -> return "c2"
                "Double" -> return "f8"
                "Guid" -> return "g16"
                "Int16" -> return "i2"
                "Int32" -> return "i4"
                "Int64" -> return "i8"
                "SByte" -> return "i1"
                "Single" -> return "f4"
                "UInt16" -> return "u2"
                "UInt32" -> return "u4"
                "UInt64" -> return "u8"
                else -> {
                    val declaration = lookup(tr)
                    if (declaration is SparseEnum) {
                        return "enum(${tr.namespace}.${tr.name};${if (declaration.isFlagEnum) "u4" else "i4"})"
                    }
                    if (declaration is SparseStruct) {
                        val fields = declaration.fields.sortedBy { it.index }.map { getSignature2(it.type, lookup) }
                            .joinToString(separator = ";")
                        return "struct(${tr.namespace}.${tr.name};${fields})"
                    }
                    throw IllegalArgumentException("Invalid Value type")
                }
            }
        } else {
            val entity = lookup(typeReference)
            if (entity is IDirectProjectable<*> && typeReference.genericParameters != null) {
                val typeParameters =
                    typeReference.genericParameters.map { it.type }
                        .joinToString(";") { getSignature2(it!!, lookup) }

                return "pinterface(${entity.guid.guidToSignatureFormat()};$typeParameters)"
            }
            if (entity is SparseDelegate) {
                return "delegate(${entity.guid.guidToSignatureFormat()})"
            }
            if (entity is SparseInterface) {
                return entity.guid.guidToSignatureFormat()
            }
            if (entity is SparseClass) {
                return "rc(${entity.namespace}.${entity.name};${getSignature2(entity.defaultInterface, lookup)})"
            }
        }
        throw IllegalArgumentException("Invalid type")
    }


    //private val wrtPInterfaceNamespaceNative = GUID("11f47ad5-7b73-42c0-abae-878b1e16adee")
    private val wrtPinterfaceNamespaceJava = UUID.fromString("11f47ad5-7b73-42c0-abae-878b1e16adee")
    fun CreateIID(type: SparseTypeReference, lookup: LookUp): GUID? {
        val signature: String = getSignature2(type, lookup)
        return UUID.V5.from(wrtPinterfaceNamespaceJava, signature).toString()
            .let { GUID.fromString(it) }
    }

    private fun SparseTypeReference.isTypeOf(namespace: String, name: String): Boolean {
        return this.namespace == namespace && this.name == name
    }

    private fun SparseTypeReference.isSystemType(name: String): Boolean {
        return isTypeOf("System", name)

    }

    private fun SparseTypeReference.isValueType(
        lookup: LookUp
    ): Boolean {
        val declaration = lookup(this)
        return this.isSystemType("Boolean") ||
                this.isSystemType("Byte") ||
                this.isSystemType("Char") ||
                this.isSystemType("Double") ||
                this.isSystemType("Guid") ||
                this.isSystemType("Int16") ||
                this.isSystemType("Int32") ||
                this.isSystemType("SByte") ||
                this.isSystemType("Single") ||
                this.isSystemType("UInt16") ||
                this.isSystemType("UInt32") ||
                this.isSystemType("UInt64") ||
                declaration is SparseEnum ||
                declaration is SparseStruct
    }

    private fun String.guidToSignatureFormat(): String {
        return GUID.fromString(this).toGuidString().lowercase()
    }

}