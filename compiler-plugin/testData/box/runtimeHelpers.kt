// Locks in the semantics of the helpers that generated code calls.
@file:OptIn(KomprehensionInternalApi::class)

import com.jsoizo.komprehension.internal.KomprehensionInternalApi
import com.jsoizo.komprehension.internal.empty
import com.jsoizo.komprehension.internal.flatMap
import com.jsoizo.komprehension.internal.fromArray
import com.jsoizo.komprehension.internal.fromIterable
import com.jsoizo.komprehension.internal.fromMap
import com.jsoizo.komprehension.internal.fromNullable
import com.jsoizo.komprehension.internal.fromSequence
import com.jsoizo.komprehension.internal.pure

fun box(): String {
    // Order and full traversal matter: a generator must yield every element, left to right.
    if (fromIterable(listOf(1, 2, 3)) != listOf(1, 2, 3)) return "Fail: fromIterable"
    if (fromSequence(sequenceOf("a", "b")) != listOf("a", "b")) return "Fail: fromSequence"
    if (fromArray(arrayOf(1, 2, 3)) != listOf(1, 2, 3)) return "Fail: fromArray"
    if (fromMap(mapOf("a" to 1, "b" to 2)) != listOf("a" to 1, "b" to 2)) return "Fail: fromMap"
    if (fromNullable(7) != listOf(7)) return "Fail: fromNullable"

    // A null source contributes no elements. This is the contract the nullable overloads exist for.
    val nullIterable: List<Int>? = null
    val nullSequence: Sequence<Int>? = null
    val nullArray: Array<Int>? = null
    val nullMap: Map<String, Int>? = null
    if (fromIterable(nullIterable) != emptyList<Int>()) return "Fail: fromIterable(null)"
    if (fromSequence(nullSequence) != emptyList<Int>()) return "Fail: fromSequence(null)"
    if (fromArray(nullArray) != emptyList<Int>()) return "Fail: fromArray(null)"
    if (fromMap(nullMap) != emptyList<Pair<String, Int>>()) return "Fail: fromMap(null)"
    if (fromNullable<Int>(null) != emptyList<Int>()) return "Fail: fromNullable(null)"

    if (pure(1) != listOf(1)) return "Fail: pure"
    if (empty<Int>() != emptyList<Int>()) return "Fail: empty"

    // Every outer element must contribute, and the inner results must concatenate in order.
    val concatenated = flatMap(fromIterable(listOf(1, 2))) { a -> fromIterable(listOf(a, a * 10)) }
    if (concatenated != listOf(1, 10, 2, 20)) return "Fail: flatMap order: $concatenated"

    // The shape the transformer emits: a generator, then a guard, then pure/empty.
    val guarded = flatMap(fromIterable(listOf(1, 2, 3))) { a ->
        flatMap(fromNullable(a * 10)) { b ->
            if (b >= 20) pure(a to b) else empty()
        }
    }
    if (guarded != listOf(2 to 20, 3 to 30)) return "Fail: guarded: $guarded"

    return "OK"
}
