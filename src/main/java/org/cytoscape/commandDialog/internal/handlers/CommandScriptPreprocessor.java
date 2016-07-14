package org.cytoscape.commandDialog.internal.handlers;

import java.util.HashMap;
import java.util.Map;
/**
 * Command script preprocessor with utility methods to perform static analysis of command script and
 * handle basic variable substitution   
 * 
 * @author ashish
 *
 */
public class CommandScriptPreprocessor {
	private static final String VARIABLE_PREFIX = "$";
	private static final String SPACE_CHARACTER = " ";
	private static final String ARGS_SEPARATOR = ",";
	private static final String KEY_VALUE_SEPARATOR = ":";
	private static final String ASSIGNMENT_OPERATOR = ":=";
	
	public static enum CommandType {
		STATEMENT,
		ASSIGNMENT
	}
	
	public static CommandType getCommandType(String command) {
		if(command.contains(ASSIGNMENT_OPERATOR)) {
			return CommandType.ASSIGNMENT;
		} else {
			return CommandType.STATEMENT;
		}
	}
	
	public static String preprocessSingleCommand(String currentCommand, Map<String, String> userArguments) {
		if(userArguments == null || userArguments.size() == 0) {
			return currentCommand;
		}
		
		StringBuilder sb = new StringBuilder(currentCommand);
		int nextVariableStartingIndex = sb.indexOf(VARIABLE_PREFIX);
		int nextVariableEndingIndex;
		String potentialMatch;
		
		while(nextVariableStartingIndex != -1) {
			nextVariableEndingIndex = sb.indexOf(SPACE_CHARACTER, nextVariableStartingIndex);
			if(nextVariableEndingIndex == -1) {
				nextVariableEndingIndex = sb.length();
			}
			potentialMatch = sb.substring(nextVariableStartingIndex + 1, nextVariableEndingIndex);
			if(userArguments.containsKey(potentialMatch)) {
				sb.replace(nextVariableStartingIndex, nextVariableEndingIndex, String.valueOf(userArguments.get(potentialMatch)));
			} else {
				throw new RuntimeException("Value for '" + potentialMatch + "' was not provided.");
			}
			
			nextVariableStartingIndex = sb.indexOf(VARIABLE_PREFIX);
		}
		
		return sb.toString();
	}
	
	public static Map<String, String> constructUserArgumentMap(String args) {
		Map<String, String> map = new HashMap<>();
		if(args != null) {
			args = args.trim();
			String[] parts = args.split(ARGS_SEPARATOR);
			
			for(String s : parts) {
				String[] keyValue = s.split(KEY_VALUE_SEPARATOR);
				
				if(keyValue == null || keyValue.length != 2) {
					throw new RuntimeException("Invalid arguments supplied");
				}
				
				map.put(keyValue[0].trim(), keyValue[1].trim());
			}
		}
		
		return map;
	}
	
	public static Map<String, String> parseAssignmentCommand(String command) {
		Map<String, String> map = new HashMap<>(1);
		String[] parts = command.split(ASSIGNMENT_OPERATOR);
		
		if(parts == null || parts.length != 2) {
			throw new RuntimeException("Invalid assignment command.");
		}
		
		map.put(parts[0].trim(), parts[1].trim());
		return map;
	}
}
