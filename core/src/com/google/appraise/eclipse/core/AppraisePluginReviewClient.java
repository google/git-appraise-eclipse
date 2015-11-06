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
import com.google.appraise.eclipse.core.client.data.User;
import com.google.appraise.eclipse.core.client.git.AppraiseGitReviewClient;
import com.google.appraise.eclipse.core.client.git.GitClientException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Talks to the git notes to read Appraise reviews and their comments from
 * a specific {@link TaskRepository}.
 */
public class AppraisePluginReviewClient {
  private static final String CREATE_REVIEW_WARNING =
      "Creating a new review will push the code to be reviewed from the review branch, "
      + "sync git-notes, merge if necessary, and push a new git-notes commit "
      + "on the refs/notes/devtools/review ref.";

  private static final String UPDATE_REVIEW_WARNING =
      "Updating a review with new values and comments will "
      + "sync git-notes, merge if necessary, and push a new git-notes commit "
      + "on the refs/notes/devtools/review ref.";

  private static final String WRITE_COMMENTS_WARNING =
      "Writing comments will sync git-notes, merge if necessary, and push a new git-notes commit "
      + "on the refs/notes/devtools/discuss ref.";

  private Repository gitRepo;

  private AppraiseGitReviewClient gitClient;

  private User currentUser;

  public AppraisePluginReviewClient(TaskRepository repository) throws GitClientException {
    this.gitRepo = AppraisePluginUtils.getGitRepoForRepository(repository);
    if (this.gitRepo == null) {
      throw new GitClientException(
          "No git repository connected to " + repository.getRepositoryUrl());
    }
    this.gitClient = new AppraiseGitReviewClient(this.gitRepo);

    String currentUserName = gitRepo.getConfig().getString("user", null, "name");
    String currentUserEmail = gitRepo.getConfig().getString("user", null, "email");
    this.currentUser = new User(currentUserName, currentUserEmail);
  }

  /**
   * Retrieves all the reviews in the current project's repository.
   */
  public List<ReviewResult> listReviews() {
    try {
      Map<String, Review> reviews = gitClient.listReviews();
      List<ReviewResult> results = new ArrayList<>();
      for (Map.Entry<String, Review> reviewEntry : reviews.entrySet()) {
        results.add(new ReviewResult(reviewEntry.getKey(), currentUser, reviewEntry.getValue()));
      }
      Collections.sort(results, new Comparator<ReviewResult>() {
        @Override
        public int compare(ReviewResult first, ReviewResult second) {
          return (int) (second.getReview().getTimestamp() - first.getReview().getTimestamp());
        }
      });
      return results;
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Error loading reviews", e);
      return null;
    }
  }

  /**
   * Retrieves a specific review from the git notes. Returns null if not found.
   */
  public ReviewResult getReview(String hash) {
    try {
      Review review = gitClient.getReview(hash);
      return new ReviewResult(hash, currentUser, review);
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Failed to load review " + hash, e);
      return null;
    }
  }

  /**
   * Gets all the comments for a specific review by hash.
   */
  public List<ReviewCommentResult> listCommentsForReview(String hash) {
    List<ReviewCommentResult> comments = new ArrayList<>();
    try {
      Map<String, ReviewComment> commentsData = gitClient.listCommentsForReview(hash);
      for (Map.Entry<String, ReviewComment> commentData : commentsData.entrySet()) {
        comments.add(new ReviewCommentResult(commentData.getKey(), commentData.getValue()));
      }
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Error loading domments for " + hash, e);
      return null;
    }
    return comments;
  }

  /**
   * Writes a comment to the specified review by taking comment text out of the
   * given task attribute.
   * @param taskId Is the review commit hash in our model.
   * @param newComments The comment to append inside a task attribute.
   * @return whether the comment was written out or not.
   */
  public boolean writeComment(String taskId, TaskAttribute newComments) {
    if (!displayWriteWarning(WRITE_COMMENTS_WARNING)) {
      return false;
    }
    try {
      gitClient.writeComment(taskId, newComments.getValue());
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Error writing comment for " + taskId, e);
      return false;
    }
    return true;
  }

  /**
   * Writes a comment to the specified review.
   * @param taskId Is the review commit hash in our model.
   * @param comment The comment to append.
   * @return whether the comment was written out or not.
   */
  public boolean writeComment(String taskId, ReviewComment comment) {
    if (!displayWriteWarning(WRITE_COMMENTS_WARNING)) {
      return false;
    }
    try {
      gitClient.writeComment(taskId, comment);
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Error writing comment for " + taskId, e);
      return false;
    }
    return true;
  }

  /**
   * Writes a {@link Review} to the git notes.
   * @return the new review's hash.
   */
  public String writeReview(String reviewCommitHash, Review review) throws GitClientException {
    if (!displayWriteWarning(CREATE_REVIEW_WARNING)) {
      return null;
    }
    return gitClient.createReview(reviewCommitHash, review);
  }

  /**
   * Checks an existing review to potentially be updated, and write a new comment if given.
   * @return whether or not the review was updated.
   */
  public boolean updateReview(String reviewCommitHash, Review review, String newComment) {
    if (!displayWriteWarning(UPDATE_REVIEW_WARNING)) {
      return false;
    }
    try {
      gitClient.updateReviewWithComment(reviewCommitHash, review, newComment);
    } catch (GitClientException e) {
      AppraiseConnectorPlugin.logError("Error updating review " + reviewCommitHash, e);
      return false;
    }
    return true;
  }
  
  /**
   * Gets the commit that we will write review notes and comments to.
   */
  public RevCommit getReviewCommit(String reviewBranch, String targetBranch)
      throws GitClientException {
    return gitClient.getReviewCommit(reviewBranch, targetBranch);
  }

  /**
   * Gets the diff between review and target branches.
   */
  public List<DiffEntry> getReviewDiffs(String reviewBranch, String targetBranch)
      throws GitClientException {
    return gitClient.calculateBranchDiffs(targetBranch, reviewBranch);
  }

  /**
   * Returns whether or not the given review has been submitted. Conventionally,
   * this means that the review commit is an ancestor of the target ref.
   */
  public boolean isReviewSubmitted(ReviewResult review) throws GitClientException {
    if (review.getReview().getTargetRef() == null || review.getReview().getTargetRef().isEmpty()) {
      return false;
    }
    return gitClient.areAncestorDescendent(review.getHash(), review.getReview().getTargetRef());
  }

  /**
   * Confirms that the repository is in a valid state to request a code review.
   */
  public boolean canRequestReview(TaskData taskData) {
    String reviewRef =
        taskData.getRoot()
            .getAttribute(AppraiseReviewTaskSchema.getDefault().REVIEW_REF.getKey())
            .getValue();
    String targetRef =
        taskData.getRoot()
            .getAttribute(AppraiseReviewTaskSchema.getDefault().TARGET_REF.getKey())
            .getValue();
    return gitClient.canRequestReviewOnReviewRef(reviewRef, targetRef);
  }

  private boolean displayWriteWarning(final String message) {
    final AtomicBoolean result = new AtomicBoolean(false);
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        result.set(MessageDialog.openConfirm(null, "Appraise Review Plugin", message));
      }
    });
    return result.get();
  }
}
