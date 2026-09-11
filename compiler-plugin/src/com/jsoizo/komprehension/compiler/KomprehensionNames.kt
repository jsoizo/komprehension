package com.jsoizo.komprehension.compiler

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object KomprehensionNames {
    val RUNTIME_PACKAGE: FqName = FqName("com.jsoizo.komprehension")
    val INTERNAL_PACKAGE: FqName = FqName("com.jsoizo.komprehension.internal")

    val COMPREHENSION_SCOPE: ClassId = ClassId(RUNTIME_PACKAGE, Name.identifier("ComprehensionScope"))

    val COMPREHEND: CallableId = CallableId(RUNTIME_PACKAGE, Name.identifier("comprehend"))

    val FROM: Name = Name.identifier("from")
    val BIND: Name = Name.identifier("bind")
    val WHERE: Name = Name.identifier("where")

    val FROM_ITERABLE: CallableId = internal("fromIterable")
    val FROM_SEQUENCE: CallableId = internal("fromSequence")
    val FROM_ARRAY: CallableId = internal("fromArray")
    val FROM_MAP: CallableId = internal("fromMap")
    val FROM_NULLABLE: CallableId = internal("fromNullable")
    val FLAT_MAP: CallableId = internal("flatMap")
    val PURE: CallableId = internal("pure")
    val EMPTY: CallableId = internal("empty")

    /**
     * Containers that resolve to `from(T?)` and would silently bind the whole container as one element.
     * Listed explicitly rather than detected structurally: `String` is also a container by some
     * definitions, yet binding the whole string is the behaviour people expect.
     */
    val UNSUPPORTED_GENERATOR_SOURCES: Set<ClassId> = buildSet {
        for (name in listOf("Int", "Long", "Short", "Byte", "Double", "Float", "Char", "Boolean")) {
            add(kotlin("${name}Array"))
        }
        add(kotlin("Result"))
        addAll(listOf("Iterator", "ListIterator").map { collections(it) })
        addAll(listOf("Stream", "IntStream", "LongStream", "DoubleStream").map { javaStream(it) })
        addAll(listOf("Optional", "OptionalInt", "OptionalLong", "OptionalDouble").map { javaUtil(it) })
        add(javaUtil("Enumeration"))
    }

    private fun internal(name: String): CallableId = CallableId(INTERNAL_PACKAGE, Name.identifier(name))

    private fun kotlin(name: String) = ClassId(FqName("kotlin"), Name.identifier(name))
    private fun collections(name: String) = ClassId(FqName("kotlin.collections"), Name.identifier(name))
    private fun javaUtil(name: String) = ClassId(FqName("java.util"), Name.identifier(name))
    private fun javaStream(name: String) = ClassId(FqName("java.util.stream"), Name.identifier(name))
}
