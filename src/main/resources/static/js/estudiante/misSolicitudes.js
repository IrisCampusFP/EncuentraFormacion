document.addEventListener('DOMContentLoaded', async () => {
    await cargarSolicitudes();
    const solicitudId = sessionStorage.getItem('mis_solicitudes_destacar');
    if (solicitudId) {
        sessionStorage.removeItem('mis_solicitudes_destacar');
        abrirDetallePorId(Number(solicitudId));
    }
});

window.addEventListener('ef:solicitud_evento', () => cargarSolicitudes());

let paginaActual = 0;
const PAGE_SIZE = 10;

async function cargarSolicitudes() {
    const lista = document.getElementById('listaSolicitudes');
    lista.innerHTML = '<div class="text-center text-muted py-4">Cargando...</div>';
    try {
        const r = await fetch(`/solicitudes-formacion?page=${paginaActual}&size=${PAGE_SIZE}`, { credentials: 'include' });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        const data = await r.json();
        const items = data.content ?? data;
        const total = data.totalPages ?? 1;

        if (!items.length) {
            lista.innerHTML = `
                <div class="card">
                  <div class="card-body text-center text-muted py-5">
                    <i class="bi bi-send d-block fs-1 mb-2 opacity-25"></i>
                    Aún no has enviado ninguna solicitud de admisión.
                  </div>
                </div>`;
            document.getElementById('paginacion').innerHTML = '';
            return;
        }

        lista.innerHTML = items.map(s => `
            <div class="card mb-2 solicitud-card ef-list-card-hover" data-id="${s.id}" style="cursor:pointer" role="button">
              <div class="card-body d-flex justify-content-between align-items-center gap-3">
                <div class="flex-grow-1 overflow-hidden">
                  <h3 class="h6 fw-semibold mb-1 text-truncate">${s.formacionNombre}</h3>
                  <p class="mb-0 text-muted small text-truncate">${s.centroNombre}</p>
                  <small class="text-muted">${formatFechaHora(s.fechaSolicitud)}</small>
                </div>
                <div class="flex-shrink-0 d-flex align-items-center gap-2">
                  <span class="badge ${badgeEstado(s.estado)}">${etiquetaEstado(s.estado)}</span>
                  <i class="bi bi-chevron-right text-muted small"></i>
                </div>
              </div>
            </div>`).join('');

        renderPaginacion(paginaActual, total, 'paginacion', p => { paginaActual = p; cargarSolicitudes(); });

        lista.querySelectorAll('.solicitud-card').forEach(card => {
            card.addEventListener('click', () => {
                const id = parseInt(card.dataset.id, 10);
                const s = items.find(x => x.id === id);
                if (s) abrirDetalle(s);
            });
        });
    } catch (e) {
        lista.innerHTML = '';
        mostrarError(typeof e === 'string' ? e : 'Error al cargar solicitudes');
    }
}

function abrirDetalle(s) {
    document.getElementById('modalNombreFormacion').textContent = s.formacionNombre;
    document.getElementById('modalCentro').textContent = s.centroNombre;
    document.getElementById('modalEstadoBadge').innerHTML = `<span class="badge ${badgeEstado(s.estado)}">${etiquetaEstado(s.estado)}</span>`;
    document.getElementById('modalFechaSolicitud').textContent = formatFechaHora(s.fechaSolicitud);

    const filaRespuesta = document.getElementById('filaFechaRespuesta');
    if (s.fechaRespuesta) {
        document.getElementById('modalFechaRespuesta').textContent = formatFechaHora(s.fechaRespuesta);
        filaRespuesta.classList.remove('d-none');
    } else {
        filaRespuesta.classList.add('d-none');
    }

    const footer = document.getElementById('modalFooter');
    const btnCancelar = s.estado === 'PENDIENTE'
        ? `<button class="btn btn-outline-danger btn-sm btn-cancelar-modal" data-id="${s.id}">
             <i class="bi bi-x-circle me-1"></i>Cancelar solicitud
           </button>`
        : '';
    footer.innerHTML = `
        <div class="d-flex justify-content-between align-items-center w-100 gap-2">
          <div>${btnCancelar}</div>
          <div class="d-flex gap-2">
            <a href="/vistas/publico/perfil-centro.html?uuid=${s.centroUuid}" class="btn btn-outline-secondary btn-sm">
              <i class="bi bi-building me-1"></i>Ver centro
            </a>
            <a href="/vistas/publico/detalle-formacion.html?uuid=${s.formacionUuid}" class="btn btn-primary btn-sm">
              <i class="bi bi-book me-1"></i>Ver formación
            </a>
          </div>
        </div>`;

    footer.querySelector('.btn-cancelar-modal')?.addEventListener('click', () => {
        bootstrap.Modal.getInstance(document.getElementById('modalDetalle'))?.hide();
        confirmarCancelacion(s.id, null);
    });

    new bootstrap.Modal(document.getElementById('modalDetalle')).show();
}

