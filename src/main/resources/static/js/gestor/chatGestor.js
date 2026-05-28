let miUsuarioId = null;
let conversacionesList = [];
let conversacionActiva = null;

document.addEventListener('DOMContentLoaded', () => {
    fetch('/perfil', { credentials: 'include' })
        .then(r => r.ok ? r.json() : null)
        .then(p => {
            if (p) {
                miUsuarioId = p.id;
                WsClient.init(p.id);
            }
            cargarFormacionesParaFiltro().then(() => cargarConversaciones());
        });

    window.addEventListener('ef:nuevo_mensaje', async e => {
        if (conversacionActiva && String(conversacionActiva) === String(e.detail.conversacionId)) {
            fetch(`/notificaciones/gestor/por-conversacion/${conversacionActiva}/leidas`, { method: 'PUT', credentials: 'include' }).catch(() => {});
            await cargarMensajes(conversacionActiva);
            cargarConversaciones();
        } else {
            cargarConversaciones();
        }
    });

    document.getElementById('contenedorMensajes').addEventListener('click', e => {
        const link = e.target.closest('[data-solicitud-destino]');
        if (!link) return;
        sessionStorage.setItem(link.dataset.solicitudDestino, link.dataset.solicitudId);
    });

    window.addEventListener('ef:solicitud_evento', e => {
        if (String(conversacionActiva) === String(e.detail.conversacionId)) {
            insertarEventoSolicitud(e.detail.evento);
            const cont = document.getElementById('contenedorMensajes');
            cont.scrollTop = cont.scrollHeight;
        }
    });

    window.addEventListener('resize', () => requestAnimationFrame(ajustarAlturaCard));
    ajustarAlturaCard();

    document.getElementById('formEnviar').addEventListener('submit', enviarMensaje);

    document.getElementById('filtroFormacion').addEventListener('change', () => {
        cargarConversaciones();
    });
});

async function cargarFormacionesParaFiltro() {
    try {
        const res = await fetch('/gestor/formaciones?size=100');
        if (res.ok) {
            const data = await res.json();
            const sel = document.getElementById('filtroFormacion');
            data.content.forEach(f => {
                const opt = document.createElement('option');
                opt.value = f.id;
                opt.textContent = f.nombre;
                sel.appendChild(opt);
            });
            const paramId = sessionStorage.getItem('chat_filtro_formacion');
            if (paramId) {
                sel.value = paramId;
                sessionStorage.removeItem('chat_filtro_formacion');
            }
        }
    } catch (e) { console.error('Error cargando formaciones', e); }
}

