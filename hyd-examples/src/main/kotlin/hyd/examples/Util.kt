package hyd.examples

import hyd.dsl.*

class StartWithUpperCase: ICheckValue<String> {
  override fun EValue<String>.check() {
    if (!value.first().isUpperCase())
      throw Exception("Name '$value' should start with UpperCase char!")
  }
}

val dependencies = DslDependencies(
  checkers = mapOf("StartWithUpperCase" to StartWithUpperCase())
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