/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.archicontribs.database.GUI.DBGuiUtils;

import lombok.Getter;

/**
 * The BDLogger class is a proxy for the log4j Logger class.
 * 
 * @author Herve jouin
 */
public class DBLogger {
	private static boolean initialised = false;
	private String topCornerString = (char)0x250c+" ";
	private String verticalString = (char)0x2502+" ";
	private String bottomCornerString = (char)0x2514+" ";
	
	/**
	 * Gets the logger
	 */
	@Getter private Logger logger;

	/**
	 * Creates a proxy to to the Log4J logger class
	 * @param clazz Class that will be reported on the log lines
	 */
	public <T> DBLogger(Class<T> clazz) {
		if ( ! initialised ) {
			try {
				configure();
			} catch (Exception e) {
				initialised = false;
				DBGuiUtils.popup(Level.ERROR, "Failed to configure logger", e);
			}
		}
		this.logger = Logger.getLogger(clazz);
	}
	
	/**
	 * Configure the logger
	 * @throws Exception 
	 */
	public void configure() throws DBException {
		LinkedProperties properties = getLoggerProperties();

		if ( properties != null ) {
			PropertyConfigurator.configure(properties);
			initialised = true;
		} else {
			LogManager.shutdown();
			initialised = false;
		}
		
		if ( initialised ) {
			Logger oldLogger = this.logger;
			this.logger = Logger.getLogger(DBLogger.class);
			info("Logger initialised.");
			if ( isTraceEnabled() ) {
				StringBuilder param = new StringBuilder();
				String eol = "";
				if ( properties!= null ) {
					for ( Object oKey: properties.orderedKeys() ) {
						param.append("   "+(String)oKey+" = "+properties.getProperty((String)oKey)+eol);
						eol = "\n";
					}
					trace(param.toString());
				}
			}
			this.logger = oldLogger;
		}
    }

	/**
	 * Logs a message
	 * @param clazz Class that will be reported in the log line
     * @param level level of the log line (may be Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE)
     * @param message Message to print on the log line
     * @param t Exception to add to the log file (the exception message and stacktrace will be added to the log file)
	 */
	public <T> void log(Class<T> clazz, Level level, String message, Throwable t) {
		String className = clazz.getName();
		
		if ( initialised ) {
			String[] lines = message.split("\n");
			if ( lines.length == 1 ) {
				this.logger.log(className, level, "- "+message.replace("\r",""), t);
			} else {
				this.logger.log(className, level, this.topCornerString+lines[0].replace("\r",""), null);
				for ( int i=1 ; i < lines.length-1 ; ++i) {
					this.logger.log(className, level, this.verticalString+lines[i].replace("\r",""), null);
				}
				this.logger.log(className, level, this.bottomCornerString+lines[lines.length-1].replace("\r",""), t);
			}
		}
	}
	
	/**
	 * Logs a message
	 * @param level level of the log line (may be Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE)
     * @param message Message to print on the log line
	 */
	public void log(Level level, String message)						{ log(this.getClass(), level, message, null); }
	
	/**
	 * Logs a message
     * @param clazz Class that will be reported in the log line
     * @param level level of the log line (may be Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE)
     * @param message Message to print on the log line
	 */
	public <T> void log(Class<T> clazz, Level level, String message)	{ log(clazz, level, message, null); }
	
	/**
	 * Logs a fatal message
	 * @param message 
	 */
	public void fatal(String message)									{ log(this.getClass(), Level.FATAL, message, null); }
	
