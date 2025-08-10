package at.blvckbytes.component_markup_js;

import org.teavm.dom.html.HTMLElement;
import org.teavm.jso.JSBody;

public class JSComponentsEmitter {

  @JSBody(
    params = { "components" },
    script = "window.onEmitComponents(components);"
  )
  public static native void onEmitComponents(HTMLElement[] components);
}
