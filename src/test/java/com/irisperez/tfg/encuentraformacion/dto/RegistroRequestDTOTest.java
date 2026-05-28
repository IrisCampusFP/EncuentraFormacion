package com.irisperez.tfg.encuentraformacion.dto;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegistroRequestDTO createValidDTO() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Pérez");
        dto.setUsername("juanp");
        dto.setEmail("juan@test.com");
        dto.setPassword("Abcde123!");
        return dto;
    }

    @Test
    void debeRechazarPasswordSinMayuscula() {
        RegistroRequestDTO dto = createValidDTO();
        dto.setPassword("abcde123!");
        
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);
        
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void debeRechazarPasswordSinCaracterEspecial() {
        RegistroRequestDTO dto = createValidDTO();
        dto.setPassword("Abcde123");
        
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);
        
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void debeAceptarPasswordCompleta() {
        RegistroRequestDTO dto = createValidDTO();
        dto.setPassword("Abcde123!");
        
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);
        
        assertThat(violations).isEmpty();
    }
}
