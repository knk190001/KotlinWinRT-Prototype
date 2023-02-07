package com.github.knk190001.winrtbinding.generator.model.entities

import com.beust.klaxon.Json

data class SparseTypeReference(
    @Json("Name")
    val name: String,
    @Json("Namespace")
    val namespace: String,
    @Json("GenericParameters")
    val genericParameters: List<SparseGenericParameter>?,
    @Json("IsArray")
    val isArray: Boolean = false,
    @Json("IsReference")
    val isReference: Boolean = false
) {
    fun projectType(typeVariable: String, newTypeReference: SparseTypeReference): SparseTypeReference {
        if (name == typeVariable) {
            return newTypeReference
        }
        return copy(genericParameters = genericParameters?.map {
            it.projectType(typeVariable, newTypeReference)
        })
    }

    fun normalize(): SparseTypeReference {
        if (!this.name.contains("_")) return this
        return copy(name = "${name.replaceAfter('_', "").dropLast(1)}`${genericParameters!!.count()}")
    }

    private fun hasActualizedGenericParameter(): Boolean {
        if (genericParameters == null) return false
        return genericParameters.none {
            it.type == null
        }
    }
    private val separator = "_"

    fun getProjectedName(): String {
        if (!hasActualizedGenericParameter()) return name
        val nameWithoutBackTick = name.replaceAfterLast('`',"").dropLast(1)
        val pName = genericParameters!!.map {
            it.type!!.getProjectedName()
        }.fold(nameWithoutBackTick + separator) { acc, parameterName ->
            "$acc${parameterName}$separator"
        }

        return pName
    }//

    fun withProjectedName(): SparseTypeReference {
        return copy(name = getProjectedName())
    }

    override operator fun equals(other: Any?): Boolean {
        if (other !is INamedEntity) {
            return false
        }
        return name == other.name && namespace == other.namespace
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + (genericParameters?.hashCode() ?: 0)
        return result
    }
}
