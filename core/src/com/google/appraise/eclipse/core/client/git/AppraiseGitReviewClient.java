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

import com.google.appraise.eclipse.core.client.data.Review;
import com.google.appraise.eclipse.core.client.data.ReviewComment;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListNotesCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ShowNoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.notes.DefaultNoteMerger;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.notes.NoteMapMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Jgit-based utility routines for working with Appraise-style reviews.
 */
public class AppraiseGitReviewClient {
  /**
   * The wildcard refspec to fetch all git notes updates.
   */
  private static final String DEVTOOLS_PULL_REFSPEC =
      "+refs/notes/devtools/*:refs/notes/origin/devtools/*";

  /**
   * The wildcard refspec to push all git notes commits.
   */
  private static final String DEVTOOLS_PUSH_REFSPEC = "refs/notes/devtools/*:refs/notes/devtools/*";

  private static final Logger logger = Logger.getLogger(AppraiseGitReviewClient.class.getName());

  // Ref defines the git-notes ref that we expect to contain review requests.
  private static final String REVIEWS_REF = "refs/notes/devtools/reviews";

  // Ref defines the git-notes origin ref for review requests.
  private static final String REVIEWS_ORIGIN_REF = "refs/notes/origin/devtools/reviews";

  // Ref defines the git-notes ref that we expect to contain review comments.
  private static final String COMMENTS_REF = "refs/notes/devtools/discuss";

  // Ref defines the git-notes origin ref for review comments.
  private static final String COMMENTS_ORIGIN_REF = "refs/notes/origin/devtools/discuss";

  /**
   * The git repository to be accessed.
   */
  private final Repository repo;

  /**
   * The indentity used for commits. From the config of the current repository.
   */
  private final PersonIdent author;

  /**
   * Creates a new client for the given git repository.
   */
  public AppraiseGitReviewClient(Repository repo) {
    this.repo = repo;
    this.author = new PersonIdent(repo);
  }

  /**
   * Gets the review commit, which is the first commit on the review branch
   * after the merge base.
   */
  public RevCommit getReviewCommit(String reviewBranch, String targetBranch)
      throws GitClientException {
    try (RevWalk walk = new RevWalk(repo)) {
      walk.markStart(walk.parseCommit(repo.resolve(reviewBranch)));
      walk.markUninteresting(walk.parseCommit(repo.resolve(targetBranch)));
      walk.sort(RevSort.REVERSE);
      return walk.next();
    } catch (Exception e) {
      throw new GitClientException(
          "Failed to get review commit for " + reviewBranch + " and " + targetBranch, e);
    }
  }

  /**
   * Retrieves all the reviews in the current project's repository by commit hash.
   */
  public Map<String, Review> listReviews() throws GitClientException {
    // Get the most up-to-date list of reviews.
    syncCommentsAndReviews();

    Map<String, Review> reviews = new LinkedHashMap<>();

    Git git = new Git(repo);
    try {
      ListNotesCommand cmd = git.notesList();
      cmd.setNotesRef(REVIEWS_REF);
      List<Note> notes = cmd.call();
      for (Note note : notes) {
        String rawNoteDataStr = noteToString(repo, note);
        Review latest = extractLatestReviewFromNotes(rawNoteDataStr);
        if (latest != null) {
          reviews.put(note.getName(), latest);
        }
      }
    } catch (Exception e) {
      throw new GitClientException(e);
    } finally {
      git.close();
    }
    return reviews;
  }

  /**
   * Pulls the most recent notes data for a review out of the raw notes data string, leveraging
   * the timestamp.
   */
  private Review extractLatestReviewFromNotes(String rawNoteDataStr) throws GitClientException {
    String[] noteDataStrs = rawNoteDataStr.split("\n");
    Review latest = parseReviewJson(noteDataStrs[0]);
    for (int i = 1; i < noteDataStrs.length; i++) {
      Review anotherOne = parseReviewJson(noteDataStrs[i]);
      try {
        if (latest == null
            || ((anotherOne != null && anotherOne.getTimestamp() > latest.getTimestamp()))) {
          latest = anotherOne;
        }
      } catch (Exception e) {
        throw new GitClientException(e);
      }
    }
    return latest;
  }

