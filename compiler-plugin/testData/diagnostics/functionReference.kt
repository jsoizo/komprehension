// RUN_PIPELINE_TILL: BACKEND
// A reference produces no IrCall, so the rewrite never sees the block and the runtime stub would throw
// at run time. The transformer watches IrFunctionReference to turn that into a compile error.
import com.jsoizo.komprehension.ComprehensionScope
import com.jsoizo.komprehension.comprehend

fun storedInLocal(): List<Int> {
    val f: (ComprehensionScope.() -> Int) -> List<Int> = <!KOMPREHENSION_MUST_BE_CALLED_DIRECTLY!>::comprehend<!>
    return f { 1 }
}

fun passedAsArgument(): List<List<Int>> =
    listOf<ComprehensionScope.() -> Int>({ 1 }).map(<!KOMPREHENSION_MUST_BE_CALLED_DIRECTLY!>::comprehend<!>)
