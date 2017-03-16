package org.cytoscape.commandDialog.internal.interpreter;

public class AssignmentCommand extends Command {
	private final String targetVariable;
	
	public AssignmentCommand(String command, String variable) {
		super(command);
		targetVariable = variable;
	}
	
	public String getTargetVariable() {
		return targetVariable;
	}
}
