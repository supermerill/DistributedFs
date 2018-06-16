package remi.distributedFS.log;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {

	static public Logger logNet;
	static public Logger logOs;
	static public Logger logDb;
	static public Logger logManager;
	static public Logger logGui;

    static private FileHandler netFile;
    static private FileHandler osFile;
    static private FileHandler dbFile;
    static private FileHandler guiFile;
    static private FileHandler managerFile;
	private static Formatter formatterTxt;
	static {
		try {
		    formatterTxt = new TimeTypeFormatter();
		
	        
		    logNet = Logger.getLogger("NET");
		    netFile = new FileHandler("net.log");
		    netFile.setLevel(Level.ALL);
	        netFile.setFormatter(formatterTxt);
	        logNet.addHandler(netFile);

	        logOs = Logger.getLogger("OS");
		    osFile = new FileHandler("os.log");
		    osFile.setLevel(Level.ALL);
		    osFile.setFormatter(formatterTxt);
	        logOs.addHandler(osFile);

	        logDb = Logger.getLogger("DB");
	        dbFile = new FileHandler("db.log");
	        dbFile.setLevel(Level.ALL);
	        dbFile.setFormatter(formatterTxt);
	        logDb.addHandler(dbFile);


	        logManager = Logger.getLogger("MANAGER");
	        managerFile = new FileHandler("manager.log");
	        managerFile.setLevel(Level.ALL);
	        managerFile.setFormatter(formatterTxt);
	        logManager.addHandler(managerFile);


	        logGui = Logger.getLogger("GUI");
	        guiFile = new FileHandler("gui.log");
	        guiFile.setLevel(Level.ALL);
	        guiFile.setFormatter(formatterTxt);
	        logGui.addHandler(guiFile);
	        

		    Logger rootLogger = logNet.getParent();
		    Handler[] handlers = rootLogger.getHandlers();
	        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
	        	rootLogger.removeHandler(handlers[0]);
	        	rootLogger.addHandler(new ConsoleOutHandler());
	        }
	        

		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
        
	}

}
