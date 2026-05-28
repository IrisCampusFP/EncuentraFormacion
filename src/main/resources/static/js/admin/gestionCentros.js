document.addEventListener('DOMContentLoaded', () => {
    const tBodyCentros = document.getElementById("tBodyCentros");
    const recuadroAlert = document.getElementById("recuadroAlert");
    const dialogCrearCentro = document.getElementById("dialogCrearCentro");
    const dialogEditarCentro = document.getElementById("dialogEditarCentro");
    const dialogDetalleCentro = document.getElementById("dialogDetalleCentro");
    const contenidoDetalleCentro = document.getElementById("contenidoDetalleCentro");
    const dialogGestoresCentro = document.getElementById("dialogGestoresCentro");
    const contenidoGestoresCentro = document.getElementById("contenidoGestoresCentro");
    const msgCrearError = document.getElementById("msgCrearError");

    let comboboxCrear  = null;
    let comboboxEditar = null;
    let codigoOriginalEditar = null;

    const filtroBusqueda   = document.getElementById("filtroBusqueda");
    const filtroTipo       = document.getElementById("filtroTipo");
    const filtroGestor     = document.getElementById("filtroGestor");
    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");

    // Mostrar nombre del usuario (admin) en el title
    async function inicializarTitle() {
        try {
            const r = await fetch("/perfil", { method: "GET", credentials: "include" });
            const data = await r.json();
            document.title = `Gestión de centros (${data.nombre})`;
        } catch (error) {
            console.error('Error al inicializar el título', error);
        }
    }
    inicializarTitle();

    let paginaActual = 0;
    let debounceTimer;

    filtroBusqueda.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => { paginaActual = 0; cargarCentros(0); }, 400);
    });
    filtroTipo.addEventListener('change',   () => { paginaActual = 0; cargarCentros(0); });
    filtroGestor.addEventListener('change', () => { paginaActual = 0; cargarCentros(0); });

    btnLimpiarFiltros.addEventListener('click', () => {
        filtroBusqueda.value = '';
        filtroTipo.value     = '';
        filtroGestor.value   = '';
        paginaActual = 0;
        cargarCentros(0);
    });

    const idParamUrl = new URLSearchParams(window.location.search).get('id');
    if (idParamUrl) filtroBusqueda.value = idParamUrl;

    cargarCentros();

    fetch("/provincias")
        .then(r => r.json())
        .then(provincias => {
            comboboxCrear  = crearCombobox(provincias, 'comboProvinciaCrear',  'provinciaCrear',  'comboProvinciaCrearWrap',  'comboProvinciaCrearLista',  'comboProvinciaCrear-clear');
            comboboxEditar = crearCombobox(provincias, 'comboProvinciaEditar', 'provinciaEditar', 'comboProvinciaEditarWrap', 'comboProvinciaEditarLista', 'comboProvinciaEditar-clear');
        })
        .catch(() => console.error('Error al cargar provincias'));

    // Validación de código en blur (crear): formato + unicidad
    document.getElementById("codigoCrear").addEventListener("blur", async () => {
        const input = document.getElementById("codigoCrear");
        const error = document.getElementById("codigoCrearError");
        const val   = input.value.trim();
        error.textContent = "";
        input.classList.remove("is-invalid");
        if (!val) return;
        if (!/^\d{8}$/.test(val)) {
            error.textContent = "El código debe tener exactamente 8 dígitos numéricos.";
            input.classList.add("is-invalid");
            return;
        }
        try {
            const r = await fetch(`/centros/existe?codigo=${encodeURIComponent(val)}`);
            if (r.ok) {
                const centro = await r.json();
                error.textContent = `Ya existe un centro con ese código: ${centro.nombreComercial}.`;
                input.classList.add("is-invalid");
            }
        } catch (_) {}
    });

    // Validación de código en blur (editar): formato + unicidad si cambió
    document.getElementById("codigoEditar").addEventListener("blur", async () => {
        const input = document.getElementById("codigoEditar");
        const error = document.getElementById("codigoEditarError");
        const val   = input.value.trim();
        error.textContent = "";
        input.classList.remove("is-invalid");
        if (!val) return;
        if (!/^\d{8}$/.test(val)) {
            error.textContent = "El código debe tener exactamente 8 dígitos numéricos.";
            input.classList.add("is-invalid");
            return;
        }
        if (val === codigoOriginalEditar) return;
        try {
            const r = await fetch(`/centros/existe?codigo=${encodeURIComponent(val)}`);
            if (r.ok) {
                const centro = await r.json();
                error.textContent = `Ya existe un centro con ese código: ${centro.nombreComercial}.`;
                input.classList.add("is-invalid");
            }
        } catch (_) {}
    });

    // Listeners para los botones de la tabla y clic en fila para detalle
    tBodyCentros.addEventListener('click', async (e) => {
        const tdGestores = e.target.closest('td.td-gestores');
        if (tdGestores) {
            await abrirGestoresCentro(tdGestores.dataset.id);
            return;
        }

        const target = e.target.closest('button, a') ?? e.target;
        if (target.classList.contains('btn-editar-centro')) {
            await cargarDialogEditar(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-quitar-verificacion')) {
            await quitarVerificacion(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-eliminar-centro')) {
            await eliminarCentro(target.getAttribute('data-id'));
        } else {
            const fila = e.target.closest('tr.ef-fila-clicable');
            if (fila) await abrirDetalleCentro(fila.dataset.id);
        }
    });

    // CARGAR LOS DATOS DE LOS CENTROS EN EL BODY DE LA TABLA
    async function cargarCentros(page = 0, size = 10) {
        tBodyCentros.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-3">Cargando centros...</td></tr>';

        const params = new URLSearchParams({ page, size });
        const q           = filtroBusqueda.value.trim();
        const tipo        = filtroTipo.value;
        const tieneGestor = filtroGestor.value;
        if (q) {
            const esId = /^\d+$/.test(q);
            if (esId) params.set('id', q); else params.set('q', q);
        }
        if (tipo)        params.set('tipo', tipo);
        if (tieneGestor) params.set('tieneGestor', tieneGestor);

        try {
            const response = await fetch(`/centros/verificados?${params.toString()}`, { method: "GET" });
            if (!response.ok) {
                const data = await response.json();
                throw data.errorMsg || `Error ${response.status}: No se ha podido cargar la lista de centros.`;
            }
            const paginaData = await response.json();
            const centros = paginaData.content || [];

            if (centros.length === 0) {
                tBodyCentros.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">No hay centros verificados.</td></tr>';
                renderPaginacion(0, 0, 0);
                return;
            }

            // Se muestran los datos de cada centro fila por fila (tr) en el tBody
            let tBody = "";
            centros.forEach(c => {
                const webCell = c.paginaWeb
                    ? '<a href="' + c.paginaWeb + '" target="_blank" class="text-gradient-sucess">' + c.paginaWeb + '</a>'
                    : '-';
                const tdGestorClass = c.tieneGestor ? 'td-gestores' : '';
                const tdGestorAttrs = c.tieneGestor ? ' style="cursor:pointer;" title="Ver gestores"' : '';
                const tdGestorBadge = c.tieneGestor
                    ? '<span class="badge text-bg-info">Sí</span>'
                    : '<span class="badge text-bg-secondary">No</span>';
                tBody += `
                <tr id="tr-centro-${c.id}" class="ef-fila-clicable" data-id="${c.id}" title="Clic para ver detalle">
                    <td>${c.id}</td>
                    <td>${c.nombreComercial}</td>
                    <td class="text-nowrap">${c.codigo}</td>
                    <td class="text-nowrap">${c.tipo}</td>
                    <td>${c.email || "-"}</td>
                    <td class="text-nowrap">${c.telefono || "-"}</td>
                    <td>${c.direccion}</td>
                    <td>${c.localidad}</td>
                    <td>${c.provincia}</td>
                    <td>${webCell}</td>
                    <td>${c.fechaAlta ? new Date(c.fechaAlta).toLocaleString() : "-"}</td>
                    <td>${c.fechaModificacion ? new Date(c.fechaModificacion).toLocaleString() : "-"}</td>
                    <td>${c.fechaVerificacion ? new Date(c.fechaVerificacion).toLocaleString() : "-"}</td>
                    <td class="${tdGestorClass}" data-id="${c.id}"${tdGestorAttrs}>${tdGestorBadge}</td>
                    <td class="text-nowrap">
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-editar-centro" data-id="${c.id}"><i class="bi bi-pencil me-1"></i>Editar</button>
                            <button class="btn btn-sm btn-outline-warning ef-btn-accion btn-quitar-verificacion" data-id="${c.id}"><span class="ef-icon-slash me-1"><i class="bi bi-patch-check"></i></span>Desverificar</button>
                            <button class="btn btn-sm btn-outline-danger ef-btn-accion btn-eliminar-centro" data-id="${c.id}"><i class="bi bi-trash me-1"></i>Eliminar</button>
                        </div>
                    </td>
                </tr>`;
            });
            tBodyCentros.innerHTML = tBody;
            paginaActual = paginaData.page.number;
            renderPaginacion(paginaData.page.number, paginaData.page.totalPages, paginaData.page.totalElements);
        } catch (error) {
            mostrarError(error);
        }
    }

    function renderPaginacion(currentPage, totalPages, totalElements) {
        const container = document.getElementById("paginacionCentros");
        if (!container) return;

        const total = totalElements ?? 0;
        const pages = totalPages ?? 1;

        if (pages <= 1) {
            container.innerHTML = `<span class="text-secondary small">${total} centro${total !== 1 ? 's' : ''} en total</span>`;
            return;
        }

        container.innerHTML = `
            <nav aria-label="Paginación de centros">
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
            <span class="text-secondary small">${total} centro${total !== 1 ? 's' : ''} en total</span>`;

        if (currentPage > 0)
            document.getElementById("btnPaginaAnterior").addEventListener('click', () => { paginaActual = currentPage - 1; cargarCentros(currentPage - 1); });
        if (currentPage < pages - 1)
            document.getElementById("btnPaginaSiguiente").addEventListener('click', () => { paginaActual = currentPage + 1; cargarCentros(currentPage + 1); });
    }

    // Se muestra el error correspondiente en el recuadro durante 3 segs
    function mostrarError(errorMsg) {
        recuadroAlert.textContent = errorMsg;
        recuadroAlert.classList.remove("d-none");

        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 3000);
    }

    // DIALOG DE DETALLE (solo lectura, se abre al clicar una fila)

    document.getElementById("btnCerrarDialogDetalleCentro").addEventListener('click', () => dialogDetalleCentro.close());
    document.getElementById("btnCerrarDetalleCentroFooter").addEventListener('click', () => dialogDetalleCentro.close());
    dialogDetalleCentro.addEventListener('click', (e) => {
        if (e.target === dialogDetalleCentro) dialogDetalleCentro.close();
    });

    // DIALOG DE GESTORES (se abre desde la columna Gestionado de la tabla)

    document.getElementById("btnCerrarDialogGestores").addEventListener('click', () => dialogGestoresCentro.close());
    dialogGestoresCentro.addEventListener('click', (e) => {
        if (e.target === dialogGestoresCentro) dialogGestoresCentro.close();
    });

    async function abrirGestoresCentro(idCentro) {
        contenidoGestoresCentro.innerHTML = '<p class="text-center text-secondary py-3">Cargando gestores...</p>';
        dialogGestoresCentro.showModal();

        try {
            const r = await fetch(`/centros/${idCentro}/gestores`);
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}`;
            }
            const gestores = await r.json();

            if (gestores.length === 0) {
                contenidoGestoresCentro.innerHTML = '<p class="text-center text-secondary py-3">Este centro no tiene gestores asignados.</p>';
                return;
            }

            contenidoGestoresCentro.innerHTML = `<div class="ef-gestores-list">${gestores.map(renderGestorItem).join('')}</div>`;
        } catch (error) {
            contenidoGestoresCentro.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    async function abrirDetalleCentro(id) {
        contenidoDetalleCentro.innerHTML = '<p class="text-center text-secondary py-3">Cargando datos...</p>';
        dialogDetalleCentro.showModal();

        try {
            const [rCentro, rGestores] = await Promise.all([
                fetch(`/centros/${id}`),
                fetch(`/centros/${id}/gestores`)
            ]);
            if (!rCentro.ok) {
                const data = await rCentro.json();
                throw data.errorMsg || `Error ${rCentro.status}`;
            }
            const [c, gestores] = await Promise.all([rCentro.json(), rGestores.ok ? rGestores.json() : []]);
            contenidoDetalleCentro.innerHTML = renderHtmlDetalleCentro(c, gestores);
        } catch (error) {
            contenidoDetalleCentro.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    function renderGestorItem(g) {
        const iniciales = `${g.nombre.charAt(0)}${g.apellidos.charAt(0)}`.toUpperCase();
        const url = '/vistas/admin/gestion-usuarios.html?id=' + g.id;
        return `
            <a href="${url}" class="ef-gestor-item" title="Ver en gestión de usuarios">
                <span class="ef-gestor-avatar">${iniciales}</span>
                <span class="ef-gestor-info">
                    <span class="ef-gestor-nombre">${g.nombre} ${g.apellidos}</span>
                    <span class="ef-gestor-email">${g.email}</span>
                </span>
                <i class="bi bi-arrow-right-circle ef-gestor-arrow"></i>
            </a>`;
    }

    function renderHtmlDetalleCentro(c, gestores = []) {
        const tipoBadge = c.tipo ? `<span class="badge text-bg-secondary">${c.tipo}</span>` : '-';
        const verificadoBadge = c.verificado
            ? '<span class="badge text-bg-success">Verificado</span>'
            : '<span class="badge text-bg-warning text-dark">No verificado</span>';
        const gestorBadge = '<span class="badge text-bg-secondary">Sin gestor</span>';
        const webHtml = c.paginaWeb
            ? `<a href="${c.paginaWeb}" target="_blank" rel="noopener noreferrer" class="text-gradient-primary">${c.paginaWeb}</a>`
            : '-';

        return `
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
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Gestión</span><span class="detalle-valor">${gestores.length > 0 ? gestores.map(g => '<a href=' + g.id + '"/vistas/admin/gestion-usuarios.html?id=" class="ef-gestor-enlace">' + g.nombre + ' ' + g.apellidos + '</a>').join('<br>') : gestorBadge}</span></div></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de alta</span><span class="detalle-valor">${c.fechaAlta ? new Date(c.fechaAlta).toLocaleString('es-ES') : '-'}</span></div></div>
                <div class="col-md-3"><div class="detalle-campo"><span class="detalle-etiqueta">Última modificación</span><span class="detalle-valor">${c.fechaModificacion ? new Date(c.fechaModificacion).toLocaleString('es-ES') : '-'}</span></div></div>
                ${c.fechaVerificacion ? `<div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha verificación</span><span class="detalle-valor">${new Date(c.fechaVerificacion).toLocaleString('es-ES')}</span></div></div>` : ''}

                ${c.descripcion ? `
                <div class="col-12"><p class="detalle-seccion">Descripción</p></div>
                <div class="col-12"><p class="detalle-valor mb-0">${c.descripcion}</p></div>` : ''}

            </div>`;
    }

    // CREAR NUEVO CENTRO

    // Al pulsar el botón 'Crear nuevo centro' se abre el modal (dialog) con el formulario
    document.getElementById("btnCrearCentro").addEventListener('click', () => {
        dialogCrearCentro.showModal();
    });

    document.getElementById("btnCancelarCrearCentro").addEventListener('click', () => {
        dialogCrearCentro.close();
    });

    // Al enviar el formulario se llama a la función crear centro
    document.getElementById("formCrearCentro").addEventListener('submit', (e) => {
        e.preventDefault();
        crearCentro();
    });

    // Función que envía al backend los datos del nuevo centro, se guardan y se recarga la tabla
    async function crearCentro() {
        const codigoCrear = document.getElementById("codigoCrear").value.trim();
        if (!/^\d{8}$/.test(codigoCrear)) {
            msgCrearError.textContent = "El código de centro debe tener exactamente 8 dígitos numéricos.";
            msgCrearError.classList.remove("d-none");
            return;
        }

        const provinciaVal = comboboxCrear?.valor;
        if (!provinciaVal) {
            msgCrearError.textContent = "Debes seleccionar una provincia de la lista.";
            msgCrearError.classList.remove("d-none");
            return;
        }

        try {
            const r = await fetch("/centros", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nombreComercial: document.getElementById("nombreCrear").value.trim(),
                    codigo: codigoCrear,
                    tipo: document.getElementById("tipoCrear").value,
                    email: document.getElementById("emailCrear")?.value?.trim() || null,
                    telefono: document.getElementById("telefonoCrear")?.value?.trim() || null,
                    direccion: document.getElementById("direccionCrear").value.trim(),
                    localidad: document.getElementById("localidadCrear").value.trim(),
                    provincia: provinciaVal,
                    web: document.getElementById("webCrear")?.value?.trim() || null,
                    descripcion: document.getElementById("descripcionCrear")?.value?.trim() || null
                })
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido crear el centro.`;
            }

            dialogCrearCentro.close();
            await cargarCentros(0); // nuevo centro → primera página
        } catch (error) {
            msgCrearError.textContent = error;
            msgCrearError.classList.remove("d-none");
        }
    }

    dialogCrearCentro.addEventListener("close", () => {
        document.getElementById("formCrearCentro").reset();
        comboboxCrear?.limpiar();
        msgCrearError.classList.add("d-none");
        const codigoInput = document.getElementById("codigoCrear");
        codigoInput.classList.remove("is-invalid");
        document.getElementById("codigoCrearError").textContent = "";
    });

    // EDITAR CENTRO

    // Al pulsar el botón editar se llama a esta función que obtiene los datos del centro por su id
    async function cargarDialogEditar(id) {
        try {
            const r = await fetch(`/centros/${id}`, { method: "GET" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se han podido cargar los datos del centro.`;
            }
            const c = await r.json();

            codigoOriginalEditar = c.codigo;
            document.getElementById("idCentroEditado").value = c.id;
            document.getElementById("nombreEditar").value = c.nombreComercial;
            document.getElementById("codigoEditar").value = c.codigo;
            document.getElementById("tipoEditar").value = c.tipo;
            document.getElementById("emailEditar").value = c.email || "";
            document.getElementById("telefonoEditar").value = c.telefono || "";
            document.getElementById("direccionEditar").value = c.direccion;
            document.getElementById("localidadEditar").value = c.localidad;
            comboboxEditar?.preseleccionar(c.provincia);
            document.getElementById("webEditar").value = c.paginaWeb || "";
            document.getElementById("descripcionEditar").value = c.descripcion || "";

            dialogEditarCentro.showModal(); // Una vez cargados los datos se muestra el dialog
        } catch (error) {
            mostrarError(error);
        }
    }

    document.getElementById("btnCancelarEditarCentro").addEventListener('click', () => {
        dialogEditarCentro.close();
    });

    // Al enviar el formulario se llama a la función editar centro
    document.getElementById("formEditarCentro").addEventListener('submit', (e) => {
        e.preventDefault();
        const id = document.getElementById("idCentroEditado").value;
        editarCentro(id);
    });

    // Se hace un fetch (metodo PUT) que envía los datos actualizados al backend.
    async function editarCentro(id) {
        const editError    = document.getElementById("editError");
        const codigoEditar = document.getElementById("codigoEditar").value.trim();
        if (!/^\d{8}$/.test(codigoEditar)) {
            editError.textContent = "El código de centro debe tener exactamente 8 dígitos numéricos.";
            editError.classList.remove("d-none");
            return;
        }

        const provinciaVal = comboboxEditar?.valor;
        if (!provinciaVal) {
            editError.textContent = "Debes seleccionar una provincia de la lista.";
            editError.classList.remove("d-none");
            return;
        }

        try {
            const r = await fetch(`/centros/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nombreComercial: document.getElementById("nombreEditar").value.trim(),
                    codigo: codigoEditar,
                    tipo: document.getElementById("tipoEditar").value,
                    email: document.getElementById("emailEditar").value.trim() || null,
                    telefono: document.getElementById("telefonoEditar").value.trim() || null,
                    direccion: document.getElementById("direccionEditar").value.trim(),
                    localidad: document.getElementById("localidadEditar").value.trim(),
                    provincia: provinciaVal,
                    paginaWeb: document.getElementById("webEditar").value.trim() || null,
                    descripcion: document.getElementById("descripcionEditar").value.trim() || null
                })
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido actualizar el centro.`;
            }

            dialogEditarCentro.close();
            await cargarCentros(paginaActual);
        } catch (error) {
            editError.textContent = error;
            editError.classList.remove("d-none");
        }
    }

    dialogEditarCentro.addEventListener("close", () => {
        document.getElementById("formEditarCentro").reset();
        comboboxEditar?.limpiar();
        document.getElementById("editError").classList.add("d-none");
        const codigoInput = document.getElementById("codigoEditar");
        codigoInput.classList.remove("is-invalid");
        document.getElementById("codigoEditarError").textContent = "";
    });

    // QUITAR VERIFICACIÓN

    async function quitarVerificacion(id) {
        const ok = await efConfirm({
            title:       'Quitar verificación',
            message:     `¿Seguro que quieres retirar la verificación del centro con ID <strong>${id}</strong>?`,
            confirmText: 'Quitar verificación',
            variant:     'warning',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/centros/${id}/quitar-verificacion`, { method: "PUT" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido quitar la verificación.`;
            }

            const pagina = tBodyCentros.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarCentros(pagina);
        } catch (error) {
            mostrarError(error || `Error al quitar verificación al centro con id: ${id}`);
        }
    }

    // ELIMINAR CENTRO

    async function eliminarCentro(id) {
        const ok = await efConfirm({
            title:       'Eliminar centro',
            message:     `Esta acción es <strong>irreversible</strong>. ¿Seguro que quieres eliminar el centro con ID <strong>${id}</strong>?`,
            confirmText: 'Eliminar',
            variant:     'danger',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/centros/${id}`, { method: "DELETE" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido eliminar el centro.`;
            }

            const pagina = tBodyCentros.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarCentros(pagina);
        } catch (error) {
            mostrarError(error || `Error al eliminar el centro con id: ${id}`);
        }
    }
});
