// src/test/java/com/example/photo_album/integration/PostgresConcurrencyTest.java
package com.example.photo_album.integration;

import com.example.photo_album.model.User;
import com.example.photo_album.repository.UserRepository;
import com.example.photo_album.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
public class PostgresConcurrencyTest extends AbstractPostgresqlTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void testConcurrentUserCreation() throws InterruptedException {
        // Clean up any existing users
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // End current transaction before starting threads
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Create a fixed thread pool
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Create a list of tasks
        List<Callable<User>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            tasks.add(() -> {
                try {
                    // All threads try to create a user with the same username
                    String uniqueEmail = "test" + index + "@example.com";
                    User user = userService.registerUser(
                            "testuser",
                            uniqueEmail,
                            "password123"
                    );
                    System.out.println("Successfully created user with email: " + uniqueEmail);
                    return user;
                } catch (Exception e) {
                    System.out.println("Failed to create user: " + e.getMessage());
                    // Return null if the operation fails
                    return null;
                }
            });
        }

        // Execute all tasks concurrently
        List<Future<User>> futures = executorService.invokeAll(tasks);

        // Shutdown the executor
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(20, TimeUnit.SECONDS);

        System.out.println("All threads completed: " + terminated);

        // Start a new transaction for assertion
        TestTransaction.start();

        // Count successful user creations
        long successfulCreations = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        System.out.println("Error getting future result: " + e.getMessage());
                        return null;
                    }
                })
                .filter(user -> user != null)
                .count();

        System.out.println("Successful creations: " + successfulCreations);
        System.out.println("Total users in DB: " + userRepository.count());

        // Print all users in the database for debugging
        userRepository.findAll().forEach(u ->
                System.out.println("User in DB: " + u.getUsername() + ", " + u.getEmail())
        );

        // Verify that only one user with the username "testuser" was created
        assertThat(successfulCreations).isEqualTo(1);
        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }

    @Test
    void testConcurrentUniqueUserCreation() throws InterruptedException {
        // Use a direct approach without transaction in the test
        userRepository.deleteAll();

        // Create a countdown latch to coordinate the start of all threads
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // Track creation results
        AtomicInteger successCount = new AtomicInteger(0);
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        // Create and start all threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    // Wait for signal to start
                    startLatch.await();

                    String username = "testuser" + index;
                    String email = "test" + index + "@example.com";

                    // Try with retries
                    for (int attempt = 0; attempt < 3; attempt++) {
                        try {
                            User user = userService.registerUser(username, email, "password123");
                            if (user != null) {
                                successCount.incrementAndGet();
                                System.out.println("Successfully created user: " + username);
                                break; // Success, exit retry loop
                            }
                        } catch (Exception e) {
                            System.out.println("Attempt " + (attempt+1) + " failed for user " +
                                    username + ": " + e.getMessage());
                            if (attempt == 2) {
                                failures.add(username + ": " + e.getMessage());
                            } else {
                                Thread.sleep(200); // Wait before retry
                            }
                        }
                    }
                } catch (Exception e) {
                    failures.add("Thread error for user " + index + ": " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        System.out.println("All threads completed: " + completed);

        // Print failures for debugging
        if (!failures.isEmpty()) {
            System.out.println("Failures: ");
            failures.forEach(System.out::println);
        }

        // Get count from database
        long dbCount = userRepository.count();
        System.out.println("Success count: " + successCount.get());
        System.out.println("DB count: " + dbCount);

        // Print all users for debugging
        userRepository.findAll().forEach(u ->
                System.out.println("User in DB: " + u.getUsername() + ", " + u.getEmail())
        );

        // Verify results - all users should be created
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(dbCount).isEqualTo(threadCount);
    }

    @Test
    @Transactional
    void testUsernameUniqueConstraint() {
        // Clean up any existing users
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create a user directly with the repository
        User user1 = User.builder()
                .username("uniqueuser")
                .email("unique@example.com")
                .password(passwordEncoder.encode("password"))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user1);
        entityManager.flush(); // Force SQL execution

        // Try to create another user with the same username
        User user2 = User.builder()
                .username("uniqueuser") // Same username
                .email("different@example.com")
                .password(passwordEncoder.encode("password"))
                .createdAt(LocalDateTime.now())
                .build();

        // This should fail with a constraint violation
        try {
            userRepository.save(user2);
            entityManager.flush(); // Force SQL execution
            // If we reach here, the constraint wasn't enforced
            assertThat(false).withFailMessage("Expected username constraint violation was not thrown").isTrue();
        } catch (Exception e) {
            // Check if the exception or its cause contains information about the constraint violation
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " " + e.getCause().getMessage();
            }
            assertThat(errorMessage).contains("duplicate key");
        }
    }

    @Test
    @Transactional
    void testEmailUniqueConstraint() {
        // Clean up any existing users
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create a user directly with the repository
        User user1 = User.builder()
                .username("firstuser")
                .email("unique@example.com")
                .password(passwordEncoder.encode("password"))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user1);
        entityManager.flush(); // Force SQL execution

        // Try to create another user with the same email
        User user3 = User.builder()
                .username("differentuser")
                .email("unique@example.com") // Same email
                .password(passwordEncoder.encode("password"))
                .createdAt(LocalDateTime.now())
                .build();

        // This should also fail with a constraint violation
        try {
            userRepository.save(user3);
            entityManager.flush(); // Force SQL execution
            // If we reach here, the constraint wasn't enforced
            assertThat(false).withFailMessage("Expected email constraint violation was not thrown").isTrue();
        } catch (Exception e) {
            // Check if the exception or its cause contains information about the constraint violation
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " " + e.getCause().getMessage();
            }
            assertThat(errorMessage).contains("duplicate key");
        }
    }

}