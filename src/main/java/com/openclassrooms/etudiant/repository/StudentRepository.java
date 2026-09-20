package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// task4 - JpaRepository fournit deja save, findAll, findById et deleteById : les cinq
// operations du CRUD n'ont besoin d'aucune requete ecrite a la main.
// findByEmail est derive du nom de la methode par Spring Data, comme findByLogin dans
// UserRepository. Il sert a refuser un doublon a la creation.
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
}
