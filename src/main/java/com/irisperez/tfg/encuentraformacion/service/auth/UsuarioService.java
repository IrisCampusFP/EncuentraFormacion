package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.dto.usuario.PerfilUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroUsuarioEstudianteRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioUpdateDTO;
import com.irisperez.tfg.encuentraformacion.mapper.usuario.UsuarioMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudGestionRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.repository.GradoEstudiosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolService rolService;
    private final GradoEstudiosRepository gradoEstudiosRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProvinciaRepository provinciaRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final SolicitudGestionRepository solicitudGestionRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, RolService rolService,
                          GradoEstudiosRepository gradoEstudiosRepository, EstudianteRepository estudianteRepository,
                          ProvinciaRepository provinciaRepository,
                          UsuarioMapper usuarioMapper, PasswordEncoder passwordEncoder,
                          SolicitudGestionRepository solicitudGestionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolService = rolService;
        this.gradoEstudiosRepository = gradoEstudiosRepository;
        this.estudianteRepository = estudianteRepository;
        this.provinciaRepository = provinciaRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.solicitudGestionRepository = solicitudGestionRepository;
    }


    /*

    YA NO ES NECESARIO TRAS LA IMPLEMENTACIÓN DE SPRING SECURITY -> AUTENTICACIÓN Y AUTORIZACIÓN BASADA EN ROLES

    @Transactional
    public boolean comprobarPassword(String password, String email) {

        Usuario usuario = obtenerUsuarioPorEmail(email);

        int numMaxIntentosFallidos = 3;

        // Si el usuario está inactivo, no puede acceder
        if (!usuario.getActivo()) {
            throw new IllegalStateException("El usuario está inactivo");
        }

        // Comprobar contraseña
        if (usuario.checkPassword(password)) {
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
            return true;
        }

        // Si la contraseña no es correcta aumenta el numero de intentos fallidos
        int intentosFallidos = (usuario.getIntentosFallidos() != null) ? usuario.getIntentosFallidos() : 0;

        intentosFallidos++;

        usuario.setIntentosFallidos(intentosFallidos);

        // Se guarda el numero de intentos fallidos
        usuarioRepository.save(usuario);

        // Si llega a 3 fallidos, el usuario se bloquea
        if (intentosFallidos >= numMaxIntentosFallidos) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            throw new IllegalStateException("Usuario bloqueado. Has superado el número máximo de intentos fallidos (" + numMaxIntentosFallidos + ")");
        }

        return false;
    }

     */


    // CREATE

    // Crear usuario
    @Transactional
    public UsuarioDTO crearUsuario(Usuario usuario) {

        if (usuario == null) throw new IllegalArgumentException("Usuario nulo");

        // Si el DNI es una cadena vacía, se guarda como null
        if (usuario.getDni() != null && usuario.getDni().trim().isEmpty()) {
            usuario.setDni(null);
        }

        comprobarEmailUnico(usuario.getEmail());
        comprobarUsernameUnico(usuario.getUsername());
        comprobarDniUnico(usuario.getDni());

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Hashea la contraseña antes de guardarla

//        // Si no tiene ningún rol asignado, se le asigna el rol por defecto (ESTUDIANTE)
//        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
//
//            Rol rolPorDefecto = rolService.obtenerRolPorId(2L)
//                    .orElseThrow(() -> new RuntimeException("El rol asignado por defecto no existe"));
//
//            usuario.getRoles().add(rolPorDefecto);
//        }

        return usuarioMapper.toDTO(usuarioRepository.save(usuario));
    }

    // Registrar usuario
    @Transactional
    public UsuarioDTO registrarUsuario(RegistroRequestDTO dto) {

        Usuario usuario = usuarioMapper.createUsuarioFromDTO(dto);

        return crearUsuario(usuario);

    }

    // Registrar estudiante
    @Transactional
    public UsuarioDTO registrarEstudiante(RegistroUsuarioEstudianteRequestDTO dto) {

        Usuario usuario = usuarioMapper.createUsuarioEstudianteFromDTO(dto);

        // Creo la entidad Estudiante y la vinculo al usuario registrado
        Estudiante estudiante = new Estudiante();
        estudiante.setUsuario(usuario);

        // Busco GradoEstudios por su nombre y lo asigno al estudiante
        if (dto.getGradoEstudios() != null && !dto.getGradoEstudios().isBlank()) {
            GradoEstudios grado = gradoEstudiosRepository.findByNombre(dto.getGradoEstudios())
                    .orElseThrow(() -> new IllegalArgumentException("Grado de estudios no válido."));
            estudiante.setGradoEstudios(grado);
        } else {
            throw new IllegalArgumentException("Debes indicar un Grado de Estudios.");
        }

        if (dto.getProvinciaId() != null) {
            Provincia provincia = provinciaRepository.findById(dto.getProvinciaId())
                    .orElseThrow(() -> new IllegalArgumentException("Provincia no válida."));
            estudiante.setProvincia(provincia);
        }

        if (dto.getLocalidad() != null && !dto.getLocalidad().isBlank()) {
            estudiante.setLocalidad(dto.getLocalidad().trim());
        }

        usuario.setEstudiante(estudiante);

        // Asigno el rol de ESTUDIANTE al usuario creado
        Rol rolEstudiante = rolService.obtenerRolPorNombre(RolNombre.ESTUDIANTE);
        usuario.getRoles().add(rolEstudiante);

        return crearUsuario(usuario);

    }


    // READ

    // Comprobar si existe email
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    // Comprobar si existe username
    @Transactional(readOnly = true)
    public boolean existeUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    // Obtener usuario por email
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario registrado con ese email."));
    }

    // Obtener DTO usuario por email
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioDTOPorEmail(String email) {
        Usuario usuario = obtenerUsuarioPorEmail(email);
        UsuarioDTO dto = usuarioMapper.toDTO(usuario);
        dto.setTieneSolicitudGestionPendiente(
            solicitudGestionRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoSolicitud.PENDIENTE)
        );
        return dto;
    }

    // Obtener todos los usuarios
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerUsuarios() {
        return usuarioMapper.toDTOList(usuarioRepository.findAll());
    }

    // Obtener usuarios paginados (sin filtros — mantiene compatibilidad)
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> obtenerUsuariosPaginados(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(usuarioMapper::toDTO);
    }

    // Búsqueda con filtros opcionales para el panel de administración
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> buscarConFiltros(Long id, String q, Boolean activo, Long rolId, Pageable pageable) {
        String qNorm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : "";
        return usuarioRepository.buscarConFiltros(id, qNorm, activo, rolId, pageable)
                .map(usuarioMapper::toDTO);
    }

    // Obtener usuario por id
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún usuario con id: " + id));
    }

    // Obtener DTO usuario por id (con centros gestionados cargados)
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioDTOPorId(Long id) {
        Usuario usuario = usuarioRepository.findByIdWithCentros(id)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún usuario con id: " + id));
        return usuarioMapper.toDTO(usuario);
    }


