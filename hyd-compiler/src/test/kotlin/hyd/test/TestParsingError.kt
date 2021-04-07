import hyd.dsl.*
import org.junit.Test

class TestParsingError {
  @Test fun testExpectedEOF() {
    val dsl = """
    |grammar test.Grammar
    |""".trimMargin()
    dsl.expect(DslException(2, 0, "Missing ';' at '<EOF>'"))
  }

  @Test fun testRuleNotFound() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |Root: Rule ;
    |""".trimMargin()
    dsl.expect(DslException(3, 6, "Rule 'Rule' not found!"))
  }

  @Test fun testCheckerNotFound() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[NonExistentChecker]
    |Root: value -type-> text ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'NonExistentChecker' not found!"))
  }

  @Test fun testIncompatibleCheckerType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[StartWithUpperCase]
    |Root: value -type-> int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'StartWithUpperCase' of type 'TEXT' is incompatible with dsl input 'INT'!"))
  }

  @Test fun testUnrecognizedCheckerType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[UnrecognizedType]
    |Root: value -type-> int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'UnrecognizedType' with an unrecognized type 'java.lang.Void'!"))
  }

  @Test fun testOptionalNotSupportSplitter() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |Root: (value -exist-> 'test')?';' ;
    |""".trimMargin()
    dsl.expect(DslException(3, 30, "Optional multiplicity doesn't support splitter!"))
  }

  @Test fun testKeyNotFoundInExpression() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |id@[StartWithUpperCase]
    |Root: value -type-> text* ;
    |""".trimMargin()
    dsl.expect(DslException(3, 0, "Key 'id' not found in the rule expression!"))
  }
}