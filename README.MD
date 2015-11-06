# Google Appraise Eclipse Plugin

Git Appraise is a code review system that uses git-notes as a data store. The Google Appraise Eclipse Plugin supports an Appraise-based code reveiw workflow inside Eclipse. It is implemented as a Mylyn Task Repository connector and leverages the Egit plugin.

## Links

* [Git Appraise](https://github.com/google/git-appraise) -- for the Git Appraise command-line tool and more information about Git Appraise in general.

## How to develop

Import the following projects into Eclipse:
* com.google.appraise.eclipse.core
* com.google.appraise.eclipse.ui
* feature
* site

To debug, right-click the "core" project and Run As...Eclipse Application.

To build the update site archive (site/target/site_assembly.zip):
mvn package

Please send pull requests.

## Releases
Releases will be tagged in the repository and an announcement will be made on our Github page. We have no strict timetable but aim to do this every three months or whenever a more urgent need arises.

## Roadmap
* Improved UI for creating and responding to comments (e.g. inline with the source).
* Smoother Mylyn integration.

## User documentation

### Prerequisites

* Install [Egit](http://www.eclipse.org/egit/download/).
* Install the Appraise Eclipse Plugin. For now, this means using the update site archive file. (See the "How to develop" section above). Details:
  * Help | Install New Software...
  * Add...
  * Archive...
  * Browse to the site.zip file you downloaded or built using the instructions above.
  * OK.
  * Turn off "Group items by category" if it is selected.
  * Choose "Appraise" and Next, Finish.
  * Note that this will fail if you did not already install Egit.
* In the Git perspective in Eclipse, add a repository for the project you want to work with.
* Import or create an Eclipse project connected to the Git repository. *None of what follows will work otherwise.*

### Adding an Appraise Task Repository
* Window | Show View | Mylyn | Task Repositories.
* Click on "Add a Task Repository...".
* Select "Appraise Reviews".
* Under "Server", you should see an item for each Git-connected Eclipse project in your workspace. Choose the one you want. Verify that your User ID is correct for that Git repo and click "Finish".
* You will now have the option to add a query.

### Creating a review query
* In the Task List view, select (from the toolbar or right-click) "New Query...".
* Select the Appraise repository and click "Next".
* Enter a title for your query. (This will show up in the Task List).
* You may choose to filter for reviews where you are the requestor or reviewer, or filter by a specific review commit hash prefix.
* You can refresh the query (to see new reviews from your collaborators, for example) with "Synchronize" or "Synchronize Automatically".


### Requesting a code review
Note that the Appraise workflow model expects you to request a review from a working branch, and the Eclipse Plugin assumes that you currently have that branch checked out. (You can confirm this by looking at the project node name in the Package Explorer.)
* In the Task List view, select (from the toolbar or right-click) "New Task...".
* Select the Appraise repository and click "Finish".
* The Task Editor appears:
  * The Review Ref should be pre-filled with the name of your current branch.
  * The Target Ref (where your review will be submitted) defaults to refs/heads/master. You can change this if necessary.
  * You should select some code reviewers by filling in their User ID's as a comma-separated list in the "Reviewers" text box.
  * Your commited changes are listed, and you can view the diffs.
* When you are satisfied, request the review by clicking the "Submit" button for the task. (Underneath the hood, this will create git-notes on the refs/notes/devtools/reviews ref and push them.)

### Reviewing code
The code review workflow for Appraise in Eclipse is based around Mylyn's task activation model.
* Right-click a review task in the Task List and choose "Activate".
* You will be given the option to checkout the review branch.
* Double-click the task to open the task editor where you can:
  * See the changes with diffs.
  * See the comments.
  * Make new comments.
  * Add or remove reviewers.
* Window | Show View | Other | Other and select "Appraise Review Tasks" to open the "Appraise Review Tasks" view.
  * There, you can view all the review comments and, if there is a location associated with the comment, double-click to open the relevant source file.
* Open a source file.
  * If there are any comments from the active review associated with that source file, they will appear as markers in the gutter.
  * Right-click any existing marker and select a comment from the "Appraise Review Comments" menu to reply to the comment.
  * Right-click anywhere in the source editor and select "Appraise Review Comments" to create a new comment associated with the file the file, the location clicked, or the overall review.
* After making all comments, return to the task editor to:
  * Submit comments with the "Submit" button. (This has the effect of merging git-notes for the review commit on the refs/notes/devtools/discuss ref.)
