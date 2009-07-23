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
package com.google.jstestdriver;

import java.util.List;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.jstestdriver.ActionFactory.ActionFactoryFileFilter;
import com.google.jstestdriver.hooks.FileReaderHook;

/**
 * Guice module for configuring JsTestDriver.
 * 
 * @author corysmith
 * 
 */
public class JsTestDriverModule extends AbstractModule {

  private final FlagsImpl flags;
  private final String defaultServerAddress;
  private final Set<FileInfo> fileSet;
  private final List<Class<? extends Module>> plugins;

  public JsTestDriverModule(FlagsImpl flags, Set<FileInfo> fileSet,
      String defaultServerAddress, List<Class<? extends Module>> plugins) {
    this.flags = flags;
    this.fileSet = fileSet;
    this.defaultServerAddress = defaultServerAddress;
    this.plugins = plugins;
  }

  @Override
  protected void configure() {
    // install plugin modules.
    for (Class<? extends Module> module : plugins) {
      try {
        install(module.newInstance());
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    // provide a default list of plugin hooks.
    Multibinder.newSetBinder(binder(), FileReaderHook.class);
    bind(FileReader.class).to(HookedFileReader.class);
    bind(JsTestDriverFileFilter.class).to(ActionFactoryFileFilter.class);
  }
  
  @Provides
  public HeartBeatManager provideHeartBeatManager() {
    return new SimpleHeartBeatManager();
  }

  @Provides
  public List<Action> createActions(ActionFactory factory, FileLoader fileLoader) {
    return new ActionParser(factory, fileLoader).parseFlags(flags, fileSet, defaultServerAddress);
  }
}
