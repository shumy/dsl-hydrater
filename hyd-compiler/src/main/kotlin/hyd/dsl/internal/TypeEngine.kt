package hyd.dsl.internal

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

import hyd.dsl.ValueType

object TypeEngine {
  private val BOOL = typeOf<Boolean>()
  private val TEXT = typeOf<String>()
  private val INT = typeOf<Long>()
  private val FLOAT = typeOf<Double>()

  private val TIME = typeOf<LocalTime>()
  private val DATE = typeOf<LocalDate>()
  private val DATETIME = typeOf<LocalDateTime>()

  fun convert(type: KType): ValueType {
    if (type.isSubtypeOf(BOOL)) return ValueType.TEXT
    if (type.isSubtypeOf(TEXT)) return ValueType.TEXT
    if (type.isSubtypeOf(INT)) return ValueType.INT
    if (type.isSubtypeOf(FLOAT)) return ValueType.FLOAT

    if (type.isSubtypeOf(TIME)) return ValueType.TIME
    if (type.isSubtypeOf(DATE)) return ValueType.DATE
    if (type.isSubtypeOf(DATETIME)) return ValueType.DATETIME

    throw NotImplementedError("A dsl branch is not implemented! - TypeEngine.convert()")
  }
}