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

  @Test fun testSameTypeEnumExpression() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |Root: value = ('text' | int) ;
    |""".trimMargin()
    dsl.expect(DslException(3, 24, "All enum values should be of the same type!"))
  }

  @Test fun testCheckerNotFound() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[NonExistentChecker]
    |Root: value = text ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'NonExistentChecker' not found!"))
  }

  @Test fun testCheckerUnrecognizedType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[UnrecognizedType]
    |Root: value = int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'UnrecognizedType' with an unrecognized type 'java.lang.Void'!"))
  }

  @Test fun testInvalidCheckEntity() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[IsEntityValid]
    |Root: value = int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Invalid checker 'IsEntityValid'. Expecting implementation of 'ICheckValue'!"))
  }

  @Test fun testInvalidCheckValue() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |this@[StartWithUpperCase]
    |Root: value = int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 6, "Invalid checker 'StartWithUpperCase'. Expecting implementation of 'ICheckEntity'!"))
  }

  @Test fun testIncompatibleCheckerType() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |value@[StartWithUpperCase]
    |Root: value = int ;
    |""".trimMargin()
    dsl.expect(DslException(3, 7, "Checker 'StartWithUpperCase' of type 'String' is incompatible with dsl input 'Long'!"))
  }

  @Test fun testOptionalNotSupportSplitter() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |Root: (value = 'test')? #';' ;
    |""".trimMargin()
    dsl.expect(DslException(3, 25, "Optional multiplicity doesn't support splitter!"))
  }

  @Test fun testKeyNotFoundInExpression() {
    val dsl = """
    |grammar test.Grammar ;
    |
    |name@[StartWithUpperCase]
    |Root: value = text* ;
    |""".trimMargin()
    dsl.expect(DslException(3, 0, "Key 'name' not found in the rule expression!"))
  }
}