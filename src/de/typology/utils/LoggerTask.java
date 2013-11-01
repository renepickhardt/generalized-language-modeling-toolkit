package de.typology.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerTask implements Runnable {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private Logger logger;
	private FileHandler fileHandler;

	public LoggerTask() {
		this.logger = Logger.getLogger("SimpleLogger");
	}

	@Override
	public void run() {
		// configure the logger with handler and formatter

		try {
			this.fileHandler = new FileHandler("messages.log", true);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.logger.addHandler(this.fileHandler);
		this.logger.setLevel(Level.ALL);
		SimpleFormatter formatter = new SimpleFormatter();
		this.fileHandler.setFormatter(formatter);

	}

}
