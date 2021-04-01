package hyd.dsl

import hyd.dsl.antlr.*
import hyd.dsl.antlr.HydraterDslParser.*

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.*

const val RULE = "root"

class DslException(val msg: String) : Exception(msg)

class Compiler(private val dsl: String): HydraterDslBaseListener() {
  private lateinit var grammar: Grammar

  private val errors = mutableListOf<String>()
  private val lazyRefs = mutableMapOf<String, LazyRef>()

  fun errors(): List<String> = errors

  fun compile(): Grammar {
    val lexer = HydraterDslLexer(CharStreams.fromString(dsl))
    val tokens = CommonTokenStream(lexer)
    val parser = HydraterDslParser(tokens)
    val tree = parser.root()

    val walker = ParseTreeWalker()
    walker.walk(this, tree)

    return grammar
  }

  override fun visitErrorNode(error: ErrorNode) {
    errors.add(error.text)
  }

  override fun enterRoot(ctx: RootContext) {
    val namespace = ctx.ID().joinToString(separator = ".") { it.text }
    val name = ctx.NAME().text

    lazyRefs[RULE] = LazyRef(ctx.expr().processExpr())
    ctx.entity().forEach {
      val lRef = lazyRefs.getOrPut(it.NAME().text!!) { LazyRef() }
      lRef.ref = it.expr().processExpr()
    }

    val rules = lazyRefs.map {
      val expr = it.value.ref ?: throw DslException("Rule '${it.key}' not found!")
      it.key to expr
    }.toMap()

    grammar = Grammar("$namespace.$name", rules)
  }

  private fun ExprContext.processExpr(): Expression {
    if (expr().size == 1) {
      val multiplicity = multiplicity()?.let {
        val type = it.value.text.extractMultiplicity()
        val splitter = it.splitter?.let { it.text } ?: ","
        EMultiplicity(type, splitter)
      }  ?: EMultiplicity(MultiplicityType.ONE)

      val next = expr().last().processExpr()
      return EBound(next, multiplicity)
    }

    if (oper != null) {
      if (oper.text == "&")
        return EAnd(left.processExpr(), right.processExpr())

      if (oper.text == "|")
        return EOr(left.processExpr(), right.processExpr())
    }

    if (single() != null) {
      if (single().token != null)
        return EToken(single().token.text.extract())
      
      if (single().ref != null)
        return ERef(findRule(single().ref.text))
    }

    if (map() != null) {
      val key = map().key.text
      val assign = map().assign()

      if (assign.aExist() != null)
        return EMapValue(key, assign.aExist().TEXT().text.extract(), isExist = true)
      
      if (assign.aText() != null)
        return EMapValue(key, assign.aText().TEXT().text.extract(), isExist = false)
      
      if (assign.aRef() != null)
        return EMapRef(key, findRule(assign.aRef().NAME().text))
      
      if (assign.aType() != null)
        return EMapType(key, assign.aType().type().text.extractType())
    }

    throw NotImplementedError("A dsl branch is not implemented! - processExpr()")
  }

  private fun findRule(name: String): LazyRef {
    return lazyRefs.getOrPut(name) { LazyRef() }
  }
}

/* ------------------------- helpers -------------------------*/
private fun String.extract(): String = substring(1, this.length - 1)

private fun String.extractType(): ValueType = when {
  this == "bool" -> ValueType.BOOL
  this == "int" -> ValueType.INT
  this == "float" -> ValueType.FLOAT
  this == "date" -> ValueType.DATE
  this == "time" -> ValueType.TIME
  this == "datetime" -> ValueType.DATETIME
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
}

private fun String.extractMultiplicity(): MultiplicityType = when {
  this == "?" -> MultiplicityType.OPTIONAL
  this == "+" -> MultiplicityType.PLUS
  this == "*" -> MultiplicityType.MANY
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.select()")
}