package com.jsoizo.komprehension.compiler.runners

import com.jsoizo.komprehension.compiler.services.configurePlugin
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor.SuppressionChecker
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.DEFAULT_UNUSED_DIAGNOSTICS
import org.jetbrains.kotlin.test.configuration.commonBackendHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineStepsWithoutCompilationErrorHandlers
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.NonSourceErrorMessagesHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.PsiLightTreeMetaInfoProcessor
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind

// A copy of AbstractFirPhasedDiagnosticTest without NoIrCompilationErrorsHandler, which throws on the
// first IR error and so cannot coexist with testData that expects one. The JS side already omits it.
open class AbstractJvmDiagnosticTest :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
            LANGUAGE + "+EnableDfaWarningsInK2"
            DIAGNOSTICS with DEFAULT_UNUSED_DIAGNOSTICS.map { "-$it" }

            +FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
            +JvmEnvironmentConfigurationDirectives.FULL_JDK

            +CodegenTestDirectives.IGNORE_DEXING // Avoids loading R8 from the classpath.
        }

        setupJvmPipelineStepsWithoutCompilationErrorHandlers(FirParser.LightTree)
        configureCommonDiagnosticTestPaths()

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::TagsGeneratorChecker, ::NoFirCompilationErrorsHandler)
        }

        configureIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
        }

        configureJvmArtifactsHandlersStep {
            commonBackendHandlersForCodegenTest(includeNoCompilationErrorsHandler = false)
        }

        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)
        useAfterAnalysisCheckers(::NonSourceErrorMessagesHandler)
        useFailureSuppressors(::PhasedPipelineChecker, ::FirFailingTestSuppressor)
        enableMetaInfoHandler()
        useAdditionalService<SuppressionChecker>(::SuppressionChecker.bind(null, null))

        configurePlugin()
    }
}
