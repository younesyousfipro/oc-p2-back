package com.openclassrooms.etudiant.dto;

import lombok.Data;

// task4 - DTO de SORTIE, renvoye par les cinq routes.
//
// Porte l'id, contrairement au DTO d'entree : le client en a besoin pour construire
// les URLs de detail, modification et suppression.
//
// createdAt / updatedAt ne sont volontairement pas exposes : aucune des cinq routes
// n'en a besoin. Les ajouter ici serait deux lignes si le besoin apparait.
@Data
public class StudentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;

}
