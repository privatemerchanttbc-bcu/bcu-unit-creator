package unitcreator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static PrintWriter writer;
    private static final File LOG_FILE = resolveLogFile();

    static {
        try {
            writer = new PrintWriter(new FileWriter(LOG_FILE, false), true);
            writer.println("=== Log started " + new Date() + " ===");
            writer.flush();
        } catch (Exception e) {
            System.err.println("[UnitCreator] Cannot open log file: " + e);
        }
    }

    private Logger() {}

    private static File resolveLogFile() {
        String configured = System.getProperty("unitcreator.log");
        File file = configured != null && configured.trim().length() > 0
                ? new File(configured)
                : new File("unit-creator.log");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return file;
    }

    public static synchronized void log(String msg) {
        String line = "[" + FMT.format(new Date()) + "] " + msg;
        System.out.println("[UnitCreator] " + msg);
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }

    public static synchronized void err(String msg, Throwable t) {
        String line = "[" + FMT.format(new Date()) + "] ERROR: " + msg;
        System.err.println("[UnitCreator] ERROR: " + msg);
        if (writer != null) {
            writer.println(line);
            if (t != null) t.printStackTrace(writer);
            writer.flush();
        }
        if (t != null) t.printStackTrace();
    }

    public static File getLogFile() { return LOG_FILE; }

    public static synchronized void closeForTests() {
        if (writer == null) return;
        writer.flush();
        writer.close();
        writer = null;
    }
}
