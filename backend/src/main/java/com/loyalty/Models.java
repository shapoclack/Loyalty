package com.loyalty;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

class Models {
 enum Role { VIEWER, EDITOR, ADMIN }
 enum Status { ACTIVE, BLOCKED, INACTIVE }
 enum Tier { BASE, SILVER, GOLD }
 static Tier tier(BigDecimal value) { return value.compareTo(new BigDecimal("50000"))>=0?Tier.GOLD:value.compareTo(new BigDecimal("10000"))>=0?Tier.SILVER:Tier.BASE; }
 @MappedSuperclass static abstract class Identity implements org.springframework.data.domain.Persistable<UUID> {
  @Id UUID id=UUID.randomUUID();
  @Transient boolean fresh=true;
  public UUID getId(){return id;}
  public boolean isNew(){return fresh;}
  @PostLoad @PostPersist void persisted(){fresh=false;}
 }
 @Entity(name="User") @Table(name="app_users") static class User extends Identity {
  @Column(nullable=false,unique=true,length=100) String login;
  @Column(nullable=false,length=120) String name;
  @Column(nullable=false,length=100) String passwordHash;
  boolean active=true; boolean mustChangePassword=true;
  @Version long version;
  @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="user_roles",joinColumns=@JoinColumn(name="user_id")) @Column(name="role") @Enumerated(EnumType.STRING) Set<Role> roles=new HashSet<>();
 }
 @Entity(name="Session") @Table(name="refresh_sessions") static class Session extends Identity {
  @ManyToOne(optional=false) User user;
  @Column(nullable=false,unique=true,length=64) String tokenHash;
  Instant expiresAt; boolean revoked; Instant createdAt=Instant.now();
 }
 @Entity(name="Segment") @Table(name="segments") static class Segment extends Identity {
  @Column(nullable=false,unique=true,length=80) String name; boolean active=true; @Version long version;
 }
 @Entity(name="Customer") @Table(name="customers") static class Customer extends Identity {
  @Column(nullable=false,unique=true,length=64) String code;
  @Column(nullable=false,length=120) String name;
  @Column(length=254) String email;
  @Column(length=30) String phone;
  @Column(length=2000) String description;
  @ManyToOne(optional=false) Segment segment;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Status status=Status.ACTIVE;
  @Column(nullable=false,precision=15,scale=2) BigDecimal totalPurchases=BigDecimal.ZERO;
  boolean marketingConsent; boolean archived;
  @ElementCollection @CollectionTable(name="customer_tags",joinColumns=@JoinColumn(name="customer_id")) @Column(name="tag",length=40) Set<String> tags=new HashSet<>();
  Instant createdAt=Instant.now(); Instant updatedAt=Instant.now(); @Version long version;
  @PreUpdate void updated() { updatedAt=Instant.now(); }
 }
 @Entity(name="Note") @Table(name="notes") static class Note extends Identity {
  @ManyToOne(optional=false) Customer customer;
  @ManyToOne(optional=false) User author;
  @Column(nullable=false,length=4000) String text;
  boolean pinned; Instant createdAt=Instant.now(); Instant updatedAt=Instant.now(); @Version long version;
 }
 @Entity(name="Audit") @Table(name="audit_events") static class Audit extends Identity {
  UUID userId; @Column(length=100) String username;
  @Column(length=60) String event; @Column(length=60) String entityType; UUID entityId;
  Instant createdAt=Instant.now(); @Column(length=500) String description; @Column(length=80) String correlationId;
 }
 @Entity(name="ImportJob") @Table(name="import_jobs") static class ImportJob extends Identity {
  UUID userId; @Column(columnDefinition="text") String payload; @Column(length=20) String mode;
  Instant expiresAt; boolean applied; @Version long version;
 }
 @Entity(name="LoginAttempt") @Table(name="login_attempts") static class LoginAttempt {
  @Id @Column(length=64) String key; int failures; Instant windowStart; Instant lockedUntil;
 }
}
