plugins {
    `java-library`
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    `maven-publish`
    id("com.jfrog.artifactory") version "4.9.0"
}

group = "io.github.cottonmc"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

if (rootProject.file("private.gradle").exists()) {
    apply(from = rootProject.file("private.gradle"))
}

//the artifactory block is written in the groovy dsl
apply(from = rootProject.file("artifactory.gradle"))

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("io.arrow-kt:arrow-optics:0.10.4")
    kapt("io.arrow-kt:arrow-meta:0.10.4")
    testCompile("junit", "junit", "4.12") // TODO: Add tests
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])

            artifactId = "proguard-mappings-parser" // kebab-case :)
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}