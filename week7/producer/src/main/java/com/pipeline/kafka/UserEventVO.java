package com.pipeline.kafka;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserEventVO {
    private String timestamp;
    private String userAgent;
    private String colorName;
    private String userName;
}
