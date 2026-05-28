let paginaActual = 0;
const TAMANO_PAGINA = 12;

let provinciaSeleccionada = null;
let tituloOficialSeleccionado = null;
let sortActual = 'valoracion';
const SORT_DEFAULT = 'valoracion';
let debounceTimer = null;
let azAscendente = true;

let esEstudianteBuscador = false;
let estaAutenticadoBuscador = false;
let favoritasUuids = new Set();

document.addEventListener('DOMContentLoaded', async () => {
    cargarGradosEstudios();
    cargarProvincias();
    inicializarComboboxTituloOficial();
    inicializarSliderPrecio();
    inicializarSortBar();

    document.getElementById('formBuscador').addEventListener('submit', e => {
        e.preventDefault();
        paginaActual = 0;
        buscar();
    });
    // CSP: se usa event delegation en lugar de onclick inline para cumplir con la política de seguridad.
    const btnLimpiar = document.getElementById('btnLimpiarFiltros');
    if (btnLimpiar) btnLimpiar.addEventListener('click', limpiarFiltros);
    // Event delegation para paginación
    document.getElementById('paginacion').addEventListener('click', e => {
        const btn = e.target.closest('[data-page]:not([disabled])');
        if (btn) cambiarPagina(parseInt(btn.dataset.page));
    });
    // Event delegation para botones guardar — stopPropagation evita que el click
    // active la navegación data-href de la tarjeta (registrada en utils.js sobre document)
    document.getElementById('resultados').addEventListener('click', async e => {
        const btn = e.target.closest('.btn-guardar-buscador');
        if (!btn) return;
        e.stopPropagation();
        if (!esEstudianteBuscador) {
            const returnTo = encodeURIComponent(window.location.href);
            window.location.href = `/vistas/auth/login.html?returnTo=${returnTo}&guardarFormacion`;
            return;
        }
        await _toggleFavoritoBuscador(btn);
    });

    await _inicializarEstadoFavoritos();
    buscar();
});

// ─── Datos de referencia ──────────────────────────────────────────────────────

async function cargarGradosEstudios() {
    try {
        const res = await fetch('/tipo-estudios');
        if (!res.ok) return;
        const tipos = await res.json();
        const sel = document.getElementById('filtroTipoEstudios');
        tipos.forEach(t => {
            const opt = document.createElement('option');
            opt.value = t.id;
            opt.textContent = t.nombre;
            sel.appendChild(opt);
        });
    } catch { /* no crítico */ }
}

async function cargarProvincias() {
    try {
        const res = await fetch('/provincias');
        if (!res.ok) return;
        const provincias = await res.json();
        inicializarComboboxProvincia(provincias);
    } catch { /* no crítico */ }
}

