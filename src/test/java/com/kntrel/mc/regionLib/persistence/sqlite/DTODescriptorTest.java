package com.kntrel.mc.regionLib.persistence.sqlite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DTODescriptorTest {

    //MOCKS
    @DTO.Table("test_table")
    public record TestDTO (
            @DTO.Id @DTO.Column("id") Integer id,
            @DTO.Column("name") String name
    ) {}

    @DTO.Table("person")
    public record MockPerson (
            @DTO.Id @DTO.Column("id") Integer id,
            @DTO.Column("name") String name,
            @DTO.Column("age") Integer age,
            @DTO.Column("height") Double height,
            @DTO.Column("country") String country,
            @DTO.Column("gender") String gender
    ) {}

    @DTO.Table("person_favorite_movie")
    public record PersonFavoriteMovie (
            @DTO.Id @DTO.Column("person_id") Integer personId,
            @DTO.Id @DTO.Column("movie_name") String movieName,
            @DTO.Column("genre") String genre,
            @DTO.Column("length") Integer length,
            @DTO.Column("year") Integer year
    ) {}

    @Test
    void testSimpleDTO() {
        DTODescriptor desc = new DTODescriptor(TestDTO.class);

        List<DTODescriptor.Column>  cols = desc.getColumns(),
                                    idCols = desc.getIdColumns();

        assertEquals(2, cols.size());
        assertEquals(1, idCols.size());

        assertEquals("name", cols.get(1).name());
        assertEquals("id", idCols.getFirst().name());

        assertEquals(
                "INSERT INTO test_table(id, name) VALUES (?, ?);",
                desc.getInsertSQL()
        );
        assertEquals(
                "DELETE FROM test_table WHERE id = ?;",
                desc.getDeleteSQL()
        );
        assertEquals(
                "UPDATE test_table SET name = ? WHERE id = ?;",
                desc.getUpdateSQL()
        );
    }

    @Test
    void testMockPersonDTO() {
        DTODescriptor desc = new DTODescriptor(MockPerson.class);

        List<DTODescriptor.Column>  cols = desc.getColumns(),
                                    idCols = desc.getIdColumns();

        assertEquals(6, cols.size());
        assertEquals(1, idCols.size());

        assertEquals("id", cols.getFirst().name());
        assertEquals("name", cols.get(1).name());
        assertEquals("age", cols.get(2).name());
        assertEquals("height", cols.get(3).name());
        assertEquals("country", cols.get(4).name());
        assertEquals("gender", cols.get(5).name());
        assertEquals("id", idCols.getFirst().name());

        assertEquals(
                "INSERT INTO person(id, name, age, height, country, gender) VALUES (?, ?, ?, ?, ?, ?);",
                desc.getInsertSQL()
        );
        assertEquals(
                "DELETE FROM person WHERE id = ?;",
                desc.getDeleteSQL()
        );
        assertEquals(
                "UPDATE person SET name = ?, age = ?, height = ?, country = ?, gender = ? WHERE id = ?;",
                desc.getUpdateSQL()
        );
    }

    @Test
    void testPersonFavoriteMovieDTO() {
        DTODescriptor desc = new DTODescriptor(PersonFavoriteMovie.class);

        List<DTODescriptor.Column>  cols = desc.getColumns(),
                                    idCols = desc.getIdColumns();

        assertEquals(5, cols.size());
        assertEquals(2, idCols.size());

        assertEquals("person_id", cols.getFirst().name());
        assertEquals("movie_name", cols.get(1).name());
        assertEquals("genre", cols.get(2).name());
        assertEquals("length", cols.get(3).name());
        assertEquals("year", cols.get(4).name());

        assertEquals("person_id", idCols.getFirst().name());
        assertEquals("movie_name", idCols.get(1).name());

        assertEquals(
                "INSERT INTO person_favorite_movie(person_id, movie_name, genre, length, year) VALUES (?, ?, ?, ?, ?);",
                desc.getInsertSQL()
        );
        assertEquals(
                "DELETE FROM person_favorite_movie WHERE person_id = ? AND movie_name = ?;",
                desc.getDeleteSQL()
        );
        assertEquals(
                "UPDATE person_favorite_movie SET genre = ?, length = ?, year = ? WHERE person_id = ? AND movie_name = ?;",
                desc.getUpdateSQL()
        );
    }

    @Test
    void testProjectionHelpers() {
        DTODescriptor desc = new DTODescriptor(MockPerson.class);

        assertTrue(desc.getColumn("name").isPresent());
        assertEquals("name", desc.getColumn("name").orElseThrow().name());
        assertTrue(desc.getColumn("unknown").isEmpty());

        DTODescriptor.Projection full = desc.fullProjection();
        assertEquals(desc.getColumns(), full.columns());

        DTODescriptor.Projection projected = desc.project("country", "id", "name");
        assertEquals(3, projected.columns().size());
        assertEquals("country", projected.columns().get(0).name());
        assertEquals("id", projected.columns().get(1).name());
        assertEquals("name", projected.columns().get(2).name());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> desc.project("missing_column"));
        assertTrue(ex.getMessage().contains("missing_column"));
    }
}
