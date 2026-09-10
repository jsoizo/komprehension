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

    private fun internal(name: String): CallableId = CallableId(INTERNAL_PACKAGE, Name.identifier(name))
}
