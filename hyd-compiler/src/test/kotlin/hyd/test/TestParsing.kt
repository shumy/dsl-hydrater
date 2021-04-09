import hyd.dsl.*
import org.junit.Test

class TestParsing {
  @Test fun testTokenExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: 'Hello World' ;
    """
    dsl.check(
      "Root" to ERule(EToken("Hello World"))
    )
  }

  @Test fun testOrExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: 'left' | 'right' ;
    """
    dsl.check(
      "Root" to ERule(EOr(EToken("left"), EToken("right")))
    )
  }

  @Test fun testAndExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: 'left' & 'right' ;
    """
    dsl.check(
      "Root" to ERule(EAnd(EToken("left"), EToken("right")))
    )
  }

  @Test fun testRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: Rule ;
      
      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(ERef("Rule", Rule)),
      "Rule" to Rule
    )
  }

  @Test fun testMapExistExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value -exist-> 'text' ;
    """
    dsl.check(
      "Root" to ERule(EMapExist("value", "text"))
    )
  }

  @Test fun testMapRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value -ref-> Rule ;

      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(EMapRef("value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.ONE))),
      "Rule" to Rule
    )
  }

  @Test fun testMapManyRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value -ref-> Rule* ;

      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(EMapRef("value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.MANY))),
      "Rule" to Rule
    )
  }

  @Test fun testCheckers() {
    val dsl = """
      grammar test.Grammar ;

      this@[IsEntityValid]
      value@[StartWithUpperCase]
      Root: value -type-> text* ;
    """
    dsl.check(
      "Root" to ERule(
        EMapType("value", DataType.TEXT, EMultiplicity(MultiplicityType.MANY)),
        mapOf(
          "value" to EChecker("value", listOf(startWithUpperCase)),
          "this" to EChecker("this", listOf(isEntityValid))
        )
      )
    )
  }

  @Test fun testAllMapTypeExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root:
          boolV -type-> bool
        | textV -type-> text
        | intV -type-> int
        | floatV -type-> float
        | dateV -type-> date
        | timeV -type-> time
        | datetimeV -type-> datetime
      ;
    """
    dsl.check(
      "Root" to ERule(EOr(
        EOr(
          EOr(
            EOr(
              EOr(
                EOr(
                  EMapType("boolV", DataType.BOOL, EMultiplicity(MultiplicityType.ONE)),
                  EMapType("textV", DataType.TEXT, EMultiplicity(MultiplicityType.ONE))
                ),
                EMapType("intV", DataType.INT, EMultiplicity(MultiplicityType.ONE))
              ),
              EMapType("floatV", DataType.FLOAT, EMultiplicity(MultiplicityType.ONE))
            ),
            EMapType("dateV", DataType.DATE, EMultiplicity(MultiplicityType.ONE))
          ),
          EMapType("timeV", DataType.TIME, EMultiplicity(MultiplicityType.ONE))
        ),
        EMapType("datetimeV", DataType.DATETIME, EMultiplicity(MultiplicityType.ONE))
      ))
    )
  }

  @Test fun testExprWithMultiplicityExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: ('set' & Rule)+';' ;

      Rule: ('v1' | 'v2') ;
    """
    val Rule = ERule(EBound(EOr(EToken("v1"), EToken("v2")), EMultiplicity(MultiplicityType.ONE)))
    dsl.check(
      "Root" to ERule(EBound(
        EAnd(EToken("set"), ERef("Rule", Rule)),
        EMultiplicity(MultiplicityType.PLUS, ";")
      )),
      "Rule" to Rule
    )
  }
}