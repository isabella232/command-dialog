package org.cytoscape.commandDialog.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class EchoCommandTaskFactory extends AbstractTaskFactory {
	public EchoCommandTaskFactory() {
		super();
	}

	public boolean isReady() {
		return true;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new EchoCommandTask());
	}
}
