package com.loyalty;

import com.fasterxml.jackson.databind.*;
import com.loyalty.Models.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {
 @Autowired Attempts attempts; @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired Users users; @Autowired Segments segments;
 static String adminToken; static String refreshToken; static UUID customerId; static long version;
 String body(Object v)throws Exception{return json.writeValueAsString(v);}
 JsonNode read(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
 JsonNode login(String login,String password)throws Exception{return read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("login",login,"password",password,"mobile",true)))).andExpect(status().isOk()).andReturn());}
 Map<String,Object> customer(String code,String name,int amount){return new HashMap<>(Map.of("code",code,"name",name,"segmentId",segments.findAll().getFirst().id,"status","ACTIVE","totalPurchases",amount,"marketingConsent",true,"tags",List.of("vip"),"version",0));}
 @Test @Order(1) void contextAndMandatoryPasswordChange()throws Exception{
  mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.correlationId").exists());
  var t=login("admin@test.local","TemporaryPassword123");String old=t.get("accessToken").asText();
  mvc.perform(get("/api/customers").header("Authorization","Bearer "+old)).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/password").header("Authorization","Bearer "+old).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("currentPassword","TemporaryPassword123","newPassword","PermanentPassword456")))).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
  t=login("admin@test.local","PermanentPassword456");adminToken=t.get("accessToken").asText();refreshToken=t.get("refreshToken").asText();
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.mustChangePassword").value(false));
  assertThat(users.findByLogin("admin@test.local").orElseThrow().passwordHash).startsWith("$2");
 }
 @Test @Order(2) void createFilterAndOptimisticConflict()throws Exception{
  var c=read(mvc.perform(post("/api/customers").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(customer("TEST-001","Анна Тест",50000)))).andExpect(status().isOk()).andExpect(jsonPath("$.tier").value("GOLD")).andReturn());customerId=UUID.fromString(c.get("id").asText());version=c.get("version").asLong();
  mvc.perform(get("/api/customers?q=Анна&tier=GOLD&amount=40000&amountOp=gt&tag=vip&marketingConsent=true&size=10").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/customers?q=%27%20OR%201=1").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/api/customers?size=101").header("Authorization","Bearer "+adminToken)).andExpect(status().isBadRequest());
  var p=customer("TEST-001","Новое имя",10000);p.put("version",version);
  c=read(mvc.perform(put("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isOk()).andReturn());version=c.get("version").asLong();
  mvc.perform(put("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isConflict());
 }
 @Test @Order(3) void notesBulkArchiveAndAnalytics()throws Exception{
  mvc.perform(post("/api/customers/"+customerId+"/notes").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("text","Важный клиент","pinned",true,"version",0)))).andExpect(status().isOk()).andExpect(jsonPath("$.authorName").value("Test Admin"));
  UUID missing=UUID.randomUUID();
  mvc.perform(post("/api/customers/bulk").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("ids",List.of(customerId,missing),"action","ARCHIVE","versions",Map.of(customerId,version,missing,0))))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.success == true)]").isNotEmpty()).andExpect(jsonPath("$[?(@.success == false)]").isNotEmpty());
  mvc.perform(get("/api/analytics/summary").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(1)).andExpect(jsonPath("$.byTier.SILVER").value(1));
  var c=read(mvc.perform(get("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken)).andReturn());version=c.get("version").asLong();
  mvc.perform(post("/api/customers/"+customerId+"/restore?version="+version).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
 }
 @Test @Order(4) void importPreviewApplyAndReplay()throws Exception{
  String csv=String.join(",",CsvService.HEADERS)+"\nIMPORT-1,Imported,a@example.com,,,"+segments.findAll().getFirst().id+",ACTIVE,10,true,new\n";
  var preview=read(mvc.perform(multipart("/api/customers/import/preview").file(new MockMultipartFile("file","customers.csv","text/csv",csv.getBytes(java.nio.charset.StandardCharsets.UTF_8))).param("mode","NEW_ONLY").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.validRows").value(1)).andExpect(jsonPath("$.invalidRows").value(0)).andReturn());
  var p=Map.of("previewId",preview.get("previewId").asText());
  mvc.perform(post("/api/customers/import/apply").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(1));
  mvc.perform(post("/api/customers/import/apply").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isConflict());
  mvc.perform(get("/api/customers/export?q=Imported").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("IMPORT-1")));
 }
 @Test @Order(5) void viewerCannotWriteAndDisabledUserCannotLogin()throws Exception{
  var p=Map.of("login","viewer","name","Reader","roles",List.of("VIEWER"),"temporaryPassword","ViewerPassword123");
  mvc.perform(post("/api/admin/users").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist());
  var t=login("viewer","ViewerPassword123");String access=t.get("accessToken").asText();
  mvc.perform(post("/api/auth/password").header("Authorization","Bearer "+access).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("currentPassword","ViewerPassword123","newPassword","ViewerPermanent456")))).andExpect(status().isOk());
  access=login("viewer","ViewerPermanent456").get("accessToken").asText();
  mvc.perform(get("/api/customers").header("Authorization","Bearer "+access)).andExpect(status().isOk());
  mvc.perform(post("/api/customers").header("Authorization","Bearer "+access).contentType(MediaType.APPLICATION_JSON).content(body(customer("NO","Forbidden",0)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/audit").header("Authorization","Bearer "+access)).andExpect(status().isForbidden());
  var u=users.findByLogin("viewer").orElseThrow();
  mvc.perform(put("/api/admin/users/"+u.id).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("login",u.login,"name",u.name,"roles",List.of("VIEWER"),"active",false,"version",u.version)))).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("login","viewer","password","ViewerPermanent456")))).andExpect(status().isUnauthorized());
 }
 @Test @Order(6) void refreshRotationLogoutAndCsrfHeader()throws Exception{
  mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken",refreshToken)))).andExpect(status().isForbidden());
  var t=read(mvc.perform(post("/api/auth/refresh").header("X-Refresh-Request","1").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken",refreshToken)))).andExpect(status().isOk()).andReturn());
  mvc.perform(post("/api/auth/refresh").header("X-Refresh-Request","1").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken",refreshToken)))).andExpect(status().isUnauthorized());
  refreshToken=t.get("refreshToken").asText();
  mvc.perform(post("/api/auth/logout").header("X-Refresh-Request","1").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken",refreshToken)))).andExpect(status().isOk());
  mvc.perform(post("/api/auth/refresh").header("X-Refresh-Request","1").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken",refreshToken)))).andExpect(status().isUnauthorized());
 }
 @Test @Order(7) void throttlesFailedLogins()throws Exception{attempts.deleteAll();for(int i=0;i<5;i++)mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("login","unknown","password","wrong")))).andExpect(status().isUnauthorized());mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("login","unknown","password","wrong")))).andExpect(status().isTooManyRequests());}
 @Test @Order(8) void swaggerRestrictedAndDocumentationTokenCannotCallApi()throws Exception{
  mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
  mvc.perform(get("/v3/api-docs").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/customers']").exists());
  var response=mvc.perform(post("/api/admin/swagger-session").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andReturn().getResponse();
  String cookie=response.getHeader("Set-Cookie");String docsToken=cookie.substring(cookie.indexOf('=')+1,cookie.indexOf(';'));
  mvc.perform(get("/v3/api-docs").cookie(new jakarta.servlet.http.Cookie("loyalty_docs",docsToken))).andExpect(status().isOk());
  mvc.perform(get("/api/customers").header("Authorization","Bearer "+docsToken)).andExpect(status().isUnauthorized());
 }
 @Test @Order(9) void staleCsvAndPermanentDelete()throws Exception{
  var c=read(mvc.perform(get("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken)).andReturn());
  String csv=String.join(",",CsvService.HEADERS)+"\nTEST-001,CSV update,a@example.com,,,"+segments.findAll().getFirst().id+",ACTIVE,10,true,new\n";
  var preview=read(mvc.perform(multipart("/api/customers/import/preview").file(new MockMultipartFile("file","customers.csv","text/csv",csv.getBytes(java.nio.charset.StandardCharsets.UTF_8))).param("mode","UPSERT").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andReturn());
  mvc.perform(delete("/api/customers/"+customerId).param("version",c.get("version").asText()).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
  mvc.perform(post("/api/customers/import/apply").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("previewId",preview.get("previewId").asText())))).andExpect(status().isConflict());
  c=read(mvc.perform(get("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken)).andReturn());
  mvc.perform(delete("/api/customers/"+customerId+"/permanent").param("version",c.get("version").asText()).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
  mvc.perform(get("/api/customers/"+customerId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNotFound());
 }

}
