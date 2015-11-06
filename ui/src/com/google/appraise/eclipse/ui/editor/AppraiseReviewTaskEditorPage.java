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

import com.google.appraise.eclipse.core.AppraiseConnectorPlugin;
import com.google.appraise.eclipse.core.AppraiseReviewTaskSchema;
import com.google.appraise.eclipse.ui.AppraiseUiPlugin;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.ColumnSpan;
import org.eclipse.mylyn.tasks.ui.editors.LayoutHint.RowSpan;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Custom task editor page for Appraise reviews.
 */
public class AppraiseReviewTaskEditorPage extends AbstractTaskEditorPage {
  static final URL baseURL = AppraiseUiPlugin.getDefault().getBundle().getEntry("/icons/");

  public AppraiseReviewTaskEditorPage(TaskEditor parentEditor) {
    super(parentEditor, AppraiseReviewTaskEditorPage.class.getName(), "Appraise Review",
        AppraiseConnectorPlugin.CONNECTOR_KIND);
    setNeedsPrivateSection(false);
    setNeedsSubmit(true);
    setNeedsSubmitButton(true);
  }

  @Override
  protected AttributeEditorFactory createAttributeEditorFactory() {
    return new AttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite()) {
      @Override
      public AbstractAttributeEditor createEditor(String type, TaskAttribute taskAttribute) {
        if (taskAttribute.getId().equals(
                AppraiseReviewTaskSchema.getDefault().REVIEW_COMMIT.getKey())) {
          return new CommitAttributeEditor(getModel(), taskAttribute);
        } else if (taskAttribute.getId().equals(TaskAttribute.COMMENT_TEXT)) {
          return new CommentAttributeEditor(getModel(), taskAttribute);
        } else if (taskAttribute.getId().startsWith(AppraiseReviewTaskSchema.PREFIX_DIFF)) {
          return new DiffAttributeEditor(getModel(), taskAttribute);
        } else {
          AbstractAttributeEditor editor = super.createEditor(type, taskAttribute);
          editor.setLayoutHint(new LayoutHint(RowSpan.SINGLE, ColumnSpan.SINGLE));
          return editor;
        }
      }
    };
  }

  @Override
  protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
    Set<TaskEditorPartDescriptor> descriptors = new LinkedHashSet<TaskEditorPartDescriptor>();
    Set<TaskEditorPartDescriptor> superDescriptors = super.createPartDescriptors();
    TaskEditorPartDescriptor commentsDescriptor = null;
    TaskEditorPartDescriptor newCommentsDescriptor = null;
    for (TaskEditorPartDescriptor taskEditorPartDescriptor : superDescriptors) {
      TaskEditorPartDescriptor descriptor = getNewDescriptor(taskEditorPartDescriptor);
      if (descriptor != null) {
        if (ID_PART_COMMENTS.equals(descriptor.getId())) {
          commentsDescriptor = descriptor;
        } else if (ID_PART_NEW_COMMENT.equals(descriptor.getId())) {
          newCommentsDescriptor = descriptor;
        } else {
          descriptors.add(descriptor);
        }
      }
    }
    descriptors.add(new TaskEditorPartDescriptor("com.google.appraise.eclipse.ui.diff") {
      @Override
      public AbstractTaskEditorPart createPart() {
        return new AppraiseDiffViewerPart();
      }
    });
    if (commentsDescriptor != null) {
      descriptors.add(commentsDescriptor);
    }
    if (newCommentsDescriptor != null) {
      descriptors.add(newCommentsDescriptor);
    }
    return descriptors;
  }

  private TaskEditorPartDescriptor getNewDescriptor(TaskEditorPartDescriptor descriptor) {
    if (PATH_ACTIONS.equals(descriptor.getPath()) || PATH_PEOPLE.equals(descriptor.getPath())) {
      return null;
    } else if (ID_PART_ATTRIBUTES.equals(descriptor.getId())) {
      return new TaskEditorPartDescriptor(ID_PART_ATTRIBUTES) {
        @Override
        public AbstractTaskEditorPart createPart() {
          return new AppraiseReviewAttributePart();
        }
      };
    } else if (ID_PART_COMMENTS.equals(descriptor.getId())) {
      return new TaskEditorPartDescriptor(ID_PART_COMMENTS) {
        @Override
        public AbstractTaskEditorPart createPart() {
          return new AppraiseTaskEditorCommentPart();
        }
      };
    }
    return descriptor;
  }
}
