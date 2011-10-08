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
package com.google.jstestdriver.util;


import java.io.Writer;

/**
 * Null implementation of the StopWatch interface.
 * @author corysmith@google.com (Cory Smith)
 *
 */
public class NullStopWatch implements StopWatch {

  public void print(Writer writer) {
  }

  public void start(String operation, Object... args) {
  }

  public void stop(String operation, Object... args) {
  }
}
