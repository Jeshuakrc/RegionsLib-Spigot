package com.jkantrell.regionslib.io;

import com.jkantrell.regionslib.RegionsLib;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public abstract class LangManager {

    //FIELDS
    private static String langPath_;
    private static HashMap<String, YamlConfiguration> langFiles_ = new HashMap<>();
    private static String langDirName_= "lang";
    public static String ext_ = ".yml";

    //PUBLIC METHODS
    public static String getString(String path, Player player, String... parms) {
        String val = LangManager.getStringNonFormatted(path,player);
        return String.format(val, parms);
    }
    public static String getStringNonFormatted(String path, Player player){
        YamlConfiguration langFile = LangManager.getFileFromLang_(player.getLocale());
        String val = langFile.getString(path);
        if (val.length()>0) {
            if (val.charAt(0) == '%' && val.charAt(1) == '/') {
                Path newPath = Paths.get(LangManager.getLangsPath() + val.substring(1));
                File file = new File(newPath.toString());
                if (file.exists()) {
                    try {
                        val = Files.readString(newPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        val = val.replaceAll("\r","");
        return val;
    }
    public static int getInt(String path, Player player){
        YamlConfiguration langFile = LangManager.getFileFromLang_(player.getLocale());
        return langFile.getInt(path);
    }
    public static String getFormattedList(Player player, String[] list){

        return LangManager.getFormattedList(player, list.length, list);
    }
    public static String getFormattedList(Player player, int maxToInclude, String[] list){

        StringBuilder s = new StringBuilder();
        boolean isLast = false;
        int length = list.length;
        for (int i = 0; !isLast; i++){

            isLast = i >= (length - 1);
            if(i == 0) {
                s.append(list[i]);
            } else {
                String add;
                if (i > maxToInclude - 1) {
                    String rem = Integer.toString(length - i);
                    add = LangManager.getString("lists_remaining", player, rem);
                    s.append(add);
                    break;
                } else {
                    add = ( isLast  ? LangManager.getString("lists_lastSeparator", player)
                                    : LangManager.getString("lists_separator", player) ) + list[i];
                    s.append(add);
                }
            }
        }
        return s.toString();
    }
    public static String getLangFileName(Player player) {
        return getFileFromLang_(player.getLocale()).getDefaults().getString("langFileName");
    }


    //PROTECTED METHODS
    protected static String getLangsPath() {

        if (langPath_==null) {
            Path path = Paths.get(ConfigManager.getConfigPath() + "/" + langDirName_);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            langPath_ = path.toString();
        }
        return langPath_;
    }

    //PRIVATE METHODS
    private static YamlConfiguration getFileFromLang_(String langCode) {

        File file;
        YamlConfiguration conf = null;

        if (langFiles_.containsKey(langCode)) {
            conf = langFiles_.get(langCode);
            conf.addDefault("langFileName", langCode + ext_);
        } else {
            String langFile = langCode + ext_;
            file = new File(getLangsPath() + "/" + langFile);
            if (!file.exists()) {
                InputStream resource = RegionsLib.getMain().getResource(langDirName_ + "/" + langFile);
                if (resource != null) {
                    try {
                        OutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(resource.readAllBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    file = null;
                    String newLang = langCode.split("_")[0];
                    if (!newLang.equals(langCode)) {
                        return LangManager.getFileFromLang_(newLang);
                    } else {
                        String defaultLang = ConfigManager.getDefaultLanguageCode();
                        if (!defaultLang.equals(langCode)) {
                            return LangManager.getFileFromLang_(defaultLang);
                        }
                    }
                }
            }
            if (file != null) {
                try {
                    conf = new YamlConfiguration();
                    conf.load(file);
                    langFiles_.put(langCode,conf);
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }
            }
            conf.addDefault("langFileName", langFile);
        }
        return conf;
    }
}
