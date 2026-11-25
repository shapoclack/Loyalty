package com.loyalty;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.util.*;
@RestController class AnalyticsController {
 final EntityManager em; AnalyticsController(EntityManager e){em=e;}
 @GetMapping("/api/analytics/summary") @Transactional(readOnly=true) Map<String,Object> summary(){Map<String,Object> result=new LinkedHashMap<>();result.put("total",count("select count(c) from Customer c"));result.put("active",count("select count(c) from Customer c where c.archived=false"));result.put("archived",count("select count(c) from Customer c where c.archived=true"));result.put("marketingConsent",count("select count(c) from Customer c where c.marketingConsent=true"));Object average=em.createQuery("select avg(c.totalPurchases) from Customer c").getSingleResult();result.put("averagePurchases",average==null?0:average);result.put("bySegment",group("select c.segment.name,count(c) from Customer c group by c.segment.name"));result.put("byStatus",group("select c.status,count(c) from Customer c group by c.status"));result.put("byTier",group("select case when c.totalPurchases >= 50000 then 'GOLD' when c.totalPurchases >= 10000 then 'SILVER' else 'BASE' end,count(c) from Customer c group by case when c.totalPurchases >= 50000 then 'GOLD' when c.totalPurchases >= 10000 then 'SILVER' else 'BASE' end"));return result;}
 long count(String fixedQuery){return em.createQuery(fixedQuery,Long.class).getSingleResult();}
 Map<String,Long> group(String fixedQuery){Map<String,Long> out=new LinkedHashMap<>();em.createQuery(fixedQuery,Object[].class).getResultList().forEach(row->out.put(row[0].toString(),((Number)row[1]).longValue()));return out;}
}
