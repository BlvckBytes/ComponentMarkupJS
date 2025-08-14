package at.blvckbytes.component_markup_js;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;

public class JSTranslationResolver {

  @JSBody(
    params = { "key" },
    script = "return window.tryResolveTranslationKey(key);"
  )
  public static native @Nullable String tryResolveTranslationKey(String key);
}
