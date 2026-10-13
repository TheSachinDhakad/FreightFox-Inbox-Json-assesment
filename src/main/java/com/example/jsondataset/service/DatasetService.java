package com.example.jsondataset.service;

import com.example.jsondataset.dto.InsertRecordResponse;
import com.example.jsondataset.entity.DatasetRecord;
import com.example.jsondataset.exception.BadRequestException;
import com.example.jsondataset.exception.ResourceNotFoundException;
import com.example.jsondataset.repository.DatasetRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DatasetService {
    private final DatasetRecordRepository repository;
    private final ObjectMapper objectMapper;

    public DatasetService(DatasetRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InsertRecordResponse insertRecord(String datasetName, JsonNode body) {
        validateDatasetName(datasetName);

        if (body == null || !body.isObject()) {
            throw new BadRequestException("Request body must be a JSON object.");
        }

        DatasetRecord saved = repository.save(
                new DatasetRecord(datasetName, body.toString())
        );
        return new InsertRecordResponse(
                "Record added successfully", datasetName, saved.getId()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> query(String datasetName, String groupBy,
                                     String sortBy, String order) {
        validateDatasetName(datasetName);

        if (groupBy != null && !groupBy.isBlank()
                && sortBy != null && !sortBy.isBlank()) {
            throw new BadRequestException("Use either groupBy or sortBy in one request, not both.");
        }

        List<DatasetRecord> storedRecords =
                repository.findByDatasetNameOrderByIdAsc(datasetName);

        if (storedRecords.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No records found for dataset '" + datasetName + "'."
            );
        }

        List<JsonNode> records = new ArrayList<>();
        for (DatasetRecord stored : storedRecords) {
            try {
                records.add(objectMapper.readTree(stored.getRecordData()));
            } catch (JsonProcessingException ex) {
                // This should not occur because inserts are parsed as JSON before persistence.
                throw new IllegalStateException("A stored record contains invalid JSON.", ex);
            }
        }

        if (groupBy != null && !groupBy.isBlank()) {
            return groupRecords(records, groupBy.trim());
        }

        if (sortBy != null && !sortBy.isBlank()) {
            return sortRecords(records, sortBy.trim(), order);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dataset", datasetName);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    private Map<String, Object> groupRecords(List<JsonNode> records, String field) {
        Map<String, List<JsonNode>> grouped = new LinkedHashMap<>();

        for (JsonNode record : records) {
            JsonNode value = record.get(field);
            String key = (value == null || value.isNull()) ? "null" : value.asText();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("groupedRecords", grouped);
        return response;
    }

    private Map<String, Object> sortRecords(List<JsonNode> records, String field, String order) {
        String normalizedOrder = (order == null || order.isBlank()) ? "asc" : order.toLowerCase(Locale.ROOT);
        if (!normalizedOrder.equals("asc") && !normalizedOrder.equals("desc")) {
            throw new BadRequestException("order must be either 'asc' or 'desc'.");
        }

        Comparator<JsonNode> comparator = (left, right) ->
                compareJsonValues(left.get(field), right.get(field));

        if (normalizedOrder.equals("desc")) {
            comparator = comparator.reversed();
        }

        records.sort(comparator);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sortedRecords", records);
        return response;
    }

    private int compareJsonValues(JsonNode left, JsonNode right) {
        boolean leftMissing = left == null || left.isNull();
        boolean rightMissing = right == null || right.isNull();

        if (leftMissing && rightMissing) return 0;
        if (leftMissing) return 1;
        if (rightMissing) return -1;

        if (left.isNumber() && right.isNumber()) {
            BigDecimal a = left.decimalValue();
            BigDecimal b = right.decimalValue();
            return a.compareTo(b);
        }

        if (left.isBoolean() && right.isBoolean()) {
            return Boolean.compare(left.booleanValue(), right.booleanValue());
        }

        return left.asText().compareToIgnoreCase(right.asText());
    }

    private void validateDatasetName(String datasetName) {
        if (datasetName == null || !datasetName.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new BadRequestException(
                    "datasetName must be 1-100 characters and contain only letters, digits, '_' or '-'."
            );
        }
    }
}
