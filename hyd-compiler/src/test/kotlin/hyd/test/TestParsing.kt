import hyd.dsl.*
import org.junit.Test

class TestParsing {
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

  @Test fun testAndExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: 'left' & 'right' ;
    """
    dsl.check(
      "root" to EAnd(EToken("left"), EToken("right"))
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
      "root" to ERef("Rule", Rule),
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
      "root" to EMapRef("value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.ONE)),
      "Rule" to Rule
    )
  }

  @Test fun testMapManyRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -ref-> Rule* ;

      Rule: 'token' ; 
    """
    val Rule = EToken("token")
    dsl.check(
      "root" to EMapRef("value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.MANY)),
      "Rule" to Rule
    )
  }

  @Test fun testMapTypeWithCheckerExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -type-> text@StartWithUpperCase* ;
    """
    dsl.check(
      "root" to EMapType("value", ValueType.TEXT, EMultiplicity(MultiplicityType.MANY), StartWithUpperCase::class)
    )
  }

  @Test fun testAllMapTypeExpression() {
    val dsl = """
      grammar test.Grammar ;

      root:
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
      "root" to EOr(
        EOr(
          EOr(
            EOr(
              EOr(
                EOr(
                  EMapType("boolV", ValueType.BOOL, EMultiplicity(MultiplicityType.ONE)),
                  EMapType("textV", ValueType.TEXT, EMultiplicity(MultiplicityType.ONE))
                ),
                EMapType("intV", ValueType.INT, EMultiplicity(MultiplicityType.ONE))
              ),
              EMapType("floatV", ValueType.FLOAT, EMultiplicity(MultiplicityType.ONE))
            ),
            EMapType("dateV", ValueType.DATE, EMultiplicity(MultiplicityType.ONE))
          ),
          EMapType("timeV", ValueType.TIME, EMultiplicity(MultiplicityType.ONE))
        ),
        EMapType("datetimeV", ValueType.DATETIME, EMultiplicity(MultiplicityType.ONE))
      )
    )
  }

  @Test fun testExprWithMultiplicityExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: ('set' & Rule)+';' ;

      Rule: ('v1' | 'v2') ;
    """
    val Rule = EBound(EOr(EToken("v1"), EToken("v2")), EMultiplicity(MultiplicityType.ONE))
    dsl.check(
      "root" to EBound(
        EAnd(EToken("set"), ERef("Rule", Rule)),
        EMultiplicity(MultiplicityType.PLUS, ";")
      ),
      "Rule" to Rule
    )
  }
}