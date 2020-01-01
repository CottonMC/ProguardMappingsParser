package io.github.cottonmc.proguardparser.test

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.io.unsafeRun.runBlocking
import arrow.unsafe
import io.github.cottonmc.proguardparser.FieldMapping
import io.github.cottonmc.proguardparser.MethodMapping
import io.github.cottonmc.proguardparser.ProjectMapping
import io.github.cottonmc.proguardparser.parseProguardMappings
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

private fun <R> runBlocking(fn: suspend () -> R): R =
    unsafe { runBlocking { IO.effect { fn() } } }

object MappingsSpek : Spek({
    Feature("mappings parser") {
        Scenario("parsing mappings") {
            lateinit var input: List<String>
            Given("an input") {
                input = MappingsSpek::class.java.getResourceAsStream("/test.txt").reader().use { it.readLines() }
            }

            lateinit var result: Either<Throwable, ProjectMapping>
            When("it is parsed") {
                result = runBlocking { Either.catch { parseProguardMappings(input) } }
            }

            Then("it should succeed") {
                expectThat(result)
                    .assertThat("is a project mapping") { it.isRight() }
            }
        }

        Scenario("finding methods") {
            lateinit var project: ProjectMapping
            Given("a project") {
                val input = MappingsSpek::class.java.getResourceAsStream("/test.txt").reader().use { it.readLines() }
                project = parseProguardMappings(input)
            }

            var method: MethodMapping? = null
            When("a method is searched") {
                method = project.findClass("juuxel.terrestrialvacation.TerrestrialVacation")
                    .findMethod("id", parameters = listOf("java.lang.String"))
            }

            Then("it should have found the correct method") {
                expectThat(method)
                    .isNotNull()
                    .and {
                        this.get("old name") { from }.isEqualTo("id")
                    }
                    .and {
                        this.get("new name") { to }.isEqualTo("tileState")
                    }
            }
        }

        Scenario("finding fields") {
            lateinit var project: ProjectMapping
            Given("a project") {
                val input = MappingsSpek::class.java.getResourceAsStream("/test.txt").reader().use { it.readLines() }
                project = parseProguardMappings(input)
            }

            var field: FieldMapping? = null
            When("a field is searched") {
                field = project.findClass("juuxel.terrestrialvacation.TerrestrialVacation")
                    .findField("_biomes")
            }

            Then("it should have found the correct field") {
                expectThat(field)
                    .isNotNull()
                    .and {
                        this.get("old name") { from }.isEqualTo("_biomes")
                    }
                    .and {
                        this.get("new name") { to }.isEqualTo("tileState")
                    }
            }
        }

        Scenario("finding packages") {
            lateinit var project: ProjectMapping
            Given("a project") {
                val input = MappingsSpek::class.java.getResourceAsStream("/test.txt").reader().use { it.readLines() }
                project = parseProguardMappings(input)
            }

            var packageName: String? = null
            When("a package is searched") {
                packageName = project.findPackage("juuxel.terrestrialvacation.dimension")
            }

            Then("it should have found the correct package") {
                expectThat(packageName)
                    .isNotNull()
                    .isEqualTo("juuxel.terrestrialvacation.castle")
            }
        }
    }
})
