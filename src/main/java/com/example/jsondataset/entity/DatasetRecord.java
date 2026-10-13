package com.example.jsondataset.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dataset_records", indexes = {
        @Index(name = "idx_dataset_name", columnList = "dataset_name")
})
public class DatasetRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_name", nullable = false, length = 100)
    private String datasetName;

    @Column(name = "record_data", nullable = false, columnDefinition = "JSON")
    private String recordData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DatasetRecord() {
    }

    public DatasetRecord(String datasetName, String recordData) {
        this.datasetName = datasetName;
        this.recordData = recordData;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getRecordData() {
        return recordData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
