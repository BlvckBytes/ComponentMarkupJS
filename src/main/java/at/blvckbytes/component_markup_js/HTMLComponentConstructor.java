package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.node.terminal.DeferredRenderer;
import at.blvckbytes.component_markup.markup.ast.node.terminal.RendererParameter;
import at.blvckbytes.component_markup.platform.*;
import at.blvckbytes.component_markup.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.jso.JSBody;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HTMLComponentConstructor implements ComponentConstructor {

  private static final String COMPONENT_CLASS = "rendered-component";

  private static final SlotContext MODIFIED_CHAT = new SlotContext((char) 0, SlotContext.getForSlot(SlotType.CHAT).defaultStyle);

  private static final RuntimeException UNSUPPORTED_EXCEPTION = new UnsupportedOperationException("Not supported by the web-editor");

  @JSBody(script = "return document;")
  private static native HTMLDocument dom();

  @Override
  public SlotContext getSlotContext(SlotType slot) {
    if (slot == SlotType.CHAT)
      return MODIFIED_CHAT;

    return SlotContext.getForSlot(slot);
  }

  @Override
  public Object createTextComponent(String text) {
    HTMLElement element = dom().createElement("span");

    element.setAttribute("class", COMPONENT_CLASS);

    if (!text.isEmpty())
      element.appendChild(dom().createTextNode(text));

    return element;
  }

  @Override
  public Object createKeyComponent(String key) {
    String binding = JSKeybindResolver.tryResolveKeybind(key);

    if (binding == null)
      return createTextComponent(key);

    if (binding.indexOf('.') < 0)
      return createTextComponent(binding);

    String translation = JSTranslationResolver.tryResolveTranslationKey(binding);

    if (translation == null)
      return createTextComponent(binding);

    return createTextComponent(translation);
  }

  @Override
  public Object createTranslateComponent(String key, List<Object> with, @Nullable String fallback) {
    String translation = JSTranslationResolver.tryResolveTranslationKey(key);

    if (translation == null)
      return createTextComponent(key);

    List<Object> result = new ArrayList<>();

    int remainingChars = translation.length();

    int nextAppendIndex = 0;
    int withIndex = 0;

    for (int i = 0; i < translation.length(); ++i) {
      char c = translation.charAt(i);
      --remainingChars;

      if (c == '%' && remainingChars != 0) {
        char nextChar = translation.charAt(i + 1);

        int index;

        if (nextChar >= '0' && nextChar <= '9')
          index = (nextChar - '0') - 1;
        else
          index = withIndex++;

        if (withIndex == with.size())
          return createTextComponent(key);

        if (i != 0)
          result.add(createTextComponent(translation.substring(nextAppendIndex, i)));

        if (remainingChars > 1 && translation.charAt(i + 2) == '$')
          i += 2;

        nextAppendIndex = i + 2;

        result.add(with.get(index));
        ++i;
      }
    }

    if (nextAppendIndex <= translation.length() - 1)
      result.add(createTextComponent(translation.substring(nextAppendIndex)));

    if (result.size() == 1)
      return result.get(0);

    return setMembers(createTextComponent(""), MembersSlot.CHILDREN, result);
  }

  @Override
  public DeferredComponent createDeferredComponent(DeferredRenderer<?> renderer, RendererParameter parameter, InterpretationEnvironment environmentSnapshot, SlotContext slotContext) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickChangePageAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickCopyToClipboardAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickOpenFileAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickOpenUrlAction(Object component, URL value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickRunCommandAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setClickSuggestCommandAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setHoverItemAction(Object component, @Nullable String material, @Nullable Integer count, @Nullable Object name, @Nullable List<Object> lore, boolean hideProperties) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setHoverTextAction(Object component, Object text) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setHoverEntityAction(Object component, String type, UUID id, @Nullable Object name) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setInsertAction(Object component, String value) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public void setColor(Object component, long packedColor) {
    ((HTMLElement) component).getStyle().setProperty("color", PackedColor.asNonAlphaHex(packedColor));
  }

  @Override
  public void setShadowColor(Object component, long packedColor) {
    ((HTMLElement) component).getStyle().setProperty("--shadow-color", PackedColor.asAlphaHex(packedColor));
  }

  @Override
  public void setFont(Object component, @Nullable String font) {
    if (font == null) {
      ((HTMLElement) component).getStyle().removeProperty("--font");
      return;
    }

    ((HTMLElement) component).getStyle().setProperty("--font", font);
  }

  @Override
  public void setObfuscatedFormat(Object component, TriState value) {
    if (value == TriState.TRUE)
      appendClass(component, COMPONENT_CLASS + "--obfuscated");
  }

  @Override
  public void setBoldFormat(Object component, TriState value) {
    if (value == TriState.TRUE)
      appendClass(component, COMPONENT_CLASS + "--bold");
  }

  @Override
  public void setStrikethroughFormat(Object component, TriState value) {
    if (value == TriState.TRUE)
      appendClass(component, COMPONENT_CLASS + "--strikethrough");
  }

  @Override
  public void setUnderlinedFormat(Object component, TriState value) {
    if (value == TriState.TRUE)
      appendClass(component, COMPONENT_CLASS + "--underlined");
  }

  @Override
  public void setItalicFormat(Object component, TriState value) {
    if (value == TriState.TRUE)
      appendClass(component, COMPONENT_CLASS + "--italic");
  }

  private void appendClass(Object component, String value) {
    HTMLElement element = (HTMLElement) component;

    String classList = element.getAttribute("class");

    if (classList == null || classList.isBlank())
      classList = value;
    else
      classList += " " + value;

    element.setAttribute("class", classList);
  }

  @Override
  public @Nullable Object setMembers(Object component, MembersSlot slot, @Nullable List<Object> children) {
    if (slot != MembersSlot.CHILDREN)
      return null;

    HTMLElement element = (HTMLElement) component;

    while (element.hasChildNodes())
      element.removeChild(element.getFirstChild());

    if (children != null) {
      for (Object child : children)
        element.appendChild((HTMLElement) child);
    }

    return element;
  }

  @Override
  public @Nullable List<Object> getMembers(Object component, MembersSlot slot) {
    throw UNSUPPORTED_EXCEPTION;
  }

  @Override
  public Object shallowCopyIncludingMemberLists(Object component) {
    throw UNSUPPORTED_EXCEPTION;
  }
}
