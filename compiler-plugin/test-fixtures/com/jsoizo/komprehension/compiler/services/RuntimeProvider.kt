package com.jsoizo.komprehension.compiler.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

fun TestConfigurationBuilder.configureRuntime() {
    useConfigurators(::RuntimeProvider)
    useCustomRuntimeClasspathProviders(::RuntimeClasspathProviderImpl)
}

private class RuntimeProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        when {
            platform.isJvm() -> {
                configuration.addJvmClasspathRoots(jvmRuntimeClasspath)
            }

            platform.isJs() -> {
                val libraries = configuration.getList(JSConfigurationKeys.LIBRARIES)
                configuration.put(
                    JSConfigurationKeys.LIBRARIES,
                    libraries + jsRuntimeClasspath.map { it.absolutePath },
                )
            }
        }
    }
}

private class RuntimeClasspathProviderImpl(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        val targetPlatform = module.targetPlatform(testServices)
        return when {
            targetPlatform.isJvm() -> jvmRuntimeClasspath
            targetPlatform.isJs() -> jsRuntimeClasspath
            else -> emptyList()
        }
    }
}

private val jvmRuntimeClasspath = classpathFiles("komprehensionRuntime.jvm.classpath")
private val jsRuntimeClasspath = classpathFiles("komprehensionRuntime.js.classpath")

private fun classpathFiles(property: String): List<File> {
    val value = System.getProperty(property)
        ?: error("Unable to get a valid classpath from '$property' property")
    return value.split(File.pathSeparator).map(::File)
}
