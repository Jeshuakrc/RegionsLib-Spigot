package com.kntrel.mc.regionLib;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class RegionLibLogHandler extends Handler {
    private final Logger logger_;

    RegionLibLogHandler(String name) {
        this.logger_ = LogManager.getLogger(name);
    }

    private static Level fromJUL(java.util.logging.Level jul) {
        return switch (jul.intValue()) {
            case Integer.MAX_VALUE -> Level.OFF;
            case 1000 -> Level.ERROR;
            case 900 -> Level.WARN;
            case 800 -> Level.INFO;
            case 700, 500 -> Level.DEBUG;
            case 400, 300 -> Level.TRACE;
            case Integer.MIN_VALUE -> Level.ALL;
            default -> Level.forName(jul.getName(), jul.intValue());
        };
    }

    @Override
    public void publish(LogRecord record) {
        logger_.log(fromJUL(record.getLevel()), record.getMessage());
    }

    @Override public void flush() {}

    @Override public void close() throws SecurityException {}
}
