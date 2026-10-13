package com.example.jsondataset.controller;

import com.example.jsondataset.dto.InsertRecordResponse;
import com.example.jsondataset.service.DatasetService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {
    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping(value = "/{datasetName}/record", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InsertRecordResponse> insertRecord(
            @PathVariable String datasetName,
            @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(datasetService.insertRecord(datasetName, body));
    }

    @GetMapping("/{datasetName}/query")
    public ResponseEntity<Map<String, Object>> query(
            @PathVariable String datasetName,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return ResponseEntity.ok(datasetService.query(datasetName, groupBy, sortBy, order));
    }
}
