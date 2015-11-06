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

import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorAttributePart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

import java.util.Collection;
import java.util.List;

/**
 * The custom attributes part.
 */
public class AppraiseReviewAttributePart extends TaskEditorAttributePart {
  private final AppraiseReviewTaskSchema schema;

  public AppraiseReviewAttributePart() {
    super();
    schema = AppraiseReviewTaskSchema.getDefault();
  }

  @Override
  protected List<TaskAttribute> getOverlayAttributes() {
    List<TaskAttribute> attributes = super.getOverlayAttributes();
    TaskAttribute root = getModel().getTaskData().getRoot();
    
    TaskAttribute requester = root.getAttribute(schema.REQUESTER.getKey());
    attributes.add(requester);

    TaskAttribute reviewers = root.getAttribute(schema.REVIEWERS.getKey());
    attributes.add(reviewers);

    return attributes;
  }

  @Override
  protected Collection<TaskAttribute> getAttributes() {
    List<TaskAttribute> attributes = super.getOverlayAttributes();
    TaskAttribute root = getModel().getTaskData().getRoot();

    TaskAttribute reviewCommit = root.getAttribute(schema.REVIEW_COMMIT.getKey());
    attributes.add(reviewCommit);

    TaskAttribute requester = root.getAttribute(schema.REQUESTER.getKey());
    attributes.add(requester);

    TaskAttribute reviewers = root.getAttribute(schema.REVIEWERS.getKey());
    attributes.add(reviewers);

    TaskAttribute reviewRef = root.getAttribute(schema.REVIEW_REF.getKey());
    attributes.add(reviewRef);

    TaskAttribute submitted = root.getAttribute(schema.IS_SUBMITTED.getKey());
    attributes.add(submitted);

    TaskAttribute targetRef = root.getAttribute(schema.TARGET_REF.getKey());
    attributes.add(targetRef);

    return attributes;
  }
}
