package com.jkantrell.regionslib.regions.rules;

import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RuleKey {

    //STATIC FIELDS
    private static final HashMap<String,RuleKey> allRuleKeys_ = new HashMap<>();

    //FIELDS
    private final String label_;
    private final JavaPlugin plugin_;
    private final RuleDataType<?> dataType_;
    private boolean isDisposable_ = false;
    private String accessPermission_ = null;

    //CONSTRUCTORS
    private RuleKey(JavaPlugin plugin, String label, RuleDataType<?> dataType) {
        this.plugin_ = plugin;
        this.label_ = label;
        this.dataType_ = dataType;
    }

    //Static methods
    public static RuleKey registerNew(JavaPlugin plugin, String label, RuleDataType<?> dataType) {
        if (RuleKey.allRuleKeys_.containsKey(label)) {
            RuleKey key = RuleKey.allRuleKeys_.get(label);
            if (!key.isDisposable()) {
                throw new IllegalArgumentException("There's already a rule key labeled '" + label + "' registered by plugin " + key.plugin_.getName() + ".");
            }
            RuleKey.allRuleKeys_.remove(label);
        }
        RuleKey r = new RuleKey(plugin,label,dataType);
        RuleKey.allRuleKeys_.put(label, r);
        return r;
    }
    public static boolean exists(String label, RuleDataType<?> dataType) {
        if (!RuleKey.allRuleKeys_.containsKey(label)) { return false; }
        return RuleKey.allRuleKeys_.get(label).dataType_.equals(dataType);
    }
    public static boolean exists(String label) {
        return RuleKey.allRuleKeys_.containsKey(label);
    }
    public static List<RuleKey> getAll() {
        return new ArrayList<>(RuleKey.allRuleKeys_.values());
    }
    public static RuleKey get(String label) {
        if (!RuleKey.exists(label)) { return null; }
        return RuleKey.allRuleKeys_.get(label);
    }

    //SETTERS
    void setDisposable(boolean isDisposable) {
        this.isDisposable_ = isDisposable;
    }
    public void setAccessPermission(String permission) {
        this.accessPermission_ = permission;
    }

    //GETTERS
    boolean isDisposable() {
        return this.isDisposable_;
    }
    public String getLabel() {
        return this.label_;
    }
    public JavaPlugin getPluginWhoRegistered() {
        return this.plugin_;
    }
    public RuleDataType<?> getDataType() {
        return this.dataType_;
    }

    //METHODS
    public boolean testPermission(Permissible target) {
        if (this.accessPermission_ == null) { return true; }
        return target.hasPermission(this.accessPermission_);
    }

}
