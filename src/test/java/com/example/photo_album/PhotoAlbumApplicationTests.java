// src/test/java/com/example/photo_album/PhotoAlbumApplicationTests.java
package com.example.photo_album;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"aws.region=eu-north-1",
		"aws.s3.bucket=test-bucket",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class PhotoAlbumApplicationTests {

	@Test
	void contextLoads() {
	}
}