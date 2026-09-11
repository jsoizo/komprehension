// RUN_PIPELINE_TILL: BACKEND
// Every source below falls through to the `from(T?)` overload, which would bind the whole container
// as one element. The plugin must reject them instead of desugaring silently.
import com.jsoizo.komprehension.comprehend

// Primitive arrays are neither Iterable nor Array<out T>, so they reach `from(T?)`. A nullable source
// is the same case: `T?` absorbs the question mark and `T` is still the container.
fun primitiveArrays(nums: IntArray, maybeChars: CharArray?): Int = comprehend {
    val a = <!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>from(nums)<!>
    val b = <!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>from(maybeChars)<!>
    a.size + b.size
}.size

// The same nullable source through a type that is listed by a plain literal, so the `T?` case stays
// covered independently of the primitive arrays above.
fun nullableIterator(maybeIter: Iterator<Int>?): Int = comprehend {
    val a = <!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>from(maybeIter)<!>
    if (a.hasNext()) 1 else 0
}.size

// `bind` carries its source in the extension receiver rather than a regular parameter, so it reaches
// the same check through the other branch of `sourceParamOf`. An explicit receiver leaves the call
// node spanning only the selector, so the marker starts after the dot.
fun iteratorThroughBind(iter: Iterator<Int>): Int = comprehend {
    val bound = iter.<!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>bind()<!>
    if (bound.hasNext()) 1 else 0
}.size

// A generator in result position is consumed by the rewrite, so it is validated on its own path.
fun iteratorInResultPosition(entries: ListIterator<String>): Int = comprehend {
    <!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>from(entries)<!>
}.size

// A generator whose value is discarded arrives wrapped in IMPLICIT_COERCION_TO_UNIT. `Result` is the
// source users are most likely to expect to unwrap.
fun discardedResult(xs: List<Int>, res: Result<String>): Int = comprehend {
    val a = from(xs)
    <!KOMPREHENSION_UNSUPPORTED_GENERATOR_SOURCE!>from(res)<!>
    a
}.size

// String resolves to `from(T?)` as well but is deliberately off the list: binding the whole string is
// the expected behaviour. No diagnostic here.
fun stringIsNotRejected(s: String): Int = comprehend {
    val a = from(s)
    val b = s.bind()
    a.length + b.length
}.size
