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

import com.google.appraise.eclipse.core.client.data.Review;
import com.google.appraise.eclipse.core.client.data.ReviewComment;
import com.google.appraise.eclipse.core.client.data.ReviewCommentResult;
import com.google.appraise.eclipse.core.client.data.ReviewResult;
import com.google.appraise.eclipse.core.client.git.GitClientException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema.Field;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The task data handler for Appraise reviews.
 */
public class AppraiseReviewsTaskDataHandler extends AbstractTaskDataHandler {
  /**
   * Ref to the master branch, which is special.
   */
  private static final String MASTER_REF = "refs/heads/master";

  /**
   * Stash a reference to the default schema for convenience.
   */
  private AppraiseReviewTaskSchema schema;

  public AppraiseReviewsTaskDataHandler() {
    super();
    this.schema = AppraiseReviewTaskSchema.getDefault();
  }

  @Override
  public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData,
      Set<TaskAttribute> oldAttributes, IProgressMonitor monitor) throws CoreException {
    AppraisePluginReviewClient client;
    try {
      client = new AppraisePluginReviewClient(repository);
    } catch (GitClientException e) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Failed to initialize git client", e));
    }

    String taskId;
    if (taskData.isNew()) {
      taskId = createNewReview(taskData, client);
    } else {
      taskId = updateExistingReview(taskData, client);
    }
    return new RepositoryResponse(RepositoryResponse.ResponseKind.TASK_UPDATED, taskId);
  }

  /**
   * Helper method for creating a new comment and writing it out.
   */
  private String updateExistingReview(TaskData taskData, AppraisePluginReviewClient client)
      throws CoreException {
    String reviewCommitHash = getReviewCommitHash(taskData);
    Review review = buildReviewFromTaskData(taskData);
    String newComment = null;
    TaskAttribute newComments = taskData.getRoot().getAttribute(TaskAttribute.COMMENT_NEW);
    if (newComments != null) {
      newComment = newComments.getValue();
    }
    if (!client.updateReview(reviewCommitHash, review, newComment)) {
      throw new CoreException(Status.CANCEL_STATUS);
    }
    return reviewCommitHash;
  }

  /**
   * Helper method for creating a new review and writing it out.
   */
  private String createNewReview(TaskData taskData, AppraisePluginReviewClient client)
      throws CoreException {
    boolean canRequestReview = client.canRequestReview(taskData);
    if (!canRequestReview) {
      throw new CoreException(Status.CANCEL_STATUS);
    }

    // Create a new review.
    String taskId = null;
    try {
      Review review = buildReviewFromTaskData(taskData);
      review.setTimestamp(System.currentTimeMillis() / 1000);
      String reviewCommitHash = getReviewCommitHash(taskData);
      taskId = client.writeReview(reviewCommitHash, review);
      if (taskId == null) {
        throw new CoreException(Status.CANCEL_STATUS);
      }
    } catch (GitClientException e) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Failed to write review", e));
    }
    return taskId;
  }

  @Override
  public boolean initializeTaskData(TaskRepository repository, TaskData taskData,
      ITaskMapping initializationData, IProgressMonitor monitor) throws CoreException {
    schema.initialize(taskData);

    if (taskData.isNew()) {
      createNewReviewTask(repository, taskData);
    }
    return true;
  }

  /**
   * Helper method to get the review commit hash from task data.
   */
  private String getReviewCommitHash(TaskData taskData) {
    String reviewCommitHash =
        taskData.getRoot()
            .getAttribute(AppraiseReviewTaskSchema.getDefault().REVIEW_COMMIT.getKey())
            .getValue();
    return reviewCommitHash;
  }

  /**
   * Helper method to build a review from the given task data instance's attributes.
   */
  private Review buildReviewFromTaskData(TaskData taskData) {
    TaskAttribute root = taskData.getRoot();
    Review review = new Review();
    review.setRequester(
        root.getAttribute(AppraiseReviewTaskSchema.getDefault().REQUESTER.getKey()).getValue());
    review.setReviewRef(
        root.getAttribute(AppraiseReviewTaskSchema.getDefault().REVIEW_REF.getKey()).getValue());
    review.setTargetRef(
        root.getAttribute(AppraiseReviewTaskSchema.getDefault().TARGET_REF.getKey()).getValue());
    review.setDescription(
        root.getAttribute(AppraiseReviewTaskSchema.getDefault().DESCRIPTION.getKey()).getValue());
    review.setReviewersString(
        root.getAttribute(AppraiseReviewTaskSchema.getDefault().REVIEWERS.getKey()).getValue());

    return review;
  }

  /**
   * Sets up a new review on the current branch, as a task.
   */
  private void createNewReviewTask(TaskRepository repository, TaskData taskData)
      throws CoreException {
    Repository repo = AppraisePluginUtils.getGitRepoForRepository(repository);
    AppraisePluginReviewClient client;

    try {
      client = new AppraisePluginReviewClient(repository);
    } catch (GitClientException e1) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Failed to initialize git client"));
    }

    // Reviews get created on the current branch and point to the master ref by default.
    String currentBranch;
    try {
      currentBranch = repo.getFullBranch();
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Error retrieving current branch", e));
    }
    if (MASTER_REF.equals(currentBranch)) {
      throw new CoreException(new Status(IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Cannot create review on master branch"));
    }
    setAttributeValue(taskData, schema.REQUESTER, repository.getUserName());
    setAttributeValue(taskData, schema.TARGET_REF, MASTER_REF);
    setAttributeValue(taskData, schema.REVIEW_REF, currentBranch);

    // Review notes get written on the first commit that differs from the master.
    RevCommit reviewCommit;
    try {
      reviewCommit = client.getReviewCommit(currentBranch, MASTER_REF);
    } catch (GitClientException e) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Cannot find a merge base", e));
    }
    if (reviewCommit == null) {
      throw new CoreException(new Status(IStatus.INFO, AppraiseConnectorPlugin.PLUGIN_ID,
          "No commits to review on " + currentBranch));
    }
    setAttributeValue(taskData, schema.DESCRIPTION, reviewCommit.getFullMessage());
    setAttributeValue(taskData, schema.REVIEW_COMMIT, reviewCommit.getName());

    // TODO: If the user changes the target branch, we should refresh the diffs
    // on the task data.
    try {
      List<DiffEntry> diffs = client.getReviewDiffs(currentBranch, MASTER_REF);
      populateDiffs(repository, diffs, taskData);
    } catch (Exception e) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Failed to load review diffs", e));
    }
  }

  @Override
  public TaskAttributeMapper getAttributeMapper(TaskRepository repository) {
    return new TaskAttributeMapper(repository);
  }

  /**
   * Convenience method to set the value of a given Attribute in the given {@link TaskData}.
   */
  private void setAttributeValue(TaskData data, Field attr, String value) {
    TaskAttribute attribute = data.getRoot().getAttribute(attr.getKey());
    setAttributeValue(attribute, value);
  }

  /**
   * Helper method for setting attribute values (mostly because nulls aren't allowed in
   * attribute values).
   */
  private void setAttributeValue(TaskAttribute attribute, String value) {
    if (value != null) {
      attribute.setValue(value);
    }
  }

  /**
   * Creates partial task data suitable for the list view. This excludes comments.
   */
  public TaskData createPartialTaskData(TaskRepository repository, ReviewResult review) {
    TaskData taskData = new TaskData(getAttributeMapper(repository), repository.getConnectorKind(),
        repository.getRepositoryUrl(), review.getHash());
    schema.initialize(taskData);
    taskData.setPartial(true);
    populateTaskData(taskData, review, repository);
    return taskData;
  }

  private void populateTaskData(TaskData taskData, ReviewResult review, TaskRepository repository) {
    setAttributeValue(taskData, schema.REQUESTER, review.getReview().getRequester());
    setAttributeValue(taskData, schema.REVIEW_REF, review.getReview().getReviewRef());
    setAttributeValue(taskData, schema.TARGET_REF, review.getReview().getTargetRef());
    setAttributeValue(taskData, schema.REVIEW_COMMIT, review.getHash());
    setAttributeValue(taskData, schema.DESCRIPTION, review.getReview().getDescription());
    if (review.getReview().getTimestamp() > 0) {
      Date date = new Date(review.getReview().getTimestamp() * 1000);
      setAttributeValue(taskData, schema.CREATED, Long.toString(date.getTime()));
    }

    IProject project = AppraisePluginUtils.getProjectForRepository(repository);
    TaskAttribute productAttr = taskData.getRoot().createAttribute(TaskAttribute.PRODUCT);
    setAttributeValue(productAttr, project.getName());

    taskData.getRoot()
        .createAttribute(TaskAttribute.COMMENT_NEW)
        .getMetaData()
        .setType(TaskAttribute.TYPE_LONG_RICH_TEXT)
        .setReadOnly(false);

    TaskAttribute reviewersAttr = taskData.getRoot().getAttribute(schema.REVIEWERS.getKey());
    setAttributeValue(reviewersAttr, review.getReview().getReviewersString());
  }

  private IRepositoryPerson createPerson(String userName, TaskRepository repository) {
    IRepositoryPerson person = repository.createPerson(userName);
    person.setName(userName);
    return person;
  }

  /**
   * Creates the full task data (including comments and diffs).
   */
  public TaskData createFullTaskData(TaskRepository repository, ReviewResult review,
      List<ReviewCommentResult> comments, List<DiffEntry> diffs, boolean isSubmitted) {
    TaskData taskData = new TaskData(getAttributeMapper(repository), repository.getConnectorKind(),
        repository.getRepositoryUrl(), review.getHash());
    schema.initialize(taskData);
    populateTaskData(taskData, review, repository);

    setAttributeValue(taskData, schema.IS_SUBMITTED, Boolean.toString(isSubmitted));
    populateDiffs(repository, diffs, taskData);
    populateComments(repository, comments, taskData, review.getReview().getTimestamp());

    return taskData;
  }

  /**
   * Fills the comments into the given task data.
   */
  private void populateComments(
      TaskRepository repository, List<ReviewCommentResult> comments, TaskData taskData,
      long reviewTimestamp) {
    int commentCount = 1;
    long maxCommentTimestamp = reviewTimestamp;
    
    for (ReviewCommentResult comment : comments) {
      TaskCommentMapper commentMapper = new TaskCommentMapper();
      ReviewComment commentData = comment.getComment();
      if (commentData.getAuthor() != null) {
        commentMapper.setAuthor(createPerson(commentData.getAuthor(), repository));
      }
      
      Date timestamp = new Date(commentData.getTimestamp() * 1000);
      commentMapper.setCreationDate(timestamp);
      maxCommentTimestamp = Math.max(maxCommentTimestamp, commentData.getTimestamp());

      if (commentData.getDescription() != null) {
        commentMapper.setText(commentData.getDescription());
      } else {
        // Null comments seem to not be allowed.
        commentMapper.setText("");
      }
      commentMapper.setCommentId("" + commentCount);
      commentMapper.setNumber(commentCount);

      TaskAttribute commentAttribute =
          taskData.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT + commentCount);
      if (commentData.getResolved() != null) {
        TaskAttribute resolvedAttribute =
            commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_RESOLVED_ATTRIBUTE);
        setAttributeValue(resolvedAttribute, Boolean.toString(commentData.getResolved()));
      }

      TaskAttribute idAttribute =
          commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_ID_ATTRIBUTE);
      setAttributeValue(idAttribute, comment.getId());

      if (commentData.getParent() != null) {
        TaskAttribute parentAttribute =
            commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_PARENT_ATTRIBUTE);
        setAttributeValue(parentAttribute, commentData.getParent());
      }

      if (commentData.getLocation() != null) {
        TaskAttribute locationFileAttr =
            commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_LOCATION_FILE);
        setAttributeValue(locationFileAttr, commentData.getLocation().getPath());

        TaskAttribute locationLineAttr =
            commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_LOCATION_LINE);
        if (commentData.getLocation().getRange() != null) {
          setAttributeValue(
              locationLineAttr, "" + commentData.getLocation().getRange().getStartLine());
        }

        TaskAttribute locationCommitAttr =
            commentAttribute.createAttribute(AppraiseReviewTaskSchema.COMMENT_LOCATION_COMMIT);
        setAttributeValue(locationCommitAttr, commentData.getLocation().getCommit());
      }

      commentMapper.applyTo(commentAttribute);
      commentCount++;
    }
    
    Date date = new Date(maxCommentTimestamp * 1000);
    setAttributeValue(taskData, schema.MODIFIED, Long.toString(date.getTime()));
  }

  /**
   * Fills the diffs into the given task data.
   */
  private void populateDiffs(TaskRepository repository, List<DiffEntry> diffs, TaskData taskData) {
    int diffCount = 1;
    for (DiffEntry diffEntry : diffs) {
      TaskAttribute diffAttribute =
          taskData.getRoot().createAttribute(AppraiseReviewTaskSchema.PREFIX_DIFF + diffCount);
      diffAttribute.getMetaData().setType(AppraiseReviewTaskSchema.TYPE_DIFF);

      TaskAttribute diffNewPathAttribute =
          diffAttribute.createAttribute(AppraiseReviewTaskSchema.DIFF_NEWPATH);
      setAttributeValue(diffNewPathAttribute, diffEntry.getNewPath());

      TaskAttribute diffOldPathAttribute =
          diffAttribute.createAttribute(AppraiseReviewTaskSchema.DIFF_OLDPATH);
      setAttributeValue(diffOldPathAttribute, diffEntry.getNewPath());

      TaskAttribute diffTypeAttribute =
          diffAttribute.createAttribute(AppraiseReviewTaskSchema.DIFF_TYPE);
      setAttributeValue(diffTypeAttribute, diffEntry.getChangeType().name());

      TaskAttribute diffTextAttribute =
          diffAttribute.createAttribute(AppraiseReviewTaskSchema.DIFF_TEXT);
      ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();
      try (DiffFormatter formatter = new DiffFormatter(diffOutputStream)) {
        formatter.setRepository(AppraisePluginUtils.getGitRepoForRepository(repository));
        try {
          formatter.format(diffEntry);
          String diffText = new String(diffOutputStream.toByteArray(), "UTF-8");
          setAttributeValue(diffTextAttribute, diffText);
        } catch (IOException e) {
          AppraiseConnectorPlugin.logWarning(
              "Failed to load a diff for " + taskData.getTaskId(), e);
        }
      }
      diffCount++;
    }
  }
}
