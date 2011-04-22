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
package com.google.jstestdriver.idea.javascript;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.jstestdriver.idea.javascript.navigation.NavigationRegistryBuilderImpl;
import com.google.jstestdriver.idea.javascript.predefined.Marker;
import com.intellij.lang.javascript.library.JSLibraryMappings;
import com.intellij.lang.javascript.library.JSLibraryType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.scripting.ScriptingLibraryManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class JSUnitTestingSupport implements ProjectComponent {

  public static final Set<Project> ourOpenProjects = new HashSet<Project>();

  private Project myProject;

  public JSUnitTestingSupport(Project project) {
    myProject = project;
    ourOpenProjects.add(project);
  }

  @NotNull
  @Override
  public String getComponentName() {
    return JSUnitTestingSupport.class.getName();
  }

  private VirtualFile getVirtualFiles(Class<?> clazz, String resourceName) {
    VirtualFile file = VfsUtil.findFileByURL(clazz.getResource(resourceName));
    if (file == null) {
      throw new RuntimeException("Can't find virtual file for '" + resourceName + "', class " + clazz);
    }
    return file;
  }

  @Override
  public void projectOpened() {
    StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
      @Override
      public void run() {
        installLibrary();
        NavigationRegistryBuilderImpl.register();
      }
    });
  }

  private void installLibrary() {
    final ScriptingLibraryManager libraryManager = new ScriptingLibraryManager(myProject, JSLibraryType.getInstance());
    String[] resourceNames = new String[] {"Asserts.js", "TestCase.js", "qunit/equiv.js", "qunit/QUnitAdapter.js"};
    VirtualFile[] sourceFiles = new VirtualFile[resourceNames.length];
    for (int i = 0; i < resourceNames.length; i++) {
      sourceFiles[i] = getVirtualFiles(Marker.class, resourceNames[i]);
    }
    String libraryName = "JsTD Assertion Framework";
    libraryManager.createLibrary(libraryName, sourceFiles, new VirtualFile[] {}, new String[] {});
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        libraryManager.commitChanges();
      }
    });

    JSLibraryMappings mappings = ServiceManager.getService(myProject, JSLibraryMappings.class);
    mappings.associate(myProject.getBaseDir(), libraryName);
  }

  @Override
  public void projectClosed() {
    ourOpenProjects.remove(myProject);
  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {
  }
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
package com.google.jstestdriver.idea.javascript;

import org.jetbrains.annotations.NotNull;

import com.google.jstestdriver.idea.javascript.navigation.NavigationRegistryBuilderImpl;
import com.google.jstestdriver.idea.javascript.predefined.Marker;
import com.intellij.lang.javascript.library.JSLibraryManager;
import com.intellij.lang.javascript.library.JSLibraryMappings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.scripting.ScriptingLibraryManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class JSUnitTestingSupport implements ProjectComponent {

  private Project myProject;

  public JSUnitTestingSupport(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public String getComponentName() {
    return JSUnitTestingSupport.class.getName();
  }

  private VirtualFile getVirtualFiles(Class<?> clazz, String resourceName) {
    VirtualFile file = VfsUtil.findFileByURL(clazz.getResource(resourceName));
    if (file == null) {
      throw new RuntimeException("Can't find virtual file for '" + resourceName + "', class " + clazz);
    }
    return file;
  }

  @Override
  public void projectOpened() {
    StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
      @Override
      public void run() {
        installLibrary();
        NavigationRegistryBuilderImpl.register();
      }
    });
  }

  private void installLibrary() {
    final ScriptingLibraryManager libraryManager = ServiceManager.getService(myProject, JSLibraryManager.class);
    String[] resourceNames = new String[] {"Asserts.js", "TestCase.js"};
    VirtualFile[] sourceFiles = new VirtualFile[resourceNames.length];
    for (int i = 0; i < resourceNames.length; i++) {
      sourceFiles[i] = getVirtualFiles(Marker.class, resourceNames[i]);
    }
    String libraryName = "JsTD Assertion Framework";
    libraryManager.createLibrary(libraryName, sourceFiles, new VirtualFile[] {}, new String[] {});
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        libraryManager.commitChanges();
      }
    });

    JSLibraryMappings mappings = ServiceManager.getService(myProject, JSLibraryMappings.class);
    mappings.associate(myProject.getBaseDir(), libraryName);
  }

  @Override
  public void projectClosed() {
  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {
  }
}
