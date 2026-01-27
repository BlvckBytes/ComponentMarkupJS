package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.constructor.ComponentConstructor;
import at.blvckbytes.component_markup.constructor.ConstructorFeature;
import at.blvckbytes.component_markup.constructor.SlotContext;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.markup.ast.node.style.Format;
import at.blvckbytes.component_markup.util.TriState;
import at.blvckbytes.component_markup.util.TriStateBitFlags;
import at.blvckbytes.component_markup.util.color.PackedColor;
import at.blvckbytes.component_markup.util.logging.GlobalLogger;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class HTMLComponentConstructor implements ComponentConstructor<HTMLElement, HTMLElement> {

  private static final String COMPONENT_CLASS = "rendered-component";
  public static final String LINE_CLASS = "rendered-component-line";
  private static final String HOVER_TEXT_CLASS = COMPONENT_CLASS + "__hover-text";

  private static final SlotContext MODIFIED_CHAT = new SlotContext((char) 0, SlotContext.getForSlot(SlotType.CHAT).defaultStyle);

  @JSBody(script = "return document;")
  private static native HTMLDocument dom();

  @Override
  public Class<HTMLElement> getComponentClass() {
    return HTMLElement.class;
  }

  @Override
  public boolean doesSupport(ConstructorFeature feature) {
    return true;
  }

  @Override
  public SlotContext getSlotContext(SlotType slot) {
    if (slot == SlotType.CHAT)
      return MODIFIED_CHAT;

    return SlotContext.getForSlot(slot);
  }

  @Override
  public HTMLElement createTextComponent(String text) {
    HTMLElement element = dom().createElement("span");

    addClass(element, COMPONENT_CLASS);

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
  public boolean setText(HTMLElement component, String text) {
    var elementChildren = component.getChildren();

    for (int childIndex = elementChildren.getLength() - 1; childIndex >= 0; --childIndex) {
      var child = elementChildren.item(childIndex);

      if (!child.getTagName().equalsIgnoreCase("span"))
        continue;

      if (containsClass(child, COMPONENT_CLASS))
        continue;

      component.replaceChild(dom().createTextNode(text), child);
      return true;
    }

    return false;
  }

  @Override
  public HTMLElement createKeyComponent(String key) {
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
  public HTMLElement createTranslateComponent(String key, List<HTMLElement> with, @Nullable String fallback) {
    String translation = JSTranslationResolver.tryResolveTranslationKey(key);

    if (translation == null)
      return createTextComponent(key);

    List<HTMLElement> result = new ArrayList<>();

    int nextAppendIndex = 0;
    int withIndex = 0;

    for (int charIndex = 0; charIndex < translation.length(); ++charIndex) {
      int remainingChars = translation.length() - 1 - charIndex;

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

    HTMLElement container = createTextComponent("");
    addChildren(container, result);
    return container;
  }

  @Override
  public void setClickChangePageAction(HTMLElement component, String value) {}

  @Override
  public void setClickCopyToClipboardAction(HTMLElement component, String value) {}

  @Override
  public void setClickOpenFileAction(HTMLElement component, String value) {}

  @Override
  public void setClickOpenUrlAction(HTMLElement component, String value) {}

  @Override
  public void setClickRunCommandAction(HTMLElement component, String value) {}

  @Override
  public void setClickSuggestCommandAction(HTMLElement component, String value) {}

  @Override
  public void setHoverItemAction(HTMLElement component, @Nullable String material, @Nullable Integer count, @Nullable HTMLElement name, @Nullable List<HTMLElement> lore, boolean hideProperties) {
    if (name == null) {
      if (material == null)
        material = "stone";

      if (material.startsWith("minecraft:"))
        material = material.substring(material.indexOf(':') + 1);

      name = createTranslateComponent("block.minecraft." + material.toLowerCase(), Collections.emptyList(), null);
    }
    else
      extendDefaultStyles(name, SlotType.ITEM_NAME);

    List<HTMLElement> lines = new ArrayList<>();

    lines.add(name);

    if (lore != null) {
      for (HTMLElement loreLine : lore) {
        extendDefaultStyles(loreLine, SlotType.ITEM_LORE);
        lines.add(loreLine);
      }
    }

    setHoverTextLines(component, lines);
  }

  @Override
  public void setHoverTextAction(HTMLElement component, HTMLElement text) {
    setHoverTextLines(component, Collections.singletonList(text));
  }

  @Override
  public void setHoverEntityAction(HTMLElement component, String type, UUID id, @Nullable HTMLElement name) {
    List<HTMLElement> lines = new ArrayList<>();

    if (name != null) {
      extendDefaultStyles(name, SlotType.ENTITY_NAME);
      lines.add(name);
    }

    if (type.startsWith("minecraft:"))
      type = type.substring(type.indexOf(':') + 1);

    lines.add(createTranslateComponent(
      "gui.entity_tooltip.type",
      Collections.singletonList(
        createTranslateComponent("entity.minecraft." + type.toLowerCase(), Collections.emptyList(), null)
      ),
      null
    ));

    lines.add(createTextComponent(String.valueOf(id)));

    setHoverTextLines(component, lines);
  }

  @Override
  public void setInsertAction(HTMLElement component, String value) {}

  @Override
  public void setColor(HTMLElement component, long packedColor, boolean overwrite) {
    var style = component.getStyle();

    var color = style.getPropertyValue("color");

    if (color != null && !color.isBlank() && !overwrite)
      return;

    style.setProperty("color", PackedColor.asNonAlphaHex(packedColor));

    setShadowColor(component, PackedColor.setClampedA(packedColor, 60), false);
  }

  @Override
  public void setShadowColor(HTMLElement component, long packedColor) {
    setShadowColor(component, packedColor, true);
  }

  private void setShadowColor(HTMLElement component, long packedColor, boolean override) {
    var style = component.getStyle();
    var shadowColor = style.getPropertyValue("--shadow-color");

    if (shadowColor != null && !shadowColor.isBlank() && !override)
      return;

    style.setProperty("--shadow-color", PackedColor.asAlphaHex(packedColor));
  }

  @Override
  public void setFont(HTMLElement component, @Nullable String font) {}

  @Override
  public void setObfuscatedFormat(HTMLElement component, TriState value) {
    setTriStateFormat(component, value, Format.OBFUSCATED, true);
  }

  @Override
  public void setBoldFormat(HTMLElement component, TriState value) {
    setTriStateFormat(component, value, Format.BOLD, true);
  }

  @Override
  public void setStrikethroughFormat(HTMLElement component, TriState value) {
    setTriStateFormat(component, value, Format.STRIKETHROUGH, true);
  }

  @Override
  public void setUnderlinedFormat(HTMLElement component, TriState value) {
    setTriStateFormat(component, value, Format.UNDERLINED, true);
  }

  @Override
  public void setItalicFormat(HTMLElement component, TriState value) {
    setTriStateFormat(component, value, Format.ITALIC, true);
  }

  @Override
  public HTMLElement finalizeComponent(HTMLElement component) {
    return component;
  }

  @Override
  public void addChildren(HTMLElement component, @Nullable List<HTMLElement> children) {
    var elementChildren = component.getChildren();

    for (int childIndex = elementChildren.getLength() - 1; childIndex >= 0; --childIndex) {
      var child = elementChildren.item(childIndex);

      if (!containsClass(child, HOVER_TEXT_CLASS))
        component.removeChild(child);
    }

    if (children != null) {
      for (HTMLElement child : children)
        component.appendChild(child);
    }
  }

  private void setHoverTextLines(HTMLElement component, @Nullable List<HTMLElement> children) {
    var elementChildren = component.getChildren();

    for (int childIndex = elementChildren.getLength() - 1; childIndex >= 0; --childIndex) {
      var child = elementChildren.item(childIndex);

      if (containsClass(child, HOVER_TEXT_CLASS))
        component.removeChild(child);
    }

    if (children != null) {
      var hoverContainer = dom().createElement("div");
      addClass(hoverContainer, HOVER_TEXT_CLASS);

      for (HTMLElement child : children) {
        addClass(child, LINE_CLASS);

        if (child.getChildren().getLength() == 0)
          child.appendChild(dom().createTextNode(" "));

        hoverContainer.appendChild(child);
      }

      component.appendChild(hoverContainer);
    }
  }

  private void extendDefaultStyles(HTMLElement element, SlotType type) {
    var defaultStyle = getSlotContext(type).defaultStyle;

    if (defaultStyle.getPackedColor() != PackedColor.NULL_SENTINEL)
      setColor(element, defaultStyle.getPackedColor(), false);

    if (defaultStyle.packedShadowColor != PackedColor.NULL_SENTINEL)
      setShadowColor(element, defaultStyle.packedShadowColor, false);

    for (Format format : Format.VALUES) {
      TriState state = TriStateBitFlags.read(defaultStyle.formats, format.ordinal());

      // As of now, there's no need to ever remove formats again, so don't call into the constructor
      if (state != TriState.NULL)
        setTriStateFormat(element, state, format, false);
    }
  }

  public static void removeClass(Element element, String className) {
    var classAttribute = element.getAttribute("class");

    int index;

    if (classAttribute == null || (index = classAttribute.indexOf(className)) < 0)
      return;

    classAttribute = classAttribute.substring(0, index) + classAttribute.substring(index + className.length());

    if (classAttribute.isBlank()) {
      element.removeAttribute("class");
      return;
    }

    element.setAttribute("class", classAttribute);
  }

  public static boolean containsClass(Element element, String className) {
    var classAttribute = element.getAttribute("class");
    return classAttribute != null && classAttribute.contains(className);
  }

  public static void addClass(Element element, String className) {
    var classAttribute = element.getAttribute("class");

    if (classAttribute == null || classAttribute.isBlank()) {
      element.setAttribute("class", className);
      return;
    }

    if (!classAttribute.contains(className))
      element.setAttribute("class", classAttribute + " " + className);
  }

  private void setTriStateFormat(HTMLElement element, TriState value, Format format, boolean override) {
    String classTrue;
    String classFalse;

    switch (format) {
      case BOLD:
        classTrue = COMPONENT_CLASS + "--bold";
        classFalse = COMPONENT_CLASS + "--non-bold";
        break;

      case ITALIC:
        classTrue = COMPONENT_CLASS + "--italic";
        classFalse = COMPONENT_CLASS + "--non-italic";
        break;

      case OBFUSCATED:
        classTrue = COMPONENT_CLASS + "--obfuscated";
        classFalse = COMPONENT_CLASS + "--non-obfuscated";
        break;

      case UNDERLINED:
        classTrue = COMPONENT_CLASS + "--underlined";
        classFalse = COMPONENT_CLASS + "--non-underlined";
        break;

      case STRIKETHROUGH:
        classTrue = COMPONENT_CLASS + "--strikethrough";
        classFalse = COMPONENT_CLASS + "--non-strikethrough";
        break;

      default:
        GlobalLogger.log(Level.WARNING, "Encountered unknown format: " + format.name());
        return;
    }

    if (!override) {
      if (containsClass(element, classTrue) || containsClass(element, classFalse))
        return;
    }

    removeClass(element, classTrue);
    removeClass(element, classFalse);

    switch (value) {
      case TRUE:
        addClass(element, classTrue);
        break;

      case FALSE:
        addClass(element, classFalse);
        break;
    }
  }
}
