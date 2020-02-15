package io.github.cottonmc.proguardparser

fun ProjectMapping.toProguardMappings(): List<String> =
    classes.flatMap { c ->
        val fields = c.fields.map { "${it.type} ${it.from} -> ${it.to}" }
        val methods = c.methods.map {
            // TODO: Add back line numbers
            val params = it.parameters.joinToString(separator = ",")
            "${it.returnType} ${it.from}($params) -> ${it.to}"
        }
        listOf("${c.from} -> ${c.to}:") + (fields + methods).map { "    $it" }
    }
