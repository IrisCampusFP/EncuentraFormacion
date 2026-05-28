let centroIdGlobal = null;
let tiposEstudiosData = [];
let formacionesLista = [];
let formacionEditandoId = null;
let faqsLista = [];
let faqEditandoId = null;
let paginaActual = 0;
const PAGE_SIZE = 3;

document.addEventListener('DOMContentLoaded', () => {
    cargarCentro();
    cargarFormaciones();
    cargarFaqs();
    cargarStats();
    cargarTiposEstudios();

    window.addEventListener('ef:nuevo_mensaje', cargarStats);

    document.getElementById('btnEditarCentro').addEventListener('click', () => {
        new bootstrap.Modal(document.getElementById('modalEditarCentro')).show();
    });
    document.getElementById('formCentro').addEventListener('submit', guardarCentro);

    document.getElementById('btnNuevaFormacion').addEventListener('click', abrirDialogCrear);
    document.getElementById('btnCancelarFormacion').addEventListener('click', cerrarDialogFormacion);
    document.getElementById('formFormacion').addEventListener('submit', guardarFormacion);

    document.getElementById('btnPagAnterior').addEventListener('click', () => cargarFormaciones(paginaActual - 1));
    document.getElementById('btnPagSiguiente').addEventListener('click', () => cargarFormaciones(paginaActual + 1));

    document.getElementById('btnNuevaFaq').addEventListener('click', abrirDialogFaqCrear);
    document.getElementById('btnCancelarFaq').addEventListener('click', cerrarDialogFaq);
    document.getElementById('formFaq').addEventListener('submit', guardarFaq);

    // Navegar al chat o solicitudes con filtro de formación sin exponerlo en la URL
    document.getElementById('listaFormaciones').addEventListener('click', e => {
        const linkChat = e.target.closest('a[data-formacion-id][href*="chat"]');
        if (linkChat) {
            e.preventDefault();
            sessionStorage.setItem('chat_filtro_formacion', linkChat.dataset.formacionId);
            window.location.href = linkChat.href;
            return;
        }
        const linkSolicitudes = e.target.closest('a[data-formacion-id][href*="solicitudes"]');
        if (linkSolicitudes) {
            e.preventDefault();
            sessionStorage.setItem('gestor_solicitudes_formacion_id', linkSolicitudes.dataset.formacionId);
            window.location.href = linkSolicitudes.href;
            return;
        }
    });

    // Event delegation para formaciones
    document.getElementById('listaFormaciones').addEventListener('click', e => {
        const btnEditar = e.target.closest('[data-action="editar"]');
        if (btnEditar) {
            const f = formacionesLista.find(x => x.id === Number(btnEditar.dataset.id));
            if (f) abrirDialogEditar(f);
            return;
        }
        const btnToggle = e.target.closest('[data-action="toggle"]');
        if (btnToggle) {
            const id = Number(btnToggle.dataset.id);
            const reactivar = btnToggle.dataset.activa === 'false';
            toggleFormacion(id, reactivar);
        }
    });

    // Event delegation para FAQ (evita problemas de escape con onclick inline)
    document.getElementById('listaFaq').addEventListener('click', e => {
        const btnEditar = e.target.closest('.btn-faq-editar');
        if (btnEditar) { abrirDialogFaqEditar(Number(btnEditar.dataset.id)); return; }
        const btnEliminar = e.target.closest('.btn-faq-eliminar');
        if (btnEliminar) { eliminarFaq(Number(btnEliminar.dataset.id)); return; }
        const btnSubir = e.target.closest('.btn-faq-subir');
        if (btnSubir) { guardarOrdenFaq(Number(btnSubir.dataset.id), 'UP'); return; }
        const btnBajar = e.target.closest('.btn-faq-bajar');
        if (btnBajar) { guardarOrdenFaq(Number(btnBajar.dataset.id), 'DOWN'); }
    });

    const iframe = document.getElementById('iframePreviewPerfil');

    document.getElementById('linkPerfilPublico').addEventListener('click', e => {
        e.preventDefault();
        const href = e.currentTarget.getAttribute('href');
        if (!href || href === '#') return;
        iframe.src = href + (href.includes('?') ? '&' : '?') + 'preview=true';
        new bootstrap.Modal(document.getElementById('modalPreviewPerfil')).show();
    });

    document.getElementById('modalPreviewPerfil').addEventListener('hidden.bs.modal', () => {
        iframe.src = 'about:blank';
    });

});

