// Locks in which `from` / `bind` overload each source type resolves to.
// A regression here silently changes what a generator binds, so it must fail loudly.

import com.jsoizo.komprehension.comprehend

fun iterables(xs: List<Int>, maybe: List<Int>?, nested: List<List<Int>>) {
    comprehend { val v: Int = from(xs); v }
    comprehend { val v: Int = from(maybe); v }
    comprehend { val v: List<Int> = from(nested); v }
    comprehend { val v: Int = xs.bind(); v }
    comprehend { val v: Int = maybe.bind(); v }
}

fun sequences(s: Sequence<Int>, maybe: Sequence<Int>?) {
    comprehend { val v: Int = from(s); v }
    comprehend { val v: Int = from(maybe); v }
    comprehend { val v: Int = s.bind(); v }
}

fun arrays(a: Array<String>, maybe: Array<String>?) {
    comprehend { val v: String = from(a); v }
    comprehend { val v: String = from(maybe); v }
    comprehend { val v: String = a.bind(); v }
}

fun maps(m: Map<String, Int>, maybe: Map<String, Int>?) {
    comprehend { val v: Pair<String, Int> = from(m); v }
    comprehend { val v: Pair<String, Int> = from(maybe); v }
    comprehend { val (k, n) = from(m); k to n }
    comprehend { val v: Pair<String, Int> = m.bind(); v }
}

fun nullables(n: Int?, s: String) {
    comprehend { val v: Int = from(n); v }
    comprehend { val v: Int = n.bind(); v }
    // String is not Iterable in Kotlin, so it binds as a plain value.
    comprehend { val v: String = from(s); v }
}

fun resultType(scores: Map<String, Map<String, Int>>) {
    val result: List<Pair<String, Int>> = comprehend {
        val (name, subjects) = from(scores)
        val eng = subjects["English"].bind()
        val math = from(subjects["Math"])
        where(eng >= 70)
        name to (eng + math)
    }
    result.size
}
