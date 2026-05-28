package com.irisperez.tfg.encuentraformacion.service.faq;

import com.irisperez.tfg.encuentraformacion.dto.faq.CrearFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.EditarFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.FaqGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.faq.FaqGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.FaqCentro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FaqCentroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorFaqServiceTest {

    @Mock
    private FaqCentroRepository faqRepository;
    @Mock
    private CentroRepository centroRepository;
    @Mock
    private FaqGestorMapper mapper;

    @InjectMocks
    private GestorFaqService gestorFaqService;

    private Centro centro;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);
    }

    @Test
    void getFaqs_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(faqRepository.findByCentroIdOrderByOrdenAsc(1L)).thenReturn(List.of(new FaqCentro()));
        when(mapper.toDTO(any())).thenReturn(new FaqGestorDTO());

        List<FaqGestorDTO> result = gestorFaqService.getFaqs(10L);

        assertEquals(1, result.size());
    }

    @Test
    void crear_ok() {
        CrearFaqDTO dto = new CrearFaqDTO();
        dto.setPregunta("Q");
        dto.setRespuesta("A");

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(faqRepository.findByCentroIdOrderByOrdenAsc(1L)).thenReturn(List.of(new FaqCentro())); // size = 1
        when(faqRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(new FaqGestorDTO());

        gestorFaqService.crear(10L, dto);

        verify(faqRepository).save(argThat(faq -> faq.getOrden() == 2 && faq.getPregunta().equals("Q")));
    }

    @Test
    void editar_ok() {
        EditarFaqDTO dto = new EditarFaqDTO();
        dto.setPregunta("Q2");

        FaqCentro faq = new FaqCentro();
        faq.setId(100L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(faqRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(faq));
        when(faqRepository.save(faq)).thenReturn(faq);

        gestorFaqService.editar(10L, 100L, dto);

        assertEquals("Q2", faq.getPregunta());
    }

    @Test
    void editar_faqDeOtroCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(faqRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorFaqService.editar(10L, 100L, new EditarFaqDTO()));
    }

    @Test
    void eliminar_ok() {
        FaqCentro faq = new FaqCentro();
        faq.setId(100L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(faqRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(faq));

        gestorFaqService.eliminar(10L, 100L);

        verify(faqRepository).delete(faq);
    }
}
