package hyd.dsl

import kotlin.reflect.KClass
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

enum class MultiplicityType {
  OPTIONAL, ONE, PLUS, MANY
}

data class Grammar(val name: String, val rules: Map<String, ERule>)

data class ERule(val expr: Expression, val checkers: Map<String, EChecker> = emptyMap())

data class EChecker(val key: String, val checkers: List<ICheck>)

sealed class Expression()
  data class EBound(val next: Expression, val multiplicity: EMultiplicity): Expression()

  data class EAnd(val left: Expression, val right: Expression): Expression()

  data class EOr(val left: Expression, val right: Expression): Expression()
  
  data class EToken(val value: String): Expression()
  
  class ERef(val name: String, internal val lRef: LazyRef): Expression() {
    constructor(name: String, rule: ERule) : this(name, LazyRef(0, 0, rule))

    val rule: ERule
      get() = lRef.rule!!
    
    override fun equals(other: Any?) = (other is ERef)
      && name == other.name
      && rule == other.rule
    
    override fun toString() = "ERef($name)"
  }

  sealed class EMap(open val key: String, open val type: DataType<*>): Expression()

    data class EMapExist(override val key: String, val value: String): EMap(key, DataType.BOOL)

    data class EMapType(override val key: String, override val type: DataType<*>, val multiplicity: EMultiplicity): EMap(key, type)

    data class EMapRef(override val key: String, val ref: ERef, val multiplicity: EMultiplicity): EMap(key, DataType.REF)

data class EMultiplicity(val type: MultiplicityType, val splitter: String? = null)

sealed class DataType<T: Any>(val type: KClass<T>) {
  object BOOL: DataType<Boolean>(Boolean::class)
  object TEXT: DataType<String>(String::class)
  object INT: DataType<Long>(Long::class)
  object FLOAT: DataType<Double>(Double::class)
  object DATE: DataType<LocalDate>(LocalDate::class)
  object TIME: DataType<LocalTime>(LocalTime::class)
  object DATETIME: DataType<LocalDateTime>(LocalDateTime::class)
  object REF: DataType<Entity>(Entity::class)
}

class LazyRef(val line: Int, val pos: Int, internal var rule: ERule? = null)


fun Grammar.format(): String {
  val mRules = rules.map{ it.formatRules() }.joinToString("")
  return """Grammar($name)
  |$mRules
  |""".trimMargin()
}

fun Map.Entry<String, ERule>.formatRules(): String {
  val mCheckers = value.checkers.map{ it.value.format() }.joinToString("")
  return """
  |
  |  $mCheckers$key: ${value.expr.format()} ;
  |""".trimMargin()
}

fun EChecker.format(): String {
  val mCheckers = checkers.map{ it::class.simpleName }.joinToString(",")
  return "${key}@[$mCheckers]\n  "
}

fun Expression.format(): String = when (this) {
  is EBound -> "(${next.format()})${multiplicity.format()}"
  is EAnd -> "${left.format()} ${right.format()}"
  is EOr -> "${left.format()} | ${right.format()}"
  is EToken -> "'$value'"
  is ERef -> name
  is EMap -> format()
}

fun EMap.format(): String = when (this) {
  is EMapExist -> "$key -exist-> '$value'"
  is EMapType -> "$key -type-> ${type.format()}${multiplicity.format()}"
  is EMapRef -> "$key -ref-> ${ref.name}${multiplicity.format()}"
}

fun EMultiplicity.format(): String = when (type) {
  MultiplicityType.ONE -> ""
  MultiplicityType.OPTIONAL -> "?"
  MultiplicityType.MANY -> "*${splitter.format()}"
  MultiplicityType.PLUS -> "+${splitter.format()}"
}

fun DataType<*>.format(): String = when (this) {
  DataType.BOOL -> "bool"
  DataType.TEXT -> "text"
  DataType.INT -> "int"
  DataType.FLOAT -> "float"
  DataType.DATE -> "date"
  DataType.TIME -> "time"
  DataType.DATETIME -> "datetime"
  DataType.REF -> "ref"
}

fun String?.format(): String = if (this == null) "" else " '$this'"