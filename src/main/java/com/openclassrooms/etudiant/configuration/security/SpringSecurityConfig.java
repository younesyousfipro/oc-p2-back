package com.openclassrooms.etudiant.configuration.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Autowired
    private CustomUserDetailService customUserDetailService;

    // task2 - JWT implementation - secret de signature, injecte depuis application.yml
    // (qui le lit lui-meme dans .env).
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // task2 - JWT implementation - beans de signature et de lecture des tokens.
    //
    // Meme logique que le PasswordEncoder ci-dessus : la config declare la brique
    // cryptographique, les services s'en servent sans connaitre l'implementation.
    //
    // JwtEncoder : utilise par JwtService pour signer les tokens emis.
    // JwtDecoder : lira et validera les tokens entrants. Inutilise a l'etape 2, il est
    //   declare des maintenant car c'est lui qui rendra la protection du CRUD triviale
    //   a l'etape 4 (une seule ligne .oauth2ResourceServer(...) dans la filter chain).
    //
    // La cle est symetrique (HMAC-SHA256) : la meme sert a signer et a verifier.

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(authorize -> authorize
                        // No auth needed on :
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        // Others protected routes will be added here.
                        // task4 - CRUD etudiants : reserve aux agents authentifies.
                        // Redondant avec anyRequest() juste en dessous, mais explicite : la
                        // regle se lit dans la config au lieu de se deduire d'un fourre-tout.
                        .requestMatchers("/api/students/**").authenticated()
                        .anyRequest().authenticated()
                )
                // task4 - validation des tokens entrants.
                //
                // .anyRequest().authenticated() bloquait deja /api/students, mais aucun
                // mecanisme ne permettait de s'authentifier : toute requete repondait 401
                // sans recours. Cette ligne branche le BearerTokenAuthenticationFilter de
                // Spring, qui lit l'en-tete Authorization, verifie la signature et
                // l'expiration du JWT via le JwtDecoder declare plus haut, puis alimente le
                // contexte de securite.
                //
                // C'est le filtre qui rejette : StudentController ignore jusqu'a
                // l'existence de l'authentification.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(
                        (request, response, exception) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                        }));
        return http.build();
    }

}
