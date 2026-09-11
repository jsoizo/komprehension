package com.jsoizo.komprehension.compiler.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

internal object KomprehensionErrors : KtDiagnosticsContainer() {
    // Declared through the public constructors rather than the error0/error1 delegates: those bake a
    // `reified P : PsiElement` into the class initializer, which breaks once the plugin jar is loaded
    // from a classloader without the IntelliJ classes. psiType stays Any::class because the reporter
    // asserts `factory.psiType.isInstance(element)` and anything narrower fails there.
    val ILLEGAL_COMPREHENSION_SHAPE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        name = "KOMPREHENSION_ILLEGAL_SHAPE",
        severity = Severity.ERROR,
        defaultPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        psiType = Any::class,
        rendererFactory = KomprehensionErrorMessages,
    )

    val RUNTIME_NOT_FOUND: KtSourcelessDiagnosticFactory = KtSourcelessDiagnosticFactory(
        name = "KOMPREHENSION_RUNTIME_NOT_FOUND",
        severity = Severity.ERROR,
        rendererFactory = KomprehensionErrorMessages,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KomprehensionErrorMessages
}

internal object KomprehensionErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by
        KtDiagnosticFactoryToRendererMap("KOMPREHENSION") { map ->
            map.put(
                KomprehensionErrors.ILLEGAL_COMPREHENSION_SHAPE,
                "Cannot desugar this ''comprehend'' block: {0}",
                CommonRenderers.STRING,
            )
            map.put(KomprehensionErrors.RUNTIME_NOT_FOUND, "{0}")
        }
}
