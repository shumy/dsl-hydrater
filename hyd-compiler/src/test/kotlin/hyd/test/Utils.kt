import hyd.dsl.*

class StartWithUpperCase: DslChecker<String> {
  override fun check(value: String) = throw NotImplementedError("Just a test!")
}

class UnrecognizedType: DslChecker<Void> {
  override fun check(value: Void) = throw NotImplementedError("Just a test!")
}


val dependencies = DslDependencies(
  checkers = mapOf("StartWithUpperCase" to StartWithUpperCase::class, "UnrecognizedType" to UnrecognizedType::class)
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