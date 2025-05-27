package com.jkantrell.regionslib.region.hierarchy;

import java.util.Optional;

public interface HierarchyRepository {

    Optional<Hierarchy> get(Long id);

    void save(Hierarchy hierarchy);

}
