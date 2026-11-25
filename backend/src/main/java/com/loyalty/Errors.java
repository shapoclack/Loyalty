package com.loyalty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.*;
import org.springframework.security.access.AccessDeniedException;
class Problem extends RuntimeException {
 final int status; Problem(int status,String message){super(message);this.status=status;}
 static Problem missing(){return new Problem(404,"Запись не найдена");}
 static void version(long actual,Long expected){if(expected==null || actual!=expected)throw new Problem(409,"Запись изменена. Обновите данные и повторите действие");}
}
@RestControllerAdvice
class Errors {
 record Body(Instant timestamp,int status,String message,String correlationId,Map<String,String> fields) {}
 static Body body(int status,String message,HttpServletRequest r,Map<String,String> fields){return new Body(Instant.now(),status,message,(String)r.getAttribute("correlationId"),fields);}
 @ExceptionHandler(Problem.class) ResponseEntity<Body> problem(Problem e,HttpServletRequest r){return ResponseEntity.status(e.status).body(body(e.status,e.getMessage(),r,Map.of()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Body> validation(MethodArgumentNotValidException e,HttpServletRequest r){Map<String,String> f=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->f.put(x.getField(),x.getDefaultMessage()));return ResponseEntity.badRequest().body(body(400,"Проверьте поля запроса",r,f));}
 @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class,DataIntegrityViolationException.class}) ResponseEntity<Body> conflict(Exception e,HttpServletRequest r){return ResponseEntity.status(409).body(body(409,"Конфликт версии или уникального значения",r,Map.of()));}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Body> forbidden(Exception e,HttpServletRequest r){return ResponseEntity.status(403).body(body(403,"Недостаточно прав",r,Map.of()));}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.MultipartException.class,jakarta.validation.ConstraintViolationException.class}) ResponseEntity<Body> bad(Exception e,HttpServletRequest r){return ResponseEntity.badRequest().body(body(400,"Некорректные параметры запроса",r,Map.of()));}
 @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<Body> notFound(Exception e,HttpServletRequest r){return ResponseEntity.status(404).body(body(404,"Ресурс не найден",r,Map.of()));}
 @ExceptionHandler(Exception.class) ResponseEntity<Body> other(Exception e,HttpServletRequest r){org.slf4j.LoggerFactory.getLogger(Errors.class).error("Request {} failed",r.getAttribute("correlationId"),e);return ResponseEntity.internalServerError().body(body(500,"Внутренняя ошибка сервера",r,Map.of()));}
}
