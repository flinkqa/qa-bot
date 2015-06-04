package org.apache.flink.tooling;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.egit.github.core.Comment;

import java.util.Random;

/**
 * Github interaction tests only work if the config-test.properties files has the authorization key set
 * and the repository has been set to flinkqa/flink.
 */
public class AppTest 
	extends TestCase
{
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public AppTest( String testName )
	{
		super( testName );
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( AppTest.class );
	}

	public void testPostComment() {
		App app = new App();
		app.loadConfiguration("config-test.properties");

		String commentText = "Unit test: " + new Random().nextInt();
		// add to the flink pull request (has to exist)
		app.addComment(1, commentText);

		Comment[] type = new Comment[]{};
		Comment[] comments = app.getComments(1).toArray(type);

		assertTrue(comments[comments.length - 1].getBody().equals(commentText));
	}

	public void testGetPullRequests() {
	}

	public void testRunCommandStdout() {
		String expected = "testing";
		String output = App.runCommand("echo", "-n", expected);
		assertEquals(expected, output);
	}

	public void testRunCommandStderr() {
		String className = "ILLEGALCLASS";
		String output = App.runCommand("java", className);
		assertEquals("Error: Could not find or load main class " + className + "\n", output);
	}

	public void testIllegalCommand() {
		String output =	App.runCommand("ILLEGALPROGRAM");
		assertTrue(output.startsWith("Error running"));
	}
}
