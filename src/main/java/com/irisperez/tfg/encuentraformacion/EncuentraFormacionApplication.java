package com.irisperez.tfg.encuentraformacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // Serializa las respuestas paginadas con un JSON estable en lugar de la clase interna PageImpl
@EnableScheduling // Habilita el programador de tareas para el mantenimiento de seguridad y limpieza
public class EncuentraFormacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncuentraFormacionApplication.class, args);
    }

}
