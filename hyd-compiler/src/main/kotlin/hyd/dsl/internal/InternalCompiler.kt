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
  private val refs = mutableMapOf<String, LazyRef>()

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
      val expr = it.value.rule ?: throw DslException(it.value.line, it.value.pos, "Rule '${it.key}' not found!")
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
    ref.rule = ERule(expr, checkers)
  }

  private fun ExprContext.process(): Expression {
    if (expr().size == 1)
      return EBound(expr().last().process(), multiplicity().process())

    if (left != null) {
      if (oper == null)
        return EAnd(left.process(), right.process())

      if (oper.text == "|")
        return EOr(left.process(), right.process())
    }

    if (end() != null)
      return end().process()

    if (map() != null)
      return map().process()

    throw NotImplementedError("A dsl branch is not implemented! - ExprContext.process()")
  }

  private fun MapContext.process(): EMap {
    val key = key.text // TODO: check if already exists
    val value = assign()
    val multiplicity = multiplicity().process()
    
    if (value.or() != null) {
      val head = value.or().end().first().process()
      val all = value.or().end().map {
        val expr = it.process()
        if (expr::class != head::class)
          throw DslException(it.start.line, it.stop.charPositionInLine, "All enum values should be of the same type!")
        expr
      }

      val type = head.typeOf()
      return EMap(type, key, EEnum(all), multiplicity)
    }

    if (value.end() != null) {
      val end = value.end().process()
      val type = end.typeOf()
      return EMap(type, key, end, multiplicity)
    }

    throw NotImplementedError("A dsl branch is not implemented! - MapContext.process()")
  }

  private fun EndContext.process(): EndExpression {
    if (TEXT() != null)
      return EToken(TEXT().text.extract())
  
    if (NAME() != null)
      return ERef(NAME().text, findRuleRef(start.line, stop.charPositionInLine, NAME().text), embedded = true)

    if (type() != null)
      return EType(TypeEngine.extract(type().text))
    
    if (ref() != null)
      return ERef(ref().NAME().text, findRuleRef(start.line, stop.charPositionInLine, ref().NAME().text), embedded = false)
    
    throw NotImplementedError("A dsl branch is not implemented! - TokenContext.process()")
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

  private fun findRuleRef(line: Int, pos: Int, name: String): LazyRef {
    return refs.getOrPut(name) { LazyRef(line, pos) }
  }

  private fun findEntityChecker(line: Int, pos: Int, name: String): ICheckEntity =
    findChecker(ICheckEntity::class, line, pos, name)

  private fun findValueChecker(line: Int, pos: Int, name: String, dataType: DataType<*>): ICheckValue<*> {
    val checker = findChecker(ICheckValue::class, line, pos, name)
    val checkerType = checker::class.allSupertypes.first().arguments.first().type!!

    val checkerDataType = TypeEngine.convert(checkerType) ?: throw DslException(line, pos, "Checker '$name' with an unrecognized type '$checkerType'!")
    if (checkerDataType.type != dataType.type)
      throw DslException(line, pos, "Checker '$name' of type '${checkerDataType.type.simpleName}' is incompatible with dsl input '${dataType.type.simpleName}'!")

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

private fun String.extractMultiplicity(): MultiplicityType = when (this) {
  "?" -> MultiplicityType.OPTIONAL
  "+" -> MultiplicityType.PLUS
  "*" -> MultiplicityType.MANY
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.extractMultiplicity()")
}

private fun Expression.findKey(name: String): DataType<*>? = when (this) {
  is EBound -> next.findKey(name)
  is EAnd -> left.findKey(name) ?: right.findKey(name)
  is EOr -> left.findKey(name) ?: right.findKey(name)
  is EMap -> if (key == name) type else null
  else -> null
}

private fun EndExpression.typeOf(): DataType<*> = when (this) {
  is EToken -> DataType.BOOL
  is ERef -> DataType.REF
  is EType -> type
  is EEnum -> values.first().typeOf()
}