async function confirmarCancelacion(id, btnOrigen) {
    const ok = await efConfirm({
        title:       'Cancelar solicitud',
        message:     '¿Seguro que quieres cancelar esta solicitud de formación?',
        confirmText: 'Sí, cancelar',
        cancelText:  'Volver',
        variant:     'danger',
    });
    if (!ok) return;
    cancelarSolicitud(id, btnOrigen);
}

async function cancelarSolicitud(id, btnOrigen) {
    if (btnOrigen) btnOrigen.disabled = true;
    try {
        const res = await fetch(`/solicitudes-formacion/${id}`, { method: 'DELETE', credentials: 'include' });
        if (!res.ok) { const d = await res.json(); throw d.errorMsg || `Error ${res.status}`; }
        cargarSolicitudes();
    } catch (err) {
        if (btnOrigen) btnOrigen.disabled = false;
        mostrarError(typeof err === 'string' ? err : 'No se pudo cancelar la solicitud');
    }
}

function badgeEstado(estado) {
    const m = {
        PENDIENTE:  'bg-warning text-dark',
        ACEPTADA:   'bg-success',
        RECHAZADA:  'bg-danger',
        CANCELADA:  'bg-secondary'
    };
    return m[estado] ?? 'bg-secondary';
}

function etiquetaEstado(estado) {
    const m = {
        PENDIENTE:  'Pendiente',
        ACEPTADA:   'Aceptada',
        RECHAZADA:  'Rechazada',
        CANCELADA:  'Cancelada'
    };
    return m[estado] ?? estado;
}

function formatFecha(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatFechaHora(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function mostrarError(msg) {
    const el = document.getElementById('recuadroAlert');
    el.textContent = msg;
    el.classList.remove('d-none');
}

async function abrirDetallePorId(id) {
    try {
        const r = await fetch(`/solicitudes-formacion/${id}`, { credentials: 'include' });
        if (!r.ok) return;
        abrirDetalle(await r.json());
    } catch (e) { /* silencioso: no interrumpe la carga normal */ }
}

function renderPaginacion(actual, total, containerId, onPage) {
    const el = document.getElementById(containerId);
    if (total <= 1) { el.innerHTML = ''; return; }
    el.innerHTML = `
        <button class="btn btn-sm btn-outline-secondary" ${actual === 0 ? 'disabled' : ''} id="btnPrev">
          <i class="bi bi-chevron-left"></i> Anterior
        </button>
        <span class="text-muted small">Página ${actual + 1} de ${total}</span>
        <button class="btn btn-sm btn-outline-secondary" ${actual >= total - 1 ? 'disabled' : ''} id="btnNext">
          Siguiente <i class="bi bi-chevron-right"></i>
        </button>`;
    el.querySelector('#btnPrev')?.addEventListener('click', () => onPage(actual - 1));
    el.querySelector('#btnNext')?.addEventListener('click', () => onPage(actual + 1));
}
