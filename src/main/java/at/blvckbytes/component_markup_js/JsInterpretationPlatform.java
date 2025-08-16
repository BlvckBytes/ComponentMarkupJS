package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.*;
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
  public String formatDate(String format, @Nullable String locale, @Nullable String timeZone, long timestamp, EnumSet<FormatDateWarning> encounteredWarnings) {
    if (encounteredWarnings.contains(FormatDateWarning.INVALID_LOCALE))
      locale = null;

    if (encounteredWarnings.contains(FormatDateWarning.INVALID_TIMEZONE))
      timeZone = null;

    try {
      return ApproximatedDateFormatter.format(format, locale, timeZone, timestamp);
    } catch (Throwable e) {
      if (e.getMessage().contains("invalid language")) {
        if (!encounteredWarnings.add(FormatDateWarning.INVALID_LOCALE))
          return "?";
      }

      else if (e.getMessage().contains("invalid time zone")) {
        if (!encounteredWarnings.add(FormatDateWarning.INVALID_TIMEZONE))
          return "?";
      }

      else {
        System.err.println(e.getMessage());
        return "?";
      }

      return formatDate(format, locale, timeZone, timestamp, encounteredWarnings);
    }
  }

  @Override
  public String formatNumber(String format, @Nullable String roundingMode, @Nullable String locale, Number number, EnumSet<FormatNumberWarning> encounteredWarnings) {
    if (encounteredWarnings.contains(FormatNumberWarning.INVALID_LOCALE))
      locale = null;

    if (encounteredWarnings.contains(FormatNumberWarning.INVALID_ROUNDING_MODE))
      roundingMode = null;

    try {
      return ApproximatedNumberFormatter.format(format, roundingMode, locale, number.doubleValue());
    } catch (Throwable e) {
      if (e.getMessage().contains("invalid language")) {
        if (!encounteredWarnings.add(FormatNumberWarning.INVALID_LOCALE))
          return "?";
      }

      else if (e.getMessage().contains("invalid rounding")) {
        if (!encounteredWarnings.add(FormatNumberWarning.INVALID_ROUNDING_MODE))
          return "?";
      }

      else {
        System.err.println(e.getMessage());
        return "?";
      }

      return formatNumber(format, roundingMode, locale, number, encounteredWarnings);
    }
  }
}
