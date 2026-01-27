package com.kntrel.mc.regionLib.region.hierarchy;

import java.util.List;
import java.util.Optional;

public interface HierarchyRepository {

    List<Hierarchy> getAll();
    Optional<Hierarchy> get(long id);
    List<Hierarchy> getByName(String name);



    void save(Hierarchy hierarchy);

}
