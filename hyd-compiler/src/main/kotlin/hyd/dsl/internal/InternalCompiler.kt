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
import kotlin.reflect.full.isSubclassOf

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
    // first pass to compile rules
    ctx.entity().forEach { it.process() }

    // second pass necessary to check lazy references
    val rules = refs.map {
      val expr = it.value.lRef.rule ?: throw DslException(it.value.lRef.line, it.value.lRef.pos, "Rule '${it.key}' not found!")
      it.key to expr
    }.toMap()

    val grammarName = ctx.identity().process()
    grammar = Grammar(grammarName, rules)
  }

  private fun EntityContext.process() {
    val name = NAME().text
    val ref = findRuleRef(start.line, stop.charPositionInLine, name)

    val expr = expr().process()

    //TODO: check for multiple checkers with the same key?
    val checkers = checker().map { it.process(expr) }.map { it.key to it }.toMap()
    ref.lRef.rule = ERule(expr, checkers)
  }

  private fun ExprContext.process(): Expression {
    if (expr().size == 1)
      return EBound(expr().last().process(), multiplicity().process())

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
        return EMapExist(key, assign.aExist().TEXT().text.extract())
      
      if (assign.aRef() != null) {
        val ref = findRuleRef(start.line, stop.charPositionInLine, assign.aRef().NAME().text)
        return EMapRef(key, ref, assign.aRef().multiplicity().process())
      }

      if (assign.aType() != null) {
        val vType = assign.aType().type()
        val type = vType.value.text.extractType()
        val multiplicity = assign.aType().multiplicity().process()
        return EMapType(key, type, multiplicity)
      }
    }

    throw NotImplementedError("A dsl branch is not implemented! - processExpr()")
  }

  private fun CheckerContext.process(expr: Expression): EChecker {
    val key = key.text
    val checkers = if (key == "this") {
      identity().map { findEntityChecker(it.start.line, it.stop.charPositionInLine, it.process()) }
    } else {
      val type = expr.findKey(key) ?: throw DslException(start.line, start.charPositionInLine, "Key '$key' not found in the rule expression!")
      identity().map { findValueChecker(it.start.line, it.stop.charPositionInLine, it.process(), type) }
    }

    return EChecker(key, checkers)
  }

  private fun IdentityContext.process(): String {
    val namespace = ID().joinToString(separator = ".") { it.text }
    val name = NAME().text
    return if (namespace.isEmpty()) name else "$namespace.$name"
  }
  
  private fun MultiplicityContext?.process() : EMultiplicity {
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

  private fun findEntityChecker(line: Int, pos: Int, name: String): ICheckEntity =
    findChecker(ICheckEntity::class, line, pos, name)

  private fun findValueChecker(line: Int, pos: Int, name: String, type: DataType<*>): ICheckValue<*, *> {
    val checker = findChecker(ICheckValue::class, line, pos, name)
    val value = checker::class.allSupertypes.first().arguments.first().type!!

    val checkerType =TypeEngine.convert(value) ?: throw DslException(line, pos, "Checker '$name' with an unrecognized type '$value'!")
    if (checkerType != type)
      throw DslException(line, pos, "Checker '$name' of type '${checkerType::class.simpleName}' is incompatible with dsl input '${type::class.simpleName}'!")

    return checker
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T: ICheck> findChecker(type: KClass<T>, line: Int, pos: Int, name: String): T {
    val checker = deps.checkers[name] ?: throw DslException(line, pos, "Checker '$name' not found!")
    if (!checker::class.isSubclassOf(type))
      throw DslException(line, pos, "Invalid checker '$name'. Expecting implementation of '${type.simpleName}'!")
    
      return checker as T
  }
}

/* ------------------------- helpers -------------------------*/
private fun String.extract(): String = substring(1, this.length - 1)

private fun String.extractType(): DataType<*> = when (this) {
  "bool" -> DataType.BOOL
  "text" -> DataType.TEXT
  "int" -> DataType.INT
  "float" -> DataType.FLOAT
  "date" -> DataType.DATE
  "time" -> DataType.TIME
  "datetime" -> DataType.DATETIME
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
}

private fun String.extractMultiplicity(): MultiplicityType = when (this) {
  "?" -> MultiplicityType.OPTIONAL
  "+" -> MultiplicityType.PLUS
  "*" -> MultiplicityType.MANY
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.select()")
}

private fun Expression.findKey(name: String): DataType<*>? = when (this) {
  is EBound -> next.findKey(name)
  is EAnd -> left.findKey(name) ?: right.findKey(name)
  is EOr -> left.findKey(name) ?: right.findKey(name)
  is EMap -> if (key == name) type else null
  is EToken -> null
  is ERef -> null
}