package org.cytoscape.commandDialog.internal.interpreter;

import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Class to parse a single command and also maintain the variable states
 * 
 * @author ashish
 *
 */
public class CommandInterpreter {
	private ScriptEngine engine;
	private Bindings bindings;
	private static CommandInterpreter _instance = null;
	private String forCondition = "";
	private boolean loopCompletedOnce = false;
	private LoopCommand loopCommand = null;
	private CommandInterpreterState currentState = CommandInterpreterState.SEQUENTIAL_PROCESSING;
	private boolean insideIfBlock = false;
	private boolean insideForBlock = false;
	
	final static Pattern IF_STATEMENT_PATTERN = Pattern.compile("if(.+?)then", Pattern.CASE_INSENSITIVE);
	final static Pattern WHILE_STATEMENT_PATTERN = Pattern.compile("while(.+?)loop", Pattern.CASE_INSENSITIVE);
	
	public enum CommandInterpreterState {
		SEQUENTIAL_PROCESSING,
		LOOPING,	
		SKIPPING
	}
	
	
	private CommandInterpreter() {
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("js");
		bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
	}
	
	public static CommandInterpreter get() {
		if(_instance == null) {
			_instance = new CommandInterpreter();
		}
		
		return _instance;
	} 
	
	public Set<Entry<String, Object>> getAllVariables() {
		return bindings.entrySet();
	}
	
	public Object getVariableValue(String variable) throws CommandInterpreterException {
		checkVariable(variable);
		
		if(!doesVariableExist(variable)) {
			throw new CommandInterpreterException("Undefined variable '" + variable + "'");
		}
		
		return bindings.get(variable);
	}
	
	public void addVariable(String variableName, Object variableValue) throws CommandInterpreterException {
		checkVariable(variableName);		
		bindings.put(variableName, variableValue);
	}
	
	public boolean doesVariableExist(String variableName) {
		if(bindings.containsKey(variableName)) {
			return true;
		}
		return false;
	}
	
	private void checkVariable(String variable) throws CommandInterpreterException {
		if(variable == null || variable.isEmpty()) {
			throw new CommandInterpreterException("Variable name can not be empty!");
		}
	}
	
	
	public Command getProcessedCommand(String input) throws CommandInterpreterException {
		CommandType commandType = CommandInterpreterUtils.getCommandType(input);

		switch (commandType) {
			case CYTOSCAPE_STATEMENT:
				return handleCytoscapeStatement(input);
				
			case ASSIGNMENT:
				return handleAssignmentStatement(input);
			
			case IF_STATEMENT:
				return handleIfStatement(input);
				
			case ELSE_STATEMENT:
				return handleElseStatement(input);
				
			case END_IF_STATEMENT:
				return handleEndIfStatement(input);
				
			case FOR_STATEMENT:
				return handleForStatement(input);
				
			case END_FOR_STATEMENT:
				return handleEndForStatement(input);
				
			default:
				throw new CommandInterpreterException("Invalid command.");
		}
	}
	
	private Command handleCytoscapeStatement(String input) throws CommandInterpreterException {
		if(currentState.equals(CommandInterpreterState.SKIPPING)) {
			return null;
		} else if(currentState.equals(CommandInterpreterState.LOOPING) && !loopCompletedOnce) {
			loopCommand.addLoopCommand(input);
		}
		
		return new Command(CommandInterpreterUtils.preprocessSingleCommand(input));
	}
	
	private Command handleAssignmentStatement(String input) throws CommandInterpreterException {
		if(currentState.equals(CommandInterpreterState.SKIPPING)) {
			return null;
		} else if(currentState.equals(CommandInterpreterState.LOOPING) && !loopCompletedOnce) {
			loopCommand.addLoopCommand(input);
		}
		
		String[] pair = CommandInterpreterUtils.parseAssignmentCommand(input);
		String c = CommandInterpreterUtils.preprocessSingleCommand(pair[1]);
		
		try {
			String varName = (pair[0].replaceAll("\\$", ""));
			if(varName.equals("var")) {
				throw new CommandInterpreterException("'var' is a reserved word and can not be used as a variable name");
			}
			String assignmentCommand = varName + "=" + c;
			engine.eval(assignmentCommand);
		} catch (ScriptException ex) {
			// its not a valid script, means its a cytoscape command
			return new AssignmentCommand(c, pair[0]);
		}
		return null;
	}
	
