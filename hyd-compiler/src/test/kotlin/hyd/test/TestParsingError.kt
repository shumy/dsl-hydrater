import hyd.dsl.*
import org.junit.Test

class TestParsingError {
  @Test fun testNotFoundRule() {
    val dsl = """
      grammar test.Grammar ;

      root: Rule ;
    """
    dsl.expect("Rule 'Rule' not found!")
  }
}