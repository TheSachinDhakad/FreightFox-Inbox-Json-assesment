package com.example.jsondataset.service;

import com.example.jsondataset.dto.InsertRecordResponse;
import com.example.jsondataset.entity.DatasetRecord;
import com.example.jsondataset.exception.BadRequestException;
import com.example.jsondataset.repository.DatasetRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatasetServiceTest {
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
    void insertRecordSavesJson() throws Exception {
        when(repository.save(any(DatasetRecord.class))).thenAnswer(invocation -> {
            DatasetRecord record = invocation.getArgument(0);
            // Simulate generated database ID through a persisted test fixture.
            return record;
        });
        JsonNode body = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        InsertRecordResponse response = service.insertRecord("employees", body);
        assertEquals("Record added successfully", response.message());
        assertEquals("employees", response.dataset());
        verify(repository).save(any(DatasetRecord.class));
    }

    @Test
    void groupByDepartmentReturnsGroups() throws Exception {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"John\",\"department\":\"Engineering\"}"),
                new DatasetRecord("employees", "{\"name\":\"Jane\",\"department\":\"Engineering\"}"),
                new DatasetRecord("employees", "{\"name\":\"Alice\",\"department\":\"Marketing\"}")
        ));
        Map<String, Object> result = service.query("employees", "department", null, "asc");
        Map<?, ?> groups = (Map<?, ?>) result.get("groupedRecords");
        assertEquals(2, groups.size());
        assertEquals(2, ((List<?>) groups.get("Engineering")).size());
    }

    @Test
    void sortByAgeAscending() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"name\":\"John\",\"age\":30}"),
                new DatasetRecord("employees", "{\"name\":\"Jane\",\"age\":25}")
        ));
        Map<String, Object> result = service.query("employees", null, "age", "asc");
        List<?> sorted = (List<?>) result.get("sortedRecords");
        assertEquals(25, ((JsonNode) sorted.get(0)).get("age").asInt());
        assertEquals(30, ((JsonNode) sorted.get(1)).get("age").asInt());
    }

    @Test
    void rejectsInvalidOrder() {
        when(repository.findByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
                new DatasetRecord("employees", "{\"age\":30}")
        ));
        assertThrows(BadRequestException.class,
                () -> service.query("employees", null, "age", "sideways"));
    }

    @Test
    void rejectsInvalidDatasetName() throws Exception {
        JsonNode body = mapper.readTree("{\"name\":\"John\"}");
        assertThrows(BadRequestException.class, () -> service.insertRecord("employees/drop", body));
    }
}
