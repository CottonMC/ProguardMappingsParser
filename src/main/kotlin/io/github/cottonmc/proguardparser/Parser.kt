package io.github.cottonmc.proguardparser

import juuxel.leafthrough.StringReader

private const val INDENT: String = "    "

private fun StringReader.expect(char: Char, line: Int) =
    try {
        expect(char)
    } catch (e: IllegalStateException) {
        throw IllegalStateException("Line $line: ${e.message}", e)
    }

// TODO: Also add this to StringReader
private fun StringReader.expectAll(str: String, line: Int) {
    for (c in str) {
        expect(c, line)
    }
}

fun parseProguardMappings(lines: List<String>): ProjectMapping =
    parseProguardMappings(lines.asSequence())

fun parseProguardMappings(lines: Sequence<String>): ProjectMapping {
    var project = ProjectMapping(emptyList())
    var foundClass = false
    lateinit var currentClass: ClassMapping
    var reader: StringReader? = null
    for ((lineIndex, line) in lines.withIndex()) {
        if (line.trimStart().startsWith('#')) continue

        if (reader == null) {
            reader = StringReader(line)
        } else {
            reader.resetTo(line)
        }

        val lineNumber = lineIndex + 1
        when {
            !line.startsWith(' ') /* top-level == class */ -> {
                if (foundClass) {
                    project = ProjectMapping.classes.modify(project) { it + currentClass }
                }
                foundClass = true
                val from = reader.readUntil(' ')
                reader.expectAll(" -> ", lineNumber)
                val to = reader.readUntil(':')
                reader.expect(':', lineNumber)
                currentClass = ClassMapping(
                    from,
                    to,
                    emptyList(),
                    emptyList()
                )
            }

            !foundClass -> throw IllegalArgumentException("Invalid line (should be class start): '$line'")

            '(' in line /* parens => method */ -> {
                reader.expectAll(INDENT, lineNumber)
                val firstComponent = reader.readUntil(' ') // (startline:endline:)returntype
                reader.expect(' ', lineNumber)
                val secondComponent = reader.readUntil('(') // original method name (might have class as well)
                reader.expect('(', lineNumber)
                val thirdComponent = reader.readUntil(')') // parameters
                reader.expect(')', lineNumber)
                val fourthComponent = reader.readUntil(' ') // original start/end line
                reader.expectAll(" -> ", lineNumber)
                val fifthComponent = reader.getRemaining() // obf name

                val startLine: Int
                val endLine: Int
                val returnType: String
                if (':' in firstComponent) {
                    val (s, e, r) = firstComponent.split(':')
                    startLine = s.toInt()
                    endLine = e.toInt()
                    returnType = r
                } else {
                    startLine = MethodMapping.MISSING_LINE
                    endLine = MethodMapping.MISSING_LINE
                    returnType = firstComponent
                }

                val originalName = secondComponent.substringAfterLast('.') // strip possible class name
                val parameters = thirdComponent.split(',')

                val originalStartLine: Int
                val originalEndLine: Int
                if (fourthComponent.startsWith(':')) {
                    val parts = fourthComponent.substring(1).split(':')
                    originalStartLine = parts[0].toInt()
                    originalEndLine = if (parts.size == 2) parts[1].toInt() else MethodMapping.MISSING_LINE
                } else {
                    originalStartLine = MethodMapping.MISSING_LINE
                    originalEndLine = MethodMapping.MISSING_LINE
                }

                currentClass = ClassMapping.methods.modify(currentClass) {
                    it + MethodMapping(
                        returnType = returnType,
                        from = originalName,
                        to = fifthComponent,
                        parameters = parameters,
                        startLine = startLine,
                        endLine = endLine,
                        originalStartLine = originalStartLine,
                        originalEndLine = originalEndLine
                    )
                }
            }

            else -> {
                reader.expectAll(INDENT, lineNumber)

                val type = reader.readUntil(' ')
                reader.expect(' ', lineNumber)
                val from = reader.readUntil(' ')
                reader.expectAll(" -> ", lineNumber)
                val to = reader.getRemaining()

                currentClass = ClassMapping.fields.modify(currentClass) {
                    it + FieldMapping(type, from, to)
                }
            }
        }
    }
    project = ProjectMapping.classes.modify(project) { it + currentClass }

    return project
}
