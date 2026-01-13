package com.kntrel.mc.regionLib.test.mock;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;

import java.util.*;

public class MockHierarchyRepository implements HierarchyRepository {

    //FACTORY
    public static MockHierarchyRepository ofSingle(Hierarchy hierarchy) {
        return new MockHierarchyRepository(List.of(hierarchy));
    }
    public static MockHierarchyRepository ofSingle(String name, int groups) {
        Hierarchy hierarchy = new Hierarchy(0L, name);
        for (int i = 1; i <= groups; i++) {
            hierarchy.addGroup("group" + i, i, Collections.emptyList());
        }
        return ofSingle(hierarchy);
    }
    public static MockHierarchyRepository ofSingle(String name) {
        return ofSingle(name, 1);
    }
    public static MockHierarchyRepository with(String... names) {
        List<Hierarchy> hierarchies = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Hierarchy h = new Hierarchy((long) i, names[i]);
            h.addGroup("default", 1, Collections.emptyList());
            hierarchies.add(h);
        }
        return new MockHierarchyRepository(hierarchies);
    }


    //FIELDS
    private final List<Hierarchy> hierarchies_;


    //CONSTRUCTORS
    public MockHierarchyRepository(Collection<Hierarchy> hierarchies) {
        this.hierarchies_ = new ArrayList<>(hierarchies);
    }


    //IMPLEMENTATION
    @Override public List<Hierarchy> getAll() {
        return this.hierarchies_;
    }
    @Override public Optional<Hierarchy> get(Long id) {
        return this.hierarchies_.stream()
                .filter(hierarchy -> hierarchy.getId().equals(id))
                .findFirst();
    }
    @Override public List<Hierarchy> getByName(String name) {
        return this.hierarchies_.stream()
                .filter(hierarchy -> hierarchy.getName().equalsIgnoreCase(name))
                .toList();
    }
    @Override public void save(Hierarchy hierarchy) {
        if (!this.hierarchies_.contains(hierarchy)) {
            this.hierarchies_.add(hierarchy);
        }
    }
}
