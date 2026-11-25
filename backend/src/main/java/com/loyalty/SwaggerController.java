package com.loyalty;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import com.loyalty.Models.User;

/** A read-only documentation cookie; never accepted for business API requests. */
@RestController class SwaggerController {
 final JwtEncoder encoder;
 @Value("${app.issuer}") String issuer;
 @Value("${app.cookie-secure}") boolean secure;
 SwaggerController(JwtEncoder encoder){this.encoder=encoder;}
 @PostMapping("/api/admin/swagger-session") void open(@AuthenticationPrincipal User user,HttpServletResponse response){
  var claims=JwtClaimsSet.builder().issuer(issuer).subject(user.id.toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).claim("uv",Long.toString(user.version)).claim("scope","docs").build();
  String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(),claims)).getTokenValue();
  response.addHeader("Set-Cookie",ResponseCookie.from("loyalty_docs",token).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(Duration.ofMinutes(5)).build().toString());
 }
}
