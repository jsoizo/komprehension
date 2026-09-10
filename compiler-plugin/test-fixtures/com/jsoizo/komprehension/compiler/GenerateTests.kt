package com.jsoizo.komprehension.compiler

import com.jsoizo.komprehension.compiler.runners.AbstractJsBoxTest
import com.jsoizo.komprehension.compiler.runners.AbstractJsDiagnosticTest
import com.jsoizo.komprehension.compiler.runners.AbstractJvmBoxTest
import com.jsoizo.komprehension.compiler.runners.AbstractJvmDiagnosticTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testsRoot = args[0], testDataRoot = args[1]) {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractJsDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }
            testClass<AbstractJsBoxTest> {
                model("box")
            }
        }
    }
}
