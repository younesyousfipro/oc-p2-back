package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// task-tests - tests d'integration des cinq routes CRUD, sur le modele de
// UserControllerTest : contexte Spring complet et vraie base MySQL en conteneur.
//
// Difference avec le modele : ces routes exigent un Bearer token. Il est produit dans
// le @BeforeEach par le vrai JwtService, donc signe avec la cle de l'application et
// valide par le vrai filtre de securite a chaque requete.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class StudentControllerTest {

    private static final String URL = "/api/students";
    private static final String LOGIN = "agent";
    private static final String FIRST_NAME = "Jean";
    private static final String LAST_NAME = "Dupont";
    private static final String EMAIL = "jean.dupont@test.fr";
    private static final String NEW_FIRST_NAME = "Jeanne";
    private static final String NEW_LAST_NAME = "Durand";
    private static final String NEW_EMAIL = "jeanne.durand@test.fr";

    @Container
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.0");

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private String bearerToken;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    public void beforeEach() {
        // Le token porte le login dans son claim "sub". Aucun utilisateur n'a besoin
        // d'exister en base : le filtre verifie la signature du token, il ne relit pas
        // la table user.
        User agent = new User();
        agent.setLogin(LOGIN);
        bearerToken = "Bearer " + jwtService.generateToken(agent);
    }

    @AfterEach
    public void afterEach() {
        studentRepository.deleteAll();
    }

    // Creation d'un etudiant.
    @Test
    public void createStudent() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .content(objectMapper.writeValueAsString(requestDTO(FIRST_NAME, LAST_NAME, EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL));
    }

    // Liste des etudiants.
    @Test
    public void findAllStudents() throws Exception {
        // GIVEN
        studentRepository.save(student(FIRST_NAME, LAST_NAME, EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value(EMAIL));
    }

    // Detail d'un etudiant.
    @Test
    public void findStudentById() throws Exception {
        // GIVEN
        Student saved = studentRepository.save(student(FIRST_NAME, LAST_NAME, EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isOk())
                // intValue() : JsonPath lit un petit entier JSON comme un Integer, que
                // l'on ne peut pas comparer directement au Long de l'entite.
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId().intValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL));
    }

    // Modification d'un etudiant.
    @Test
    public void updateStudent() throws Exception {
        // GIVEN
        Student saved = studentRepository.save(student(FIRST_NAME, LAST_NAME, EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .content(objectMapper.writeValueAsString(
                                requestDTO(NEW_FIRST_NAME, NEW_LAST_NAME, NEW_EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId().intValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(NEW_FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(NEW_EMAIL));
    }

    // Suppression d'un etudiant.
    @Test
    public void deleteStudent() throws Exception {
        // GIVEN
        Student saved = studentRepository.save(student(FIRST_NAME, LAST_NAME, EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    // Sans Bearer token, la route est refusee. C'est le critere central du projet :
    // toutes les routes CRUD sont protegees.
    @Test
    public void findAllStudentsWithoutToken() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private Student student(String firstName, String lastName, String email) {
        Student student = new Student();
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
