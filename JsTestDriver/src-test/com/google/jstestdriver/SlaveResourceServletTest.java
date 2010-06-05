/*
 * Copyright 2008 Google Inc.
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

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
public class SlaveResourceServletTest extends TestCase {

  public void testIdChoppedOffFromThePath() throws Exception {
    String location = getClass().getPackage().getName().replace(".", "/");
    SlaveResourceServlet servlet = new SlaveResourceServlet(new SlaveResourceService(location));
    OutputStream oStream = new ByteArrayOutputStream();

    servlet.service("/XXX/Test.file", oStream);
    assertTrue(oStream.toString().length() > 0);
    oStream.close();
  }

  public void testStripOffId() throws Exception {
    assertEquals("/B/C", SlaveResourceServlet.stripId("/X/B/C"));
  }
}