async function cargarConversaciones() {
    const formacionId = document.getElementById('filtroFormacion').value;
    let url = '/gestor/chat';
    if (formacionId) url += `?formacionId=${formacionId}`;

    const lista = document.getElementById('listaConversaciones');
    
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error('Error al cargar conversaciones');
        conversacionesList = await res.json();

        if (conversacionesList.length === 0) {
            lista.innerHTML = '<div class="d-flex flex-column align-items-center justify-content-center h-100 text-muted"><i class="bi bi-chat-left-dots fs-3 mb-2"></i>No hay conversaciones</div>';
            document.getElementById('placeholderChat').classList.add('d-none');
            return;
        }
        if (!conversacionActiva) document.getElementById('placeholderChat').classList.remove('d-none');

        lista.innerHTML = conversacionesList.map(c => {
            const isActive = c.id === conversacionActiva ? 'chat-item-active' : '';
            const unread = c.mensajesNoLeidos > 0 ? `<span class="badge bg-danger rounded-pill">${c.mensajesNoLeidos}</span>` : '';
            const inicial = c.estudianteNombre ? c.estudianteNombre[0].toUpperCase() : 'U';

            let chipsFormaciones = '';
            if (c.formaciones && c.formaciones.length > 0) {
                chipsFormaciones = c.formaciones.map(f =>
                    `<span class="badge rounded-pill bg-secondary bg-opacity-10 text-secondary fw-normal" style="font-size: 0.68em;"><i class="bi bi-mortarboard me-1" style="font-size:.6rem"></i>${escapeHtml(f.nombre)}</span>`
                ).join(' ');
            }

            return `
                <div class="p-3 border-bottom chat-item ${isActive}"
                     data-conv-id="${c.id}">
                    <div class="d-flex justify-content-between align-items-baseline mb-1">
                        <div class="d-flex align-items-center gap-2 min-w-0 flex-wrap">
                            <span class="fw-semibold text-truncate">${escapeHtml(c.estudianteNombre)}</span>
                            ${chipsFormaciones}
                        </div>
                        <small class="text-muted text-nowrap ms-2 flex-shrink-0" style="font-size: 0.75rem;">${formatHora(c.ultimaFecha)}</small>
                    </div>
                    <div class="d-flex justify-content-between align-items-center gap-2">
                        <p class="mb-0 text-muted small text-truncate flex-grow-1">
                            ${c.ultimoMensaje
                                ? (c.ultimoMensajeEsMio
                                    ? '<span class="text-secondary">Tú:</span> '
                                    : '<i class="bi bi-reply-fill opacity-50 me-1" style="font-size:.65rem;transform:scaleX(-1);display:inline-block"></i>')
                                  + escapeHtml(c.ultimoMensaje)
                                : 'Sin mensajes'}
                        </p>
                        ${unread}
                    </div>
                </div>
            `;
        }).join('');

        lista.querySelectorAll('.chat-item').forEach(el => {
            el.addEventListener('click', () => {
                const id = parseInt(el.dataset.convId);
                const conv = conversacionesList.find(c => c.id === id);
                if (conv) seleccionarConversacion(id, conv);
            });
        });

        const fromSS = sessionStorage.getItem('ef:openConvId');
        if (fromSS) sessionStorage.removeItem('ef:openConvId');
        const convParam = fromSS || new URLSearchParams(window.location.search).get('conv');
        if (convParam && !conversacionActiva) {
            const idToSelect = parseInt(convParam);
            const conv = conversacionesList.find(c => c.id === idToSelect);
            if (conv) seleccionarConversacion(idToSelect, conv);
        }

    } catch (e) {
        lista.innerHTML = '<div class="alert alert-danger m-3">Error al cargar chats</div>';
    }
}

function seleccionarConversacion(id, conv) {
    conversacionActiva = id;

    document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('chat-item-active'));
    const itemActual = document.querySelector(`.chat-item[data-conv-id="${id}"]`);
    if (itemActual) itemActual.classList.add('chat-item-active');

    document.getElementById('placeholderChat').classList.add('d-none');
    document.getElementById('cabeceraConversacion').classList.remove('d-none');
    document.getElementById('contenedorMensajes').classList.remove('d-none');
    document.getElementById('formularioMensaje').classList.remove('d-none');

    document.getElementById('cabeceraEstudiante').textContent = conv.estudianteNombre;

    const contTags = document.getElementById('cabeceraFormaciones');
    if (conv.formaciones && conv.formaciones.length > 0) {
        contTags.innerHTML = conv.formaciones.map(f =>
            `<span class="badge rounded-pill bg-secondary bg-opacity-25 text-dark fw-normal">
                <i class="bi bi-mortarboard me-1" style="font-size:.7rem"></i><span class="text-truncate" style="max-width:200px;" title="${escapeHtml(f.nombre)}">${escapeHtml(f.nombre)}</span>
            </span>`
        ).join('');
    } else {
        contTags.innerHTML = '';
    }

    cargarMensajes(id);
}

async function cargarMensajes(convId) {
    const cont = document.getElementById('contenedorMensajes');
    try {
        const [mensajes, eventos] = await Promise.all([
            fetch(`/gestor/chat/${convId}/mensajes`).then(r => { if (!r.ok) throw new Error(); return r.json(); }),
            fetch(`/gestor/chat/${convId}/eventos-solicitud`).then(r => r.ok ? r.json() : [])
        ]);

        const items = [
            ...mensajes.map(m => ({ tipo: 'mensaje', fecha: m.fechaEnvio, data: m })),
            ...eventos.map(e => ({ tipo: 'evento', fecha: e.fecha, data: e }))
        ].sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

        if (!items.length) {
            cont.innerHTML = '<div class="text-center text-muted my-4">No hay mensajes aún</div>';
            return;
        }

        cont.innerHTML = items.map(item => {
            if (item.tipo === 'evento') return htmlEventoSolicitud(item.data);
            const m = item.data;
            const esMio = m.remitenteId === miUsuarioId;
            return `
                <div class="d-flex mb-2 ${esMio ? 'justify-content-end' : 'justify-content-start'}">
                  <div class="px-3 py-2 rounded-3 ${esMio ? 'bg-primary text-white' : 'bg-light'}" style="max-width:70%">
                    <div style="font-size:.9rem">${escapeHtml(m.contenido)}</div>
                    <small class="d-block text-end mt-1 ${esMio ? 'text-white-50' : 'text-muted'}" style="font-size:.7rem">${formatHora(m.fechaEnvio)}</small>
                  </div>
                </div>`;
        }).join('');

        requestAnimationFrame(() => { cont.scrollTop = cont.scrollHeight; });

        const convIdx = conversacionesList.findIndex(c => c.id === convId);
        if (convIdx >= 0 && conversacionesList[convIdx].mensajesNoLeidos > 0) {
            cargarConversaciones();
        }
    } catch (e) {
        cont.innerHTML = '<div class="alert alert-danger m-3">Error al cargar los mensajes.</div>';
    }
}

