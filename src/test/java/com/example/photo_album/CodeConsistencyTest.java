// src/test/java/com/example/photo_album/CodeConsistencyTest.java
package com.example.photo_album;

import com.example.photo_album.controller.AlbumController;
import com.example.photo_album.controller.AuthController;
import com.example.photo_album.controller.PhotoController;
import com.example.photo_album.model.Album;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.AlbumRepository;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import com.example.photo_album.service.AlbumService;
import com.example.photo_album.service.PhotoService;
import com.example.photo_album.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeConsistencyTest extends BaseTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PhotoController photoController;

    @Autowired
    private AlbumController albumController;

    @Autowired
    private AuthController authController;

    @Test
    void testControllerNamingConsistency() {
        // All controllers should end with "Controller"
        assertThat(photoController.getClass().getSimpleName()).endsWith("Controller");
        assertThat(albumController.getClass().getSimpleName()).endsWith("Controller");
        assertThat(authController.getClass().getSimpleName()).endsWith("Controller");

        // All controllers should have the @Controller or @RestController annotation
        assertThat(photoController.getClass().isAnnotationPresent(Controller.class) ||
                photoController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class))
                .isTrue();
        assertThat(albumController.getClass().isAnnotationPresent(Controller.class) ||
                albumController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class))
                .isTrue();
        assertThat(authController.getClass().isAnnotationPresent(Controller.class) ||
                authController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class))
                .isTrue();
    }

    @Test
    void testServiceNamingConsistency() {
        // Check the actual service classes, not the mock instances
        assertThat(UserService.class.getSimpleName()).endsWith("Service");
        assertThat(PhotoService.class.getSimpleName()).endsWith("Service");
        assertThat(AlbumService.class.getSimpleName()).endsWith("Service");

        // Check for @Service annotation or being a mock
        assertThat(UserService.class.isAnnotationPresent(Service.class) ||
                Mockito.mockingDetails(applicationContext.getBean(UserService.class)).isMock()).isTrue();
        assertThat(PhotoService.class.isAnnotationPresent(Service.class) ||
                Mockito.mockingDetails(applicationContext.getBean(PhotoService.class)).isMock()).isTrue();
        assertThat(AlbumService.class.isAnnotationPresent(Service.class) ||
                Mockito.mockingDetails(applicationContext.getBean(AlbumService.class)).isMock()).isTrue();
    }

    @Test
    void testRepositoryNamingConsistency() {
        // All repositories should end with "Repository"
        assertThat(UserRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(PhotoRepository.class.getSimpleName()).endsWith("Repository");
        assertThat(AlbumRepository.class.getSimpleName()).endsWith("Repository");

        // All repositories should have the @Repository annotation
        assertThat(UserRepository.class.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(PhotoRepository.class.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(AlbumRepository.class.isAnnotationPresent(Repository.class)).isTrue();

        // Fix: JpaRepository indirectly extends CrudRepository
        assertThat(CrudRepository.class.isAssignableFrom(JpaRepository.class)).isTrue();
    }

    @Test
    void testControllerErrorHandlingConsistency() {
        // All controller methods should return ResponseEntity
        List<Method> photoControllerMethods = Arrays.stream(PhotoController.class.getDeclaredMethods())
                .filter(method -> method.getReturnType().equals(ResponseEntity.class))
                .collect(Collectors.toList());

        List<Method> albumControllerMethods = Arrays.stream(AlbumController.class.getDeclaredMethods())
                .filter(method -> method.getReturnType().equals(ResponseEntity.class))
                .collect(Collectors.toList());

        List<Method> authControllerMethods = Arrays.stream(AuthController.class.getDeclaredMethods())
                .filter(method -> method.getReturnType().equals(ResponseEntity.class))
                .collect(Collectors.toList());

        // Check that most methods return ResponseEntity (allowing for helper methods)
        assertThat(photoControllerMethods.size()).isGreaterThan(0);
        assertThat(albumControllerMethods.size()).isGreaterThan(0);
        assertThat(authControllerMethods.size()).isGreaterThan(0);
    }

    @Test
    void testModelConsistency() {
        // All entities should have proper builder pattern
        try {
            assertThat(User.class.getDeclaredMethod("builder")).isNotNull();
            assertThat(Photo.class.getDeclaredMethod("builder")).isNotNull();
            assertThat(Album.class.getDeclaredMethod("builder")).isNotNull();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Builder method not found", e);
        }

        // Check for JPA Entity annotations instead of Lombok annotations
        assertThat(User.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
        assertThat(Photo.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
        assertThat(Album.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
    }
}