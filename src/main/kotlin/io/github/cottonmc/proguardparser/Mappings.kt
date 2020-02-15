package io.github.cottonmc.proguardparser

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.extensions.fx
import arrow.optics.optics

/**
 * A renameable object, such as a [class][ClassMapping], [method][MethodMapping] or a [field][FieldMapping].
 */
interface Renameable {
    /**
     * The name in the original namespace (usually unobfuscated).
     */
    val from: String

    /**
     * The name in the target namespace (usually obfuscated).
     */
    val to: String
}

private inline fun <T : Renameable> checkDuplicates(
    ts: List<T>, type: String,
    crossinline additionalChecker: (a: T, b: T) -> Boolean = { _, _ -> false }
): Either<String, List<T>> {
    ts.fold(emptyMap<String, T>()) { map, value ->
        if (value.from in map && additionalChecker(value, map[value.from]!!))
            return Left("Duplicate original $type name ${value.from}: ${map[value.from]}, $value")
        else
            map + (value.from to value)
    }

    ts.fold(emptyMap<String, T>()) { map, value ->
        if (value.to in map && additionalChecker(value, map[value.from]!!))
            return Left("Duplicate target $type name ${value.to}: ${map[value.to]}, $value")
        else
            map + (value.to to value)
    }

    return Right(ts)
}

/**
 * A project mapping represents a single mapping file as a list of classes.
 *
 * @property classes the classes in this project
 */
@optics data class ProjectMapping(val classes: List<ClassMapping>) {
    fun findClass(oldName: String): ClassMapping =
        classes.find { it.from == oldName } ?: throw NoSuchElementException("Unknown class: $oldName")

    fun findClassOrNull(oldName: String): ClassMapping? =
        classes.find { it.from == oldName }

    // This relies on classes existing in the old package.
    fun findPackage(oldName: String): String? =
        classes.find { it.from.substringBeforeLast('.') == oldName }?.to?.substringBeforeLast('.')

    fun findClassesInPackage(oldName: String): List<ClassMapping> =
        classes.filter { it.from.startsWith("$oldName.") }

    fun validate(): Either<String, ProjectMapping> =
        Either.fx {
            ProjectMapping(checkDuplicates(classes, "class").bind().map { it.validate().bind() })
        }

    companion object
}

/**
 * A class.
 *
 * @property fields the [fields][FieldMapping]
 * @property methods the [methods][MethodMapping]
 */
@optics data class ClassMapping(
    override val from: String,
    override val to: String,
    val fields: List<FieldMapping>,
    val methods: List<MethodMapping>
) : Renameable {
    /**
     * The simple name of the class in the original namespace (usually unobfuscated).
     */
    val fromSimpleName: String get() = from.substringAfterLast('.')

    /**
     * The simple name of the class in the target namespace (usually obfuscated).
     */
    val toSimpleName: String get() = to.substringAfterLast('.')

    @JvmOverloads
    fun findMethod(oldName: String, returnType: String? = null, parameters: List<String>? = null): MethodMapping? =
        methods.find {
            it.from == oldName && (returnType == null || it.returnType == returnType) && (parameters == null || it.parameters == parameters)
        }

    @JvmOverloads
    fun findField(oldName: String, type: String? = null): FieldMapping? =
        fields.find {
            it.from == oldName && (type == null || it.type == type)
        }

    fun validate(): Either<String, ClassMapping> =
        Either.fx {
            !checkDuplicates(methods, "method") { a, b -> a.returnType == b.returnType && a.parameters == b.parameters }
            !checkDuplicates(fields, "field") { a, b -> a.type == b.type }
            this@ClassMapping
        }

    companion object
}

/**
 * A field.
 *
 * @property type the field type in original names
 */
@optics data class FieldMapping(val type: String, override val from: String, override val to: String): Renameable {
    companion object
}

/**
 * A method.
 *
 * @property returnType the return type in original names
 * @property parameters the parameters in original names
 * @property originalClass the original class of this method before merging
 * @property startLine the starting line in the bytecode
 * @property endLine the final line in the bytecode
 * @property originalStartLine the starting line before optimization
 * @property originalEndLine the starting line before optimization
 */
@optics data class MethodMapping(
    val returnType: String,
    override val from: String,
    override val to: String,
    val parameters: List<String>,
    val originalClass: String? = null,
    val startLine: Int = MISSING_LINE,
    val endLine: Int = MISSING_LINE,
    val originalStartLine: Int = MISSING_LINE,
    val originalEndLine: Int = MISSING_LINE
): Renameable {
    companion object {
        /**
         * The constant used for line numbers when they are missing.
         */
        const val MISSING_LINE: Int = -1
    }
}
