package com.openclassrooms.etudiant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// task4 - DTO d'ENTREE, utilise pour la creation et la modification.
//
// Pas d'id : il vient toujours de l'URL (/api/students/{id}), jamais du corps de la
// requete. Le client ne peut donc pas designer l'enregistrement a ecraser.
//
// C'est ici que vit la validation, a l'entree du controller (@Valid), comme pour
// RegisterDTO et LoginRequestDTO.
@Data
public class StudentRequestDTO {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    @Email
    private String email;

}
