package com.loyalty;
import com.loyalty.Models.*;
import com.loyalty.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@Service class AuthService {
 final Users users; final Sessions sessions; final Attempts attempts; final PasswordEncoder passwords;final JwtEncoder jwt; final AuditService audit;
 @Value("${app.issuer}") String issuer; @Value("${app.access-minutes}") long accessMinutes; @Value("${app.refresh-days}") long refreshDays;
 AuthService(Users u,Sessions s,Attempts a,PasswordEncoder p,JwtEncoder j,AuditService audit){users=u;sessions=s;attempts=a;passwords=p;jwt=j;this.audit=audit;}
 static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 static void policy(String p){if(p==null || p.length()<12 || p.getBytes(StandardCharsets.UTF_8).length>72 || !p.matches("(?s).*[A-Z].*") || !p.matches("(?s).*[a-z].*") || !p.matches("(?s).*[0-9].*"))throw new Problem(400,"Пароль: 12–72 байта, строчная и заглавная латинские буквы, цифра");}
 record Tokens(String accessToken,String refreshToken,long expiresIn,UserView user) {}
 @Transactional(noRollbackFor=Problem.class) public Tokens login(Login input,String ip){String login=input.login().toLowerCase(Locale.ROOT).trim();String accountKey=hash("account:"+login),ipKey=hash("ip:"+ip);check(accountKey);check(ipKey);User u=users.findByLogin(login).orElse(null);boolean valid=passwords.matches(input.password(),u==null?DUMMY:u.passwordHash);if(u==null||!u.active||!valid){fail(accountKey);fail(ipKey);audit.add(u,"LOGIN_FAILED","USER",u==null?null:u.id,"Неудачный вход");throw new Problem(401,"Неверный логин или пароль");}attempts.deleteById(accountKey);audit.add(u,"LOGIN","USER",u.id,"Вход в систему");return issue(u);}
 static final String DUMMY=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode("DummyPassword123456");
 void check(String key){var a=attempts.findById(key).orElse(null);if(a!=null&&a.lockedUntil!=null&&a.lockedUntil.isAfter(Instant.now()))throw new Problem(429,"Слишком много попыток. Повторите через 15 минут");}
 void fail(String key){var a=attempts.findById(key).orElseGet(()->{var x=new LoginAttempt();x.key=key;x.windowStart=Instant.now();return x;});if(a.windowStart.isBefore(Instant.now().minusSeconds(900))){a.failures=0;a.windowStart=Instant.now();}if(++a.failures>=5)a.lockedUntil=Instant.now().plusSeconds(900);attempts.save(a);}
 Tokens issue(User u){byte[] bytes=new byte[48];new SecureRandom().nextBytes(bytes);String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);Session s=new Session();s.user=u;s.tokenHash=hash(refresh);s.expiresAt=Instant.now().plus(Duration.ofDays(refreshDays));sessions.save(s);Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer(issuer).subject(u.id.toString()).issuedAt(now).expiresAt(now.plusSeconds(accessMinutes*60)).claim("uv",Long.toString(u.version)).build();String access=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(),claims)).getTokenValue();return new Tokens(access,refresh,accessMinutes*60,UserView.of(u));}
 @Transactional public Tokens refresh(String token){if(token==null)throw new Problem(401,"Refresh-токен отсутствует");Session s=sessions.findByTokenHash(hash(token)).orElseThrow(()->new Problem(401,"Сессия недействительна"));if(s.revoked||s.expiresAt.isBefore(Instant.now())||!s.user.active)throw new Problem(401,"Сессия истекла");s.revoked=true;return issue(s.user);}
 @Transactional public void logout(String token){if(token!=null)sessions.findByTokenHash(hash(token)).ifPresent(s->{s.revoked=true;audit.add(s.user,"LOGOUT","USER",s.user.id,"Выход");});}
 @Transactional public void password(User principal,Password p){User u=users.findById(principal.id).orElseThrow();if(!passwords.matches(p.currentPassword(),u.passwordHash))throw new Problem(400,"Текущий пароль неверен");policy(p.newPassword());if(passwords.matches(p.newPassword(),u.passwordHash))throw new Problem(400,"Новый пароль должен отличаться");u.passwordHash=passwords.encode(p.newPassword());u.mustChangePassword=false;revoke(u.id);audit.add("PASSWORD_CHANGED","USER",u.id,"Пароль изменён");}
 void revoke(UUID userId){sessions.findByUserIdAndRevokedFalse(userId).forEach(s->s.revoked=true);}
}
