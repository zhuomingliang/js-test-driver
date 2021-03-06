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

/**
 * Externalizes the Thread.sleep commands.
 * @author corysmith@google.com (Cory Smith)
 *
 */
public class Sleeper {
  /**
   * Stops execution in the current thread for the definied milliseconds.
   * @throws InterruptedException 
   */
  public void sleep(long milliseconds) throws InterruptedException {
    Thread.sleep(milliseconds);
  }
}
