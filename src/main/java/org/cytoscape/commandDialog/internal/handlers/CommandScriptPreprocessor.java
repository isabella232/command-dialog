package org.cytoscape.commandDialog.internal.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
	
	/**
	 * Interprets the source script line by line and substitute user defined variables with provided values
	 * The resulting script is written back to the source file.
	 * 
	 * @param sourceFilePath
	 * @param userArguments
	 * @throws IOException
	 * @returns path of the pre-processed file
	 */
	public static String preprocess(Map<String, Object> userArguments) throws Exception {
		String sourceFilePath = userArguments.get("file") == null ? null : String.valueOf(userArguments.get("file"));
		List<String> outputScript = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath))){
			String currentCommand = null;
			
			while ((currentCommand = reader.readLine()) != null) {
				if(currentCommand != null && !currentCommand.isEmpty()) {
					outputScript.add(preprocessSingleCommand(currentCommand, userArguments));
				}				
			}
		}
		
		return writePreprocessedScriptToFile(sourceFilePath, outputScript);
	}

	private static String preprocessSingleCommand(String currentCommand, Map<String, Object> userArguments) {
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
	
	private static String writePreprocessedScriptToFile(String sourcePath, List<String> commands) throws Exception {
		String outputFilePath = getIntermediateScriptPath(sourcePath);
		
		try (PrintWriter writer = new PrintWriter(outputFilePath)){
			for(String command : commands) {
				writer.println(command);
			}
		}	
		return outputFilePath;
	}
	
	private static String getIntermediateScriptPath(String sourcePath) {
		File exitingFile = new File(sourcePath);
		File parentDir = exitingFile.getParentFile();
		File intermediateFile = new File(parentDir, exitingFile.getName() + "-intermediate");
		return intermediateFile.getAbsolutePath();		
	}
}
