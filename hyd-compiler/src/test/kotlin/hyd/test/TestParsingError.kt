import hyd.dsl.*
import org.junit.Test

class TestParsingError {
  @Test fun testRuleNotFound() {
    val dsl = """
      grammar test.Grammar ;

      root: Rule ;
    """
    dsl.expect("Rule 'Rule' not found!")
  }

  @Test fun testCheckerNotFound() {
    val dsl = """
      grammar test.Grammar ;

      root: value -type-> text@NonExistentChecker ;
    """
    dsl.expect("Checker 'NonExistentChecker' not found!")
  }

  @Test fun testIncompatibleCheckerType() {
    val dsl = """
      grammar test.Grammar ;

      root: value -type-> int@StartWithUpperCase ;
    """
    dsl.expect("Checker 'StartWithUpperCase' of type 'TEXT' is incompatible with dsl input 'INT'!")
  }

  @Test fun testOptionalNotSupportSplitter() {
    val dsl = """
      grammar test.Grammar ;

      root: (value -exist-> 'test')?';' ;
    """
    dsl.expect("Optional multiplicity doesn't support splitter!")
  }
}