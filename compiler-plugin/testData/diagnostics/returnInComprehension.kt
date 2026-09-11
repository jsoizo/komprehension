// RUN_PIPELINE_TILL: BACKEND
// A `return@comprehend` nested inside another expression is rejected, whatever it hides in: a plain
// statement, a generator's source, the result expression, an inline lambda. One written as a statement
// of the block itself is the block's result and stays legal.
import com.jsoizo.komprehension.comprehend

fun earlyReturnInIf(): List<Int> = comprehend {
    val a = from(listOf(1, 2, 3))
    if (a > 2) <!KOMPREHENSION_RETURN_NOT_ALLOWED!>return@comprehend 0<!>
    a * 10
}

// The elvis idiom puts the return inside a generator's source, which is scanned apart from the statement.
fun returnInGeneratorSource(maybe: List<Int>?): List<Int> = comprehend {
    val v = from(maybe ?: <!KOMPREHENSION_RETURN_NOT_ALLOWED!>return@comprehend 0<!>)
    v * 2
}

// The last statement is itself the block's return, so this one is reached only through its value.
fun returnInsideResultExpression(): List<String> = comprehend {
    val n = from(listOf(1, 2, 3))
    if (n == 2) <!KOMPREHENSION_RETURN_NOT_ALLOWED!>return@comprehend "two"<!> else "n=$n"
}

// Legal Kotlin, because `forEach` is inline. The scan has to descend into the nested lambda to see it.
fun nonLocalReturnFromInlineLambda(): List<Int> = comprehend {
    val n = from(listOf(1, 2, 3))
    listOf(10, 20).forEach { if (it % n == 0) <!KOMPREHENSION_RETURN_NOT_ALLOWED!>return@comprehend it<!> }
    n
}

// The boundary: an explicit return as the last statement is the block's result, not a violation.
fun explicitFinalReturnIsFine(): List<Int> = comprehend {
    val a = from(listOf(1, 2))
    return@comprehend a * 2
}

// A return that targets the inner lambda leaves only that lambda, so it is none of this block's business.
fun returnToInnerLambdaIsFine(): List<Int> = comprehend {
    val n = from(listOf(1, 2, 3))
    listOf(10, 20).forEach { if (it % n == 0) return@forEach }
    n
}
