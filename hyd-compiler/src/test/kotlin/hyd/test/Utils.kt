import hyd.dsl.Compiler
import hyd.dsl.Expression

fun String.check(vararg rules: Pair<String, Expression>) {
  val result = Compiler(this).compile()
  println(result.grammar.rules)

  assert(result.errors.isEmpty())
  assert(result.grammar.name == "test.Grammar")
  assert(result.grammar.rules == rules.toMap())
}

fun String.expect(vararg errors: String) {
  val result = Compiler(this).compile()
  println(result.errors)
  
  assert(errors.toList() == result.errors)
}