// --- Centro ---

async function cargarCentro() {
    try {
        const res = await fetch('/gestor/centro');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const centro = await res.json();
        centroIdGlobal = centro.id;

        document.getElementById('centroNombre').textContent = centro.nombreComercial;

        const TIPO_LABEL = { PUBLICO: 'Público', PRIVADO: 'Privado', CONCERTADO: 'Concertado' };
        const badgeTipo = document.getElementById('centroBadgeTipo');
        const tipoSlug = centro.tipo ? centro.tipo.toLowerCase() : '';
        badgeTipo.className = tipoSlug ? `ef-tipo-badge ef-tipo-badge--${tipoSlug}` : '';
        badgeTipo.textContent = TIPO_LABEL[centro.tipo] || centro.tipo || '';

        if (centro.verificado) {
            document.getElementById('centroBadgeVerif').classList.remove('d-none');
        } else {
            document.getElementById('centroBadgeVerif').classList.add('d-none');
        }

        const partes = [];
        if (centro.localidad) partes.push(centro.localidad);
        if (centro.provincia) partes.push(centro.provincia);
        if (centro.telefono) partes.push(`<i class="bi bi-telephone me-1"></i>${escapeHtml(centro.telefono)}`);
        if (centro.email) partes.push(`<i class="bi bi-envelope me-1"></i>${escapeHtml(centro.email)}`);
        if (centro.paginaWeb) partes.push(`<i class="bi bi-globe me-1"></i><a href="${escapeHtml(centro.paginaWeb)}" target="_blank" rel="noopener">Web</a>`);
        document.getElementById('centroContacto').innerHTML = partes.join(' &nbsp;·&nbsp; ');

        document.getElementById('centroDescripcionInfo').textContent = centro.descripcion || '';

        if (centro.uuid) {
            document.getElementById('linkPerfilPublico').href = `/vistas/publico/perfil-centro.html?uuid=${centro.uuid}`;
        }

        document.getElementById('cNombreComercial').value = centro.nombreComercial || '';
        document.getElementById('cCodigo').value = centro.codigo || '';
        document.getElementById('cTipo').value = centro.tipo || 'PUBLICO';
        document.getElementById('cTelefono').value = centro.telefono || '';
        document.getElementById('cEmail').value = centro.email || '';
        document.getElementById('cWeb').value = centro.paginaWeb || '';
        document.getElementById('cDireccion').value = centro.direccion || '';
        document.getElementById('cLocalidad').value = centro.localidad || '';
        document.getElementById('cProvincia').value = centro.provincia || '';
        document.getElementById('cDescripcion').value = centro.descripcion || '';

    } catch (e) {
        console.error('Error cargando centro:', e);
        document.getElementById('centroNombre').textContent = 'Error al cargar';
    }
}

