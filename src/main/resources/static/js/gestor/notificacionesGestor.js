document.addEventListener('DOMContentLoaded', () => {
    cargarNotificaciones();

    fetch('/perfil').then(r => r.ok ? r.json() : null).then(p => {
        if (p) WsClient.init(p.id);
    });

    window.addEventListener('ef:notificacion', () => {
        paginaActual = 0;
        cargarNotificaciones();
    });

    document.getElementById('btnMarcarTodas').addEventListener('click', async () => {
        try {
            const r = await fetch('/notificaciones/gestor/leidas', { method: 'PUT', credentials: 'include' });
            if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
            cargarNotificaciones();
        } catch (e) {
            mostrarError(typeof e === 'string' ? e : 'Error al marcar notificaciones');
        }
    });
});

let paginaActual = 0;

async function cargarNotificaciones() {
    const lista = document.getElementById('listaNotificaciones');
    lista.innerHTML = '<div class="text-center text-muted py-4">Cargando...</div>';
    try {
        const r = await fetch(`/notificaciones/gestor?page=${paginaActual}&size=20`, { credentials: 'include' });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        const data = await r.json();
        const items = data.content ?? data;
        const total = data.totalPages ?? 1;

        if (!items.length) {
            lista.innerHTML = `
                <div class="card">
                  <div class="card-body text-center text-muted py-5">
                    <i class="bi bi-bell-slash d-block fs-1 mb-2 opacity-25"></i>
                    No tienes notificaciones.
                  </div>
                </div>`;
            document.getElementById('paginacion').innerHTML = '';
            return;
        }

        const colapsados = colapsarPorConversacion(items);
        lista.innerHTML = `<div class="notif-list">${renderGrupos(colapsados)}</div>`;
        renderPaginacion(paginaActual, total, 'paginacion', p => { paginaActual = p; cargarNotificaciones(); });

        lista.querySelectorAll('.notif-item').forEach(el => {
            el.addEventListener('click', () => {
                const ids = el.dataset.ids.split(',');
                handleClick(ids, el.dataset.leida === 'true', el.dataset.url, '/notificaciones/gestor');
            });
        });
    } catch (e) {
        lista.innerHTML = '';
        mostrarError(typeof e === 'string' ? e : 'Error al cargar notificaciones');
    }
}

// Colapsa los NUEVO_MENSAJE con el mismo conv=X en un único ítem con badge de conteo
function colapsarPorConversacion(items) {
    const convMap = new Map();
    const resultado = [];

    items.forEach(n => {
        const convId = extraerConvId(n.urlReferencia);
        if (n.tipo === 'NUEVO_MENSAJE' && convId) {
            if (convMap.has(convId)) {
                const grupo = convMap.get(convId);
                grupo.ids.push(n.id);
                if (!n.leida) grupo.unreadCount++;
            } else {
                const grupo = {
                    _grupo: true,
                    ids: [n.id],
                    tipo: n.tipo,
                    titulo: n.titulo,
                    mensaje: n.mensaje,
                    urlReferencia: n.urlReferencia,
                    fechaCreacion: n.fechaCreacion,
                    unreadCount: n.leida ? 0 : 1
                };
                convMap.set(convId, grupo);
                resultado.push(grupo);
            }
        } else {
            resultado.push({ _grupo: false, ids: [n.id], unreadCount: n.leida ? 0 : 1, ...n });
        }
    });

    resultado.forEach(item => { item.leida = item.unreadCount === 0; });
    return resultado;
}

function extraerConvId(url) {
    if (!url) return null;
    const m = url.match(/[?&]conv=(\d+)/);
    return m ? m[1] : null;
}

function renderGrupos(items) {
    const grupos = {};
    const orden = [];

    items.forEach(n => {
        const cat = categoriaFecha(n.fechaCreacion);
        if (!grupos[cat]) { grupos[cat] = []; orden.push(cat); }
        grupos[cat].push(n);
    });

    return orden.map(cat => `
        <div class="notif-grupo">
          <div class="notif-grupo-header">${cat}</div>
          ${grupos[cat].map(renderItem).join('')}
        </div>`).join('');
}

function renderItem(item) {
    const unread = !item.leida;
    const hasUrl = !!item.urlReferencia;
    const idsAttr = item.ids.join(',');
    const showBadge = item._grupo && item.unreadCount > 1;

    return `
        <div class="notif-item ${unread ? 'notif-unread' : ''} ${hasUrl ? 'notif-clickable' : ''}"
             data-ids="${idsAttr}" data-leida="${item.leida}" data-url="${escapeAttr(item.urlReferencia || '')}">
          ${unread ? '<span class="notif-dot"></span>' : '<span class="notif-dot-placeholder"></span>'}
          <span class="notif-icon-wrap notif-icon-${iconClass(item.tipo)}">
            <i class="bi ${iconoTipo(item.tipo)} fs-6"></i>
          </span>
          <div class="notif-body">
            <div class="notif-title ${unread ? 'fw-semibold' : ''}">${escapeHtml(item.titulo)}</div>
            <div class="notif-msg">${escapeHtml(item.mensaje)}</div>
          </div>
          <div class="d-flex flex-column align-items-end gap-1 flex-shrink-0">
            <span class="notif-time">${formatFechaRelativa(item.fechaCreacion)}</span>
            ${showBadge ? `<span class="badge rounded-pill bg-primary notif-badge">${item.unreadCount}</span>` : ''}
          </div>
        </div>`;
}

async function handleClick(ids, leida, url, baseEndpoint) {
    const convId = extraerConvId(url);
    if (convId) sessionStorage.setItem('ef:openConvId', convId);

    if (!leida) {
        try {
            await Promise.all(ids.map(id =>
                fetch(`${baseEndpoint}/${id}/leida`, { method: 'PUT', credentials: 'include' })
            ));
            const el = document.querySelector(`.notif-item[data-ids="${ids.join(',')}"]`);
            if (el) {
                el.classList.remove('notif-unread');
                el.dataset.leida = 'true';
                const dot = el.querySelector('.notif-dot');
                if (dot) dot.className = 'notif-dot-placeholder';
                el.querySelector('.notif-title')?.classList.remove('fw-semibold');
                el.querySelector('.notif-badge')?.remove();
            }
        } catch { /* silencioso */ }
    }
    if (url) window.location.href = convId ? url.split('?')[0] : url;
}

function iconClass(tipo) {
    return { NUEVO_MENSAJE: 'primary' }[tipo] ?? 'primary';
}

function iconoTipo(tipo) {
    return { NUEVO_MENSAJE: 'bi-chat-dots-fill' }[tipo] ?? 'bi-bell-fill';
}

function categoriaFecha(iso) {
    if (!iso) return 'Anteriores';
    const d = new Date(iso);
    const ahora = new Date();
    const hoy = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate());
    const ayer = new Date(hoy - 86400000);
    const dDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());

    if (dDate.getTime() === hoy.getTime()) return 'Hoy';
    if (dDate.getTime() === ayer.getTime()) return 'Ayer';
    if (ahora - d < 7 * 86400000) return 'Esta semana';
    return d.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
        .replace(/^\w/, c => c.toUpperCase());
}

function formatFechaRelativa(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const diffMin = Math.floor((Date.now() - d.getTime()) / 60000);
    if (diffMin < 1) return 'Ahora';
    if (diffMin < 60) return `hace ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `hace ${diffH} h`;
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function escapeAttr(str) {
    if (!str) return '';
    return str.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function mostrarError(msg) {
    const el = document.getElementById('recuadroAlert');
    el.textContent = msg;
    el.classList.remove('d-none');
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
