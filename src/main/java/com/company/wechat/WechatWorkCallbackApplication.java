package com.company.wechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业微信回调服务主启动类
 * 
 * @author Company
 */
@SpringBootApplication
public class WechatWorkCallbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatWorkCallbackApplication.class, args);
        System.out.println("================================");
        System.out.println("企业微信回调服务启动成功！");
        System.out.println("================================");
    }
}
