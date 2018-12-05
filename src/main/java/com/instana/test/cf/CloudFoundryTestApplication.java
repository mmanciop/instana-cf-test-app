package com.instana.test.cf;

import com.instana.sdk.annotation.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@SpringBootApplication
@SuppressWarnings("unused")
public class CloudFoundryTestApplication {

	private static Logger APPLICATION_LOGGER = LoggerFactory.getLogger(CloudFoundryTestApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CloudFoundryTestApplication.class, args);
	}

	@RestController
	@RequestMapping("/api")
	static class ApiController {

		@Autowired
		private ApiImpl apiImpl;

		@GetMapping
		ResponseEntity<String> fetchInstanaWebPage() {
			return apiImpl.issueRequest();
		}

	}

	static class ApiImpl {

		private final RestTemplate restTemplate;

		ApiImpl(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Span(value="recurrent-task", type = Span.Type.ENTRY)
		ResponseEntity<String> issueRequest() {
			try {
				ResponseEntity<String> entity = restTemplate.getForEntity(URI.create("https://www.google.com"), String.class);

				HttpStatus responseStatus = entity.getStatusCode();

				APPLICATION_LOGGER.info("Request to Google succeeded with status code {}", responseStatus);

				return entity;
			} catch (Exception ex) {
				APPLICATION_LOGGER.error("Request to Google failed", ex);

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.toString());
			}
		}

	}

	@Configuration
	static class RestTemplateConfiguration {

		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}

	@Configuration
	public static class ApiImplConfiguration {

		@Bean
		ApiImpl apiImpl(RestTemplate restTemplate) {
			return new ApiImpl(restTemplate);
		}

	}

	@Configuration
	@EnableScheduling
	static class SchedulingConfiguration {

		@Autowired
		private ApiImpl apiImpl;

		@Scheduled(fixedRate=10_000)
		@Span(value="recurrent-task", type = Span.Type.ENTRY)
		void issueRequest() {
			apiImpl.issueRequest();
		}

	}

}