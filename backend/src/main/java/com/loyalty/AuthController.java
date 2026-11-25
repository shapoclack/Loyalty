package com.loyalty;
import com.loyalty.Models.User;
import com.loyalty.Dtos.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import java.time.Duration;
@RestController @RequestMapping("/api/auth")
class AuthController {
 final AuthService auth; @Value("${app.cookie-secure}") boolean secure; @Value("${app.refresh-days}") long days;
 AuthController(AuthService a){auth=a;}
 @PostMapping("/login") Object login(@Valid @RequestBody Login p,HttpServletRequest r,HttpServletResponse s){return deliver(auth.login(p,r.getRemoteAddr()),p.mobile(),s);}
 @PostMapping("/refresh") Object refresh(@RequestBody(required=false) Refresh p,HttpServletRequest r,HttpServletResponse s){boolean mobile=p!=null&&p.refreshToken()!=null;return deliver(auth.refresh(token(p,r)),mobile,s);}
 @PostMapping("/logout") void logout(@RequestBody(required=false) Refresh p,HttpServletRequest r,HttpServletResponse s){auth.logout(token(p,r));cookie(s,"",0);}
 @GetMapping("/me") UserView me(@AuthenticationPrincipal User u){return UserView.of(u);}
 @PostMapping("/password") void password(@AuthenticationPrincipal User u,@Valid @RequestBody Password p,HttpServletResponse s){auth.password(u,p);cookie(s,"",0);}
 Object deliver(AuthService.Tokens t,boolean mobile,HttpServletResponse s){if(!mobile)cookie(s,t.refreshToken(),days*86400);return new AuthService.Tokens(t.accessToken(),mobile?t.refreshToken():null,t.expiresIn(),t.user());}
 void cookie(HttpServletResponse s,String token,long seconds){s.addHeader("Set-Cookie",ResponseCookie.from("loyalty_refresh",token).httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(Duration.ofSeconds(seconds)).build().toString());}
 String token(Refresh p,HttpServletRequest r){if(p!=null&&p.refreshToken()!=null)return p.refreshToken();if(r.getCookies()!=null)for(Cookie c:r.getCookies())if(c.getName().equals("loyalty_refresh"))return c.getValue();return null;}
}
