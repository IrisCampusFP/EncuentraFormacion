const PAGE_SIZE = 12;
let paginaActual = 0;

document.addEventListener('DOMContentLoaded', () => {
    cargarGuardadas(0);

    document.getElementById('paginacion').addEventListener('click', e => {
        const btn = e.target.closest('[data-page]:not([disabled])');
        if (btn) cargarGuardadas(parseInt(btn.dataset.page));
    });

    document.getElementById('listaGuardadas').addEventListener('click', async e => {
        const btn = e.target.closest('.btn-quitar-guardada');
        if (!btn) return;
        e.stopPropagation();
        const uuid = btn.dataset.uuid;
        const card = btn.closest('.ef-card-formacion');
        const nombre = card?.querySelector('h6')?.textContent?.trim() ?? 'esta formación';
        const ok = await efConfirm({
            title:       'Quitar formación guardada',
            message:     `¿Quieres quitar <strong>${nombre}</strong> de tus formaciones guardadas? Podrás volver a guardarla cuando quieras.`,
            confirmText: 'Quitar',
            variant:     'warning',
        });
        if (!ok) return;
        btn.disabled = true;
        try {
            const res = await fetch(`/favoritos/${uuid}`, { method: 'DELETE', credentials: 'include' });
            if (!res.ok) { const d = await res.json(); throw d.errorMsg || `Error ${res.status}`; }
            const itemsActuales = document.querySelectorAll('.btn-quitar-guardada').length;
            const nuevaPagina = itemsActuales === 1 && paginaActual > 0 ? paginaActual - 1 : paginaActual;
            cargarGuardadas(nuevaPagina);
        } catch (err) {
            btn.disabled = false;
            mostrarError(typeof err === 'string' ? err : 'No se pudo quitar la formación');
        }
    });
});

async function cargarGuardadas(pagina) {
    const lista = document.getElementById('listaGuardadas');
    lista.innerHTML = '<div class="col-12 text-center text-muted py-4">Cargando...</div>';
    try {
        const r = await fetch(`/favoritos?page=${pagina}&size=${PAGE_SIZE}`, { credentials: 'include' });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        const data = await r.json();

        const items = data.content;
        paginaActual = data.number ?? data.page?.number ?? 0;

        if (!items.length) {
            lista.innerHTML = '<div class="col-12"><div class="card"><div class="card-body text-center text-muted py-5"><i class="bi bi-bookmark fs-1 d-block mb-2 opacity-25"></i>No tienes formaciones guardadas todavía.</div></div></div>';
            document.getElementById('paginacion').innerHTML = '';
            return;
        }

        lista.innerHTML = items.map(f => crearTarjetaGuardada(f)).join('');

        renderPaginacion(data);
    } catch (e) {
        lista.innerHTML = '';
        mostrarError(typeof e === 'string' ? e : 'Error al cargar formaciones guardadas');
    }
}

function crearTarjetaGuardada(f) {
    const { texto: precio, color: precioColor } = formatPrecio(f.precio);
    const estrellas = f.valoracionMedia ? renderEstrellas(f.valoracionMedia) : '';

    let badgeTipoEstudios = '';
    if (f.tipoEstudios?.nombre) {
        const c = BADGE_TIPO_ESTUDIOS[f.tipoEstudios.nombre] || 'ef-badge-default';
        badgeTipoEstudios = `<span class="badge me-1 ef-badge-custom ${c}">${escapar(f.tipoEstudios.nombre)}</span>`;
    }

    const bm = BADGE_MODALIDAD[f.modalidad] || 'ef-badge-default';
    const badgeModalidad = `<span class="badge me-1 ef-badge-custom ${bm}">${formatModalidad(f.modalidad)}</span>`;

    let badgeTipoCentro = '';
    if (f.centroTipo) {
        const c = BADGE_TIPO_CENTRO[f.centroTipo] || { class: 'ef-badge-default', label: f.centroTipo };
        badgeTipoCentro = `<span class="badge me-1 ef-badge-custom ${c.class}">${c.label}</span>`;
    }

    const subtitulo = f.tituloOficial && f.tituloOficial !== f.nombre
        ? `<p class="text-muted small mb-2 ef-line-height-13">${escapar(truncar(f.tituloOficial, 90))}</p>`
        : '';

    return `
        <div class="col-md-6 col-lg-4">
            <div class="ef-card-formacion card h-100 shadow-sm ef-card-hover ef-card-hover-pointer"
                 data-href="/vistas/publico/detalle-formacion.html?uuid=${f.uuid}">
                <div class="card-body d-flex flex-column p-4">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            ${badgeTipoEstudios}
                            ${badgeModalidad}
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
                                <button class="btn btn-sm btn-outline-danger btn-quitar-guardada"
                                        data-uuid="${f.uuid}" title="Quitar de guardadas">
                                    <i class="bi bi-bookmark-x"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
}

function renderPaginacion(data) {
    const contenedor = document.getElementById('paginacion');
    const totalPages = data.totalPages ?? data.page?.totalPages ?? 1;
    if (totalPages <= 1) {
        contenedor.innerHTML = '';
        return;
    }

    const isFirst = data.first ?? (paginaActual === 0);
    const isLast  = data.last  ?? (paginaActual >= totalPages - 1);

    let html = '<ul class="pagination justify-content-center">';
    html += `<li class="page-item ${isFirst ? 'disabled' : ''}">
        <button class="page-link" data-page="${paginaActual - 1}" ${isFirst ? 'disabled' : ''}><i class="bi bi-chevron-left"></i></button></li>`;
    for (let i = 0; i < totalPages; i++) {
        html += `<li class="page-item ${i === paginaActual ? 'active' : ''}">
            <button class="page-link" data-page="${i}">${i + 1}</button></li>`;
    }
    html += `<li class="page-item ${isLast ? 'disabled' : ''}">
        <button class="page-link" data-page="${paginaActual + 1}" ${isLast ? 'disabled' : ''}><i class="bi bi-chevron-right"></i></button></li>`;
    html += '</ul>';
    contenedor.innerHTML = html;
}

function mostrarError(msg) {
    const el = document.getElementById('recuadroAlert');
    el.textContent = msg;
    el.classList.remove('d-none');
}
