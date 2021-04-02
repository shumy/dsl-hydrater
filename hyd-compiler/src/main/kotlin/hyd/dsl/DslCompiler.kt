package hyd.dsl

import hyd.dsl.internal.InternalCompiler

class DslCompiler(val deps: DslDependencies = DslDependencies()) {
  fun compile(dsl: String): DslResult = InternalCompiler(dsl, deps).compile()
}

data class DslResult(val grammar: Grammar, val errors: List<DslException>)

data class DslException(val line: Int, val pos: Int, val msg: String) : Exception(msg)