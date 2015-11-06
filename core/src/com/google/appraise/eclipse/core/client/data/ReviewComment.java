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

/**
 * Appraise review comment data object.
 */
public class ReviewComment {
  private String author;
  private long timestamp;
  private Boolean resolved;
  private ReviewCommentLocation location;
  private String description;
  
  /**
   * This (if set) is the id of a comment that this comment is in reply to.
   * See the javadoc in {@link ReviewCommentResult}.
   */
  private String parent;
  
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAuthor() {
    return author;
  }
  
  public void setAuthor(String author) {
    this.author = author;
  }
  
  public long getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  
  public Boolean getResolved() {
    return resolved;
  }
  
  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
  }
  
  public ReviewCommentLocation getLocation() {
    return location;
  }
  
  public void setLocation(ReviewCommentLocation location) {
    this.location = location;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }
}

