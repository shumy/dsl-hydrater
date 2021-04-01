package hyd.dsl

enum class AssignType {
  EXIST, TEXT, REF, TYPE
}

enum class ValueType {
  BOOL, INT, FLOAT, DATE, TIME, DATETIME
}

data class Grammar(val name: String, val root: Expression, val entities: Map<String, Entity>)

sealed class Expression()
  data class EOr(val left: Expression, val right: Expression): Expression()
  data class EText(val value: String): Expression()
  data class ERef(val value: Entity): Expression()
  data class EMap(val key: String, val assign: AssignType, val value: String? = null, val ref: Entity? = null, val type: ValueType? = null): Expression()

data class Entity(val name: String, val expr: Expression)