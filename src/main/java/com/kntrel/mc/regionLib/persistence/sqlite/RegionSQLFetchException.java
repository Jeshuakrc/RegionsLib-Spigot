package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.region.exception.RegionFetchException;

import java.sql.SQLException;

public class RegionSQLFetchException extends RegionFetchException {

    //FIELDS
    private final String statement_;


    //CONSTRUCTORS
    public RegionSQLFetchException(String statement, SQLException cause) {
        super(cause);
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
        return "Failed to fetch region from SQLite. Statement: " + this.statement_;
    }

}
