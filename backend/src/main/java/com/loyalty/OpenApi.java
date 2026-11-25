package com.loyalty;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
@Configuration class OpenApi {
 @Bean OpenAPI contract(){return new OpenAPI().info(new Info().title("Loyalty API").version("1.0").description("Клиенты программы лояльности. Bearer JWT для web и mobile. Пагинация от нуля; version обязателен при изменении." )).components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))).addSecurityItem(new SecurityRequirement().addList("bearerAuth"));}
}
