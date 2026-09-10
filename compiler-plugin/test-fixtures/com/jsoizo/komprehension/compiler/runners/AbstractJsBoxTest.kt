package com.jsoizo.komprehension.compiler.runners

import com.jsoizo.komprehension.compiler.services.configurePlugin
import org.jetbrains.kotlin.js.test.runners.AbstractJsTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractJsBoxTest : AbstractJsTest(
    pathToTestDir = "compiler-plugin/testData/box",
    testGroupOutputDirPrefix = "box/",
    parser = FirParser.LightTree,
) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        // No IR dump here: the JS pipeline binds its dump handler to DUMP_IR_AFTER_INLINE, so
        // DUMP_IR would be silently ignored. IR review happens through the JVM box tests.
        with(builder) {
            configurePlugin()
        }
    }
}
