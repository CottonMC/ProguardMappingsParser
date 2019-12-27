plugins {
    `java-library`
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    `maven-publish`
    id("com.jfrog.artifactory") version "4.9.0"
}

group = "io.github.cottonmc"
version = "1.0.0"

repositories {
    jcenter()
}

if (rootProject.file("private.gradle").exists()) {
    apply(from = rootProject.file("private.gradle"))
}

//the artifactory block is written in the groovy dsl
apply(from = rootProject.file("artifactory.gradle"))

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("io.arrow-kt:arrow-optics:0.10.4")
    testImplementation("io.arrow-kt:arrow-fx:0.10.4")
    kapt("io.arrow-kt:arrow-meta:0.10.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testImplementation("io.strikt:strikt-core:0.23.2")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.9")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
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

    test {
        useJUnitPlatform()
    }
}
