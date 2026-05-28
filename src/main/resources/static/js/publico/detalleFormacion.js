document.addEventListener('DOMContentLoaded', () => {
    const uuid = resolverUuid('formacion');
    if (!uuid) {
        mostrarError('Formación no especificada.');
        return;
    }
    cargarDetalle(uuid);
});

function resolverUuid(tipo) {
    const params = new URLSearchParams(window.location.search);
    if (params.get('uuid')) return params.get('uuid');
    const m = window.location.hash.match(new RegExp(`^#\\/${tipo}\\/([^/]+)`));
    return m ? m[1] : null;
}

async function cargarDetalle(uuid) {
    try {
        const res = await fetch(`/formaciones/${uuid}`);
        if (res.status === 404) { mostrarError('Esta formación no existe o ya no está disponible.'); return; }
        if (!res.ok) throw new Error();
        const f = await res.json();
        renderizarDetalle(f);
    } catch {
        mostrarError('No se pudo cargar la información. Inténtalo de nuevo.');
    }
}

function toSlug(nombre) {
    return nombre.toLowerCase()
        .normalize('NFD').replace(/[̀-ͯ]/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
}


function renderizarDetalle(f) {
    document.title = `${f.nombre} — EncuentraFormación`;

    const uuid = new URLSearchParams(window.location.search).get('uuid')
        || window.location.hash.match(/^#\/formacion\/([^/]+)/)?.[1];
    const slug = toSlug(f.nombre);
    const realUrl = `/vistas/publico/detalle-formacion.html?uuid=${uuid}`;
    history.replaceState({ realUrl }, '', `/vistas/publico/detalle-formacion.html#/formacion/${uuid}/${slug}`);

    const parentUrl = referrerPath();
    breadcrumbPush(f.nombre, realUrl, parentUrl);
    breadcrumbRender('breadcrumbList');

    const { texto: precio, color: precioColor } = formatPrecio(f.precio);

    const estrellasSummary = f.valoracionMedia
        ? `<span>${renderEstrellas(f.valoracionMedia)}</span> <strong>${f.valoracionMedia}</strong> <span class="text-muted">(${f.totalValoraciones} valoracion${f.totalValoraciones !== 1 ? 'es' : ''})</span>`
        : `<span class="text-muted fst-italic">Sin valoraciones aún</span>`;

    const attrs = [
        f.tipoEstudios?.nombre ? { icon: 'bi-mortarboard-fill', value: escapar(f.tipoEstudios.nombre), color: 'ef-color-primary' } : null,
        { icon: 'bi-display',       value: formatModalidad(f.modalidad), color: 'ef-color-accent' },
        f.horario ? { icon: 'bi-clock', value: formatHorario(f.horario), color: 'ef-color-secondary' } : null,
        f.duracionHoras ? { icon: 'bi-hourglass-split', value: `${f.duracionHoras} h`,  color: 'ef-color-success' } : null,
        (f.fechaInicio || f.fechaFin) ? {
            icon: 'bi-calendar3',
            value: f.fechaInicio && f.fechaFin
                ? `${formatFecha(f.fechaInicio)} – ${formatFecha(f.fechaFin)}`
                : f.fechaInicio ? `Desde ${formatFecha(f.fechaInicio)}` : `Hasta ${formatFecha(f.fechaFin)}`,
            color: 'ef-color-info'
        } : null,
    ].filter(Boolean);

    const attrsHtml = attrs.map((a, i) => `
        ${i > 0 ? '<span class="ef-attr-sep"></span>' : ''}
        <span class="ef-attr-item">
            <i class="bi ${a.icon} ${a.color}"></i>
            <span>${a.value}</span>
        </span>`).join('');

    const valoracionesHtml = _renderListaValoraciones(f.valoraciones);

    const btnSolicitarHtml = f.centroTieneGestor
        ? `<button class="btn btn-primary w-100" id="btnSolicitar">
               <i class="bi bi-send-fill me-2"></i>Solicitar admisión
           </button>`
        : '';

    document.getElementById('contenidoPrincipal').innerHTML = `

        <!-- CABECERA DE LA FORMACIÓN -->
        <div class="ef-detail-hero mb-4">
            <h1 class="ef-detail-title">${escapar(f.nombre)}</h1>
            ${f.tituloOficial ? `<p class="ef-detail-subtitle">${escapar(f.tituloOficial)}</p>` : ''}
            <div class="ef-attrs-row mt-3 mb-3">${attrsHtml}</div>
            <div class="ef-stars-summary">${estrellasSummary}</div>
        </div>

        <div class="row g-4 align-items-start">

            <!-- COLUMNA PRINCIPAL -->
            <div class="col-lg-8">

                <!-- DESCRIPCIÓN -->
                ${f.descripcion ? `
                <div class="card p-4 mb-4">
                    <h2 class="ef-section-heading"><i class="bi bi-info-circle-fill me-2 ef-color-primary"></i>Descripción</h2>
                    <p class="mb-0 ef-line-height-175">${escapar(f.descripcion)}</p>
                </div>` : ''}

                <!-- VALORACIONES -->
                <div class="card p-4" id="seccionValoraciones">
                    <div class="d-flex align-items-center justify-content-between mb-4">
                        <h2 class="ef-section-heading mb-0"><i class="bi bi-star-fill me-2 ef-color-warning"></i>Valoraciones</h2>
                        ${f.valoracionMedia ? `
                        <span class="text-muted small">${renderEstrellas(f.valoracionMedia)} <strong class="text-body">${f.valoracionMedia}</strong> / 5</span>` : ''}
                    </div>
                    <!-- El formulario/zona de acción se inyecta aquí por JS -->
                    <div id="zonaFormValoracion"></div>
                    <div class="ef-reviews-list" id="listaValoraciones">
                        ${valoracionesHtml}
                    </div>
                </div>
            </div>

            <!-- COLUMNA LATERAL -->
            <div class="col-lg-4">
                <div class="ef-sidebar-sticky">

                <!-- PRECIO + CTAs -->
                <div class="card p-4 mb-4 ef-price-card">
                    <div class="ef-price-label">Precio</div>
                    ${f.precio != null
                        ? `<div class="ef-price-value ${precioColor}">${precio}</div>`
                        : `<div class="small ef-color-muted">${precio}</div>`}
                    <div class="d-flex flex-column gap-2 mt-3" id="ctaButtons">
                        <button class="btn btn-outline-secondary w-100" id="btnGuardar" disabled>
                            <i class="bi bi-bookmark me-2"></i>Guardar formación
                        </button>
                        ${f.centroTieneGestor
                            ? `<button class="btn btn-outline-primary w-100" id="btnContactar">
                                   <i class="bi bi-chat-dots-fill me-2"></i>Contactar con el centro
                               </button>`
                            : ''}
                        ${btnSolicitarHtml}
                    </div>
                </div>

                <!-- INFO DEL CENTRO -->
                <div class="card p-4">
                    <h2 class="ef-section-heading mb-3"><i class="bi bi-building-fill me-2 ef-color-primary"></i>Centro</h2>
                    <p class="fw-bold mb-1 ef-fs-lg">
                        ${escapar(f.centroNombre)}${f.centroVerificado ? ` <i class="bi bi-patch-check-fill ef-verified-icon ef-verified-icon--center" title="Centro verificado"></i>` : ''}${f.centroTipo ? (() => { const b = BADGE_TIPO_CENTRO[f.centroTipo] || { class: 'ef-badge-default', label: f.centroTipo }; return `<span class="badge ef-badge-custom ef-badge-detail ${b.class}">${b.label}</span>`; })() : ''}
                    </p>
                    ${f.centroProvincia || f.centroLocalidad ? `
                    <p class="text-muted mb-3 ef-fs-sm">
                        <i class="bi bi-geo-alt me-1"></i>${[f.centroLocalidad, f.centroProvincia].filter(Boolean).map(escapar).join(', ')}
                    </p>` : ''}
                    ${f.centroDescripcion ? `<p class="mb-3 ef-fs-sm ef-color-secondary ef-line-height-16">${truncar(escapar(f.centroDescripcion), 160)}</p>` : ''}
                    <div class="ef-contact-list">
                        ${f.centroDireccion ? `<div class="ef-contact-item"><i class="bi bi-geo-alt-fill"></i><span>${escapar(f.centroDireccion)}</span></div>` : ''}
                        ${f.centroTelefono ? `<div class="ef-contact-item"><i class="bi bi-telephone-fill"></i><span>${escapar(f.centroTelefono)}</span></div>` : ''}
                        ${f.centroEmail ? `<div class="ef-contact-item"><i class="bi bi-envelope-fill"></i><span>${escapar(f.centroEmail)}</span></div>` : ''}
                        ${f.centroPaginaWeb ? `<div class="ef-contact-item"><i class="bi bi-globe2"></i><a href="${escapar(f.centroPaginaWeb)}" target="_blank" rel="noopener noreferrer">${escapar(f.centroPaginaWeb).replace(/^https?:\/\//, '')}</a></div>` : ''}
                    </div>
                    <a href="/vistas/publico/perfil-centro.html?uuid=${f.centroUuid}"
                       class="btn btn-outline-primary w-100 mt-3 d-flex align-items-center justify-content-center gap-2"
                       id="btnVerPerfil">
                        <i class="bi bi-arrow-right ef-arrow-fix"></i> Ver perfil del centro
                    </a>
                </div>

                </div><!-- /ef-sidebar-sticky -->
            </div>
        </div>`;

    inicializarBotonesCTA(f);
    inicializarZonaValoracion(f);

    window.addEventListener('ef:solicitud_evento', async (e) => {
        const eventoUuid = e.detail?.evento?.formacionUuid;
        if (eventoUuid && eventoUuid !== f.uuid) return;

        const slot = document.querySelector('#ctaButtons .ef-solicitud-estado, #btnSolicitar');
        if (!slot) return;
        try {
            const r = await fetch(`/solicitudes-formacion/check/${f.uuid}`, { credentials: 'include' });
            if (!r.ok) return;
            const sol = await r.json();

            if (sol.estado === 'RECHAZADA' || sol.estado === 'CANCELADA') {
                const nuevoBtn = document.createElement('button');
                nuevoBtn.className = 'btn btn-primary w-100';
                nuevoBtn.id = 'btnSolicitar';
                nuevoBtn.innerHTML = '<i class="bi bi-send-fill me-2"></i>Solicitar admisión';
                slot.parentElement.replaceChild(nuevoBtn, slot);
                const returnTo = encodeURIComponent(window.location.href);
                await _inicializarBtnSolicitar(f, true, `/vistas/auth/login.html?returnTo=${returnTo}`);
            } else {
                _actualizarBtnSolicitar(slot, sol);
            }
        } catch { /* no crítico */ }
    });
}

// ─── Valoraciones ─────────────────────────────────────────────────────────────

function _renderListaValoraciones(valoraciones) {
    if (!valoraciones || valoraciones.length === 0) {
        return `<div class="text-center py-4" id="emptyValoraciones">
                    <i class="bi bi-chat-square-text ef-fs-xl text-muted"></i>
                    <p class="text-muted mt-2 mb-0">Esta formación aún no tiene valoraciones.</p>
                </div>`;
    }
    return valoraciones.map(v => _renderValoracionItem(v, false)).join('');
}

function _renderValoracionItem(v, esMia) {
    const nombre = v.nombreEstudiante || 'Estudiante';
    const fechaTexto = v.fechaModificacion
        ? `${formatFechaHora(v.fechaModificacion)} <span class="text-muted fst-italic">(editado)</span>`
        : formatFechaHora(v.fecha);
    const badgeMia = esMia ? `<span class="ef-badge-propia ms-2">Tu valoración</span>` : '';

    return `<div class="ef-review-item" data-valoracion-id="${v.id}">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <div class="d-flex align-items-center gap-2">
                        <div class="ef-review-avatar">${nombre[0].toUpperCase()}</div>
                        <div>
                            <div class="fw-semibold ef-fs-md">${escapar(nombre)}${badgeMia}</div>
                            <div class="text-muted ef-fs-xs">${fechaTexto}</div>
                        </div>
                    </div>
                    <div class="ef-stars-sm">${renderEstrellas(v.estrellas)}</div>
                </div>
                ${v.comentario ? `<p class="mb-0 ef-fs-md ef-color-secondary">${escapar(v.comentario)}</p>` : ''}
            </div>`;
}

function _htmlFormularioValoracion(valoracionExistente) {
    const estrellas = valoracionExistente?.estrellas ?? 0;
    const comentario = valoracionExistente?.comentario ?? '';
    const esEdicion = !!valoracionExistente;

    const starsHtml = [5,4,3,2,1].map(n => `
        <input type="radio" id="star${n}" name="estrellas" value="${n}" ${estrellas === n ? 'checked' : ''}>
        <label for="star${n}" title="${n} estrella${n > 1 ? 's' : ''}"><i class="bi bi-star-fill"></i></label>
    `).join('');

    return `<div class="ef-review-form" id="formValoracionWrapper">
                <div class="d-flex align-items-center gap-2 mb-3">
                    <div class="ef-review-avatar" id="avatarFormValoracion">?</div>
                    <div class="fw-semibold ef-fs-md" id="nombreFormValoracion">Tú</div>
                </div>
                <div class="ef-star-picker mb-3" id="starPicker" role="radiogroup" aria-label="Puntuación">
                    ${starsHtml}
                </div>
                <div id="starError" class="text-danger ef-fs-xs mb-2 d-none">Selecciona una puntuación.</div>
                <textarea class="form-control ef-fs-sm mb-3" id="comentarioValoracion"
                    rows="3" maxlength="1000"
                    placeholder="Describe tu experiencia con esta formación (opcional)">${escapar(comentario)}</textarea>
                <div class="d-flex gap-2">
                    <button class="btn btn-primary btn-sm" id="btnEnviarValoracion">
                        <i class="bi bi-send me-1"></i>${esEdicion ? 'Guardar cambios' : 'Publicar valoración'}
                    </button>
                    <button class="btn btn-link btn-sm text-muted p-0" id="btnCancelarValoracion">Cancelar</button>
                </div>
            </div>`;
}

async function inicializarZonaValoracion(f) {
    const zona = document.getElementById('zonaFormValoracion');
    if (!zona) return;

    const perfil = await _obtenerPerfil();
    const esEstudiante = perfil?.roles?.some(r => r.nombre === 'ESTUDIANTE') ?? false;
    const returnTo = encodeURIComponent(window.location.href);

    if (!perfil) {
        _inyectarBtnEscribirResenaSinAuth(returnTo);
        return;
    }

    if (!esEstudiante) return;

    // Consultamos si ya tiene valoración propia
    let miValoracion = null;
    try {
        const r = await fetch(`/valoraciones/usuario/${f.uuid}`, { credentials: 'include' });
        if (r.ok) miValoracion = await r.json();
        // 204 → sin valoración previa
    } catch { /* no crítico */ }

    if (miValoracion) {
        _mostrarValoracionPropia(f, perfil, miValoracion);
    } else {
        _inyectarBtnEscribirResena(f, perfil);
    }
}

function _inyectarBtnEscribirResena(f, perfil) {
    const heading = document.querySelector('#seccionValoraciones .d-flex');
    if (!heading) return;

    const btn = document.createElement('button');
    btn.className = 'btn btn-outline-primary btn-sm';
    btn.id = 'btnEscribirResena';
    btn.innerHTML = '<i class="bi bi-pencil-square me-1"></i>Escribir una reseña';
    heading.appendChild(btn);

    btn.addEventListener('click', () => {
        btn.remove();
        _mostrarFormularioCrear(f, perfil);
    });
}

function _inyectarBtnEscribirResenaSinAuth(returnTo) {
    const heading = document.querySelector('#seccionValoraciones .d-flex');
    if (!heading) return;

    const btn = document.createElement('button');
    btn.className = 'btn btn-outline-primary btn-sm';
    btn.id = 'btnEscribirResena';
    btn.innerHTML = '<i class="bi bi-pencil-square me-1"></i>Escribir una reseña';
    heading.appendChild(btn);

    const zona = document.getElementById('zonaFormValoracion');

    btn.addEventListener('click', () => {
        if (zona.querySelector('.ef-review-cta')) return;
        zona.innerHTML = `<div class="ef-review-cta">
            <div class="ef-review-cta__stars">${[1,2,3,4,5].map(() => '<i class="bi bi-star-fill"></i>').join('')}</div>
            <p class="ef-review-cta__title">¿Has cursado esta formación?</p>
            <p class="ef-review-cta__sub">Comparte tu experiencia y ayuda a otros estudiantes a elegir.</p>
            <a href="/vistas/auth/login.html?returnTo=${returnTo}" class="btn btn-outline-primary btn-sm">
                Inicia sesión para valorar
            </a>
        </div>`;
    });
}

function _mostrarFormularioCrear(f, perfil) {
    const zona = document.getElementById('zonaFormValoracion');
    zona.innerHTML = _htmlFormularioValoracion(null);
    _rellenarAvatarForm(perfil);
    _inicializarLimpiezaErrorEstrellas();

    document.getElementById('btnCancelarValoracion').addEventListener('click', () => {
        zona.innerHTML = '';
        _inyectarBtnEscribirResena(f, perfil);
    });

    document.getElementById('btnEnviarValoracion').addEventListener('click', async () => {
        const estrellas = _leerEstrellas();
        if (!estrellas) { document.getElementById('starError').classList.remove('d-none'); return; }
        document.getElementById('starError').classList.add('d-none');

        const comentario = document.getElementById('comentarioValoracion').value.trim();
        const btn = document.getElementById('btnEnviarValoracion');
        btn.disabled = true;

        try {
            const res = await fetch('/valoraciones', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ formacionUuid: f.uuid, estrellas, comentario: comentario || null })
            });
            if (res.status === 201) {
                const nueva = await res.json();
                _onValoracionGuardada(f, perfil, nueva);
            } else if (res.status === 409) {
                // Ya existía — recargamos estado
                btn.disabled = false;
                inicializarZonaValoracion(f);
            } else {
                btn.disabled = false;
            }
        } catch { btn.disabled = false; }
    });
}

function _mostrarValoracionPropia(f, perfil, valoracion) {
    // Limpia cualquier formulario abierto
    document.getElementById('zonaFormValoracion').innerHTML = '';

    // Coloca la reseña propia al inicio de la lista con badge
    const lista = document.getElementById('listaValoraciones');
    const empty = document.getElementById('emptyValoraciones');
    if (empty) empty.remove();

    const existente = lista.querySelector(`[data-valoracion-id="${valoracion.id}"]`);
    if (existente) existente.remove();

    lista.insertAdjacentHTML('afterbegin', _renderValoracionItem(valoracion, true));

    // Botón "Editar tu reseña" en el heading de la sección
    _inyectarBtnEditar(f, perfil, valoracion);
}

function _inyectarBtnEditar(f, perfil, valoracion) {
    const heading = document.querySelector('#seccionValoraciones .d-flex');
    if (!heading) return;

    heading.querySelector('#btnEscribirResena')?.remove();
    heading.querySelector('#btnEditarResena')?.remove();

    const btn = document.createElement('button');
    btn.className = 'btn btn-outline-secondary btn-sm';
    btn.id = 'btnEditarResena';
    btn.innerHTML = '<i class="bi bi-pencil me-1"></i>Editar tu reseña';
    heading.appendChild(btn);

    btn.addEventListener('click', () => {
        _abrirFormularioEdicion(f, perfil, valoracion);
    });
}

function _abrirFormularioEdicion(f, perfil, valoracion) {
    // Quita el botón del heading mientras el formulario está abierto
    const heading = document.querySelector('#seccionValoraciones .d-flex');
    heading?.querySelector('#btnEditarResena')?.remove();

    const zona = document.getElementById('zonaFormValoracion');
    zona.innerHTML = _htmlFormularioValoracion(valoracion);
    _rellenarAvatarForm(perfil);
    _inicializarLimpiezaErrorEstrellas();

    document.getElementById('btnCancelarValoracion').addEventListener('click', () => {
        _mostrarValoracionPropia(f, perfil, valoracion);
    });

    document.getElementById('btnEnviarValoracion').addEventListener('click', async () => {
        const estrellas = _leerEstrellas();
        if (!estrellas) { document.getElementById('starError').classList.remove('d-none'); return; }
        document.getElementById('starError').classList.add('d-none');

        const comentario = document.getElementById('comentarioValoracion').value.trim();
        const btn = document.getElementById('btnEnviarValoracion');
        btn.disabled = true;

        try {
            const res = await fetch(`/valoraciones/${valoracion.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ estrellas, comentario: comentario || null })
            });
            if (res.ok) {
                const actualizada = await res.json();
                _onValoracionGuardada(f, perfil, actualizada);
            } else {
                btn.disabled = false;
            }
        } catch { btn.disabled = false; }
    });
}

