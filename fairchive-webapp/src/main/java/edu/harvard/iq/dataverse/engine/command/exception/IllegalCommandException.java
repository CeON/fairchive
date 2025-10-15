package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * Thrown when a command does not make sense - e.g moving a {@link Dataverse}
 * to one of its children.
 *
 * @author michael
 */
@SuppressWarnings("serial")
public class IllegalCommandException extends CommandException {

    public IllegalCommandException(String message, Command<?> aCommand) {
        super(message, aCommand);
    }

    public IllegalCommandException(String message) {
        super(message, null);
    }
}
