package com.loyalty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.loyalty.Models.*;
@Component class Bootstrap implements CommandLineRunner {
 final Users users;final Segments segments;final PasswordEncoder encoder;
 @Value("${app.admin-login}") String login;@Value("${app.admin-name}") String name;@Value("${app.admin-password}") String password;
 Bootstrap(Users u,Segments s,PasswordEncoder p){users=u;segments=s;encoder=p;}
 @Transactional public void run(String... args){if(users.count()==0){AuthService.policy(password);User u=new User();u.login=login.toLowerCase(java.util.Locale.ROOT);u.name=name;u.passwordHash=encoder.encode(password);u.roles.add(Role.ADMIN);users.save(u);}if(segments.count()==0){Segment s=new Segment();s.name="Основной";segments.save(s);}}
}
