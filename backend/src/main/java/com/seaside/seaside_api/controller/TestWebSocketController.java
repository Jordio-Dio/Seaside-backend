package com.seaside.seaside_api.controller;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/telemetrie/{id}")
    public String publier(@PathVariable String id) {

        Map<String, Object> data = Map.of(
                "batteryPercent", 87,
                "entrees", 15,
                "licence", "ACTIVE"
        );

        messagingTemplate.convertAndSend(
                "/topic/telemetrie/" + id,
                data
        );

        return "Message envoyé sur /topic/telemetrie/" + id;
    }
}