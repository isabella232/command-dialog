package org.cytoscape.commandDialog.internal.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Command script preprocessor with utility methods to perform static analysis of command script and
 * handle basic variable substitution   
 * 
 * @author ashish
 *
 */
public class CommandInterpreterUtils {
	static final String VARIABLE_PREFIX = "$";
	static final String SPACE_CHARACTER = " ";
	static final String ARGS_SEPARATOR = ",";
	static final String KEY_VALUE_SEPARATOR = ":";
	static final String ASSIGNMENT_OPERATOR = ":=";
	static final String IF_STATEMENT_PREFIX = "IF";
	static final String ELSE_STATEMENT_PREFIX = "ELSE";
	static final String ENDIF_STATEMENT_PREFIX = "END IF";
	static final String FOR_STATEMENT_PREFIX = "FOR";
	static final String ENDFOR_STATEMENT_PREFIX = "END FOR";
	static final Pattern VARIABLE_EXTRACTION_PATTERN = Pattern.compile("\\$(\\w+)");
	
	public static CommandType getCommandType(String command) {
		if(command.contains(ASSIGNMENT_OPERATOR)) {
			return CommandType.ASSIGNMENT;
		}
		
		command = command.toUpperCase();
		if (command.startsWith(IF_STATEMENT_PREFIX)) {
			return CommandType.IF_STATEMENT;
		} else if (command.startsWith(ELSE_STATEMENT_PREFIX)) {
			return CommandType.ELSE_STATEMENT;
		} else if (command.startsWith(ENDIF_STATEMENT_PREFIX)) {
			return CommandType.END_IF_STATEMENT;
		} else if (command.startsWith(FOR_STATEMENT_PREFIX)) {
			return CommandType.FOR_STATEMENT;
		} else if (command.startsWith(ENDFOR_STATEMENT_PREFIX)) {
			return CommandType.END_FOR_STATEMENT;
		} else if (command.contains("+")) {
			return CommandType.END_FOR_STATEMENT;
		} else {	
			return CommandType.CYTOSCAPE_STATEMENT;
		}
	}
	
	public static String preprocessSingleCommand(String currentCommand) throws CommandInterpreterException {
		Matcher matcher = VARIABLE_EXTRACTION_PATTERN.matcher(currentCommand);
		String result = currentCommand;
		
		while(matcher.find()) {
			String variable = matcher.group(1);
			if(CommandInterpreter.get().doesVariableExist(variable)) {
				Object v = (CommandInterpreter.get().getVariableValue(variable));
				String value = "";
				if(v instanceof String) {
					value = "\"" + String.valueOf(v) + "\""; 
				} else {
					value = String.valueOf(v); 
				}
				result = result.replaceFirst("\\$" + variable, value);				
			} else {
				throw new CommandInterpreterException("Value for '" + variable + "' was not provided.");
			}
		}
		
		return result;
	}
	
	public static void parseAndInitializeCommandScriptArguments(String args) throws CommandInterpreterException {
		if(args != null && !args.isEmpty()) {
			args = args.trim();
			String[] parts = args.split(ARGS_SEPARATOR);
			
			for(String s : parts) {
				String[] keyValue = s.split(KEY_VALUE_SEPARATOR);
				
				if(keyValue == null || keyValue.length != 2) {
					throw new CommandInterpreterException("Invalid arguments supplied. Argument: " + s);
				}
				CommandInterpreter.get().addVariable(keyValue[0].trim(), keyValue[1].trim());
			}
		}
	}
	
	public static String[] parseAssignmentCommand(String command) {
		String map[] = new String[2];
		String[] parts = command.split(ASSIGNMENT_OPERATOR);
		
		if(parts == null || parts.length != 2) {
			throw new RuntimeException("Invalid assignment command.");
		}
		map[0] = parts[0].trim();
		map[1] = parts[1].trim();
		return map;
	}
}
