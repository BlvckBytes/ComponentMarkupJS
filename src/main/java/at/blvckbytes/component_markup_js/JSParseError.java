package at.blvckbytes.component_markup_js;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface JSParseError extends JSObject {

  @JSProperty
  @Nullable String getErrorMessage();

  @JSProperty
  int getErrorCharIndex();

}
