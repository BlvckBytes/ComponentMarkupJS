package at.blvckbytes.component_markup_js;

import at.blvckbytes.component_markup.util.ErrorScreen;
import at.blvckbytes.component_markup.util.InputView;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

public class ConsoleLogger implements InterpreterLogger {

  public static final ConsoleLogger INSTANCE = new ConsoleLogger();

  private ConsoleLogger() {}

  @Override
  public void log(InputView view, int position, String message, @Nullable Throwable e) {
    for (var line : ErrorScreen.make(view, position, message))
      System.err.println(line);

    if (e != null)
      e.printStackTrace();
  }
}
