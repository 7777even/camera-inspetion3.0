package com.enviro.brain.mapper;

import com.enviro.brain.entity.InspectionRecord;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
class InspectionRecordMapperTest {

    @Autowired
    private InspectionRecordMapper mapper;

    @Test
    void insert_shouldGenerateId() {
        InspectionRecord record = new InspectionRecord();
        record.setBatchId("BATCH-001");
        record.setInspectionDate(LocalDate.now());
        record.setTotalCameras(10);
        record.setOnlineCount(8);
        record.setOfflineCount(2);
        record.setAbnormalCount(0);
        record.setStatus("COMPLETED");

        mapper.insert(record);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getId()).isGreaterThan(0);
    }

    @Test
    void findById_shouldReturnRecord() {
        InspectionRecord record = new InspectionRecord();
        record.setBatchId("BATCH-002");
        record.setInspectionDate(LocalDate.now());
        record.setTotalCameras(5);
        record.setOnlineCount(4);
        record.setOfflineCount(1);
        record.setAbnormalCount(0);
        record.setStatus("COMPLETED");
        mapper.insert(record);

        InspectionRecord found = mapper.findById(record.getId());

        assertThat(found).isNotNull();
        assertThat(found.getBatchId()).isEqualTo("BATCH-002");
        assertThat(found.getTotalCameras()).isEqualTo(5);
    }

    @Test
    void findByBatchId_shouldReturnRecords() {
        InspectionRecord record = new InspectionRecord();
        record.setBatchId("BATCH-003");
        record.setInspectionDate(LocalDate.now());
        record.setTotalCameras(3);
        record.setOnlineCount(2);
        record.setOfflineCount(1);
        record.setAbnormalCount(0);
        record.setStatus("COMPLETED");
        mapper.insert(record);

        List<InspectionRecord> results = mapper.findByBatchId("BATCH-003");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getBatchId()).isEqualTo("BATCH-003");
    }

    @Test
    void findBySyncVersionGreaterThan_shouldReturnNewerRecords() {
        // Insert records with different syncVersion values
        InspectionRecord recordV1 = new InspectionRecord();
        recordV1.setBatchId("BATCH-SYNC-V1");
        recordV1.setInspectionDate(LocalDate.now());
        recordV1.setTotalCameras(1);
        recordV1.setOnlineCount(1);
        recordV1.setOfflineCount(0);
        recordV1.setAbnormalCount(0);
        recordV1.setStatus("COMPLETED");
        recordV1.setSyncVersion(1L);
        mapper.insert(recordV1);

        InspectionRecord recordV2 = new InspectionRecord();
        recordV2.setBatchId("BATCH-SYNC-V2");
        recordV2.setInspectionDate(LocalDate.now());
        recordV2.setTotalCameras(2);
        recordV2.setOnlineCount(2);
        recordV2.setOfflineCount(0);
        recordV2.setAbnormalCount(0);
        recordV2.setStatus("COMPLETED");
        recordV2.setSyncVersion(2L);
        mapper.insert(recordV2);

        // Query records with syncVersion > 1: should only return recordV2
        List<InspectionRecord> results = mapper.findBySyncVersionGreaterThan(1L, 10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(r -> r.getSyncVersion() > 1L);
        assertThat(results).extracting(InspectionRecord::getBatchId)
                .contains("BATCH-SYNC-V2")
                .doesNotContain("BATCH-SYNC-V1");
    }

    @Test
    void findAll_shouldReturnAllRecords() {
        InspectionRecord record1 = new InspectionRecord();
        record1.setBatchId("BATCH-ALL-1");
        record1.setInspectionDate(LocalDate.now());
        record1.setTotalCameras(1);
        record1.setOnlineCount(1);
        record1.setOfflineCount(0);
        record1.setAbnormalCount(0);
        record1.setStatus("COMPLETED");
        mapper.insert(record1);

        InspectionRecord record2 = new InspectionRecord();
        record2.setBatchId("BATCH-ALL-2");
        record2.setInspectionDate(LocalDate.now());
        record2.setTotalCameras(1);
        record2.setOnlineCount(1);
        record2.setOfflineCount(0);
        record2.setAbnormalCount(0);
        record2.setStatus("COMPLETED");
        mapper.insert(record2);

        List<InspectionRecord> results = mapper.findAll();

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }
}
