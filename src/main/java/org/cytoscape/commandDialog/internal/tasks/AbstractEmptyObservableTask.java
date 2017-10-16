package org.cytoscape.commandDialog.internal.tasks;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

public abstract class AbstractEmptyObservableTask extends AbstractTask implements ObservableTask {

	@Override
	abstract public void run(TaskMonitor taskMonitor) throws Exception;

  @SuppressWarnings("unchecked")
  @Override
  public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class))
			return (R)"";
		else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> { return "{}"; };
			return (R)res;
		}
		return null;
	}

	@Override
  public List<Class<?>> getResultClasses() {
    return Arrays.asList(JSONResult.class, String.class);
  }

}
