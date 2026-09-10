package com.jsoizo.komprehension

@DslMarker
public annotation class ComprehensionDsl

/**
 * Receiver of a [comprehend] block.
 *
 * Inside a `comprehend` block the compiler plugin erases every member call. Anywhere else — including
 * a user-declared extension on this type — the members simply throw.
 */
@ComprehensionDsl
public class ComprehensionScope internal constructor() {
    // Sources are declared as nullable: a non-null overload cannot coexist with the nullable one
    // (identical JVM signature), and accepting null lets `null` mean "empty source" instead of
    // silently falling through to the `T?` overload and binding the container itself.
    public fun <T> from(source: Iterable<T>?): T = pluginNotApplied()

    public fun <T> from(source: Sequence<T>?): T = pluginNotApplied()

    public fun <T> from(source: Array<out T>?): T = pluginNotApplied()

    public fun <K, V> from(source: Map<K, V>?): Pair<K, V> = pluginNotApplied()

    public fun <T : Any> from(source: T?): T = pluginNotApplied()

    public fun <T> Iterable<T>?.bind(): T = pluginNotApplied()

    public fun <T> Sequence<T>?.bind(): T = pluginNotApplied()

    public fun <T> Array<out T>?.bind(): T = pluginNotApplied()

    public fun <K, V> Map<K, V>?.bind(): Pair<K, V> = pluginNotApplied()

    public fun <T : Any> T?.bind(): T = pluginNotApplied()

    public fun where(condition: Boolean): Unit = pluginNotApplied()
}

/**
 * Combines every generator in [block] into a single list, in the manner of a Scala for-comprehension.
 *
 * ```
 * val result = comprehend {
 *     val (name, subjects) = from(scores)
 *     val eng = subjects["English"].bind()
 *     where(eng >= 70)
 *     name to eng
 * }
 * ```
 *
 * Requires the Komprehension compiler plugin; without it every member of [ComprehensionScope] throws.
 */
public fun <R> comprehend(block: ComprehensionScope.() -> R): List<R> = pluginNotApplied()

private fun pluginNotApplied(): Nothing =
    throw IllegalStateException(
        "The Komprehension compiler plugin is not applied to this module. " +
            "Apply the 'com.jsoizo.komprehension' Gradle plugin.",
    )
