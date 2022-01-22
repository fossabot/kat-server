package com.catkatpowered.katserver.log;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KatLogger {

    private static final KatLogger INSTANCE = new KatLogger();

    @Getter @Setter
    private HashMap<String, Logger> loggerManager = new HashMap<String, Logger>();

    private KatLogger() {
    }

    public static KatLogger getInstance() {
        return INSTANCE;
    }

    public  Logger getLogger(String name) {

        var logger = this.loggerManager.get(name);

        if (logger == null) {
            logger = LogManager.getLogger(name);
            this.loggerManager.put(name, logger);
        }
        return logger;
    }
}
