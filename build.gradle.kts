// ServerPulse - Copyright (C) VibeSSH.
// Licensed under the GNU General Public License v3.0 or later; see LICENSE.
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    java
    // Bundles the platform and its transitive libraries into the plugin jar and
    // relocates them, since the server provides none of them. Shadow 9.x is the
    // line that supports Gradle 9 - 8.3.x fails :shadowJar with "could not add
    // file to ZIP" on this wrapper.
    id("com.gradleup.shadow") version "9.0.0"
}

// group / version come from gradle.properties.

java {
    // The platform's toolchain is JDK 25, but the Paper side targets Java 21 bytecode.
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-parameters"))
}

dependencies {
    // Paper API - provided by the server at runtime.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Gson - Paper ships it, so compile against it but do not bundle it.
    compileOnly("com.google.code.gson:gson:2.14.0")

    // The RoyalMC platform. platform-paper pulls platform-api/common, LiteCommands,
    // Multification, XSeries and Adventure transitively; platform-config pulls Okaeri.
    // Publish them first: (cd royalmc-platform && ./gradlew publishToMavenLocal).
    implementation("pl.royalmc:platform-paper:1.25.0")
    implementation("pl.royalmc:platform-config:1.25.0")
}

tasks.shadowJar {
    archiveFileName.set("ServerPulse-${project.version}.jar")

    manifest {
        attributes(
            "Implementation-Title" to "ServerPulse",
            "Implementation-Version" to project.version.toString(),
            "License" to "GPL-3.0-or-later",
        )
    }

    // Relocate everything the plugin bundles so two RoyalMC plugins on one server
    // cannot fight over library versions. XSeries in particular must be relocated.
    val libs = "dev.vibessh.serverpulse.libs"
    relocate("com.cryptomorin.xseries", "$libs.xseries")
    relocate("dev.rollczi", "$libs.litecommands")
    relocate("eu.okaeri", "$libs.okaeri")
    relocate("com.eternalcode.multification", "$libs.multification")

    mergeServiceFiles()
}

// `build` produces the shaded jar; the thin jar is not useful on its own.
tasks.build {
    dependsOn(tasks.shadowJar)
}
