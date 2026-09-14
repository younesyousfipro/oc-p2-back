package com.openclassrooms.etudiant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// task2 - JWT implementation - DTO de reponse de /api/login (nouveau fichier).
//
// Le controller retournait le token en String brute (text/plain). Un DTO respecte
// l'exigence de la consigne (les entites et valeurs nues ne transitent pas telles
// quelles), se consomme directement en JSON cote Angular a l'etape 3, et laisse la
// porte ouverte a un champ supplementaire (expiresIn...) sans casser le contrat.
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;

}