	/**
	 * Logs a fatal message
	 * @param message 
	 * @param t 
	 */
    public void fatal(String message, Throwable t)						{ log(this.getClass(), Level.FATAL, message, t); }
	/**
	 * Logs a fatal message
	 * @param clazz 
	 * @param message 
	 */
    public <T> void fatal(Class<T> clazz, String message)				{ log(clazz, Level.FATAL, message, null); }
	/**
	 * Logs a fatal message
	 * @param clazz 
	 * @param message 
	 * @param t 
	 */
    public <T> void fatal(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.FATAL, message, t); }
    
	/**
	 * Logs an error message
	 * @param message 
	 */
    public void error(String message)									{ log(this.getClass(), Level.ERROR, message, null); }
	/**
	 * Logs an error message
	 * @param message 
	 * @param t 
	 */
    public void error(String message, Throwable t)						{ log(this.getClass(), Level.ERROR, message, t); }
	/**
	 * Logs an error message
	 * @param clazz 
	 * @param message 
	 */
    public <T> void error(Class<T> clazz, String message)				{ log(clazz, Level.ERROR, message, null); }
	/**
	 * Logs an error message
	 * @param clazz 
	 * @param message 
	 * @param t 
	 */
    public <T> void error(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.ERROR, message, t); }
	
	/**
	 * Logs a warn message
	 * @param message 
	 */
    public void warn(String message)									{ log(this.getClass(), Level.WARN, message, null); }
	/**
	 * Logs a warn message
	 * @param message 
	 * @param t 
	 */
    public void warn(String message, Throwable t)						{ log(this.getClass(), Level.WARN, message, t); }
	/**
	 * Logs a warn message
	 * @param clazz 
	 * @param message 
	 */
    public <T> void warn(Class<T> clazz, String message)				{ log(clazz, Level.WARN, message, null); }
	/**
	 * Logs a warn message
	 * @param clazz 
	 * @param message 
	 * @param t 
	 */
    public <T> void warn(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.WARN, message, t); }
    
	/**
	 * Logs an info message
	 * @param message to print in the log file
	 */
    public void info(String message)									{ log(this.getClass(), Level.INFO, message, null); }
	/**
	 * Logs an info message
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
    public void info(String message, Throwable t)						{ log(this.getClass(), Level.INFO, message, t); }
	/**
	 * Logs an info message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 */
    public <T> void info(Class<T> clazz, String message)				{ log(clazz, Level.INFO, message, null); }
	/**
	 * Logs an info message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
    public <T> void info(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.INFO, message, t); }
    
	/**
	 * Logs a debug message
	 * @param message to print in the log file
	 */
    public void debug(String message)									{ log(this.getClass(), Level.DEBUG, message, null); }
	/**
	 * Logs a debug message
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
	public void debug(String message, Throwable t)						{ log(this.getClass(), Level.DEBUG, message, t); }
	/**
	 * Logs a debug message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 */
    public <T> void debug(Class<T> clazz, String message)				{ log(clazz, Level.DEBUG, message, null); }
	/**
	 * Logs a debug message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
    public <T> void debug(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.DEBUG, message, t); }
    
	/**
	 * Logs a trace message
	 * @param message to print in the log file
	 */
    public void trace(String message)									{ log(this.getClass(), Level.TRACE, message, null); }
	/**
	 * Logs a trace message
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
    public void trace(String message, Throwable t)						{ log(this.getClass(), Level.TRACE, message, t); }
	/**
	 * Logs a trace message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 */
    public <T> void trace(Class<T> clazz, String message)				{ log(clazz, Level.TRACE, message, null); }
	/**
	 * Logs a trace message
	 * @param clazz class for which the message is printed to the log file
	 * @param message to print in the log file
	 * @param t exception to add to the log file
	 */
    public <T> void trace(Class<T> clazz, String message, Throwable t)	{ log(clazz, Level.TRACE, message, t); }

	/**
	 * Get the initialized state of the logger
	 * @return true if the logger is initialized, false if it needs to be initialized
	 */
    public static boolean isInitialised() {
    	return initialised;
    }
    
	/**
	 * Gets the logger properties
	 * @return the logger properties
	 * @throws DBException if the properties cannot be get
	 */
	private LinkedProperties getLoggerProperties() throws DBException {
		//LogManager.resetConfiguration();
		
		
		String loggerMode = DBPlugin.INSTANCE.getPreferenceStore().getString("loggerMode");
		if ( loggerMode == null )
			return null;

		LinkedProperties properties = new LinkedProperties() {
			private static final long serialVersionUID = 1L;
			@Override
			public Set<Object> keySet() { return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet())); }
		};
		
		debug("getting logger preferences from store");
		
		switch (loggerMode) {
		case "disabled":
			return null;
			
		case "simple":
    		properties.setProperty("log4j.rootLogger",									DBPlugin.INSTANCE.getPreferenceStore().getString("loggerLevel").toUpperCase()+", stdout, file");
    		
    		properties.setProperty("log4j.appender.stdout",								"org.apache.log4j.ConsoleAppender");
    		properties.setProperty("log4j.appender.stdout.Target",						"System.out");
    		properties.setProperty("log4j.appender.stdout.layout",						"org.apache.log4j.PatternLayout");
    		properties.setProperty("log4j.appender.stdout.layout.ConversionPattern",	"%d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-40.40C{1} %m%n");
    		
    		properties.setProperty("log4j.appender.file",								"org.apache.log4j.FileAppender");
    		properties.setProperty("log4j.appender.file.ImmediateFlush",				"true");
    		properties.setProperty("log4j.appender.file.Append",						"false");
    		properties.setProperty("log4j.appender.file.Encoding",						"UTF-8");
    		properties.setProperty("log4j.appender.file.File",							DBPlugin.INSTANCE.getPreferenceStore().getString("loggerFilename"));
    		properties.setProperty("log4j.appender.file.layout", 						"org.apache.log4j.PatternLayout");
    		properties.setProperty("log4j.appender.file.layout.ConversionPattern",		"%d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-40.40C{1} %m%n");
    		break;
    		
		case "expert":
    		String loggerExpert = DBPlugin.INSTANCE.getPreferenceStore().getString("loggerExpert");
    		if ( loggerExpert == null ) DBPlugin.INSTANCE.getPreferenceStore().getDefaultString("loggerExpert");
    		
    		try {
				properties.load(new StringReader(loggerExpert));
			} catch (@SuppressWarnings("unused") IOException err) {
				throw new DBException("Error while parsing \"loggerExpert\" properties from the preference store");
			}
    		break;
    		
			default:
				break;
		}
		
		return properties;
	}
	
	/**
	 * List that maintain elements order 
	 */
	private class LinkedProperties extends Properties {
		private static final long serialVersionUID = 1L;
		
		private final HashSet<Object> keys = new LinkedHashSet<>();

	    public LinkedProperties() {
	    	super();
	    }

	    public Iterable<Object> orderedKeys() {
	        return Collections.list(keys());
	    }

	    @Override
        public synchronized Enumeration<Object> keys() {
	        return Collections.<Object>enumeration(this.keys);
	    }

	    @Override
        public synchronized Object put(Object key, Object value) {
	        this.keys.add(key);
	        return super.put(key, value);
	    }
	}
	
	/**
	 * @return true if the logger is configured to print trace messages
	 */
	public boolean isTraceEnabled() {
		return initialised && this.logger.isTraceEnabled();
	}
	
	/**
	 * @return true if the logger is configured to print debug messages
	 */
	public boolean isDebugEnabled() {
		return initialised && this.logger.isDebugEnabled();
	}
	
	/**
     * @return true if the logger is configured to print SQL requests
     */
    public boolean isTraceSQLEnabled() {
	    return isTraceEnabled() && DBPlugin.INSTANCE.getPreferenceStore().getBoolean("traceSQL");
	}
}
