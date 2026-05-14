package org.hylee.phms.server.service;

import org.hylee.phms.server.vo.WatchStreamData;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 接收手表推送、维护最新状态，并将可映射字段写入当前系统。
 */
public interface WatchStreamService {

    void markRequest(String path);

    Map<String, Object> latestResponse();

    Map<String, Object> debugResponse();

    IngestResult ingest(Map<String, String> queryParams);

    SseEmitter openEvents();

    record IngestResult(boolean ok, String error, WatchStreamData data) {
        public static IngestResult success(WatchStreamData data) {
            return new IngestResult(true, null, data);
        }

        public static IngestResult error(String error) {
            return new IngestResult(false, error, null);
        }
    }
}
