package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class JsonHierarchyRepository implements HierarchyRepository {

    //STATIC
    private static final Type TYPE_TOKEN = new TypeToken<List<Hierarchy>>(){}.getType();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Hierarchy.class, new HierarchyDeserializer()).create();


    //FIELDS
    private final File jsonFile;
    private final Map<Long, Hierarchy> hierarchies;
    private final ThreadPoolExecutor writeExecutor;


    //CONSTRUCTORS
    public JsonHierarchyRepository(File file) {
        this.jsonFile = file;
        this.hierarchies = new HashMap<>();
        this.writeExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>());

        if (!this.jsonFile.exists()) { return; }

        try (FileReader reader = new FileReader(this.jsonFile)) {
            List<Hierarchy> list = GSON.fromJson(reader, TYPE_TOKEN);
            list.forEach(h -> this.hierarchies.put(h.getId(), h));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    //IMPLEMENTATION
    @Override
    public List<Hierarchy> getAll() {
        return List.copyOf(this.hierarchies.values());
    }
    @Override
    public Optional<Hierarchy> get(Long id) {
        return Optional.ofNullable(this.hierarchies.get(id));
    }

    @Override
    public List<Hierarchy> getByName(String name) {
        return this.hierarchies.values().stream().filter(h -> h.getName().equals(name)).toList();
    }

    @Override
    public void save(Hierarchy hierarchy) {
        this.hierarchies.put(hierarchy.getId(), hierarchy);

        if (!this.jsonFile.exists()) {
            this.jsonFile.getParentFile().mkdirs();
        }

        this.writeExecutor.getQueue().clear();

        String json = GSON.toJson(this.hierarchies.values());
        this.writeExecutor.submit(() -> {
            try (FileWriter writer = new FileWriter(this.jsonFile)) {
                writer.write(json);
                writer.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    // INNER CLASSES
    private static class HierarchyDeserializer implements JsonDeserializer<Hierarchy> {

        @Override
        public Hierarchy deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonHierarchy = json.getAsJsonObject();

            Hierarchy hierarchy = new Hierarchy(
                    jsonHierarchy.get("id").getAsLong(),
                    jsonHierarchy.get("name").getAsString()
            );

            JsonObject jsonGroup; ArrayList<String> abilities;
            for (JsonElement element : jsonHierarchy.get("groups").getAsJsonArray()) {
                jsonGroup = element.getAsJsonObject();
                abilities = new ArrayList<>();
                String ability;
                for (JsonElement element1 : jsonGroup.get("abilities").getAsJsonArray()) {
                    ability = element1.getAsString().toLowerCase().trim();
                    abilities.add(ability);
                }
                hierarchy.addGroup(
                        jsonGroup.get("name").getAsString(),
                        jsonGroup.get("level").getAsInt(),
                        abilities
                );
            }

            return hierarchy;
        }
    }
}