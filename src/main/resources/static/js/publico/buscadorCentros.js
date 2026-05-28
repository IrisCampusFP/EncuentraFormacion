let paginaActual = 0;
const TAMANO_PAGINA = 12;

let provinciaSeleccionada = null;
let sortActual = 'valorados';
const SORT_DEFAULT = 'valorados';
let azAscendente = true;

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('info')) {
        const infoAlertText = document.getElementById('infoAlertText');
        const infoAlert     = document.getElementById('infoAlert');
        infoAlertText.textContent = decodeURIComponent(urlParams.get('info'));
        infoAlert.classList.remove('d-none');
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    cargarProvincias();
    inicializarSortBar();
    buscar();
    document.getElementById('formBuscador').addEventListener('submit', e => {
        e.preventDefault();
        paginaActual = 0;
        buscar();
    });
    // CSP — sin onclick inline
    const btnLimpiar = document.getElementById('btnLimpiarFiltros');
    if (btnLimpiar) btnLimpiar.addEventListener('click', limpiarFiltros);
    // Event delegation para paginación — registrado una sola vez
    document.getElementById('paginacion').addEventListener('click', e => {
        const btn = e.target.closest('[data-page]:not([disabled])');
        if (btn) cambiarPagina(parseInt(btn.dataset.page));
    });
});

// ─── Provincias ───────────────────────────────────────────────────────────────

async function cargarProvincias() {
    try {
        const res = await fetch('/provincias');
        if (!res.ok) return;
        inicializarComboboxProvincia(await res.json());
    } catch { /* no crítico */ }
}

function inicializarComboboxProvincia(opciones) {
    const input      = document.getElementById('comboProvinciaInput');
    const wrap       = document.getElementById('comboProvinciaWrap');
    const lista      = document.getElementById('comboProvinciaLista');
    const btnLimpiar = document.getElementById('comboProvincia-clear');

    function abrirDropdown(texto) {
        const q = normalizar(texto);
        const filtradas = q ? opciones.filter(o => normalizar(o.nombre).includes(q)) : opciones;
        lista.innerHTML = filtradas.length === 0
            ? `<li class="ef-combobox-sin-resultados">Sin resultados</li>`
            : filtradas.map(o => `<li data-id="${o.id}" data-nombre="${escapar(o.nombre)}">${escapar(o.nombre)}</li>`).join('');
        lista.classList.remove('d-none');
    }

    function cerrarDropdown() {
        lista.classList.add('d-none');
        if (!provinciaSeleccionada) input.value = '';
    }

    function seleccionar(id, nombre) {
        provinciaSeleccionada = { id, nombre };
        input.value = nombre;
        wrap.classList.add('seleccionado');
        btnLimpiar.classList.remove('d-none');
        lista.classList.add('d-none');
    }

    function limpiarCombo() {
        provinciaSeleccionada = null;
        input.value = '';
        wrap.classList.remove('seleccionado');
        btnLimpiar.classList.add('d-none');
        lista.classList.add('d-none');
    }

    input.addEventListener('focus', () => abrirDropdown(input.value));
    input.addEventListener('input', () => {
        if (provinciaSeleccionada) limpiarCombo();
        abrirDropdown(input.value);
    });
    lista.addEventListener('mousedown', e => {
        const li = e.target.closest('li[data-id]');
        if (!li) return;
        e.preventDefault();
        seleccionar(li.dataset.id, li.dataset.nombre);
        paginaActual = 0;
        buscar();
    });
    btnLimpiar.addEventListener('click', () => { limpiarCombo(); paginaActual = 0; buscar(); });
    document.addEventListener('click', e => {
        if (!e.target.closest('#comboProvincia')) cerrarDropdown();
    });
}

// ─── Sort ─────────────────────────────────────────────────────────────────────

function actualizarIconoAZ(btn, estado) {
    const icon = btn.querySelector('i');
    if (!icon) return;
    if (estado === 'az') {
        icon.className = 'bi bi-sort-alpha-down me-1';
    } else if (estado === 'za') {
        icon.className = 'bi bi-sort-alpha-up-alt me-1';
    } else {
        icon.className = 'bi bi-sort-alpha-down me-1';
    }
}

