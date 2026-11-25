package com.loyalty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import com.loyalty.Models.*;
import com.loyalty.Dtos.*;
import java.util.*;
@Service @Transactional class CustomerService {
 final Customers customers;final Segments segments;final Notes notes;final AuditService audit;
 CustomerService(Customers c,Segments s,Notes n,AuditService a){customers=c;segments=s;notes=n;audit=a;}
 Customer get(UUID id){return customers.findById(id).orElseThrow(Problem::missing);}
 @Transactional(readOnly=true) public CustomerView one(UUID id){return CustomerView.of(get(id));}
 @Transactional(readOnly=true) public PageView<CustomerView> list(CustomerFilter f,Pageable p){return PageView.of(customers.findAll(f.spec(),p).map(CustomerView::of));}
 public CustomerView create(CustomerInput p){Customer c=new Customer();copy(c,p);customers.saveAndFlush(c);audit.add("CREATE","CUSTOMER",c.id,"Создан клиент "+c.code);return CustomerView.of(c);}
 public CustomerView update(UUID id,CustomerInput p){Customer c=get(id);Problem.version(c.version,p.version());copy(c,p);customers.flush();audit.add("UPDATE","CUSTOMER",id,"Изменён клиент "+c.code);return CustomerView.of(c);}
 void copy(Customer c,CustomerInput p){Segment s=segments.findById(p.segmentId()).orElseThrow(Problem::missing);if(!s.active&&(c.segment==null||!c.segment.id.equals(s.id)))throw new Problem(400,"Сегмент отключён");c.code=p.code();c.name=p.name().trim();c.email=p.email();c.phone=p.phone();c.description=p.description();c.segment=s;c.status=p.status();c.totalPurchases=p.totalPurchases();c.marketingConsent=p.marketingConsent();c.tags=new HashSet<>(p.tags());}
 public void archive(UUID id,long version,boolean value){Customer c=get(id);Problem.version(c.version,version);c.archived=value;customers.flush();audit.add(value?"ARCHIVE":"RESTORE","CUSTOMER",id,"Архив: "+value);}
 public void permanent(UUID id,long version){Customer c=get(id);Problem.version(c.version,version);if(!c.archived)throw new Problem(400,"Сначала архивируйте запись");notes.deleteByCustomerId(id);customers.delete(c);customers.flush();audit.add("DELETE","CUSTOMER",id,"Окончательное удаление");}
 public void bulkOne(UUID id,Bulk p){Customer c=get(id);Problem.version(c.version,p.versions().get(id));switch(p.action()){case ARCHIVE->c.archived=true;case RESTORE->c.archived=false;case STATUS->{if(p.status()==null)throw new Problem(400,"Нужен статус");c.status=p.status();}case ADD_TAG->{validTag(p.tag());if(c.tags.size()>=20&&!c.tags.contains(p.tag()))throw new Problem(400,"Максимум 20 тегов");c.tags.add(p.tag());c.updatedAt=java.time.Instant.now();}case REMOVE_TAG->{validTag(p.tag());c.tags.remove(p.tag());c.updatedAt=java.time.Instant.now();}}customers.flush();audit.add("BULK_"+p.action(),"CUSTOMER",id,"Массовая операция");}
 void validTag(String t){if(t==null||t.isBlank())throw new Problem(400,"Нужен тег");}
 @Transactional(readOnly=true) public List<NoteView> notes(UUID id){get(id);return notes.findByCustomerIdOrderByPinnedDescCreatedAtDesc(id).stream().map(NoteView::of).toList();}
 public NoteView note(UUID id,UUID noteId,NoteInput p,User user){Customer c=get(id);Note n=noteId==null?new Note():getNote(id,noteId);if(noteId!=null)Problem.version(n.version,p.version());n.customer=c;if(noteId==null)n.author=user;n.text=p.text();n.pinned=p.pinned();n.updatedAt=java.time.Instant.now();notes.saveAndFlush(n);audit.add(noteId==null?"NOTE_CREATE":"NOTE_UPDATE","NOTE",n.id,"Заметка клиента "+c.code);return NoteView.of(n);}
 Note getNote(UUID id,UUID nid){Note n=notes.findById(nid).orElseThrow(Problem::missing);if(!n.customer.id.equals(id))throw Problem.missing();return n;}
 public void deleteNote(UUID id,UUID nid,long version){Note n=getNote(id,nid);Problem.version(n.version,version);notes.delete(n);audit.add("NOTE_DELETE","NOTE",nid,"Удалена заметка");}
}
