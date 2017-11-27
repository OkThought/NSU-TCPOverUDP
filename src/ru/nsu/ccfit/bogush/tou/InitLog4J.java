package ru.nsu.ccfit.bogush.tou;

import java.nio.file.Paths;

public class InitLog4J {
    private static final String CONFIG_FILE = "logging.xml";
    private static volatile boolean init = false;

    static void initIfNotInitYet() {
        if (!init) {
            init = true;
            System.setProperty("log4j.configurationFile", CONFIG_FILE);
        }
    }

    private InitLog4J() {}
}