function inicializarSortBar() {
    document.querySelectorAll('.sort-pill').forEach(btn => {
        btn.addEventListener('click', () => {
            const key = btn.dataset.sort;

            // Pill A-Z: az → za → desactivar
            if (key === 'az' || key === 'za') {
                if (sortActual === 'az') {
                    azAscendente = false;
                    sortActual = 'za';
                    actualizarIconoAZ(btn, 'za');
                    activarSortPill(btn);
                } else if (sortActual === 'za') {
                    azAscendente = true;
                    sortActual = null;
                    actualizarIconoAZ(btn, null);
                    desactivarSortPills();
                } else {
                    azAscendente = true;
                    sortActual = 'az';
                    actualizarIconoAZ(btn, 'az');
                    activarSortPill(btn);
                }
                paginaActual = 0;
                buscar();
                return;
            }

            // Pills simples (valorados): toggle on/off
            if (sortActual === key) {
                sortActual = null;
                desactivarSortPills();
            } else {
                sortActual = key;
                activarSortPill(btn);
            }
            paginaActual = 0;
            buscar();
        });
    });
}

function desactivarSortPills() {
    document.querySelectorAll('.sort-pill').forEach(p => {
        p.classList.remove('btn-primary');
        p.classList.add('btn-outline-secondary');
    });
}

function activarSortPill(btnActivo) {
    document.querySelectorAll('.sort-pill').forEach(p => {
        p.classList.remove('btn-primary');
        p.classList.add('btn-outline-secondary');
    });
    btnActivo.classList.remove('btn-outline-secondary');
    btnActivo.classList.add('btn-primary');
}

// ─── Búsqueda ─────────────────────────────────────────────────────────────────

function construirParams() {
    const params = new URLSearchParams();

    const nombre   = document.getElementById('filtroNombre').value.trim();
    const localidad = document.getElementById('filtroLocalidad').value.trim();
    const tipo     = document.getElementById('filtroTipo').value;

    if (nombre) params.append('nombre', nombre);
    if (localidad) params.append('localidad', localidad);
    if (provinciaSeleccionada) params.append('provinciaId', provinciaSeleccionada.id);
    if (tipo) params.append('tipo', tipo);

    params.append('page', paginaActual);
    params.append('size', TAMANO_PAGINA);
    if (sortActual) params.append('sortBy', sortActual);
    return params;
}

async function buscar() {
    mostrarCargando(true);
    try {
        const res = await fetch(`/centros/buscar?${construirParams()}`);
        if (!res.ok) throw new Error();
        renderizarResultados(await res.json());
    } catch {
        document.getElementById('resultados').innerHTML =
            '<div class="col-12"><div class="alert alert-danger">No se pudieron cargar los centros. Inténtalo de nuevo.</div></div>';
    } finally {
        mostrarCargando(false);
    }
}

// ─── Renderizado ──────────────────────────────────────────────────────────────


