package org.cytoscape.commandDialog.internal.interpreter;

public class Command {
	private final String processedCommand;
	
	public Command(String command) {
		this.processedCommand = command;		
	}
	
	public String getProcessedCommand() {
		return processedCommand;
	}
}
