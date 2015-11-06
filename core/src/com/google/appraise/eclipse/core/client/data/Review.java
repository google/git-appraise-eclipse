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

/**
 * Appraise review data object.
 */
public class Review {
  private String reviewRef;
  private String targetRef;
  private String requester;
  private String[] reviewers;
  private String description;
  private long timestamp;

  public String getReviewRef() {
    return reviewRef;
  }

  public void setReviewRef(String reviewRef) {
    this.reviewRef = reviewRef;
  }

  public String getTargetRef() {
    return targetRef;
  }

  public void setTargetRef(String targetRef) {
    this.targetRef = targetRef;
  }

  public String getRequester() {
    return requester;
  }

  public void setRequester(String requester) {
    this.requester = requester;
  }

  public String[] getReviewers() {
    return reviewers;
  }

  public void setReviewers(String[] reviewers) {
    this.reviewers = reviewers;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Gets the list of reviews in a comma-delimited format.
   */
  public String getReviewersString() {
    StringBuilder sb = new StringBuilder();
    if (reviewers != null) {
      for (String reviewer : reviewers) {
        if (sb.length() > 0) {
          sb.append(',');
        }
        sb.append(reviewer);
      }
    }
    return sb.toString();
  }

  /**
   * Sets the list of reviewers from a comma-delimited format.
   */
  public void setReviewersString(String reviewersStr) {
    if (reviewersStr != null) {
      this.reviewers = reviewersStr.split(",");
    } else {
      this.reviewers = new String[] {};
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((requester == null) ? 0 : requester.hashCode());
    result = prime * result + ((reviewRef == null) ? 0 : reviewRef.hashCode());
    result = prime * result + Arrays.hashCode(reviewers);
    result = prime * result + ((targetRef == null) ? 0 : targetRef.hashCode());
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Review other = (Review) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (requester == null) {
      if (other.requester != null)
        return false;
    } else if (!requester.equals(other.requester))
      return false;
    if (reviewRef == null) {
      if (other.reviewRef != null)
        return false;
    } else if (!reviewRef.equals(other.reviewRef))
      return false;
    if (!Arrays.equals(reviewers, other.reviewers))
      return false;
    if (targetRef == null) {
      if (other.targetRef != null)
        return false;
    } else if (!targetRef.equals(other.targetRef))
      return false;
    if (timestamp != other.timestamp)
      return false;
    return true;
  }
}
