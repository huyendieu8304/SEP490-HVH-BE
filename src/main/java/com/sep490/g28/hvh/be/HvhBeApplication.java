package com.sep490.g28.hvh.be;

import com.sep490.g28.hvh.be.integration.email.RabbitMqEmailProperties;
import com.sep490.g28.hvh.be.notification.config.RabbitMqNotificationProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({RabbitMqEmailProperties.class, RabbitMqNotificationProperties.class})
@EnableSpringDataWebSupport(
		pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
@EnableScheduling
public class HvhBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(HvhBeApplication.class, args);
	}


	@PostConstruct
	void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}
}
