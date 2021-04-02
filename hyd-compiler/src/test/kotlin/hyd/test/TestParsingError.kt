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

  @Test fun testOptionalNotSupportSplitter() {
    val dsl = """
      grammar test.Grammar ;

      root: (value -exist-> 'test')?';' ;
    """
    dsl.expect("Optional multiplicity doesn't support splitter!")
  }
}