package itu.socobis.back;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SocobisBackApplication {

	private static final Logger log = LoggerFactory.getLogger(SocobisBackApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SocobisBackApplication.class, args);
	}

	@Bean
	public CommandLineRunner testDbConnection(DataSource dataSource) {
		return args -> {
			log.info("Starting DB connection test...");
			try (Connection conn = dataSource.getConnection();
				 Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
				if (rs.next()) {
					int v = rs.getInt(1);
					log.info("DB connection test successful, query returned: {}", v);
				} else {
					log.warn("DB connection test executed but returned no rows");
				}
			} catch (SQLException e) {
				log.error("DB connection test failed", e);
			}
		};
	}

}
