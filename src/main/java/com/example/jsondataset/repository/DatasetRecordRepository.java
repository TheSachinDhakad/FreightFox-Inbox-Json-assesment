package com.example.jsondataset.repository;

import com.example.jsondataset.entity.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, Long> {
    List<DatasetRecord> findByDatasetNameOrderByIdAsc(String datasetName);
}