function _onValoracionGuardada(f, perfil, valoracion) {
    _mostrarValoracionPropia(f, perfil, valoracion);
}

function _inicializarLimpiezaErrorEstrellas() {
    document.querySelectorAll('#starPicker input[type="radio"]').forEach(input => {
        input.addEventListener('change', () => {
            const err = document.getElementById('starError');
            if (err) err.classList.add('d-none');
        });
    });
}

function _rellenarAvatarForm(perfil) {
    const inicial = (perfil?.nombre || 'T')[0].toUpperCase();
    const avatarEl = document.getElementById('avatarFormValoracion');
    const nombreEl = document.getElementById('nombreFormValoracion');
    if (avatarEl) avatarEl.textContent = inicial;
    if (nombreEl && perfil?.nombre) nombreEl.textContent = `${perfil.nombre}${perfil.apellidos ? ' ' + perfil.apellidos : ''}`;
}

function _leerEstrellas() {
    const checked = document.querySelector('#starPicker input[type="radio"]:checked');
    return checked ? parseInt(checked.value, 10) : null;
}

// ─── Lógica de CTAs (Guardar / Contactar / Solicitar) ────────────────────────

async function inicializarBotonesCTA(f) {
    const perfil = await _obtenerPerfil();
    const esEstudiante = perfil?.roles?.some(r => r.nombre === 'ESTUDIANTE') ?? false;
    const returnTo = encodeURIComponent(window.location.href);
    const loginUrl = `/vistas/auth/login.html?returnTo=${returnTo}`;

    _inicializarBtnGuardar(f, esEstudiante, loginUrl);
    _inicializarBtnContactar(f, esEstudiante, loginUrl);
    _inicializarBtnSolicitar(f, esEstudiante, loginUrl);
}

