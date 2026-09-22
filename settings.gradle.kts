pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // Provisions a JDK 21 automatically when the machine does not have one, matching
    // the royalmc-platform toolchain the plugin is built against.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Repositories are declared here, not in build.gradle.kts, the same way the
    // platform does it.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        // The platform artifacts (pl.royalmc:platform-*) are consumed from the local
        // Maven cache. Publish them first with `./gradlew publishToMavenLocal` inside
        // royalmc-platform; swap this for GitHub Packages once that is set up.
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.panda-lang.org/releases/")
        maven("https://repo.eternalcode.pl/releases/")
        maven("https://storehouse.okaeri.eu/repository/maven-public/")
    }
}

rootProject.name = "serverpulse"
