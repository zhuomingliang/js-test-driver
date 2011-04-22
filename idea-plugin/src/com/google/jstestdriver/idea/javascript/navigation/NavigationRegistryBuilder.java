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
package com.google.jstestdriver.idea.javascript.navigation;

import java.io.File;
import java.util.Collections;

import com.intellij.openapi.project.Project;

public abstract class NavigationRegistryBuilder {

  private static NavigationRegistryBuilder INSTANCE = new NavigationRegistryBuilder() {
    @Override
    public NavigationRegistry buildNavigationRegistry(Project project, File configFile, File baseDir) {
      return new NavigationRegistry(Collections.<TestCase>emptyList());
    }
  };

  protected static void setInstance(NavigationRegistryBuilder instance) {
    INSTANCE = instance;
  }

  public static NavigationRegistryBuilder getInstance() {
    return INSTANCE;
  }

  public abstract NavigationRegistry buildNavigationRegistry(Project project, File configFile, File baseDir);

}
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
package com.google.jstestdriver.idea.javascript.navigation;

import java.io.File;
import java.util.Collections;

import com.intellij.openapi.project.Project;

public abstract class NavigationRegistryBuilder {

  private static NavigationRegistryBuilder INSTANCE = new NavigationRegistryBuilder() {
    @Override
    public NavigationRegistry buildNavigationRegistry(Project project, File configFile, File baseDir) {
      return new NavigationRegistry(Collections.<TestCase>emptyList());
    }
  };

  protected static void setInstance(NavigationRegistryBuilder instance) {
    INSTANCE = instance;
  }

  public static NavigationRegistryBuilder getInstance() {
    return INSTANCE;
  }

  public abstract NavigationRegistry buildNavigationRegistry(Project project, File configFile, File baseDir);

}
