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
package com.google.jstestdriver.coverage;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author corysmith@google.com (Cory Smith)
 * 
 */
public class CoverageAccumulatorTest extends TestCase {

  public void testWrite() throws Exception {
    final String a = "foo.js";
    final String b = "zar.js";
    final List<CoveredLine> expected = Arrays.asList(
        new CoveredLine(a, 1, 3),
        new CoveredLine(a, 2, 2),
        new CoveredLine(a, 3, 3),
        new CoveredLine(a, 4, 0),
        new CoveredLine(b, 1, 3));

    final CoverageAccumulator accumulator = new CoverageAccumulator();
    final CoverageWriterFake coverageWriter = new CoverageWriterFake();

    accumulator.add("ff", Arrays.asList(
      new CoveredLine(b, 1, 1),
      new CoveredLine(a, 1, 1),
      new CoveredLine(a, 2, 1),
      new CoveredLine(a, 3, 1),
      new CoveredLine(a, 4, 0)
    ));

    accumulator.add("op", Arrays.asList(
      new CoveredLine(b, 1, 1),
      new CoveredLine(a, 1, 0),
      new CoveredLine(a, 2, 0),
      new CoveredLine(a, 3, 1),
      new CoveredLine(a, 4, 0)
    ));

    accumulator.add("ie", Arrays.asList(
      new CoveredLine(a, 1, 1),
      new CoveredLine(a, 2, 1),
      new CoveredLine(a, 3, 1),
      new CoveredLine(a, 4, 0),
      new CoveredLine(b, 1, 1)
    ));

    accumulator.write(coverageWriter);

    coverageWriter.assertLines(expected);
  }

  private final class CoverageWriterFake implements CoverageWriter {
    List<CoveredLine> lines = new LinkedList<CoveredLine>();

    public CoverageWriter addLine(CoveredLine line) {
      lines.add(line);
      return this;
    }

    public void assertLines(List<CoveredLine> expected) {
      assertEquals(expected, lines);
    }

    public void flush() {
      // TODO Auto-generated method stub
      
    }
  }
}
