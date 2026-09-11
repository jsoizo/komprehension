// RUN_PIPELINE_TILL: BACKEND
// Every shape the argument can take other than a lambda literal written at the call site. The call is
// rejected before its block is scanned, so each one must report exactly one diagnostic.
import com.jsoizo.komprehension.ComprehensionScope
import com.jsoizo.komprehension.comprehend

fun storedInVariable(): List<Int> {
    val block: ComprehensionScope.() -> Int = { 1 }
    return <!KOMPREHENSION_BLOCK_MUST_BE_LAMBDA!>comprehend(block)<!>
}

// An anonymous function arrives as the same IR node as a lambda; only its origin separates the two.
// The generator in its body must stay unreported: the block never reaches validation.
fun anonymousFunction(): List<Int> =
    <!KOMPREHENSION_BLOCK_MUST_BE_LAMBDA!>comprehend(fun ComprehensionScope.(): Int { return from(listOf(1)) })<!>

fun makeBlock(): ComprehensionScope.() -> Int = { 1 }

fun returnedFromFactory(): List<Int> = <!KOMPREHENSION_BLOCK_MUST_BE_LAMBDA!>comprehend(makeBlock())<!>

fun ComprehensionScope.constantBlock(): Int = 1

// The reference is to 'constantBlock', not to 'comprehend', so MUST_BE_CALLED_DIRECTLY does not apply.
fun functionReference(): List<Int> =
    <!KOMPREHENSION_BLOCK_MUST_BE_LAMBDA!>comprehend(ComprehensionScope::constantBlock)<!>
