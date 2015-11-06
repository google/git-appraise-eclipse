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

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * Common utility routines for working with Jgit.
 */
public class JgitUtils {
  
  // Some retry stuff.
  public static final int MAX_LOCK_FAILURE_CALLS = 10;
  public static final int SLEEP_ON_LOCK_FAILURE_MS = 25;

  /**
   * Updates the given ref to the new commit.
   */
  public static RefUpdate updateRef(Repository repo, ObjectId newObjectId,
      ObjectId expectedOldObjectId, String refName) throws IOException {
    RefUpdate refUpdate = repo.updateRef(refName);
    refUpdate.setNewObjectId(newObjectId);
    if (expectedOldObjectId == null) {
      refUpdate.setExpectedOldObjectId(ObjectId.zeroId());
    } else {
      refUpdate.setExpectedOldObjectId(expectedOldObjectId);
    }
    return refUpdate;
  }
}
