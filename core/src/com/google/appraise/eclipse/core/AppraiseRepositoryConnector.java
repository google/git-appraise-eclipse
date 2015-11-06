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

import com.google.appraise.eclipse.core.client.data.ReviewCommentResult;
import com.google.appraise.eclipse.core.client.data.ReviewResult;
import com.google.appraise.eclipse.core.client.git.AppraiseGitReviewClient;
import com.google.appraise.eclipse.core.client.git.GitClientException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

import java.util.Date;
import java.util.List;

/**
 * The Appraise review repository connector implementation.
 */
public class AppraiseRepositoryConnector extends AbstractRepositoryConnector {
  private final AppraiseReviewsTaskDataHandler taskDataHandler;

  public AppraiseRepositoryConnector() {
    taskDataHandler = new AppraiseReviewsTaskDataHandler();
  }

  @Override
  public boolean canCreateNewTask(TaskRepository repository) {
    return true;
  }

  @Override
  public boolean canCreateTaskFromKey(TaskRepository repository) {
    return true;
  }

  @Override
  public String getConnectorKind() {
    return AppraiseConnectorPlugin.CONNECTOR_KIND;
  }

  @Override
  public String getLabel() {
    return "Appraise Reviews";
  }

  @Override
  public String getRepositoryUrlFromTaskUrl(String taskUrl) {
    return null;
  }

  @Override
  public TaskData getTaskData(TaskRepository repository, String taskIdOrKey,
      IProgressMonitor monitor) throws CoreException {
    AppraisePluginReviewClient client;
    try {
      client = getReviewClient(repository);
    } catch (GitClientException e) {
      throw new CoreException(new Status(IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Failed to initialize git client" + taskIdOrKey, e));
    }
    
    ReviewResult review = client.getReview(taskIdOrKey);
    if (review == null) {
      throw new CoreException(new Status(
          IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID, "Failed to review " + taskIdOrKey));
    }

    List<ReviewCommentResult> comments;
    try {
      comments = client.listCommentsForReview(taskIdOrKey);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Failed to load review comments for " + taskIdOrKey, e));
    }

    List<DiffEntry> diffs;
    try {
      diffs =
          new AppraiseGitReviewClient(AppraisePluginUtils.getGitRepoForRepository(repository))
              .getDiff(taskIdOrKey);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Failed to load review diffs for " + taskIdOrKey, e));
    }

    boolean isSubmitted = false;
    try {
      isSubmitted = client.isReviewSubmitted(review);
    } catch (Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Failed check is-submitted for " + taskIdOrKey, e));
    }

    return taskDataHandler.createFullTaskData(repository, review, comments, diffs, isSubmitted);
  }

  @Override
  public String getTaskIdFromTaskUrl(String taskUrl) {
    return null;
  }

  @Override
  public String getTaskUrl(String repositoryUrl, String taskIdOrKey) {
    return null;
  }

  @Override
  public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {
    Date repositoryDate = getTaskMapping(taskData).getModificationDate();
    Date localDate = task.getModificationDate();

    if (repositoryDate == null)
      return false;

    return !repositoryDate.equals(localDate);
  }

  @Override
  public IStatus performQuery(TaskRepository repository, IRepositoryQuery query,
      TaskDataCollector collector, ISynchronizationSession session, IProgressMonitor monitor) {
    AppraisePluginReviewClient client;

    try {
      client = getReviewClient(repository);
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Failed to initialize git client", e);
      return Status.CANCEL_STATUS;
    }
    
    boolean reviewer =
        Boolean.parseBoolean(query.getAttribute(AppraiseConnectorPlugin.QUERY_REVIEWER));
    boolean requester =
        Boolean.parseBoolean(query.getAttribute(AppraiseConnectorPlugin.QUERY_REQUESTER));
    String reviewCommitPrefix =
        query.getAttribute(AppraiseConnectorPlugin.QUERY_REVIEW_COMMIT_PREFIX);

    List<ReviewResult> reviews = client.listReviews();
    if (reviews == null) {
      return new Status(Status.ERROR, AppraiseConnectorPlugin.PLUGIN_ID,
          "Error running review list query");
    }
    
    for (ReviewResult review : reviews) {
      TaskData taskData = taskDataHandler.createPartialTaskData(repository, review);
      boolean shouldAccept = false;
      if (!reviewer && !requester) {
        // Accept everything if no filters are set.
        shouldAccept = true;
      } else if (reviewer && review.isCurrentUserReviewer()) {
        shouldAccept = true;
      } else if (requester && review.isCurrentUserRequester()) {
        shouldAccept = true;
      }

      if (reviewCommitPrefix != null && !reviewCommitPrefix.isEmpty()) {
        shouldAccept = shouldAccept && review.getHash().startsWith(reviewCommitPrefix);
      }

      if (shouldAccept) {
        collector.accept(taskData);
      }
    }
    return Status.OK_STATUS;
  }

  @Override
  public void updateRepositoryConfiguration(TaskRepository taskRepository, IProgressMonitor monitor)
      throws CoreException {}

  @Override
  public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {
    Date oldModificationDate = task.getModificationDate();
    getTaskMapping(taskData).applyTo(task);
  }

  @Override
  public TaskMapper getTaskMapping(TaskData taskData) {
    return new AppraiseTaskMapper(taskData);
  }

  protected AppraisePluginReviewClient getReviewClient(TaskRepository taskRepository)
      throws GitClientException {
    return new AppraisePluginReviewClient(taskRepository);
  }

  @Override
  public AbstractTaskDataHandler getTaskDataHandler() {
    return taskDataHandler;
  }
}