// ─── Combobox provincia (datos estáticos, carga única) ────────────────────────

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

    function limpiar() {
        provinciaSeleccionada = null;
        input.value = '';
        wrap.classList.remove('seleccionado');
        btnLimpiar.classList.add('d-none');
        lista.classList.add('d-none');
    }

    input.addEventListener('focus', () => abrirDropdown(input.value));
    input.addEventListener('input', () => {
        if (provinciaSeleccionada) limpiar();
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
    btnLimpiar.addEventListener('click', () => { limpiar(); paginaActual = 0; buscar(); });
    document.addEventListener('click', e => {
        if (!e.target.closest('#comboProvincia')) cerrarDropdown();
    });
}

// ─── Combobox título oficial (datos dinámicos, búsqueda async con debounce) ───

function inicializarComboboxTituloOficial() {
    const input      = document.getElementById('comboTituloInput');
    const wrap       = document.getElementById('comboTituloWrap');
    const lista      = document.getElementById('comboTituloLista');
    const btnLimpiar = document.getElementById('comboTitulo-clear');

    function mostrarCargandoDropdown() {
        lista.innerHTML = `<li class="ef-combobox-sin-resultados"><i class="bi bi-hourglass-split me-1"></i>Buscando...</li>`;
        lista.classList.remove('d-none');
    }

    function mostrarResultados(titulos) {
        if (titulos.length === 0) {
            lista.innerHTML = `<li class="ef-combobox-sin-resultados">Sin resultados</li>`;
        } else {
            lista.innerHTML = titulos
                .map(t => `<li data-titulo="${escapar(t)}">${escapar(t)}</li>`)
                .join('');
        }
        lista.classList.remove('d-none');
    }

    function cerrarDropdown() {
        lista.classList.add('d-none');
        if (!tituloOficialSeleccionado) input.value = '';
    }

    function seleccionar(titulo) {
        tituloOficialSeleccionado = titulo;
        input.value = titulo;
        wrap.classList.add('seleccionado');
        btnLimpiar.classList.remove('d-none');
        lista.classList.add('d-none');
    }

    function limpiar() {
        tituloOficialSeleccionado = null;
        input.value = '';
        wrap.classList.remove('seleccionado');
        btnLimpiar.classList.add('d-none');
        lista.classList.add('d-none');
        clearTimeout(debounceTimer);
    }

    input.addEventListener('input', () => {
        if (tituloOficialSeleccionado) {
            tituloOficialSeleccionado = null;
            wrap.classList.remove('seleccionado');
            btnLimpiar.classList.add('d-none');
        }
        const q = input.value.trim();
        clearTimeout(debounceTimer);
        if (q.length < 2) { lista.classList.add('d-none'); return; }
        mostrarCargandoDropdown();
        debounceTimer = setTimeout(async () => {
            try {
                const res = await fetch(`/formaciones/titulos-oficiales?q=${encodeURIComponent(q)}`);
                if (!res.ok) return;
                mostrarResultados(await res.json());
            } catch { lista.classList.add('d-none'); }
        }, 300);
    });

    input.addEventListener('focus', () => {
        const q = input.value.trim();
        if (q.length >= 2 && !tituloOficialSeleccionado) input.dispatchEvent(new Event('input'));
    });

    lista.addEventListener('mousedown', e => {
        const li = e.target.closest('li[data-titulo]');
        if (!li) return;
        e.preventDefault();
        seleccionar(li.dataset.titulo);
        paginaActual = 0;
        buscar();
    });

    btnLimpiar.addEventListener('click', () => { limpiar(); paginaActual = 0; buscar(); });

    document.addEventListener('click', e => {
        if (!e.target.closest('#comboTitulo')) cerrarDropdown();
    });
}

// ─── Slider de precio con escala no lineal ────────────────────────────────────
// Posiciones 0-150 → precios 0-5000 € (alta resolución)
// Posiciones 150-200 → precios 5000-10000 € (baja resolución)

const SLIDER_MAX  = 200;
const PRECIO_MAX  = 10000;
const TRAMO_POS   = 150;  // posición del punto de inflexión
const TRAMO_PRECIO = 5000; // precio en el punto de inflexión

function posToPrice(pos) {
    let precio;
    if (pos <= TRAMO_POS) precio = pos / TRAMO_POS * TRAMO_PRECIO;
    else precio = TRAMO_PRECIO + (pos - TRAMO_POS) / (SLIDER_MAX - TRAMO_POS) * (PRECIO_MAX - TRAMO_PRECIO);
    return Math.round(precio / 50) * 50;
}

function priceToPos(price) {
    if (price <= TRAMO_PRECIO) return Math.round(price / TRAMO_PRECIO * TRAMO_POS);
    return Math.round(TRAMO_POS + (price - TRAMO_PRECIO) / (PRECIO_MAX - TRAMO_PRECIO) * (SLIDER_MAX - TRAMO_POS));
}

function resolverLabelPrecio(precio) {
    if (precio === 0)          return 'Gratis';
    if (precio >= PRECIO_MAX)  return 'Sin límite';
    return `Hasta ${precio.toLocaleString('es-ES')} €`;
}

function inicializarSliderPrecio() {
    const slider = document.getElementById('filtroPrecio');
    const input  = document.getElementById('filtroPrecioInput');
    const label  = document.getElementById('precioLabel');

    slider.max  = SLIDER_MAX;
    slider.min  = 0;
    slider.step = 1;
    slider.value = SLIDER_MAX;

    slider.addEventListener('input', () => {
        const precio = posToPrice(parseInt(slider.value, 10));
        label.textContent = resolverLabelPrecio(precio);
        input.value = precio >= PRECIO_MAX ? '' : precio;
    });

    slider.addEventListener('change', () => {
        paginaActual = 0;
        buscar();
    });

    input.addEventListener('input', () => {
        const raw = input.value.trim();
        if (raw === '') {
            slider.value = SLIDER_MAX;
            label.textContent = 'Sin límite';
            return;
        }
        const precio = Math.max(0, Math.min(parseInt(raw, 10) || 0, PRECIO_MAX));
        slider.value = priceToPos(precio);
        label.textContent = resolverLabelPrecio(precio);
    });

    input.addEventListener('change', () => {
        paginaActual = 0;
        buscar();
    });
}

// ─── Barra de ordenamiento ────────────────────────────────────────────────────

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

function desactivarSortPills() {
    document.querySelectorAll('.sort-pill').forEach(p => {
        p.classList.remove('btn-primary');
        p.classList.add('btn-outline-secondary');
    });
}

function inicializarSortBar() {
    document.querySelectorAll('.sort-pill').forEach(btn => {
        btn.addEventListener('click', () => {
            const key = btn.dataset.sort;

            // Pill de precio: ↑ → ↓ → desactivar
            if (key === 'precioAsc' || key === 'precioDesc') {
                if (sortActual === 'precioAsc') {
                    sortActual = 'precioDesc';
                    btn.dataset.sort = 'precioDesc';
                    btn.querySelector('.sort-dir').textContent = '↓';
                    activarSortPill(btn);
                } else if (sortActual === 'precioDesc') {
                    sortActual = null;
                    btn.dataset.sort = 'precioAsc';
                    btn.querySelector('.sort-dir').textContent = '↑';
                    desactivarSortPills();
                } else {
                    sortActual = 'precioAsc';
                    btn.dataset.sort = 'precioAsc';
                    btn.querySelector('.sort-dir').textContent = '↑';
                    activarSortPill(btn);
                }
                paginaActual = 0;
                buscar();
                return;
            }

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

            // Pills simples (recientes, valoracion, proximasFechas): toggle on/off
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

function activarSortPill(btnActivo) {
    document.querySelectorAll('.sort-pill').forEach(p => {
        p.classList.remove('btn-primary');
        p.classList.add('btn-outline-secondary');
    });
    btnActivo.classList.remove('btn-outline-secondary');
    btnActivo.classList.add('btn-primary');
}

// ─── Construcción de parámetros ───────────────────────────────────────────────

function construirParams() {
    const params = new URLSearchParams();

    const nombre       = document.getElementById('filtroNombre').value.trim();
    const localidad    = document.getElementById('filtroLocalidad').value.trim();
    const modalidad    = document.getElementById('filtroModalidad').value;
    const horario      = document.getElementById('filtroHorario').value;
    const tipoEstudios = document.getElementById('filtroTipoEstudios').value;
    const tipoCentro   = document.getElementById('filtroTipoCentro').value;
    const precioVal = posToPrice(parseInt(document.getElementById('filtroPrecio').value, 10));
    const fechaInicio = document.getElementById('filtroFechaInicio').value;

    if (nombre) params.append('nombre', nombre);
    if (tituloOficialSeleccionado) params.append('tituloOficial', tituloOficialSeleccionado);
    if (localidad) params.append('localidad', localidad);
    if (provinciaSeleccionada) params.append('provinciaId', provinciaSeleccionada.id);
    if (modalidad) params.append('modalidad', modalidad);
    if (horario) params.append('horario', horario);
    if (tipoEstudios) params.append('tipoEstudiosId', tipoEstudios);
    if (tipoCentro) params.append('tipoCentro', tipoCentro);
    if (precioVal === 0) {
        params.append('soloGratuitas', 'true');
    } else if (precioVal < PRECIO_MAX) {
        params.append('precioMax', precioVal);
    }
    if (fechaInicio) params.append('fechaInicioDesde', fechaInicio);

    params.append('page', paginaActual);
    params.append('size', TAMANO_PAGINA);
    if (sortActual) params.append('sortBy', sortActual);
    return params;
}

function hayFiltrosActivos() {
    const precioPos = parseInt(document.getElementById('filtroPrecio').value, 10);
    return !!(
        document.getElementById('filtroNombre').value.trim() ||
        tituloOficialSeleccionado ||
        document.getElementById('filtroLocalidad').value.trim() ||
        provinciaSeleccionada ||
        document.getElementById('filtroModalidad').value ||
        document.getElementById('filtroHorario').value ||
        document.getElementById('filtroTipoEstudios').value ||
        document.getElementById('filtroTipoCentro').value ||
        precioPos < SLIDER_MAX ||
        document.getElementById('filtroFechaInicio').value
    );
}

// ─── Estado de favoritos ──────────────────────────────────────────────────────

async function _inicializarEstadoFavoritos() {
    const perfil = await _obtenerPerfil();
    estaAutenticadoBuscador = perfil !== null;
    esEstudianteBuscador = perfil?.roles?.some(r => r.nombre === 'ESTUDIANTE') ?? false;
    if (!esEstudianteBuscador) return;
    try {
        const r = await fetch('/favoritos', { credentials: 'include' });
        if (r.ok) {
            const items = await r.json();
            favoritasUuids = new Set(items.map(f => f.uuid));
        }
    } catch { /* no crítico */ }
}

async function _toggleFavoritoBuscador(btn) {
    const uuid = btn.dataset.uuid;
    const activo = btn.dataset.guardada === 'true';
    btn.disabled = true;
    try {
        const method = activo ? 'DELETE' : 'POST';
        const res = await fetch(`/favoritos/${uuid}`, { method, credentials: 'include' });
        if (res.status === 204 || res.ok) {
            const nuevo = !activo;
            if (nuevo) favoritasUuids.add(uuid);
            else favoritasUuids.delete(uuid);
            _actualizarBtnGuardarBuscador(btn, nuevo);
        } else if (res.status === 409) {
            favoritasUuids.add(uuid);
            _actualizarBtnGuardarBuscador(btn, true);
        }
    } catch { /* no crítico */ } finally {
        btn.disabled = false;
    }
}

function _actualizarBtnGuardarBuscador(btn, guardada) {
    btn.dataset.guardada = guardada;
    btn.title = guardada ? 'Quitar de guardadas' : 'Guardar formación';
    const icon = btn.querySelector('i');
    if (icon) icon.className = `bi bi-bookmark${guardada ? '-fill' : ''}`;
    btn.classList.toggle('text-success', guardada);
    btn.classList.toggle('text-muted', !guardada);
}

// ─── Búsqueda principal ───────────────────────────────────────────────────────

async function buscar() {
    const params = construirParams();
    mostrarCargando(true);
    try {
        const res = await fetch(`/formaciones?${params}`);
        if (!res.ok) throw new Error();
        renderizarResultados(await res.json());
    } catch {
        document.getElementById('resultados').innerHTML =
            '<div class="col-12"><div class="alert alert-danger">No se pudieron cargar las formaciones. Inténtalo de nuevo.</div></div>';
    } finally {
        mostrarCargando(false);
    }
}

// ─── Renderizado de resultados ────────────────────────────────────────────────

function renderizarResultados(pagina) {
    const contenedor = document.getElementById('resultados');
    const paginacion = document.getElementById('paginacion');
    const contador   = document.getElementById('totalResultados');

    const total = pagina.totalElements ?? (pagina.page ? pagina.page.totalElements : pagina.content.length) ?? 0;
    const hayFiltros = hayFiltrosActivos();
    const palabra = hayFiltros ? 'encontrada' : 'disponible';
    const palabraPlural = hayFiltros ? 'encontradas' : 'disponibles';

    contador.textContent = total === 1
        ? `1 formación ${palabra}`
        : `${total.toLocaleString('es-ES')} formaciones ${palabraPlural}`;

    if (pagina.content.length === 0) {
        contenedor.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-search fs-1 text-muted d-block mb-3"></i>
                <p class="text-muted">No se encontraron formaciones con esos filtros.</p>
                <button class="btn btn-outline-primary btn-sm" id="btnLimpiarFiltrosSinRes">Limpiar filtros</button>
            </div>`;
        paginacion.innerHTML = '';
        return;
    }

    contenedor.innerHTML = pagina.content.map(crearTarjetaFormacion).join('');
    renderizarPaginacion(pagina);
    // Conectar botón limpiar en estado sin resultados
    const btnSinRes = document.getElementById('btnLimpiarFiltrosSinRes');
    if (btnSinRes) btnSinRes.addEventListener('click', limpiarFiltros);
}

// ─── Tarjeta de formación ─────────────────────────────────────────────────────

function crearTarjetaFormacion(f) {
    const { texto: precio, color: precioColor } = formatPrecio(f.precio);
    const estrellas = f.valoracionMedia ? renderEstrellas(f.valoracionMedia) : '';
    const guardada = favoritasUuids.has(f.uuid);
    // Mostrar botón a no autenticados (para invitarles a registrarse) y a estudiantes.
    // GESTOR y ADMIN autenticados no ven el botón (no tienen favoritos).
    const mostrarBtnGuardar = esEstudianteBuscador || !estaAutenticadoBuscador;
    const btnGuardarHtml = mostrarBtnGuardar
        ? `<button class="btn btn-sm btn-guardar-buscador ${guardada ? 'text-success' : 'text-muted'}"
                   data-uuid="${f.uuid}" data-guardada="${guardada}"
                   title="${guardada ? 'Quitar de guardadas' : 'Guardar formación'}">
               <i class="bi bi-bookmark${guardada ? '-fill' : ''}"></i>
           </button>`
        : '';

    // Badge modalidad: color canonical = índigo (--ef-accent)
    const bm = BADGE_MODALIDAD[f.modalidad] || 'ef-badge-default';
    const badgeModalidadHtml = `<span class="badge me-1 ef-badge-custom ${bm}">${formatModalidad(f.modalidad)}</span>`;

    // Badge tipo de estudios: color canonical = azul (--ef-primary)
    let badgeTipoEstudios = '';
    if (f.tipoEstudios?.nombre) {
        const c = BADGE_TIPO_ESTUDIOS[f.tipoEstudios.nombre] || 'ef-badge-default';
        badgeTipoEstudios = `<span class="badge me-1 ef-badge-custom ${c}">${escapar(f.tipoEstudios.nombre)}</span>`;
    }

    let badgeTipoCentro = '';
    if (f.centroTipo) {
        const c = BADGE_TIPO_CENTRO[f.centroTipo] || { class: 'ef-badge-default', label: f.centroTipo };
        badgeTipoCentro = `<span class="badge me-1 ef-badge-custom ${c.class}">${c.label}</span>`;
    }

    // Título oficial: solo si existe y es distinto al nombre
    const subtitulo = f.tituloOficial && f.tituloOficial !== f.nombre
        ? `<p class="text-muted small mb-2 ef-line-height-13">${escapar(truncar(f.tituloOficial, 90))}</p>`
        : '';

    return `
        <div class="col-md-6 col-lg-4">
            <div class="card h-100 shadow-sm ef-card-hover ef-card-hover-pointer"
                 data-href="/vistas/publico/detalle-formacion.html?uuid=${f.uuid}">
                <div class="card-body d-flex flex-column p-4">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            ${badgeTipoEstudios}
                            ${badgeModalidadHtml}
                            ${badgeTipoCentro}
                        </div>
                        ${f.horario ? `<span class="small ef-color-secondary flex-shrink-0 ms-2 text-nowrap"><i class="bi bi-clock"></i> ${formatHorario(f.horario)}</span>` : ''}
                    </div>
                    <h6 class="card-title fw-semibold mb-1">${escapar(f.nombre)}</h6>
                    ${subtitulo}
                    <div class="flex-grow-1"></div>
                    <div class="border-top pt-2">
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted"><i class="bi bi-geo-alt"></i> ${escapar(f.centroLocalidad || '')}</small>
                            ${f.precio != null ? `<strong class="${precioColor}">${precio}</strong>` : ''}
                        </div>
                        <div class="d-flex justify-content-between align-items-center mt-1">
                            <small class="text-muted text-truncate me-2 ef-max-w-120">
                                <i class="bi bi-building"></i> ${escapar(f.centroNombre || '')}
                            </small>
                            <div class="d-flex align-items-center gap-2">
                                ${estrellas
                                    ? `<span class="small">${estrellas} <span class="text-muted">(${f.totalValoraciones})</span></span>`
                                    : '<span class="text-muted small">Sin valoraciones</span>'}
                                ${btnGuardarHtml}
                            </div>
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
    document.getElementById('filtroNombre').value      = '';
    document.getElementById('filtroLocalidad').value   = '';
    document.getElementById('filtroModalidad').value   = '';
    document.getElementById('filtroHorario').value     = '';
    document.getElementById('filtroTipoEstudios').value = '';
    document.getElementById('filtroTipoCentro').value  = '';
    document.getElementById('filtroFechaInicio').value = '';

    const slider = document.getElementById('filtroPrecio');
    slider.value = SLIDER_MAX;
    document.getElementById('precioLabel').textContent = 'Sin límite';

    // Limpiar combobox provincia
    provinciaSeleccionada = null;
    const provInput = document.getElementById('comboProvinciaInput');
    const provWrap  = document.getElementById('comboProvinciaWrap');
    const provClear = document.getElementById('comboProvincia-clear');
    if (provInput) provInput.value = '';
    if (provWrap) provWrap.classList.remove('seleccionado');
    if (provClear) provClear.classList.add('d-none');

    // Limpiar combobox título oficial
    tituloOficialSeleccionado = null;
    clearTimeout(debounceTimer);
    const tituloInput = document.getElementById('comboTituloInput');
    const tituloWrap  = document.getElementById('comboTituloWrap');
    const tituloClear = document.getElementById('comboTitulo-clear');
    const tituloLista = document.getElementById('comboTituloLista');
    if (tituloInput) tituloInput.value = '';
    if (tituloWrap) tituloWrap.classList.remove('seleccionado');
    if (tituloClear) tituloClear.classList.add('d-none');
    if (tituloLista) tituloLista.classList.add('d-none');

    // Resetear ordenamiento al default
    sortActual = SORT_DEFAULT;
    azAscendente = true;
    const pillPrecio = document.querySelector('.sort-pill[data-sort="precioAsc"], .sort-pill[data-sort="precioDesc"]');
    if (pillPrecio) { pillPrecio.dataset.sort = 'precioAsc'; pillPrecio.querySelector('.sort-dir').textContent = '↑'; }
    const pillAZ = document.getElementById('pillAZ');
    if (pillAZ) { pillAZ.dataset.sort = 'az'; actualizarIconoAZ(pillAZ, 'az'); }
    const pillDefault = document.querySelector('.sort-pill[data-sort="valoracion"]');
    if (pillDefault) activarSortPill(pillDefault);

    paginaActual = 0;
    buscar();
}

function mostrarCargando(visible) {
    document.getElementById('spinner').classList.toggle('d-none', !visible);
    if (visible) document.getElementById('resultados').innerHTML = '';
}
