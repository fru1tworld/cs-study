# Spring Framework & Spring Boot 공식 문서 정리

> 최종 업데이트: 2025-11-28
> Spring Framework: https://docs.spring.io/spring-framework/reference/
> Spring Boot: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/

## 버전 정보

| 프레임워크 | 버전 | 상태 |
|-----------|------|------|
| Spring Framework | 6.2 | Current (JDK 17-23, Jakarta EE 9-10) |
| Spring Framework | 7.0 | 2025년 11월 (JDK 17+, Jakarta EE 11) |
| Spring Boot | 3.5 | Current (Spring Framework 6.2 기반) |
| Spring Boot | 4.0 | 2025년 11월 (Spring Framework 7.0 기반) |

---

## Spring Framework 핵심 개념

### IoC (Inversion of Control) & DI (Dependency Injection)

```java
// 인터페이스 정의
public interface MessageService {
    String getMessage();
}

// 구현체
@Component
public class EmailService implements MessageService {
    @Override
    public String getMessage() {
        return "Email message";
    }
}

// 의존성 주입 (생성자 주입 - 권장)
@Service
public class NotificationService {
    private final MessageService messageService;

    public NotificationService(MessageService messageService) {
        this.messageService = messageService;
    }
}
```

### Bean Scope

| Scope | 설명 |
|-------|------|
| singleton | 기본값, 컨테이너당 하나의 인스턴스 |
| prototype | 요청마다 새 인스턴스 |
| request | HTTP 요청당 하나 (웹) |
| session | HTTP 세션당 하나 (웹) |
| application | ServletContext당 하나 (웹) |

```java
@Component
@Scope("prototype")
public class PrototypeBean { }
```

---

## Spring Boot 핵심 기능

### 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── DemoApplication.java
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       └── config/
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── static/
└── test/
```

### 메인 애플리케이션

```java
@SpringBootApplication  // @Configuration + @EnableAutoConfiguration + @ComponentScan
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 설정 (application.yml)

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example: DEBUG
```

---

## REST Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

---

## Spring Data JPA

### Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // getters, setters
}
```

### Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

    @Query(value = "SELECT * FROM users WHERE email LIKE %:domain", nativeQuery = true)
    List<User> findByEmailDomain(@Param("domain") String domain);
}
```

### Service

```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User create(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
```

---

## 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException e) {
        return new ErrorResponse("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception e) {
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
    }
}

public record ErrorResponse(String code, String message) {}
```

---

## Validation

```java
public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    String name,

    @Min(value = 0, message = "Age must be positive")
    @Max(value = 150, message = "Age must be less than 150")
    Integer age
) {}
```

---

## 테스트

### 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findById_WhenExists_ReturnsUser() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }
}
```

### 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_WithValidRequest_ReturnsCreated() throws Exception {
        CreateUserRequest request = new CreateUserRequest("test@example.com", "Test User", 25);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
```

---

## Spring Boot 4.0 주요 변경사항 (2025년 11월)

- Spring Framework 7.0 기반
- Spring Security 7.0
- Spring Data 2025.1
- Hibernate 7.1
- Jackson 3.0
- Flyway 11.11
- Jakarta EE 11 (Tomcat 11, Hibernate Validator 9)

---

## 참고 문서

- [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
