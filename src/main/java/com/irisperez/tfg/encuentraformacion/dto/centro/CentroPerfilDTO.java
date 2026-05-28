package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.dto.faq.FaqCentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
public class CentroPerfilDTO {
    @JsonIgnore
    private Long id;
    private UUID uuid;
    private String nombreComercial;
    private String descripcion;
    private String direccion;
    private String localidad;
    private String provincia;
    private TipoCentro tipo;
    private String telefono;
    private String email;
    private String paginaWeb;
    private Boolean verificado;

    // Valoración global del centro (media de todas sus formaciones)
    private Double valoracionMedia;
    private Integer totalValoraciones;

    // Formaciones activas del centro
    private List<FormacionResumenDTO> formaciones;

    // FAQs ordenadas por campo orden ASC
    private List<FaqCentroDTO> faqs;
}
