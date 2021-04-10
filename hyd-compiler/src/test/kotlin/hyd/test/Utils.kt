import hyd.dsl.*

class IsEntityValid: ICheckEntity {
  override fun Entity.check() = throw NotImplementedError("Just a test!")
}

class StartWithUpperCase: ICheckValue<String> {
  override fun EValue<String>.check() = throw NotImplementedError("Just a test!")
}

class UnrecognizedType: ICheckValue<Void> {
  override fun EValue<Void>.check() = throw NotImplementedError("Just a test!")
}

val isEntityValid = IsEntityValid()
val unrecognizedType = UnrecognizedType()
val startWithUpperCase = StartWithUpperCase()

val dependencies = DslDependencies(
  checkers = mapOf(
    "IsEntityValid" to isEntityValid,
    "UnrecognizedType" to unrecognizedType,
    "StartWithUpperCase" to startWithUpperCase
  )
)

val compiler = DslCompiler(dependencies)

fun String.check(vararg rules: Pair<String, ERule>) {
  val result = compiler.compile(this)
  println("Errors: ${result.errors}")
  println(result.grammar.rules)

  assert(result.errors.isEmpty())
  assert(result.grammar.name == "test.Grammar")
  assert(result.grammar.rules == rules.toMap())
}

fun String.expect(vararg errors: DslException) {
  val result = compiler.compile(this)
  println(result.errors)
  
  assert(errors.toList() == result.errors)
}