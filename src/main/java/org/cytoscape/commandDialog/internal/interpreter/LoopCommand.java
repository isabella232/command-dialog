package org.cytoscape.commandDialog.internal.interpreter;

import java.util.LinkedList;
import java.util.List;

public class LoopCommand extends Command {
	List<String> loopCommands;
	public LoopCommand() {
		super("");
		loopCommands = new LinkedList<>();
	}
	
	public List<String> getLoopCommands() {
		return loopCommands;
	}
	
	public void addLoopCommand(String command) {
		loopCommands.add(command);
	}
	
	public void clearCommand() {
		loopCommands.clear();
	}
}
