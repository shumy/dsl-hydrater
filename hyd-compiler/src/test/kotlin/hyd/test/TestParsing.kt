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
      "root" to EMapRef("value", ERef("Rule", Rule)),
      "Rule" to Rule
    )
  }

  @Test fun testMapTypeWithCheckerExpression() {
    val dsl = """
      grammar test.Grammar ;

      root: value -type-> text@checkers.StartWithUpperCase ;
    """
    dsl.check(
      "root" to EMapType("value", ValueType.TEXT, "checkers.StartWithUpperCase")
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
                  EMapType("boolV", ValueType.BOOL),
                  EMapType("textV", ValueType.TEXT)
                ),
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

      root: ('set' & Rule)+ #';';

      Rule: ('v1' | 'v2') ; 
    """
    val Rule = EBound(EOr(EToken("v1"), EToken("v2")), EMultiplicity(MultiplicityType.ONE, ","))
    dsl.check(
      "root" to EBound(
        EAnd(EToken("set"), ERef("Rule", Rule)),
        EMultiplicity(MultiplicityType.PLUS, ";")
      ),
      "Rule" to Rule
    )
  }
}