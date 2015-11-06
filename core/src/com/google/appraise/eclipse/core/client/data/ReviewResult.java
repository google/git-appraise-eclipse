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
package com.google.appraise.eclipse.core.client.data;

import java.util.Arrays;
import java.util.List;

/**
 * Appraise review result, which exposes the hash as an id for the UI to use as a handle.
 */
public class ReviewResult {
  private Review review;
  private String hash;
  private User currentUser;

  public ReviewResult(String hash, User currentUser, Review noteData) {
    this.currentUser = currentUser;
    this.hash = hash;
    this.review = noteData;
  }

  public Review getReview() {
    return review;
  }

  public void setReview(Review review) {
    this.review = review;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public boolean isCurrentUserReviewer() {
    if (currentUser == null) {
      return false;
    }
    if (review.getReviewers() == null) {
      return false;
    }
    List<String> reviewers = Arrays.asList(review.getReviewers());
    return (reviewers.contains(currentUser.getUserName())
        || reviewers.contains(currentUser.getEmail()));
  }

  public boolean isCurrentUserRequester() {
    if (currentUser == null) {
      return false;
    }
    return (currentUser.getUserName().equals(review.getRequester())
        || currentUser.getEmail().equals(review.getRequester()));
  }
}