	private Command handleIfStatement(String input) throws CommandInterpreterException {
		if(insideIfBlock || insideForBlock) {
			throw new CommandInterpreterException("Nesting not allowed.");
		}
		
		Matcher matcher = IF_STATEMENT_PATTERN.matcher(input);
		String condition = "";
		try {
			matcher.find();
			condition = matcher.group(1);
		} catch (Exception ex) {
			throw new CommandInterpreterException("Unable to parse if statement: " + input);
		}
		
		condition = condition.replaceAll("\\$", "");
		try {
			Boolean result = (Boolean) engine.eval(condition);
			if(!result) {
				currentState = CommandInterpreterState.SKIPPING;
			}
			
			insideIfBlock = true;				
		} catch (ScriptException ex) {
			throw new CommandInterpreterException("Unable to evalue if condition: " + input);
		}
		
		return null;
	}
	
	private Command handleElseStatement(String input) throws CommandInterpreterException {
		if(!insideIfBlock) {
			throw new CommandInterpreterException("Else without If.");
		}
		currentState = currentState.equals(CommandInterpreterState.SKIPPING) ? CommandInterpreterState.SEQUENTIAL_PROCESSING : CommandInterpreterState.SKIPPING;
		return null;
	}
	
	private Command handleEndIfStatement(String input) throws CommandInterpreterException {
		currentState = CommandInterpreterState.SEQUENTIAL_PROCESSING;
		insideIfBlock = false;
		return null;
	}
	
	private Command handleForStatement(String input) throws CommandInterpreterException {
		if(insideIfBlock || insideForBlock) {
			throw new CommandInterpreterException("Nesting not allowed.");
		}
		
		Matcher matcher = WHILE_STATEMENT_PATTERN.matcher(input);
		String condition = "";
		try {
			matcher.find();
			condition = matcher.group(1);
		} catch (Exception ex) {
			throw new CommandInterpreterException("Unable to parse for statement: " + input);
		}
		
		condition = condition.replaceAll("\\$", "");
		try {
			Boolean result = (Boolean) engine.eval(condition);
			if(!result) {
				currentState = CommandInterpreterState.SKIPPING;
			} else {
				currentState = CommandInterpreterState.LOOPING;
				this.forCondition = condition;
			}					
		} catch (ScriptException ex) {
			throw new CommandInterpreterException("Unable to evalue for condition: " + input);
		}
		
		insideForBlock = true;
		loopCommand = new LoopCommand();
		
		return null;
	}
	
	private Command handleEndForStatement(String input) throws CommandInterpreterException {
		if(currentState.equals(CommandInterpreterState.SKIPPING)) {
			return handleForEndWhenExistingLoop();
		} else if(currentState.equals(CommandInterpreterState.LOOPING)) {
			try {
				Boolean result = (Boolean) engine.eval(this.forCondition);
				if(!result) {
					return handleForEndWhenExistingLoop();
				} else {
					loopCompletedOnce = true;
					if(!loopCommand.getLoopCommands().contains(input)) {
						loopCommand.addLoopCommand(input);
					}
					return loopCommand;
				}			
			} catch (ScriptException ex) {
				throw new CommandInterpreterException("Unable to evalue for condition: " + input);
			}
		}
		
		return null;
	}
	
	private Command handleForEndWhenExistingLoop() {
		currentState = CommandInterpreterState.SEQUENTIAL_PROCESSING;
		insideForBlock = false;
		loopCompletedOnce = false;
		loopCommand.clearCommand();
		loopCommand = null;
		return null;
	}
}