  /**
   * Gets a specific review. Returns null if it is not found.
   */
  public Review getReview(String reviewCommitHash) throws GitClientException {
    try (Git git = new Git(repo)) {
      String noteDataStr = readOneNote(git, REVIEWS_REF, reviewCommitHash);
      return extractLatestReviewFromNotes(noteDataStr);
    }
  }

  /**
   * Helper method that parses the given JSON data for a review and returns
   * null if the parsing fails for any reason.
   */
  private Review parseReviewJson(String noteDataStr) {
    try {
      return new Gson().fromJson(noteDataStr, Review.class);
    } catch (JsonSyntaxException jse) {
      logger.warning("Weird data in review note: " + noteDataStr);
      return null;
    }
  }

  /**
   * Reads a single note out as a string from the given commit hash.
   * Returns null if the note isn't found.
   */
  private String readOneNote(Git git, String notesRef, String hash) throws GitClientException {
    try (RevWalk walker = new RevWalk(git.getRepository())) {
      ShowNoteCommand cmd = git.notesShow();
      cmd.setNotesRef(notesRef);
      ObjectId ref = git.getRepository().resolve(hash);
      RevCommit commit = walker.parseCommit(ref);
      cmd.setObjectId(commit);
      Note note = cmd.call();
      if (note == null) {
        return null;
      }
      return noteToString(repo, note);
    } catch (Exception e) {
      throw new GitClientException(e);
    }
  }

  /**
   * Adds a new comment to the review and writes it to the notes.
   * @param reviewCommitHash Is the review commit hash in our model.
   * @param commentData The comment to append.
   */
  public void writeComment(String reviewCommitHash, String commentData) throws GitClientException {
    ReviewComment comment = new ReviewComment();
    comment.setDescription(commentData);
    // Will fill in the time and author.
    writeComment(reviewCommitHash, comment);
  }

  /**
   * Writes the given comment to the given review, automatically filling in
   * the author and timestamp.
   */
  public void writeComment(String reviewCommitHash, ReviewComment comment)
      throws GitClientException {
    // Sync to minimize the chances of non-linear merges.
    syncCommentsAndReviews();

    // Commit.
    commitCommentNote(reviewCommitHash, comment);

    // Push.
    try {
      pushCommentsAndReviews();
    } catch (Exception e) {
      throw new GitClientException("Error pushing, review is " + reviewCommitHash, e);
    }
  }

  /**
   * Helper method that commits a new comment to the git notes.
   */
  private void commitCommentNote(String reviewCommitHash, ReviewComment comment) {
    try (GitNoteWriter<ReviewComment> writer =
        GitNoteWriter.createNoteWriter(reviewCommitHash, repo, author, COMMENTS_REF)) {
      // We store time in seconds in the notes.
      comment.setTimestamp(System.currentTimeMillis() / 1000);
      comment.setAuthor(author.getEmailAddress());

      List<ReviewComment> comments = new ArrayList<ReviewComment>();
      comments.add(comment);
      writer.create("Writing comment for " + reviewCommitHash, comments);
    }
  }

  /**
   * Writes a new {@link Review} based on the given task data.
   * @return the new review's hash.
   */
  public String createReview(String reviewCommitHash, Review review) throws GitClientException {
    // Sync to minimize the chances of non-linear merges.
    syncCommentsAndReviews();

    // Push the code under review, or the user won't be able to access the commit with the
    // notes.
    try (Git git = new Git(repo)) {
      assert !"master".equals(review.getReviewRef());
      RefSpec reviewRefSpec = new RefSpec(review.getReviewRef());
      PushCommand pushCommand = git.push();
      pushCommand.setRefSpecs(reviewRefSpec);
      try {
        pushCommand.call();
      } catch (Exception e) {
        throw new GitClientException("Error pushing review commit(s) to origin", e);
      }
    }

    // Commit.
    commitReviewNote(reviewCommitHash, review);

    // Push.
    try {
      pushCommentsAndReviews();
    } catch (Exception e) {
      throw new GitClientException("Error pushing, review is " + reviewCommitHash, e);
    }

    return reviewCommitHash;
  }

