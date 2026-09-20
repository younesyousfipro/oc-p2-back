package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

// task4 - logique metier du CRUD etudiants.
//
// Le service expose des DTO et non des entites : le controller n'a ainsi jamais a
// manipuler Student, conformement a l'exigence "pas d'entite dans les controllers".
// C'est plus strict que UserController, qui appelle le mapper lui-meme.
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentDtoMapper studentDtoMapper;

    public StudentDTO create(StudentRequestDTO studentRequestDTO) {
        Assert.notNull(studentRequestDTO, "Student must not be null");
        log.info("Creating new student");

        if (studentRepository.findByEmail(studentRequestDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "Student with email " + studentRequestDTO.getEmail() + " already exists");
        }

        Student student = studentDtoMapper.toEntity(studentRequestDTO);
        return studentDtoMapper.toDto(studentRepository.save(student));
    }

    public List<StudentDTO> findAll() {
        return studentDtoMapper.toDtoList(studentRepository.findAll());
    }

    public StudentDTO findById(Long id) {
        return studentDtoMapper.toDto(findEntityById(id));
    }

    public StudentDTO update(Long id, StudentRequestDTO studentRequestDTO) {
        Assert.notNull(studentRequestDTO, "Student must not be null");
        log.info("Updating student {}", id);

        // On recharge l'existant puis on ecrase ses champs metier : l'id et les
        // timestamps de creation sont ainsi preserves.
        Student student = findEntityById(id);
        studentDtoMapper.updateEntityFromDto(studentRequestDTO, student);
        return studentDtoMapper.toDto(studentRepository.save(student));
    }

    public void delete(Long id) {
        log.info("Deleting student {}", id);
        studentRepository.delete(findEntityById(id));
    }

    private Student findEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id " + id));
    }
}
