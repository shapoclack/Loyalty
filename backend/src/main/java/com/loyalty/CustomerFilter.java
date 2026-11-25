package com.loyalty;
import com.loyalty.Models.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.*;
public record CustomerFilter(String q,UUID segmentId,Status status,Boolean marketingConsent,Tier tier,String tag,Boolean archived,BigDecimal amount,String amountOp) {
 Specification<Customer> spec(){return (root,query,b)->{List<Predicate> p=new ArrayList<>();
  if(q!=null&&!q.isBlank()){String text="%"+q.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";p.add(b.or(b.like(b.lower(root.get("name")),text,'\\'),b.like(b.lower(root.get("code")),text,'\\'),b.like(b.lower(root.get("email")),text,'\\'),b.like(b.lower(root.get("phone")),text,'\\')));}
  if(segmentId!=null)p.add(b.equal(root.get("segment").get("id"),segmentId));if(status!=null)p.add(b.equal(root.get("status"),status));if(marketingConsent!=null)p.add(b.equal(root.get("marketingConsent"),marketingConsent));if(archived!=null)p.add(b.equal(root.get("archived"),archived));if(tag!=null&&!tag.isBlank())p.add(b.isMember(tag,root.get("tags")));
  Path<BigDecimal> value=root.get("totalPurchases");if(tier!=null)switch(tier){case BASE->p.add(b.lessThan(value,new BigDecimal("10000")));case SILVER->{p.add(b.greaterThanOrEqualTo(value,new BigDecimal("10000")));p.add(b.lessThan(value,new BigDecimal("50000")));}case GOLD->p.add(b.greaterThanOrEqualTo(value,new BigDecimal("50000")));}
  if(amount!=null)p.add(switch(amountOp==null?"eq":amountOp){case "eq"->b.equal(value,amount);case "gt"->b.greaterThan(value,amount);case "gte"->b.greaterThanOrEqualTo(value,amount);case "lt"->b.lessThan(value,amount);case "lte"->b.lessThanOrEqualTo(value,amount);default->throw new Problem(400,"Оператор: eq, gt, gte, lt, lte");});return b.and(p.toArray(Predicate[]::new));};}
 static Pageable page(int page,int size,String sort,Set<String> fields){if(page<0||size<1||size>100)throw new Problem(400,"Размер страницы 1–100, номер от 0");String[] parts=sort.split(",");if(!fields.contains(parts[0])||parts.length>2||(parts.length==2&&!Set.of("asc","desc").contains(parts[1])))throw new Problem(400,"Недопустимая сортировка");return PageRequest.of(page,size,Sort.by(parts.length==2&&parts[1].equals("desc")?Sort.Direction.DESC:Sort.Direction.ASC,parts[0]).and(Sort.by("id")));}
 static Pageable page(int page,int size,String sort){return page(page,size,sort,Set.of("name","code","totalPurchases","createdAt","updatedAt","status"));}
}
