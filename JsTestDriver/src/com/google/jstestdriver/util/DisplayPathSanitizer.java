/*
 * Copyright 2011 Google Inc.
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
package com.google.jstestdriver.util;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;

/**
 * Handles the clean up and sanitization of paths to be displayed in the browser.
 * @author Cory Smith (corbinrsmith@gmail.com)
 */
public class DisplayPathSanitizer {

  private final File basePath;

  @Inject
  public DisplayPathSanitizer(@Named("basePath") File basePath) {
    this.basePath = basePath;
  }

  public String sanitize(String absolutePath) {
    return (absolutePath.startsWith(basePath.getAbsolutePath())
        ? absolutePath.substring(basePath.getAbsolutePath().length() + 1)
        : absolutePath).replaceAll("\\\\", "/");
  }
}