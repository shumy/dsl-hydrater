package hyd.dsl.internal

import hyd.dsl.*
import hyd.dsl.antlr.*
import hyd.dsl.antlr.HydraterDslParser.*

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*

import java.util.*

import kotlin.reflect.KClass
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.allSupertypes

const val RULE = "root"

internal class InternalCompiler(private val dsl: String, private val deps: DslDependencies): HydraterDslBaseListener() {
  private lateinit var grammar: Grammar

  private val errorListener = ErrorListener()
  private val errors = mutableListOf<DslException>()
  private val refs = mutableMapOf<String, ERef>()

  internal fun compile(): DslResult {
    try {
      val lexer = HydraterDslLexer(CharStreams.fromString(dsl))
      lexer.addErrorListener(errorListener)
      
      val tokens = CommonTokenStream(lexer)

      val parser = HydraterDslParser(tokens)
      parser.addErrorListener(errorListener)

      val tree = parser.root()
      val walker = ParseTreeWalker()
      walker.walk(this, tree)
    } catch (ex: DslException) {
      errors.add(ex)
      return DslResult(Grammar("Grammar with errors!", emptyMap()), errors)
    }

    return DslResult(grammar, errors)
  }

  override fun visitErrorNode(error: ErrorNode) {
    errors.add(DslException(error.symbol.line, error.symbol.charPositionInLine, error.text))
  }

  override fun enterRoot(ctx: RootContext) {
    refs[RULE] = ERef(RULE, LazyRef(0, 0, ctx.expr().process()))
    ctx.entity().forEach {
      val ref = findRuleRef(0, 0, it.NAME().text!!)
      ref.lRef.expr = it.expr().process()
    }

    val rules = refs.map {
      val expr = it.value.lRef.expr ?: throw DslException(it.value.lRef.line, it.value.lRef.pos, "Rule '${it.key}' not found!")
      it.key to expr
    }.toMap()

    val grammarName = ctx.identity().processIdentity()
    grammar = Grammar(grammarName, rules)
  }

  private fun ExprContext.process(): Expression {
    if (expr().size == 1)
      return EBound(expr().last().process(), multiplicity().processMultiplicity())

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
        return findRuleRef(start.line, stop.charPositionInLine, single().ref.text)
    }

    if (map() != null) {
      val key = map().key.text
      val assign = map().assign()

      if (assign.aExist() != null)
        return EMapValue(key, assign.aExist().TEXT().text.extract(), isExist = true)
      
      if (assign.aText() != null)
        return EMapValue(key, assign.aText().TEXT().text.extract(), isExist = false)
      
      if (assign.aRef() != null) {
        val ref = findRuleRef(start.line, stop.charPositionInLine, assign.aRef().NAME().text)
        return EMapRef(key, ref, assign.aRef().multiplicity().processMultiplicity())
      }

      if (assign.aType() != null) {
        val vType = assign.aType().type()
        val type = vType.value.text.extractType()
        val multiplicity = assign.aType().multiplicity().processMultiplicity()
        val checker = vType.checker?.let { findChecker(start.line, stop.charPositionInLine, it.processIdentity(), type) }
        return EMapType(key, type, multiplicity, checker)
      }
    }

    throw NotImplementedError("A dsl branch is not implemented! - processExpr()")
  }

  private fun IdentityContext.processIdentity(): String {
    val namespace = ID().joinToString(separator = ".") { it.text }
    val name = NAME().text
    return if (namespace.isEmpty()) name else "$namespace.$name"
  }
  
  private fun MultiplicityContext?.processMultiplicity() : EMultiplicity {
    return this?.let {
      val type = it.value.text.extractMultiplicity()
      val splitter = it.splitter?.let { it.text.extract() }

      if (type == MultiplicityType.OPTIONAL && it.splitter != null)
        throw DslException(start.line, stop.charPositionInLine, "Optional multiplicity doesn't support splitter!")

      EMultiplicity(type, splitter)
    } ?: EMultiplicity(MultiplicityType.ONE)
  }

  private fun findRuleRef(line: Int, pos: Int, name: String): ERef {
    return refs.getOrPut(name) { ERef(name, LazyRef(line, pos)) }
  }

  private fun findChecker(line: Int, pos: Int, name: String, type: ValueType): KClass<out DslChecker<*>> {
    val checker = deps.checkers[name] ?: throw DslException(line, pos, "Checker '$name' not found!")
    val value = checker.allSupertypes.first().arguments.first().type!!
    
    val checkerType = TypeEngine.convert(value) ?: throw DslException(line, pos, "Checker '$name' with an unrecognized type '$value'!")
    if (checkerType != type)
      throw DslException(line, pos, "Checker '$name' of type '$checkerType' is incompatible with dsl input '$type'!")

    return checker
  }
}

/* ------------------------- helpers -------------------------*/
private fun String.extract(): String = substring(1, this.length - 1)

private fun String.extractType(): ValueType = when (this) {
  "bool" -> ValueType.BOOL
  "text" -> ValueType.TEXT
  "int" -> ValueType.INT
  "float" -> ValueType.FLOAT
  "date" -> ValueType.DATE
  "time" -> ValueType.TIME
  "datetime" -> ValueType.DATETIME
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
}

private fun String.extractMultiplicity(): MultiplicityType = when (this) {
  "?" -> MultiplicityType.OPTIONAL
  "+" -> MultiplicityType.PLUS
  "*" -> MultiplicityType.MANY
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.select()")
}