// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy.intentions.control;

import org.jetbrains.plugins.groovy.util.ActionTest;
import org.jetbrains.plugins.groovy.util.GroovyLatestTest;
import org.junit.Test;

public class NegateComparisonIntentionTest extends GroovyLatestTest implements ActionTest {
  @Test
  public void negate_considers_context() {
    getFixture().addFileToProject("com/foo/Node.java", "package com.foo; class Node {}");
    doActionTest("Negate", "import com.foo.Node; (1 as Node) <caret>== 2", """
      import com.foo.Node;
      
      !((1 as Node) != 2)""");
  }
}
