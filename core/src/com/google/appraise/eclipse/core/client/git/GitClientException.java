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
package com.google.appraise.eclipse.core.client.git;

/**
 * Custom exception thrown by the git client for Appraise.
 */
public class GitClientException extends Exception {

  private static final long serialVersionUID = 4935975503414735796L;

  public GitClientException() {
    super();
  }
  
  public GitClientException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public GitClientException(String message) {
    super(message);
  }
  
  public GitClientException(Throwable cause) {
    super(cause);
  }
}
