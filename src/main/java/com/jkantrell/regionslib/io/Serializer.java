package com.jkantrell.regionslib.io;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Permission;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.rules.Rule;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import com.jkantrell.regionslib.regions.dataContainers.RegionDataContainer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Serializer {

    //NEW IMPLEMENTATION
    public static final Gson GSON;
    static {
        GSON = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()

                .registerTypeAdapter(Hierarchy.class, new Hierarchy.JDeserializer())
                .registerTypeAdapter(Rule.class, new Rule.JSerializer())
                .registerTypeAdapter(Rule.class, new Rule.JDeserializer())
                .registerTypeAdapter(Region.class, new Region.JSerializer())
                .registerTypeAdapter(Region.class, new Region.JDeserializer())
                .registerTypeAdapter(Permission.class, new Permission.JSerializer())
                .registerTypeAdapter(RegionData.class, new RegionData.JSerializer())
                .registerTypeAdapter(RegionData.class, new RegionData.JDeserializer())
                .registerTypeAdapter(RegionDataContainer.class, new RegionDataContainer.JSerializer())
                .registerTypeAdapter(RegionDataContainer.class, new RegionDataContainer.JDeserializer())

                .create();
    }

    public static class FILES {
        public static final File REGIONS = new File(RegionsLib.CONFIG.configPath, "regions.json");
        public static final File HIERARCHIES = new File(RegionsLib.CONFIG.configPath, "hierarchies.json");
    }

    public static <T> T deserializeFile (File file, Class<T> typeOf) {
        Serializer.ensureFileExistence(file, false);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return Serializer.GSON.fromJson(reader,typeOf);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
    public static <T> List<T> deserializeFileList(File file, Class<T> typeOf) {
        Serializer.ensureFileExistence(file, true);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            ArrayList<T> list = new ArrayList<>();
            for (JsonElement element : array) {
                list.add(GSON.fromJson(element,typeOf));
            }
            return list;
        } catch (FileNotFoundException e) {
            return null;
        }
    }
    public static String serialize (Object toSerialize) {
        return GSON.toJson(toSerialize);
    }
    public static void serializeToFile(File file, Object toSerialize) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(serialize(toSerialize));
            writer.flush();
        } catch (IOException e) {

        }

    }

    private static void ensureFileExistence (File file, boolean array) {
        if (file.exists()) { return; }
        file.getParentFile().mkdirs();

        if (!array) { return; }
        try {
            FileWriter writer = new FileWriter(file);
            writer.write("[]");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
