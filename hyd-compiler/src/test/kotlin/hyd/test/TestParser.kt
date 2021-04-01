import hyd.dsl.*
import org.junit.Test

fun String.check(vararg rules: Pair<String, Expression>) {
  val grammar = Compiler(this).compile()
  println(grammar.rules)

  assert(grammar.name == "test.Grammar")
  assert(grammar.rules == rules.toMap())
}

class TestParser {
  @Test fun testTokenExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: 'Hello World' ;
    """
    dsl.check(
      "root" to EToken("Hello World")
    )
  }

  @Test fun testOrExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: 'left' | 'right' ;
    """
    dsl.check(
      "root" to EOr(EToken("left"), EToken("right"))
    )
  }

  @Test fun testRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: Rule ;
      
      Rule: 'token' ; 
    """
    val Rule = EToken("token")
    dsl.check(
      "root" to ERef(Rule),
      "Rule" to Rule
    )
  }

  @Test fun testMapExistExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -exist-> 'text' ;
    """
    dsl.check(
      "root" to EMapValue("value", "text", isExist =  true)
    )
  }

  @Test fun testMapTextExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -text-> 'text' ;
    """
    dsl.check(
      "root" to EMapValue("value", "text", isExist =  false)
    )
  }

  @Test fun testMapRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -ref-> Rule ;

      Rule: 'token' ; 
    """
    val Rule = EToken("token")
    dsl.check(
      "root" to EMapRef("value", Rule),
      "Rule" to Rule
    )
  }

  @Test fun testMapTypeExpression() {
    val dsl = """
      grammar test.Grammar ;

      root:
          boolV -type-> bool
        | intV -type-> int
        | floatV -type-> float
        | dateV -type-> date
        | timeV -type-> time
        | datetimeV -type-> datetime
      ;
    """
    dsl.check(
      "root" to EOr(
        EOr(
          EOr(
            EOr(
              EOr(
                EMapType("boolV", ValueType.BOOL),
                EMapType("intV", ValueType.INT)
              ),
              EMapType("floatV", ValueType.FLOAT)
            ),
            EMapType("dateV", ValueType.DATE)
          ),
          EMapType("timeV", ValueType.TIME)
        ),
        EMapType("datetimeV", ValueType.DATETIME)
      )
    )
  }

  @Test fun testExprWithMultiplicityExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: ('set' & Rule)+ ;

      Rule: ('v1' | 'v2') ; 
    """
    val Rule = EBound(EOr(EToken("v1"), EToken("v2")), EMultiplicity(MultiplicityType.ONE, ","))
    dsl.check(
      "root" to EBound(
        EAnd(EToken("set"), ERef(Rule)),
        EMultiplicity(MultiplicityType.PLUS, ",")
      ),
      "Rule" to Rule
    )
  }
}