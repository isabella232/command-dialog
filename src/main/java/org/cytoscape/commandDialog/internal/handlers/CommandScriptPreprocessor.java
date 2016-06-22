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
			String[] parts = args.split(",");
			for(String s : parts) {
				String[] keyValue = s.split(":");
				if(keyValue == null || keyValue.length != 2) {
					throw new RuntimeException("Invalid arguments supplied");
				}
				map.put(keyValue[0], keyValue[1]);
			}
		}
		
		return map;
	}
}
