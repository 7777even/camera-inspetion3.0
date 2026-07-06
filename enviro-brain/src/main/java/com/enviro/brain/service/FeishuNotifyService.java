package com.enviro.brain.service;

import com.enviro.brain.entity.CameraResult;
import com.enviro.brain.entity.InspectionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeishuNotifyService {

    @Value("${enviro.feishu.webhook-url:}")
    private String webhookUrl;

    private RestTemplate restTemplate = new RestTemplate();

    public void sendInspectionReport(InspectionRecord record, List<CameraResult> results) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.info("[FeishuNotify] Webhook URL 未配置，跳过通知");
            return;
        }

        try {
            String cardJson = buildCardJson(record, results);
            log.info("[FeishuNotify] 发送JSON: {}", cardJson);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(cardJson, headers);
            restTemplate.postForObject(webhookUrl, entity, String.class);
            log.info("[FeishuNotify] 飞书通知发送成功");
        } catch (Exception e) {
            log.error("[FeishuNotify] 飞书通知发送失败: {}", e.getMessage());
        }
    }

    private String buildCardJson(InspectionRecord record, List<CameraResult> results) {
        String time = record.getCreatedAt() != null
                ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "";
        int total = record.getTotalCameras() != null ? record.getTotalCameras() : results.size();
        int online = record.getOnlineCount() != null ? record.getOnlineCount()
                : (int) results.stream().filter(r -> "online".equals(r.getStatus())).count();
        int offline = record.getOfflineCount() != null ? record.getOfflineCount()
                : (int) results.stream().filter(r -> "offline".equals(r.getStatus())).count();
        int abnormal = record.getAbnormalCount() != null ? record.getAbnormalCount()
                : (int) results.stream().filter(r -> "abnormal".equals(r.getStatus())).count();

        StringBuilder detail = new StringBuilder();
        for (CameraResult r : results) {
            if ("online".equals(r.getStatus())) continue;
            if (detail.length() > 0) detail.append("\\n");  // JSON 字面 "\n"，飞书渲染为换行
            String err = r.getErrorMessage() != null ? r.getErrorMessage() : "";
            // 截断+清洗：移除换行（飞书卡片 markdown 不会显示多行错误），避免 JSON 转义混乱
            err = err.replace("\n", " ").replace("\r", " ");
            if (err.length() > 80) err = err.substring(0, 80) + "...";
            detail.append("- ").append(r.getCameraName()).append("：").append(r.getStatus());
            if (!err.isEmpty()) detail.append("（").append(err).append("）");
        }
        String detailStr = detail.length() == 0 ? "无" : detail.toString();

        return "{"
                + "\"msg_type\":\"interactive\","
                + "\"card\":{"
                + "\"header\":{\"title\":{\"tag\":\"plain_text\",\"content\":\"🔔 危废仓库摄像头巡检报告\"},\"template\":\"red\"},"
                + "\"elements\":["
                + "{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**巡检时间**：" + time + "\"}},"
                + "{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**概况**：总 " + total
                + " 路\\n🟢 在线 " + online + "\\n🔴 离线 " + offline + "\\n🟡 异常 " + abnormal + "\"}},"
                + "{\"tag\":\"hr\"},"
                + "{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"**离线/异常详情**：\\n" + detailStr + "\"}},"
                + "{\"tag\":\"note\",\"elements\":[{\"tag\":\"plain_text\",\"content\":\"环保小脑自动巡检 " + time + "\"}]}"
                + "]}}";
    }
}
