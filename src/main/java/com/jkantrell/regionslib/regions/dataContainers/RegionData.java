package com.jkantrell.regionslib.regions.dataContainers;

import com.google.gson.*;

import java.lang.reflect.Type;

public class RegionData {

    //FIELDS
    private String key_;
    private JsonPrimitive val_;

    //CONSTRUCTOR
    public RegionData(String key, JsonPrimitive val){
        key_ = key;
        val_ = val;
    }
    public RegionData(String key, Number val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, String val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, char val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, boolean val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }

    //GETTERS
    public String getKey(){
        return key_;
    }
    public int getAsInt(){
        return val_.getAsInt();
    }
    public byte getAsByte(){
        return val_.getAsByte();
    }
    public short getAsShort(){
        return val_.getAsShort();
    }
    public boolean getAsBoolean(){
        return val_.getAsBoolean();
    }
    public long getAsLong(){
        return val_.getAsLong();
    }
    public double getAsDouble(){
        return val_.getAsDouble();
    }
    public float getAsFloat(){
        return val_.getAsFloat();
    }
    public char getAsChar(){
        return val_.getAsCharacter();
    }
    public String getAsString(){
        return val_.getAsString();
    }
    public JsonPrimitive getValue() {
        return val_;
    }

    //SETTERS
    void setValue(JsonPrimitive val){
        val_ = val;
    }
    public void setValue(Number val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(String val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(char val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(boolean val){
        val_ = new JsonPrimitive(val);
    }

    public static class JSerializer implements JsonSerializer<RegionData> {

        @Override
        public JsonElement serialize(RegionData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonRegionData = new JsonObject();
            jsonRegionData.addProperty("key", src.getKey());
            jsonRegionData.add("value", src.getValue());

            return jsonRegionData;
        }
    }

    public static class JDeserializer implements JsonDeserializer<RegionData> {

        @Override
        public RegionData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonRegionData = json.getAsJsonObject();
            return new RegionData(
                    jsonRegionData.get("key").getAsString(),
                    jsonRegionData.get("value").getAsJsonPrimitive()
            );
        }
    }
}
