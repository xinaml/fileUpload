package com.xinaml.fileupload.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author lgq
 */
@Component
public class PathCommon {
    public static final String SEPARATOR = "/";
    @Value("${storage_path}")
    private String storage_path;
    public static String ROOT_PATH = "";

    @PostConstruct
    public void init() {
        ROOT_PATH = storage_path;
        System.out.println("root path="+ROOT_PATH);
    }

}
