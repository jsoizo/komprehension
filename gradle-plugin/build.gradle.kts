plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
    packageName("com.jsoizo.komprehension.gradle")

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")

    val pluginProject = project(":compiler-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val runtimeProject = project(":runtime")
    buildConfigField(
        type = "String",
        name = "RUNTIME_LIBRARY_COORDINATES",
        expression = "\"${runtimeProject.group}:${runtimeProject.name}:${runtimeProject.version}\""
    )
}

gradlePlugin {
    plugins {
        create("komprehension") {
            id = rootProject.group.toString()
            displayName = "Komprehension"
            description = "for-comprehension for Kotlin"
            implementationClass = "com.jsoizo.komprehension.gradle.KomprehensionGradlePlugin"
        }
    }
}
