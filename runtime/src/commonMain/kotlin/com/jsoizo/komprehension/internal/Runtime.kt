package com.jsoizo.komprehension.internal

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Emitted by the Komprehension compiler plugin. Not part of the supported API.",
)
public annotation class KomprehensionInternalApi

// These exist instead of the stdlib equivalents so that the IR transformer can resolve each one from a
// CallableId with no overload disambiguation: stdlib's `flatMap` alone has several shapes, and picking
// the right one by signature would break whenever the stdlib changes.

@KomprehensionInternalApi
public fun <T> fromIterable(source: Iterable<T>?): List<T> = source?.toList() ?: emptyList()

@KomprehensionInternalApi
public fun <T> fromSequence(source: Sequence<T>?): List<T> = source?.toList() ?: emptyList()

@KomprehensionInternalApi
public fun <T> fromArray(source: Array<out T>?): List<T> = source?.toList() ?: emptyList()

@KomprehensionInternalApi
public fun <K, V> fromMap(source: Map<K, V>?): List<Pair<K, V>> =
    source?.map { it.key to it.value } ?: emptyList()

@KomprehensionInternalApi
public fun <T : Any> fromNullable(source: T?): List<T> = if (source == null) emptyList() else listOf(source)

@KomprehensionInternalApi
public fun <T, R> flatMap(source: List<T>, transform: (T) -> List<R>): List<R> {
    val result = ArrayList<R>(source.size)
    for (element in source) {
        result.addAll(transform(element))
    }
    return result
}

@KomprehensionInternalApi
public fun <R> pure(value: R): List<R> = listOf(value)

@KomprehensionInternalApi
public fun <R> empty(): List<R> = emptyList()
