package com.loyalty;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import com.loyalty.Models.*;
interface Users extends JpaRepository<User,UUID> { Optional<User> findByLogin(String login); }
interface Sessions extends JpaRepository<Session,UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Session> findByTokenHash(String tokenHash);
 List<Session> findByUserIdAndRevokedFalse(UUID id);
}
interface Segments extends JpaRepository<Segment,UUID> {}
interface Customers extends JpaRepository<Customer,UUID>,JpaSpecificationExecutor<Customer> { Optional<Customer> findByCode(String code); }
interface Notes extends JpaRepository<Note,UUID> { List<Note> findByCustomerIdOrderByPinnedDescCreatedAtDesc(UUID id); void deleteByCustomerId(UUID id); }
interface Audits extends JpaRepository<Audit,UUID>,JpaSpecificationExecutor<Audit> {}
interface ImportJobs extends JpaRepository<ImportJob,UUID> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select j from ImportJob j where j.id = :id") Optional<ImportJob> lockById(UUID id); }
interface Attempts extends JpaRepository<LoginAttempt,String> {
 @Override @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<LoginAttempt> findById(String key);
}
