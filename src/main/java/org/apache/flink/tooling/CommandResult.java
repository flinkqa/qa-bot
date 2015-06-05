package org.apache.flink.tooling;

public class CommandResult {
	protected String output;
	protected int resultCode;

	public CommandResult(String output, int resultCode) {
		this.output = output;
		this.resultCode = resultCode;
	}
}
