package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.markup.ast.node.MarkupNode;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.interpreter.ComponentOutput;
import at.blvckbytes.component_markup.markup.interpreter.MarkupInterpreter;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.markup.parser.token.HierarchicalToken;
import at.blvckbytes.component_markup.markup.parser.token.OutputFlag;
import at.blvckbytes.component_markup.markup.parser.token.TokenOutput;
import at.blvckbytes.component_markup.platform.SlotType;
import at.blvckbytes.component_markup.util.StringView;
import org.teavm.jso.JSExport;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.EnumSet;
import java.util.List;

public class ComponentMarkupJs {

  private static final HTMLComponentConstructor COMPONENT_CONSTRUCTOR = new HTMLComponentConstructor();

  private static final EnumSet<OutputFlag> NO_FLAGS = EnumSet.noneOf(OutputFlag.class);

  private static final EnumSet<OutputFlag> LENIENT_FLAGS = EnumSet.of(
    OutputFlag.ENABLE_DUMMY_TAG,
    OutputFlag.UNMATCHED_CLOSING_TAGS_ARE_NO_OPS,
    OutputFlag.ALLOW_MISSING_ATTRIBUTES
  );

  @JSExport
  public static JSParseError tokenize(String input, boolean lenient, boolean expression, boolean interpret) {
    TokenOutput tokenOutput = new TokenOutput(lenient ? LENIENT_FLAGS : NO_FLAGS);
    StringView inputView = StringView.of(input);

    String errorMessage = null;
    int errorCharIndex = -1;

    List<HierarchicalToken> hierarchicalTokens = null;

    if (expression) {
      try {
        tokenOutput.onInitialization(inputView);
        ExpressionParser.parse(inputView, tokenOutput);
        tokenOutput.onInputEnd();
        hierarchicalTokens = tokenOutput.getResult();
      } catch (ExpressionParseException e) {
        errorMessage = e.getErrorMessage();
        errorCharIndex = e.position;
      }
    }

    else {
      try {
        MarkupNode ast = MarkupParser.parse(inputView, BuiltInTagRegistry.INSTANCE, tokenOutput);

        if (interpret) {
          ComponentOutput output = MarkupInterpreter.interpret(
            COMPONENT_CONSTRUCTOR,
            new InterpretationEnvironment(),
            null,
            COMPONENT_CONSTRUCTOR.getSlotContext(SlotType.CHAT),
            ast
          );

          HTMLElement[] components = new HTMLElement[output.unprocessedComponents.size()];

          for (int i = 0; i < components.length; ++i) {
            HTMLElement component = (HTMLElement) output.unprocessedComponents.get(i);
            HTMLComponentConstructor.addClass(component, HTMLComponentConstructor.LINE_CLASS);
            components[i] = component;
          }

          JSComponentsEmitter.onEmitComponents(components);
        }

        hierarchicalTokens = tokenOutput.getResult();
      } catch (MarkupParseException e) {
        errorMessage = e.getErrorMessage();
        errorCharIndex = e.getCharIndex();
      }
    }

    if (hierarchicalTokens != null) {
      HierarchicalToken.toSequence(hierarchicalTokens, (type, value) -> {
        JSTokenEmitter.onEmitToken(type.name(), value.startInclusive, value.endExclusive, value.buildString());
      });
    }

    return JSParseErrorFactory.create(errorMessage, errorCharIndex);
  }
}
