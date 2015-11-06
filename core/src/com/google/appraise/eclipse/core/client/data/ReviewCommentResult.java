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
 * Encapsulates a {@link ReviewComment} with additional data (e.g. the SHA-1
 * hash of the JSON to use as an id).
 */
public class ReviewCommentResult {
  private String id;
  private ReviewComment comment;

  public ReviewCommentResult(String id, ReviewComment comment) {
    this.id = id;
    this.comment = comment;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ReviewComment getComment() {
    return comment;
  }

  public void setComment(ReviewComment comment) {
    this.comment = comment;
  }  
}
