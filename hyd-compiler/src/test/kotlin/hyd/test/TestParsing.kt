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

      Root: 'left' 'right' ;
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

      Root: value = 'text' ;
    """
    dsl.check(
      "Root" to ERule(EMap(DataType.BOOL, "value", EToken("text"), EMultiplicity(MultiplicityType.ONE)))
    )
  }

  @Test fun testMapEnumExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value = ('one' | 'two' | 'three')+ ;
    """
    val values = listOf(EToken("one"), EToken("two"), EToken("three"))
    dsl.check(
      "Root" to ERule(EMap(DataType.BOOL, "value", EEnum(values), EMultiplicity(MultiplicityType.PLUS)))
    )
  }

  @Test fun testMapEmbeddedExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value = Rule ;

      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(EMap(DataType.EMBEDDED, "value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.ONE))),
      "Rule" to Rule
    )
  }

  @Test fun testMapRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value = &Rule ;

      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(EMap(DataType.REF, "value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.ONE))),
      "Rule" to Rule
    )
  }

  @Test fun testMapManyRefExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: value = &Rule* ;

      Rule: 'token' ; 
    """
    val Rule = ERule(EToken("token"))
    dsl.check(
      "Root" to ERule(EMap(DataType.REF, "value", ERef("Rule", Rule), EMultiplicity(MultiplicityType.MANY))),
      "Rule" to Rule
    )
  }

  @Test fun testAllMapTypeExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root:
          boolV = bool
        | textV = text
        | intV = int
        | floatV = float
        | dateV = date
        | timeV = time
        | datetimeV = datetime
        | embeddedV = embedded
        | refV = ref
      ;
    """
    dsl.check(
      "Root" to ERule(EOr(
        EOr(
          EOr(
            EOr(
              EOr(
                EOr(
                  EOr(
                    EOr(
                      EMap(DataType.BOOL, "boolV", EType(DataType.BOOL), EMultiplicity(MultiplicityType.ONE)),
                      EMap(DataType.TEXT, "textV", EType(DataType.TEXT), EMultiplicity(MultiplicityType.ONE))
                    ),
                    EMap(DataType.INT, "intV", EType(DataType.INT), EMultiplicity(MultiplicityType.ONE))
                  ),
                  EMap(DataType.FLOAT, "floatV", EType(DataType.FLOAT), EMultiplicity(MultiplicityType.ONE))
                ),
                EMap(DataType.DATE, "dateV", EType(DataType.DATE), EMultiplicity(MultiplicityType.ONE))
              ),
              EMap(DataType.TIME,"timeV", EType(DataType.TIME), EMultiplicity(MultiplicityType.ONE))
            ),
            EMap(DataType.DATETIME, "datetimeV", EType(DataType.DATETIME), EMultiplicity(MultiplicityType.ONE))
          ),
          EMap(DataType.EMBEDDED, "embeddedV", EType(DataType.EMBEDDED), EMultiplicity(MultiplicityType.ONE))
        ),
        EMap(DataType.REF, "refV", EType(DataType.REF), EMultiplicity(MultiplicityType.ONE))
      ))
    )
  }

  @Test fun testMultiplicityExpression() {
    val dsl = """
      grammar test.Grammar ;

      Root: ('set' Rule)+ #';' ;

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

  @Test fun testCheckers() {
    val dsl = """
      grammar test.Grammar ;

      this@[IsEntityValid]
      value@[StartWithUpperCase]
      Root: value = text* ;
    """
    dsl.check(
      "Root" to ERule(
        EMap(DataType.TEXT, "value", EType(DataType.TEXT), EMultiplicity(MultiplicityType.MANY)),
        mapOf(
          "value" to EChecker("value", listOf(startWithUpperCase)),
          "this" to EChecker("this", listOf(isEntityValid))
        )
      )
    )
  }
}