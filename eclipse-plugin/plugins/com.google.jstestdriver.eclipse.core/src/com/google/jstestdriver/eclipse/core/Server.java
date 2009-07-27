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
package com.google.jstestdriver.eclipse.core;

import java.util.HashMap;

import com.google.jstestdriver.CapturedBrowsers;
import com.google.jstestdriver.FileData;
import com.google.jstestdriver.FilesCache;
import com.google.jstestdriver.ServerStartupAction;

public class Server {

  private ServerStartupAction startupAction;
  public static final int SERVER_PORT = 4224;
  public static final String SERVER_URL = "http://localhost:" + SERVER_PORT + "/capture";
  private final CapturedBrowsers capturedBrowsers;
  
  public Server() {
    capturedBrowsers = new CapturedBrowsers();
  }
  
  public CapturedBrowsers getCapturedBrowsers() {
    return capturedBrowsers;
  }

  public void start() {
    // TODO(shyamseshadri): Get rid of this and use the builder when we have something substantial.
    startupAction = new ServerStartupAction(SERVER_PORT, capturedBrowsers,
        new FilesCache(new HashMap<String, FileData>()));
    startupAction.run();
  }

  public void stop() {
    startupAction.getServer().stop();
  }
}