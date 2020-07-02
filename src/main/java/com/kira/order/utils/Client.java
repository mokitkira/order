package com.kira.order.utils;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;

/**
 * @Author: Kira
 * @Description:
 * @Date:Create：in 2020/7/2 9:37 下午
 */
public class Client {
    public static void main(String[] args) {
        for(int i=0;i<100;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MultiValueMap<String, String> params = new LinkedMultiValueMap();
                        params.add("orderNum","10001");
                        params.add("userName","user:"+GenerateSequenceUtil.generateSequenceNo());
                        while(true) {
                            Thread.sleep(5000);
                            HttpUtil.post("http://localhost:8080/order/call", params);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
}
