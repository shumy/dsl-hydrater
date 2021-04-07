package hyd.dsl

import kotlin.reflect.KClass

enum class ValueType {
  BOOL, TEXT, INT, FLOAT, DATE, TIME, DATETIME
}

enum class MultiplicityType {
  OPTIONAL, ONE, PLUS, MANY
}

data class Grammar(val name: String, val rules: Map<String, ERule>)

data class ERule(val expr: Expression, val checkers: Map<String, EChecker> = emptyMap())

data class EChecker(val key: String, val checkers: List<KClass<out DslChecker<*>>>)

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
    
    override fun toString() = "ERef($rule)"
  }

  sealed class EMap(open val key: String): Expression()

    data class EMapExist(override val key: String, val value: String): EMap(key)

    data class EMapType(override val key: String, val type: ValueType, val multiplicity: EMultiplicity): EMap(key)

    data class EMapRef(override val key: String, val ref: ERef, val multiplicity: EMultiplicity): EMap(key)

data class EMultiplicity(val type: MultiplicityType, val splitter: String? = null)

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
  val mCheckers = checkers.map{ it.simpleName }.joinToString(",")
  return "${key}@[$mCheckers]\n  "
}

fun Expression.format(): String = when (this) {
  is EBound -> "(${next.format()})${multiplicity.format()}"
  is EAnd -> "${left.format()} & ${right.format()}"
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

fun ValueType.format(): String = when (this) {
  ValueType.BOOL -> "bool"
  ValueType.TEXT -> "text"
  ValueType.INT -> "int"
  ValueType.FLOAT -> "float"
  ValueType.DATE -> "date"
  ValueType.TIME -> "time"
  ValueType.DATETIME -> "datatime"
}

fun String?.format(): String = if (this == null) "" else " '$this'"