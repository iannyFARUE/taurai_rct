package com.hexicode.taurai_rtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TauraiRtcApplication {

	public static void main(String[] args) {
	      try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        SpringApplication.run(TauraiRtcApplication.class, args);
	}

}
