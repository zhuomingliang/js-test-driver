/*
 * Copyright 2010 Google Inc.
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
package com.google.jstestdriver.debugger;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.jstestdriver.FileInfo;
import com.google.jstestdriver.Time;
import com.google.jstestdriver.hooks.ResourcePreProcessor;

import java.util.LinkedList;
import java.util.List;

public class DebuggerFileListProcessor implements ResourcePreProcessor {
  private static final String DEBUGGER_SOURCE = "/* Execution has been paused "
      + "using the 'debugger' statement."
      + "Please set the appropriate break points "
      + "and resume execution. */\ndebugger;";
  private final Time time;

  @Inject
  public DebuggerFileListProcessor(Time time) {
    this.time = time;
  }

  public List<FileInfo> processTests(List<FileInfo> files) {
    LinkedList<FileInfo> processed = Lists.newLinkedList(files);
    processed.add(new FileInfo("debugger.js", time.now().getMillis(), -1,
        false, false, DEBUGGER_SOURCE, "debugger.js"));
    return processed;
  }

  public List<FileInfo> processPlugins(List<FileInfo> files) {
    
    return files;
  }

  public List<FileInfo> processDependencies(List<FileInfo> files) {
    // TODO Auto-generated method stub
    return null;
  }
}
