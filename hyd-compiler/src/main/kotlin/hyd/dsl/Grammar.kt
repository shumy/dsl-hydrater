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
  data class EMap(val type: DataType<*>, val key: String, val value: EndExpression, val multiplicity: EMultiplicity): Expression()
  
  sealed class EndExpression(): Expression()
    data class EToken(val value: String): EndExpression()
    data class EType(val type: DataType<*>): EndExpression()
    data class EEnum(val values: List<EndExpression>): EndExpression()
    class ERef(val name: String, internal val lRef: LazyRef, val embedded: Boolean): EndExpression() {
      constructor(name: String, rule: ERule, embedded: Boolean) : this(name, LazyRef(0, 0, rule), embedded)

      val rule: ERule
        get() = lRef.rule!!
      
      override fun equals(other: Any?) = (other is ERef)
        && name == other.name
        && rule == other.rule
        && embedded == other.embedded
      
      override fun toString() = "ERef($name, $embedded)"
    }

data class EMultiplicity(val type: MultiplicityType, val splitter: String? = null)

sealed class DataType<T: Any>(val type: KClass<T>) {
  object ID: DataType<String>(String::class)
  object REF: DataType<Entity>(Entity::class)
  object BOOL: DataType<Boolean>(Boolean::class)
  object TEXT: DataType<String>(String::class)
  object INT: DataType<Long>(Long::class)
  object FLOAT: DataType<Double>(Double::class)
  object DATE: DataType<LocalDate>(LocalDate::class)
  object TIME: DataType<LocalTime>(LocalTime::class)
  object DATETIME: DataType<LocalDateTime>(LocalDateTime::class)
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
  is EMap -> format()
  is EToken -> "'$value'"
  is EType -> type.format()
  is ERef -> name
  is EEnum -> {
    val all = values.map{it.format()}.joinToString("|")
    "($all)"
  }
}

fun EMap.format(): String = "$key=${value.format()}${multiplicity.format()}"

fun EMultiplicity.format(): String = when (type) {
  MultiplicityType.ONE -> ""
  MultiplicityType.OPTIONAL -> "?"
  MultiplicityType.MANY -> "*${splitter.format()}"
  MultiplicityType.PLUS -> "+${splitter.format()}"
}

fun DataType<*>.format(): String = when (this) {
  DataType.ID -> "id"
  DataType.REF -> "ref"
  DataType.BOOL -> "bool"
  DataType.TEXT -> "text"
  DataType.INT -> "int"
  DataType.FLOAT -> "float"
  DataType.DATE -> "date"
  DataType.TIME -> "time"
  DataType.DATETIME -> "datetime"
}

fun String?.format(): String = if (this == null) "" else " #'$this'"