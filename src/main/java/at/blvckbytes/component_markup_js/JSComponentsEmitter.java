package at.blvckbytes.component_markup_js;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

public class JSComponentsEmitter {

  @JSBody(
    params = { "components" },
    script = "window.onEmitComponents(components);"
  )
  public static native void onEmitComponents(HTMLElement[] components);
}
