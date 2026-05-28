package com.irisperez.tfg.encuentraformacion.service.faq;

import com.irisperez.tfg.encuentraformacion.dto.faq.CrearFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.EditarFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.FaqGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.faq.FaqGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.FaqCentro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FaqCentroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GestorFaqService {

    private final FaqCentroRepository faqRepository;
    private final CentroRepository centroRepository;
    private final FaqGestorMapper mapper;

    public List<FaqGestorDTO> getFaqs(Long usuarioId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        return faqRepository.findByCentroIdOrderByOrdenAsc(centro.getId())
            .stream().map(mapper::toDTO).toList();
    }

    @Transactional
    public FaqGestorDTO crear(Long usuarioId, CrearFaqDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        int siguienteOrden = faqRepository.findByCentroIdOrderByOrdenAsc(centro.getId()).size() + 1;

        FaqCentro faq = new FaqCentro();
        faq.setCentro(centro);
        faq.setPregunta(dto.getPregunta());
        faq.setRespuesta(dto.getRespuesta());
        faq.setOrden(siguienteOrden);
        faq.setActiva(true);
        return mapper.toDTO(faqRepository.save(faq));
    }

    @Transactional
    public FaqGestorDTO editar(Long usuarioId, Long faqId, EditarFaqDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        FaqCentro faq = faqRepository.findByIdAndCentroId(faqId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ no encontrada"));

        if (dto.getPregunta() != null) faq.setPregunta(dto.getPregunta());
        if (dto.getRespuesta() != null) faq.setRespuesta(dto.getRespuesta());
        if (dto.getOrden() != null) faq.setOrden(dto.getOrden());
        return mapper.toDTO(faqRepository.save(faq));
    }

    @Transactional
    public void eliminar(Long usuarioId, Long faqId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        FaqCentro faq = faqRepository.findByIdAndCentroId(faqId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ no encontrada"));
        faqRepository.delete(faq);
    }

    private Centro obtenerCentroDelGestor(Long usuarioId) {
        return centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gestionas ningún centro"));
    }
}
