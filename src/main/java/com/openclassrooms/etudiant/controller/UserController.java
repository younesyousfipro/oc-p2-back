package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.LoginResponseDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // task2 - Debug 1

    // @PostMapping("/api/login")
    // public ResponseEntity<?> login(LoginRequestDTO loginRequestDTO) {
    //     String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
    //     return ResponseEntity.ok(jwtToken);
    // }
    //
    // Sans @RequestBody, Spring ne deserialise pas le corps JSON : il construit un
    // LoginRequestDTO vide et poursuit sans erreur.
    // @Valid enforce @NotBlank ajoute dans LoginRequestDTO

    // task2 - JWT implementation - reponse encapsulee dans un DTO.
    //
    // return ResponseEntity.ok(jwtToken);
    //
    // Renvoyait le token en String brute (text/plain). Renvoie desormais
    // { "token": "eyJ..." } en JSON.

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(new LoginResponseDTO(jwtToken));
    }


}
