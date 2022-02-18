package com.jkantrell.regionslib.regions.dataContainers;

import com.google.gson.*;
import com.jkantrell.regionslib.io.Serializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RegionDataContainer {

    //FIELDS
    private List<RegionData> data_= new ArrayList<>();

    //PUBLIC METHODS
    public RegionData get(String key){

        RegionData r = null;
        for (RegionData i : data_) {
            if(i.getKey().equals(key)){
                r = i;
                break;
            }
        }
        return r;
    }
    public RegionData get(int index){
        return data_.get(index);
    }
    public void add(RegionData data){
        data_.add(data);
    }
    public void clear(){
        data_.removeAll(data_);
    }
    public int size() { return data_.size(); }
    public void remove(String key){
        data_.remove(this.get(key));
    }
    public boolean has(String key) {
        for (RegionData i : data_) {
            if(i.getKey().equals(key)){
                return true;
            }
        }
        return false;
    }
    public boolean isEmpty() {
        return data_.isEmpty();
    }

    public static class JSerializer implements JsonSerializer<RegionDataContainer> {

        @Override
        public JsonElement serialize(RegionDataContainer src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray jsonDataContainer = new JsonArray();
            for (RegionData data : src.data_) {
                jsonDataContainer.add(Serializer.GSON.toJsonTree(data));
            }

            return jsonDataContainer;
        }
    }

    public static class JDeserializer implements JsonDeserializer<RegionDataContainer> {

        @Override
        public RegionDataContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            RegionDataContainer regionDataContainer = new RegionDataContainer();
            JsonArray jsonDataContainer = json.getAsJsonArray();
            for (JsonElement jsonElement : jsonDataContainer) {
                regionDataContainer.add(Serializer.GSON.fromJson(jsonElement,RegionData.class));
            }
            return regionDataContainer;
        }
    }
}
