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
  data class EBound(val next: Expression, val multiplicity: EMultiplicity?): Expression()

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

    data class EMapType(override val key: String, val type: ValueType, val checker: KClass<out DslChecker<*>>? = null): EMap(key)

    data class EMapRef(override val key: String, val ref: ERef): EMap(key)

data class EMultiplicity(val type: MultiplicityType, val splitter: String? = ",")

data class LazyRef(var expr: Expression? = null)