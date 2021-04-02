package hyd.examples

import hyd.dsl.DslCompiler

val compiler = DslCompiler(dependencies)

fun main() {
  compiler.loadFileAndCompile("domain.dsl")
}