package com.openclassrooms.etudiant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {

    // task2 - Debug 1
    //
    // private String login;
    // private String password;
    //
    // Les @NotBlank manquaient au starter (import orphelin).
    // @NotBlank oblige à des valeurs non nulles, non vide, et pas uniquement des espaces.
    // La validation appartient a l'entree du controller (@Valid) qui enforce @NotBlank
    // ============================================

    @NotBlank
    private String login;
    @NotBlank
    private String password;

}
