package hyd.examples

import hyd.dsl.*

class StartWithUpperCase: DslChecker<String> {
  override fun check(value: String) {
    if (!value.first().isUpperCase())
      throw DslException("Name '$value' should start with UpperCase char!")
  }
}

val dependencies = DslDependencies(
  checkers = mapOf("StartWithUpperCase" to StartWithUpperCase::class)
)

fun getResourceAsText(path: String): String {
  return StartWithUpperCase::class.java.classLoader.getResource(path).readText()
}

fun DslCompiler.loadFileAndCompile(file: String): Grammar {
  val dsl = getResourceAsText(file)
  val result = this.compile(dsl)
  println("---------- compiling file $file ----------")
  println("Errors: ${result.errors}")
  println(result.grammar.format())

  return result.grammar
}