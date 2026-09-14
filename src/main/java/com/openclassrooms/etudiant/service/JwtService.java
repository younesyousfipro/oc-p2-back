package com.openclassrooms.etudiant.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    // task2 - JWT implementation - implementation de generateToken.
    //
    // public String generateToken(UserDetails userDetails) {
    //     return null; // TODO
    // }
    //
    // La methode retournait null : /api/login repondait 200 avec un corps vide, meme
    // apres correction des trois bugs precedents.
    //
    // Le service decide du CONTENU du token (qui, emis quand, valable jusqu'a quand) et
    // delegue la SIGNATURE au JwtEncoder injecte. Meme repartition que UserService, qui
    // decide quand hacher et delegue le hachage au PasswordEncoder.

    private static final long EXPIRATION_HOURS = 1;

    private final JwtEncoder jwtEncoder;

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();

        // "sub" identifie le porteur du token : le login, qui permettra de retrouver
        // l'utilisateur a l'etape 4 quand il faudra valider les tokens entrants.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

}
