package com.loyalty;
import com.loyalty.Models.*;
import com.loyalty.Dtos.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import java.util.*;
@RestController @Transactional class AdminController {
 final Segments segments;final Users users;final Audits audits;final AuthService auth;final PasswordEncoder passwords;final AuditService audit;
 AdminController(Segments s,Users u,Audits a,AuthService auth,PasswordEncoder p,AuditService audit){segments=s;users=u;audits=a;this.auth=auth;passwords=p;this.audit=audit;}
 @GetMapping("/api/segments") List<SegmentView> segments(){return segments.findAll(org.springframework.data.domain.Sort.by("name")).stream().map(SegmentView::of).toList();}
 @PostMapping("/api/segments") @PreAuthorize("hasRole('ADMIN')") SegmentView segment(@Valid @RequestBody SegmentInput p){Segment s=new Segment();s.name=p.name();s.active=p.active();segments.saveAndFlush(s);audit.add("CREATE","SEGMENT",s.id,"Создан сегмент");return SegmentView.of(s);}
 @PutMapping("/api/segments/{id}") @PreAuthorize("hasRole('ADMIN')") SegmentView segment(@PathVariable UUID id,@Valid @RequestBody SegmentInput p){Segment s=segments.findById(id).orElseThrow(Problem::missing);Problem.version(s.version,p.version());s.name=p.name();s.active=p.active();segments.flush();audit.add("UPDATE","SEGMENT",id,"Изменён сегмент");return SegmentView.of(s);}
 @GetMapping("/api/admin/users") PageView<UserView> users(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="login,asc") String sort){return PageView.of(users.findAll(CustomerFilter.page(page,size,sort,Set.of("login","name","active"))).map(UserView::of));}
 @PostMapping("/api/admin/users") UserView create(@Valid @RequestBody CreateUser p){AuthService.policy(p.temporaryPassword());User u=new User();u.login=p.login().toLowerCase(Locale.ROOT);u.name=p.name();u.roles=new HashSet<>(p.roles());u.passwordHash=passwords.encode(p.temporaryPassword());users.saveAndFlush(u);audit.add("CREATE","USER",u.id,"Создан пользователь");return UserView.of(u);}
 @PutMapping("/api/admin/users/{id}") UserView update(@PathVariable UUID id,@Valid @RequestBody UserInput p,@AuthenticationPrincipal User caller){User u=users.findById(id).orElseThrow(Problem::missing);Problem.version(u.version,p.version());if(id.equals(caller.id)&&(!p.active()||!p.roles().contains(Role.ADMIN)))throw new Problem(400,"Нельзя отключить собственный доступ администратора");u.login=p.login().toLowerCase(Locale.ROOT);u.name=p.name();u.roles=new HashSet<>(p.roles());u.active=p.active();auth.revoke(id);users.flush();audit.add("UPDATE","USER",id,"Изменён пользователь");return UserView.of(u);}
 @PostMapping("/api/admin/users/{id}/reset-password") void reset(@PathVariable UUID id,@Valid @RequestBody Reset p){AuthService.policy(p.temporaryPassword());User u=users.findById(id).orElseThrow(Problem::missing);u.passwordHash=passwords.encode(p.temporaryPassword());u.mustChangePassword=true;auth.revoke(id);audit.add("PASSWORD_RESET","USER",id,"Назначен временный пароль");}
 record AuditView(UUID id,UUID userId,String username,String event,String entityType,UUID entityId,java.time.Instant createdAt,String description,String correlationId){static AuditView of(Audit a){return new AuditView(a.id,a.userId,a.username,a.event,a.entityType,a.entityId,a.createdAt,a.description,a.correlationId);}}
 @GetMapping("/api/admin/audit") PageView<AuditView> audit(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt,desc") String sort){String term="%"+q.toLowerCase(Locale.ROOT).replace("%","\\%").replace("_","\\_")+"%";return PageView.of(audits.findAll((r,query,b)->b.or(b.like(b.lower(r.get("description")),term,'\\'),b.like(b.lower(r.get("event")),term,'\\'),b.like(b.lower(r.get("username")),term,'\\'),b.like(b.lower(r.get("correlationId")),term,'\\')),CustomerFilter.page(page,size,sort,Set.of("createdAt","event","username","entityType"))).map(AuditView::of));}
}
