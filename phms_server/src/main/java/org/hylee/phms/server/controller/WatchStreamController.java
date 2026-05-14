package org.hylee.phms.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.hylee.phms.server.service.WatchStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 兼容 hms 项目的手表数据接收入口。
 */
@RestController
@RequestMapping({"/stream", "/api/stream"})
public class WatchStreamController {

    private final WatchStreamService watchStreamService;

    public WatchStreamController(WatchStreamService watchStreamService) {
        this.watchStreamService = watchStreamService;
    }

    @GetMapping(params = "!heartRate")
    public Map<String, Object> streamInfo(HttpServletRequest request) {
        watchStreamService.markRequest(request.getRequestURI());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("message", "Watch stream server is running");
        return info;
    }

    @GetMapping(params = "heartRate")
    public ResponseEntity<?> ingestCompat(HttpServletRequest request,
                                          @RequestParam Map<String, String> queryParams) {
        return doIngest(request, queryParams);
    }

    @GetMapping("/ingest")
    public ResponseEntity<?> ingest(HttpServletRequest request,
                                    @RequestParam Map<String, String> queryParams) {
        return doIngest(request, queryParams);
    }

    @GetMapping("/latest")
    public Map<String, Object> latest(HttpServletRequest request) {
        watchStreamService.markRequest(request.getRequestURI());
        return watchStreamService.latestResponse();
    }

    @GetMapping("/debug/state")
    public Map<String, Object> debugState(HttpServletRequest request) {
        watchStreamService.markRequest(request.getRequestURI());
        return watchStreamService.debugResponse();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletRequest request) {
        watchStreamService.markRequest(request.getRequestURI());
        return watchStreamService.openEvents();
    }

    private ResponseEntity<?> doIngest(HttpServletRequest request, Map<String, String> queryParams) {
        watchStreamService.markRequest(request.getRequestURI());
        WatchStreamService.IngestResult result = watchStreamService.ingest(queryParams);
        if (!result.ok()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", result.error());
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("ok");
    }
}
