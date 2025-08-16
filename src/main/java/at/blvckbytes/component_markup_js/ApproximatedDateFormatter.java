/*
 * Copyright (c) 2025, BlvckBytes
 * SPDX-License-Identifier: MIT
 */

package at.blvckbytes.component_markup_js;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSModule;
import org.teavm.jso.JSObject;

// This file resides right next to TeaVM's output over at the docs-project
@JSModule("./approximatedDateFormatter.js")
@JSClass
public class ApproximatedDateFormatter implements JSObject {

  public static native String format(String format, @Nullable String locale, @Nullable String timeZone, double timestamp);

}
