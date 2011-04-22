/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.jstestdriver.idea.config;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;

public class JstdConfigFileType extends LanguageFileType {

  public static final JstdConfigFileType INSTANCE = new JstdConfigFileType();
  private static final Icon ICON = IconLoader.getIcon("/com/google/jstestdriver/idea/ui/JsTestDriver.png");

  /**
   * Creates a language file type for the specified language.
   */
  protected JstdConfigFileType() {
    super(YAMLLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "JsTestDriver";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "JsTestDriver config file";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "jstd";
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }

}
