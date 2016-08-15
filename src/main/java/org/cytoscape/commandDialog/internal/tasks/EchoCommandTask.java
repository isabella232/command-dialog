package org.cytoscape.commandDialog.internal.tasks;

import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreter;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreterException;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

public class EchoCommandTask extends AbstractTask {
	@Tunable(description="Variable name:")
	public String variableName;
	
	public EchoCommandTask() {
		super();
	}
	
	@Override
	public void run(TaskMonitor arg0) throws Exception {
		try {
			String value = "";
			if(variableName.equals("*")) {
				value = CommandInterpreter.get().getAllVariables().toString();
				arg0.showMessage(TaskMonitor.Level.INFO, "All defined variables: " + value);
			}else {
				value = String.valueOf(CommandInterpreter.get().getVariableValue(variableName));
				arg0.showMessage(TaskMonitor.Level.INFO, "The value of variable '" + variableName + "' is: '" + value + "'");
			}
			
		} catch (CommandInterpreterException ex) {
			arg0.showMessage(TaskMonitor.Level.ERROR, ex.getMessage());
		}		
	}
}
