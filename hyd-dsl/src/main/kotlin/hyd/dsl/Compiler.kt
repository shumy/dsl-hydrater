package hyd.dsl

import hyd.dsl.antlr.*
import hyd.dsl.antlr.HydraterDslParser.*

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class DslException(val msg: String) : Exception(msg)

class Compiler(private val dsl: String): HydraterDslBaseListener() {
  lateinit var grammar: Grammar
    internal set

  val errors = mutableListOf<String>()

  private val entities = mutableMapOf<String, Entity>()
  private val stack = Stack<String>()
  init { compile() }

  private fun <R: Any> scope(obj: String, scope: () -> R): R {
    stack.push(obj)
      val result = scope()
    stack.pop()
    return result
  }

  private fun compile() {
    val lexer = HydraterDslLexer(CharStreams.fromString(dsl))
    val tokens = CommonTokenStream(lexer)
    val parser = HydraterDslParser(tokens)
    val tree = parser.root()

    val walker = ParseTreeWalker()
    walker.walk(this, tree)
  }

  override fun visitErrorNode(error: ErrorNode) {
    errors.add(error.text)
  }

  override fun enterRoot(ctx: RootContext) {
    val namespace = ctx.ID().joinToString(separator = ".") { it.text }
    val name = ctx.NAME().text

    val root = ctx.expr().processExpr()
    grammar = Grammar("$namespace.$name", root, entities)
  }

  private fun ExprContext.processExpr(): Expression {
    if (left != null)
      return EOr(left.processExpr(), right.processExpr())

    if (single() != null) {
      if (single().token != null)
        return EText(single().token.text.extract())
      
      if (single().ref != null)
        return ERef(findEntity(single().ref.text))
    }

    if (map() != null) {
      val key = map().key.text
      val assign = map().assign()

      if (assign.aExist() != null)
        return EMap(key, AssignType.EXIST, value = assign.aExist().TEXT().text.extract())
      
      if (assign.aText() != null)
        return EMap(key, AssignType.TEXT, value = assign.aText().TEXT().text.extract())
      
      if (assign.aRef() != null)
        return EMap(key, AssignType.REF, ref = findEntity(assign.aRef().NAME().text))
      
      if (assign.aType() != null)
        return EMap(key, AssignType.REF, type = assign.aType().text.extract().type())
    }

    throw NotImplementedError("A dsl branch is not implemented! - processExpr()")
  }

  private fun findEntity(name: String): Entity {
    return entities.get(name) ?: throw DslException("Rule $name not found!")
  }
}

/* ------------------------- helpers -------------------------*/
private fun String.extract(): String = substring(1, this.length - 1)

private fun String.type(): ValueType = when {
  this == "bool" -> ValueType.BOOL
  this == "int" -> ValueType.INT
  this == "float" -> ValueType.FLOAT
  this == "date" -> ValueType.DATE
  this == "time" -> ValueType.TIME
  this == "datetime" -> ValueType.DATETIME
  else -> throw NotImplementedError("A dsl branch is not implemented! - String.type()")
}