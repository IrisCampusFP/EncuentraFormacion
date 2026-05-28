package com.irisperez.tfg.encuentraformacion.service.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroGestorRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion.SolicitudGestionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudGestion;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.RolRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudGestionRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SolicitudGestionService {

    private final SolicitudGestionRepository solicitudGestionRepository;
    private final UsuarioService usuarioService;
    private final CentroService centroService;
    private final UsuarioRepository usuarioRepository;
    private final CentroRepository centroRepository;
    private final RolRepository rolRepository;
    private final SolicitudGestionMapper solicitudGestionMapper;
    private final CentroMapper centroMapper;

    @Autowired
    public SolicitudGestionService(SolicitudGestionRepository solicitudGestionRepository,
                                   UsuarioService usuarioService,
                                   CentroService centroService,
                                   UsuarioRepository usuarioRepository,
                                   CentroRepository centroRepository,
                                   RolRepository rolRepository,
                                   SolicitudGestionMapper solicitudGestionMapper,
                                   CentroMapper centroMapper) {
        this.solicitudGestionRepository = solicitudGestionRepository;
        this.usuarioService = usuarioService;
        this.centroService = centroService;
        this.usuarioRepository = usuarioRepository;
        this.centroRepository = centroRepository;
        this.rolRepository = rolRepository;
        this.solicitudGestionMapper = solicitudGestionMapper;
        this.centroMapper = centroMapper;
    }

    // Obtener solicitudes pendientes
    @Transactional(readOnly = true)
    public List<SolicitudGestionDTO> obtenerSolicitudesPendientes() {
        return solicitudGestionMapper.toDTOList(solicitudGestionRepository.findByEstado(EstadoSolicitud.PENDIENTE));
    }

    // Obtener solicitudes procesadas (historial) ordenadas por fecha descendente
    @Transactional(readOnly = true)
    public List<SolicitudGestionDTO> obtenerSolicitudesProcesadas() {
        return solicitudGestionMapper.toDTOList(solicitudGestionRepository.obtenerHistorialSolicitudes(EstadoSolicitud.PENDIENTE));
    }

    // Versiones paginadas para los paneles de administración
    @Transactional(readOnly = true)
    public Page<SolicitudGestionDTO> obtenerSolicitudesPendientesPaginadas(Pageable pageable) {
        return solicitudGestionRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable)
                .map(solicitudGestionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudGestionDTO> obtenerSolicitudesProcesadasPaginadas(Pageable pageable) {
        return solicitudGestionRepository.obtenerHistorialSolicitudesPaginado(EstadoSolicitud.PENDIENTE, pageable)
                .map(solicitudGestionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudGestionDTO> buscarPendientesConFiltro(String q, Boolean verificadoCentro, Pageable pageable) {
        String qNorm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : "";
        return solicitudGestionRepository.buscarPendientesConFiltro(EstadoSolicitud.PENDIENTE, qNorm, verificadoCentro, pageable)
                .map(solicitudGestionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudGestionDTO> buscarHistorialConFiltros(String q, EstadoSolicitud estadoFiltro, Pageable pageable) {
        String qNorm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : "";
        return solicitudGestionRepository.buscarHistorialConFiltros(EstadoSolicitud.PENDIENTE, estadoFiltro, qNorm, pageable)
                .map(solicitudGestionMapper::toDTO);
    }

    // Listar todas las solicitudes del usuario autenticado, ordenadas de más reciente a más antigua
    @Transactional(readOnly = true)
    public List<SolicitudGestionDTO> listarSolicitudesDelUsuario() {
        Usuario usuario = obtenerUsuarioAutenticado();
        return solicitudGestionMapper.toDTOList(
                solicitudGestionRepository.findAllByUsuarioOrderByFechaSolicitudDesc(usuario));
    }

    // Obtener una solicitud concreta del usuario autenticado validando propiedad
    @Transactional(readOnly = true)
    public SolicitudGestionDTO obtenerSolicitudDelUsuarioPorId(Long idSolicitud) {
        Usuario usuario = obtenerUsuarioAutenticado();
        SolicitudGestion solicitud = solicitudGestionRepository.findByIdAndUsuario(idSolicitud, usuario)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud o no tienes permiso para verla."));
        return solicitudGestionMapper.toDTO(solicitud);
    }

    // Obtener la solicitud pendiente más reciente del usuario autenticado (compatibilidad con flujo sin ?id)
    @Transactional(readOnly = true)
    public SolicitudGestionDTO obtenerSolicitudDelUsuario() {
        Usuario usuario = obtenerUsuarioAutenticado();
        SolicitudGestion solicitud = solicitudGestionRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario);
        if (solicitud == null || solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("No tienes ninguna solicitud pendiente de revisión.");
        }
        return solicitudGestionMapper.toDTO(solicitud);
    }

    // Enviar una solicitud de gestión desde una cuenta ya autenticada
    @Transactional
    public SolicitudGestionDTO enviarSolicitudUsuarioAutenticado(Long centroId, MultipartFile prueba) throws IOException {
        Usuario usuario = obtenerUsuarioAutenticado();

        Centro centro = centroRepository.findById(centroId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún centro con ese identificador."));

        if (solicitudGestionRepository.existsByUsuarioIdAndCentroIdAndEstado(
                usuario.getId(), centroId, EstadoSolicitud.PENDIENTE)) {
            throw new IllegalStateException("Ya tienes una solicitud pendiente para ese centro.");
        }

        SolicitudGestion nueva = new SolicitudGestion();
        nueva.setUsuario(usuario);
        nueva.setCentro(centro);
        nueva.setEstado(EstadoSolicitud.PENDIENTE);
        nueva.setFechaSolicitud(LocalDateTime.now());

        if (prueba == null || prueba.isEmpty()) {
            throw new IllegalArgumentException("El documento que prueba tu vinculación con el centro es obligatorio.");
        }
        nueva.setPruebaTitularidad(prueba.getBytes());

        return solicitudGestionMapper.toDTO(solicitudGestionRepository.save(nueva));
    }

    // Registrar un centro nuevo y enviar solicitud de gestión en una sola transacción
    @Transactional
    public SolicitudGestionDTO enviarSolicitudConCentroNuevo(
            RegistroCentroRequestDTO datosCentro, MultipartFile prueba) throws IOException {

        Usuario usuario = obtenerUsuarioAutenticado();

        if (prueba == null || prueba.isEmpty()) {
            throw new IllegalArgumentException("El documento que prueba tu vinculación con el centro es obligatorio.");
        }

        CentroDTO centroDTO = centroService.registrarCentro(datosCentro);
        Centro centro = centroRepository.findById(centroDTO.getId())
                .orElseThrow(() -> new IllegalStateException("Error al recuperar el centro recién creado."));

        SolicitudGestion nueva = new SolicitudGestion();
        nueva.setUsuario(usuario);
        nueva.setCentro(centro);
        nueva.setEstado(EstadoSolicitud.PENDIENTE);
        nueva.setFechaSolicitud(LocalDateTime.now());
        nueva.setPruebaTitularidad(prueba.getBytes());

        return solicitudGestionMapper.toDTO(solicitudGestionRepository.save(nueva));
    }

    // Obtener el usuario autenticado desde el contexto de Spring Security
    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        String email = auth.getName();

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("El usuario no existe en la base de datos."));
    }

    // Obtener imagen (prueba de titularidad)
    @Transactional(readOnly = true)
    public byte[] obtenerPruebaTitularidad(Long idSolicitud) {
        SolicitudGestion solicitud = solicitudGestionRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud con id: " + idSolicitud));
        return solicitud.getPruebaTitularidad();
    }

    // Obtener los datos completos del Centro vinculado a una solicitud (solo admin)
    @Transactional(readOnly = true)
    public CentroDTO obtenerCentroDeSolicitud(Long idSolicitud) {
        SolicitudGestion solicitud = solicitudGestionRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud con id: " + idSolicitud));
        return centroMapper.toDTO(solicitud.getCentro());
    }

    // Obtener los datos del centro de una solicitud propia (usuario autenticado, con validación de propiedad)
    @Transactional(readOnly = true)
    public CentroDTO obtenerCentroDePropiaSolicitud(Long idSolicitud) {
        Usuario usuario = obtenerUsuarioAutenticado();
        SolicitudGestion solicitud = solicitudGestionRepository.findByIdAndUsuario(idSolicitud, usuario)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud o no tienes permiso para verla."));
        return centroMapper.toDTO(solicitud.getCentro());
    }

    // Registrar gestor para un centro que ya existe en el sistema
    @Transactional
    public void registrarGestorCentroExistente(RegistroGestorRequestDTO dto, MultipartFile titularidad) throws IOException {
        Usuario usuario = registrarUsuario(dto.getDatosUsuario());
        Centro centro = centroRepository.findById(dto.getIdCentroExistente())
                .orElseThrow(() -> new IllegalArgumentException("El centro seleccionado no existe."));
        crearSolicitud(usuario, centro, titularidad);
    }

    // Registrar gestor registrando también un centro nuevo
    @Transactional
    public void registrarGestorCentroNuevo(RegistroGestorRequestDTO dto, MultipartFile titularidad) throws IOException {
        if (dto.getDatosCentroNuevo() == null)
            throw new IllegalArgumentException("Debe proporcionar los datos del nuevo centro.");
        Usuario usuario = registrarUsuario(dto.getDatosUsuario());
        CentroDTO centroDTO = centroService.registrarCentro(dto.getDatosCentroNuevo());
        Centro centro = centroRepository.findById(centroDTO.getId())
                .orElseThrow(() -> new IllegalStateException("Error al recuperar el centro creado"));
        crearSolicitud(usuario, centro, titularidad);
    }

    // Registra el usuario y lo recupera como entidad
    private Usuario registrarUsuario(RegistroRequestDTO datosUsuario) {
        UsuarioDTO usuarioDTO = usuarioService.registrarUsuario(datosUsuario);
        return usuarioRepository.findById(usuarioDTO.getId())
                .orElseThrow(() -> new IllegalStateException("Error al recuperar el usuario creado"));
    }

    // Crea y persiste la solicitud de gestión
    private void crearSolicitud(Usuario usuario, Centro centro, MultipartFile titularidad) throws IOException {
        if (titularidad == null || titularidad.isEmpty())
            throw new IllegalArgumentException("El documento que prueba tu titularidad es obligatorio.");
        SolicitudGestion solicitud = new SolicitudGestion();
        solicitud.setUsuario(usuario);
        solicitud.setCentro(centro);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setPruebaTitularidad(titularidad.getBytes());
        solicitudGestionRepository.save(solicitud);
    }

    // Aprobar solicitud
    @Transactional
    public SolicitudGestionDTO aprobarSolicitud(Long idSolicitud) {
        SolicitudGestion solicitud = solicitudGestionRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud con id: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya ha sido procesada anteriormente.");
        }

        Usuario usuario = solicitud.getUsuario();
        Centro centro = solicitud.getCentro();

        // No se puede aprobar si el centro no está verificado
        if (!Boolean.TRUE.equals(centro.getVerificado())) {
            throw new IllegalStateException("No se puede aprobar la solicitud, el centro no está verificado.");
        }

        // Se cambia el estado de la solicitud a Aceptada
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaResolucion(LocalDateTime.now());

        // Si el usuario no tiene el rol de gestor de centro, se le asigna:

        // Se comprueba si el usuario tiene ya el rol de gestor de centro
        boolean esGestor = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombre() == RolNombre.GESTOR_CENTRO);
        
        // Si el usuario no tiene el rol de gestor de centro
        if (!esGestor) {
            // Se busca el rol ROLE_GESTOR_CENTRO en la base de datos
            Rol rolGestor = rolRepository.findByNombre(RolNombre.GESTOR_CENTRO)
                    .orElseThrow(() -> new IllegalStateException("El rol " + RolNombre.GESTOR_CENTRO + " no existe en base de datos."));
            
            usuario.getRoles().add(rolGestor); // Se asigna al usuario el rol de gestor de centro
        }

        // Se vincula el centro al usuario 
        usuario.getCentrosGestionados().add(centro);
        // Se marca el centro como gestionado
        centro.setTieneGestor(true);

        // Se guardan los cambios
        usuarioRepository.save(usuario);
        centroRepository.save(centro);
        
        return solicitudGestionMapper.toDTO(solicitudGestionRepository.save(solicitud));
    }

    // Rechazar solicitud
    @Transactional
    public SolicitudGestionDTO rechazarSolicitud(Long idSolicitud) {
        SolicitudGestion solicitud = solicitudGestionRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud con id: " + idSolicitud));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya ha sido procesada anteriormente.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());

        return solicitudGestionMapper.toDTO(solicitudGestionRepository.save(solicitud));
    }

    // Cancelar solicitud (un usuario solo puede cancelar su propia solicitud)
    @Transactional
    public void cancelarSolicitud(Long idSolicitud) {

        Usuario usuario = obtenerUsuarioAutenticado();

        SolicitudGestion solicitud = solicitudGestionRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la solicitud."));

        // Comprobamos que la solicitud pertenece realmente al usuario logueado
        if (!solicitud.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalStateException("No tienes permiso para cancelar esta solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("No se puede cancelar la solicitud porque ya ha sido procesada.");
        }

        solicitudGestionRepository.delete(solicitud);

        // Si el usuario se queda con 0 roles, se elimina de la BBDD
        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            usuarioRepository.delete(usuario);
        }
    }
}
