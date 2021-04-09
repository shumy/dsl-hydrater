package hyd.dsl

data class Entity(val parent: Entity, val rule: ERule, val values: Map<String, EValue<*>>) {
  fun exist(key: String): Boolean = values[key]?.type != null
  
  @Suppress("UNCHECKED_CAST")
  fun <T: Any> get(type: DataType<T>, key: String): EValue<T> {
    val value = values[key] ?: throw NoSuchElementException()
    if (value.type != type) throw NoSuchElementException()
    return value as EValue<T>
  }
}

data class EValue<T: Any>(val parent: Entity, val map: EMap, val multiplicity: MultiplicityType, val values: List<T>, internal val type: DataType<T>) {
  val exist: Boolean
    get() = !values.isEmpty()

  val value: T
    get() = values.first()
}