async function _inicializarBtnGuardar(f, esEstudiante, loginUrl) {
    const btn = document.getElementById('btnGuardar');
    if (!btn) return;
    btn.disabled = false;

    if (!esEstudiante) {
        btn.addEventListener('click', () => { window.location.href = `${loginUrl}&guardarFormacion`; });
        return;
    }

    try {
        const r = await fetch(`/favoritos/${f.uuid}/estado`, { credentials: 'include' });
        if (r.ok) {
            const { guardada } = await r.json();
            _actualizarBtnGuardar(btn, guardada);
        }
    } catch { /* no crítico, queda en estado neutro */ }

    btn.addEventListener('click', async () => {
        const activo = btn.dataset.guardada === 'true';
        btn.disabled = true;
        try {
            const method = activo ? 'DELETE' : 'POST';
            const res = await fetch(`/favoritos/${f.uuid}`, { method, credentials: 'include' });
            if (res.status === 204 || res.ok) {
                _actualizarBtnGuardar(btn, !activo);
            } else if (res.status === 409) {
                _actualizarBtnGuardar(btn, true);
            }
        } catch { /* no crítico */ } finally {
            btn.disabled = false;
        }
    });
}

function _inicializarBtnContactar(f, esEstudiante, loginUrl) {
    const btn = document.getElementById('btnContactar');
    if (!btn) return;

    if (!esEstudiante) {
        btn.addEventListener('click', () => { window.location.href = `${loginUrl}&contactarCentro`; });
        return;
    }

    btn.addEventListener('click', async () => {
        btn.disabled = true;
        try {
            const res = await fetch('/chat/iniciar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ centroUuid: f.centroUuid, formacionUuid: f.uuid })
            });
            if (res.ok) {
                const conv = await res.json();
                window.location.href = `/vistas/estudiante/chat.html?conv=${conv.id}`;
            } else {
                btn.disabled = false;
            }
        } catch { btn.disabled = false; }
    });
}

