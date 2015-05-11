package org.apache.flink.tooling;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Unit test for simple App.
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

	/**
	 * Rigourous Test :-)
	 */
	public void testApp()
	{
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
