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
 * Appraise review comment location range data object.
 */
public class ReviewCommentLocationRange {
  private int startLine;

  public int getStartLine() {
    return startLine;
  }
  
  public void setStartLine(int startLine) {
    this.startLine = startLine;
  }
}
