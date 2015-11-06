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
package com.google.appraise.eclipse.core;

import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema;
import org.eclipse.mylyn.tasks.core.data.DefaultTaskSchema;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * The field definitions for Appraise reviews.
 */
public class AppraiseReviewTaskSchema extends AbstractTaskSchema {
  private static final AppraiseReviewTaskSchema instance = new AppraiseReviewTaskSchema();

  public static AppraiseReviewTaskSchema getDefault() {
    return instance;
  }

  private final DefaultTaskSchema parent = DefaultTaskSchema.getInstance();

  public static final String COMMENT_ID_ATTRIBUTE = "appraise.comment.id";

  public static final String COMMENT_RESOLVED_ATTRIBUTE = "appraise.comment.resolved";

  public static final String COMMENT_PARENT_ATTRIBUTE = "appraise.comment.parent";

  public static final String COMMENT_LOCATION_FILE = "appraise.comment.location.file";

  public static final String COMMENT_LOCATION_LINE = "appraise.comment.location.line";

  public static final String COMMENT_LOCATION_COMMIT = "appraise.comment.location.commit";

  public static final String DIFF_TEXT = "appraise.diff.text";

  public static final String DIFF_NEWPATH = "appraise.diff.newpath";

  public static final String DIFF_OLDPATH = "appraise.diff.oldpath";

  public static final String DIFF_TYPE = "appraise.diff.type";

  public static final String TYPE_DIFF = "appraise.diff";

  /**
   * The prefix for elements of the unified diff for all the commits in the review.
   * Not populated in the partial task data.
   */
  public static final String PREFIX_DIFF = "com.google.appraise.review.Diff-";

  public final Field IS_SUBMITTED =
      createField("com.google.appraise.review.IsSubmitted", "Submitted", TaskAttribute.TYPE_BOOLEAN,
          Flag.READ_ONLY);

  public final Field REVIEW_REF =
      createField("com.google.appraise.review.ReviewRef", "Review Ref", TaskAttribute.TYPE_SHORT_TEXT);

  public final Field TARGET_REF =
      createField("com.google.appraise.review.TargetRef", "Target Ref", TaskAttribute.TYPE_SHORT_TEXT);

  public final Field DESCRIPTION = inheritFrom(parent.SUMMARY).create();

  public final Field CREATED = inheritFrom(parent.DATE_CREATION).create();

  public final Field MODIFIED = inheritFrom(parent.DATE_MODIFICATION).create();
  
  public final Field REQUESTER =
      createField("com.google.appraise.review.Requester", "Requester", TaskAttribute.TYPE_SHORT_TEXT);

  public final Field REVIEWERS =
      createField("com.google.appraise.review.Reviewers", "Reviewers", TaskAttribute.TYPE_SHORT_TEXT);

  public final Field REVIEW_COMMIT = createField("com.google.appraise.review.ReviewCommit",
      "Review Commit", TaskAttribute.TYPE_SHORT_TEXT, Flag.READ_ONLY);
  
  public final Field KIND = inheritFrom(parent.TASK_KIND).create();
}
