package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.markup.ast.node.MarkupNode;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.interpreter.MarkupInterpreter;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.markup.parser.token.HierarchicalToken;
import at.blvckbytes.component_markup.markup.parser.token.OutputFlag;
import at.blvckbytes.component_markup.markup.parser.token.TokenOutput;
import at.blvckbytes.component_markup.platform.DataProvider;
import at.blvckbytes.component_markup.platform.PlatformEntity;
import at.blvckbytes.component_markup.platform.SlotType;
import at.blvckbytes.component_markup.platform.coordinates.Coordinates;
import at.blvckbytes.component_markup.platform.selector.TargetSelector;
import at.blvckbytes.component_markup.util.AsciiCasing;
import at.blvckbytes.component_markup.util.InputView;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSExport;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ComponentMarkupJs {

  private static final HTMLComponentConstructor COMPONENT_CONSTRUCTOR = new HTMLComponentConstructor();

  private static final EnumSet<OutputFlag> NO_FLAGS = EnumSet.noneOf(OutputFlag.class);

  private static final EnumSet<OutputFlag> LENIENT_FLAGS = EnumSet.of(
    OutputFlag.ENABLE_DUMMY_TAG,
    OutputFlag.UNMATCHED_CLOSING_TAGS_ARE_NO_OPS,
    OutputFlag.ALLOW_MISSING_ATTRIBUTES
  );

  private static final String[] ENTITY_TYPES = new String[] {
    "ALLAY", "ARMADILLO", "AXOLOTL", "BAT", "BEE", "BIRCH_BOAT", "BLAZE", "BOGGED", "BREEZE", "CAMEL",
    "CAT", "CAVE_SPIDER", "CHICKEN", "COD", "COW", "CREEPER", "DOLPHIN", "DONKEY", "DROWNED", "ELDER_GUARDIAN",
    "ENDER_DRAGON", "ENDERMAN", "ENDERMITE", "EVOKER", "FOX", "FROG", "GHAST", "GIANT", "GLOW_SQUID",
    "GOAT", "GUARDIAN", "HOGLIN", "HORSE", "HUSK", "ILLUSIONER", "IRON_GOLEM", "LLAMA", "MOOSHROOM",
    "MULE", "OCELOT", "PANDA", "PARROT", "PHANTOM", "PIG", "PIGLIN", "PIGLIN_BRUTE", "PILLAGER",
    "POLAR_BEAR", "PUFFERFISH", "RABBIT", "RAVAGER", "SALMON", "SHEEP", "SILVERFISH", "SKELETON", "SKELETON_HORSE",
    "SLIME", "SNIFFER", "SNOW_GOLEM", "SPIDER", "SQUID", "STRAY", "STRIDER", "TRADER_LLAMA", "TRIDENT",
    "TROPICAL_FISH", "TURTLE", "VEX", "VILLAGER", "VINDICATOR", "WANDERING_TRADER", "WARDEN", "WITCH", "WITHER",
    "WITHER_SKELETON", "WITHER_SKULL", "WOLF", "ZOGLIN", "ZOMBIE", "ZOMBIE_HORSE", "ZOMBIE_VILLAGER", "ZOMBIFIED_PIGLIN"
  };

  static {
    for (int i = 0; i < ENTITY_TYPES.length; ++i) {
      String type = ENTITY_TYPES[i];
      StringBuilder result = new StringBuilder();

      boolean isFirstLetter = false;

      for (int charIndex = 0; charIndex < type.length(); ++charIndex) {
        char currentChar = type.charAt(charIndex);

        if (currentChar != '_') {
          if (isFirstLetter)
            currentChar = AsciiCasing.upper(currentChar);
          else
            currentChar = AsciiCasing.lower(currentChar);

          isFirstLetter = false;
        }
        else {
          isFirstLetter = true;
          result.append('-');
          continue;
        }

        result.append(currentChar);
      }

      ENTITY_TYPES[i] = JsInterpretationPlatform.INSTANCE.toTitleCase(result.toString());
    }
  }

  private static PlatformEntity makeRandomEntity(String name, Coordinates origin, int randomCoordinateRadius) {
    return new PlatformEntity(name, name, UUID.randomUUID()) {

      private final int x = ThreadLocalRandom.current().nextInt((int) origin.x - randomCoordinateRadius, (int) origin.x + randomCoordinateRadius);
      private final int y = ThreadLocalRandom.current().nextInt((int) origin.y - randomCoordinateRadius, (int) origin.y + randomCoordinateRadius);
      private final int z = ThreadLocalRandom.current().nextInt((int) origin.z - randomCoordinateRadius, (int) origin.z + randomCoordinateRadius);

      @Override
      public int x() {
        return this.x;
      }

      @Override
      public int y() {
        return this.y;
      }

      @Override
      public int z() {
        return this.z;
      }

      @Override
      public String world() {
        return "world";
      }
    };
  }

  private static final Coordinates WORLD_ORIGIN = new Coordinates(891, 70, 134, "world");

  private static final PlatformEntity RECIPIENT = makeRandomEntity("BlvckBytes", WORLD_ORIGIN, 5);

  private static final DataProvider DATA_PROVIDER = new DataProvider(){

    @Override
    public List<PlatformEntity> executeSelector(TargetSelector selector, Coordinates origin, @Nullable PlatformEntity self) {
      List<PlatformEntity> result = new ArrayList<>();

      // TODO: This could someday acknowledge filters, but as of now, I just want to display data
      for (int i = 0; i < 10; ++i) {
        int typeIndex = ThreadLocalRandom.current().nextInt(0, ENTITY_TYPES.length);
        result.add(makeRandomEntity(ENTITY_TYPES[typeIndex], origin, 25));
      }

      return result;
    }
  };

  @JSExport
  public static JSParseError tokenize(String input, boolean lenient, boolean expression, boolean interpret, int renderCount) {
    TokenOutput tokenOutput = new TokenOutput(lenient ? LENIENT_FLAGS : NO_FLAGS);
    InputView inputView = InputView.of(input);

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
          List<Object> result = MarkupInterpreter.interpret(
            COMPONENT_CONSTRUCTOR,
            DATA_PROVIDER,
            new InterpretationEnvironment(new HashMap<>(), InterpretationEnvironment.DEFAULT_INTERPRETER, JsInterpretationPlatform.INSTANCE)
              .withVariable("render_count", renderCount),
            RECIPIENT,
            COMPONENT_CONSTRUCTOR.getSlotContext(SlotType.CHAT),
            ast
          );

          HTMLElement[] components = new HTMLElement[result.size()];

          for (int i = 0; i < components.length; ++i) {
            HTMLElement component = (HTMLElement) result.get(i);
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
