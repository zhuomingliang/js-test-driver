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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

public class JstdConfigFileUtils {

  private JstdConfigFileUtils() {}

  public static final Set<String> VALID_TOP_LEVEL_KEYS = new HashSet<String>(Arrays.asList("load", "test", "exclude",
      "server", "plugin", "serve", "timeout", "basepath", "proxy"));

  static VirtualFile extractBasepath(YAMLDocument document) {
    VirtualFile configVF = document.getContainingFile().getOriginalFile().getVirtualFile();
    VirtualFile defaultBasePathVF = null;
    if (configVF != null) {
      defaultBasePathVF = configVF.getParent();
    }
    List<YAMLPsiElement> children = document.getYAMLElements();
    for (YAMLPsiElement child : children) {
      if (child instanceof YAMLKeyValue) {
        YAMLKeyValue keyValue = (YAMLKeyValue) child;
        if ("basepath".equals(keyValue.getKeyText())) {
          String basePath = keyValue.getValueText();
          if (defaultBasePathVF != null) {
            VirtualFile vf = defaultBasePathVF.findFileByRelativePath(basePath);
            if (vf != null) {
              return vf;
            }
          }
          File file = new File(basePath);
          if (file.exists()) {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
            if (vf != null) {
              return vf;
            }
          }
        }
      }
    }
    return defaultBasePathVF;
  }

  static <T, K> K getVerifiedHierarchyHead(PsiElement psiElement, List<Class<? extends T>> hierarchyClasses, Class<K> headHierarchyClass) {
    for (Class<?> clazz : hierarchyClasses) {
      if (!clazz.isInstance(psiElement)) {
        return null;
      }
      psiElement = psiElement.getParent();
    }
    return tryCast(psiElement, headHierarchyClass);
  }

  public static <T> T tryCast(Object o, Class<T> clazz) {
    if (clazz.isInstance(o)) {
      return clazz.cast(o);
    }
    return null;
  }
}
