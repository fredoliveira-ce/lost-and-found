package nl.fred.lostandfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// PMD UseUtilityClass: this is a standard Spring Boot application entry
// point (the @SpringBootApplication convention), not a utility class - a
// private constructor here would be unconventional, not an improvement.
@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class LostAndFoundApplication {

	public static void main(final String[] args) {
		SpringApplication.run(LostAndFoundApplication.class, args);
	}

}
