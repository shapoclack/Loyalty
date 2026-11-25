package com.loyalty;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.loyalty.Models.*;
import java.util.UUID;
@Service class AuditService {
 final Audits audits; AuditService(Audits a){audits=a;}
 void add(String event,String type,UUID id,String description){var auth=SecurityContextHolder.getContext().getAuthentication();User u=auth!=null&&auth.getPrincipal() instanceof User x?x:null;add(u,event,type,id,description);}
 void add(User user,String event,String type,UUID id,String description){Audit a=new Audit();if(user!=null){a.userId=user.id;a.username=user.login;}a.event=event;a.entityType=type;a.entityId=id;a.description=description;var attrs=RequestContextHolder.getRequestAttributes();a.correlationId=attrs instanceof ServletRequestAttributes s?(String)s.getRequest().getAttribute("correlationId"):"system";audits.save(a);}
}
