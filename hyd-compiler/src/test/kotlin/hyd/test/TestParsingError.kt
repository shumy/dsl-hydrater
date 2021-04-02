import hyd.dsl.*
import org.junit.Test

class TestParsingError {
  @Test fun testExpectedEOF() {
    val dsl = """
    |grammar test.Grammar
    |""".trimMargin()
    dsl.expect(DslException(2, 0, "mismatched input '<EOF>' expecting ';'"))
  }

  @Test fun testRuleNotFound() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |root: Rule ;
    |""".trimMargin()
    dsl.expect(DslException(3, 6, "Rule 'Rule' not found!"))
  }

  @Test fun testCheckerNotFound() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |root: value -type-> text@NonExistentChecker ;
    |""".trimMargin()
    dsl.expect(DslException(3, 25, "Checker 'NonExistentChecker' not found!"))
  }

  @Test fun testIncompatibleCheckerType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |root: value -type-> int@StartWithUpperCase ;
    |""".trimMargin()
    dsl.expect(DslException(3, 24, "Checker 'StartWithUpperCase' of type 'TEXT' is incompatible with dsl input 'INT'!"))
  }

  @Test fun testUnrecognizedCheckerType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |root: value -type-> int@UnrecognizedType ;
    |""".trimMargin()
    dsl.expect(DslException(3, 24, "Checker 'UnrecognizedType' with an unrecognized type 'java.lang.Void'!"))
  }

  @Test fun testOptionalNotSupportSplitter() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |root: (value -exist-> 'test')?';' ;
    |""".trimMargin()
    dsl.expect(DslException(3, 30, "Optional multiplicity doesn't support splitter!"))
  }
}