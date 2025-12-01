# NestJS 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://docs.nestjs.com/

## 개요

NestJS는 효율적이고 확장 가능한 서버 사이드 애플리케이션을 구축하기 위한 진보적인 Node.js 프레임워크입니다. TypeScript를 기본으로 지원하며, OOP(객체지향 프로그래밍), FP(함수형 프로그래밍), FRP(함수형 반응형 프로그래밍)의 요소를 결합합니다.

내부적으로 Express(또는 Fastify)를 래핑하여 HTTP 서버를 구성합니다.

---

## 핵심 아키텍처

### 1. Modules (모듈)

모듈은 `@Module()` 데코레이터를 사용하여 정의됩니다. 애플리케이션을 기능 단위로 조직화합니다.

```typescript
@Module({
  imports: [],      // 이 모듈에서 필요한 다른 모듈
  controllers: [],  // 이 모듈의 컨트롤러
  providers: [],    // 이 모듈의 서비스/프로바이더
  exports: [],      // 외부에 공개할 프로바이더
})
export class AppModule {}
```

**주요 속성:**
- `imports`: 이 모듈에서 사용할 외부 모듈 목록
- `controllers`: HTTP 요청을 처리할 컨트롤러 목록
- `providers`: 비즈니스 로직을 담당하는 서비스 목록
- `exports`: 다른 모듈에서 사용할 수 있도록 공개할 프로바이더

### 2. Controllers (컨트롤러)

컨트롤러는 들어오는 HTTP 요청을 처리하고 응답을 반환합니다.

```typescript
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  findAll(): User[] {
    return this.usersService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string): User {
    return this.usersService.findOne(id);
  }

  @Post()
  create(@Body() createUserDto: CreateUserDto): User {
    return this.usersService.create(createUserDto);
  }
}
```

**HTTP 메서드 데코레이터:**
- `@Get()`, `@Post()`, `@Put()`, `@Patch()`, `@Delete()`

**파라미터 데코레이터:**
- `@Param()`: URL 파라미터
- `@Query()`: 쿼리 스트링
- `@Body()`: 요청 본문
- `@Headers()`: 헤더

### 3. Providers (프로바이더)

프로바이더는 `@Injectable()` 데코레이터가 붙은 클래스로, 의존성 주입(DI)의 핵심입니다.

```typescript
@Injectable()
export class UsersService {
  private readonly users: User[] = [];

  findAll(): User[] {
    return this.users;
  }

  create(user: User): User {
    this.users.push(user);
    return user;
  }
}
```

**의존성 주입(Dependency Injection):**
- NestJS는 생성자 기반 의존성 주입을 사용
- IoC 컨테이너가 인스턴스 생성 및 생명주기 관리

---

## 주요 기능

### Middleware

요청이 라우트 핸들러에 도달하기 전에 실행되는 함수입니다.

```typescript
@Injectable()
export class LoggerMiddleware implements NestMiddleware {
  use(req: Request, res: Response, next: NextFunction) {
    console.log('Request...');
    next();
  }
}
```

### Guards

인증/인가 로직을 처리합니다.

```typescript
@Injectable()
export class AuthGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    return validateRequest(request);
  }
}
```

### Interceptors

메서드 실행 전/후에 추가 로직을 바인딩합니다.

```typescript
@Injectable()
export class TransformInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(map(data => ({ data })));
  }
}
```

### Pipes

데이터 변환 및 유효성 검사를 수행합니다.

```typescript
@Get(':id')
findOne(@Param('id', ParseIntPipe) id: number) {
  return this.usersService.findOne(id);
}
```

### Exception Filters

예외를 처리하고 클라이언트에 응답을 반환합니다.

```typescript
@Catch(HttpException)
export class HttpExceptionFilter implements ExceptionFilter {
  catch(exception: HttpException, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const status = exception.getStatus();

    response.status(status).json({
      statusCode: status,
      message: exception.message,
    });
  }
}
```

---

## 요청 생명주기

```
Request → Middleware → Guards → Interceptors (before) → Pipes → Route Handler → Interceptors (after) → Exception Filters → Response
```

---

## CLI 명령어

```bash
# 새 프로젝트 생성
nest new project-name

# 리소스 생성 (CRUD)
nest g resource users

# 개별 요소 생성
nest g controller users
nest g service users
nest g module users
nest g guard auth
nest g interceptor logging
nest g pipe validation
nest g filter http-exception
```

---

## 참고 문서

- [Modules](https://docs.nestjs.com/modules)
- [Controllers](https://docs.nestjs.com/controllers)
- [Providers](https://docs.nestjs.com/providers)
- [Middleware](https://docs.nestjs.com/middleware)
- [Guards](https://docs.nestjs.com/guards)
- [Interceptors](https://docs.nestjs.com/interceptors)
- [Pipes](https://docs.nestjs.com/pipes)
- [Exception Filters](https://docs.nestjs.com/exception-filters)
