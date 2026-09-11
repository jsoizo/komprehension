package com.jsoizo.komprehension.sample

import com.jsoizo.komprehension.comprehend

private val scores: Map<String, Map<String, Int>> = mapOf(
    "taro" to mapOf("English" to 80, "Math" to 90),
    "hana" to mapOf("English" to 60, "Math" to 100),
    "jiro" to mapOf("English" to 75),
)

fun main() {
    // Note there is no dependency on the runtime in build.gradle.kts: the Gradle plugin adds it.
    val report = comprehend {
        val (name, subjects) = from(scores)
        val english = subjects["English"].bind()
        val math = from(subjects["Math"])

        where(english >= 70)

        name to (english + math)
    }
    println(report)

    // jiro has no Math score, so `from` on a null drops the whole candidate rather than throwing.
    check(report == listOf("taro" to 170)) { "unexpected: $report" }
}
