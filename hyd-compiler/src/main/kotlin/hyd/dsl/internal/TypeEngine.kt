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

  private val ENTITY = typeOf<Entity>()

  fun convert(type: KType): DataType<*>? {
    if (type.isSubtypeOf(ENTITY)) return DataType.REF

    if (type.isSubtypeOf(BOOL)) return DataType.BOOL
    if (type.isSubtypeOf(TEXT)) return DataType.TEXT
    if (type.isSubtypeOf(INT)) return DataType.INT
    if (type.isSubtypeOf(FLOAT)) return DataType.FLOAT

    if (type.isSubtypeOf(TIME)) return DataType.TIME
    if (type.isSubtypeOf(DATE)) return DataType.DATE
    if (type.isSubtypeOf(DATETIME)) return DataType.DATETIME

    return null
  }

  fun extract(type: String): DataType<*> = when (type) {
    "id" -> DataType.ID
    "ref" -> DataType.REF
    "bool" -> DataType.BOOL
    "text" -> DataType.TEXT
    "int" -> DataType.INT
    "float" -> DataType.FLOAT
    "date" -> DataType.DATE
    "time" -> DataType.TIME
    "datetime" -> DataType.DATETIME
    else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
  }
}