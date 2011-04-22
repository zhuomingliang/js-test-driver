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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class NavigationRegistry {
  private final Map<String, TestCase> myTestCaseMap;

  NavigationRegistry(List<TestCase> testCases) {
    myTestCaseMap = Maps.newHashMap();
    for (TestCase testCase : testCases) {
      myTestCaseMap.put(testCase.getName(), testCase);
    }
  }

  public TestCase getTestCaseByName(String name) {
    return myTestCaseMap.get(name);
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
package com.google.jstestdriver.idea.javascript.navigation;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class NavigationRegistry {
  private final Map<String, TestCase> myTestCaseMap;

  NavigationRegistry(List<TestCase> testCases) {
    myTestCaseMap = Maps.newHashMap();
    for (TestCase testCase : testCases) {
      myTestCaseMap.put(testCase.getName(), testCase);
    }
  }

  public TestCase getTestCaseByName(String name) {
    return myTestCaseMap.get(name);
  }
}
