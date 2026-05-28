document.addEventListener('DOMContentLoaded', () => {
    const tBodyCentros = document.getElementById("tBodyCentros");
    const recuadroAlert = document.getElementById("recuadroAlert");
    const dialogEditarCentro = document.getElementById("dialogEditarCentro");
    const dialogDetalleCentro = document.getElementById("dialogDetalleCentro");
    const contenidoDetalleCentro = document.getElementById("contenidoDetalleCentro");

    let comboboxEditar = null;
    let codigoOriginalEditar = null;

    const filtroBusqueda    = document.getElementById("filtroBusqueda");
    const filtroTipo        = document.getElementById("filtroTipo");
    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");

    // Listeners cerrar dialogos
    document.getElementById("btnCerrarDialogEditarCentro").addEventListener("click", () => dialogEditarCentro.close());
    dialogEditarCentro.addEventListener("close", () => {
        document.getElementById("formEditarCentro").reset();
        comboboxEditar?.limpiar();
        document.getElementById("editError").classList.add("d-none");
        const codigoInput = document.getElementById("codigoEditar");
        codigoInput.classList.remove("is-invalid");
        document.getElementById("codigoEditarError").textContent = "";
    });
    document.getElementById("btnCerrarDialogDetalleCentro").addEventListener('click', () => dialogDetalleCentro.close());
    document.getElementById("btnCerrarDetalleCentroFooter").addEventListener('click', () => dialogDetalleCentro.close());
    dialogDetalleCentro.addEventListener('click', (e) => { if (e.target === dialogDetalleCentro) dialogDetalleCentro.close(); });

    // Mostrar nombre del usuario (admin) en el title
    async function inicializarTitle() {
        try {
            const r = await fetch("/perfil", { method: "GET", credentials: "include" });
            const data = await r.json();
            document.title = `Verificación de centros (${data.nombre})`;
        } catch (error) {
            console.error('Error al cargar título:', error);
        }
    }
    inicializarTitle();

    let paginaActual = 0;
    let debounceTimer;

    filtroBusqueda.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => { paginaActual = 0; cargarCentros(0); }, 400);
    });
    filtroTipo.addEventListener('change', () => { paginaActual = 0; cargarCentros(0); });

    btnLimpiarFiltros.addEventListener('click', () => {
        filtroBusqueda.value = '';
        filtroTipo.value     = '';
        paginaActual = 0;
        cargarCentros(0);
    });

    const idParamUrl = new URLSearchParams(window.location.search).get('id');
    if (idParamUrl) filtroBusqueda.value = idParamUrl;

    cargarCentros();

    fetch("/provincias")
        .then(r => r.json())
        .then(provincias => {
            comboboxEditar = crearCombobox(provincias, 'comboProvinciaEditar', 'provinciaEditar', 'comboProvinciaEditarWrap', 'comboProvinciaEditarLista', 'comboProvinciaEditar-clear');
        })
        .catch(() => console.error('Error al cargar provincias'));

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
        const target = e.target.closest('button, a') ?? e.target;

        if (target.classList.contains('btn-editar-centro')) {
            await cargarDialogEditar(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-verificar-centro')) {
            await verificarCentro(target.getAttribute('data-id'));
        } else if (target.classList.contains('btn-eliminar-centro')) {
            await eliminarCentro(target.getAttribute('data-id'));
        } else {
            const fila = e.target.closest('tr.ef-fila-clicable');
            if (fila) await abrirDetalleCentro(fila.dataset.id);
        }
    });

    // CARGAR LOS DATOS DE LOS CENTROS SIN VERIFICAR EN EL BODY DE LA TABLA
    async function cargarCentros(page = 0, size = 10) {
        tBodyCentros.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-3">Cargando centros no verificados...</td></tr>';

        const params = new URLSearchParams({ page, size });
        const q    = filtroBusqueda.value.trim();
        const tipo = filtroTipo.value;
        const esId = q && /^\d+$/.test(q);
        if (esId) params.set('id', q); else if (q) params.set('q', q);
        if (tipo) params.set('tipo', tipo);

        try {
            const response = await fetch(`/centros/sin-verificar?${params.toString()}`, { method: "GET" });
            if (!response.ok) {
                const data = await response.json();
                throw data.errorMsg || `Error ${response.status}: No se ha podido cargar la lista de centros.`;
            }

            const paginaData = await response.json();
            const centros = paginaData.content || [];

            if (centros.length === 0) {
                tBodyCentros.innerHTML = '<tr><td colspan="100%" class="text-center text-secondary py-4 fs-6">No hay centros pendientes de verificación.</td></tr>';
                renderPaginacion(0, 0, 0);
                return;
            }

            // Se muestran los datos de cada centro fila por fila
            let tBody = "";
            centros.forEach(c => {
                tBody += `
                    <tr id="tr-centro-${c.id}" class="ef-fila-clicable" data-id="${c.id}" title="Clic para ver detalle">
                        <td>${c.id}</td>
                        <td>${c.nombreComercial}</td>
                        <td>${c.codigo}</td>
                        <td>${c.tipo}</td>
                        <td>${c.email || "-"}</td>
                        <td class="text-nowrap">${c.telefono || "-"}</td>
                        <td>${c.direccion}</td>
                        <td>${c.localidad}</td>
                        <td>${c.provincia}</td>
                        <td>${c.paginaWeb ? `<a href="${c.paginaWeb}" target="_blank">${c.paginaWeb}</a>` : "-"}</td>
                        <td>${c.fechaAlta ? new Date(c.fechaAlta).toLocaleString() : "-"}</td>
                        <td>${c.fechaModificacion ? new Date(c.fechaModificacion).toLocaleString() : "-"}</td>
                        <!-- Botones para acciones CRUD -->
                        <td class="text-nowrap">
                            <div class="d-flex gap-2 justify-content-center">
                                <button class="btn btn-sm btn-outline-edit ef-btn-accion btn-editar-centro" data-id="${c.id}"><i class="bi bi-pencil me-1"></i>Editar</button>
                                <button class="btn btn-sm btn-outline-success ef-btn-accion btn-verificar-centro" data-id="${c.id}"><i class="bi bi-patch-check me-1"></i>Verificar</button>
                                <button class="btn btn-sm btn-outline-danger ef-btn-accion btn-eliminar-centro" data-id="${c.id}"><i class="bi bi-trash me-1"></i>Eliminar</button>
                            </div>
                        </td>
                    </tr>
                `;
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
            container.innerHTML = `<span class="text-secondary small">${total} centro${total !== 1 ? 's' : ''} pendiente${total !== 1 ? 's' : ''}</span>`;
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
            <span class="text-secondary small">${total} centro${total !== 1 ? 's' : ''} pendiente${total !== 1 ? 's' : ''}</span>`;

        if (currentPage > 0)
            document.getElementById("btnPaginaAnterior").addEventListener('click', () => cargarCentros(currentPage - 1));
        if (currentPage < pages - 1)
            document.getElementById("btnPaginaSiguiente").addEventListener('click', () => cargarCentros(currentPage + 1));
    }

    function mostrarError(msg) {
        recuadroAlert.textContent = msg;
        recuadroAlert.className = "alert alert-danger mb-3";
        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 4000);
    }

    function mostrarExito(msg) {
        recuadroAlert.textContent = msg;
        recuadroAlert.className = "alert alert-success mb-3";
        setTimeout(() => {
            recuadroAlert.textContent = "";
            recuadroAlert.classList.add("d-none");
        }, 3000);
    }

    // DIALOG DE DETALLE (solo lectura, se abre al clicar una fila)

    async function abrirDetalleCentro(id) {
        contenidoDetalleCentro.innerHTML = '<p class="text-center text-secondary py-3">Cargando datos...</p>';
        dialogDetalleCentro.showModal();

        try {
            const r = await fetch(`/centros/${id}`);
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}`;
            }
            const c = await r.json();
            contenidoDetalleCentro.innerHTML = renderHtmlDetalleCentro(c);
        } catch (error) {
            contenidoDetalleCentro.innerHTML = `<p class="text-center text-danger">${error}</p>`;
        }
    }

    function renderHtmlDetalleCentro(c) {
        const tipoBadge = c.tipo ? `<span class="badge text-bg-secondary">${c.tipo}</span>` : '-';
        const verificadoBadge = c.verificado
            ? '<span class="badge text-bg-success">Verificado</span>'
            : '<span class="badge text-bg-warning text-dark">No verificado</span>';
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

                <div class="col-12 mt-1"><p class="detalle-seccion">Estado y fechas</p></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Verificación</span><span class="detalle-valor">${verificadoBadge}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Fecha de alta</span><span class="detalle-valor">${c.fechaAlta ? new Date(c.fechaAlta).toLocaleString('es-ES') : '-'}</span></div></div>
                <div class="col-md-4"><div class="detalle-campo"><span class="detalle-etiqueta">Última modificación</span><span class="detalle-valor">${c.fechaModificacion ? new Date(c.fechaModificacion).toLocaleString('es-ES') : '-'}</span></div></div>

                ${c.descripcion ? `
                <div class="col-12 mt-1"><p class="detalle-seccion">Descripción</p></div>
                <div class="col-12"><p class="detalle-valor mb-0">${c.descripcion}</p></div>` : ''}
            </div>`;
    }

    // EDITAR CENTRO

    // Se ejecuta al pulsar el botón editar, obtiene los datos del centro por su id
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

            dialogEditarCentro.showModal();
        } catch (error) {
            mostrarError(error);
        }
    }

    // Al enviar el formulario se llama a la función editar centro
    document.getElementById("formEditarCentro").addEventListener("submit", async (e) => {
        e.preventDefault();
        const id = document.getElementById("idCentroEditado").value;
        await editarCentro(id);
    });

    // Hace un fetch (metodo PUT) que envia los datos del centro actualizados al backend
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
            const formData = {
                nombreComercial: document.getElementById("nombreEditar").value.trim(),
                codigo: codigoEditar,
                tipo: document.getElementById("tipoEditar").value,
                email: document.getElementById("emailEditar").value.trim() || null,
                telefono: document.getElementById("telefonoEditar").value.trim() || null,
                direccion: document.getElementById("direccionEditar").value.trim(),
                localidad: document.getElementById("localidadEditar").value.trim(),
                provincia: provinciaVal,
                paginaWeb: document.getElementById("webEditar").value.trim() || null,
                descripcion: document.getElementById("descripcionEditar").value.trim() || null,
                verificado: false
            };

            const r = await fetch(`/centros/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            });

            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido actualizar el centro.`;
            }

            dialogEditarCentro.close();
            cargarCentros(paginaActual);
            mostrarExito("Centro actualizado correctamente.");
        } catch (error) {
            mostrarError(error);
        }
    }

    // VERIFICAR CENTRO
    async function verificarCentro(id) {
        const ok = await efConfirm({
            title:       'Verificar centro',
            message:     `¿Seguro que quieres verificar el centro con ID <strong>${id}</strong>?`,
            confirmText: 'Verificar',
            variant:     'primary',
        });
        if (!ok) return;

        try {
            const r = await fetch(`/centros/${id}/verificar`, { method: "PUT" });
            if (!r.ok) {
                const data = await r.json();
                throw data.errorMsg || `Error ${r.status}: No se ha podido verificar el centro.`;
            }

            const pagina = tBodyCentros.querySelectorAll('tr').length <= 1 ? Math.max(0, paginaActual - 1) : paginaActual;
            await cargarCentros(pagina);
        } catch (error) {
            mostrarError(error || `Error al modificar el estado verificado del centro con id: ${id}`);
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
            mostrarError(error);
        }
    }
});
