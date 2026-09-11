// RUN_PIPELINE_TILL: BACKEND
// 'where' is a guard only as a statement written directly in the comprehend block. validateShape reaches
// the other positions through four different scans, so each one gets a case here.
import com.jsoizo.komprehension.comprehend

// A statement that is neither a generator, a guard nor the result is scanned whole.
fun whereInsideIf(xs: List<Int>): Int = comprehend {
    val a = from(xs)
    if (a > 0) <!KOMPREHENSION_WHERE_IN_UNSUPPORTED_POSITION!>where(a < 10)<!>
    a
}.size

// The statement is a well-formed generator, so only its source argument is scanned. The violation hides there.
fun whereInsideGeneratorSource(xs: List<Int>): Int = comprehend {
    val a = from(xs)
    val b = from(xs.filter { <!KOMPREHENSION_WHERE_IN_UNSUPPORTED_POSITION!>where(it > a)<!>; it > 0 })
    a + b
}.size

// The outer 'where' is correctly placed and must stay unreported: the guard branch scans only its condition.
fun whereInsideWhereCondition(xs: List<Int>): Int = comprehend {
    val a = from(xs)
    where(xs.any { <!KOMPREHENSION_WHERE_IN_UNSUPPORTED_POSITION!>where(it > a)<!>; it > 0 })
    a
}.size

// The result expression is scanned too, so a 'where' folded into it is not a way past the rule.
// The inner lambda must not take the scope as its own receiver: 'run { }' resolves to 'T.run' here,
// and validateShape then reports KOMPREHENSION_SCOPE_ESCAPES on that receiver instead.
fun whereInsideResultExpression(xs: List<Int>): Int = comprehend {
    val a = from(xs)
    a.let { <!KOMPREHENSION_WHERE_IN_UNSUPPORTED_POSITION!>where(it > 0)<!>; it }
}.size
