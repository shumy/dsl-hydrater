package hyd.dsl

import hyd.dsl.antlr.*
import hyd.dsl.antlr.HydraterDslParser.*

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.*

const val RULE = "root"

class Compiler(private val dsl: String): HydraterDslBaseListener() {
  private lateinit var grammar: Grammar

  private val errors = mutableListOf<String>()
  private val refs = mutableMapOf<String, ERef>()

  fun compile(): DslResult {
    try {
      val lexer = HydraterDslLexer(CharStreams.fromString(dsl))
      val tokens = CommonTokenStream(lexer)
      val parser = HydraterDslParser(tokens)
      val tree = parser.root()

      val walker = ParseTreeWalker()
      walker.walk(this, tree)
    } catch (ex: Exception) {
      errors.add(ex.message!!)
      return DslResult(Grammar("Grammar Errors!", emptyMap()), errors)
    }

    return DslResult(grammar, errors)
  }

  override fun visitErrorNode(error: ErrorNode) {
    errors.add(error.text)
  }

  override fun enterRoot(ctx: RootContext) {
    refs[RULE] = ERef(RULE, LazyRef(ctx.expr().process()))
    ctx.entity().forEach {
      val ref = findRuleRef(it.NAME().text!!)
      ref.lRef.expr = it.expr().process()
    }

    val rules = refs.map {
      val expr = it.value.lRef.expr ?: throw DslException("Rule '${it.key}' not found!")
      it.key to expr
    }.toMap()

    val grammarName = ctx.identity().processIdentity()
    grammar = Grammar(grammarName, rules)
  }

  private fun ExprContext.process(): Expression {
    if (expr().size == 1) {
      val next = expr().last().process()
      val multiplicity = multiplicity().processMultiplicity()
      return EBound(next, multiplicity)
    }

    if (oper != null) {
      if (oper.text == "&")
        return EAnd(left.process(), right.process())

      if (oper.text == "|")
        return EOr(left.process(), right.process())
    }

    if (single() != null) {
      if (single().token != null)
        return EToken(single().token.text.extract())
      
      if (single().ref != null)
        return findRuleRef(single().ref.text)
    }

    if (map() != null) {
      val key = map().key.text
      val assign = map().assign()

      if (assign.aExist() != null)
        return EMapValue(key, assign.aExist().TEXT().text.extract(), isExist = true)
      
      if (assign.aText() != null)
        return EMapValue(key, assign.aText().TEXT().text.extract(), isExist = false)
      
      if (assign.aRef() != null)
        return EMapRef(key, findRuleRef(assign.aRef().NAME().text))
      
      if (assign.aType() != null) {
        val vType = assign.aType().type()
        return EMapType(key, vType.value.text.extractType(), vType.checker?.let { it.processIdentity() })
      }
    }

    throw NotImplementedError("A dsl branch is not implemented! - processExpr()")
  }

  private fun IdentityContext.processIdentity(): String {
    val namespace = ID().joinToString(separator = ".") { it.text }
    val name = NAME().text
    return "$namespace.$name"
  }
  
  private fun MultiplicityContext?.processMultiplicity() : EMultiplicity {
    return this?.let {
      val type = it.value.text.extractMultiplicity()
      val splitter = it.splitter?.let { it.text.extract() } ?: ","
      EMultiplicity(type, splitter)
    } ?: EMultiplicity(MultiplicityType.ONE)
  }

  private fun findRuleRef(name: String): ERef {
    return refs.getOrPut(name) { ERef(name, LazyRef()) }
  }
}

data class DslResult(val grammar: Grammar, val errors: List<String>)

class DslException(val msg: String) : Exception(msg)

/* ------------------------- helpers -------------------------*/
private fun String.extract(): String = substring(1, this.length - 1)

private fun String.extractType(): ValueType = when {
  this == "bool" -> ValueType.BOOL
  this == "text" -> ValueType.TEXT
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