  /**
   * Helper method that commits a new comment to the git notes.
   */
  private void commitReviewNote(String reviewCommitHash, Review review) {
    try (GitNoteWriter<Review> writer =
        GitNoteWriter.createNoteWriter(reviewCommitHash, repo, author, REVIEWS_REF)) {
      List<Review> reviews = new ArrayList<Review>();
      reviews.add(review);
      writer.create("Writing review for " + reviewCommitHash, reviews);
    }
  }

  /**
   * Pushes the local comments and reviews back to the origin.
   */
  private void pushCommentsAndReviews() throws Exception {
    try (Git git = new Git(repo)) {
      RefSpec spec = new RefSpec(DEVTOOLS_PUSH_REFSPEC);
      PushCommand pushCommand = git.push();
      pushCommand.setRefSpecs(spec);
      pushCommand.call();
    }
  }

  /**
   * Gets the diff entries associated with a specific review commit.
   * The review commit is the commit hash at which the review was requested.
   * Subsequent commits on that review can be inferred from the append-only comments.
   */
  public List<DiffEntry> getDiff(String requestCommitHash)
      throws GitClientException, IOException, GitAPIException {
    Review review = getReview(requestCommitHash);

    // If the target ref is missing or the corresponding branch does not exist,
    // the review is bogus.
    if (review.getTargetRef() == null || review.getTargetRef().isEmpty()) {
      throw new GitClientException("Review target ref not set: " + requestCommitHash);
    }

    try (Git git = new Git(repo)) {
      if (!isBranchExists(review.getTargetRef())) {
        throw new GitClientException("Review target ref does not exist: " + requestCommitHash + ", "
            + review.getTargetRef());
      }

      if (review.getReviewRef() == null || review.getReviewRef().isEmpty()) {
        // If there is no review ref, then show the diff from the single commit.
        RevCommit revCommit = resolveRevCommit(requestCommitHash);
        return calculateCommitDiffs(git, resolveParentRevCommit(revCommit), revCommit);
      } else if (isBranchExists(review.getReviewRef())
          && !areAncestorDescendent(review.getReviewRef(), review.getTargetRef())) {
        // If the review ref branch exists and is not already submitted,
        // then show the diff between review ref and target ref.
        return calculateBranchDiffs(git, review.getTargetRef(), review.getReviewRef());
      } else {
        // If the review ref points to a non-existent branch, the review is over, so read the
        // comments and diff between the parent and the "last" (chronologically) one.
        Map<String, ReviewComment> comments = listCommentsForReview(git, requestCommitHash);
        RevCommit revCommit = resolveRevCommit(requestCommitHash);
        RevCommit parent = resolveParentRevCommit(revCommit);
        RevCommit last = findLastCommitInComments(comments.values(), revCommit);
        return calculateCommitDiffs(git, parent, last);
      }
    }
  }

  /**
   * Fetches review and comment git notes and updates the local refs, performing
   * merges if necessary.
   */
  public void syncCommentsAndReviews() throws GitClientException {
    RevWalk revWalk = null;
    try (Git git = new Git(repo)) {
      revWalk = new RevWalk(repo);

      // Fetch the latest.
      RefSpec spec = new RefSpec(DEVTOOLS_PULL_REFSPEC);
      git.fetch().setRefSpecs(spec).call();

      syncNotes(revWalk, COMMENTS_REF, COMMENTS_ORIGIN_REF);
      revWalk.reset();
      syncNotes(revWalk, REVIEWS_REF, REVIEWS_ORIGIN_REF);
    } catch (Exception e) {
      throw new GitClientException("Error syncing notes", e);
    } finally {
      if (revWalk != null) {
        revWalk.close();
      }
    }
  }

