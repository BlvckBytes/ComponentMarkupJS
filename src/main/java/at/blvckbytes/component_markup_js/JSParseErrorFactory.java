package at.blvckbytes.component_markup_js;

import org.teavm.jso.JSBody;

public class JSParseErrorFactory {

  @JSBody(
    params = { "errorMessage", "errorCharIndex" },
    script = "return {errorMessage: errorMessage, errorCharIndex: errorCharIndex};"
  )
  public static native JSParseError create(String errorMessage, int errorCharIndex);
}
