package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.node.terminal.DeferredRenderer;
import at.blvckbytes.component_markup.markup.ast.node.terminal.RendererParameter;
import at.blvckbytes.component_markup.platform.*;
import at.blvckbytes.component_markup.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HTMLComponentConstructor implements ComponentConstructor {

  private static final String COMPONENT_CLASS = "rendered-component";
  private static final String HOVER_TEXT_CLASS = COMPONENT_CLASS + "__hover-text";

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

    if (!text.isEmpty()) {
      // Text-nodes are not elements, and thus hit-tests are impossible.
      // The additional container allows to avoid rendering hover-events on large whitespace.
      HTMLElement textElement = dom().createElement("span");
      textElement.appendChild(dom().createTextNode(text));
      element.appendChild(textElement);
    }

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

    int remainingChars = translation.length() - 1;

    int nextAppendIndex = 0;
    int withIndex = 0;

    for (int charIndex = 0; charIndex < translation.length(); ++charIndex) {
      --remainingChars;

      if (translation.charAt(charIndex) == '%' && remainingChars > 0) {
        char nextChar = translation.charAt(charIndex + 1);

        int index;

        if (nextChar >= '0' && nextChar <= '9')
          index = (nextChar - '0') - 1;
        else
          index = withIndex++;

        if (index < 0 || index >= with.size())
          return createTextComponent(key);

        if (charIndex != 0)
          result.add(createTextComponent(translation.substring(nextAppendIndex, charIndex)));

        if (remainingChars > 1 && translation.charAt(charIndex + 2) == '$')
          charIndex += 2;

        nextAppendIndex = charIndex + 2;

        result.add(with.get(index));
        ++charIndex;
      }
    }

    if (nextAppendIndex <= translation.length() - 1)
      result.add(createTextComponent(translation.substring(nextAppendIndex)));

    if (result.size() == 1)
      return result.getFirst();

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
    if (setMembers(component, MembersSlot.HOVER_TEXT_VALUE, Collections.singletonList(text)) == null)
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
    var style = ((HTMLElement) component).getStyle();
    var shadowColorValue = style.getPropertyValue("--shadow-color");

    if (shadowColorValue == null || shadowColorValue.isBlank()) {
      long shadowColor = packedColor;
      shadowColor = PackedColor.setClampedA(shadowColor, 60);
      style.setProperty("--shadow-color", PackedColor.asAlphaHex(shadowColor));
    }

    style.setProperty("color", PackedColor.asNonAlphaHex(packedColor));
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
    HTMLElement element = (HTMLElement) component;

    if (slot == MembersSlot.CHILDREN) {
      var elementChildren = element.getChildNodes();

      for (int childIndex = elementChildren.getLength() - 1; childIndex >= 0; --childIndex) {
        var child = elementChildren.item(childIndex);

        if (child.getNodeType() == Node.ELEMENT_NODE && child.hasAttributes()) {
          var classAttribute = child.getAttributes().getNamedItem("class");

          if (classAttribute != null && classAttribute.getValue().contains(HOVER_TEXT_CLASS))
            continue;
        }

        element.removeChild(child);
      }

      while (element.hasChildNodes())
        element.removeChild(element.getFirstChild());

      if (children != null) {
        for (Object child : children)
          element.appendChild((HTMLElement) child);
      }

      return element;
    }

    if (slot == MembersSlot.HOVER_TEXT_VALUE) {
      var elementChildren = element.getChildNodes();

      for (int childIndex = elementChildren.getLength() - 1; childIndex >= 0; --childIndex) {
        var child = elementChildren.item(childIndex);

        if (child.getNodeType() != Node.ELEMENT_NODE || !child.hasAttributes())
          continue;

        var classAttribute = child.getAttributes().getNamedItem("class");

        if (classAttribute != null && classAttribute.getValue().contains(HOVER_TEXT_CLASS))
          element.removeChild(child);
      }

      if (children != null) {
        for (Object child : children) {
          var childElement = (HTMLElement) child;
          var classAttribute = childElement.getAttribute("class");

          if (classAttribute == null || classAttribute.isBlank())
            childElement.setAttribute("class", HOVER_TEXT_CLASS);
          else if (!classAttribute.contains(HOVER_TEXT_CLASS))
            childElement.setAttribute("class", classAttribute + " " + HOVER_TEXT_CLASS);


          element.appendChild(childElement);
        }
      }

      return element;
    }

    return null;
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
