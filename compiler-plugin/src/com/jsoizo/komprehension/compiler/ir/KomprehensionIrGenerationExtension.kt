package com.jsoizo.komprehension.compiler.ir

import com.jsoizo.komprehension.compiler.diagnostics.KomprehensionErrors
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class KomprehensionIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val reporter = pluginContext.diagnosticReporter

        val symbols = when (val resolution = KomprehensionSymbols.resolve(pluginContext)) {
            is SymbolResolution.Resolved -> resolution.symbols
            is SymbolResolution.Missing -> {
                reporter.report(
                    KomprehensionErrors.RUNTIME_NOT_FOUND,
                    "The Komprehension runtime is not usable from this module: " +
                        "could not resolve ${resolution.what}.",
                )
                return
            }
        }

        // Per file, so each transformer holds the IrFile its diagnostics need to be anchored to.
        for (irFile in moduleFragment.files) {
            irFile.transformChildrenVoid(ComprehendTransformer(pluginContext, symbols, irFile, reporter))
        }
    }
}
