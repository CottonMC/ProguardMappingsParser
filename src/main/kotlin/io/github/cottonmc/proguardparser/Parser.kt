package io.github.cottonmc.proguardparser

private val CLASS_LINE_PATTERN = Regex("(.+) -> (.+):")
private val METHOD_LINE_PATTERN = Regex(" +(.+) (.+)\\((.*)\\) -> (.+)")
private val FIELD_LINE_PATTERN = Regex(" +(.+) ([^(]+) -> (.+)")

fun parseProguardMappings(lines: List<String>): ProjectMapping {
    var project = ProjectMapping(emptyList())
    var foundClass = false
    lateinit var currentClass: ClassMapping
    for (line in lines) {
        if (line.trimStart().startsWith('#')) continue

        when {
            CLASS_LINE_PATTERN.matches(line) -> {
                if (foundClass) {
                    project = ProjectMapping.classes.modify(project) { it + currentClass }
                }
                foundClass = true
                val classLine = CLASS_LINE_PATTERN.find(line) ?: throw IllegalStateException()
                currentClass = ClassMapping(
                    classLine.groups[1]!!.value,
                    classLine.groups[2]!!.value,
                    emptyList(),
                    emptyList()
                )
            }

            !foundClass -> throw IllegalArgumentException("Invalid line (should be class start): '$line'")

            METHOD_LINE_PATTERN.matches(line) -> {
                val methodLine = METHOD_LINE_PATTERN.find(line) ?: throw IllegalStateException()
                currentClass = ClassMapping.methods.modify(currentClass) {
                    it + MethodMapping(
                        methodLine.groups[1]!!.value,
                        methodLine.groups[2]!!.value,
                        methodLine.groups[4]!!.value,
                        methodLine.groups[3]!!.value.split(',')
                    )
                }
            }

            FIELD_LINE_PATTERN.matches(line) -> {
                val fieldLine = FIELD_LINE_PATTERN.find(line) ?: throw IllegalStateException()
                currentClass = ClassMapping.fields.modify(currentClass) {
                    it + FieldMapping(
                        fieldLine.groups[1]!!.value,
                        fieldLine.groups[2]!!.value,
                        fieldLine.groups[3]!!.value
                    )
                }
            }

            else -> throw IllegalArgumentException("Invalid line (should be a class or a member): '$line'")
        }
    }
    project = ProjectMapping.classes.modify(project) { it + currentClass }

    return project
}
