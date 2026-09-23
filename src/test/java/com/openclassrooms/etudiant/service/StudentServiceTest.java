package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.mapper.StudentDtoMapperImpl;
import com.openclassrooms.etudiant.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// task-tests - tests unitaires de StudentService, sur le modele de UserServiceTest.
//
// Seuls les cas nominaux sont couverts : la consigne du projet interdit de tester
// les cas d'erreur et de verifier les effets de bord.
@ExtendWith(SpringExtension.class)
public class StudentServiceTest {

    private static final Long ID = 1L;
    private static final String FIRST_NAME = "Jean";
    private static final String LAST_NAME = "Dupont";
    private static final String EMAIL = "jean.dupont@test.fr";
    private static final String NEW_FIRST_NAME = "Jeanne";
    private static final String NEW_LAST_NAME = "Durand";
    private static final String NEW_EMAIL = "jeanne.durand@test.fr";

    @Mock
    private StudentRepository studentRepository;

    // Le mapper n'est pas mocke mais instancie pour de vrai : il est deterministe et
    // sans acces disque ou reseau, donc le doubler n'isolerait de rien. Les assertions
    // portent ainsi sur les valeurs reellement mappees, et non sur un objet que le
    // test se serait lui-meme fait renvoyer.
    @Spy
    private StudentDtoMapper studentDtoMapper = new StudentDtoMapperImpl();

    @InjectMocks
    private StudentService studentService;

    // Creation d'un etudiant dont l'email n'existe pas encore en base.
    @Test
    public void test_create_student() {
        // GIVEN
        StudentRequestDTO request = requestDTO(FIRST_NAME, LAST_NAME, EMAIL);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        // save() est ce qui attribue l'id : toEntity() produit une entite sans id.
        when(studentRepository.save(any(Student.class)))
                .thenReturn(student(ID, FIRST_NAME, LAST_NAME, EMAIL));

        // WHEN
        StudentResponseDTO result = studentService.create(request);

        // THEN
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(LAST_NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    // Liste complete des etudiants, convertie en DTO.
    @Test
    public void test_find_all_students() {
        // GIVEN
        when(studentRepository.findAll()).thenReturn(List.of(
                student(ID, FIRST_NAME, LAST_NAME, EMAIL),
                student(2L, NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL)));

        // WHEN
        List<StudentResponseDTO> result = studentService.findAll();

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo(EMAIL);
        assertThat(result.get(1).getFirstName()).isEqualTo(NEW_FIRST_NAME);
    }

    // Detail d'un etudiant existant.
    @Test
    public void test_find_student_by_id() {
        // GIVEN
        when(studentRepository.findById(ID))
                .thenReturn(Optional.of(student(ID, FIRST_NAME, LAST_NAME, EMAIL)));

        // WHEN
        StudentResponseDTO result = studentService.findById(ID);

        // THEN
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    // Modification d'un etudiant existant : les champs metier changent, l'id est conserve.
    @Test
    public void test_update_student() {
        // GIVEN
        Student existing = student(ID, FIRST_NAME, LAST_NAME, EMAIL);
        when(studentRepository.findById(ID)).thenReturn(Optional.of(existing));
        // updateEntityFromDto ecrase les champs de l'instance existante : au moment ou
        // le service appelle save(), cette meme instance porte deja les nouvelles valeurs.
        when(studentRepository.save(any(Student.class))).thenReturn(existing);

        // WHEN
        StudentResponseDTO result = studentService.update(
                ID, requestDTO(NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL));

        // THEN
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getFirstName()).isEqualTo(NEW_FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(NEW_LAST_NAME);
        assertThat(result.getEmail()).isEqualTo(NEW_EMAIL);
    }

    // Suppression d'un etudiant existant. delete() ne retourne rien : le seul
    // comportement observable est l'entite transmise au repository.
    @Test
    public void test_delete_student() {
        // GIVEN
        Student existing = student(ID, FIRST_NAME, LAST_NAME, EMAIL);
        when(studentRepository.findById(ID)).thenReturn(Optional.of(existing));

        // WHEN
        studentService.delete(ID);

        // THEN
        verify(studentRepository).delete(existing);
    }

    private Student student(Long id, String firstName, String lastName, String email) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        return student;
    }

    private StudentRequestDTO requestDTO(String firstName, String lastName, String email) {
        StudentRequestDTO studentRequestDTO = new StudentRequestDTO();
        studentRequestDTO.setFirstName(firstName);
        studentRequestDTO.setLastName(lastName);
        studentRequestDTO.setEmail(email);
        return studentRequestDTO;
    }
}
