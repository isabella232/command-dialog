package org.cytoscape.commandDialog.internal.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreter;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreterException;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

public class EchoCommandTask extends AbstractTask implements ObservableTask {

	@Tunable(description="Variable name",
	         longDescription="The name of the variable or '*' to display the value of all variables",
					 exampleStringValue="*")
	public String variableName;

	private String resultString = null;
	private String value = "";
	private Set<Entry<String, Object>> allVariables;

	public EchoCommandTask() {
		super();
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		try {
			if(variableName.equals("*")) {
				allVariables = CommandInterpreter.get().getAllVariables();
				value = allVariables.toString();
				resultString = "All defined variables: " + value;
				arg0.showMessage(TaskMonitor.Level.INFO, resultString);
			}else {
				value = String.valueOf(CommandInterpreter.get().getVariableValue(variableName));
				resultString = "The value of variable '" + variableName + "' is: '" + value + "'";
				arg0.showMessage(TaskMonitor.Level.INFO, resultString);
			}

		} catch (CommandInterpreterException ex) {
			arg0.showMessage(TaskMonitor.Level.ERROR, ex.getMessage());
		}
	}

  @SuppressWarnings("unchecked")
  @Override
  public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(JSONResult.class)) {
			return (R)new MyJSONResult();
		}
		return (R)resultString;
  }

  @Override
  public List<Class<?>> getResultClasses() {
    return Arrays.asList(JSONResult.class, String.class);
  }

	public class MyJSONResult implements JSONResult {

  	@Override

		public String getJSON() {
			if(variableName.equals("*")) {
				String result = null;
				for (Entry<String, Object> entry: allVariables) {
					if (result == null)
						result = "["+getValue(entry.getKey(), entry.getValue());
					else
						result += ","+getValue(entry.getKey(), entry.getValue());
				}
				return result+"]";

			}else {
				return getValue(variableName, value);
			}

		}

		public String getValue(String name, Object value) {
			return "{"+name+":"+value.toString()+"}";
		}
	}
}
