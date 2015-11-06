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
 * Represents an Appraise user (which currently is basically a Git user).
 */
public class User {

  private String userName;
  private String email;
  
  public User(String userName, String email) {
    this.userName = userName;
    this.email = email;
  }

  public String getUserName() {
    return userName;
  }
  
  public void setUserName(String userName) {
    this.userName = userName;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(String email) {
    this.email = email;
  }
}
