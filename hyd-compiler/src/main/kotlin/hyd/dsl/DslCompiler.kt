package hyd.dsl

import hyd.dsl.internal.InternalCompiler

class DslCompiler(val deps: DslDependencies = DslDependencies()) {
  fun compile(dsl: String): DslResult = InternalCompiler(dsl, deps).compile()
}

data class DslResult(val grammar: Grammar, val errors: List<String>)

class DslException(val msg: String) : Exception(msg)