document.addEventListener('DOMContentLoaded', () => {
    if (new URLSearchParams(location.search).get('preview') === 'true') {
        ['navbar-container', 'breadcrumbContainer', 'footer-container'].forEach(id =>
            document.getElementById(id)?.remove()
        );
        document.getElementById('contenidoPrincipal')?.classList.add('mt-4');
        document.addEventListener('click', e => {
            if (e.target.closest('a[href]')) { e.preventDefault(); return; }
            if (e.target.closest('[data-href]')) { e.stopImmediatePropagation(); }
        }, true);
    }

    const uuid = resolverUuid('centro');
    if (!uuid) { mostrarError('Centro no especificado.'); return; }
    cargarPerfil(uuid);
});

function resolverUuid(tipo) {
    const params = new URLSearchParams(window.location.search);
    if (params.get('uuid')) return params.get('uuid');
    const m = window.location.hash.match(new RegExp(`^#\\/${tipo}\\/([^/]+)`));
    return m ? m[1] : null;
}

async function cargarPerfil(uuid) {
    try {
        const res = await fetch(`/centros/${uuid}/perfil`);
        if (res.status === 404) { mostrarError('Este centro no existe.'); return; }
        if (!res.ok) throw new Error();
        const centro = await res.json();
        renderizarPerfil(centro, uuid);
        cargarFormaciones(uuid, 0);
    } catch {
        mostrarError('No se pudo cargar la información del centro.');
    }
}

