package com.loyalty;
import com.loyalty.Models.*;
import com.loyalty.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.csv.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.io.*;
@Service class CsvService {
 static final String[] HEADERS={"code","name","email","phone","description","segmentId","status","totalPurchases","marketingConsent","tags"};
 final Customers customers;final Segments segments;final ImportJobs jobs;final CustomerService service;final ObjectMapper json;final Validator validator;final AuditService audit;
 CsvService(Customers c,Segments s,ImportJobs j,CustomerService cs,ObjectMapper o,Validator v,AuditService a){customers=c;segments=s;jobs=j;service=cs;json=o;validator=v;audit=a;}
 public record Row(int number,CustomerInput data,UUID existingId,Long expectedVersion,String error) {}
 public record Preview(UUID previewId,int validRows,int invalidRows,List<Row> rows) {}
 @Transactional public Preview preview(String csv,String mode,User user)throws IOException {
  if(!Set.of("NEW_ONLY","UPSERT").contains(mode))throw new Problem(400,"Режим: NEW_ONLY или UPSERT");
  if(csv.startsWith("\ufeff"))csv=csv.substring(1);List<Row> rows=new ArrayList<>();Set<String> seen=new HashSet<>();
  try(CSVParser parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get().parse(new StringReader(csv))){if(!parser.getHeaderNames().equals(List.of(HEADERS)))throw new Problem(400,"CSV должен содержать заголовок: "+String.join(",",HEADERS));for(CSVRecord r:parser){if(rows.size()>=5000)throw new Problem(400,"Максимум 5000 строк");int n=(int)r.getRecordNumber()+1;try{
    String code=r.get("code");if(!seen.add(code))throw new IllegalArgumentException("Код повторяется в файле");Customer existing=customers.findByCode(code).orElse(null);if(existing!=null&&mode.equals("NEW_ONLY"))throw new IllegalArgumentException("Код уже существует");
    String bool=r.get("marketingConsent");if(!Set.of("true","false").contains(bool))throw new IllegalArgumentException("marketingConsent: true или false");Set<String> tags=r.get("tags").isBlank()?Set.of():new HashSet<>(Arrays.asList(unprotect(r.get("tags")).split("\\|")));
    var p=new CustomerInput(code,unprotect(r.get("name")),unprotect(r.get("email")),unprotect(r.get("phone")),unprotect(r.get("description")),UUID.fromString(r.get("segmentId")),Status.valueOf(r.get("status")),new BigDecimal(r.get("totalPurchases")),Boolean.parseBoolean(bool),tags,existing==null?0L:existing.version);
    var violations=validator.validate(p);if(!violations.isEmpty())throw new IllegalArgumentException(violations.iterator().next().getPropertyPath()+": "+violations.iterator().next().getMessage());Segment seg=segments.findById(p.segmentId()).orElseThrow(()->new IllegalArgumentException("Сегмент не найден"));if(!seg.active)throw new IllegalArgumentException("Сегмент отключён");rows.add(new Row(n,p,existing==null?null:existing.id,existing==null?null:existing.version,null));
   }catch(IllegalArgumentException e){rows.add(new Row(n,null,null,null,"Неверная строка: "+e.getMessage()));}}}catch(UncheckedIOException e){throw new Problem(400,"Повреждённый CSV");}
  if(rows.isEmpty())throw new Problem(400,"CSV пуст");ImportJob job=new ImportJob();job.userId=user.id;job.mode=mode;job.expiresAt=Instant.now().plusSeconds(1800);job.payload=json.writeValueAsString(rows);jobs.save(job);int errors=(int)rows.stream().filter(x->x.error()!=null).count();return new Preview(job.id,rows.size()-errors,errors,rows);
 }
 @Transactional public Map<String,Object> apply(UUID id,User user)throws IOException{ImportJob job=jobs.lockById(id).orElseThrow(Problem::missing);if(!job.userId.equals(user.id))throw new Problem(403,"Предпросмотр принадлежит другому пользователю");if(job.applied||job.expiresAt.isBefore(Instant.now()))throw new Problem(409,"Предпросмотр применён или истёк");List<Row> rows=json.readValue(job.payload,json.getTypeFactory().constructCollectionType(List.class,Row.class));if(rows.stream().anyMatch(r->r.error()!=null))throw new Problem(400,"Исправьте все ошибки и повторите проверку");for(Row r:rows){if(r.existingId()==null){if(customers.findByCode(r.data().code()).isPresent())throw new Problem(409,"После проверки появился клиент с этим кодом");service.create(r.data());}else{service.update(r.existingId(),r.data());}}job.applied=true;audit.add("IMPORT","CUSTOMER",id,"Импортировано строк: "+rows.size());return Map.of("applied",rows.size());}
 @Transactional(readOnly=true) public String export(CustomerFilter f,String sort)throws IOException{StringWriter writer=new StringWriter();writer.write('\ufeff');try(CSVPrinter csv=new CSVPrinter(writer,CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())){int page=0;org.springframework.data.domain.Page<Customer> batch;do{batch=customers.findAll(f.spec(),CustomerFilter.page(page++,100,sort));for(Customer c:batch)csv.printRecord(c.code,safe(c.name),safe(c.email),safe(c.phone),safe(c.description),c.segment.id,c.status,c.totalPurchases,c.marketingConsent,safe(String.join("|",new TreeSet<>(c.tags))));}while(batch.hasNext());}return writer.toString();}
 static String unprotect(String value){return value.startsWith("'")&&!safe(value.substring(1)).equals(value.substring(1))?value.substring(1):value;}
 static String safe(String value){return value!=null&&value.matches("(?s)^[=+@\\-\\t\\r].*")?"'"+value:value;}
}
