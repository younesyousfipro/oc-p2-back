package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public String login(String login, String password) {
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        Optional<User> user = userRepository.findByLogin(login);
        // task2 - Debug 2
        //
        // if (user.isPresent() && passwordEncoder.matches(password, password)) {
        //
        // matches() attend le mot de passe tape en 1er argument et le hash stocke en 2e.
        // Le starter passait deux fois le clair : la comparaison renvoyait toujours false,
        // quels que soient les identifiants.

        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {

            // task2 - Debug 3
            //
            // UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            //         .username(login).build();
            // return jwtService.generateToken(userDetails);
            //
            // Le builder recevait un username mais aucun password. Son constructeur exige
            // un mot de passe non nul -> IllegalArgumentException "Cannot pass null or
            // empty values to constructor".
            //
            // Inutile de surcroit : l'entite User du projet implemente deja UserDetails
            // (voir User.java), l'objet etait donc deja disponible dans user.get().
            // C'est ce que fait CustomUserDetailService, qui retourne l'entite directement.

            return jwtService.generateToken(user.get());
        } else {
            // task3 - 401 au lieu de 400
            //
            // throw new IllegalArgumentException("Invalid credentials");
            //
            // Permet au front (etape 3) de distinguer "identifiants refuses" (401, message
            // a afficher a l'utilisateur) d'une vraie panne (400/500, message technique).
            throw new BadCredentialsException("Invalid credentials");
        }
    }


}
