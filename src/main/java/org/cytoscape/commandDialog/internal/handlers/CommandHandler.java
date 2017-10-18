/* vim: set ts=2: */
/**
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions, and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions, and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   3. Redistributions must acknowledge that this software was
 *      originally developed by the UCSF Computer Graphics Laboratory
 *      under support by the NIH National Center for Research Resources,
 *      grant P41-RR01081.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.cytoscape.commandDialog.internal.handlers;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import org.cytoscape.command.AvailableCommands;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.commandDialog.internal.interpreter.AssignmentCommand;
import org.cytoscape.commandDialog.internal.interpreter.Command;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreter;
import org.cytoscape.commandDialog.internal.interpreter.CommandInterpreterException;
import org.cytoscape.commandDialog.internal.interpreter.LoopCommand;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.util.AbstractBounded;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedFloat;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.BoundedLong;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSingleSelection;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends Handler implements PaxAppender, TaskObserver {

	boolean processingCommand;
	AvailableCommands availableCommands;
	CommandExecutorTaskFactory commandExecutor;
	MessageHandler resultsText;
	TaskManager taskManager; // Task Manager
	private String lastCommandResult;

	private final static Logger logger = LoggerFactory.getLogger(CommandHandler.class);

	public CommandHandler(AvailableCommands availableCommands, 
	                      CommandExecutorTaskFactory commandExecutor,
	                      TaskManager taskManager) {
		this.availableCommands = availableCommands;
		this.commandExecutor = commandExecutor;
		this.taskManager = taskManager;
	}

	public String handleCommand(MessageHandler resultsText, String input) {
		if (input.length() == 0 || input.startsWith("#")) return null;
		input = input.trim();

		// handle defined command types
		this.resultsText = resultsText;
		Command processedCommand = null;

		try {
			processedCommand = CommandInterpreter.get().getProcessedCommand(input);
			if(processedCommand == null) return "";

			if(processedCommand instanceof LoopCommand) {
				handleLoopCommand((LoopCommand)processedCommand, resultsText);
			} else {
				String command = processedCommand.getProcessedCommand();

				// Handle our built-ins
				if (command.startsWith("help")) {
					getHelpReturn(command);
				} else {
					// processingCommand = true;
					// taskManager.execute(commandExecutor.createTaskIterator(Collections.singletonList(input)));
					// processingCommand = false;

					String[] nsCommand = null;
		
					if ((nsCommand = isNamespace(command)) != null) {
						handleCommand(nsCommand[1], nsCommand[0]);
					} else {
						throw new RuntimeException("Failed to find command namespace: '" + command + "'");
					}

					if(processedCommand instanceof AssignmentCommand) {
						CommandInterpreter.get().addVariable(((AssignmentCommand) processedCommand).getTargetVariable(), lastCommandResult);
					}
				}
			}
		} catch (RuntimeException e) {
			logger.error("Error handling command \"" + input + "\"", e);
			resultsText.appendError("  " + e.getMessage());
		} catch (CommandInterpreterException ex) {
			logger.error("Error handling command \"" + input + "\"", ex);
			resultsText.appendError("  " + ex.getMessage());
		}

		resultsText.appendMessage("");
		return lastCommandResult;
	}

	private void handleLoopCommand(LoopCommand command, MessageHandler resultsText) {
		int size = command.getLoopCommands().size();
		List<String> commands = command.getLoopCommands();

		for(int i=0; i<command.getLoopCommands().size(); i++) {
			handleCommand(resultsText, command.getLoopCommands().get(i));
		}
	}

	// Handle unique matches
	private String[] isNamespace(String input) {
		String namespace = null;
		// Namespaces must always be single word
		String [] splits = input.split(" ");
		try {
			String[] nsCommand = new String[2];
			nsCommand[0] = uniqueMatch(splits[0], availableCommands.getNamespaces());
			if (nsCommand[0] == null) return null;
			nsCommand[1] = input.substring(splits[0].length()).trim();
			return nsCommand;
		} catch (RuntimeException e) {
			throw new RuntimeException("Namespace abbreviation not unique");
		}
	}

	private void handleCommand(String inputLine, String ns) {
		// Parse the input, breaking up the tokens into appropriate
		// commands, subcommands, and maps
		Map<String,Object> userArguments = new HashMap<String, Object>(); 
		String command = parseInput(inputLine, userArguments);
		command = validateCommand(command, ns);
		List<String> validArgumentNames = availableCommands.getArguments(ns, command);
		userArguments = validateCommandArguments(command, ns, validArgumentNames, userArguments);
		
		// Now we have all of the possible arguments and the arguments that the user
		// has provided.  Check to make sure all required arguments are available
		for (String arg: validArgumentNames) {
			if (availableCommands.getArgRequired(ns, command, arg) &&
			    !userArguments.containsKey(arg))
				throw new RuntimeException("Error: argument '"+arg+"' is required for command: '"+ns+" "+command+"'");
		}

		processingCommand = true;
		lastCommandResult = null;

		taskManager.execute(commandExecutor.createTaskIterator(ns, command, userArguments, this), this);
	}

	private Map<String, Object> validateCommandArguments(String command, String ns, List<String> validArgumentNames, Map<String, Object> userArguments) {
		Map<String, Object> updatedArgs = new HashMap<>();
		for(String userArg : userArguments.keySet()) {
			String argKey = uniqueMatch(userArg, validArgumentNames);
			if (argKey == null)
				throw new RuntimeException("Error: argument '" + userArg + " isn't applicable to command: '" + ns + " " + command +"'");
			updatedArgs.put(argKey, userArguments.get(userArg));
		}	
		return updatedArgs;
	}

	private String validateCommand(String command, String namespace) {
		if(command ==  null || command.isEmpty()) {
			throw new RuntimeException("Command can not be empty.");
		}
		
		String sub = uniqueMatch(command, availableCommands.getCommands(namespace));

		if (sub == null && (command != null && command.length() > 0))
			throw new RuntimeException("Failed to find command: '" + command + "' (from namespace: " + namespace + ")");

		return sub;
	}

	private String parseInput(String input, Map<String,Object> settings) {

		// Tokenize
		StringReader reader = new StringReader(input.replace("\\", "\\\\"));
		StreamTokenizer st = new StreamTokenizer(reader);

		// We don't really want to parse numbers as numbers...
		st.ordinaryChar('/');
		st.ordinaryChar('_');
		st.ordinaryChar('-');
		st.ordinaryChar('.');
		st.ordinaryChar(':');
		st.ordinaryChars('0', '9');

		st.wordChars('/', '/');
		st.wordChars('_', '_');
		st.wordChars('-', '-');
		st.wordChars('.', '.');
		st.wordChars(':', ':');
		st.wordChars('0', '9');

		List<String> tokenList = new ArrayList<String>();
		int tokenIndex = 0;
		int i;
		try {
			while ((i = st.nextToken()) != StreamTokenizer.TT_EOF) {
				switch(i) {
					case '=':
						// Get the next token
						i = st.nextToken();
						if (i == StreamTokenizer.TT_WORD || i == '"') {
							tokenIndex--;
							String key = tokenList.get(tokenIndex);
							settings.put(key, st.sval);
							tokenList.remove(tokenIndex);
						}
						break;
					case '"':
					case StreamTokenizer.TT_WORD:
						tokenList.add(st.sval);
						tokenIndex++;
						break;
					default:
						break;
				}
			} 
		} catch (Exception e) { return ""; }

		// Concatenate the commands together
		String command = "";
		for (String word: tokenList) command += word+" ";

		// Now, the last token of the args goes with the first setting
		return command.trim();
	}

	private void getHelpReturn(String input) {
		String tokens[] = input.split(" ");
		if (tokens.length == 1) {
			// Return all of the namespaces
			List<String> namespaces = availableCommands.getNamespaces();
			resultsText.appendMessage("Available namespaces:");
			for (String ns: namespaces) {
				resultsText.appendMessage("   "+ns);
			}
			return;
		} else if (tokens.length == 2) {
			if (tokens[1].equals("all")) {
				helpAll();
				return;
			}

			// Get all of the commands for the given namespace
			List<String> commands = availableCommands.getCommands(tokens[1]);
			if(commands.size() == 0) {
				resultsText.appendError("Can't find "+tokens[1]+" namespace");
				return;
			}
			resultsText.appendMessage("Available commands:");
			// TODO: Need to get the description for this command
			for (String command: commands) {
				String desc = availableCommands.getDescription(tokens[1], command);
				if (desc != null && desc.length() > 0)
					resultsText.appendMessage("&nbsp;&nbsp;<b>"+tokens[1]+" "+command+"</b>&nbsp;&nbsp;<i>"+desc+"</i>");
				else
					resultsText.appendMessage("&nbsp;&nbsp;<b>"+tokens[1]+" "+command+"</b>");
			}
		} else if (tokens.length > 2) {
			// Get all of the arguments for a specific command
			String command = "";
			for (int i = 2; i < tokens.length; i++) command += tokens[i]+" ";
			command = command.trim();
			// First, do a little sanity checking
			boolean found = false;
			List<String> commands = availableCommands.getCommands(tokens[1]);
			for (String c: commands) {
				if (c.equalsIgnoreCase(command)) {
					found = true;
					break;
				}
			}
			if (!found) {
				resultsText.appendError("Can't find command "+tokens[1]+" "+command);
				return;
			}

			generateArgumentHelp(tokens[1], command);
		}
	}

	private void generateArgumentHelp(String namespace, String command) {
		String longDescription = availableCommands.getLongDescription(namespace, command);
		String message = "";
		// System.out.println("generateArgumentHelp");
		if (longDescription != null) {
			// System.out.println("longDescription = "+longDescription);
			// Do we have an HTML string?
			if (longDescription.trim().startsWith("<html>") || longDescription.trim().startsWith("<HTML>")) {
				// Yes.  Strip off the "<html></html>" wrapper
				longDescription = longDescription.trim().substring(6);
				longDescription = longDescription.substring(0,longDescription.length()-7);
				// System.out.println("longDescription(html) = "+longDescription);
			} else {
				// No, pass it through the markdown converter
				Parser parser = Parser.builder().build();
				Node document = parser.parse(longDescription);
				HtmlRenderer renderer = HtmlRenderer.builder().build();
				longDescription = renderer.render(document);
				// System.out.println("longDescription(markdown) = "+longDescription);
			}
			message += longDescription;
		}
		List<String> argList = availableCommands.getArguments(namespace, command);
		message += "<br/><br/><b>"+namespace+" "+command+"</b> arguments:";
		// resultsText.appendMessage(commandArgs);
		message += "<dl style='list-style-type:none;margin-top:0px;color:blue'>";
		for (String arg: argList) {
			message += "<dt>";
			if (availableCommands.getArgRequired(namespace, command, arg)) {
				message += "<b>"+arg+"</b>";
			} else {
				message += arg;
			}
			message += "="+getTypeString(namespace, command, arg);
			message += ": ";
			message += "</dt>";
			message += "<dd>";
			message += normalizeArgDescription(availableCommands.getArgDescription(namespace, command, arg),
			                                   availableCommands.getArgLongDescription(namespace, command, arg));
			message += "</dd>";
		}
		resultsText.appendMessage(message+"</dl>");
	}

	private String normalizeArgDescription(String desc, String longDesc) {
		if (longDesc != null && longDesc.length() > 0) {
			return longDesc;
		}

		if (desc != null) {
			desc = desc.trim();
			if (desc.endsWith(":")) desc = desc.substring(0, desc.length() - 1);
		}

		return desc;
	}

	private String getTypeString(String namespace, String command, String arg) {
		Class<?> clazz = availableCommands.getArgType(namespace, command, arg);
		Object object = availableCommands.getArgValue(namespace, command, arg);
		String keywords = keyword("all")+"|"+keyword("selected")+"|"+keyword("unselected");
		// Special handling for various types
		if (clazz.equals(NodeList.class)) {
			String args = "["+variable("nodeColumn:value")+"|"+
			              variable("node name")+keyword(",")+"...]|"+keywords;
			return fixedSpan(args);
		} else if (clazz.equals(EdgeList.class)) {
			String args = "["+variable("edgeColumn:value")+"|"+
			              variable("edge name")+keyword(",")+"...]|"+keywords;
			return fixedSpan(args);
		} else if (clazz.equals(CyNetwork.class)) {
			return fixedSpan(keyword("current")+"|["+variable("column:value")+"|"+variable("network name")+"]");
		} else if (clazz.equals(CyTable.class)) {
			String args = keyword("Node:")+variable("network name")+"|"+
			              keyword("Edge:")+variable("network name")+"|"+
			              keyword("Network:")+variable("network name")+"|"+
			              variable("table name");
			return fixedSpan(args);
		} else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
			return fixedSpan(keyword("true")+"|"+keyword("false"));
		} else if (clazz.equals(ListSingleSelection.class)) {
			if (object != null) {
				ListSingleSelection lss = (ListSingleSelection)object;
				String str = "&lt;"+classString(clazz.getSimpleName())+"&nbsp(";
				List<Object> list = lss.getPossibleValues();
				for (int index = 0; index < list.size()-1; index++) { 
					str += keyword(list.get(index).toString())+"|"; 
				}
				if (!list.isEmpty())
					str += keyword(list.get(list.size()-1).toString()); 
				str += ")&gt;";

				return fixedSpan(str);
			}
		} else if (clazz.equals(ListMultipleSelection.class)) {
			if (object != null) {
				ListMultipleSelection lss = (ListMultipleSelection)object;
				String str = "&lt;"+classString(clazz.getSimpleName())+"&nbsp[";
				List<Object> list = lss.getPossibleValues();
				for (int index = 0; index < list.size()-1; index++) { 
					str += keyword(list.get(index).toString())+","; 
				}
				if (!list.isEmpty())
					str += keyword(list.get(list.size()-1).toString());
				str += "]&gt;";

				return fixedSpan(str);
			}
		} else if (clazz.equals(BoundedDouble.class) || clazz.equals(BoundedFloat.class) ||
	 	          clazz.equals(BoundedInteger.class) || clazz.equals(BoundedLong.class)) {
			if (object != null)
				return boundedTypeString(clazz, object);
		}
		return fixedSpan("&lt;"+classString(clazz.getSimpleName())+"&gt;");
	}

	private String fixedSpan(String s) {
		return "<span style='font-family:Courier;color:black'>"+s+"</span>";
	}

	private String keyword(String s) {
		return "<span style='font-family:Courier;color:#CC00CC'>"+s+"</span>";
	}

	private String variable(String s) {
		return "<span style='font-family:Courier;color:#A000A0;font-style:italics'>"+s+"</span>";
	}

	private String classString(String s) {
		return "<span style='font-family:Courier;color:#FF00FF;font-style:italics'>"+s+"</span>";
	}

	private void helpAll() {
		for (String namespace: availableCommands.getNamespaces()) {
			resultsText.appendMessage(namespace);
			for (String command: availableCommands.getCommands(namespace)) {
				command = command.trim();
				generateArgumentHelp(namespace, command);
				resultsText.appendMessage("<br/>");
			}
		}
	}

	private String boundedTypeString(Class<?> type, Object object) {
		if (object instanceof AbstractBounded) {
			AbstractBounded ab = (AbstractBounded)object;
			String str = "&lt;"+classString(type.getSimpleName())+"&nbsp;(";
			str += ab.getLowerBound().toString() + "&lt;";
			if (!ab.isLowerBoundStrict())
				str += "=";
			if (ab.getValue() != null) {
				str += ab.getValue().toString();
			} else {
				str += classString(ab.getLowerBound().getClass().getSimpleName());
			}
			str += "&lt;";
			if (!ab.isUpperBoundStrict())
				str += "=";
			str += ab.getUpperBound().toString() + ")&gt;";
			return fixedSpan(str);
		} else {
			return fixedSpan("&lt;"+classString(type.getSimpleName())+"&gt;");
		}
	}

	private String uniqueMatch(String input, List<String> matches) {
		String[] words = input.split(" ");
		int wordCount = words.length;
		String bestMatch = null;
		for (String match: matches) {
			String[] matchWords = match.split(" ");
			if (wordCount != matchWords.length)
				continue;
			boolean foundMatch = true;
			for (int word = 0; word < wordCount; word++) {
				if (!matchWords[word].toLowerCase().startsWith(words[word].toLowerCase())) {
					foundMatch = false;
					break;
				}
			}
			if (foundMatch)  {
				if (bestMatch == null) {
					bestMatch = match;
				} else {
					// OK, we've found multiple matches, but maybe one of them is an exact match
					String[] foundWords = bestMatch.split(" ");
					boolean foundExact = false;
					for (int word = 0; word < wordCount; word++) {
						// Are both a match?
						if (foundWords[word].equalsIgnoreCase(words[word]) &&
								matchWords[word].equalsIgnoreCase(words[word]))
							continue; // Yes, just keep going
						if (foundWords[word].equalsIgnoreCase(words[word])) {
							foundExact = true;
							break; // bestMatch is still the better match
						} else if (matchWords[word].equalsIgnoreCase(words[word])) {
							foundExact = true;
							bestMatch = match;
						}
					}
					if (!foundExact)
						throw new RuntimeException("Not unique");
				}
			}
		}
		return bestMatch;
	}

	public void doAppend(PaxLoggingEvent event) {
		// Get prefix
		// Handle levels
		if (!processingCommand) {
			return;
		}
		// System.out.println("doAppend: "+event.getMessage());
		// System.out.println("Thread: "+Thread.currentThread().getName());

		PaxLevel level = event.getLevel();
		if (level.toInt() == 40000)
			resultsText.appendError(event.getMessage());
		else if (level.toInt() == 30000)
			resultsText.appendWarning(event.getMessage());
		else
			resultsText.appendMessage(event.getMessage());
	}

	/**
	 * Callback invoked by TFExecutor when the ObservableTask completes
	 */
	public void taskFinished(ObservableTask t) {
		Object res = t.getResults(String.class);
		if (res != null) {
			lastCommandResult = res.toString();
			resultsText.appendResult(lastCommandResult);
		}
	}

	public void allFinished(FinishStatus status) {
		processingCommand = false;
		resultsText.appendCommand(status.getType().toString());
	}

	// Handler methods
	public void close() {}
	public void flush() {}

	public void publish(final LogRecord record) {
		if (record == null) {
			return;
		}

		Level level = record.getLevel();

		if (level.equals(Level.SEVERE))
			resultsText.appendError(record.getMessage());
		else if (level.equals(Level.WARNING))
			resultsText.appendWarning(record.getMessage());
		else if (level.equals(Level.INFO))
			resultsText.appendMessage(record.getMessage());
	}
}
