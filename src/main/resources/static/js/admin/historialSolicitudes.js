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
    const filtroEstado      = document.getElementById("filtroEstado");
    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");

    // Mostrar nombre del usuario (admin) en el title
    async function inicializarTitle() {
        try {
            const r = await fetch("/perfil", { method: "GET", credentials: "include" });
            const data = await r.json();
            document.title = `Historial Solicitudes (${data.nombre})`;
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
    filtroEstado.addEventListener('change', () => { paginaActual = 0; cargarSolicitudes(0); });

    btnLimpiarFiltros.addEventListener('click', () => {
        filtroBusqueda.value = '';
        filtroEstado.value   = '';
        paginaActual = 0;
        cargarSolicitudes(0);
    });

    cargarSolicitudes();

    // Event delegation para la tabla
    tBodySolicitudes.addEventListener('click', async (e) => {
        const target = e.target.closest('button, .campo-clickable') ?? e.target;
        if (target.classList.contains('btn-ver-documento')) {
            verPruebaTitularidad(target.getAttribute('data-id'));
        } else if (target.classList.contains('td-detalle-solicitante')) {
            await verDetalleSolicitante(target.getAttribute('data-id'));
        } else if (target.classList.contains('td-detalle-centro')) {
            await verDetalleCentro(target.getAttribute('data-id'));
        } else {
            const fila = e.target.closest('tr.ef-fila-clicable');
            if (fila) abrirDetalleSolicitud(fila.dataset);
        }
    });

    // DIALOG DETALLE SOLICITUD
    document.getElementById("btnCerrarDialogDetalleSolicitud").addEventListener("click", () => dialogDetalleSolicitud.close());
    document.getElementById("btnCerrarDetalleSolicitudFooter").addEventListener("click", () => dialogDetalleSolicitud.close());
    dialogDetalleSolicitud.addEventListener('click', (e) => { if (e.target === dialogDetalleSolicitud) dialogDetalleSolicitud.close(); });

    function abrirDetalleSolicitud(d) {
        const fecha = d.fecha ? new Date(d.fecha).toLocaleString('es-ES') : '-';
        const fechaResolucion = d.fechaResolucion ? new Date(d.fechaResolucion).toLocaleString('es-ES') : '-';
        const verificadoBadge = d.verificadoCentro === 'true'
            ? '<span class="badge text-bg-success">Verificado</span>'
            : '<span class="badge text-bg-warning text-dark">No verificado</span>';

        contenidoDetalleSolicitud.innerHTML = `
            <div class="row g-4">
                <div class="col-12"><p class="detalle-seccion">Información general</p></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">ID solicitud</span><span class="detalle-valor">${d.id}</span></div></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de solicitud</span><span class="detalle-valor">${fecha}</span></div></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha resolución</span><span class="detalle-valor">${fechaResolucion}</span></div></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Estado</span><span class="detalle-valor">${obtenerBadgeEstado(d.estado)}</span></div></div>

                <div class="col-12"><p class="detalle-seccion">Solicitante</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre completo</span><span class="detalle-valor">${d.nombre} ${d.apellidos}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID usuario</span><span class="detalle-valor">${d.idUsuario}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Prueba de titularidad</span><span class="detalle-valor">
                    <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-ver-documento-detalle" data-id="${d.id}">
                        <i class="bi bi-file-earmark-text me-1"></i>Ver documento
                    </button>
                </span></div></div>

                <div class="col-12"><p class="detalle-seccion">Centro solicitado</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Nombre del centro</span><span class="detalle-valor">${d.nombreCentro}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">ID centro</span><span class="detalle-valor">${d.idCentro}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Estado verificación</span><span class="detalle-valor">${verificadoBadge}</span></div></div>
            </div>`;

        dialogDetalleSolicitud.showModal();
    }

    contenidoDetalleSolicitud.addEventListener('click', (e) => {
        const btnDoc = e.target.closest('.btn-ver-documento-detalle');
        if (btnDoc) {
            dialogDetalleSolicitud.close();
            verPruebaTitularidad(btnDoc.getAttribute('data-id'));
        }
    });

    // CSP: los cierres de diálogos se gestionan mediante event listeners para cumplir con la política de seguridad.
    const btnCerrarImagen = document.getElementById('btnCerrarDialogImagen');
    const btnCerrarUsuario = document.getElementById('btnCerrarDialogUsuario');
    const btnCerrarCentro = document.getElementById('btnCerrarDialogCentro');
    if (btnCerrarImagen) btnCerrarImagen.addEventListener('click', () => {
        dialogVerImagen.close();
        if (objectUrlActivo) { URL.revokeObjectURL(objectUrlActivo); objectUrlActivo = null; }
        contenedorPrueba.innerHTML = '';
    });
    if (btnCerrarUsuario) btnCerrarUsuario.addEventListener('click', () => dialogDetalleUsuario.close());
    if (btnCerrarCentro) btnCerrarCentro.addEventListener('click', () => dialogDetalleCentro.close());

    dialogDetalleUsuario.addEventListener('click', (e) => { if (e.target === dialogDetalleUsuario) dialogDetalleUsuario.close(); });
    dialogDetalleCentro.addEventListener('click', (e) => { if (e.target === dialogDetalleCentro) dialogDetalleCentro.close(); });

    const btnCerrarUsuarioFooter = document.getElementById('btnCerrarDialogUsuarioFooter');
    const btnCerrarCentroFooter = document.getElementById('btnCerrarDialogCentroFooter');
    if (btnCerrarUsuarioFooter) btnCerrarUsuarioFooter.addEventListener('click', () => dialogDetalleUsuario.close());
    if (btnCerrarCentroFooter) btnCerrarCentroFooter.addEventListener('click', () => dialogDetalleCentro.close());

    // Event delegation para el contenido del modal de centro
    contenidoDetalleCentro.addEventListener('click', async (e) => {
        const target = e.target;
        if (target.classList.contains('btn-verificar-centro')) {
            await verificarCentro(target.getAttribute('data-id'));
        }
    });

    // CARGAR TODAS LAS SOLICITUDES (HISTORIAL)
    async function cargarSolicitudes(page = 0, size = 10) {
        tBodySolicitudes.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-3">Cargando solicitudes...</td></tr>';

        const params = new URLSearchParams({ page, size });
        const q      = filtroBusqueda.value.trim();
        const estado = filtroEstado.value;
        if (q)      params.set('q', q);
        if (estado) params.set('estado', estado);

        try {
            const response = await fetch(`/solicitudes-gestion?${params.toString()}`, { method: "GET" });
            if (!response.ok) {
                const data = await response.json();
                throw data.errorMsg || `Error ${response.status}: No se ha podido cargar el historial de solicitudes.`;
            }
            const paginaData = await response.json();
            const solicitudes = paginaData.content || [];

            if (solicitudes.length === 0) {
                tBodySolicitudes.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">No hay solicitudes en el historial.</td></tr>';
                renderPaginacion(0, 0, 0);
                return;
            }

            let tBody = "";
            solicitudes.forEach(s => {
                // Se determina el badge de color según el estado de la solicitud
                const badgeEstado = obtenerBadgeEstado(s.estado);

                tBody += `
                <tr class="ef-fila-clicable"
                    data-id="${s.id}"
                    data-fecha="${s.fechaSolicitud || ''}"
                    data-fecha-resolucion="${s.fechaResolucion || ''}"
                    data-estado="${s.estado || ''}"
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
                    <td>${badgeEstado}</td>
                    <td>${s.fechaResolucion ? new Date(s.fechaResolucion).toLocaleString() : "-"}</td>
                    <td class="text-nowrap">
                        <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-ver-documento" data-id="${s.id}"><i class="bi bi-file-earmark-text me-1"></i>Ver documento</button>
                    </td>
                </tr>`;
            });
            tBodySolicitudes.innerHTML = tBody;
            paginaActual = paginaData.page.number;
            renderPaginacion(paginaData.page.number, paginaData.page.totalPages, paginaData.page.totalElements);
        } catch (error) {
            mostrarError(error, false);
        }
    }

    function renderPaginacion(currentPage, totalPages, totalElements) {
        const container = document.getElementById("paginacionHistorial");
        if (!container) return;

        const total = totalElements ?? 0;
        const pages = totalPages ?? 1;

        if (pages <= 1) {
            container.innerHTML = `<span class="text-secondary small">${total} solicitud${total !== 1 ? 'es' : ''} en el historial</span>`;
            return;
        }

        container.innerHTML = `
            <nav aria-label="Paginación del historial">
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
            <span class="text-secondary small">${total} solicitud${total !== 1 ? 'es' : ''} en el historial</span>`;

        if (currentPage > 0)
            document.getElementById("btnPaginaAnterior").addEventListener('click', () => cargarSolicitudes(currentPage - 1));
        if (currentPage < pages - 1)
            document.getElementById("btnPaginaSiguiente").addEventListener('click', () => cargarSolicitudes(currentPage + 1));
    }

    // Se obtiene el badge de color según el estado de la solicitud
    function obtenerBadgeEstado(estado) {
        const s = 'class="ef-fs-xs ef-fw-500"';
        switch (estado) {
            case "ACEPTADA":  return `<span class="badge bg-success" ${s}>Aceptada</span>`;
            case "RECHAZADA": return `<span class="badge bg-danger" ${s}>Rechazada</span>`;
            case "PENDIENTE": return `<span class="badge bg-warning text-dark" ${s}>Pendiente</span>`;
            case "CANCELADA": return `<span class="badge bg-secondary" ${s}>Cancelada</span>`;
            default: return `<span class="badge bg-secondary" ${s}>${estado || "-"}</span>`;
        }
    }

    function mostrarError(msg, success = false) {
        recuadroAlert.textContent = msg;

        if (success) {
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

    // DIALOG DETALLES SOLICITANTE

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

            document.getElementById('btnIrGestionCentro').href = '/vistas/admin/gestion-centros.html?id=' + c.id;
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
                            <button class="btn btn-outline-success btn-verificar-centro" data-id="${c.id}">
                                <i class="bi bi-patch-check me-2"></i>Verificar centro
                            </button>
                        </div>
                    </div>` : ''}
                </div>`;
        } catch (error) {
            contenidoDetalleCentro.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    // VERIFICAR CENTRO DESDE EL DIALOG DE DETALLE

    async function verificarCentro(idCentro) {
        const ok = await efConfirm({
            title:       'Verificar centro',
            message:     `¿Seguro que quieres verificar el centro con ID <strong>${idCentro}</strong>?`,
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
            mostrarError("Centro verificado con éxito.", true);
            await cargarSolicitudes();
        } catch (error) {
            mostrarError(error);
        }
    }
});
