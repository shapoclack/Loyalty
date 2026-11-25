package com.loyalty;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.loyalty.Models.*;
class Dtos {
 record Login(@NotBlank @Size(max=100) String login,@NotBlank @Size(max=72) String password,boolean mobile) {}
 record Refresh(@Size(max=200) String refreshToken) {}
 record Password(@NotBlank String currentPassword,@NotBlank @Size(min=12,max=72) String newPassword) {}
 record UserInput(@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9@._+\\-]+") String login,@NotBlank @Size(max=120) String name,@NotEmpty Set<@NotNull Role> roles,boolean active,@NotNull @PositiveOrZero Long version) {}
 record CreateUser(@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9@._+\\-]+") String login,@NotBlank @Size(max=120) String name,@NotEmpty Set<@NotNull Role> roles,@NotBlank @Size(min=12,max=72) String temporaryPassword) {}
 record Reset(@NotBlank @Size(min=12,max=72) String temporaryPassword) {}
 record UserView(UUID id,String login,String name,Set<@NotNull Role> roles,boolean active,boolean mustChangePassword,long version) { static UserView of(User u) { return new UserView(u.id,u.login,u.name,Set.copyOf(u.roles),u.active,u.mustChangePassword,u.version); } }
 record CustomerInput(@NotBlank @Size(max=64) @Pattern(regexp="[A-Za-z0-9_-]+") String code,@NotBlank @Size(max=120) String name,@Email @Size(max=254) String email,@Size(max=30) @Pattern(regexp="[+0-9 ()-]*") String phone,@Size(max=2000) String description,@NotNull UUID segmentId,@NotNull Status status,@NotNull @DecimalMin("0") @DecimalMax("9999999999999.99") @Digits(integer=13,fraction=2) BigDecimal totalPurchases,boolean marketingConsent,@NotNull @Size(max=20) Set<@NotBlank @Size(max=40) @Pattern(regexp="[^|\\r\\n]+") String> tags,@NotNull @PositiveOrZero Long version) {}
 record CustomerView(UUID id,String code,String name,String email,String phone,String description,UUID segmentId,String segmentName,Status status,BigDecimal totalPurchases,boolean marketingConsent,Set<String> tags,Tier tier,Instant createdAt,Instant updatedAt,long version,boolean archived) {
  static CustomerView of(Customer c) { return new CustomerView(c.id,c.code,c.name,c.email,c.phone,c.description,c.segment.id,c.segment.name,c.status,c.totalPurchases,c.marketingConsent,Set.copyOf(c.tags),Models.tier(c.totalPurchases),c.createdAt,c.updatedAt,c.version,c.archived); }
 }
 record SegmentInput(@NotBlank @Size(max=80) String name,boolean active,@NotNull @PositiveOrZero Long version) {}
 record SegmentView(UUID id,String name,boolean active,long version) { static SegmentView of(Segment s){ return new SegmentView(s.id,s.name,s.active,s.version); } }
 record NoteInput(@NotBlank @Size(max=4000) String text,boolean pinned,@NotNull @PositiveOrZero Long version) {}
 record NoteView(UUID id,String text,boolean pinned,UUID authorId,String authorName,Instant createdAt,Instant updatedAt,long version) { static NoteView of(Note n){return new NoteView(n.id,n.text,n.pinned,n.author.id,n.author.name,n.createdAt,n.updatedAt,n.version);} }
 enum BulkAction { ARCHIVE, RESTORE, STATUS, ADD_TAG, REMOVE_TAG }
 record Bulk(@NotEmpty @Size(max=100) Set<@NotNull UUID> ids,@NotNull BulkAction action,Status status,@Size(max=40) @Pattern(regexp="[^|\\r\\n]+") String tag,@NotNull Map<UUID,@PositiveOrZero Long> versions) {}
 record Apply(@NotNull UUID previewId) {}
 record PageView<T>(List<T> content,long totalElements,int totalPages,int number,int size) {
  static <T> PageView<T> of(org.springframework.data.domain.Page<T> p){return new PageView<>(p.getContent(),p.getTotalElements(),p.getTotalPages(),p.getNumber(),p.getSize());}
 }
}
