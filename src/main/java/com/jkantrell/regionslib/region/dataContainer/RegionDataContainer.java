package com.jkantrell.regionslib.region.dataContainer;

import java.util.ArrayList;
import java.util.List;

public class RegionDataContainer {

    //FIELDS
    private final List<RegionData> data_= new ArrayList<>();

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
    public List<RegionData> getAll() {
        return List.copyOf(this.data_);
    }
}