  /**
   * Helper method that syncs the notes between the given ref names.
   */
  private void syncNotes(RevWalk revWalk, String localRefName, String originRefName)
      throws Exception {
    Ref originRef = repo.getRef(originRefName);
    if (originRef == null) {
      // Most likely nobody has ever pushed anything to the devtools notes in this repo.
      return;
    }

    RevCommit originCommit = revWalk.parseCommit(originRef.getObjectId());

    Ref localRef = repo.getRef(localRefName);
    if (localRef == null) {
      // Update the local ref to the origin commit. This happens the first time a new repo is set
      // up.
      Result result = JgitUtils.updateRef(repo, originCommit, null, localRefName).update();
      if (!result.equals(Result.FAST_FORWARD)) {
        throw new GitClientException("Invalid result initializing the local ref: " + result);
      }
      return;
    }

    RevCommit localCommit = revWalk.parseCommit(localRef.getObjectId());
    RevCommit baseCommit = getMergeBase(revWalk, localCommit, originCommit);

    // If the commits are the same, there is nothing to do.
    if (localCommit.equals(originCommit)) {
      return;
    }

    if (originCommit.equals(baseCommit)) {
      // If the merge base is the same as the origin, we should push our changes to the origin,
      // because we have local ones.
      // Note that this pushes both comments and notes. Since we are typically synchronizing
      // them in close succession, it's expected that this push will happen the first time,
      // and the next time the commits will be the same in most cases.
      pushCommentsAndReviews();
    } else if (localCommit.equals(baseCommit)) {
      // If the merge base is the same as the local, we should advance our ref in a fast-forward.
      Result result = JgitUtils.updateRef(repo, originCommit, localCommit, localRefName).update();
      if (!result.equals(Result.FAST_FORWARD) && !result.equals(Result.NO_CHANGE)) {
        throw new GitClientException("Invalid result advancing the local ref: " + result);
      }
    } else {
      // If the merge base is not equal to either, we need to do a merge.
      mergeNotesAndPush(revWalk, localRefName, baseCommit, localCommit, originCommit);
    }
  }

  /**
   * Merges the notes from local and origin commits with the given merge base.
   */
  private void mergeNotesAndPush(RevWalk revWalk, String refName, RevCommit baseCommit,
      RevCommit localCommit, RevCommit originCommit) throws GitClientException {
    int remainingLockFailureCalls = JgitUtils.MAX_LOCK_FAILURE_CALLS;

    // Merge and commit.
    while (true) {
      try {
        NoteMap theirNoteMap = NoteMap.read(revWalk.getObjectReader(), originCommit);
        NoteMap ourNoteMap = NoteMap.read(revWalk.getObjectReader(), localCommit);
        NoteMap baseNoteMap;
        if (baseCommit != null) {
          baseNoteMap = NoteMap.read(revWalk.getObjectReader(), baseCommit);
        } else {
          baseNoteMap = NoteMap.newEmptyMap();
        }

        NoteMapMerger merger =
            new NoteMapMerger(repo, new DefaultNoteMerger(), MergeStrategy.RESOLVE);
        NoteMap merged = merger.merge(baseNoteMap, ourNoteMap, theirNoteMap);
        try (ObjectInserter inserter = repo.newObjectInserter()) {
          RevCommit mergeCommit = createNotesCommit(
              merged, inserter, revWalk, "Merged note commits\n", localCommit, originCommit);

          RefUpdate update = JgitUtils.updateRef(repo, mergeCommit, localCommit, refName);
          Result result = update.update();
          if (result == Result.LOCK_FAILURE) {
            if (--remainingLockFailureCalls > 0) {
              Thread.sleep(JgitUtils.SLEEP_ON_LOCK_FAILURE_MS);
            } else {
              throw new GitClientException("Failed to lock the ref: " + refName);
            }
          } else if (result == Result.REJECTED) {
            throw new GitClientException("Rejected update to " + refName + ", this is unexpected");
          } else if (result == Result.IO_FAILURE) {
            throw new GitClientException("I/O failure merging notes");
          } else {
            // OK.
            break;
          }
        }
      } catch (Exception e) {
        throw new GitClientException("Error merging notes commits", e);
      }
    }

    // And push.
    try {
      pushCommentsAndReviews();
    } catch (Exception e) {
      throw new GitClientException("Error pushing merge commit", e);
    }
  }

  /**
   * Creates a merged notes commit.
   */
  private RevCommit createNotesCommit(NoteMap map, ObjectInserter inserter, RevWalk revWalk,
      String message, RevCommit... parents) throws IOException {
    CommitBuilder commitBuilder = new CommitBuilder();
    commitBuilder.setTreeId(map.writeTree(inserter));
    commitBuilder.setAuthor(author);
    commitBuilder.setCommitter(author);
    if (parents.length > 0) {
      commitBuilder.setParentIds(parents);
    }
    commitBuilder.setMessage(message);
    ObjectId commitId = inserter.insert(commitBuilder);
    inserter.flush();
    return revWalk.parseCommit(commitId);
  }

