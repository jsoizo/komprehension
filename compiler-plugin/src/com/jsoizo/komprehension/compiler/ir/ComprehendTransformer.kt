@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jsoizo.komprehension.compiler.ir

import com.jsoizo.komprehension.compiler.diagnostics.KomprehensionErrors
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class ComprehendTransformer(
    private val ctx: IrPluginContext,
    private val symbols: KomprehensionSymbols,
    private val irFile: IrFile,
    private val reporter: IrDiagnosticReporter,
) : IrElementTransformerVoid() {

    private class Ctx(
        /** The `R` of `comprehend<R>`. */
        val resultElementType: IrType,
        /** `List<R>` — the type of every flatMap / pure / empty / when this call produces. */
        val listResultType: IrType,
        /** Only used to recognise which `IrReturn`s belong to this block. */
        val originalLambda: IrSimpleFunction,
    )

    override fun visitCall(expression: IrCall): IrExpression {
        // Children first, so a nested comprehend is already folded when this one moves statements
        // into new lambdas. transformChildrenVoid here means super.visitCall must NOT be called:
        // it would walk the subtree a second time.
        expression.transformChildrenVoid(this)

        if (expression.symbol != symbols.comprehend) return expression

        val lambda = (expression.arguments.lastOrNull() as? IrFunctionExpression)?.function
        if (lambda == null) {
            report(expression, "the block argument is not a lambda written at the call site")
            return expression
        }
        val body = lambda.body
        if (body !is IrBlockBody) {
            report(expression, "the block has no block body")
            return expression
        }
        val resultElementType = expression.typeArguments.getOrNull(0)
        if (resultElementType == null) {
            report(expression, "the result type argument is missing")
            return expression
        }

        val statements = body.statements
        // Derived rather than taken from expression.type: for an intersection R the call's own type is
        // an approximation, and the two would disagree.
        val c = Ctx(resultElementType, ctx.irBuiltIns.listClass.typeWith(resultElementType), lambda)

        // Validate before touching anything: a failure discovered mid-rewrite would leave the tree
        // half-transformed, and returning the original expression would then hand the backend a
        // broken subtree with no diagnostic attached to it.
        if (!validateShape(lambda, statements, c)) return expression

        // The original lambda leaves the tree, so the outermost level belongs to its parent.
        val rebuilt = build(statements, 0, lambda.parent, c) ?: return expression
        return rebuilt.patchDeclarationParents(lambda.parent)
    }

    /**
     * Folds `stmts[index until size]` into a single `List<R>`-typed expression.
     *
     * [parent] owns the statements at this level: the enclosing declaration at the outermost level,
     * and the lambda generated by [buildFlatMap] at every level below.
     */
    private fun build(
        stmts: List<IrStatement>,
        index: Int,
        parent: IrDeclarationParent,
        c: Ctx,
    ): IrExpression? {
        val prefix = ArrayList<IrStatement>()
        var i = index

        while (i < stmts.size) {
            val stmt = stmts[i]

            val terminatorValue = terminatorValueOrNull(stmt, c)
            if (terminatorValue != null) {
                // `comprehend { ...; from(ys) }` binds ys and yields what it bound, exactly as
                // `val y = from(ys); y` would.
                val terminalCall = generatorCallOrNull(terminatorValue)
                if (terminalCall != null) {
                    val flatMap = buildFlatMap(stmts, i, Generator(null, terminalCall), true, parent, c)
                        ?: return null
                    return withPrefix(prefix, flatMap, c)
                }
                return withPrefix(prefix, pureCall(terminatorValue, c), c)
            }

            val guard = guardConditionOrNull(stmt)
            if (guard != null) {
                val rest = build(stmts, i + 1, parent, c) ?: return null
                return withPrefix(prefix, ifThenElse(guard, rest, emptyCall(c), c), c)
            }

            val generator = generatorOrNull(stmt)
            if (generator != null) {
                val flatMap = buildFlatMap(stmts, i, generator, false, parent, c) ?: return null
                return withPrefix(prefix, flatMap, c)
            }

            prefix.add(stmt)
            i++
        }

        // Fir2Ir emits no terminal IrReturn for a lambda whose return type is Unit, so a Unit-valued
        // block simply runs out of statements. Synthesize the value it would have returned.
        if (c.resultElementType.isUnit()) {
            return withPrefix(prefix, pureCall(unitValue(), c), c)
        }

        report(c.originalLambda, "no result expression was found")
        return null
    }

    private fun unitValue(): IrExpression =
        IrGetObjectValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, ctx.irBuiltIns.unitType, ctx.irBuiltIns.unitClass)

    /** `flatMap(<helper>(source)) { p0 -> <rest> }`. */
    private fun buildFlatMap(
        stmts: List<IrStatement>,
        index: Int,
        generator: Generator,
        /** True when the generator is itself the result expression, so it yields what it binds. */
        yieldsBoundValue: Boolean,
        parent: IrDeclarationParent,
        c: Ctx,
    ): IrExpression? {
        val elementType = generator.call.type
        val sourceList = sourceListCall(generator, c) ?: return null

        val lambda = ctx.irFactory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            returnType = c.listResultType
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
        }
        lambda.parent = parent
        val p0 = lambda.addValueParameter(Name.identifier("p0"), elementType)

        // irReturn takes its target from the builder's scope, so the builder must be created with
        // this lambda's symbol.
        val builder = DeclarationIrBuilder(ctx, lambda.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)

        val rest = if (yieldsBoundValue) {
            pureCall(builder.irGet(p0), c)
        } else {
            build(stmts, index + 1, lambda, c) ?: return null
        }

        val boundVar: IrVariable? = generator.variable
        if (boundVar != null) {
            // Keeping the same IrVariable, and only swapping its initializer, is what makes every
            // existing reference to it survive the move into this lambda.
            boundVar.initializer = builder.irGet(p0)
        }
        lambda.body = builder.irBlockBody {
            if (boundVar != null) +boundVar
            +irReturn(rest)
        }

        val lambdaExpr = IrFunctionExpressionImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = ctx.irBuiltIns.functionN(1).typeWith(elementType, c.listResultType),
            function = lambda,
            origin = IrStatementOrigin.LAMBDA,
        )

        val callee = symbols.flatMap.owner
        val returnType = callee.returnType.substitute(callee.typeParameters, listOf(elementType, c.resultElementType))
        return IrCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbols.flatMap).apply {
            typeArguments[0] = elementType
            typeArguments[1] = c.resultElementType
            arguments[0] = sourceList
            arguments[1] = lambdaExpr
        }
    }

    /** `from` carries its source in its only regular parameter, `bind` in the extension receiver. */
    private fun sourceParamOf(generator: Generator): IrValueParameter? {
        val callee = generator.call.symbol.owner
        return callee.parameters.singleOrNull { it.kind == IrParameterKind.Regular }
            ?: callee.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
    }

    private fun sourceListCall(generator: Generator, c: Ctx): IrExpression? {
        val sourceParam = sourceParamOf(generator)
        if (sourceParam == null) {
            report(generator.call, "the generator has neither a 'source' parameter nor an extension receiver")
            return null
        }
        val sourceExpr = generator.call.arguments[sourceParam]
        if (sourceExpr == null) {
            report(generator.call, "the generator's source argument is absent")
            return null
        }

        val helper = symbols.helperFor(sourceParam.type)
        if (helper == null) {
            report(generator.call, "no runtime helper matches this generator overload")
            return null
        }

        val typeArgs = ArrayList<IrType>(generator.call.typeArguments.size)
        for (typeArg in generator.call.typeArguments) {
            if (typeArg == null) {
                report(generator.call, "an unresolved type argument")
                return null
            }
            typeArgs.add(typeArg)
        }
        val helperOwner = helper.owner
        if (typeArgs.size != helperOwner.typeParameters.size) {
            report(generator.call, "type argument count mismatch against the runtime helper")
            return null
        }

        val returnType = helperOwner.returnType.substitute(helperOwner.typeParameters, typeArgs)
        return IrCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, helper).apply {
            typeArgs.forEachIndexed { n, t -> typeArguments[n] = t }
            arguments[0] = sourceExpr
        }
    }

    private fun pureCall(value: IrExpression, c: Ctx): IrExpression {
        val callee = symbols.pure.owner
        val returnType = callee.returnType.substitute(callee.typeParameters, listOf(c.resultElementType))
        return IrCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbols.pure).apply {
            typeArguments[0] = c.resultElementType
            arguments[0] = value
        }
    }

    private fun emptyCall(c: Ctx): IrExpression {
        val callee = symbols.empty.owner
        val returnType = callee.returnType.substitute(callee.typeParameters, listOf(c.resultElementType))
        return IrCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbols.empty).apply {
            typeArguments[0] = c.resultElementType
        }
    }

    private fun withPrefix(prefix: List<IrStatement>, tail: IrExpression, c: Ctx): IrExpression =
        if (prefix.isEmpty()) {
            tail
        } else {
            IrBlockImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, c.listResultType, null, prefix + tail)
        }

    /**
     * Rejects every shape the rewrite cannot express, without mutating anything.
     *
     * FIR cannot take this over: its this-receiver checker returns early on implicit receivers, which
     * is exactly what `println(from(xs))` and `where(from(xs) > 0)` produce.
     */
    private fun validateShape(lambda: IrSimpleFunction, stmts: List<IrStatement>, c: Ctx): Boolean {
        val scopeSymbol: IrValueSymbol? =
            lambda.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.symbol
        var clean = true
        var sawTerminator = false

        for (stmt in stmts) {
            // Scan only what survives the rewrite. The consumed call nodes and their dispatch
            // receivers are discarded, so they must not be counted as violations.
            val terminator = terminatorValueOrNull(stmt, c)
            if (terminator != null) {
                sawTerminator = true
                // A generator in result position is consumed, so scan its source rather than itself.
                val terminalCall = generatorCallOrNull(terminator)
                val scanned = if (terminalCall != null) {
                    sourceParamOf(Generator(null, terminalCall))?.let { terminalCall.arguments[it] }
                } else {
                    terminator
                }
                if (scanned == null) {
                    report(terminator, "the generator has no resolvable source argument")
                    clean = false
                } else if (!scanUserCode(scanned, scopeSymbol, c)) {
                    clean = false
                }
                continue
            }
            val guard = guardConditionOrNull(stmt)
            if (guard != null) {
                if (!scanUserCode(guard, scopeSymbol, c)) clean = false
                continue
            }
            val generator = generatorOrNull(stmt)
            if (generator != null) {
                val source = sourceParamOf(generator)?.let { generator.call.arguments[it] }
                if (source == null) {
                    report(generator.call, "the generator has no resolvable source argument")
                    clean = false
                } else if (!scanUserCode(source, scopeSymbol, c)) {
                    clean = false
                }
                continue
            }
            if (!scanUserCode(stmt, scopeSymbol, c)) clean = false
        }

        // See build(): a Unit-valued block legitimately has no terminal IrReturn.
        if (!sawTerminator && !c.resultElementType.isUnit()) {
            report(lambda, "the block has no result expression")
            clean = false
        }
        return clean
    }

    private fun scanUserCode(subtree: IrElement, scopeSymbol: IrValueSymbol?, c: Ctx): Boolean {
        var clean = true
        subtree.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                if (scopeSymbol != null && expression.symbol == scopeSymbol) {
                    report(expression, "the 'comprehend' scope cannot be used as a value")
                    clean = false
                }
            }

            override fun visitCall(expression: IrCall) {
                val isDslCall = isGeneratorCall(expression) || expression.symbol in symbols.whereOverloads
                // Matching on the callee alone would also flag the generators of an inner comprehend that
                // failed validation and stayed in the tree, reporting valid code as misplaced.
                if (isDslCall && dispatchedOn(expression, scopeSymbol)) {
                    report(
                        expression,
                        "'from', 'bind' and 'where' are only allowed as top-level statements of the block",
                    )
                    clean = false
                    // The receiver belongs to the call just rejected; visiting it would add a second,
                    // untrue error saying the scope escaped as a value.
                    visitArgumentsExceptDispatch(expression, this)
                    return
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitReturn(expression: IrReturn) {
                // A `return@comprehend` nested in an if/when is only reachable here.
                if (expression.returnTargetSymbol == c.originalLambda.symbol) {
                    report(expression, "'return' out of a 'comprehend' block is not supported")
                    clean = false
                }
                expression.acceptChildrenVoid(this)
            }
        })
        return clean
    }

    private fun dispatchedOn(call: IrCall, scopeSymbol: IrValueSymbol?): Boolean {
        if (scopeSymbol == null) return false
        val callee = call.symbol.owner
        val index = callee.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
        if (index < 0) return false
        return (call.arguments[index] as? IrGetValue)?.symbol == scopeSymbol
    }

    private fun visitArgumentsExceptDispatch(call: IrCall, visitor: IrVisitorVoid) {
        call.symbol.owner.parameters.forEachIndexed { index, param ->
            if (param.kind != IrParameterKind.DispatchReceiver) call.arguments[index]?.acceptVoid(visitor)
        }
    }

    private class Generator(val variable: IrVariable?, val call: IrCall)

    /**
     * A generator used as a bare statement is wrapped in IMPLICIT_COERCION_TO_UNIT because it has a
     * value; `where` is not, because it already returns Unit. Both are accepted here.
     */
    private fun unwrapCoercion(statement: IrStatement): IrStatement =
        (statement as? IrTypeOperatorCall)
            ?.takeIf { it.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT }
            ?.argument
            ?: statement

    private fun generatorOrNull(statement: IrStatement): Generator? {
        val bare = unwrapCoercion(statement)
        if (bare is IrVariable) {
            val initializer = bare.initializer as? IrCall ?: return null
            return if (isGeneratorCall(initializer)) Generator(bare, initializer) else null
        }
        val call = bare as? IrCall ?: return null
        return if (isGeneratorCall(call)) Generator(null, call) else null
    }

    private fun generatorCallOrNull(expression: IrExpression): IrCall? =
        (expression as? IrCall)?.takeIf { isGeneratorCall(it) }

    private fun isGeneratorCall(call: IrCall): Boolean =
        call.symbol in symbols.fromOverloads || call.symbol in symbols.bindOverloads

    private fun guardConditionOrNull(statement: IrStatement): IrExpression? {
        val call = unwrapCoercion(statement) as? IrCall ?: return null
        if (call.symbol !in symbols.whereOverloads) return null
        val param = call.symbol.owner.parameters.firstOrNull { it.kind == IrParameterKind.Regular } ?: return null
        return call.arguments[param]
    }

    private fun terminatorValueOrNull(statement: IrStatement, c: Ctx): IrExpression? {
        val ret = statement as? IrReturn ?: return null
        if (ret.returnTargetSymbol != c.originalLambda.symbol) return null
        return ret.value
    }

    /** The else branch needs an explicit `true` condition; IrWhen has no implicit one. */
    private fun ifThenElse(
        condition: IrExpression,
        thenPart: IrExpression,
        elsePart: IrExpression,
        c: Ctx,
    ): IrExpression = IrWhenImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = c.listResultType,
        origin = IrStatementOrigin.IF,
        branches = listOf(
            IrBranchImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, condition, thenPart),
            IrElseBranchImpl(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                IrConstImpl.constTrue(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, ctx.irBuiltIns.booleanType),
                elsePart,
            ),
        ),
    )

    private fun report(at: IrElement, reason: String) {
        reporter.at(at, irFile).report(KomprehensionErrors.ILLEGAL_COMPREHENSION_SHAPE, reason)
    }

}
