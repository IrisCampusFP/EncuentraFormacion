document.addEventListener('DOMContentLoaded', () => {
    cargarFormacionesParaFiltro().then(() => {
        cargarSolicitudes().then(() => {
            const solicitudId = sessionStorage.getItem('gestor_solicitudes_destacar');
            if (solicitudId) {
                sessionStorage.removeItem('gestor_solicitudes_destacar');
                resaltarSolicitud(Number(solicitudId));
            }
        });
    });

    window.addEventListener('ef:solicitud_evento', () => cargarSolicitudes());

    // Listeners
    document.getElementById('tabPendientes').addEventListener('click', () => {
        document.getElementById('pillsResueltas').classList.add('d-none');
        paginaActual = 0;
        cargarSolicitudes();
    });

    document.getElementById('tabResueltas').addEventListener('click', () => {
        document.getElementById('pillsResueltas').classList.remove('d-none');
        paginaActual = 0;
        cargarSolicitudes();
    });

    document.querySelectorAll('.pill-estado').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.pill-estado').forEach(b => {
                b.classList.remove('active', 'btn-primary');
                b.className = b.className.replace(/btn-([a-z]+)/, 'btn-outline-$1');
            });
            
            btn.classList.add('active');
            btn.className = btn.className.replace(/btn-outline-([a-z]+)/, 'btn-$1');
            if (btn.dataset.estado === 'TODAS_RESUELTAS') {
                btn.classList.add('btn-primary');
                btn.classList.remove('btn-outline-primary');
            }
            paginaActual = 0;
            cargarSolicitudes();
        });
    });

    document.getElementById('filtroFormacion').addEventListener('change', () => {
        paginaActual = 0;
        cargarSolicitudes();
    });

    let debounceTimer;
    document.getElementById('buscarEstudiante').addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            paginaActual = 0;
            cargarSolicitudes();
        }, 300);
    });
});

let paginaActual = 0;

async function cargarFormacionesParaFiltro() {
    try {
        const res = await fetch('/gestor/formaciones?size=100', { credentials: 'include' });
        if (res.ok) {
            const data = await res.json();
            const sel = document.getElementById('filtroFormacion');
            data.content.forEach(f => {
                const opt = document.createElement('option');
                opt.value = f.id;
                opt.textContent = f.nombre;
                sel.appendChild(opt);
            });
            const filtroId = sessionStorage.getItem('gestor_solicitudes_formacion_id');
            if (filtroId) {
                sessionStorage.removeItem('gestor_solicitudes_formacion_id');
                sel.value = filtroId;
            }
        }
    } catch (e) { console.error('Error cargando formaciones', e); }
}

function buildUrl() {
    let url = `/gestor/solicitudes?page=${paginaActual}&size=20&sort=fechaSolicitud,desc`;

    const tabPendientes = document.getElementById('tabPendientes').classList.contains('active');
    
    if (tabPendientes) {
        url += '&estados=PENDIENTE';
    } else {
        const pillActiva = document.querySelector('.pill-estado.active').dataset.estado;
        if (pillActiva === 'TODAS_RESUELTAS') {
            url += '&estados=ACEPTADA&estados=RECHAZADA&estados=CANCELADA';
        } else {
            url += `&estados=${pillActiva}`;
        }
    }

    const formacionId = document.getElementById('filtroFormacion').value;
    if (formacionId) {
        url += `&formacionId=${formacionId}`;
    }

    const nombreEstudiante = document.getElementById('buscarEstudiante').value.trim();
    if (nombreEstudiante) {
        url += `&nombreEstudiante=${encodeURIComponent(nombreEstudiante)}`;
    }

    return url;
}

async function safeCount(url) {
    try {
        const r = await fetch(url, { credentials: 'include' });
        if (!r.ok) return null;
        return await r.json();
    } catch { return null; }
}

