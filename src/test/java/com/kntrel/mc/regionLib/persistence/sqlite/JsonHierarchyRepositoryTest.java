package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonHierarchyRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void deserializesHierarchiesFromJsonFile() throws IOException {
        Path jsonPath = tempDir.resolve("hierarchies.json");
        Files.writeString(jsonPath, """
                [
                  {
                    "id": 7,
                    "name": "domestic",
                    "groups": [
                      {
                        "name": "Admin",
                        "level": 1,
                        "abilities": ["MANAGE_USERS", " BREAK_BLOCKS "]
                      },
                      {
                        "name": "Member",
                        "level": 3,
                        "abilities": ["plant", "interact_with_armor_stands"]
                      }
                    ]
                  },
                  {
                    "id": 8,
                    "name": "publicSpaces",
                    "groups": [
                      {
                        "name": "Visitor",
                        "level": 5,
                        "abilities": ["use_beds"]
                      }
                    ]
                  }
                ]
                """);

        JsonHierarchyRepository repository = new JsonHierarchyRepository(jsonPath.toFile());

        List<Hierarchy> hierarchies = repository.getAll().stream()
                .sorted(Comparator.comparing(Hierarchy::getId))
                .toList();

        assertEquals(2, hierarchies.size());

        Hierarchy domestic = hierarchies.getFirst();
        assertEquals(7L, domestic.getId());
        assertEquals("domestic", domestic.getName());
        assertEquals(1, domestic.getLowestLever());
        assertEquals(3, domestic.getHighestLevel());

        Hierarchy.Group admin = domestic.getGroup(1).orElseThrow();
        Hierarchy.Group member = domestic.getGroup(3).orElseThrow();

        assertEquals("Admin", admin.getName());
        assertEquals("Member", member.getName());
        assertTrue(admin.allowedTo("manage_users"));
        assertTrue(admin.allowedTo("break_blocks"), "Deserializer should trim and normalize ability names");
        assertTrue(member.allowedTo("INTERACT_WITH_ARMOR_STANDS"));

        Hierarchy publicSpaces = repository.get(8L).orElseThrow();
        assertEquals("publicSpaces", publicSpaces.getName());
        assertTrue(publicSpaces.getGroup(5).orElseThrow().allowedTo("USE_BEDS"));

        assertEquals(1, repository.getByName("domestic").size());
        assertTrue(repository.getByName("missing").isEmpty());
    }

    @Test
    void handlesMissingJsonFileAsEmptyRepository() {
        JsonHierarchyRepository repository = new JsonHierarchyRepository(tempDir.resolve("does-not-exist.json").toFile());

        assertTrue(repository.getAll().isEmpty());
        assertTrue(repository.get(1L).isEmpty());
        assertTrue(repository.getByName("any").isEmpty());
    }

    @Test
    void wrapsInvalidJsonWithRuntimeException() throws IOException {
        Path jsonPath = tempDir.resolve("invalid.json");
        Files.writeString(jsonPath, "not json at all");

        assertThrows(RuntimeException.class, () -> new JsonHierarchyRepository(jsonPath.toFile()));
    }
}
