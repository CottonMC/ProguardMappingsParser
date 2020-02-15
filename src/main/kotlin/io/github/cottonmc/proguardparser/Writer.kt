package io.github.cottonmc.proguardparser

fun ProjectMapping.toProguardMappings(): List<String> =
    classes.flatMap { c ->
        val fields = c.fields.map { "${it.type} ${it.from} -> ${it.to}" }
        val methods = c.methods.map {
            val newLineNums =
                if (it.startLine != MethodMapping.MISSING_LINE)
                    "${it.startLine}:${it.endLine}:"
                else
                    ""

            val oldLineNums: String =
                if (it.originalStartLine != MethodMapping.MISSING_LINE) {
                    if (it.originalEndLine != MethodMapping.MISSING_LINE) {
                        ":${it.originalStartLine}:${it.originalEndLine}"
                    } else {
                        ":${it.originalStartLine}"
                    }
                } else {
                    ""
                }

            val originalClass =
                if (it.originalClass != null) it.originalClass + '.'
                else ""

            val params = it.parameters.joinToString(separator = ",")
            "$newLineNums${it.returnType} $originalClass${it.from}($params)$oldLineNums -> ${it.to}"
        }
        listOf("${c.from} -> ${c.to}:") + (fields + methods).map { "    $it" }
    }