async function actualizarContadores() {
    const formacionId = document.getElementById('filtroFormacion').value;
    const nombreEstudiante = document.getElementById('buscarEstudiante').value.trim();

    function contarUrl(estado) {
        let url = `/gestor/solicitudes/count?estado=${estado}`;
        if (formacionId) url += `&formacionId=${formacionId}`;
        if (nombreEstudiante) url += `&nombreEstudiante=${encodeURIComponent(nombreEstudiante)}`;
        return url;
    }

    const [nPend, nAcep, nRech, nCanc] = await Promise.all([
        safeCount(contarUrl('PENDIENTE')),
        safeCount(contarUrl('ACEPTADA')),
        safeCount(contarUrl('RECHAZADA')),
        safeCount(contarUrl('CANCELADA')),
    ]);

    const hayError = [nPend, nAcep, nRech, nCanc].some(n => n === null);
    const nTotal = hayError ? null : nAcep + nRech + nCanc;

    const badgePend = document.getElementById('badgePendientes');
    badgePend.classList.toggle('d-none', nPend === null);
    if (nPend !== null) badgePend.textContent = nPend;

    const badgeRes = document.getElementById('badgeResueltas');
    badgeRes.classList.toggle('d-none', nTotal === null);
    if (nTotal !== null) badgeRes.textContent = nTotal;

    document.getElementById('cntTodas').textContent      = nTotal !== null ? `(${nTotal})` : '';
    document.getElementById('cntAceptadas').textContent  = nAcep  !== null ? `(${nAcep})`  : '';
    document.getElementById('cntRechazadas').textContent = nRech  !== null ? `(${nRech})`  : '';
    document.getElementById('cntCanceladas').textContent = nCanc  !== null ? `(${nCanc})`  : '';
}

async function cargarSolicitudes() {
    const url = buildUrl();
    const lista = document.getElementById('listaSolicitudes');
    lista.innerHTML = '<div class="text-center text-muted py-4">Cargando...</div>';

    try {
        const r = await fetch(url, { credentials: 'include' });
        if (!r.ok) throw new Error('Error al cargar solicitudes');
        const data = await r.json();
        renderTabla(data.content || []);
        renderPaginacion(paginaActual, data.totalPages || 1, 'paginacion', p => { paginaActual = p; cargarSolicitudes(); });
    } catch (e) {
        lista.innerHTML = '<div class="alert alert-danger">Error al cargar las solicitudes.</div>';
    }
    actualizarContadores();
}

function renderTabla(items) {
    const lista = document.getElementById('listaSolicitudes');
    if (!items.length) {
        lista.innerHTML = '<div class="card ef-card"><div class="card-body text-center text-muted py-5"><i class="bi bi-inbox fs-1 d-block mb-3"></i>No hay solicitudes con los filtros actuales</div></div>';
        document.getElementById('paginacion').innerHTML = '';
        return;
    }

    const isPendientes = document.getElementById('tabPendientes').classList.contains('active');

    let thead = isPendientes
        ? `<tr><th>Estudiante</th><th>Email</th><th>Formación</th><th>Fecha solicitud</th><th>Acciones</th></tr>`
        : `<tr><th>Estudiante</th><th>Email</th><th>Formación</th><th>Fecha solicitud</th><th>Fecha resolución</th><th>Estado</th></tr>`;

    let tbody = items.map(s => {
        let fila = `<td>
            <div class="fw-semibold">${escapeHtml(s.estudianteNombre)} ${escapeHtml(s.estudianteApellidos || '')}</div>
        </td>
        <td><span class="text-muted small">${escapeHtml(s.estudianteEmail)}</span></td>
        <td><div class="text-truncate ef-col-formacion" title="${escapeHtml(s.formacionNombre)}">${escapeHtml(s.formacionNombre)}</div></td>
        <td><small class="text-muted">${formatFechaHora(s.fechaCreacion)}</small></td>`;

        if (isPendientes) {
            fila += `<td>
                <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-ef-accept js-accion-solicitud" data-id="${s.id}" data-estado="ACEPTADA" title="Aceptar">
                        <i class="bi bi-check-lg"></i> Aceptar
                    </button>
                    <button class="btn btn-sm btn-outline-danger js-accion-solicitud" data-id="${s.id}" data-estado="RECHAZADA" title="Rechazar">
                        <i class="bi bi-x-lg"></i> Rechazar
                    </button>
                </div>
            </td>`;
        } else {
            fila += `<td><small class="text-muted">${formatFechaHora(s.fechaResolucion) || '—'}</small></td>`;
            fila += `<td><span class="badge ${badgeEstado(s.estado)}">${s.estado}</span></td>`;
        }
        return `<tr data-solicitud-id="${s.id}">${fila}</tr>`;
    }).join('');

    lista.innerHTML = `
        <div class="card ef-card overflow-hidden">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="ef-thead">${thead}</thead>
                    <tbody>${tbody}</tbody>
                </table>
            </div>
        </div>
    `;

    lista.querySelectorAll('.js-accion-solicitud').forEach(btn => {
        btn.addEventListener('click', () => cambiarEstado(Number(btn.dataset.id), btn.dataset.estado));
    });
}

