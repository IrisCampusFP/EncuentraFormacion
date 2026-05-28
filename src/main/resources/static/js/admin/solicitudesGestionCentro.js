document.addEventListener('DOMContentLoaded', () => {
    const tBodySolicitudes = document.getElementById("tBodySolicitudes");
    const recuadroAlert = document.getElementById("recuadroAlert");
    const dialogVerImagen = document.getElementById("dialogVerImagen");
    const contenedorPrueba = document.getElementById("contenedorPrueba");
    let objectUrlActivo = null;
    const dialogDetalleUsuario = document.getElementById("dialogDetalleUsuario");
    const dialogDetalleCentro = document.getElementById("dialogDetalleCentro");
    const dialogDetalleSolicitud = document.getElementById("dialogDetalleSolicitud");
    const contenidoDetalleUsuario = document.getElementById("contenidoDetalleUsuario");
    const contenidoDetalleCentro = document.getElementById("contenidoDetalleCentro");
    const contenidoDetalleSolicitud = document.getElementById("contenidoDetalleSolicitud");

    const filtroBusqueda    = document.getElementById("filtroBusqueda");
    const filtroVerificado  = document.getElementById("filtroVerificado");
    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");

    // Mostrar nombre del usuario (admin) en el title
    async function inicializarTitle() {
        try {
            const r = await fetch("/perfil", { method: "GET", credentials: "include" });
            const data = await r.json();
            document.title = `Solicitudes Gestión (${data.nombre})`;
        } catch (error) {
            console.error('Error al inicializar el título', error);
        }
    }
    inicializarTitle();

    let paginaActual = 0;
    let debounceTimer;

    filtroBusqueda.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => { paginaActual = 0; cargarSolicitudes(0); }, 400);
    });

    filtroVerificado.addEventListener('change', () => { paginaActual = 0; cargarSolicitudes(0); });

    btnLimpiarFiltros.addEventListener('click', () => {
        filtroBusqueda.value = '';
        filtroVerificado.value = '';
        paginaActual = 0;
        cargarSolicitudes(0);
    });

    cargarSolicitudes();

    // Listeners para los botones de la tabla
    tBodySolicitudes.addEventListener('click', async (e) => {
        const target = e.target.closest('button, .campo-clickable') ?? e.target;
        if (target.classList.contains('btn-ver-documento')) {
            verPruebaTitularidad(target.getAttribute('data-id'));
        } else if (target.classList.contains('td-detalle-solicitante')) {
            await verDetalleSolicitante(target.getAttribute('data-id'));
        } else if (target.classList.contains('td-detalle-centro')) {
            await verDetalleCentro(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-aprobar-solicitud')) {
            await aprobarSolicitud(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-rechazar-solicitud')) {
            await rechazarSolicitud(target.getAttribute('data-id'));
        } else {
            const fila = e.target.closest('tr.ef-fila-clicable');
            if (fila) abrirDetalleSolicitud(fila.dataset);
        }
    });


    // CARGAR SOLICITUDES PENDIENTES
    async function cargarSolicitudes(page = 0, size = 10) {
        tBodySolicitudes.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">Cargando solicitudes...</td></tr>';

        const params = new URLSearchParams({ page, size });
        const q = filtroBusqueda.value.trim();
        if (q) params.set('q', q);
        const verificado = filtroVerificado.value;
        if (verificado !== '') params.set('verificadoCentro', verificado);

        try {
            const response = await fetch(`/solicitudes-gestion/pendientes?${params.toString()}`, { method: "GET" });
            if (!response.ok) {
                const data = await response.json();
                throw data.errorMsg || `Error ${response.status}: No se ha podido cargar la lista de solicitudes.`;
            }

            const paginaData = await response.json();
            const solicitudes = paginaData.content || [];

            if (solicitudes.length === 0) {
                tBodySolicitudes.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">No hay solicitudes pendientes.</td></tr>';
                renderPaginacion(0, 0, 0);
                return;
            }

            let tBody = "";
            solicitudes.forEach(s => {
                tBody += `
                    <tr id="tr-solicitud-${s.id}" class="ef-fila-clicable"
                        data-id="${s.id}"
                        data-fecha="${s.fechaSolicitud || ''}"
                        data-nombre="${s.nombre || ''}"
                        data-apellidos="${s.apellidos || ''}"
                        data-id-usuario="${s.idUsuario}"
                        data-nombre-centro="${s.nombreCentro || ''}"
                        data-id-centro="${s.idCentro}"
                        data-verificado-centro="${s.verificadoCentro}">
                        <td>${s.id}</td>
                        <td>${s.fechaSolicitud ? new Date(s.fechaSolicitud).toLocaleString() : "-"}</td>
                        <td>
                            <span class="campo-clickable td-detalle-solicitante" data-id="${s.idUsuario}">${s.nombre} ${s.apellidos}</span>
                        </td>
                        <td>
                            <span class="campo-clickable td-detalle-centro" data-id="${s.idCentro}">${s.nombreCentro}</span>
                            ${s.verificadoCentro ? '' : '<span class="badge text-bg-warning ms-1">(No verificado)</span>'}
                        </td>
                        <td class="text-nowrap">
                            <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-ver-documento" data-id="${s.id}"><i class="bi bi-file-earmark-text me-1"></i>Ver documento</button>
                        </td>
                        <td class="text-nowrap">
                            <div class="d-flex gap-2 justify-content-center">
                                <button class="btn btn-sm btn-outline-success ef-btn-accion btn-aprobar-solicitud" data-id="${s.id}"><i class="bi bi-check-circle me-1"></i>Aprobar</button>
                                <button class="btn btn-sm btn-outline-danger ef-btn-accion btn-rechazar-solicitud" data-id="${s.id}"><i class="bi bi-x-circle me-1"></i>Rechazar</button>
                            </div>
                        </td>
                    </tr>
                `;
            });
            tBodySolicitudes.innerHTML = tBody;
            paginaActual = paginaData.page.number;
            renderPaginacion(paginaData.page.number, paginaData.page.totalPages, paginaData.page.totalElements);
        } catch (error) {
            mostrarMensaje(error, false);
        }
    }

    function renderPaginacion(currentPage, totalPages, totalElements) {
        const container = document.getElementById("paginacionSolicitudes");
        if (!container) return;

        const total = totalElements ?? 0;
        const pages = totalPages ?? 1;

        if (pages <= 1) {
            container.innerHTML = `<span class="text-secondary small">${total} solicitud${total !== 1 ? 'es' : ''} pendiente${total !== 1 ? 's' : ''}</span>`;
            return;
        }

        container.innerHTML = `
            <nav aria-label="Paginación de solicitudes">
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
            <span class="text-secondary small">${total} solicitud${total !== 1 ? 'es' : ''} pendiente${total !== 1 ? 's' : ''}</span>`;

        if (currentPage > 0)
            document.getElementById("btnPaginaAnterior").addEventListener('click', () => cargarSolicitudes(currentPage - 1));
        if (currentPage < pages - 1)
            document.getElementById("btnPaginaSiguiente").addEventListener('click', () => cargarSolicitudes(currentPage + 1));
    }

    // Mostrar mensaje de error o éxito en el recuadro de alertas (valor de exito por defecto false)
    function mostrarMensaje(mensaje, exito = false) {
        recuadroAlert.textContent = mensaje;

        if (exito) {
            recuadroAlert.classList.remove("alert-danger");
            recuadroAlert.classList.add("alert-success");
        } else {
            recuadroAlert.classList.remove("alert-success");
            recuadroAlert.classList.add("alert-danger");
        }

        recuadroAlert.classList.remove("d-none");

        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 4000);
    }

    // DIALOG DETALLE SOLICITUD
    document.getElementById("btnCerrarDialogDetalleSolicitud").addEventListener("click", () => dialogDetalleSolicitud.close());
    document.getElementById("btnCerrarDetalleSolicitudFooter").addEventListener("click", () => dialogDetalleSolicitud.close());
    dialogDetalleSolicitud.addEventListener('click', (e) => { if (e.target === dialogDetalleSolicitud) dialogDetalleSolicitud.close(); });

    function abrirDetalleSolicitud(d) {
        const fecha = d.fecha ? new Date(d.fecha).toLocaleString('es-ES') : '-';
        const verificadoBadge = d.verificadoCentro === 'true'
            ? '<span class="badge text-bg-success">Verificado</span>'
            : '<span class="badge text-bg-warning text-dark">No verificado</span>';

        contenidoDetalleSolicitud.innerHTML = `
            <div class="row g-4">
                <div class="col-12"><p class="detalle-seccion">Información general</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID solicitud</span><span class="detalle-valor">${d.id}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de solicitud</span><span class="detalle-valor">${fecha}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Prueba de titularidad</span><span class="detalle-valor">
                    <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-ver-documento-detalle" data-id="${d.id}">
                        <i class="bi bi-file-earmark-text me-1"></i>Ver documento
                    </button>
                </span></div></div>

                <div class="col-12"><p class="detalle-seccion">Solicitante</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre completo</span><span class="detalle-valor">${d.nombre} ${d.apellidos}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID usuario</span><span class="detalle-valor">${d.idUsuario}</span></div></div>
                <div class="col-md-4"></div>

                <div class="col-12"><p class="detalle-seccion">Centro solicitado</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre del centro</span><span class="detalle-valor">${d.nombreCentro}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID centro</span><span class="detalle-valor">${d.idCentro}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Estado</span><span class="detalle-valor d-flex align-items-center gap-2">
                    ${verificadoBadge}
                    ${d.verificadoCentro === 'false' ? `<button class="btn btn-link btn-sm p-0 text-success btn-verificar-centro-detalle" data-id="${d.idCentro}" data-nombre="${d.nombreCentro}" title="Verificar centro"><i class="bi bi-patch-check-fill"></i></button>` : ''}
                </span></div></div>
            </div>`;

        dialogDetalleSolicitud.showModal();
    }

    contenidoDetalleSolicitud.addEventListener('click', async (e) => {
        const btnDoc = e.target.closest('.btn-ver-documento-detalle');
        if (btnDoc) {
            dialogDetalleSolicitud.close();
            verPruebaTitularidad(btnDoc.getAttribute('data-id'));
            return;
        }
        const btnVerificar = e.target.closest('.btn-verificar-centro-detalle');
        if (btnVerificar) {
            dialogDetalleSolicitud.close();
            await verificarCentro(btnVerificar.getAttribute('data-id'), btnVerificar.getAttribute('data-nombre'));
        }
    });

    // Mostrar prueba de titularidad (imagen en dialog, PDF en pestaña nueva)
    async function verPruebaTitularidad(idSolicitud) {
        contenedorPrueba.innerHTML = '<p class="text-secondary">Cargando documento...</p>';

        try {
            const res = await fetch(`/solicitudes-gestion/${idSolicitud}/imagen`, { credentials: 'include' });
            if (!res.ok) throw new Error('No se pudo cargar el documento.');

            const blob = await res.blob();

            if (blob.type === 'application/pdf') {
                const pdfUrl = URL.createObjectURL(blob);
                window.open(pdfUrl, '_blank', 'noopener');
                setTimeout(() => URL.revokeObjectURL(pdfUrl), 10000);
                contenedorPrueba.innerHTML = '';
                return;
            }

            if (objectUrlActivo) URL.revokeObjectURL(objectUrlActivo);
            objectUrlActivo = URL.createObjectURL(blob);
            contenedorPrueba.innerHTML = `<img src="${objectUrlActivo}" alt="Prueba de titularidad" class="img-fluid rounded ef-max-h-60vh">`;
            dialogVerImagen.showModal();
        } catch (err) {
            contenedorPrueba.innerHTML = `<p class="text-danger">${err.message}</p>`;
            dialogVerImagen.showModal();
        }
    }

    document.getElementById("btnCerrarDialogVerImagen").addEventListener("click", () => {
        dialogVerImagen.close();
        if (objectUrlActivo) {
            URL.revokeObjectURL(objectUrlActivo);
            objectUrlActivo = null;
        }
        contenedorPrueba.innerHTML = '';
    });

    // APROBAR SOLICITUD
    async function aprobarSolicitud(id) {
        const ok = await efConfirm({
            title:       'Aprobar solicitud',
            message:     `El usuario pasará a ser <strong>gestor del centro</strong>. ¿Confirmas la aprobación de la solicitud ${id}?`,
            confirmText: 'Aprobar',
            variant:     'primary',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/solicitudes-gestion/${id}/aprobar`, { method: "PUT" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido aprobar la solicitud.`;
            }

            mostrarMensaje("Solicitud aprobada con éxito. El usuario ya es gestor del centro.", true);
            const pagina = tBodySolicitudes.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarSolicitudes(pagina);
        } catch (error) {
            mostrarMensaje(error, false);
        }
    }

    // RECHAZAR SOLICITUD
    async function rechazarSolicitud(id) {
        const ok = await efConfirm({
            title:       'Rechazar solicitud',
            message:     `¿Seguro que quieres rechazar la solicitud con ID <strong>${id}</strong>?`,
            confirmText: 'Rechazar',
            variant:     'danger',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/solicitudes-gestion/${id}/rechazar`, { method: "PUT" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido rechazar la solicitud.`;
            }

            mostrarMensaje("Solicitud rechazada correctamente.", true);
            const pagina = tBodySolicitudes.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarSolicitudes(pagina);
        } catch (error) {
            mostrarMensaje(error, false);
        }
    }


    // DIALOG DETALLES SOLICITANTE

    document.getElementById("btnCerrarDialogDetalleUsuario").addEventListener("click", () => dialogDetalleUsuario.close());
    document.getElementById("btnCerrarDetalleUsuarioFooter").addEventListener("click", () => dialogDetalleUsuario.close());
    dialogDetalleUsuario.addEventListener('click', (e) => { if (e.target === dialogDetalleUsuario) dialogDetalleUsuario.close(); });

    async function verDetalleSolicitante(idUsuario) {
        contenidoDetalleUsuario.innerHTML = '<p class="text-center text-secondary py-3">Cargando datos del solicitante...</p>';
        dialogDetalleUsuario.showModal();

        try {
            const r = await fetch(`/usuarios/${idUsuario}`);
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}`;
            }
            const u = await r.json();

            const roles = u.roles?.length > 0
                ? u.roles.map(r => `<span class="badge text-bg-secondary me-1">${r.nombre}</span>`).join('')
                : '-';
            const estadoBadge = u.activo
                ? '<span class="badge text-bg-success">Activo</span>'
                : '<span class="badge text-bg-danger">Inactivo</span>';

            document.getElementById('btnIrGestionUsuario').href = '/vistas/admin/gestion-usuarios.html?id=' + u.id;
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
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Sexo</span><span class="detalle-valor">${u.sexo || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de nacimiento</span><span class="detalle-valor">${u.fechaNacimiento ? new Date(u.fechaNacimiento).toLocaleDateString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Estado</span><span class="detalle-valor">${estadoBadge}</span></div></div>

                    <div class="col-12"><p class="detalle-seccion">Roles y actividad</p></div>
                    <div class="col-12"><div class="detalle-campo"><span class="detalle-etiqueta">Roles</span><span class="detalle-valor">${roles}</span></div></div>
                    <div class="col-md-6"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de alta</span><span class="detalle-valor">${u.fechaAlta ? new Date(u.fechaAlta).toLocaleString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-6"><div class="detalle-campo"><span class="detalle-etiqueta">Última conexión</span><span class="detalle-valor">${u.ultimaConexion ? new Date(u.ultimaConexion).toLocaleString('es-ES') : '-'}</span></div></div>
                </div>`;
        } catch (error) {
            contenidoDetalleUsuario.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }


    // DIALOG DETALLES CENTRO

    document.getElementById("btnCerrarDialogDetalleCentro").addEventListener("click", () => dialogDetalleCentro.close());
    document.getElementById("btnCerrarDetalleCentroFooter").addEventListener("click", () => dialogDetalleCentro.close());
    dialogDetalleCentro.addEventListener('click', (e) => { if (e.target === dialogDetalleCentro) dialogDetalleCentro.close(); });

    async function verDetalleCentro(idCentro) {
        contenidoDetalleCentro.innerHTML = '<p class="text-center text-secondary py-3">Cargando datos del centro...</p>';
        dialogDetalleCentro.showModal();

        try {
            const r = await fetch(`/centros/${idCentro}`);
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}`;
            }
            const c = await r.json();

            const tipoBadge = c.tipo ? `<span class="badge text-bg-secondary">${c.tipo}</span>` : '-';
            const verificadoBadge = c.verificado
                ? '<span class="badge text-bg-success">Verificado</span>'
                : '<span class="badge text-bg-warning text-dark">No verificado</span>';
            const gestorBadge = c.tieneGestor
                ? '<span class="badge text-bg-info">Con gestor</span>'
                : '<span class="badge text-bg-secondary">Sin gestor</span>';
            const webHtml = c.paginaWeb
                ? `<a href="${c.paginaWeb}" target="_blank" rel="noopener noreferrer" class="text-gradient-primary">${c.paginaWeb}</a>`
                : '-';

            document.getElementById('btnIrGestionCentro').href = c.verificado
                ? '/vistas/admin/gestion-centros.html?id=' + c.id
                : '/vistas/admin/verificacion-centros.html?id=' + c.id;
            contenidoDetalleCentro.innerHTML = `
                <div class="row g-4">
                    <div class="col-12"><p class="detalle-seccion">Identificación</p></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">ID</span><span class="detalle-valor">${c.id}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre comercial</span><span class="detalle-valor">${c.nombreComercial}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Código</span><span class="detalle-valor font-monospace">${c.codigo}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Tipo</span><span class="detalle-valor">${tipoBadge}</span></div></div>

                    <div class="col-12"><p class="detalle-seccion">Contacto y ubicación</p></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Email</span><span class="detalle-valor">${c.email || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Teléfono</span><span class="detalle-valor">${c.telefono || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Página web</span><span class="detalle-valor">${webHtml}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Dirección</span><span class="detalle-valor">${c.direccion || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Localidad</span><span class="detalle-valor">${c.localidad || '-'}</span></div></div>
                    <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Provincia</span><span class="detalle-valor">${c.provincia || '-'}</span></div></div>

                    <div class="col-12"><p class="detalle-seccion">Estado y fechas</p></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Verificación</span><span class="detalle-valor">${verificadoBadge}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Gestión</span><span class="detalle-valor">${gestorBadge}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de alta</span><span class="detalle-valor">${c.fechaAlta ? new Date(c.fechaAlta).toLocaleString('es-ES') : '-'}</span></div></div>
                    <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha modificación</span><span class="detalle-valor">${c.fechaModificacion ? new Date(c.fechaModificacion).toLocaleString('es-ES') : '-'}</span></div></div>

                    ${c.descripcion ? `
                    <div class="col-12"><p class="detalle-seccion">Descripción</p></div>
                    <div class="col-12"><div class="detalle-campo"><span class="detalle-valor">${c.descripcion}</span></div></div>` : ''}

                    ${!c.verificado ? `
                    <div class="col-12">
                        <div class="d-flex justify-content-center pt-3 border-top">
                            <button class="btn btn-outline-success btn-verificar-centro" data-id="${c.id}" data-nombre="${c.nombreComercial}">
                                <i class="bi bi-patch-check me-2"></i>Verificar centro
                            </button>
                        </div>
                    </div>` : ''}
                </div>`;
        } catch (error) {
            contenidoDetalleCentro.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    // Event delegation en contenidoDetalleCentro (botón verificar)
    contenidoDetalleCentro.addEventListener('click', async (e) => {
        const target = e.target.closest('.btn-verificar-centro');
        if (target) {
            dialogDetalleCentro.close();
            await verificarCentro(target.getAttribute('data-id'), target.getAttribute('data-nombre'));
        }
    });

    // VERIFICAR CENTRO DESDE EL DIALOG DE DETALLE
    async function verificarCentro(idCentro, nombreCentro) {
        const ok = await efConfirm({
            title:       'Verificar centro',
            message:     `¿Seguro que quieres verificar <strong>${nombreCentro}</strong>?`,
            confirmText: 'Verificar',
            variant:     'primary',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/centros/${idCentro}/verificar`, { method: "PUT" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido verificar el centro.`;
            }

            dialogDetalleCentro.close();
            mostrarMensaje("Centro verificado con éxito.", true);
            cargarSolicitudes(paginaActual);
        } catch (error) {
            mostrarMensaje(error, false);
        }
    }
});
