package com.openclassrooms.etudiant.mapper;

import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.entities.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

// task4 - mapper Student, sur le modele de UserDtoMapper.
//
// unmappedTargetPolicy = ERROR : tout champ de la cible qui n'est ni mappe ni
// explicitement ignore casse la compilation. Filet de securite pour le jour ou un
// champ sera ajoute a Student.
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentDtoMapper {

    // Creation : id genere par la base, timestamps par Hibernate, rien ne vient du client.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Student toEntity(StudentRequestDTO studentRequestDTO);

    StudentResponseDTO toDto(Student student);

    List<StudentResponseDTO> toDtoList(List<Student> students);

    // Modification : ecrase les champs metier d'un Student existant, en preservant son
    // id et ses timestamps. @MappingTarget designe l'objet a modifier plutot qu'a creer.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(StudentRequestDTO studentRequestDTO, @MappingTarget Student student);
}