async function cambiarEstado(id, estado) {
    const accion = estado === 'ACEPTADA' ? 'aceptar' : 'rechazar';
    const ok = await efConfirm({
        title: estado === 'ACEPTADA' ? 'Aceptar solicitud' : 'Rechazar solicitud',
        message: `¿Seguro que quieres ${accion} esta solicitud? Se enviará una notificación al estudiante.`,
        confirmText: estado === 'ACEPTADA' ? 'Aceptar' : 'Rechazar',
        variant: estado === 'ACEPTADA' ? 'primary' : 'danger'
    });
    if (!ok) return;
    try {
        const r = await fetch(`/gestor/solicitudes/${id}/estado`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado })
        });
        if (!r.ok) throw new Error('Error al cambiar estado');
        cargarSolicitudes();
    } catch (e) {
        await efAlert({ title: 'Error', message: 'No se pudo cambiar el estado de la solicitud.', variant: 'danger' });
    }
}

function badgeEstado(estado) {
    const m = { PENDIENTE: 'bg-warning text-dark', ACEPTADA: 'bg-success', RECHAZADA: 'bg-danger', CANCELADA: 'bg-secondary' };
    return m[estado] ?? 'bg-secondary';
}

function formatFecha(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatFechaHora(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleString('es-ES', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(unsafe) {
    if (!unsafe) return '';
    return String(unsafe)
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

async function resaltarSolicitud(id) {
    try {
        const r = await fetch(`/gestor/solicitudes/${id}`, { credentials: 'include' });
        if (!r.ok) return;
        const sol = await r.json();

        const filaExistente = document.querySelector(`tr[data-solicitud-id="${id}"]`);
        if (filaExistente) {
            filaExistente.scrollIntoView({ behavior: 'smooth', block: 'center' });
            filaExistente.classList.add('ef-fila-resaltada');
            return;
        }

        const banner = document.createElement('div');
        banner.className = 'alert alert-info d-flex align-items-center gap-3 mb-3';
        banner.innerHTML = `
            <i class="bi bi-info-circle-fill fs-5"></i>
            <div class="flex-grow-1">
                <strong>${escapeHtml(sol.estudianteNombre)} ${escapeHtml(sol.estudianteApellidos || '')}</strong>
                &mdash; ${escapeHtml(sol.formacionNombre)}
                <span class="badge ${badgeEstado(sol.estado)} ms-2">${sol.estado}</span>
            </div>
            <button type="button" class="btn-close" aria-label="Cerrar"></button>`;
        banner.querySelector('.btn-close').addEventListener('click', () => banner.remove());

        const lista = document.getElementById('listaSolicitudes');
        lista.parentNode.insertBefore(banner, lista);
        banner.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) { /* silencioso */ }
}

function renderPaginacion(actual, total, containerId, onPage) {
    const el = document.getElementById(containerId);
    if (total <= 1) { el.innerHTML = ''; return; }
    el.innerHTML = `
        <button class="btn btn-outline-secondary" ${actual === 0 ? 'disabled' : ''} id="btnPrev">
          <i class="bi bi-chevron-left"></i> Anterior
        </button>
        <span class="text-muted">Página ${actual + 1} de ${total}</span>
        <button class="btn btn-outline-secondary" ${actual >= total - 1 ? 'disabled' : ''} id="btnNext">
          Siguiente <i class="bi bi-chevron-right"></i>
        </button>`;
    el.querySelector('#btnPrev')?.addEventListener('click', () => onPage(actual - 1));
    el.querySelector('#btnNext')?.addEventListener('click', () => onPage(actual + 1));
}

