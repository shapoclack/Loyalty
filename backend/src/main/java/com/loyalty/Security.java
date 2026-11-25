package com.loyalty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.Models.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.*;
import java.time.*;
@Configuration class Security {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(12);}
 @Bean JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret){return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));}
 static SecretKeySpec key(String s){byte[] b=Base64.getDecoder().decode(s);if(b.length<64)throw new IllegalArgumentException("JWT secret requires 64 random bytes");return new SecretKeySpec(b,"HmacSHA512");}
 @Bean JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret,@Value("${app.issuer}") String issuer){var decoder=NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS512).build();decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));return decoder;}
 @Bean SecurityFilterChain chain(HttpSecurity h,JwtDecoder decoder,Users users,ObjectMapper json,@Value("${app.origins}") String origins)throws Exception{
  var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.asList(origins.split(",")));cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Authorization","Content-Type","X-Correlation-ID","X-Refresh-Request"));cors.setExposedHeaders(List.of("X-Correlation-ID","Content-Disposition"));cors.setAllowCredentials(true);var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);
  h.cors(c->c.configurationSource(source)).csrf(c->c.disable()).sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(c->c.requestMatchers("/","/index.html","/app.js","/styles.css","/api/auth/login","/api/auth/refresh","/api/auth/logout","/actuator/health").permitAll().requestMatchers("/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
   .exceptionHandling(c->c.authenticationEntryPoint((r,s,e)->error(json,r,s,401,"Требуется авторизация")).accessDeniedHandler((r,s,e)->error(json,r,s,403,"Недостаточно прав")))
   .addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException{
     String cid=r.getHeader("X-Correlation-ID");if(cid==null||!cid.matches("[a-zA-Z0-9-]{1,80}"))cid=UUID.randomUUID().toString();r.setAttribute("correlationId",cid);s.setHeader("X-Correlation-ID",cid);
     String auth=r.getHeader("Authorization");
     boolean docs=r.getRequestURI().startsWith("/swagger-ui")||r.getRequestURI().startsWith("/v3/api-docs");
     if(auth==null&&docs&&r.getCookies()!=null)for(Cookie cookie:r.getCookies())if(cookie.getName().equals("loyalty_docs"))auth="Bearer "+cookie.getValue();
     if(auth!=null&&auth.startsWith("Bearer ")){try{Jwt jwt=decoder.decode(auth.substring(7));if("docs".equals(jwt.getClaimAsString("scope"))&&!docs)throw new IllegalArgumentException();User u=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow();if(!u.active || !Objects.equals(jwt.getClaimAsString("uv"),Long.toString(u.version)))throw new IllegalArgumentException();var token=new UsernamePasswordAuthenticationToken(u,null,u.roles.stream().map(x->new SimpleGrantedAuthority("ROLE_"+x)).toList());SecurityContextHolder.getContext().setAuthentication(token);
       if(u.mustChangePassword&&!Set.of("/api/auth/me","/api/auth/password","/api/auth/logout").contains(r.getRequestURI())){error(json,r,s,403,"Необходимо сменить временный пароль");return;}
     }catch(Exception e){error(json,r,s,401,"Токен недействителен или истёк");return;}}
     // Cookie refresh requires an explicit custom header; foreign origins cannot submit it without CORS permission.
     if(Set.of("/api/auth/refresh","/api/auth/logout").contains(r.getRequestURI()) && !"1".equals(r.getHeader("X-Refresh-Request"))){error(json,r,s,403,"Необходим X-Refresh-Request: 1");return;}
     chain.doFilter(r,s);
   }},UsernamePasswordAuthenticationFilter.class);
  return h.build();
 }
 static void error(ObjectMapper j,HttpServletRequest r,HttpServletResponse s,int code,String msg)throws IOException{s.setStatus(code);s.setContentType("application/json;charset=UTF-8");j.writeValue(s.getOutputStream(),Errors.body(code,msg,r,Map.of()));}
}
