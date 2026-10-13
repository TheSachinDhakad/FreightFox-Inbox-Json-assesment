package com.example.jsondataset.service;

import com.example.jsondataset.entity.DatasetRecord;
import com.example.jsondataset.exception.BadRequestException;
import com.example.jsondataset.exception.ResourceNotFoundException;
import com.example.jsondataset.repository.DatasetRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatasetServiceComprehensiveTest {
    private DatasetRecordRepository repository;
    private ObjectMapper mapper;
    private DatasetService service;

    @BeforeEach
    void setUp() {
        repository = mock(DatasetRecordRepository.class);
        mapper = new ObjectMapper();
        service = new DatasetService(repository, mapper);
    }

    @Test
    void insertRecord_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.insertRecord("employees", null));
    }

    @Test
    void insertRecord_nonObjectBody_throwsBadRequest() throws Exception {
        JsonNode arrayNode = mapper.readTree("[1,2,3]");
        assertFalse(arrayNode.isObject());
        assertThrows(BadRequestException.class, () -> service.insertRecord("employees", arrayNode));
    }

    @Test
    void query_noRecords_throwsResourceNotFound() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.query("employees", null, null, "asc"));
    }

    @Test
    void groupBy_handlesNullValues() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"John\", \"department\": null}"),
                new DatasetRecord("employees", "{\"name\":\"Jane\", \"department\": \"Sales\"}")
        ));

        Map<String, Object> res = service.query("employees", "department", null, "asc");
        Map<?, ?> groups = (Map<?, ?>) res.get("groupedRecords");
        assertTrue(groups.containsKey("null"));
        assertEquals(1, ((List<?>) groups.get("null")).size());
        assertEquals(1, ((List<?>) groups.get("Sales")).size());
    }

    @Test
    void sortBy_descendingOrder_sortsCorrectly() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"Alice\", \"age\":20}"),
                new DatasetRecord("employees", "{\"name\":\"Bob\", \"age\":40}")
        ));

        Map<String, Object> res = service.query("employees", null, "age", "desc");
        List<?> sorted = (List<?>) res.get("sortedRecords");
        assertEquals(40, ((JsonNode) sorted.get(0)).get("age").asInt());
        assertEquals(20, ((JsonNode) sorted.get(1)).get("age").asInt());
    }

    @Test
    void sort_missingField_putsMissingLast_forAsc() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"NoAge\"}"),
                new DatasetRecord("employees", "{\"name\":\"WithAge\", \"age\":10}")
        ));

        Map<String, Object> res = service.query("employees", null, "age", "asc");
        List<?> sorted = (List<?>) res.get("sortedRecords");
        // first should have age 10, second should have missing/null age
        assertTrue(((JsonNode) sorted.get(0)).has("age") && !((JsonNode) sorted.get(0)).get("age").isNull());
        assertTrue(!((JsonNode) sorted.get(1)).has("age") || ((JsonNode) sorted.get(1)).get("age").isNull());
    }

    @Test
    void sort_booleanValues_ordersFalseBeforeTrue() {
        when(repository.findByDatasetNameOrderByIdAsc("flags")).thenReturn(List.of(
                new DatasetRecord("flags", "{\"id\":1, \"active\":true}"),
                new DatasetRecord("flags", "{\"id\":2, \"active\":false}")
        ));

        Map<String, Object> res = service.query("flags", null, "active", "asc");
        List<?> sorted = (List<?>) res.get("sortedRecords");
        assertFalse(((JsonNode) sorted.get(0)).get("active").asBoolean());
        assertTrue(((JsonNode) sorted.get(1)).get("active").asBoolean());
    }

    @Test
    void rejectsInvalidOrderValue() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"age\":30}")
        ));
        assertThrows(BadRequestException.class, () -> service.query("employees", null, "age", "upward"));
    }

    @Test
    void rejectsInvalidDatasetName_tooLong() throws Exception {
        String longName = "a".repeat(150);
        JsonNode body = mapper.readTree("{\"x\":1}");
        assertThrows(BadRequestException.class, () -> service.insertRecord(longName, body));
    }

    @Test
    void sort_defaultOrder_whenOrderNull_usesAsc() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"A\", \"age\":50}"),
                new DatasetRecord("employees", "{\"name\":\"B\", \"age\":10}")
        ));

        Map<String, Object> res = service.query("employees", null, "age", null);
        List<?> sorted = (List<?>) res.get("sortedRecords");
        assertEquals(10, ((JsonNode) sorted.get(0)).get("age").asInt());
    }
}
