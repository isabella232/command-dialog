package org.cytoscape.commandDialog.internal.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.cytoscape.commandDialog.internal.handlers.CommandHandler;
import org.cytoscape.commandDialog.internal.handlers.MessageHandler;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreterException;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreterUtils;
import org.cytoscape.commandDialog.internal.ui.CommandToolDialog;
import org.cytoscape.commandDialog.internal.ui.ConsoleCommandHandler;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

public class RunCommandsTask extends AbstractTask {
	CommandToolDialog dialog;
	CommandHandler handler;

	@ProvidesTitle
	public String getTitle() { return "Execute Command File"; }

	public File file;
	@Tunable(description="Command File", required=true, params="input=true;fileCategory=unspecified")
	public File getfile() {
		return file;
	}
	public void setfile(File file) {
		this.file = file;
	}

	// add a new string tunable to specify file command arguments
	@Tunable(description="Script arguments",
	         longDescription="Enter the script arguments as key:value pairs separated by commas",
					 exampleStringValue="arg1:value,arg2:value")
	public String args;

	public RunCommandsTask(CommandToolDialog dialog, CommandHandler handler) {
		super();
		this.dialog = dialog;
		this.handler = handler;
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		executeCommandScriptInternal(arg0);
	}

	public void executeCommandScript(String fileName, CommandToolDialog dialog) {
		file = new File(fileName);
		executeCommandScriptInternal(null);
	}

	private void executeCommandScriptInternal(TaskMonitor arg0) {
		String errorMessage = "";
		try {
			executeCommandScript(dialog, new ConsoleCommandHandler());
		} catch (FileNotFoundException fnfe) {
			errorMessage = "No such file or directory: "+file.getPath();
		} catch (IOException ioe) {
			errorMessage = "Unexpected I/O error: " + ioe.getMessage();
		} catch (CommandInterpreterException ex) {
			errorMessage = "Error in executing command script. Message: " + ex.getMessage();
		}
		if(arg0 != null) {
			arg0.showMessage(TaskMonitor.Level.ERROR, errorMessage);
		} else {
			System.err.println(errorMessage);
		}
	}

	private void executeCommandScript(CommandToolDialog dialog, ConsoleCommandHandler consoleHandler) 
	       throws FileNotFoundException, IOException, CommandInterpreterException {
		if (dialog != null) {
			// We have a GUI
			dialog.setVisible(true);
		}

		CommandInterpreterUtils.parseAndInitializeCommandScriptArguments(args);

		try (BufferedReader reader = new BufferedReader(new FileReader(file))){
			String sourceCommand = null;

			// Read each line of command, pre-process it for variable substitution if required. // add same check of # and empty
			while ((sourceCommand = reader.readLine()) != null) {
				if (sourceCommand.length() == 0 || sourceCommand.startsWith("#")) continue;

				if (dialog != null) {
					dialog.executeCommand(sourceCommand);
				} else {
					consoleHandler.appendCommand(sourceCommand);
					handler.handleCommand((MessageHandler) consoleHandler, sourceCommand);
				}
			}
		}
	}
}
