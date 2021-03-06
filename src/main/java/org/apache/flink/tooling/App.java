package org.apache.flink.tooling;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private GitHubClient ghClient;
	private RepositoryId repo;
	private IssueService is;
	private String user;

	private final static String TESTED_PULL_REQUEST = "Tested pull request";
	private final static String RUN_QA = "run qa";
	private int waitMinutes = 10;
	private int minPullRequestId = -1; // basically the pull request when this service was enabled


	protected void loadConfiguration() {
		loadConfiguration("config.properties");
	}

	protected void loadConfiguration(String configFile) {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(configFile);
			// load a properties file
			prop.load(input);
		} catch(Exception e) {
			System.err.println("Error loading config file: " + configFile);
			e.printStackTrace();
			System.exit(1);
		}
		user = prop.getProperty("github.user");
		ghClient = new GitHubClient();
		ghClient.setCredentials(user, prop.getProperty("github.password"));

		repo = RepositoryId.createFromId(prop.getProperty("github.repo"));
		LOG.info("Creating configuration for repository: " + repo);

		try {
			waitMinutes = Integer.valueOf(prop.getProperty("waitminutes"));
		} catch (NumberFormatException e) {
			// use default value
			LOG.warn("Could parse waiting time, using default value " + waitMinutes);
		}
		try {
			minPullRequestId = Integer.valueOf(prop.getProperty("minpullrequestid"));
		} catch (NumberFormatException e) {
			// use default value
			LOG.warn("Could parse min pull request id, using default value " + minPullRequestId);
		}

		is = new IssueService(ghClient);
	}

	public void run() {

		loadConfiguration();

		LOG.info("Service initialized. Starting checker-loop.");
		while(true) {
			LOG.info("Just woke up to check for new work");
			checkForNewWork();
			LOG.info("Done checking all pull requests. Going to sleep for "+ waitMinutes +" minutes");
			try {
				if (waitMinutes > 0) {
					Thread.sleep(waitMinutes * 60 * 1000);
				} else {
					break;
				}
			} catch (InterruptedException e) {
				LOG.warn("Interrupted ", e);
				Thread.interrupted();
			}
		}
	}

	private void checkForNewWork() {
		PullRequestService pullRequestService = new PullRequestService(ghClient);
		PageIterator<PullRequest> pages = pullRequestService.pagePullRequests(repo, "open");
		while(pages.hasNext()) {
			Collection<PullRequest> page = pages.next();
			for(PullRequest pr : page) {
				if(pr.getNumber() < minPullRequestId) {
					LOG.info("Skpping pull request #"+pr.getNumber()+": "+pr.getTitle()+" because " +
							"its below the minPullRequestId of "+minPullRequestId);
					continue;
				}
				// check if I tested the pull request already
				boolean needsTesting = true;
				// get comments
				Collection<Comment> comments = getComments(pr.getNumber());
				// go through comments and see whats the latest state.
				for(Comment c : comments) {
					if(c.getBody().contains(TESTED_PULL_REQUEST)) {
						needsTesting = false;
					}
					if(c.getBody().toLowerCase().contains(RUN_QA)) {
						needsTesting = true;
					}
				}
				if(needsTesting) {
					runQA(pr);
				}
			}
		}
	}

	protected Collection<Comment> getComments(int id) {
		try {
			return is.getComments(repo.getOwner(), repo.getName(), id);
		} catch (IOException e) {
			LOG.warn("Error getting comments", e);
		}
		return null;
	}

	private void runQA(PullRequest pr) {
		LOG.info("running QA on #" + pr.getNumber() + ":" + pr.getTitle() + " at " + pr.getHtmlUrl());
		String repo = pr.getHead().getRepo().getCloneUrl();
		String branch = pr.getHead().getRef();
		CommandResult result = runCommand("bash", "run.sh", repo, branch);
		if (result.resultCode == 0) {
			addComment(pr.getNumber(), "Tested pull request." +
					"Result: \n" +
					result.output);
		} else {
			addComment(pr.getNumber(), "Tested pull request but QA script returned errors." +
					"Result: \n" +
					result.output);
		}
	}

	public static CommandResult runCommand(String... command) {
		LOG.info("Running command '" + Arrays.toString(command) + "'");

		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File(System.getProperty("user.dir")));
		processBuilder.redirectErrorStream(true);
		processBuilder.command(command);
		try {
			Process proc = processBuilder.start();
			// read all output at once
			String output = new Scanner(proc.getInputStream()).useDelimiter("\\A").next();
			int resultCode = proc.waitFor();
			return new CommandResult(output, resultCode);
		} catch (Throwable e) {
			LOG.warn("Error running command '"+ Arrays.toString(command) + "'.", e);
			return new CommandResult("Error running command '"+ Arrays.toString(command) + "':\n\n" + e, -1);
		}
	}

	protected void addComment(int id, String comment) {
		try {
			is.createComment(repo, id,
					// do not exceed the maximum length for comments in the GitHub API
					comment.substring(0, Math.min(comment.length(), 262144)));
		} catch (IOException e) {
			LOG.warn("Error adding comment", e);
		}
	}

	public static void main( String[] args ) {
		App a = new App();
		a.run();
    }
}