async function _inicializarBtnSolicitar(f, esEstudiante, loginUrl) {
    const btn = document.getElementById('btnSolicitar');
    if (!btn) return;

    if (!esEstudiante) {
        btn.addEventListener('click', () => { window.location.href = `${loginUrl}&solicitarAdmision`; });
        return;
    }

    try {
        const r = await fetch(`/solicitudes-formacion/check/${f.uuid}`, { credentials: 'include' });
        if (r.ok) {
            const sol = await r.json();
            if (sol.estado === 'PENDIENTE' || sol.estado === 'ACEPTADA') {
                _actualizarBtnSolicitar(btn, sol);
                return;
            }
            if (sol.estado === 'RECHAZADA') {
                const aviso = document.createElement('p');
                aviso.className = 'ef-solicitud-rechazada-aviso';
                const enlace = document.createElement('a');
                enlace.href = '#';
                enlace.className = 'ef-solicitud-rechazada-aviso__link';
                enlace.textContent = 'Ver solicitud';
                enlace.addEventListener('click', e => {
                    e.preventDefault();
                    sessionStorage.setItem('mis_solicitudes_destacar', sol.id);
                    window.location.href = '/vistas/estudiante/mis-solicitudes.html';
                });
                aviso.innerHTML = '<i class="bi bi-info-circle me-1"></i>Tu solicitud anterior fue rechazada. ';
                aviso.appendChild(enlace);
                btn.insertAdjacentElement('afterend', aviso);
            }
        }
    } catch { /* no crítico */ }

    btn.addEventListener('click', async () => {
        const ok = await efConfirm({
            title: 'Solicitar admisión',
            message: `¿Quieres enviar una solicitud de admisión para <strong>${escapar(f.nombre)}</strong>?<br>
                      <small class="text-muted mt-1 d-block">El centro recibirá tu nombre y podrá contactarte para continuar el proceso.</small>`,
            confirmText: 'Enviar solicitud',
            variant: 'primary',
        });
        if (!ok) return;
        btn.disabled = true;
        try {
            const res = await fetch('/solicitudes-formacion', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ formacionUuid: f.uuid })
            });
            if (res.status === 201 || res.ok) {
                const sol = await res.json();
                _actualizarBtnSolicitar(btn, sol);
            } else if (res.status === 409) {
                _actualizarBtnSolicitar(btn, { estado: 'PENDIENTE' });
            } else {
                btn.disabled = false;
            }
        } catch { btn.disabled = false; }
    });
}

