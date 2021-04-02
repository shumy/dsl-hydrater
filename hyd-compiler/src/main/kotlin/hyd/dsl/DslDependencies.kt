package hyd.dsl

import kotlin.reflect.KClass

data class DslDependencies(
  val checkers: Map<String, KClass<out DslChecker<*>>> = emptyMap()
)

interface DslChecker<T> {
  fun check(value: T)
}