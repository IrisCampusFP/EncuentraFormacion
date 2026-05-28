document.addEventListener('DOMContentLoaded', () => {
    [
        ['togglePasswordCrear',    'passwordCrear',    'togglePasswordCrearIcon'],
        ['togglePassNueva',        'passNueva',        'togglePassNuevaIcon'],
        ['togglePassNuevaConfirm', 'passNuevaConfirm', 'togglePassNuevaConfirmIcon'],
    ].forEach(([btnId, inputId, iconId]) => {
        document.getElementById(btnId).addEventListener('click', () => {
            const input = document.getElementById(inputId);
            const icon  = document.getElementById(iconId);
            const show  = input.type === 'password';
            input.type     = show ? 'text' : 'password';
            icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
        });
    });

    const passwordRegex = /^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>\-_=+\[\]]).{8,}$/;

    const tbodyUsuarios = document.getElementById("tBodyUsuarios");
    const recuadroAlert = document.getElementById("recuadroAlert");
    const dialogCrearUsuario = document.getElementById("dialogCrearUsuario");
    const dialogEditarUsuario = document.getElementById("dialogEditarUsuario");
    const dialogCambiarPassword = document.getElementById("dialogCambiarPassword");
    const dialogAsignarRoles = document.getElementById("dialogAsignarRoles");
    const msgCrearError = document.getElementById("msgCrearError");
    const msgEditarError = document.getElementById("msgEditarError");
    const msgPassError = document.getElementById("msgPassError");
    const msgRolesError = document.getElementById("msgRolesError");

    const dialogDetalleUsuario      = document.getElementById("dialogDetalleUsuario");
    const contenidoDetalleUsuario   = document.getElementById("contenidoDetalleUsuario");

    const filtroBusqueda = document.getElementById("filtroBusqueda");
    const filtroEstado   = document.getElementById("filtroEstado");
    const filtroRol      = document.getElementById("filtroRol");
    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");

    // Mostrar nombre del usuario (admin) en el title
    async function inicializarTitle() {
        try {
            const r = await fetch("/perfil", { method: "GET", credentials: "include" });
            const data = await r.json();
            document.title = `Gestión de usuarios (${data.nombre})`;
        } catch (error) {
            console.error('Error al cargar datos del usuario activo:', error);
        }
    }
    inicializarTitle();

    // Carga los roles disponibles en el select de filtro
    async function cargarRolesEnFiltro() {
        try {
            const r = await fetch("/roles", { credentials: "include" });
            if (!r.ok) return;
            const roles = await r.json();
            roles.forEach(rol => {
                const opt = document.createElement("option");
                opt.value = rol.id;
                opt.textContent = rol.nombre;
                filtroRol.appendChild(opt);
            });
        } catch (e) {
            console.error("Error al cargar roles en el filtro:", e);
        }
    }
    cargarRolesEnFiltro();

    let paginaActual = 0;
    let debounceTimer;

    // Listeners de filtros
    filtroBusqueda.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => { paginaActual = 0; cargarUsuarios(0); }, 400);
    });
    filtroEstado.addEventListener('change', () => { paginaActual = 0; cargarUsuarios(0); });
    filtroRol.addEventListener('change',    () => { paginaActual = 0; cargarUsuarios(0); });

    btnLimpiarFiltros.addEventListener('click', () => {
        filtroBusqueda.value = '';
        filtroEstado.value   = '';
        filtroRol.value      = '';
        paginaActual = 0;
        cargarUsuarios(0);
    });

    const idParamUrl = new URLSearchParams(window.location.search).get('id');
    if (idParamUrl) filtroBusqueda.value = idParamUrl;

    cargarUsuarios();

    // Listeners para los botones de la tabla y clic en fila para detalle
    tbodyUsuarios.addEventListener('click', async (e) => {
        const target = e.target.closest('button, a') ?? e.target;

        if (target.classList.contains('btn-editar-usuario')) {
            await cargarDialogEditar(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-roles-usuario')) {
            await cargarDialogAsignarRoles(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-estado-usuario')) {
            const nuevoEstado = target.getAttribute('data-nuevo-estado') === 'true';
            await cambiarEstado(target.getAttribute('data-id'), nuevoEstado);
        } else if (target.classList.contains('btn-eliminar-usuario')) {
            await eliminarUsuario(target.getAttribute('data-id'));
        } else {
            // Clic en celda → abrir dialog de detalle
            const fila = e.target.closest('tr.ef-fila-clicable');
            if (fila) await abrirDetalleUsuario(fila.dataset.id);
        }
    });

    // CARGAR LOS DATOS DE LOS USUARIOS EN EL BODY DE LA TABLA (con paginación y filtros)
    async function cargarUsuarios(page = 0, size = 10) {
        tbodyUsuarios.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-3">Cargando usuarios...</td></tr>';

        const params = new URLSearchParams({ page, size });
        params.append('sort', 'fechaAlta,desc');
        params.append('sort', 'id,desc');

        const q      = filtroBusqueda.value.trim();
        const activo = filtroEstado.value;
        const rolId  = filtroRol.value;
        if (q) {
            const esId = /^\d+$/.test(q);
            if (esId) params.set('id', q); else params.set('q', q);
        }
        if (activo) params.set('activo', activo);
        if (rolId)  params.set('rolId', rolId);

        try {
            const response = await fetch(`/usuarios?${params.toString()}`, { method: "GET", credentials: 'include' });
            if (!response.ok) {
                const data = await response.json();
                throw data.errorMsg || `Error ${response.status}: No se ha podido cargar la lista de usuarios.`;
            }
            const paginaData = await response.json();

            // Spring Data Page devuelve los elementos en 'content'
            const usuarios = paginaData.content || [];

            if (usuarios.length === 0) {
                tbodyUsuarios.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">No hay usuarios registrados.</td></tr>';
                renderPaginacion(0, 0, 0);
                return;
            }

            // Se muestran los datos de cada usuario fila por fila (tr) en el tBody
            let tBody = "";
            usuarios.forEach(u => {
                tBody += `
                <tr id="tr-usuario-${u.id}" class="ef-fila-clicable" data-id="${u.id}" title="Clic para ver detalle">
                    <td>${u.id}</td>
                    <td>${u.username}</td>
                    <td>${u.email}</td>
                    <td>${u.nombre} ${u.apellidos}</td>
                    <td>${u.fechaNacimiento ? new Date(u.fechaNacimiento).toLocaleDateString() : "-"}</td>
                    <td class="text-nowrap">${u.telefono || "-"}</td>
                    <td>${u.dni || "-"}</td>
                    <td>${u.sexo || "-"}</td>
                    <td>${u.ultimaConexion ? new Date(u.ultimaConexion).toLocaleString() : "-"}</td>
                    <td>${u.fechaAlta ? new Date(u.fechaAlta).toLocaleString() : "-"}</td>
                    <td id="fechaModificacion-td-${u.id}">${u.fechaModificacion ? new Date(u.fechaModificacion).toLocaleString() : "-"}</td>
                    <td id="estado-td-${u.id}">${u.activo ? '<span class="badge text-bg-success">Activo</span>' : '<span class="badge text-bg-danger">Inactivo</span>'}</td>
                    <td>${mostrarRoles(u.roles)}</td>
                    <!-- Botones para acciones CRUD -->
                    <td class="text-nowrap">
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-editar-usuario" data-id="${u.id}"><i class="bi bi-pencil me-1"></i>Editar</button>
                            <button class="btn btn-sm btn-outline-back ef-btn-accion btn-roles-usuario" data-id="${u.id}"><i class="bi bi-person-badge me-1"></i>Roles</button>
                            <button id="btn-estado-${u.id}" class="btn btn-sm btn-outline-warning ef-btn-accion ef-btn-estado btn-estado-usuario" data-id="${u.id}" data-nuevo-estado="${!u.activo}">
                                ${u.activo ? '<i class="bi bi-toggle-on me-1"></i>Desactivar' : '<i class="bi bi-toggle-off me-1"></i>Activar'}
                            </button>
                            <button class="btn btn-sm btn-outline-danger ef-btn-accion btn-eliminar-usuario" data-id="${u.id}"><i class="bi bi-trash me-1"></i>Eliminar</button>
                        </div>
                    </td>
                </tr>`;
            });
            tbodyUsuarios.innerHTML = tBody;
            paginaActual = paginaData.page.number;
            renderPaginacion(paginaData.page.number, paginaData.page.totalPages, paginaData.page.totalElements);
        } catch (error) {
            mostrarError(error);
        }
    }

    function renderPaginacion(currentPage, totalPages, totalElements) {
        const container = document.getElementById("paginacionUsuarios");
        if (!container) return;

        const total = totalElements ?? 0;
        const pages = totalPages ?? 1;

        if (pages <= 1) {
            container.innerHTML = `<span class="text-secondary small">${total} usuario${total !== 1 ? 's' : ''} en total</span>`;
            return;
        }

        container.innerHTML = `
            <nav aria-label="Paginación de usuarios">
                <ul class="pagination pagination-sm mb-0 gap-2">
                    <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                        <button class="page-link" id="btnPaginaAnterior" ${currentPage === 0 ? 'disabled' : ''}>← Anterior</button>
                    </li>
                    <li class="page-item disabled">
                        <span class="page-link">Página ${currentPage + 1} de ${pages}</span>
                    </li>
                    <li class="page-item ${currentPage >= pages - 1 ? 'disabled' : ''}">
                        <button class="page-link" id="btnPaginaSiguiente" ${currentPage >= pages - 1 ? 'disabled' : ''}>Siguiente →</button>
                    </li>
                </ul>
            </nav>
            <span class="text-secondary small">${total} usuario${total !== 1 ? 's' : ''} en total</span>`;

        if (currentPage > 0)
            document.getElementById("btnPaginaAnterior")
                .addEventListener('click', () => { paginaActual = currentPage - 1; cargarUsuarios(currentPage - 1); });

        if (currentPage < pages - 1)
            document.getElementById("btnPaginaSiguiente")
                .addEventListener('click', () => { paginaActual = currentPage + 1; cargarUsuarios(currentPage + 1); });
    }

    function mostrarRoles(roles) {
        if (!roles || roles.length === 0) return "-";
        return roles.map(rol => rol.nombre).join(', ');
    }

    function mostrarError(errorMsg) {
        recuadroAlert.textContent = errorMsg;
        recuadroAlert.className = "alert alert-danger mb-3";
        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 3000);
    }

    function mostrarExito(msg) {
        recuadroAlert.textContent = msg;
        recuadroAlert.className = "alert alert-success mb-3";
        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 3000);
    }

    // CREAR NUEVO USUARIO

    // Al pulsar el botón 'Crear nuevo usuario' se abre el modal (dialog) con el formulario
    document.getElementById("btnCrearUsuario").addEventListener('click', () => {
        dialogCrearUsuario.showModal();
    });

    // Al pulsar el boton 'Cancelar' se cierra el modal
    document.getElementById("btnCancelarCrearUsuario").addEventListener('click', () => {
        dialogCrearUsuario.close();
    });

    // Validación en tiempo real: comprueba que el email y el username no estén ya en uso
    let emailCrearValido = true;
    let usernameCrearValido = true;

    document.getElementById("emailCrear").addEventListener("blur", async (e) => {
        const input = e.target;
        const error = document.getElementById("emailCrearError");
        if (input.value.trim() === "") return;

        try {
            const res = await fetch(`/check-email-unique?email=${encodeURIComponent(input.value.trim())}`);
            const data = await res.json();
            if (data.existe) {
                error.textContent = "Ya existe un usuario registrado con ese email.";
                input.classList.add("is-invalid");
                emailCrearValido = false;
            } else {
                error.textContent = "";
                input.classList.remove("is-invalid");
                emailCrearValido = true;
            }
        } catch (error) {
            console.error('Error al verificar email:', error);
        }
    });

    document.getElementById("usernameCrear").addEventListener("blur", async (e) => {
        const input = e.target;
        const error = document.getElementById("usernameCrearError");
        if (input.value.trim() === "") return;

        try {
            const res = await fetch(`/check-username-unique?username=${encodeURIComponent(input.value.trim())}`);
            const data = await res.json();
            if (data.existe) {
                error.textContent = "Ya existe un usuario con ese nombre de usuario.";
                input.classList.add("is-invalid");
                usernameCrearValido = false;
            } else {
                error.textContent = "";
                input.classList.remove("is-invalid");
                usernameCrearValido = true;
            }
        } catch (error) {
            console.error('Error al verificar username:', error);
        }
    });

    const passwordCrearInput   = document.getElementById("passwordCrear");
    const passwordCrearFeedback = document.getElementById("passwordCrearFeedback");

    passwordCrearInput.addEventListener("blur", () => {
        const val = passwordCrearInput.value;
        if (val && !passwordRegex.test(val)) {
            passwordCrearInput.classList.add("is-invalid");
            passwordCrearFeedback.textContent = "Mínimo 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%...).";
            passwordCrearFeedback.style.display = "block";
        } else {
            passwordCrearInput.classList.remove("is-invalid");
            passwordCrearFeedback.style.display = "none";
        }
    });

    // Al enviar el formulario se llama a la función crear usuario
    document.getElementById("formCrearUsuario").addEventListener('submit', (e) => {
        e.preventDefault();

        if (!emailCrearValido) {
            msgCrearError.textContent = "Correo electrónico no válido.";
            msgCrearError.classList.remove("d-none");
            return;
        }

        if (!usernameCrearValido) {
            msgCrearError.textContent = "Nombre de usuario no válido.";
            msgCrearError.classList.remove("d-none");
            return;
        }

        if (!passwordRegex.test(passwordCrearInput.value)) {
            msgCrearError.textContent = "La contraseña no cumple con los requisitos de seguridad (mínimo 8 caracteres, una mayúscula, un número y un carácter especial).";
            msgCrearError.classList.remove("d-none");
            return;
        }

        crearUsuario();
    });

    // Función que envía al backend los datos del nuevo usuario, se guardan y se recarga la tabla
    async function crearUsuario() {
        try {
            const r = await fetch("/usuarios", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    username: document.getElementById("usernameCrear").value.trim(),
                    email: document.getElementById("emailCrear").value.trim(),
                    password: document.getElementById("passwordCrear").value.trim(),
                    nombre: document.getElementById("nombreCrear").value.trim(),
                    apellidos: document.getElementById("apellidosCrear")?.value?.trim(),
                    fechaNacimiento: document.getElementById("fechaNacimientoCrear")?.value || null,
                    telefono: document.getElementById("telefonoCrear")?.value?.trim() || null,
                    dni: document.getElementById("dniCrear")?.value?.trim() || null,
                    sexo: document.getElementById("sexoCrear")?.value || null,
                    activo: document.getElementById("activoCrear").value === "true"
                })
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido crear el usuario.`;
            }

            const usuarioCreado = await r.json();
            dialogCrearUsuario.close();
            await cargarUsuarios(0); // nuevo usuario → primera página
            await cargarDialogAsignarRoles(usuarioCreado.id); // Se abre el dialog para asignar roles
        } catch (error) {
            msgCrearError.textContent = error;
            msgCrearError.classList.remove("d-none");
        }
    }

    dialogCrearUsuario.addEventListener("close", () => {
        document.getElementById("formCrearUsuario").reset();
        msgCrearError.classList.add("d-none");
        emailCrearValido = true;
        usernameCrearValido = true;
        document.getElementById("emailCrear").classList.remove("is-invalid");
        document.getElementById("usernameCrear").classList.remove("is-invalid");
        document.getElementById("emailCrearError").textContent = "";
        document.getElementById("usernameCrearError").textContent = "";
        passwordCrearInput.classList.remove("is-invalid");
        passwordCrearFeedback.style.display = "none";
    });

    // EDITAR USUARIO

    let emailEditarValido = true;
    let usernameEditarValido = true;

    // Al pulsar el botón editar se llama a esta función que obtiene los datos del usuario por su id
    async function cargarDialogEditar(id) {
        try {
            const r = await fetch(`/usuarios/${id}`, { method: "GET" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se han podido cargar los datos del usuario.`;
            }
            const u = await r.json();

            document.getElementById("usuarioPassEditadaId").value = u.id; // Guardo el id del usuario por si selecciona cambiar contraseña

            document.getElementById("idUsuarioEditado").value = u.id;
            document.getElementById("usernameEditar").value = u.username;
            document.getElementById("emailEditar").value = u.email;
            document.getElementById("nombreEditar").value = u.nombre;
            document.getElementById("apellidosEditar").value = u.apellidos || "";
            document.getElementById("fechaNacimientoEditar").value = u.fechaNacimiento ? u.fechaNacimiento : "";
            document.getElementById("telefonoEditar").value = u.telefono || "";
            document.getElementById("dniEditar").value = u.dni || "";
            document.getElementById("sexoEditar").value = u.sexo || "";
            document.getElementById("activoEditar").value = u.activo;

            // Validación en tiempo real de email y username únicos
            // Se guardan los valores originales para no marcarlos como duplicados si no cambian
            emailEditarValido = true;
            usernameEditarValido = true;

            // Se clonan los inputs para eliminar listeners anteriores antes de añadir los nuevos
            const emailInput = document.getElementById("emailEditar");
            const emailNuevo = emailInput.cloneNode(true);
            emailInput.replaceWith(emailNuevo);
            emailNuevo.value = u.email;

            const usernameInput = document.getElementById("usernameEditar");
            const usernameNuevo = usernameInput.cloneNode(true);
            usernameInput.replaceWith(usernameNuevo);
            usernameNuevo.value = u.username;

            emailNuevo.addEventListener("blur", async () => {
                const error = document.getElementById("emailEditarError");
                const valor = emailNuevo.value.trim();
                // Si el valor no ha cambiado respecto al original, no hay duplicado
                if (valor === "" || valor === u.email) {
                    error.textContent = "";
                    emailNuevo.classList.remove("is-invalid");
                    emailEditarValido = true;
                    return;
                }
                try {
                    const res = await fetch(`/check-email-unique?email=${encodeURIComponent(valor)}`);
                    const data = await res.json();
                    if (data.existe) {
                        error.textContent = "Ya existe un usuario registrado con ese email.";
                        emailNuevo.classList.add("is-invalid");
                        emailEditarValido = false;
                    } else {
                        error.textContent = "";
                        emailNuevo.classList.remove("is-invalid");
                        emailEditarValido = true;
                    }
                } catch (error) {
                    console.error('Error al validar email', error);
                }
            });

            usernameNuevo.addEventListener("blur", async () => {
                const error = document.getElementById("usernameEditarError");
                const valor = usernameNuevo.value.trim();
                if (valor === "" || valor === u.username) {
                    error.textContent = "";
                    usernameNuevo.classList.remove("is-invalid");
                    usernameEditarValido = true;
                    return;
                }
                try {
                    const res = await fetch(`/check-username-unique?username=${encodeURIComponent(valor)}`);
                    const data = await res.json();
                    if (data.existe) {
                        error.textContent = "Ya existe un usuario con ese nombre de usuario.";
                        usernameNuevo.classList.add("is-invalid");
                        usernameEditarValido = false;
                    } else {
                        error.textContent = "";
                        usernameNuevo.classList.remove("is-invalid");
                        usernameEditarValido = true;
                    }
                } catch (err) {
                    console.error('Error validando username al editar', err);
                }
            });

            dialogEditarUsuario.showModal();
        } catch (error) {
            mostrarError(error);
        }
    }

    // Al pulsar el boton 'Cancelar' se cierra el modal
    document.getElementById("btnCancelarEditarUsuario").addEventListener('click', () => {
        dialogEditarUsuario.close();
    });

    // Al enviar el formulario se llama a la función editar usuario
    document.getElementById("formEditarUsuario").addEventListener('submit', (e) => {
        e.preventDefault();

        // Si el email ya está en uso no se envía el formulario
        if (!emailEditarValido) {
            msgEditarError.textContent = "Correo electrónico no válido.";
            msgEditarError.classList.remove("d-none");
            return;
        }

        if (!usernameEditarValido) {
            msgEditarError.textContent = "Nombre de usuario no válido.";
            msgEditarError.classList.remove("d-none");
            return;
        }

        // Se obtiene el id del usuario en el que se clicó el botón
        const id = document.getElementById("idUsuarioEditado").value;

        editarUsuario(id);
    });

    // Se hace un fetch (metodo PUT) que envía los datos actualizados al backend.
    async function editarUsuario(id) {
        try {
            const r = await fetch(`/usuarios/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    email: document.getElementById("emailEditar").value.trim(),
                    nombre: document.getElementById("nombreEditar").value.trim(),
                    apellidos: document.getElementById("apellidosEditar").value.trim(),
                    username: document.getElementById("usernameEditar").value.trim(),
                    fechaNacimiento: document.getElementById("fechaNacimientoEditar")?.value || null,
                    telefono: document.getElementById("telefonoEditar")?.value?.trim() || null,
                    dni: document.getElementById("dniEditar")?.value?.trim() || null,
                    sexo: document.getElementById("sexoEditar")?.value || null,
                    activo: document.getElementById("activoEditar").value === "true"
                    // === "true" es true (boolean) si coincide, y si no false (boolean)
                    // de esta manera conseguimos devolver un booleano y no un string
                })
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido actualizar el usuario.`;
            }

            dialogEditarUsuario.close();
            await cargarUsuarios(paginaActual); // permanece en la misma página
        } catch (error) {
            msgEditarError.textContent = error;
            msgEditarError.classList.remove("d-none");
        }
    }

    dialogEditarUsuario.addEventListener("close", () => {
        document.getElementById("formEditarUsuario").reset();
        msgEditarError.classList.add("d-none");
    });


    // CAMBIAR CONTRASEÑA

    document.getElementById("btnCambiarPassword").addEventListener('click', () => {
        dialogEditarUsuario.close();
        msgPassError.classList.add("d-none");
        passNuevaInput.classList.remove("is-invalid");
        passNuevaConfirmInput.classList.remove("is-invalid");
        passNuevaFeedback.style.display = "none";
        dialogCambiarPassword.showModal();
    });

    // Al pulsar el boton 'Cancelar' se cierra el modal y vuelve a mostrar el de editar
    document.getElementById("btnCancelarCambiarPassword").addEventListener('click', () => {
        dialogCambiarPassword.close();
        dialogEditarUsuario.showModal();
    });

    const passNuevaInput    = document.getElementById("passNueva");
    const passNuevaConfirmInput = document.getElementById("passNuevaConfirm");
    const passNuevaFeedback = document.getElementById("passNuevaFeedback");

    function mostrarFeedbackPassNueva() {
        const val  = passNuevaInput.value;
        const val2 = passNuevaConfirmInput.value;
        const politicaInvalida = val && !passwordRegex.test(val);
        const noCoinciden      = val && val2 && val !== val2;

        passNuevaInput.classList.toggle("is-invalid", !!politicaInvalida);
        passNuevaConfirmInput.classList.toggle("is-invalid", !politicaInvalida && !!noCoinciden);

        if (politicaInvalida) {
            passNuevaFeedback.textContent = "Mínimo 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%...).";
            passNuevaFeedback.style.display = "block";
        } else if (noCoinciden) {
            passNuevaFeedback.textContent = "Las contraseñas no coinciden.";
            passNuevaFeedback.style.display = "block";
        } else {
            passNuevaFeedback.style.display = "none";
        }
    }

    passNuevaInput.addEventListener("blur", mostrarFeedbackPassNueva);
    passNuevaConfirmInput.addEventListener("blur", mostrarFeedbackPassNueva);

    // Al enviar el formulario se comprueba política y coincidencia
    document.getElementById("formCambiarPassword").addEventListener('submit', (e) => {
        e.preventDefault();

        const passNueva        = passNuevaInput.value.trim();
        const passNuevaConfirm = passNuevaConfirmInput.value.trim();

        if (!passwordRegex.test(passNueva)) {
            msgPassError.textContent = "La contraseña no cumple con los requisitos de seguridad (mínimo 8 caracteres, una mayúscula, un número y un carácter especial).";
            msgPassError.classList.remove("d-none");
            return;
        }

        if (passNueva !== passNuevaConfirm) {
            msgPassError.textContent = "Las contraseñas no coinciden.";
            msgPassError.classList.remove("d-none");
            return;
        }

        msgPassError.classList.add("d-none");
        cambiarPassword(passNueva);
    });

    async function cambiarPassword(passNueva) {
        const id = document.getElementById("usuarioPassEditadaId").value;

        try {
            const r = await fetch(`/usuarios/${id}/password`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    passwordNueva: passNueva
                })
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido cambiar la contraseña.`;
            }

            dialogCambiarPassword.close();
            mostrarExito("Contraseña modificada con éxito.");
        } catch (error) {
            msgPassError.textContent = error;
            msgPassError.classList.remove("d-none");
        }
    }


    // MODIFICAR ESTADO USUARIO

    // Se hace un fetch (metodo PUT) que actualiza el estado del usuario al valor indicado
    async function cambiarEstado(id, nuevoEstado) {
        try {
            const r = await fetch(`/usuarios/${id}/estado?activo=${nuevoEstado}`, { method: "PUT" });
            if (!r.ok) {
                const errorData = await r.json();
                throw errorData.errorMsg || "Error al modificar el estado.";
            }

            // Se obtiene la fecha de modificacion capturando la fecha del servidor justo después de hacer la petición
            const fechaModificacion = new Date(r.headers.get("Date")).toLocaleString();

            const data = await r.json();

            // Manipulación directa del DOM para evitar recargar toda la tabla
            const etiquetaEstado = document.getElementById(`estado-td-${id}`);
            const btnEstado = document.getElementById(`btn-estado-${id}`);
            const fechaModificacionTd = document.getElementById(`fechaModificacion-td-${id}`);

            if (data.activo) {
                etiquetaEstado.innerHTML = '<span class="badge text-bg-success">Activo</span>';
                btnEstado.innerHTML = '<i class="bi bi-toggle-on me-1"></i>Desactivar';
                btnEstado.setAttribute("data-nuevo-estado", "false");
            } else {
                etiquetaEstado.innerHTML = '<span class="badge text-bg-danger">Inactivo</span>';
                btnEstado.innerHTML = '<i class="bi bi-toggle-off me-1"></i>Activar';
                btnEstado.setAttribute("data-nuevo-estado", "true");
            }

            fechaModificacionTd.textContent = fechaModificacion;

        } catch (error) {
            mostrarError(error || `Error al modificar el estado del usuario con id: ${id}`);
        }
    }

    // ELIMINAR USUARIO

    async function eliminarUsuario(id) {
        const ok = await efConfirm({
            title:       'Eliminar usuario',
            message:     `Esta acción es <strong>irreversible</strong>. ¿Seguro que quieres eliminar el usuario con ID <strong>${id}</strong>?`,
            confirmText: 'Eliminar',
            variant:     'danger',
        });
        if (!ok) return;

        try {
            // Se hace un fetch con metodo DELETE para eliminar el usuario de la base de datos
            const r = await fetch(`/usuarios/${id}`, { method: "DELETE" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || "Error al eliminar el usuario.";
            }

            const pagina = tbodyUsuarios.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarUsuarios(pagina);
        } catch (error) {
            mostrarError(error || `Error al eliminar el usuario con id: ${id}`);
        }
    }

    // DIALOG DE DETALLE (solo lectura, se abre al clicar una fila)

    document.getElementById("btnCerrarDialogDetalleUsuario").addEventListener('click', () => dialogDetalleUsuario.close());
    document.getElementById("btnCerrarDetalleUsuarioFooter").addEventListener('click', () => dialogDetalleUsuario.close());

    dialogDetalleUsuario.addEventListener('click', (e) => {
        if (e.target === dialogDetalleUsuario) dialogDetalleUsuario.close();
    });

    async function abrirDetalleUsuario(id) {
        contenidoDetalleUsuario.innerHTML = '<p class="text-center text-secondary py-3">Cargando datos...</p>';
        dialogDetalleUsuario.showModal();

        try {
            const r = await fetch(`/usuarios/${id}`, { credentials: 'include' });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}`;
            }
            const u = await r.json();

            const roles = u.roles?.length > 0
                ? u.roles.map(r => `<span class="badge text-bg-secondary me-1">${r.nombre}</span>`).join('')
                : '<span class="text-secondary">Sin roles</span>';
            const estadoBadge = u.activo
                ? '<span class="badge text-bg-success">Activo</span>'
                : '<span class="badge text-bg-danger">Inactivo</span>';

            const tieneRolEstudiante = u.roles?.some(r => r.nombre === 'ESTUDIANTE' || r.nombre === 'ROLE_ESTUDIANTE');
            const seccionEstudiante = tieneRolEstudiante && (u.gradoEstudiosNombre || u.provinciaNombre || u.localidad)
                ? `<div class="col-12 mt-2"><p class="detalle-seccion">Datos de estudiante</p></div>
                   <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Grado de estudios</span><span class="detalle-valor">${u.gradoEstudiosNombre || '-'}</span></div></div>
                   <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Provincia</span><span class="detalle-valor">${u.provinciaNombre || '-'}</span></div></div>
                   <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Localidad</span><span class="detalle-valor">${u.localidad || '-'}</span></div></div>`
                : '';

            const tieneRolGestor = u.roles?.some(r => r.nombre === 'GESTOR_CENTRO' || r.nombre === 'ROLE_GESTOR_CENTRO');
            const centros = u.centrosGestionados || [];
            const seccionGestor = tieneRolGestor
                ? `<div class="col-12 mt-2"><p class="detalle-seccion">Centro gestionado</p></div>
                   <div class="col-12"><div class="detalle-campo"><span class="detalle-valor">${
                       centros.length > 0
                           ? [...centros].sort((a, b) => a.nombreComercial.localeCompare(b.nombreComercial))
                               .map(c => '<a href=' + c.id + '"/vistas/admin/gestion-centros.html?id=" class="ef-gestor-enlace d-block">' + c.nombreComercial + '</a>')
                               .join('')
                           : '-'
                   }</span></div></div>`
                : '';

            contenidoDetalleUsuario.innerHTML = `
                <div class="row g-4">
                    <div class="col-12"><p class="detalle-seccion">Identificación</p></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID</span><span class="detalle-valor">${u.id}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Username</span><span class="detalle-valor">${u.username}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Email</span><span class="detalle-valor">${u.email}</span></div></div>

                    <div class="col-12"><p class="detalle-seccion">Datos personales</p></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre completo</span><span class="detalle-valor">${u.nombre} ${u.apellidos || ''}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">DNI</span><span class="detalle-valor">${u.dni || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Teléfono</span><span class="detalle-valor">${u.telefono || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de nacimiento</span><span class="detalle-valor">${u.fechaNacimiento ? new Date(u.fechaNacimiento).toLocaleDateString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Sexo</span><span class="detalle-valor">${u.sexo || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Estado</span><span class="detalle-valor">${estadoBadge}</span></div></div>

                    <div class="col-12 mt-1"><p class="detalle-seccion">Roles y actividad</p></div>
                    <div class="col-12"><div class="detalle-campo"><span class="detalle-etiqueta">Roles</span><div class="detalle-valor d-flex flex-wrap gap-1 mt-1">${roles}</div></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de alta</span><span class="detalle-valor">${u.fechaAlta ? new Date(u.fechaAlta).toLocaleString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Última modificación</span><span class="detalle-valor">${u.fechaModificacion ? new Date(u.fechaModificacion).toLocaleString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Última conexión</span><span class="detalle-valor">${u.ultimaConexion ? new Date(u.ultimaConexion).toLocaleString('es-ES') : '-'}</span></div></div>

                    ${seccionEstudiante}
                    ${seccionGestor}
                </div>`;
        } catch (error) {
            contenidoDetalleUsuario.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    // ASIGNAR ROLES

    // Al pulsar el botón asignar roles se llama a esta función que obtiene los datos del usuario y los roles disponibles
    async function cargarDialogAsignarRoles(id) {
        document.getElementById("idUsuarioAsignarRoles").value = id;
        document.getElementById("msgRolesError").classList.add("d-none");

        try {
            // Cargar roles existentes
            const r = await fetch("/roles", { method: "GET" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se han podido cargar roles existentes.`;
            }
            const rolesDisponibles = await r.json();

            // Cargar datos del usuario con sus roles actuales
            const rUsuario = await fetch(`/usuarios/${id}`);
            if (!rUsuario.ok) {
                const data = await rUsuario.json();
                throw data.errorMsg || `Error ${rUsuario.status}: No se ha podido cargar usuario.`;
            }
            const usuario = await rUsuario.json();

            // Mostrar nombre del usuario en el dialog
            document.getElementById("nombreUsuarioAsignarRoles").textContent = `${usuario.nombre} ${usuario.apellidos || ""} (${usuario.username}). Id: ${usuario.id}`;

            // Obtener los ids de los roles que tiene el usuario
            const rolesUsuario = usuario.roles.map(r => r.id);

            // Se genera el checkbox dinámicamente, seleccionando los roles que ya tiene asignados el usuario
            const checkboxRoles = document.getElementById("checkboxRoles");
            let htmlCheckboxRoles = "";

            rolesDisponibles.forEach((rol) => {
                htmlCheckboxRoles += `
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" value="${rol.id}"
                               id="rol${rol.id}" ${(rolesUsuario.includes(rol.id) ? 'checked' : '')} ${rol.id === 3 ? 'disabled' : ''}>
                        <label class="form-check-label" for="rol${rol.id}">${rol.nombre}</label>
                    </div>
                `;
            });

            checkboxRoles.innerHTML = htmlCheckboxRoles;
            dialogAsignarRoles.showModal();
        } catch (error) {
            mostrarError(error);
        }
    }

    // Al pulsar el boton 'Cancelar' se cierra el modal
    document.getElementById("btnCancelarAsignarRoles").addEventListener('click', () => {
        dialogAsignarRoles.close();
    });

    // Al enviar el formulario se llama a la función asignar roles
    document.getElementById("formAsignarRoles").addEventListener('submit', (e) => {
        e.preventDefault();
        asignarRoles();
    });

    // Función que envía los roles seleccionados al backend
    async function asignarRoles() {
        const id = document.getElementById("idUsuarioAsignarRoles").value;
        const checkboxMarcados = document.querySelectorAll("#checkboxRoles input[type='checkbox']:checked");
        const rolesSeleccionados = [];

        checkboxMarcados.forEach((cb) => {
            rolesSeleccionados.push(parseInt(cb.value));
        });

        // Comprueba que se haya seleccionado al menos un rol
        if (rolesSeleccionados.length === 0) {
            msgRolesError.textContent = "El usuario debe tener al menos un rol.";
            msgRolesError.classList.remove("d-none");
            return;
        }

        try {
            const r = await fetch(`/usuarios/${id}/roles`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(rolesSeleccionados)
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error: ${r.status}. No se han podido actualizar roles.`;
            }

            dialogAsignarRoles.close();
            await cargarUsuarios(paginaActual); // permanece en la misma página
        } catch (error) {
            msgRolesError.textContent = error;
            msgRolesError.classList.remove("d-none");
        }
    }
});
