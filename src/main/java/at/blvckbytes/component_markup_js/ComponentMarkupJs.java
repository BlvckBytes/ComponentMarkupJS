package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.markup.parser.token.HierarchicalToken;
import at.blvckbytes.component_markup.markup.parser.token.OutputFlag;
import at.blvckbytes.component_markup.markup.parser.token.TokenOutput;
import org.teavm.jso.JSExport;

import java.util.EnumSet;
import java.util.List;

public class ComponentMarkupJs {

  private static final EnumSet<OutputFlag> NO_FLAGS = EnumSet.noneOf(OutputFlag.class);

  private static final EnumSet<OutputFlag> LENIENT_FLAGS = EnumSet.of(
    OutputFlag.ENABLE_DUMMY_TAG,
    OutputFlag.UNMATCHED_CLOSING_TAGS_ARE_NO_OPS
  );

  @JSExport
  public static JSParseError tokenize(String input, boolean lenient, boolean expression) {
    TokenOutput tokenOutput = new TokenOutput(lenient ? LENIENT_FLAGS : NO_FLAGS);

    String errorMessage = null;
    int errorCharIndex = -1;

    List<HierarchicalToken> hierarchicalTokens = null;

    if (expression) {
      try {
        tokenOutput.onInitialization(input);
        ExpressionParser.parse(input, 0, tokenOutput);
        tokenOutput.onInputEnd();
        hierarchicalTokens = tokenOutput.getResult();
      } catch (ExpressionParseException e) {
        errorMessage = e.getErrorMessage();
        errorCharIndex = e.charIndex;
      }
    }

    else {
      try {
        MarkupParser.parse(input, BuiltInTagRegistry.INSTANCE, tokenOutput);
        hierarchicalTokens = tokenOutput.getResult();
      } catch (MarkupParseException e) {
        errorMessage = e.getErrorMessage();
        errorCharIndex = e.getCharIndex();
      }
    }

    if (hierarchicalTokens != null) {
      HierarchicalToken.toSequence(hierarchicalTokens, (type, beginIndex, value) -> {
        JSTokenEmitter.onEmitToken(type.name(), beginIndex, beginIndex + value.length(), value);
      });
    }

    return JSParseErrorFactory.create(errorMessage, errorCharIndex);
  }
}
