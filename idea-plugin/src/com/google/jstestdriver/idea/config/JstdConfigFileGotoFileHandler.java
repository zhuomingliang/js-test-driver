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
package com.google.jstestdriver.idea.config;

import java.util.Arrays;

import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.TreeElement;

public class JstdConfigFileGotoFileHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement getGotoDeclarationTarget(PsiElement sourceElement) {
    TreeElement treeElement = JstdConfigFileUtils.tryCast(sourceElement, TreeElement.class);
    if (treeElement != null) {
      if (treeElement.getElementType() == YAMLTokenTypes.TEXT) {
        YAMLDocument document = JstdConfigFileUtils.getVerifiedHierarchyHead(sourceElement.getParent(),
            Arrays.asList(
                YAMLSequence.class,
                YAMLCompoundValue.class,
                YAMLKeyValue.class
            ),
            YAMLDocument.class
        );
        if (document != null) {
          VirtualFile basePath = JstdConfigFileUtils.extractBasepath(document);
          String relativePath = sourceElement.getText();
          VirtualFile gotoVFile = basePath.findFileByRelativePath(relativePath);
          if (gotoVFile != null) {
            return PsiManager.getInstance(sourceElement.getProject()).findFile(gotoVFile);
          }
        }
      }
    }
    return null;
  }

}
