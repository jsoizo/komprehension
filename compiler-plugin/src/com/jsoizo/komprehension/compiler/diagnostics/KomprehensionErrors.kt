package com.jsoizo.komprehension.compiler.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
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
    private fun factory0(name: String) = KtDiagnosticFactory0(
        name = name,
        severity = Severity.ERROR,
        defaultPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        psiType = Any::class,
        rendererFactory = KomprehensionErrorMessages,
    )

    val GENERATOR_IN_UNSUPPORTED_POSITION: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_GENERATOR_IN_UNSUPPORTED_POSITION")

    val WHERE_IN_UNSUPPORTED_POSITION: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_WHERE_IN_UNSUPPORTED_POSITION")

    val RETURN_NOT_ALLOWED_IN_COMPREHENSION: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_RETURN_NOT_ALLOWED")

    val COMPREHENSION_SCOPE_ESCAPES: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_SCOPE_ESCAPES")

    val COMPREHEND_BLOCK_MUST_BE_LAMBDA: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_BLOCK_MUST_BE_LAMBDA")

    val COMPREHEND_MUST_BE_CALLED_DIRECTLY: KtDiagnosticFactory0 =
        factory0("KOMPREHENSION_MUST_BE_CALLED_DIRECTLY")

    val UNSUPPORTED_GENERATOR_SOURCE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        name = "KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE",
        severity = Severity.ERROR,
        defaultPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        psiType = Any::class,
        rendererFactory = KomprehensionErrorMessages,
    )

    // Reached only when the tree does not match what Fir2Ir is expected to produce. Users should never
    // see it; if they do, the shape of IR changed and the transformer needs updating.
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
                KomprehensionErrors.GENERATOR_IN_UNSUPPORTED_POSITION,
                "A generator ('from' / 'bind') may only appear as the initializer of a local 'val' " +
                    "or as a standalone statement directly inside 'comprehend'. Extract it into a 'val' first.",
            )
            map.put(
                KomprehensionErrors.WHERE_IN_UNSUPPORTED_POSITION,
                "'where' may only appear as a standalone statement directly inside 'comprehend'.",
            )
            map.put(
                KomprehensionErrors.RETURN_NOT_ALLOWED_IN_COMPREHENSION,
                "'return' is not allowed inside 'comprehend'. The last expression of the block is its result.",
            )
            map.put(
                KomprehensionErrors.COMPREHENSION_SCOPE_ESCAPES,
                "The 'comprehend' scope cannot be used as a value.",
            )
            map.put(
                KomprehensionErrors.COMPREHEND_BLOCK_MUST_BE_LAMBDA,
                "'comprehend' requires a lambda written at the call site.",
            )
            map.put(
                KomprehensionErrors.COMPREHEND_MUST_BE_CALLED_DIRECTLY,
                "'comprehend' cannot be used as a function reference.",
            )
            map.put(
                KomprehensionErrors.UNSUPPORTED_GENERATOR_SOURCE,
                "''{0}'' is not supported as a generator source. Convert it first, for example with ''.asIterable()''.",
                CommonRenderers.STRING,
            )
            map.put(
                KomprehensionErrors.ILLEGAL_COMPREHENSION_SHAPE,
                "Cannot desugar this ''comprehend'' block: {0}",
                CommonRenderers.STRING,
            )
            map.put(KomprehensionErrors.RUNTIME_NOT_FOUND, "{0}")
        }
}
