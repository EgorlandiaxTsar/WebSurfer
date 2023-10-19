package com.egorgoncharov.websurfer.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site {
    private String url;
    private String name;
    private int requestTimeout;
}
