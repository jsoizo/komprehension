// RUN_PIPELINE_TILL: BACKEND
// Both violations must be reported: a handler that stops at the first one would hide the second.
import com.jsoizo.komprehension.comprehend

fun twoSeparateViolations(): Int = comprehend {
    val p = listOf(1).map { <!KOMPREHENSION_GENERATOR_IN_UNSUPPORTED_POSITION!>from(listOf(2))<!> }
    val q = listOf(1).map { <!KOMPREHENSION_GENERATOR_IN_UNSUPPORTED_POSITION!>from(listOf(3))<!> }
    p.size + q.size
}.size

// The inner block fails validation and stays in the tree. Its own `val c = from(...)` sits where a
// generator belongs, so the outer block must not report it.
fun nestedFailureDoesNotCascade(): Int = comprehend {
    val a = from(listOf(1))
    val inner = comprehend {
        val c = from(listOf(7))
        val d = listOf(1).map { <!KOMPREHENSION_GENERATOR_IN_UNSUPPORTED_POSITION!>from(listOf(2))<!> }
        c + d.size
    }
    a + inner.size
}.size
