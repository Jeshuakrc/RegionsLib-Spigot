package com.jkantrell.regionslib.region.rule;

import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RuleKey<T> {

    private String key_;

    private RuleDataType<T> dataType_;


    //CONSTRUCTORS
    public RuleKey(String key, RuleDataType<T> dataType) {
        this.key_ = key;
        this.dataType_ = dataType;
    }


    //GETTERS
    public String getKey() {
        return this.key_;
    }
    public RuleDataType<T> getDataType() {
        return this.dataType_;
    }
    public boolean isKey(String key) {
        return this.equals(key);
    }


    //IMPLEMENTATION
    @Override public int hashCode() {
        return this.key_.hashCode();
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o instanceof String s) { return s.equals(this.key_); }
        if (!(o instanceof RuleKey<?> other)) { return false; }
        if (!other.getKey().equals(this.key_)) { return false; }
        return other.dataType_.equals(this.dataType_);
    }
    public boolean equals(String s) {
        return this.key_.equals(s);
    }
}