function renderizarResultados(pagina) {
    const contenedor = document.getElementById('resultados');
    const paginacion = document.getElementById('paginacion');
    const contador   = document.getElementById('totalResultados');

    const total = pagina.totalElements ?? pagina.page?.totalElements ?? pagina.content.length ?? 0;
    const hayFiltros = !!(document.getElementById('filtroNombre')?.value.trim()
        || document.getElementById('filtroLocalidad')?.value.trim()
        || document.getElementById('filtroTipo')?.value
        || provinciaSeleccionada);
    const palabra = hayFiltros ? 'encontrado' : 'disponible';
    const palabraPlural = hayFiltros ? 'encontrados' : 'disponibles';
    contador.textContent = total === 1 ? `1 centro ${palabra}` : `${total.toLocaleString('es-ES')} centros ${palabraPlural}`;

    if (pagina.content.length === 0) {
        contenedor.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-building fs-1 text-muted d-block mb-3"></i>
                <p class="text-muted">No se encontraron centros con esos filtros.</p>
                <button class="btn btn-outline-primary btn-sm" id="btnLimpiarFiltrosSinRes">Limpiar filtros</button>
            </div>`;
        paginacion.innerHTML = '';
        return;
    }

    contenedor.innerHTML = pagina.content.map(crearTarjetaCentro).join('');
    renderizarPaginacion(pagina);
    // Conectar botón de limpiar filtros en estado sin resultados
    const btnSinRes = document.getElementById('btnLimpiarFiltrosSinRes');
    if (btnSinRes) btnSinRes.addEventListener('click', limpiarFiltros);
}

function crearTarjetaCentro(c) {
    const badge = BADGE_TIPO_CENTRO[c.tipo] || { class: 'ef-badge-default', label: c.tipo || '' };
    const estrellas = c.valoracionMedia ? renderEstrellas(c.valoracionMedia) : '';
    const numFormaciones = c.totalFormaciones ?? 0;

    return `
        <div class="col-md-6 col-lg-4">
            <div class="card h-100 shadow-sm ef-card-hover ef-card-hover-pointer"
                 data-href="/vistas/publico/perfil-centro.html?uuid=${c.uuid}">
                <div class="card-body d-flex flex-column p-4">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <span class="badge ef-badge-custom ${badge.class}">
                            ${badge.label}
                        </span>
                        ${estrellas
                            ? `<span class="small">${estrellas} <span class="text-muted">(${c.totalValoraciones})</span></span>`
                            : '<span class="text-muted small">Sin valoraciones</span>'}
                    </div>
                    <h6 class="card-title fw-semibold mb-1">${escapar(c.nombreComercial)}</h6>
                    <p class="text-muted small mb-3 flex-grow-1">${truncar(escapar(c.descripcion || ''), 110)}</p>
                    <div class="border-top pt-2">
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <i class="bi bi-geo-alt me-1"></i>${escapar(c.localidad || '')}${c.provincia ? `, ${escapar(c.provincia)}` : ''}
                            </small>
                            <small class="text-muted">
                                <i class="bi bi-mortarboard me-1"></i>${numFormaciones} ${numFormaciones !== 1 ? 'formaciones' : 'formación'}
                            </small>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
}

// ─── Paginación ───────────────────────────────────────────────────────────────

function renderizarPaginacion(pagina) {
    const paginacion = document.getElementById('paginacion');
    const totalPages = pagina.totalPages ?? pagina.page?.totalPages ?? 1;

    if (totalPages <= 1) { paginacion.innerHTML = ''; return; }

    const isFirst = pagina.first ?? (paginaActual === 0);
    const isLast  = pagina.last  ?? (paginaActual >= totalPages - 1);

    const maxVisible = 5;
    const inicio = Math.max(0, paginaActual - Math.floor(maxVisible / 2));
    const fin = Math.min(totalPages, inicio + maxVisible);

    let html = '<nav><ul class="pagination justify-content-center">';
    html += `<li class="page-item ${isFirst ? 'disabled' : ''}">
        <button class="page-link" data-page="${paginaActual - 1}" ${isFirst ? 'disabled' : ''}>
            <i class="bi bi-chevron-left"></i>
        </button></li>`;
    for (let i = inicio; i < fin; i++) {
        html += `<li class="page-item ${i === paginaActual ? 'active' : ''}">
            <button class="page-link" data-page="${i}">${i + 1}</button></li>`;
    }
    html += `<li class="page-item ${isLast ? 'disabled' : ''}">
        <button class="page-link" data-page="${paginaActual + 1}" ${isLast ? 'disabled' : ''}>
            <i class="bi bi-chevron-right"></i>
        </button></li>`;
    html += '</ul></nav>';
    paginacion.innerHTML = html;
}

function cambiarPagina(pagina) {
    paginaActual = pagina;
    buscar();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ─── Limpiar filtros ──────────────────────────────────────────────────────────

function limpiarFiltros() {
    document.getElementById('filtroNombre').value   = '';
    document.getElementById('filtroLocalidad').value = '';
    document.getElementById('filtroTipo').value     = '';

    provinciaSeleccionada = null;
    const provInput = document.getElementById('comboProvinciaInput');
    const provWrap  = document.getElementById('comboProvinciaWrap');
    const provClear = document.getElementById('comboProvincia-clear');
    if (provInput) provInput.value = '';
    if (provWrap) provWrap.classList.remove('seleccionado');
    if (provClear) provClear.classList.add('d-none');

    sortActual = SORT_DEFAULT;
    azAscendente = true;
    const pillAZ = document.getElementById('pillAZ');
    if (pillAZ) { pillAZ.dataset.sort = 'az'; actualizarIconoAZ(pillAZ, 'az'); }
    desactivarSortPills();
    const pillDefault = document.querySelector('.sort-pill[data-sort="valorados"]');
    if (pillDefault) activarSortPill(pillDefault);

    paginaActual = 0;
    buscar();
}

function mostrarCargando(visible) {
    document.getElementById('spinner').classList.toggle('d-none', !visible);
    if (visible) document.getElementById('resultados').innerHTML = '';
}
