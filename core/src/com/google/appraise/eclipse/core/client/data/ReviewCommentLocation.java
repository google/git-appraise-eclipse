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
 * Appraise review comment location data object.
 */
public class ReviewCommentLocation {
  private String commit;
  private String path;
  private ReviewCommentLocationRange range;
  
  public String getCommit() {
    return commit;
  }
  
  public void setCommit(String commit) {
    this.commit = commit;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public ReviewCommentLocationRange getRange() {
    return range;
  }
  
  public void setRange(ReviewCommentLocationRange range) {
    this.range = range;
  }
}
