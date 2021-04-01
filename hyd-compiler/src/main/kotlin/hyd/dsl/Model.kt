package hyd.dsl

enum class ValueType {
  BOOL, INT, FLOAT, DATE, TIME, DATETIME
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
  
  data class ERef(private val lRef: LazyRef): Expression() {
    constructor(ref: Expression) : this(LazyRef(ref))

    val ref: Expression
      get() = lRef.ref!!
    
    override fun toString() = "ERef(ref=$ref)"
  }

  sealed class EMap(open val key: String): Expression()

    data class EMapValue(override val key: String, val value: String, val isExist: Boolean): EMap(key)

    data class EMapType(override val key: String, val type: ValueType): EMap(key)

    data class EMapRef(override val key: String, private val lRef: LazyRef): EMap(key) {
      constructor(key: String, ref: Expression) : this(key, LazyRef(ref))

      val ref: Expression
        get() = lRef.ref!!
      
      override fun toString() = "EMapRef(key=$key, ref=$ref)"
    }

data class EMultiplicity(val type: MultiplicityType, val splitter: String = ",")

data class LazyRef(var ref: Expression? = null)