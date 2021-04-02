import hyd.dsl.*

class StartWithUpperCase: DslChecker<String> {
  override fun check(value: String) = throw NotImplementedError("Just a test!")
}

val dependencies = DslDependencies(
  checkers = mapOf("StartWithUpperCase" to StartWithUpperCase::class)
)

val compiler = DslCompiler(dependencies)

fun String.check(vararg rules: Pair<String, Expression>) {
  val result = compiler.compile(this)
  println(result.grammar.rules)

  assert(result.errors.isEmpty())
  assert(result.grammar.name == "test.Grammar")
  assert(result.grammar.rules == rules.toMap())
}

fun String.expect(vararg errors: String) {
  val result = compiler.compile(this)
  println(result.errors)
  
  assert(errors.toList() == result.errors)
}