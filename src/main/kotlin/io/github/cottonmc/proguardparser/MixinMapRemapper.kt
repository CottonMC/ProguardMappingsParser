package io.github.cottonmc.proguardparser

import arrow.core.Either
import arrow.core.Left
import arrow.core.right

object MixinMapRemapper {
    fun remapMixinMaps(mappings: ProjectMapping, input: List<String>): Either<String, List<String>> {
        if (input.isEmpty()) return Left("input is empty")
        if ("v1\tnamed\tintermediary" !in input) return Left("input must use a 'v1\tnamed\tintermediary' format!")
        
        return input.map {
            when {
                it.startsWith("v1") -> it
                it.startsWith("METHOD") -> {
                    val parts = it.split('\t')
                    val className = findClassName(mappings, parts[1].replace('/', '.'))
                    val signature = parts[2]
                    val from = parts[3]
                    val to = parts[4]
                    "METHOD\t${}\t\t\t$to"
                }
                else -> it
            }
        }.right()
    }

    private fun findClassName(mappings: ProjectMapping, old: String): String =
        mappings.findClassOrNull(old)?.to ?: old

    private fun findMethodName(
        mappings: ProjectMapping, className: String, old: String, signature: MethodSignature
    ): String =
        mappings.findClassOrNull(className)
            ?.findMethod(
                old,
                returnType = signature.returnType.proguard,
                parameters = signature.parameters.map { it.proguard }
            )
            ?.to
            ?: old

    private fun convertLFormToProguard(lForm: String) =
        lForm.substring(1 until lForm.lastIndex).replace('/', '.')

    private fun parseMethodSignature(mappings: ProjectMapping, signature: String): MethodSignature {
        val builder = StringBuilder()
        val iter = signature.iterator()
        while (iter.hasNext()) {

        }
        return builder.toString()
    }

    private sealed class Type(val proguard: String, val binary: String) {
        open val mixin: String get() = binary

        override fun toString() = "$proguard $binary"

        object Void : Type("void", "V")
        object Byte : Type("byte", "B")
        object Char : Type("char", "C")
        object Double : Type("double", "D")
        object Float : Type("float", "F")
        object Int : Type("int", "I")
        object Long : Type("long", "J")
        object Short : Type("short", "S")
        object Boolean : Type("boolean", "Z")

        class Array(val of: Type) : Type(proguard = of.proguard + "[]", binary = "[" + of.binary)
        class Literal(proguard: String, binary: String) : Type(proguard, binary) {
            override val mixin = proguard.replace('.', '/')

            companion object {
                fun ofProguard(proguard: String) = Literal(proguard, "L${proguard.replace('.', '/')};")
                fun ofBinary(binary: String) = Literal(convertLFormToProguard(binary), binary)
                fun ofMixin(mixin: String) = ofBinary("L$mixin;")
            }
        }
    }

    private data class MethodSignature(val returnType: Type, val parameters: List<Type>)
}