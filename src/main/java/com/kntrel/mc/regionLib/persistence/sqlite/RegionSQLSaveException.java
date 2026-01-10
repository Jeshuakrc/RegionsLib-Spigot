package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.exception.RegionPersistenceException;
import java.sql.SQLException;

public class RegionSQLSaveException extends RegionPersistenceException {

    //FIELDS
    private final String statement_;


    //CONSTRUCTORS
    public RegionSQLSaveException(Region region, SQLException cause) {
        super(region, cause);
        this.statement_ = null;
    }
    public RegionSQLSaveException(Region region, String statement, SQLException cause) {
        super(region, cause);
        this.statement_ = statement;
    }


    //GETTERS
    public String getStatement() {
        return this.statement_;
    }
    @Override public SQLException getCause() {
        return (SQLException) super.getCause();
    }
    @Override public String getMessage() {
        String msg = "Failed to persist region id '" + this.getRegion().getId() + "' to SQLite.";
        if (this.statement_ == null) {
            return msg;
        }
        return msg + " Statement: " + this.statement_;
    }
}