async function enviarMensaje(e) {
    e.preventDefault();
    if (!conversacionActiva) return;

    const input = document.getElementById('inputMensaje');
    const contenido = input.value.trim();
    if (!contenido) return;

    const btn = document.querySelector('#formEnviar button');
    btn.disabled = true;

    try {
        const res = await fetch(`/gestor/chat/${conversacionActiva}/mensajes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contenido })
        });
        if (!res.ok) throw new Error();
        input.value = '';
        await cargarMensajes(conversacionActiva);
        await cargarConversaciones();
    } catch (err) {
        await efAlert({ title: 'Error', message: 'No se pudo enviar el mensaje.', variant: 'danger' });
    } finally {
        btn.disabled = false;
        input.focus();
    }
}

function ajustarAlturaCard() {
    const card = document.getElementById('chatCard');
    if (!card) return;
    const top = card.getBoundingClientRect().top;
    const gap = 48;
    const altura = window.innerHeight - top - gap;
    card.style.height = Math.max(400, altura) + 'px';
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

function formatHora(isoStr) {
    if (!isoStr) return '';
    const d = new Date(isoStr);
    const hoy = new Date();
    if (d.toDateString() === hoy.toDateString()) {
        return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
}

function htmlEventoSolicitud(evento) {
    const cfg = configEvento(evento.tipoEvento);
    return `
        <div class="d-flex align-items-center gap-2 my-3 px-2 text-muted" style="font-size:.78rem;">
            <hr class="flex-grow-1 m-0 opacity-25">
            <a href="/vistas/gestor/solicitudes.html"
               class="d-flex align-items-center gap-1 px-2 py-1 rounded-pill border text-decoration-none text-muted ef-evento-solicitud"
               style="background:var(--bs-body-bg);white-space:nowrap;"
               data-solicitud-id="${evento.solicitudId}"
               data-solicitud-destino="gestor_solicitudes_destacar">
                <i class="bi ${cfg.icon} ${cfg.color}" style="font-size:.8rem"></i>
                <span>${cfg.texto(evento.formacionNombre)}</span>
                <span class="opacity-50 ms-1">${formatHora(evento.fecha)}</span>
                <i class="bi bi-arrow-right ms-1 opacity-50" style="font-size:.7rem"></i>
            </a>
            <hr class="flex-grow-1 m-0 opacity-25">
        </div>`;
}

function insertarEventoSolicitud(evento) {
    const cont = document.getElementById('contenedorMensajes');
    const div = document.createElement('div');
    div.innerHTML = htmlEventoSolicitud(evento);
    cont.appendChild(div.firstElementChild);
}

function configEvento(tipo) {
    const cfg = {
        SOLICITUD_ENVIADA:   { icon: 'bi-send',        color: 'text-primary',  texto: n => `Solicitud enviada · ${n}` },
        SOLICITUD_CANCELADA: { icon: 'bi-x-circle',    color: 'text-secondary',texto: n => `Solicitud cancelada · ${n}` },
        SOLICITUD_ACEPTADA:  { icon: 'bi-check-circle',color: 'text-success',  texto: n => `Solicitud aceptada · ${n}` },
        SOLICITUD_RECHAZADA: { icon: 'bi-dash-circle', color: 'text-danger',   texto: n => `Solicitud rechazada · ${n}` },
    };
    return cfg[tipo] ?? { icon: 'bi-info-circle', color: 'text-muted', texto: n => n };
}
