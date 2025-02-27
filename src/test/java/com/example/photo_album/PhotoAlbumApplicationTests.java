// src/test/java/com/example/photo_album/PhotoAlbumApplicationTests.java
package com.example.photo_album;

import com.example.photo_album.config.CITestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@Import(CITestConfig.class)
@ActiveProfiles("ci-test")
@TestPropertySource(properties = {
		"spring.main.allow-bean-definition-overriding=true"
})
class PhotoAlbumApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring context loads successfully
	}
}