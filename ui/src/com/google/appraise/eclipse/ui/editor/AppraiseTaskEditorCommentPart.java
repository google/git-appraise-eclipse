/*******************************************************************************
 * Copyright (c) 2015 Google and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Scott McMaster - initial implementation
 *******************************************************************************/
package com.google.appraise.eclipse.ui.editor;

import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.internal.tasks.core.TaskComment;
import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorCommentPart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Custom comment editor part for Appraise. Supports extra things like the "resolved" flag.
 */
public class AppraiseTaskEditorCommentPart extends TaskEditorCommentPart {
  @Override
  protected AbstractAttributeEditor createAttributeEditor(TaskAttribute attribute) {
    if (attribute == null) {
      return null;
    }

    String type = attribute.getMetaData().getType();
    if (type != null) {
      AttributeEditorFactory attributeEditorFactory =
          getTaskEditorPage().getAttributeEditorFactory();
      AbstractAttributeEditor editor = attributeEditorFactory.createEditor(type, attribute);
      return editor;
    }
    return null;
  }

  /**
   * Add a "resolved" indicator to the toolbar title. It is implemented as a
   * fake action.
   */
  @Override
  protected void addActionsToToolbarTitle(
      ToolBarManager toolBarManager, TaskComment taskComment, CommentViewer commentViewer) {
    final TaskAttribute resolvedAttr = taskComment.getTaskAttribute().getAttribute(
        AppraiseReviewTaskSchema.COMMENT_RESOLVED_ATTRIBUTE);
    if (resolvedAttr != null && "true".equals(resolvedAttr.getValue())) {
      Action resolveAction = new Action() {
        @Override
        public String getToolTipText() {
          return "Resolved?";
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
          try {
            return ImageDescriptor.createFromURL(
                new URL(AppraiseReviewTaskEditorPage.baseURL, "greencheck.png"));
          } catch (MalformedURLException e) {
            AppraiseUiPlugin.logError("Unable to create icon image", e);
          }
          return null;
        }
      };
      toolBarManager.add(resolveAction);
    }
  }
}
