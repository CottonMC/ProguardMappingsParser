package io.github.cottonmc.proguardparser

import arrow.optics.optics

@optics data class ProjectMapping(val classes: List<ClassMapping>) {
    fun findClass(oldName: String): ClassMapping =
        classes.find { it.from == oldName } ?: throw NoSuchElementException("Unknown class: $oldName")

    // This relies on classes existing in the old package.
    fun findPackage(oldName: String): String? =
        classes.find { it.from.substringBeforeLast('.') == oldName }?.to?.substringBeforeLast('.')

    companion object
}

@optics data class ClassMapping(val from: String, val to: String, val fields: List<FieldMapping>, val methods: List<MethodMapping>) {
    fun findMethod(oldName: String, parameters: List<String>? = null): MethodMapping? =
        methods.find {
            it.from == oldName && (parameters == null || it.parameters == parameters)
        }

    fun findField(oldName: String, type: String? = null): FieldMapping? =
        fields.find {
            it.from == oldName && (type == null || it.type == type)
        }

    companion object
}

@optics data class FieldMapping(val type: String, val from: String, val to: String) {
    companion object
}

@optics data class MethodMapping(
    val returnType: String,
    val from: String,
    val to: String,
    val parameters: List<String>
) {
    companion object
}
