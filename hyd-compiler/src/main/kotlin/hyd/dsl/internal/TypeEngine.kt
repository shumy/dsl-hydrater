package hyd.dsl.internal

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

import hyd.dsl.*

internal object TypeEngine {
  private val BOOL = typeOf<Boolean>()
  private val TEXT = typeOf<String>()
  private val INT = typeOf<Long>()
  private val FLOAT = typeOf<Double>()

  private val TIME = typeOf<LocalTime>()
  private val DATE = typeOf<LocalDate>()
  private val DATETIME = typeOf<LocalDateTime>()

  private val REF = typeOf<Entity>()

  fun convert(type: KType): DataType<*>? {
    if (type.isSubtypeOf(BOOL)) return DataType.TEXT
    if (type.isSubtypeOf(TEXT)) return DataType.TEXT
    if (type.isSubtypeOf(INT)) return DataType.INT
    if (type.isSubtypeOf(FLOAT)) return DataType.FLOAT

    if (type.isSubtypeOf(TIME)) return DataType.TIME
    if (type.isSubtypeOf(DATE)) return DataType.DATE
    if (type.isSubtypeOf(DATETIME)) return DataType.DATETIME

    if (type.isSubtypeOf(REF)) return DataType.REF

    return null
  }

  fun extract(type: String): DataType<*> = when (type) {
    "bool" -> DataType.BOOL
    "text" -> DataType.TEXT
    "int" -> DataType.INT
    "float" -> DataType.FLOAT
    "date" -> DataType.DATE
    "time" -> DataType.TIME
    "datetime" -> DataType.DATETIME
    "ref" -> DataType.REF
    else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
  }

  fun typeOf(end: EndExpression): DataType<*> = when (end) {
    is EToken -> DataType.BOOL
    is ERef -> DataType.EMBEDDED
    is EType -> end.type
    is EEnum -> typeOf(end.values.first())
  }
}