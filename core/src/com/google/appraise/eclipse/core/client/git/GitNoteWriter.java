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

import com.google.gson.Gson;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.DefaultNoteMerger;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.notes.NoteMapMerger;
import org.eclipse.jgit.notes.NoteMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes git notes in the format that Appraise expects.
 * This is based off an old version of Gerrit's CreateCodeReviewNotes
 * (https://code.google.com/p/gerrit/source/browse/gerrit-server/src/main/java/com/google/gerrit/server/git/CreateCodeReviewNotes.java),
 * which is about the only non-trivial git-notes Jgit example on the web.
 *
 * @param <T> The type of note to write, which will be serialized as a JSON
 * string and appended with a newline.
 */
public class GitNoteWriter<T> implements Closeable {
  private static final Logger logger = Logger.getLogger(GitNoteWriter.class.getName());

  /**
   * The ref where the notes are to be written.
   */
  private final String ref;

  // Some Jgit objects.
  private final Repository repo;
  private final RevWalk revWalk;
  private final ObjectInserter inserter;
  private final ObjectReader reader;

  // The base commit that we need to merge into.
  private RevCommit baseCommit;
  private NoteMap base;

  // The thing we need to merge.
  private RevCommit oursCommit;
  private NoteMap ours;

  private List<T> noteRecords;

  /**
   * Who is writing the notes out, which is assumed to be the same person making
   * the reviews/comments in the first place.
   */
  private PersonIdent author;

  /**
   * The commit that we want to attach to.
   */
  private final RevCommit reviewCommit;

  /**
   * Creates a writer to write comments to a given review.
   */
  public static <T> GitNoteWriter<T> createNoteWriter(
      String reviewCommitHash, final Repository db, PersonIdent author, String ref) {
    return new GitNoteWriter<T>(reviewCommitHash, db, ref, author);
  }

  /**
   * Private ctor. Use the static factory methods.
   */
  private GitNoteWriter(String reviewHash, final Repository repo, String ref, PersonIdent author) {
    this.ref = ref;
    this.repo = repo;
    this.author = author;

    revWalk = new RevWalk(repo);
    inserter = repo.newObjectInserter();
    reader = repo.newObjectReader();

    try {
      ObjectId reviewRefObjId = repo.resolve(reviewHash);
      this.reviewCommit = revWalk.parseCommit(reviewRefObjId);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to init note writer for commit " + reviewHash, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates the given notes in the pre-configured review.
   */
  public void create(String message, List<T> noteRecords) {
    try {
      this.noteRecords = noteRecords;
      loadBase();
      applyNotes(message);
      updateRef();
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Failed to write notes for commit " + this.reviewCommit.getId(), e);
    }
  }

  private void loadBase() throws IOException {
    Ref notesBranch = repo.getRef(ref);
    if (notesBranch != null) {
      baseCommit = revWalk.parseCommit(notesBranch.getObjectId());
      base = NoteMap.read(revWalk.getObjectReader(), baseCommit);
    }
    if (baseCommit != null) {
      ours = NoteMap.read(repo.newObjectReader(), baseCommit);
    } else {
      ours = NoteMap.newEmptyMap();
    }
  }

  private void applyNotes(String message) throws IOException {
    for (T c : noteRecords) {
      add(c);
    }
    commit(message.toString());
  }

  private void commit(String message) throws IOException {
    if (baseCommit != null) {
      oursCommit = createCommit(ours, author, message, baseCommit);
    } else {
      oursCommit = createCommit(ours, author, message);
    }
  }

  private void add(T noteRecord)
      throws MissingObjectException, IncorrectObjectTypeException, IOException, RuntimeException {
    ObjectId noteContent = createNoteContent(noteRecord);
    if (ours.contains(reviewCommit)) {
      // merge the existing and the new note as if they are both new
      // means: base == null
      // there is not really a common ancestry for these two note revisions
      // use the same NoteMerger that is used from the NoteMapMerger
      NoteMerger noteMerger = new DefaultNoteMerger();
      Note newNote = new Note(reviewCommit, noteContent);
      noteContent =
          noteMerger.merge(null, newNote, ours.getNote(reviewCommit), reader, inserter).getData();
    }
    ours.set(reviewCommit, noteContent);
  }

  private ObjectId createNoteContent(T noteRecord) throws RuntimeException {
    try {
      return inserter.insert(
          Constants.OBJ_BLOB, (new Gson().toJson(noteRecord) + '\n').getBytes("UTF-8"));
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Failed create note content for commit " + this.reviewCommit.getId(), e);
      throw new RuntimeException(e);
    }
  }

  private void updateRef() throws IOException, InterruptedException, RuntimeException,
                                  MissingObjectException, IncorrectObjectTypeException,
                                  CorruptObjectException {
    if (baseCommit != null && oursCommit.getTree().equals(baseCommit.getTree())) {
      // If the trees are identical, there is no change in the notes.
      // Avoid saving this commit as it has no new information.
      return;
    }

    int remainingLockFailureCalls = JgitUtils.MAX_LOCK_FAILURE_CALLS;
    RefUpdate refUpdate = JgitUtils.updateRef(repo, oursCommit, baseCommit, ref);

    for (;;) {
      Result result = refUpdate.update();

      if (result == Result.LOCK_FAILURE) {
        if (--remainingLockFailureCalls > 0) {
          Thread.sleep(JgitUtils.SLEEP_ON_LOCK_FAILURE_MS);
        } else {
          throw new RuntimeException("Failed to lock the ref: " + ref);
        }

      } else if (result == Result.REJECTED) {
        RevCommit theirsCommit = revWalk.parseCommit(refUpdate.getOldObjectId());
        NoteMap theirs = NoteMap.read(revWalk.getObjectReader(), theirsCommit);
        NoteMapMerger merger = new NoteMapMerger(repo);
        NoteMap merged = merger.merge(base, ours, theirs);
        RevCommit mergeCommit =
            createCommit(merged, author, "Merged note records\n", theirsCommit, oursCommit);
        refUpdate = JgitUtils.updateRef(repo, mergeCommit, theirsCommit, ref);
        remainingLockFailureCalls = JgitUtils.MAX_LOCK_FAILURE_CALLS;

      } else if (result == Result.IO_FAILURE) {
        throw new RuntimeException("Couldn't create notes because of IO_FAILURE");
      } else {
        break;
      }
    }
  }

  @Override
  public void close() {
    reader.close();
    inserter.close();
    revWalk.close();
  }

  private RevCommit createCommit(
      NoteMap map, PersonIdent author, String message, RevCommit... parents) throws IOException {
    CommitBuilder b = new CommitBuilder();
    b.setTreeId(map.writeTree(inserter));
    b.setAuthor(author);
    b.setCommitter(author);
    if (parents.length > 0) {
      b.setParentIds(parents);
    }
    b.setMessage(message);
    ObjectId commitId = inserter.insert(b);
    inserter.flush();
    return revWalk.parseCommit(commitId);
  }
}
