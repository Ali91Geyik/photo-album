// src/test/java/com/example/photo_album/PhotoAlbumApplicationTests.java
package com.example.photo_album;

import com.example.photo_album.config.CITestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(CITestConfig.class)
@ActiveProfiles({"test", "ci-test"})
class PhotoAlbumApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring context loads successfully
	}
}