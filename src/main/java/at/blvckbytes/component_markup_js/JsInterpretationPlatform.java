package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.FormatDateResult;
import at.blvckbytes.component_markup.expression.interpreter.FormatDateWarning;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationPlatform;
import at.blvckbytes.component_markup.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;

import java.util.EnumSet;

public class JsInterpretationPlatform implements InterpretationPlatform {

  public static final JsInterpretationPlatform INSTANCE = new JsInterpretationPlatform();

  private JsInterpretationPlatform() {}

  @JSBody(
    params = { "input", "delimiter" },
    script = "return input.split(delimiter);"
  )
  public static native String[] plainTextSplit(String input, String delimiter);

  @JSBody(
    params = { "input", "pattern" },
    script = "return input.split(new RegExp(pattern));"
  )
  public static native String[] regexSplit(String input, String pattern);

  @JSBody(
    params = { "input", "pattern" },
    script = "return new RegExp(pattern).test(input);"
  )
  public static native boolean regexTest(String input, String pattern);

  @Override
  public String[] split(String input, String delimiter, boolean regex) {
    if (regex) {
      try {
        return regexSplit(input, delimiter);
      } catch (Throwable e) {
        return null;
      }
    }

    return plainTextSplit(input, delimiter);
  }

  @Override
  public TriState matchesPattern(String input, String pattern) {
    try {
      return regexTest(input, pattern)
        ? TriState.TRUE
        : TriState.FALSE;
    } catch (Throwable e) {
      return TriState.NULL;
    }
  }

  @JSBody(
    params = { "input" },
    script = "return input.normalize('NFD').replace(/[\\u0300-\\u036f]/g, '');"
  )
  private static native String jsAsciify(String input);

  @Override
  public String asciify(String input) {
    return jsAsciify(input);
  }

  @JSBody(
    params = { "input" },
    script = (
      "return input"
        + ".replace(/[^\\p{L}\\d]+/gu, '-')"
        + ".replace(/(^-+)|(-+$)/g, '')"
        + ".toLowerCase();"
    )
  )
  public static native String jsSlugify(String input);

  @Override
  public String slugify(String input) {
    return jsSlugify(input);
  }

  @JSBody(
    params = { "input" },
    script = (
    "if (!input) return input;"
      + "const segmenter = new Intl.Segmenter('und', { granularity: 'word' });"
      + "let result = '';"
      + "for (const { segment, isWordLike } of segmenter.segment(input)) {"
      + "  if (isWordLike) {"
      + "    const first = segment.codePointAt(0);"
      + "    const firstChar = String.fromCodePoint(first).toUpperCase();"
      + "    const rest = segment.slice(firstChar.length).toLowerCase();"
      + "    result += firstChar + rest;"
      + "  } else {"
      + "    result += segment;"
      + "  }"
      + "}"
      + "return result;"
    )
  )
  public static native String jsToTitleCase(String input);

  @Override
  public String toTitleCase(String input) {
    return jsToTitleCase(input);
  }

  @Override
  public FormatDateResult formatDate(String format, @Nullable String locale, @Nullable String timeZone, long timestamp) {
    EnumSet<FormatDateWarning> encounteredWarnings = EnumSet.noneOf(FormatDateWarning.class);
    String result = _formatDate(format, locale, timeZone, timestamp, encounteredWarnings);
    return new FormatDateResult(result, encounteredWarnings);
  }

  private String _formatDate(String format, @Nullable String locale, @Nullable String timeZone, long timestamp, EnumSet<FormatDateWarning> encounteredWarnings) {
    if (encounteredWarnings.contains(FormatDateWarning.INVALID_LOCALE))
      locale = null;

    if (encounteredWarnings.contains(FormatDateWarning.INVALID_TIMEZONE))
      timeZone = null;

    try {
      return ApproximatedDateFormatter.format(format, locale, timeZone, timestamp);
    } catch (Throwable e) {
      if (e.getMessage().contains("invalid language"))
        encounteredWarnings.add(FormatDateWarning.INVALID_LOCALE);
      else if (e.getMessage().contains("invalid time zone"))
        encounteredWarnings.add(FormatDateWarning.INVALID_TIMEZONE);

      // As to avoid endless recursion on unknown errors
      else {
        System.err.println(e.getMessage());
        return "?";
      }

      return _formatDate(format, locale, timeZone, timestamp, encounteredWarnings);
    }
  }
}
