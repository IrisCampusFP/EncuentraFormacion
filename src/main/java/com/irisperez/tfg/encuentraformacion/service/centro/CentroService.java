package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.usuario.UsuarioMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ProvinciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroService {

    private final CentroRepository centroRepository;
    private final CentroMapper centroMapper;
    private final ProvinciaService provinciaService;
    private final UsuarioMapper usuarioMapper;


    // CREATE

    // Crear centro base
    @Transactional
    public CentroDTO crearCentro(Centro centro) {

        if (centro == null) throw new IllegalArgumentException("Centro nulo");

        comprobarCodigoUnico(centro.getCodigo());
        
        if (centro.getEmail() != null && !centro.getEmail().isBlank()) {
            comprobarEmailUnico(centro.getEmail());
        }

        return centroMapper.toDTO(centroRepository.save(centro));
    }

    // Registrar centro desde formulario o DTO
    @Transactional
    public CentroDTO registrarCentro(RegistroCentroRequestDTO dto) {
        Centro centro = centroMapper.createCentroFromDTO(dto);
        centro.setProvincia(provinciaService.buscarPorNombre(dto.getProvincia()));
        return crearCentro(centro);
    }


    // READ

    // Comprobar si existe código oficial
    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return centroRepository.existsByCodigo(codigo);
    }

    // Obtener todos los centros
    @Transactional(readOnly = true)
    public List<CentroDTO> obtenerCentros() {
        return centroMapper.toDTOList(centroRepository.findAll());
    }

    // Obtener todos los centros verificados
    @Transactional(readOnly = true)
    public List<CentroDTO> obtenerCentrosVerificados() {
        return centroMapper.toDTOList(centroRepository.findByVerificadoTrue());
    }

    @Transactional(readOnly = true)
    public Page<CentroDTO> obtenerCentrosVerificadosPaginados(Pageable pageable) {
        return centroRepository.findByVerificadoTrue(pageable).map(centroMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<CentroDTO> buscarVerificadosConFiltros(Long id, String q, TipoCentro tipo, Boolean tieneGestor, Pageable pageable) {
        String qNorm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : "";
        return centroRepository.buscarVerificadosConFiltros(id, qNorm, tipo, tieneGestor, pageable)
                .map(centroMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<CentroDTO> buscarSinVerificarConFiltros(Long id, String q, TipoCentro tipo, Pageable pageable) {
        String qNorm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : "";
        return centroRepository.buscarSinVerificarConFiltros(id, qNorm, tipo, pageable)
                .map(centroMapper::toDTO);
    }

    // Obtener todos los centros sin verificar
    @Transactional(readOnly = true)
    public List<CentroDTO> obtenerCentrosNoVerificados() {
        return centroMapper.toDTOList(centroRepository.findByVerificadoFalse());
    }

    @Transactional(readOnly = true)
    public Page<CentroDTO> obtenerCentrosNoVerificadosPaginados(Pageable pageable) {
        return centroRepository.findByVerificadoFalse(pageable).map(centroMapper::toDTO);
    }

    // Obtener centro por id
    @Transactional(readOnly = true)
    public Centro obtenerCentroPorId(Long id) {
        return centroRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún centro con id: " + id));
    }

    // Obtener DTO centro por id
    @Transactional(readOnly = true)
    public CentroDTO obtenerCentroDTOPorId(Long id) {
        return centroMapper.toDTO(obtenerCentroPorId(id));
    }

    // Obtener centro por código oficial
    @Transactional(readOnly = true)
    public Centro obtenerCentroPorCodigo(String codigo) {
        return centroRepository.findByCodigo(codigo).orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún centro con el código: " + codigo));
    }

    // Obtener DTO centro por código oficial
    @Transactional(readOnly = true)
    public CentroDTO obtenerCentroDTOPorCodigo(String codigo) {
        return centroMapper.toDTO(obtenerCentroPorCodigo(codigo));
    }


    // UPDATE

    /* Actualizar centro. No se actualiza si:
     * - El centro con los nuevos datos viene vacío
     * - El centro a actualizar no existe en la BD
     */
    @Transactional
    public CentroDTO actualizarCentro(Long id, CentroUpdateDTO centroActualizado) {
        if (centroActualizado == null) throw new IllegalArgumentException("No se han recibido correctamente los nuevos datos.");

        Centro centro = obtenerCentroPorId(id);

        // Solo comprobamos unicidad si el campo ha cambiado
        if (!centroActualizado.getCodigo().equalsIgnoreCase(centro.getCodigo())) {
            comprobarCodigoUnico(centroActualizado.getCodigo());
        }

        if (centroActualizado.getEmail() != null && !centroActualizado.getEmail().isBlank()) {
            if (centro.getEmail() == null || !centroActualizado.getEmail().equalsIgnoreCase(centro.getEmail())) {
                comprobarEmailUnico(centroActualizado.getEmail());
            }
        }

        centroMapper.updateCentroFromDTO(centroActualizado, centro);
        if (centroActualizado.getProvincia() != null && !centroActualizado.getProvincia().isBlank()) {
            centro.setProvincia(provinciaService.buscarPorNombre(centroActualizado.getProvincia()));
        }

        return centroMapper.toDTO(centroRepository.save(centro));
    }


    // DELETE físico

    // Eliminar centro de la BD
    @Transactional
    public void eliminarCentro(Long id) {
        if (!centroRepository.existsById(id)) throw new IllegalStateException("No se ha encontrado ningún centro con id: " + id);
        centroRepository.deleteById(id);
    }

    // Cambios de estado

    // Cambia el estado verificado del centro a true
    @Transactional
    public void verificarCentro(Long id) {
        Centro c = obtenerCentroPorId(id);
        c.setVerificado(true);
        c.setFechaVerificacion(LocalDateTime.now());
        centroRepository.save(c);
    }

    // Cambia el estado verificado del centro a false
    @Transactional
    public void quitarVerificacionCentro(Long id) {
        Centro c = obtenerCentroPorId(id);
        c.setVerificado(false);
        centroRepository.save(c);
    }


    // Obtener gestores de un centro
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerGestoresDeCentro(Long id) {
        Centro centro = obtenerCentroPorId(id);
        return centro.getGestores().stream()
                .map(usuarioMapper::toDTO)
                .sorted(java.util.Comparator.comparing(u -> u.getApellidos() + u.getNombre()))
                .toList();
    }


    // COMPROBACIONES CAMPOS ÚNICOS

    @Transactional(readOnly = true)
    protected void comprobarCodigoUnico(String codigo) {
        if (centroRepository.existsByCodigo(codigo)) throw new IllegalArgumentException("Ya existe un centro con ese código.");
    }

    @Transactional(readOnly = true)
    protected void comprobarEmailUnico(String email) {
        if (centroRepository.existsByEmail(email)) throw new IllegalArgumentException("Ya existe un centro con ese email.");
    }

}
