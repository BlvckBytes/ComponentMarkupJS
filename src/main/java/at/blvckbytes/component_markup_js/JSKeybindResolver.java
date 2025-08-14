package at.blvckbytes.component_markup_js;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;

public class JSKeybindResolver {

  @JSBody(
    params = { "key" },
    script = "return window.tryResolveKeybind(key);"
  )
  public static native @Nullable String tryResolveKeybind(String key);
}
