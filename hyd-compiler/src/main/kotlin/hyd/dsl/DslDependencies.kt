package hyd.dsl

import kotlin.reflect.KClass

data class DslDependencies(
  val checkers: Map<String, ICheck> = emptyMap()
)

interface ICheck

interface ICheckEntity: ICheck {
  fun Entity.check()
}

interface ICheckValue<T: Any, D: DataType<T>>: ICheck {
  fun EValue<T>.check()
}