async function guardarCentro(e) {
    e.preventDefault();
    const data = {
        tipo: document.getElementById('cTipo').value,
        telefono: document.getElementById('cTelefono').value,
        email: document.getElementById('cEmail').value,
        paginaWeb: document.getElementById('cWeb').value,
        direccion: document.getElementById('cDireccion').value,
        localidad: document.getElementById('cLocalidad').value,
        provincia: document.getElementById('cProvincia').value,
        descripcion: document.getElementById('cDescripcion').value
    };
    try {
        const res = await fetch('/gestor/centro', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error();
        bootstrap.Modal.getInstance(document.getElementById('modalEditarCentro')).hide();
        cargarCentro();
    } catch (e) {
        await efAlert({ title: 'Error', message: 'No se pudieron guardar los datos del centro.', variant: 'danger' });
    }
}

// --- Stats sidebar ---

async function cargarStats() {
    try {
        const [resSol, resMsg] = await Promise.all([
            fetch('/gestor/solicitudes/count-pendientes'),
            fetch('/gestor/chat/count-no-leidos')
        ]);
        if (resSol.ok) {
            const count = await resSol.json();
            document.getElementById('statSolicitudesPendientes').textContent = count;
        }
        if (resMsg.ok) {
            const count = await resMsg.json();
            document.getElementById('statMensajesSinLeer').textContent = count;
        }
    } catch (e) {
        console.error('Error cargando stats:', e);
    }
}

// --- Formaciones ---

async function cargarTiposEstudios() {
    try {
        const res = await fetch('/tipo-estudios');
        if (res.ok) {
            tiposEstudiosData = await res.json();
            const sel = document.getElementById('fTipoEstudios');
            sel.innerHTML = '<option value="">Seleccionar...</option>';
            tiposEstudiosData.forEach(g => {
                const opt = document.createElement('option');
                opt.value = g.id;
                opt.textContent = g.nombre;
                sel.appendChild(opt);
            });
        }
    } catch (e) { console.error('Error tipos estudios:', e); }
}

async function cargarFormaciones(page = 0) {
    paginaActual = page;
    try {
        const res = await fetch(`/gestor/formaciones?page=${page}&size=${PAGE_SIZE}`);
        if (!res.ok) throw new Error();
        const pageData = await res.json();
        if (pageData.content.length === 0 && page > 0) {
            return cargarFormaciones(page - 1);
        }
        renderFormaciones(pageData.content);
        renderPaginacion(pageData);
    } catch (e) {
        document.getElementById('listaFormaciones').innerHTML =
            '<div class="alert alert-danger">Error cargando formaciones</div>';
    }
}

function renderFormaciones(listaOriginal) {
    formacionesLista = listaOriginal || [];
    const cont = document.getElementById('listaFormaciones');
    cont.innerHTML = '';
    if (formacionesLista.length === 0) {
        cont.innerHTML = '<p class="text-muted small">No tienes formaciones creadas todavía.</p>';
        return;
    }
    formacionesLista.forEach(f => {
        const isActiva = f.activa !== false;
        const dimClass = isActiva ? '' : 'ef-formacion-inactiva';

        const typeBadgeClass = f.tipoEstudiosNombre
            ? (BADGE_TIPO_ESTUDIOS[f.tipoEstudiosNombre] || 'ef-badge-default')
            : '';
        const typeBadge = f.tipoEstudiosNombre
            ? `<span class="badge ef-badge-custom ${typeBadgeClass} me-1">${escapeHtml(f.tipoEstudiosNombre)}</span>`
            : '';
        const inactivaBadge = !isActiva
            ? `<span class="badge ef-badge-custom ef-badge-default">Inactiva</span>`
            : '';

        let statsInfo = '';
        if (f.solicitudesPendientes > 0)
            statsInfo += `<a href="/vistas/gestor/solicitudes.html" data-formacion-id="${f.id}" class="text-warning-emphasis small me-3 text-decoration-none"><i class="bi bi-hourglass-split me-1"></i>${f.solicitudesPendientes} ${f.solicitudesPendientes > 1 ? 'solicitudes' : 'solicitud'}</a>`;
        if (f.chatsActivos > 0)
            statsInfo += `<a href="/vistas/gestor/chat.html" class="text-primary small text-decoration-none" data-formacion-id="${f.id}"><i class="bi bi-chat-dots me-1"></i>${f.chatsActivos} chat${f.chatsActivos > 1 ? 's' : ''}</a>`;
        if (!statsInfo) statsInfo = '<span class="text-muted small">Sin actividad</span>';

        const btnToggle = isActiva
            ? `<button class="btn btn-sm btn-outline-danger" data-action="toggle" data-id="${f.id}" data-activa="true"><i class="bi bi-eye-slash me-1"></i>Desactivar</button>`
            : `<button class="btn btn-sm btn-outline-success" data-action="toggle" data-id="${f.id}" data-activa="false"><i class="bi bi-eye me-1"></i>Reactivar</button>`;

        const detalles = [
            f.modalidad ? `<i class="bi bi-laptop me-1"></i>${escapeHtml(f.modalidad)}` : null,
            f.horario ? `<i class="bi bi-clock me-1"></i>${escapeHtml(f.horario)}` : null,
            f.duracionHoras ? `<i class="bi bi-hourglass me-1"></i>${f.duracionHoras}h` : null,
            f.precio != null ? `<i class="bi bi-currency-euro me-1"></i>${f.precio === 0 ? 'Gratis' : f.precio}` : null,
        ].filter(Boolean).join('<span class="mx-2 text-muted">·</span>');

        cont.insertAdjacentHTML('beforeend', `
            <div class="card border-0 shadow-sm">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-1">
                        <div class="${dimClass}">${typeBadge}${inactivaBadge}<h6 class="fw-semibold mt-2 mb-0">${escapeHtml(f.nombre)}</h6></div>
                        <div class="d-flex gap-2 flex-shrink-0 ms-3">
                            <button class="btn btn-sm btn-outline-secondary ${dimClass}" data-action="editar" data-id="${f.id}" title="Editar">
                                <i class="bi bi-pencil"></i>
                            </button>
                            ${btnToggle}
                        </div>
                    </div>
                    <div class="${dimClass}">
                        <p class="text-muted small mb-2">${detalles}</p>
                        <div class="pt-2 border-top">${statsInfo}</div>
                    </div>
                </div>
            </div>`);
    });
}

function renderPaginacion(pageData) {
    const cont = document.getElementById('paginacionFormaciones');
    // Spring Data 3.3+ anida la metadata bajo "page"; versiones anteriores la ponen en raíz
    const meta = pageData.page ?? pageData;
    const totalPages = meta.totalPages ?? 0;

    if (totalPages <= 1) {
        cont.classList.add('d-none');
        return;
    }
    cont.classList.remove('d-none');

    const current = meta.number ?? 0;
    const size = meta.size ?? PAGE_SIZE;
    const totalElements = meta.totalElements ?? 0;
    const from = current * size + 1;
    const to = Math.min((current + 1) * size, totalElements);
    document.getElementById('paginacionInfo').textContent = `${from}–${to} de ${totalElements}`;

    const numsCont = document.getElementById('paginacionNums');
    numsCont.innerHTML = '';
    getPaginasVisibles(current, totalPages).forEach(p => {
        if (p === '…') {
            const span = document.createElement('span');
            span.className = 'text-muted px-1';
            span.style.fontSize = '.8rem';
            span.textContent = '…';
            numsCont.appendChild(span);
        } else {
            const btn = document.createElement('button');
            btn.className = 'ef-page-dot' + (p === current ? ' ef-page-dot--active' : '');
            btn.textContent = p + 1;
            if (p !== current) btn.addEventListener('click', () => cargarFormaciones(p));
            numsCont.appendChild(btn);
        }
    });

    document.getElementById('btnPagAnterior').disabled = current === 0;
    document.getElementById('btnPagSiguiente').disabled = current >= totalPages - 1;
}

function getPaginasVisibles(current, total) {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages = new Set([0, total - 1, current]);
    if (current > 0) pages.add(current - 1);
    if (current < total - 1) pages.add(current + 1);
    const sorted = [...pages].sort((a, b) => a - b);
    const result = [];
    for (let i = 0; i < sorted.length; i++) {
        if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push('…');
        result.push(sorted[i]);
    }
    return result;
}

function abrirDialogCrear() {
    formacionEditandoId = null;
    document.getElementById('dialogFormacionTitulo').textContent = 'Nueva formación';
    document.getElementById('formFormacion').reset();
    document.getElementById('dialogFormacionError').classList.add('d-none');
    document.getElementById('dialogFormacion').showModal();
}

function abrirDialogEditar(f) {
    formacionEditandoId = f.id;
    document.getElementById('dialogFormacionTitulo').textContent = 'Editar formación';
    document.getElementById('formFormacion').reset();
    document.getElementById('dialogFormacionError').classList.add('d-none');
    document.getElementById('fNombre').value = f.nombre || '';
    document.getElementById('fTipoEstudios').value = f.tipoEstudiosId || '';
    document.getElementById('fHorario').value = f.horario || '';
    document.getElementById('fModalidad').value = f.modalidad || '';
    document.getElementById('fDuracion').value = f.duracionHoras || '';
    document.getElementById('fPrecio').value = f.precio ?? '';
    document.getElementById('fTituloOficial').value = f.tituloOficial || '';
    if (f.fechaInicio) document.getElementById('fFechaInicio').value = f.fechaInicio;
    if (f.fechaFin) document.getElementById('fFechaFin').value = f.fechaFin;
    document.getElementById('fDescripcion').value = f.descripcion || '';
    document.getElementById('dialogFormacion').showModal();
}

function cerrarDialogFormacion() {
    document.getElementById('dialogFormacion').close();
}

async function guardarFormacion(e) {
    e.preventDefault();
    const btn = document.getElementById('btnGuardarFormacion');
    btn.disabled = true;
    const data = {
        nombre: document.getElementById('fNombre').value,
        tipoEstudiosId: document.getElementById('fTipoEstudios').value ? parseInt(document.getElementById('fTipoEstudios').value) : null,
        horario: document.getElementById('fHorario').value,
        modalidad: document.getElementById('fModalidad').value,
        duracionHoras: document.getElementById('fDuracion').value ? parseInt(document.getElementById('fDuracion').value) : null,
        precio: document.getElementById('fPrecio').value !== '' ? parseFloat(document.getElementById('fPrecio').value) : null,
        tituloOficial: document.getElementById('fTituloOficial').value,
        fechaInicio: document.getElementById('fFechaInicio').value || null,
        fechaFin: document.getElementById('fFechaFin').value || null,
        descripcion: document.getElementById('fDescripcion').value
    };
    try {
        const url = formacionEditandoId ? `/gestor/formaciones/${formacionEditandoId}` : `/gestor/formaciones`;
        const res = await fetch(url, {
            method: formacionEditandoId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error(await res.text() || 'Error guardando');
        cerrarDialogFormacion();
        cargarFormaciones(formacionEditandoId ? paginaActual : 0);
    } catch (err) {
        const errDiv = document.getElementById('dialogFormacionError');
        errDiv.textContent = 'Error: ' + err.message;
        errDiv.classList.remove('d-none');
    } finally {
        btn.disabled = false;
    }
}

async function toggleFormacion(id, reactivar) {
    const ok = await efConfirm({
        title: reactivar ? 'Reactivar formación' : 'Desactivar formación',
        message: reactivar
            ? '¿Seguro que quieres reactivar esta formación? Volverá a ser visible en el buscador.'
            : '¿Seguro que quieres desactivar esta formación? Dejará de ser visible en el buscador.',
        confirmText: reactivar ? 'Reactivar' : 'Desactivar',
        variant: reactivar ? 'primary' : 'warning'
    });
    if (!ok) return;
    try {
        const method = reactivar ? 'PUT' : 'DELETE';
        const body = reactivar ? JSON.stringify({ activa: true }) : undefined;
        const headers = reactivar ? { 'Content-Type': 'application/json' } : {};
        const res = await fetch(`/gestor/formaciones/${id}`, { method, headers, body });
        if (!res.ok) throw new Error();
        cargarFormaciones(paginaActual);
    } catch (e) {
        await efAlert({ title: 'Error', message: 'No se pudo cambiar el estado de la formación.', variant: 'danger' });
    }
}

// --- FAQs ---

async function cargarFaqs() {
    try {
        const res = await fetch('/gestor/faq');
        if (!res.ok) throw new Error();
        faqsLista = await res.json();
        renderFaqs();
    } catch (e) {
        document.getElementById('listaFaq').innerHTML =
            '<div class="alert alert-danger">Error cargando preguntas frecuentes</div>';
    }
}

function renderFaqs() {
    const cont = document.getElementById('listaFaq');
    cont.innerHTML = '';
    if (faqsLista.length === 0) {
        cont.innerHTML = '<p class="text-muted small">No hay preguntas frecuentes todavía.</p>';
        return;
    }
    faqsLista.forEach((faq, idx) => {
        const isFirst = idx === 0;
        const isLast = idx === faqsLista.length - 1;
        cont.insertAdjacentHTML('beforeend', `
            <div class="ef-faq-item d-flex gap-2 align-items-start">
                <div class="d-flex flex-column align-items-center gap-0 pt-1 flex-shrink-0">
                    <button class="btn-faq-subir btn btn-link p-0 text-muted lh-1" data-id="${faq.id}"
                        ${isFirst ? 'disabled aria-disabled="true"' : ''} title="Subir">
                        <i class="bi bi-chevron-up" style="font-size:.8rem;"></i>
                    </button>
                    <button class="btn-faq-bajar btn btn-link p-0 text-muted lh-1" data-id="${faq.id}"
                        ${isLast ? 'disabled aria-disabled="true"' : ''} title="Bajar">
                        <i class="bi bi-chevron-down" style="font-size:.8rem;"></i>
                    </button>
                </div>
                <div class="flex-grow-1 min-w-0">
                    <p class="fw-semibold mb-0 small">${escapeHtml(faq.pregunta)}</p>
                    <hr class="ef-faq-divider">
                    <p class="text-muted mb-0 small">${escapeHtml(faq.respuesta)}</p>
                </div>
                <div class="d-flex gap-1 flex-shrink-0 pt-1">
                    <button class="btn-faq-editar btn btn-sm btn-outline-secondary border-0 p-1" data-id="${faq.id}" title="Editar">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn-faq-eliminar btn btn-sm btn-outline-danger border-0 p-1" data-id="${faq.id}" title="Eliminar">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>`);
    });
}

function abrirDialogFaqCrear() {
    faqEditandoId = null;
    document.getElementById('dialogFaqTitulo').textContent = 'Nueva pregunta frecuente';
    document.getElementById('formFaq').reset();
    document.getElementById('dialogFaqError').classList.add('d-none');
    document.getElementById('dialogFaq').showModal();
}

function abrirDialogFaqEditar(id) {
    const faq = faqsLista.find(f => f.id === id);
    if (!faq) return;
    faqEditandoId = faq.id;
    document.getElementById('dialogFaqTitulo').textContent = 'Editar pregunta';
    document.getElementById('faqPregunta').value = faq.pregunta;
    document.getElementById('faqRespuesta').value = faq.respuesta;
    document.getElementById('dialogFaqError').classList.add('d-none');
    document.getElementById('dialogFaq').showModal();
}

function cerrarDialogFaq() {
    document.getElementById('dialogFaq').close();
}

async function guardarFaq(e) {
    e.preventDefault();
    const data = {
        pregunta: document.getElementById('faqPregunta').value,
        respuesta: document.getElementById('faqRespuesta').value
    };
    try {
        const url = faqEditandoId ? `/gestor/faq/${faqEditandoId}` : `/gestor/faq`;
        const method = faqEditandoId ? 'PUT' : 'POST';
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error();
        cerrarDialogFaq();
        cargarFaqs();
    } catch (e) {
        const errDiv = document.getElementById('dialogFaqError');
        errDiv.textContent = 'Error al guardar la pregunta.';
        errDiv.classList.remove('d-none');
    }
}

async function eliminarFaq(id) {
    const ok = await efConfirm({
        title: 'Eliminar pregunta',
        message: '¿Seguro que quieres eliminar esta pregunta frecuente? Esta acción no se puede deshacer.',
        confirmText: 'Eliminar',
        variant: 'danger'
    });
    if (!ok) return;
    try {
        const res = await fetch(`/gestor/faq/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error();
        cargarFaqs();
    } catch (e) {
        await efAlert({ title: 'Error', message: 'No se pudo eliminar la pregunta.', variant: 'danger' });
    }
}

async function guardarOrdenFaq(id, direccion) {
    const idx = faqsLista.findIndex(f => f.id === id);
    if (idx === -1) return;
    const targetIdx = direccion === 'UP' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= faqsLista.length) return;

    const ordenA = faqsLista[idx].orden;
    const ordenB = faqsLista[targetIdx].orden;

    try {
        await Promise.all([
            fetch(`/gestor/faq/${faqsLista[idx].id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orden: ordenB })
            }),
            fetch(`/gestor/faq/${faqsLista[targetIdx].id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orden: ordenA })
            })
        ]);
        cargarFaqs();
    } catch (e) {
        await efAlert({ title: 'Error', message: 'No se pudo actualizar el orden.', variant: 'danger' });
    }
}

// --- Utils ---

function escapeHtml(s) {
    if (s == null) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
