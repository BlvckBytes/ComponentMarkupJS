package at.blvckbytes.component_markup_js;

import org.teavm.jso.JSBody;

public class JSTokenEmitter {

  @JSBody(
    params = { "type", "beginIndexInclusive", "endIndexExclusive", "value" },
    script = "window.onEmitToken(type, beginIndexInclusive, endIndexExclusive, value);"
  )
  public static native void onEmitToken(String type, int beginIndexInclusive, int endIndexExclusive, String value);

}