  /**
   * Gets the merge base for the two given commits.
   * Danger -- the commits need to be from the given RevWalk or this will
   * fail in a not-completely-obvious way.
   */
  private RevCommit getMergeBase(RevWalk walk, RevCommit commit1, RevCommit commit2)
      throws GitClientException {
    try {
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(commit1);
      walk.markStart(commit2);
      return walk.next();
    } catch (Exception e) {
      throw new GitClientException(
          "Failed to get merge base commit for " + commit1 + " and " + commit2, e);
    }
  }

  /**
   * Checks to see if two branches/commits are in an ancestor-descendent relationship.
   */
  public boolean areAncestorDescendent(String ancestor, String descendent)
      throws GitClientException {
    try (RevWalk revWalk = new RevWalk(repo)) {
      RevCommit ancestorHead = revWalk.parseCommit(repo.resolve(ancestor));
      RevCommit descendentHead = revWalk.parseCommit(repo.resolve(descendent));
      return revWalk.isMergedInto(ancestorHead, descendentHead);
    } catch (Exception e) {
      throw new GitClientException(
          "Error checking ancestor/descendent for " + ancestor + " and " + descendent, e);
    }
  }

  /**
   * Resolves the (first) parent commit.
   */
  private RevCommit resolveParentRevCommit(RevCommit revCommit)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    RevCommit parent = null;
    try (RevWalk walker = new RevWalk(repo)) {
      parent = walker.parseCommit(revCommit.getParents()[0].getId());
    }
    return parent;
  }

  /**
   * Gets the chronologically-last commit from a set of review comments.
   */
  private RevCommit findLastCommitInComments(
      Collection<ReviewComment> collection, RevCommit defaultCommit)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    RevCommit lastCommit = defaultCommit;
    for (ReviewComment comment : collection) {
      if (comment.getLocation() == null || comment.getLocation().getCommit() == null
          || comment.getLocation().getCommit().isEmpty()) {
        continue;
      }
      RevCommit currentCommit = resolveRevCommit(comment.getLocation().getCommit());
      if (currentCommit != null && currentCommit.getCommitTime() > lastCommit.getCommitTime()) {
        lastCommit = currentCommit;
      }
    }
    return lastCommit;
  }

  /**
   * Gets all the comments for a specific review hash, by comment id.
   * The comment id is conventionally the SHA-1 hash of its JSON string.
   */
  public Map<String, ReviewComment> listCommentsForReview(String requestCommitHash)
      throws GitClientException {
    try (Git git = new Git(repo)) {
      return listCommentsForReview(git, requestCommitHash);
    }
  }

  /**
   * Gets all the comments for a specific review hash, by comment id.
   * The comment id is conventionally the SHA-1 hash of its JSON string.
   */
  private Map<String, ReviewComment> listCommentsForReview(Git git, String requestCommitHash)
      throws GitClientException {
    // Get the most up-to-date list of comments.
    syncCommentsAndReviews();

    Map<String, ReviewComment> comments = new LinkedHashMap<>();
    try {
      String noteDataStr = readOneNote(git, COMMENTS_REF, requestCommitHash);
      if (noteDataStr != null) {
        for (String commentStr : noteDataStr.split("\n")) {
          try {
            String commentId = DigestUtils.shaHex(commentStr);
            ReviewComment comment = new Gson().fromJson(commentStr, ReviewComment.class);
            if (comment != null) {
              comments.put(commentId, comment);
            }
          } catch (JsonSyntaxException jse) {
            logger.warning("Failed to parse comment " + noteDataStr);
          }
        }
      }
    } catch (Exception e) {
      throw new GitClientException(e);
    }
    return comments;
  }

  private AbstractTreeIterator prepareTreeParser(String ref)
      throws IOException, MissingObjectException, IncorrectObjectTypeException {
    // from the commit we can build the tree which allows us to construct the TreeParser
    Ref head = repo.getRef(ref);
    try (RevWalk walk = new RevWalk(repo)) {
      RevCommit commit = walk.parseCommit(head.getObjectId());
      return prepareTreeParserHelper(walk, commit);
    }
  }

  private AbstractTreeIterator prepareTreeParser(RevCommit commit)
      throws IOException, MissingObjectException, IncorrectObjectTypeException {
    // from the commit we can build the tree which allows us to construct the TreeParser
    try (RevWalk walk = new RevWalk(repo)) {
      return prepareTreeParserHelper(walk, commit);
    }
  }

  private AbstractTreeIterator prepareTreeParserHelper(RevWalk walk, RevCommit commit)
      throws IOException, MissingObjectException, IncorrectObjectTypeException {
    RevTree tree = walk.parseTree(commit.getTree().getId());
    CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
    try (ObjectReader oldReader = repo.newObjectReader()) {
      oldTreeParser.reset(oldReader, tree.getId());
    }
    return oldTreeParser;
  }

  private RevCommit resolveRevCommit(String commitHash)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    ObjectId ref = repo.resolve(commitHash);
    try (RevWalk walker = new RevWalk(repo)) {
      return walker.parseCommit(ref);
    }
  }

  /**
   * Gets the diff between heads on two branches.
   * See
   * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowBranchDiff.java.
   */
  private List<DiffEntry> calculateBranchDiffs(Git git, String targetRef, String reviewRef)
      throws IOException, GitAPIException {
    AbstractTreeIterator oldTreeParser = prepareTreeParser(targetRef);
    AbstractTreeIterator newTreeParser = prepareTreeParser(reviewRef);
    return git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
  }

  /**
   * Gets the diff between heads on two branches.
   */
  public List<DiffEntry> calculateBranchDiffs(String targetRef, String reviewRef)
      throws GitClientException {
    try (Git git = new Git(repo)) {
      return calculateBranchDiffs(git, targetRef, reviewRef);
    } catch (Exception e) {
      throw new GitClientException(
          "Error loading branch diffs for " + reviewRef + " and " + targetRef, e);
    }
  }

  /**
   * Gets the diff between two commits.
   */
  private List<DiffEntry> calculateCommitDiffs(Git git, RevCommit first, RevCommit last)
      throws IOException, GitAPIException {
    AbstractTreeIterator oldTreeParser = prepareTreeParser(first);
    AbstractTreeIterator newTreeParser = prepareTreeParser(last);
    return git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
  }

  /**
   * Returns whether or not a specific named branch exists in the repo.
   */
  private boolean isBranchExists(String ref) throws IOException {
    return (repo.getRef(ref) != null);
  }

  /**
   * Utility method that converts a note to a string (assuming it's UTF-8).
   */
  private String noteToString(Repository repo, Note note)
      throws MissingObjectException, IOException, UnsupportedEncodingException {
    ObjectLoader loader = repo.open(note.getData());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    loader.copyTo(baos);
    return new String(baos.toByteArray(), "UTF-8");
  }

  /**
   * Confirms that the user is on a ref that is valid for creating a new review.
   */
  public boolean canRequestReviewOnReviewRef(String reviewRef, String targetRef) {
    // Confirm that the user is NOT targeting the same ref.
    // TODO: Should we also confirm that they are not on master?
    if (targetRef.equals(reviewRef)) {
      return false;
    }
    return true;
  }

  /**
   * Updates the given review if it has changed, and writes out a new comment if supplied.
   * Assumes the code under review has already been pushed.
   * @return the review's hash.
   */
  public String updateReviewWithComment(String reviewCommitHash, Review review, String newComment)
      throws GitClientException {
    // Sync to minimize the chances of non-linear merges.
    syncCommentsAndReviews();

    boolean needPush = false;
    Review existingReview = getReview(reviewCommitHash);
    if (!review.equals(existingReview)) {
      // Need to update the review.
      commitReviewNote(reviewCommitHash, review);
      needPush = true;
    }

    if (newComment != null && !newComment.isEmpty()) {
      // Write the new comment.
      ReviewComment comment = new ReviewComment();
      comment.setDescription(newComment);
      commitCommentNote(reviewCommitHash, comment);
      needPush = true;
    }

    // Push.
    if (needPush) {
      try {
        pushCommentsAndReviews();
      } catch (Exception e) {
        throw new GitClientException("Error pushing, review is " + reviewCommitHash, e);
      }
    }

    return reviewCommitHash;
  }
}
