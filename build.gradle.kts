import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.node.gradle) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "com.jsoizo.komprehension"
    version = "0.1.0-SNAPSHOT"

    // Without this the bytecode level follows the build JDK, so the same version of an artifact
    // fails to load on consumers with an older JDK. 1.8 matches kotlin-stdlib and the Kotlin
    // Gradle plugin, which are what these artifacts sit next to.
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
    plugins.withType<JavaBasePlugin> {
        extensions.getByType(JavaPluginExtension::class.java).apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
}
