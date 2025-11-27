package madmike.skirmish.util;

import org.slf4j.Logger;

/**
 * Utility class that wraps SLF4J Logger to automatically capture and log stack traces
 * for all error and exception logging calls.
 */
public class LoggerUtil {
	private final Logger logger;

	public LoggerUtil(Logger logger) {
		this.logger = logger;
	}

	public void trace(String msg) {
		logger.trace(msg);
	}

	public void trace(String msg, Object arg) {
		logger.trace(msg, arg);
	}

	public void trace(String msg, Object arg1, Object arg2) {
		logger.trace(msg, arg1, arg2);
	}

	public void trace(String msg, Object... arguments) {
		logger.trace(msg, arguments);
	}

	public void trace(String msg, Throwable t) {
		logger.trace(msg, t);
	}

	public void debug(String msg) {
		logger.debug(msg);
	}

	public void debug(String msg, Object arg) {
		logger.debug(msg, arg);
	}

	public void debug(String msg, Object arg1, Object arg2) {
		logger.debug(msg, arg1, arg2);
	}

	public void debug(String msg, Object... arguments) {
		logger.debug(msg, arguments);
	}

	public void debug(String msg, Throwable t) {
		logger.debug(msg, t);
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void info(String msg, Object arg) {
		logger.info(msg, arg);
	}

	public void info(String msg, Object arg1, Object arg2) {
		logger.info(msg, arg1, arg2);
	}

	public void info(String msg, Object... arguments) {
		logger.info(msg, arguments);
	}

	public void info(String msg, Throwable t) {
		logger.info(msg, t);
	}

	public void warn(String msg) {
		logger.warn(msg);
	}

	public void warn(String msg, Object arg) {
		logger.warn(msg, arg);
	}

	public void warn(String msg, Object arg1, Object arg2) {
		logger.warn(msg, arg1, arg2);
	}

	public void warn(String msg, Object... arguments) {
		logger.warn(msg, arguments);
	}

	public void warn(String msg, Throwable t) {
		logger.warn(msg, t);
	}

	public void error(String msg) {
		logger.error(msg, new Exception("Stack trace for: " + msg));
	}

	public void error(String msg, Object arg) {
		logger.error(msg, arg, new Exception("Stack trace for: " + msg));
	}

	public void error(String msg, Object arg1, Object arg2) {
		logger.error(msg, arg1, arg2, new Exception("Stack trace for: " + msg));
	}

	public void error(String msg, Object... arguments) {
		Throwable t = null;
		if (arguments.length > 0 && arguments[arguments.length - 1] instanceof Throwable) {
			t = (Throwable) arguments[arguments.length - 1];
		} else {
			t = new Exception("Stack trace for: " + msg);
		}
		logger.error(msg, arguments, t);
	}

	public void error(String msg, Throwable t) {
		logger.error(msg, t);
	}

	public void catchException(String context, Exception e) {
		logger.error("Exception caught in {}: {}", context, e.getMessage(), e);
	}

	public void catchThrowable(String context, Throwable t) {
		logger.error("Throwable caught in {}: {}", context, t.getMessage(), t);
	}
}
