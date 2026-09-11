@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jsoizo.komprehension.compiler.ir

import com.jsoizo.komprehension.compiler.KomprehensionNames
import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal sealed interface SymbolResolution {
    data class Resolved(val symbols: KomprehensionSymbols) : SymbolResolution

    /** [what] names the declaration that could not be resolved, so the error says which one. */
    data class Missing(val what: String) : SymbolResolution
}

internal class KomprehensionSymbols private constructor(
    val comprehend: IrSimpleFunctionSymbol,
    val flatMap: IrSimpleFunctionSymbol,
    val pure: IrSimpleFunctionSymbol,
    val empty: IrSimpleFunctionSymbol,
    val fromNullable: IrSimpleFunctionSymbol,
    val fromOverloads: Set<IrSimpleFunctionSymbol>,
    val bindOverloads: Set<IrSimpleFunctionSymbol>,
    val whereOverloads: Set<IrSimpleFunctionSymbol>,
    private val helperBySourceClass: Map<IrClassSymbol?, IrSimpleFunctionSymbol>,
) {
    fun helperFor(sourceType: IrType): IrSimpleFunctionSymbol? = helperBySourceClass[sourceType.classOrNull]

    companion object {
        fun resolve(ctx: IrPluginContext): SymbolResolution {
            val finder: DeclarationFinder = ctx.finderForBuiltins()

            fun one(id: CallableId): IrSimpleFunctionSymbol? = finder.findFunctions(id).singleOrNull()

            fun scopeMembers(name: Name): Set<IrSimpleFunctionSymbol> =
                finder.findFunctions(CallableId(KomprehensionNames.COMPREHENSION_SCOPE, name)).toSet()

            val comprehend = one(KomprehensionNames.COMPREHEND)
                ?: return SymbolResolution.Missing("com.jsoizo.komprehension.comprehend")
            val flatMap = one(KomprehensionNames.FLAT_MAP)
                ?: return SymbolResolution.Missing("internal.flatMap")
            val pure = one(KomprehensionNames.PURE)
                ?: return SymbolResolution.Missing("internal.pure")
            val empty = one(KomprehensionNames.EMPTY)
                ?: return SymbolResolution.Missing("internal.empty")

            val helperIds = listOf(
                KomprehensionNames.FROM_ITERABLE,
                KomprehensionNames.FROM_SEQUENCE,
                KomprehensionNames.FROM_ARRAY,
                KomprehensionNames.FROM_MAP,
                KomprehensionNames.FROM_NULLABLE,
            )
            val helperBySourceClass = HashMap<IrClassSymbol?, IrSimpleFunctionSymbol>()
            var fromNullable: IrSimpleFunctionSymbol? = null
            for (id in helperIds) {
                val helper = one(id) ?: return SymbolResolution.Missing("internal.${id.callableName}")
                val sourceType = helper.sourceParameterType()
                    ?: return SymbolResolution.Missing("the source parameter of internal.${id.callableName}")
                // classOrNull is null when the parameter is a type parameter. That is not a failure:
                // it is the key fromNullable is meant to occupy, and it is what makes from(T?) find it.
                val key = sourceType.classOrNull
                if (key == null) fromNullable = helper
                val clash = helperBySourceClass.put(key, helper)
                if (clash != null) {
                    return SymbolResolution.Missing(
                        "distinct source types for the runtime helpers (${id.callableName} collides)",
                    )
                }
            }

            val from = scopeMembers(KomprehensionNames.FROM)
            if (from.isEmpty()) return SymbolResolution.Missing("ComprehensionScope.from")
            val bind = scopeMembers(KomprehensionNames.BIND)
            if (bind.isEmpty()) return SymbolResolution.Missing("ComprehensionScope.bind")
            val where = scopeMembers(KomprehensionNames.WHERE)
            if (where.isEmpty()) return SymbolResolution.Missing("ComprehensionScope.where")

            val nullableHelper = fromNullable
                ?: return SymbolResolution.Missing("internal.fromNullable by its source type")

            return SymbolResolution.Resolved(
                KomprehensionSymbols(
                    comprehend, flatMap, pure, empty, nullableHelper, from, bind, where, helperBySourceClass,
                ),
            )
        }

        private fun IrSimpleFunctionSymbol.sourceParameterType(): IrType? =
            owner.parameters.singleOrNull { it.kind == IrParameterKind.Regular }?.type
    }
}
