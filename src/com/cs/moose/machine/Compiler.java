package com.cs.moose.machine;

import com.cs.moose.exceptions.*;
import com.cs.moose.types.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.*;

public class Compiler {
	static final Command[] commandSet = Command.values();
	private static final int MAX_MEMORY_SIZE = 65536;
	
	public static Machine getMachine(Lexer lexer) throws JumpPointException, CompilerException {
		ArrayList<String> requiredJumpDeclarations = lexer.getJumpCalls(),
						  strings = new ArrayList<String>();
		
		Hashtable<String, JumpPoint> declaredJumpPoints = lexer.getJumpPointDefinitions();
		
		for (String point : requiredJumpDeclarations) {
			if (!declaredJumpPoints.containsKey(point)) {
				throw new JumpPointException("JumpPoint not defined", point);
			}
		}
		
		short[] memory = new short[MAX_MEMORY_SIZE];
		String[] code = lexer.getProcessedCode();
		
		int i = 0;
		for (String line : code) {
			line = line.trim();
			
			if (line.length() > 0 && !line.endsWith(":")) {
				String command = null, parameter = null;
				
				Command memoryCommand = Command.FAIL;
				
				Matcher matcherJumpCommand = Lexer.patternJumpCommand.matcher(line),
						matcherJumpCommandParameter = Lexer.patternJumpCommandParameter.matcher(line);
				
				if (matcherJumpCommandParameter.find()) { // command with parameter
					command = matcherJumpCommandParameter.group(2);
					parameter = matcherJumpCommandParameter.group(3);
				} else if (matcherJumpCommand.find()) { // simple command without parameter
					command = matcherJumpCommand.group(2);
				} 
				
				command = command.toUpperCase();
				
				for (Command originalCommand : commandSet) {
					if (command.equals(originalCommand.name())) {
						memoryCommand = originalCommand;
					}
				}
				
				switch (memoryCommand) {
					case NOT:
					case HOLD:
					case NOOP: 
					case RESET: 
					case PUTA:
						memory[i] = memoryCommand.numeric();
						memory[i + 1] = 0;
						break;
						
					case JEQ:
					case JGE:
					case JGT:
					case JLE:
					case JLT:
					case JNE:
					case JOV:
					case JMP:
					case JMPN:
					case JMPNN:
					case JMPNP:
					case JMPNZ:
					case JMPP:
					case JMPV:
					case JMPZ:
						JumpPoint point = declaredJumpPoints.get(parameter);
						
						memory[i] = memoryCommand.numeric();
						memory[i + 1] = point.getMemoryPosition();
						break;
						
					case PUTS:
						if (parameter.matches("(\"([^\"]*)\"|'([^']*)'|)")) {
							parameter = parameter.substring(1, parameter.length() - 1);
							
							// add possibility to print new lines by replacing "\\n" with "\n". to prevent replacing "\\\\n" with "\n", replace "\\\n" back to original
							parameter = parameter.replaceAll("\\\\n", "\n").replaceAll("\\\\\n", "\\\\n");
							parameter = parameter.replaceAll("\\\\t", "\t").replaceAll("\\\\\t", "\\\\t");
							
							strings.add(parameter);
							
							memory[i] = memoryCommand.numeric();
							memory[i + 1] = (short)(strings.size() - 1);
							
							break;
						}
						
					default: 
						try {
							memory[i] = memoryCommand.numeric();
							memory[i + 1] = (short)Integer.parseInt(parameter);
						} catch (Exception ex) {
							throw new CompilerException("Command \"" + memoryCommand + "\" doesn't support the given parameters (parameter was \"" + parameter + "\")");
						}
						break;
				}
				
				i += 2;
			}
		}
		
		return new Machine(memory, strings);
	}
}
