// RUN_PIPELINE_TILL: BACKEND
// validateShape scans only what survives the rewrite, so each case puts the receiver in a different one
// of the five subtrees it reaches: a plain statement, a generator source, a guard, the result, and the
// source of a generator that is itself the result.
import com.jsoizo.komprehension.comprehend

// The plainest form: a statement that is neither a generator nor 'where', so it is scanned whole.
fun scopeStoredInVal(): Int = comprehend {
    val self = <!KOMPREHENSION_SCOPE_ESCAPES!>this<!>
    val x = from(listOf(1))
    x
}.size

// The generator sits where it belongs; the escape hides in the source argument, scanned on its own.
fun scopeInGeneratorSource(): Int = comprehend {
    val scope = from(listOf(<!KOMPREHENSION_SCOPE_ESCAPES!>this<!>))
    val n = from(listOf(1))
    n
}.size

// 'where' is legal here, so the receiver is only reachable through the scan of its condition.
fun scopeInWhereCondition(): Int = comprehend {
    val x = from(listOf(1, 2))
    where(<!KOMPREHENSION_SCOPE_ESCAPES!>this<!>.hashCode() != x)
    x
}.size

// The result expression is scanned as itself, because it is not a generator call.
fun scopeAsResult(): Int {
    val scopes = comprehend {
        val x = from(listOf(1, 2))
        where(x > 1)
        <!KOMPREHENSION_SCOPE_ESCAPES!>this<!>
    }
    return scopes.size
}

// A generator in result position is consumed like any other, so only its source is left to scan.
fun scopeInTerminalGeneratorSource(): Int = comprehend {
    val n = from(listOf(1, 2))
    from(listOf(<!KOMPREHENSION_SCOPE_ESCAPES!>this<!>))
}.size