//    // Obtener usuario por username
//    @Transactional(readOnly = true)
//    public UsuarioDTO obtenerUsuarioPorUsername(String username) {
//        Usuario usuario = usuarioRepository.findByUsername(username)
//                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún usuario con el username '" + username + "'"));
//        return usuarioMapper.toDTO(usuario);
//    }

//    // Obtener todos los usuarios activos
//    @Transactional(readOnly = true)
//    public List<UsuarioDTO> obtenerUsuariosActivos() {
//        return usuarioMapper.toDTOList(usuarioRepository.findByActivoTrue());
//    }


    // UPDATE

    /* Cambia la contraseña de un usuario verificando la contraseña actual.
     * No se cambia si el usuario no existe, la contraseña actual es incorrecta
     * o la nueva contraseña está vacía. La nueva contraseña se guarda hasheada (BCrypt). */
    @Transactional
    public void cambiarPassword(Long id, String passwordActual, String passwordNueva) {

        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún usuario con id: " + id));

        if (!usuario.getActivo()) {
            throw new IllegalStateException("El usuario está desactivado.");
        }

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta.");
        }

        if (passwordNueva == null || passwordNueva.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));

        usuarioRepository.save(usuario);
    }

    /* Permite al administrador cambiar la contraseña de un usuario
     * sin necesidad de conocer su contraseña anterior. */
    @Transactional
    public void cambiarPasswordAdmin(Long id, String passwordNueva) {

        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se ha encontrado ningún usuario con id: " + id));

        if (passwordNueva == null || passwordNueva.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));

        usuarioRepository.save(usuario);
    }

    /* Actualizar usuario. No se actualiza si:
     * - El usuario con los nuevos datos viene vacío
     * - El usuario a actualizar no existe en la base de datos
     */
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioUpdateDTO usuarioActualizado) {
        if(usuarioActualizado == null) throw new IllegalArgumentException("No se han recibido correctamente los nuevos datos.");

        Optional<Usuario> usuarioAActualizar = usuarioRepository.findById(id);

        if (usuarioAActualizar.isEmpty()) {
            throw new IllegalStateException("El usuario no existe en la base de datos.");
        }

        Usuario usuario = usuarioAActualizar.get();

        if (usuarioActualizado.getDni() != null && usuarioActualizado.getDni().trim().isEmpty()) {
            usuarioActualizado.setDni(null);
        }


        // Si se ha modificado el username, comprobar que el nuevo sea único en la BD
        if (!usuario.getUsername().equalsIgnoreCase(usuarioActualizado.getUsername())) {
            comprobarUsernameUnico(usuarioActualizado.getUsername());
        }
        // Si se ha modificado el email, comprobar que el nuevo sea único en la BD
        if (!usuario.getEmail().equalsIgnoreCase(usuarioActualizado.getEmail())) {
            comprobarEmailUnico(usuarioActualizado.getEmail());
        }
        // Si se ha modificado el DNI, comprobar que el nuevo sea único en la BD
        if (usuarioActualizado.getDni() != null && !usuarioActualizado.getDni().isBlank()) {
            if (usuario.getDni() == null || !usuario.getDni().equalsIgnoreCase(usuarioActualizado.getDni())) {
                comprobarDniUnico(usuarioActualizado.getDni());
            }
        }

        // Sobreescribo solo los campos recibidos en el DTO usando MapStruct
        usuarioMapper.updateUsuarioFromDTO(usuarioActualizado, usuario);

        // Guardo el usuario en la base de datos y retorno los datos del usuario actualizado
        return usuarioMapper.toDTO(usuarioRepository.save(usuario));

    }

    @Transactional
    public UsuarioDTO actualizarPerfil(Long usuarioId, PerfilUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        String nuevoUsername = dto.getUsername().trim();
        if (!nuevoUsername.equals(usuario.getUsername()) &&
                usuarioRepository.existsByUsername(nuevoUsername)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }
        String nuevoEmail = dto.getEmail().trim().toLowerCase();
        if (!nuevoEmail.equals(usuario.getEmail()) &&
                usuarioRepository.existsByEmail(nuevoEmail)) {
            throw new IllegalArgumentException("Ese correo electrónico ya está en uso.");
        }
        usuario.setUsername(nuevoUsername);
        usuario.setEmail(nuevoEmail);
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellidos());
        usuario.setTelefono(dto.getTelefono());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setSexo(dto.getSexo());
        usuarioRepository.save(usuario);

        estudianteRepository.findByUsuarioId(usuarioId).ifPresent(estudiante -> {
            estudiante.setGradoEstudios(dto.getGradoEstudiosId() != null
                ? gradoEstudiosRepository.findById(dto.getGradoEstudiosId())
                    .orElseThrow(() -> new IllegalArgumentException("Grado de estudios no válido"))
                : null);
            estudiante.setProvincia(dto.getProvinciaId() != null
                ? provinciaRepository.findById(dto.getProvinciaId())
                    .orElseThrow(() -> new IllegalArgumentException("Provincia no válida"))
                : null);
            estudiante.setLocalidad(dto.getLocalidad() != null && !dto.getLocalidad().isBlank()
                ? dto.getLocalidad().trim() : null);
            estudianteRepository.save(estudiante);
        });

        return usuarioMapper.toDTO(usuarioRepository.findById(usuarioId).orElseThrow());
    }

    // Recibe un id de usuario y una lista con ids de roles,
    // asigna los roles al usuario
    @Transactional
    public void actualizarRolesUsuario(Long idUsuario, List<Long> idsRoles) {

        // Obtengo el usuario por su id
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado."));

        if (idsRoles == null || idsRoles.isEmpty()) {
            throw new IllegalArgumentException("Un usuario debe tener al menos un rol.");
        }

        // Creo un HashSet con los roles seleccionados
        List<Rol> roles = new ArrayList<>(rolService.obtenerRolesPorId(idsRoles));

        usuario.getRoles().clear(); // Limpio los roles originales
        usuario.setRoles(roles); // Establezco los nuevos roles

        usuarioRepository.save(usuario);

        boolean tieneRolEstudiante = roles.stream()
                .anyMatch(r -> RolNombre.ESTUDIANTE.equals(r.getNombre()));
        if (tieneRolEstudiante && estudianteRepository.findByUsuarioId(idUsuario).isEmpty()) {
            Estudiante estudiante = new Estudiante();
            estudiante.setUsuario(usuario);
            estudianteRepository.save(estudiante);
        }
    }


    // DELETE lógico (Desactivar)

    // Actualiza el estado activo/inactivo del usuario al valor indicado
    public boolean actualizarEstado(Long id, boolean activo) {
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> new IllegalStateException("No se ha encontrado ningún usuario con id: " + id));
        u.setActivo(activo);
        usuarioRepository.save(u);
        return u.getActivo();
    }

    // DELETE físico

    // Eliminar usuario de la BD
    @Transactional
    public void eliminarUsuario(Long id) {
        if(!usuarioRepository.existsById(id)) throw new IllegalStateException("No se ha encontrado ningún usuario con id: " + id);
        usuarioRepository.deleteById(id);
    }


    /*

        MÉTODOS NO NECESARIOS TRAS LA IMPLEMENTACIÓN DE SPRING SECURITY :))

    // CONTROL DE PERMISOS

    // Comprueba que el usuario sea administrador
    public void comprobarAdmin() {

        UsuarioDTO usuario = (UsuarioDTO) session.getAttribute("usuarioDTO");

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        if (usuario.getRoles().stream().noneMatch(r -> r.getNombre().equalsIgnoreCase("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    // Comprueba que el usuario sea gestor
    public void comprobarGestor() {

        UsuarioDTO usuario = (UsuarioDTO) session.getAttribute("usuarioDTO");

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        if (usuario.getRoles().stream().noneMatch(r -> r.getNombre().equalsIgnoreCase("ROLE_GESTOR_CENTRO")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    // Comprueba que el usuario sea estudiante
    public void comprobarEstudiante() {

        UsuarioDTO usuario = (UsuarioDTO) session.getAttribute("usuarioDTO");

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        if (usuario.getRoles().stream().noneMatch(r -> r.getNombre().equalsIgnoreCase("ROLE_ESTUDIANTE")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
    }

     */


    // COMPROBACIONES CAMPOS ÚNICOS

    @Transactional(readOnly = true)
    protected void comprobarEmailUnico(String email) {
        if(usuarioRepository.existsByEmail(email)) throw new IllegalArgumentException("Ya existe un usuario con ese email.");
    }

    @Transactional(readOnly = true)
    protected void comprobarUsernameUnico(String username) {
        if(usuarioRepository.existsByUsername(username)) throw new IllegalArgumentException("Ya existe un usuario con ese username.");
    }

    @Transactional(readOnly = true)
    protected void comprobarDniUnico(String dni) {
        if (dni != null && !dni.isBlank() && usuarioRepository.existsByDni(dni)) throw new IllegalArgumentException("Ya existe un usuario con ese dni");
    }

}