async function cargarFormaciones(uuid, page) {
    const container  = document.getElementById('formacionesContainer');
    const pagination = document.getElementById('paginacionFormaciones');
    if (!container) return;

    container.innerHTML = '<div class="d-flex justify-content-center py-4"><div class="spinner-border spinner-border-sm text-primary" role="status"></div></div>';
    if (pagination) pagination.innerHTML = '';

    try {
        const res = await fetch(`/centros/${uuid}/formaciones?page=${page}&size=4`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        renderizarFormaciones(data, uuid);
    } catch {
        container.innerHTML = '<p class="text-muted">No se pudieron cargar las formaciones.</p>';
    }
}

function renderizarFormaciones(data, uuid) {
    const container    = document.getElementById('formacionesContainer');
    const pagination   = document.getElementById('paginacionFormaciones');
    if (!container) return;

    const totalElements = data.totalElements ?? data.page?.totalElements ?? data.content?.length ?? 0;
    const totalPages    = data.totalPages    ?? data.page?.totalPages    ?? 1;
    const currentPage   = data.number        ?? data.page?.number        ?? 0;

    if (totalElements === 0) {
        container.innerHTML = '<p class="text-muted">Este centro no tiene formaciones activas.</p>';
        return;
    }

    const cardsHtml = data.content.map(f => {
        const bm = BADGE_MODALIDAD[f.modalidad] || 'ef-badge-default';
        const { texto: precioTxt, color: precioColor } = formatPrecio(f.precio);

        let badgeTipoEstudios = '';
        if (f.tipoEstudios?.nombre) {
            const ct = BADGE_TIPO_ESTUDIOS[f.tipoEstudios.nombre] || 'ef-badge-default';
            badgeTipoEstudios = `<span class="badge ef-badge-custom-sm ${ct} me-1">${escapar(f.tipoEstudios.nombre)}</span>`;
        }

        const subtitulo = f.tituloOficial && f.tituloOficial !== f.nombre
            ? `<p class="text-muted ef-fs-xs mb-2 ef-line-height-13">${escapar(truncar(f.tituloOficial, 75))}</p>`
            : '';

        const estrellas = f.valoracionMedia
            ? `<span class="small">${renderEstrellas(f.valoracionMedia)} <span class="text-muted">(${f.totalValoraciones})</span></span>`
            : '<span class="text-muted small">Sin valoraciones</span>';

        return `
        <div class="col-md-6">
            <div class="card h-100 shadow-sm ef-card-hover ef-card-hover-pointer"
                 data-href="/vistas/publico/detalle-formacion.html?uuid=${f.uuid}">
                <div class="card-body p-3 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            ${badgeTipoEstudios}
                            <span class="badge ef-badge-custom-sm ${bm}">${formatModalidad(f.modalidad)}</span>
                        </div>
                        ${f.horario ? `<span class="small ef-color-secondary flex-shrink-0 ms-2"><i class="bi bi-clock"></i> ${formatHorario(f.horario)}</span>` : ''}
                    </div>
                    <h6 class="card-title fw-semibold mb-1">${escapar(f.nombre)}</h6>
                    ${subtitulo}
                    <div class="flex-grow-1"></div>
                    <div class="border-top pt-2 mt-auto">
                        <div class="d-flex justify-content-between align-items-center">
                            ${f.precio != null ? `<strong class="${precioColor}">${precioTxt}</strong>` : '<span></span>'}
                            ${estrellas}
                        </div>
                    </div>
                </div>
            </div>
        </div>`;
    }).join('');

    container.innerHTML = `<div class="row g-4">${cardsHtml}</div>`;

    if (pagination) {
        pagination.innerHTML = totalPages > 1
            ? renderPaginacion(totalPages, currentPage, uuid)
            : '';
        pagination.querySelectorAll('[data-page]').forEach(btn => {
            btn.addEventListener('click', () => cargarFormaciones(uuid, parseInt(btn.dataset.page)));
        });
    }
}

function renderPaginacion(totalPages, currentPage, uuid) {
    const prev = currentPage > 0;
    const next = currentPage < totalPages - 1;

    let nums = '';
    for (let i = 0; i < totalPages; i++) {
        const active = i === currentPage;
        nums += `<button class="ef-page-dot ${active ? 'ef-page-dot--active' : ''}"
                         data-page="${i}"
                         aria-label="Página ${i + 1}"
                         ${active ? 'aria-current="page" disabled' : ''}>${i + 1}</button>`;
    }

    return `
    <div class="ef-pagination d-flex align-items-center justify-content-center gap-2 pt-3">
        <button class="ef-page-arrow" data-page="${currentPage - 1}" ${!prev ? 'disabled' : ''} aria-label="Página anterior">
            <i class="bi bi-chevron-left"></i>
        </button>
        <div class="d-flex gap-1">${nums}</div>
        <button class="ef-page-arrow" data-page="${currentPage + 1}" ${!next ? 'disabled' : ''} aria-label="Página siguiente">
            <i class="bi bi-chevron-right"></i>
        </button>
    </div>`;
}

function toSlug(nombre) {
    return nombre.toLowerCase()
        .normalize('NFD').replace(/[̀-ͯ]/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
}

function renderizarPerfil(c, uuid) {
    document.title = `${c.nombreComercial} — EncuentraFormación`;
    const slug = toSlug(c.nombreComercial);
    const centroUrl = `/vistas/publico/perfil-centro.html?uuid=${uuid}`;
    const parentUrl = referrerPath();
    breadcrumbPush(c.nombreComercial, centroUrl, parentUrl);
    breadcrumbRender('breadcrumbList');
    history.replaceState({ realUrl: centroUrl }, '', `/vistas/publico/perfil-centro.html#/centro/${uuid}/${slug}`);

    const verificadoBadge = c.verificado
        ? '<i class="bi bi-patch-check-fill ef-verified-icon ef-verified-icon--baseline ms-2" title="Centro verificado"></i>'
        : '';
    const tipoSlug = c.tipo ? c.tipo.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, '-') : '';
    const tipoCentroBadge = c.tipo ? `<span class="ef-tipo-badge ef-tipo-badge--${tipoSlug}">${escapar(c.tipo)}</span>` : '';

    const estrellasHtml = c.valoracionMedia
        ? `<div class="d-flex align-items-center mb-3">
             <div class="me-2">${renderEstrellas(c.valoracionMedia)}</div>
             <span class="fw-semibold me-1">${c.valoracionMedia.toFixed(1)}</span>
             <span class="text-muted small">(${c.totalValoraciones} valoraciones)</span>
           </div>`
        : '<p class="text-muted small mb-3">Sin valoraciones aún</p>';

    let faqHtml = '';
    if (c.faqs && c.faqs.length > 0) {
        faqHtml = '<div class="accordion mt-3" id="accordionFaq">';
        faqHtml += c.faqs.map((faq, i) => `
            <div class="accordion-item">
                <h2 class="accordion-header" id="heading${i}">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse${i}">
                        ${escapar(faq.pregunta)}
                    </button>
                </h2>
                <div id="collapse${i}" class="accordion-collapse collapse" data-bs-parent="#accordionFaq">
                    <div class="accordion-body text-secondary">
                        ${escapar(faq.respuesta)}
                    </div>
                </div>
            </div>`).join('');
        faqHtml += '</div>';
    } else {
        faqHtml = '<p class="text-muted">No hay preguntas frecuentes disponibles.</p>';
    }

    document.getElementById('contenidoPrincipal').innerHTML = `
        <div class="row g-4">
            <div class="col-lg-8">
                <!-- Cabecera del Centro -->
                <div class="card border-0 shadow-sm p-4 mb-4">
                    <div class="d-flex align-items-baseline gap-1 flex-wrap mb-2">
                        <h1 class="h3 fw-bold mb-0">${escapar(c.nombreComercial)}</h1>
                        ${verificadoBadge}
                        ${c.tipo ? `<span class="ms-1">${tipoCentroBadge}</span>` : ''}
                    </div>
                    ${estrellasHtml}
                    ${c.descripcion ? `<p class="text-secondary mb-0">${escapar(c.descripcion)}</p>` : ''}
                </div>

                <!-- Formaciones Activas -->
                <div class="card border-0 shadow-sm p-4 mb-4">
                    <h5 class="fw-semibold mb-3"><i class="bi bi-book-half me-2 ef-color-primary"></i>Formaciones Activas</h5>
                    <div id="formacionesContainer"></div>
                    <div id="paginacionFormaciones"></div>
                </div>

                <!-- FAQ -->
                <div class="card border-0 shadow-sm p-4">
                    <h5 class="fw-semibold mb-0"><i class="bi bi-question-circle me-2 ef-color-accent"></i>Preguntas Frecuentes</h5>
                    ${faqHtml}
                </div>
            </div>

            <div class="col-lg-4">
                <!-- Información de Contacto -->
                <div class="card border-0 shadow-sm p-4 sticky-top ef-sticky-top-2rem">
                    <h6 class="fw-semibold mb-3"><i class="bi bi-info-circle me-2 ef-color-secondary"></i>Información de Contacto</h6>
                    <ul class="list-unstyled mb-0">
                        ${c.direccion ? `<li class="mb-3 d-flex"><i class="bi bi-geo-alt text-muted me-2 mt-1"></i> <div><span>${escapar(c.direccion)}</span><br><span class="text-muted small">${escapar(c.localidad)}, ${escapar(c.provincia)}</span></div></li>` : ''}
                        ${c.telefono ? `<li class="mb-3"><i class="bi bi-telephone text-muted me-2"></i> ${escapar(c.telefono)}</li>` : ''}
                        ${c.email ? `<li class="mb-3"><i class="bi bi-envelope text-muted me-2"></i> <a href="mailto:${escapar(c.email)}" class="text-decoration-none">${escapar(c.email)}</a></li>` : ''}
                        ${c.paginaWeb ? `<li><i class="bi bi-globe text-muted me-2"></i> <a href="${escapar(c.paginaWeb)}" target="_blank" rel="noopener" class="text-decoration-none">Visitar web</a></li>` : ''}
                    </ul>
                </div>
            </div>
        </div>
    `;
}

function mostrarError(msg) {
    document.getElementById('contenidoPrincipal').innerHTML = `
        <div class="alert alert-warning text-center py-5">
            <i class="bi bi-exclamation-triangle fs-1 d-block mb-3"></i>
            <p class="mb-3">${msg}</p>
            <a href="/" class="btn btn-primary btn-sm">Volver al buscador</a>
        </div>`;
}
