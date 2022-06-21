package com.jkantrell.regionslib.regions.dataContainers;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

public class RegionData {

    //FIELDS
    private final String key_;
    private JsonElement val_;

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
    public RegionData(String key_, Object val) {
        this.key_ = key_;
        this.setValue(val);
    }

    //GETTERS
    public String getKey(){
        return key_;
    }
    public String getAsString(){
        return val_.getAsString();
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
    public String[] getAsStringArray() {
        return new Gson().fromJson(this.val_,String[].class);
    }
    public int[] getAsIntArray() {
        return new Gson().fromJson(this.val_,int[].class);
    }
    public byte[] getAsByteArray() {
        return new Gson().fromJson(this.val_,byte[].class);
    }
    public short[] getAsShortArray() {
        return new Gson().fromJson(this.val_,short[].class);
    }
    public boolean[] getAsBooleanArray() {
        return new Gson().fromJson(this.val_,boolean[].class);
    }
    public long[] getAsLongArray() {
        return new Gson().fromJson(this.val_,long[].class);
    }
    public double[] getAsDoubleArray() {
        return new Gson().fromJson(this.val_,double[].class);
    }
    public float[] getAsFloatArray() {
        return new Gson().fromJson(this.val_,float[].class);
    }
    public char[] getAsCharArray() {
        return new Gson().fromJson(this.val_,char[].class);
    }
    public JsonElement getValue() {
        return val_;
    }

    //SETTERS
    void setValue(JsonElement val){
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
    public void setValue(Object val) {
        this.val_ = new Gson().toJsonTree(val);
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
                    jsonRegionData.get("value")
            );
        }
    }
}
