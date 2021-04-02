package hyd.dsl

import kotlin.reflect.KClass

enum class ValueType {
  BOOL, TEXT, INT, FLOAT, DATE, TIME, DATETIME
}

enum class MultiplicityType {
  OPTIONAL, ONE, PLUS, MANY
}

data class Grammar(val name: String, val rules: Map<String, Expression>)

sealed class Expression()
  data class EBound(val next: Expression, val multiplicity: EMultiplicity): Expression()

  data class EAnd(val left: Expression, val right: Expression): Expression()

  data class EOr(val left: Expression, val right: Expression): Expression()
  
  data class EToken(val value: String): Expression()
  
  data class ERef(val rule: String, internal val lRef: LazyRef): Expression() {
    constructor(rule: String, expr: Expression) : this(rule, LazyRef(expr))

    val expr: Expression
      get() = lRef.expr!!
    
    override fun toString() = "ERef($rule)"
  }

  sealed class EMap(open val key: String): Expression()

    data class EMapValue(override val key: String, val value: String, val isExist: Boolean): EMap(key)

    data class EMapType(override val key: String, val type: ValueType, val multiplicity: EMultiplicity, val checker: KClass<out DslChecker<*>>? = null): EMap(key)

    data class EMapRef(override val key: String, val ref: ERef, val multiplicity: EMultiplicity): EMap(key)

data class EMultiplicity(val type: MultiplicityType, val splitter: String? = null)

data class LazyRef(var expr: Expression? = null)


fun Grammar.format(): String {
  val mRules = rules.filter { it.key != "root" }.map{ it.format() }.joinToString("")
  return """Grammar($name)
  |  root: ${rules["root"]?.format()} ;
  |$mRules
  |""".trimMargin()
}

fun Map.Entry<String, Expression>.format(): String = """
  |
  |  $key: ${value.format()} ;
  |""".trimMargin()

fun Expression.format(): String = when (this) {
  is EBound -> "(${next.format()})${multiplicity.format()}"
  is EAnd -> "${left.format()} & ${right.format()}"
  is EOr -> "${left.format()} | ${right.format()}"
  is EToken -> "'$value'"
  is ERef -> rule
  is EMap -> this.format()
}

fun EMap.format(): String = when (this) {
  is EMapValue -> "$key -${if (isExist) "exist" else "text"}-> '$value'"
  is EMapType -> "$key -type-> ${type.format()}${checker.format()}${multiplicity.format()}"
  is EMapRef -> "$key -ref-> ${ref.rule}${multiplicity.format()}"
}

fun EMultiplicity.format(): String = when (type) {
  MultiplicityType.ONE -> ""
  MultiplicityType.OPTIONAL -> "? ${splitter?:""}"
  MultiplicityType.MANY -> "* ${splitter?:""}"
  MultiplicityType.PLUS -> "+ ${splitter?:""}"
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

fun KClass<out DslChecker<*>>?.format(): String {
  return if (this == null) "" else "@${this.simpleName}"
}