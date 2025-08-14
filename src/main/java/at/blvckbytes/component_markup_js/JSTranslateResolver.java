package at.blvckbytes.component_markup_js;

import org.jetbrains.annotations.Nullable;
import org.teavm.dom.html.HTMLElement;
import org.teavm.jso.JSBody;

public class JSTranslateResolver {

  @JSBody(
    params = { "key" },
    script = "return window.tryResolveKey(key);"
  )
  public static native @Nullable String tryResolveKey(String key);
}
