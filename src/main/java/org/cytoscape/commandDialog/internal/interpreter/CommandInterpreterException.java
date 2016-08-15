package org.cytoscape.commandDialog.internal.interpreter;

public class CommandInterpreterException extends Exception {
	private static final long serialVersionUID = -8351438938630518367L;

	public CommandInterpreterException () {
		super();
    }

    public CommandInterpreterException (String message) {
        super (message);
    }

    public CommandInterpreterException (Throwable cause) {
        super (cause);
    }

    public CommandInterpreterException (String message, Throwable cause) {
        super (message, cause);
    }
}
