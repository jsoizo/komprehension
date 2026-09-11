// A separate build, not a subproject: applying the plugin the way a real consumer does is the only way
// to check that getPluginArtifact() and the runtime dependency resolve.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    // Lets `id("com.jsoizo.komprehension")` resolve to the gradle-plugin project next door.
    includeBuild("..")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Declared again outside pluginManagement so the compiler-plugin and runtime coordinates that the
// Gradle plugin asks for are substituted with the projects rather than fetched from a repository.
includeBuild("..")

rootProject.name = "komprehension-sample"