function _actualizarBtnGuardar(btn, guardada) {
    btn.dataset.guardada = guardada;
    if (guardada) {
        btn.innerHTML = '<i class="bi bi-bookmark-fill me-2"></i>Guardada';
        btn.classList.replace('btn-outline-secondary', 'btn-outline-success');
    } else {
        btn.innerHTML = '<i class="bi bi-bookmark me-2"></i>Guardar formación';
        btn.classList.replace('btn-outline-success', 'btn-outline-secondary');
    }
}

function _actualizarBtnSolicitar(btn, sol) {
    const CONFIG = {
        PENDIENTE: { mod: 'pendiente',  icon: 'bi-hourglass-split', label: 'Solicitud enviada',     sub: 'El centro revisará tu solicitud.' },
        ACEPTADA:  { mod: 'aceptada',   icon: 'bi-check-circle-fill', label: 'Solicitud aceptada',  sub: 'El centro ha aceptado tu solicitud.' },
        RECHAZADA: { mod: 'rechazada',  icon: 'bi-x-circle-fill',   label: 'Solicitud rechazada',   sub: 'El centro no ha podido aceptarte.' },
        CANCELADA: { mod: 'cancelada',  icon: 'bi-slash-circle',    label: 'Solicitud cancelada',   sub: 'Esta solicitud fue cancelada.' },
    };
    const cfg = CONFIG[sol.estado] ?? { mod: 'cancelada', icon: 'bi-circle', label: sol.estado, sub: '' };

    btn.nextElementSibling?.classList.contains('ef-solicitud-rechazada-aviso') &&
        btn.nextElementSibling.remove();

    const wrapper = btn.parentElement;
    const div = document.createElement('div');
    div.className = `ef-solicitud-estado ef-solicitud-estado--${cfg.mod}`;
    div.innerHTML = `
        <i class="bi ${cfg.icon} ef-solicitud-estado__icon"></i>
        <div class="ef-solicitud-estado__body">
            <div class="ef-solicitud-estado__label">${cfg.label}</div>
        </div>`;

    const enlace = document.createElement('a');
    enlace.href = '#';
    enlace.className = 'ef-solicitud-estado__link';
    enlace.innerHTML = 'Ver solicitud <i class="bi bi-arrow-right"></i>';
    enlace.addEventListener('click', e => {
        e.preventDefault();
        if (sol.id) sessionStorage.setItem('mis_solicitudes_destacar', sol.id);
        window.location.href = '/vistas/estudiante/mis-solicitudes.html';
    });
    div.querySelector('.ef-solicitud-estado__body').appendChild(enlace);

    wrapper.replaceChild(div, btn);
}

// ─────────────────────────────────────────────────────────────────────────────

function mostrarError(msg) {
    document.getElementById('contenidoPrincipal').innerHTML = `
        <div class="alert alert-warning text-center py-5">
            <i class="bi bi-exclamation-triangle fs-1 d-block mb-3"></i>
            <p class="mb-3">${msg}</p>
            <a href="/" class="btn btn-primary btn-sm">Volver al buscador</a>
        </div>`;
}
