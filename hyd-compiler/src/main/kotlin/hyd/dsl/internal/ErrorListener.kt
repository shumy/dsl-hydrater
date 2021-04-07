package hyd.dsl.internal

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA

import hyd.dsl.DslException

internal class ErrorListener(): BaseErrorListener() {
  override fun syntaxError(arg0: Recognizer<*, *>, arg1: Any, line: Int, pos: Int, msg: String, arg5: RecognitionException?) {
    if (arg1 is Token)
      throw DslException(line, pos, msg.capitalize())
  }

  // TODO: handle other error listeners!
}