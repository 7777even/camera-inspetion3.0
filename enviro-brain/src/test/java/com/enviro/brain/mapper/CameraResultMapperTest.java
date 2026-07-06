package com.enviro.brain.mapper;

import com.enviro.brain.entity.CameraResult;
import com.enviro.brain.entity.InspectionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("test")
class CameraResultMapperTest {

    @Autowired
    private CameraResultMapper mapper;

    @Autowired
    private InspectionRecordMapper inspectionRecordMapper;

    /** 插入一个巡检记录，满足外键约束 */
    private Long insertRecord() {
        InspectionRecord record = new InspectionRecord();
        record.setBatchId("BATCH-TEST-" + System.nanoTime());
        record.setInspectionDate(java.time.LocalDate.now());
        record.setTotalCameras(10);
        record.setOnlineCount(8);
        record.setOfflineCount(1);
        record.setAbnormalCount(1);
        record.setStatus("RUNNING");
        inspectionRecordMapper.insert(record);
        return record.getId();
    }

    @Test
    void insert_shouldGenerateId() {
        Long recordId = insertRecord();

        CameraResult result = new CameraResult();
        result.setRecordId(recordId);
        result.setCameraCode("CAM-001");
        result.setCameraName("摄像头001");
        result.setStatus("ONLINE");
        result.setQualityScore(new BigDecimal("85.50"));
        result.setScreenshotPath("/screenshots/CAM-001-001.jpg");
        result.setErrorMessage(null);

        mapper.insert(result);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getId()).isGreaterThan(0);
    }

    @Test
    void findById_shouldReturnResult() {
        Long recordId = insertRecord();

        CameraResult result = new CameraResult();
        result.setRecordId(recordId);
        result.setCameraCode("CAM-002");
        result.setCameraName("摄像头002");
        result.setStatus("OFFLINE");
        result.setQualityScore(new BigDecimal("0.00"));
        result.setErrorMessage("Connection timeout");
        mapper.insert(result);

        CameraResult found = mapper.findById(result.getId());

        assertThat(found).isNotNull();
        assertThat(found.getCameraCode()).isEqualTo("CAM-002");
        assertThat(found.getStatus()).isEqualTo("OFFLINE");
    }

    @Test
    void findByRecordId_shouldReturnResults() {
        Long recordId = insertRecord();

        CameraResult result1 = new CameraResult();
        result1.setRecordId(recordId);
        result1.setCameraCode("CAM-003");
        result1.setCameraName("摄像头003");
        result1.setStatus("ONLINE");
        mapper.insert(result1);

        CameraResult result2 = new CameraResult();
        result2.setRecordId(recordId);
        result2.setCameraCode("CAM-004");
        result2.setCameraName("摄像头004");
        result2.setStatus("ABNORMAL");
        mapper.insert(result2);

        List<CameraResult> results = mapper.findByRecordId(recordId);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results).allMatch(r -> r.getRecordId().equals(recordId));
    }

    @Test
    void findBySyncVersionGreaterThan_shouldReturnNewerResults() {
        Long recordId = insertRecord();

        // Insert results with different syncVersion values
        CameraResult resultV2 = new CameraResult();
        resultV2.setRecordId(recordId);
        resultV2.setCameraCode("CAM-SYNC-V2");
        resultV2.setCameraName("同步测试2");
        resultV2.setStatus("ONLINE");
        resultV2.setSyncVersion(2L);
        mapper.insert(resultV2);

        CameraResult resultV3 = new CameraResult();
        resultV3.setRecordId(recordId);
        resultV3.setCameraCode("CAM-SYNC-V3");
        resultV3.setCameraName("同步测试3");
        resultV3.setStatus("ONLINE");
        resultV3.setSyncVersion(3L);
        mapper.insert(resultV3);

        // Query with syncVersion > 2: should only get V3
        List<CameraResult> results = mapper.findBySyncVersionGreaterThan(2L, 100);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(r -> r.getSyncVersion() > 2L);
        assertThat(results).extracting(CameraResult::getCameraCode)
                .contains("CAM-SYNC-V3")
                .doesNotContain("CAM-SYNC-V2");
    }

    @Test
    void findAll_shouldReturnAllResults() {
        Long recordId = insertRecord();

        CameraResult result = new CameraResult();
        result.setRecordId(recordId);
        result.setCameraCode("CAM-ALL");
        result.setCameraName("摄像头ALL");
        result.setStatus("ONLINE");
        mapper.insert(result);

        List<CameraResult> results = mapper.findAll();

        assertThat(results).isNotEmpty();
    }
}
