let miUsuarioId = null;
let conversacionesList = [];

document.addEventListener('DOMContentLoaded', () => {
    fetch('/perfil', { credentials: 'include' })
        .then(r => r.ok ? r.json() : null)
        .then(p => {
            if (p) {
                miUsuarioId = p.id;
                WsClient.init(p.id);
            }
            cargarConversaciones();
        });

    document.getElementById('formEnviar').addEventListener('submit', async e => {
        e.preventDefault();
        await enviarMensaje();
    });

    window.addEventListener('ef:nuevo_mensaje', async e => {
        if (conversacionActiva && String(conversacionActiva) === String(e.detail.conversacionId)) {
            // Marcar notificaciones como leídas antes de recargar la lista
            fetch(`/notificaciones/por-conversacion/${conversacionActiva}/leidas`, { method: 'PUT', credentials: 'include' }).catch(() => {});
            await cargarMensajes(conversacionActiva); // marca mensajes leídos en el backend
            cargarConversaciones();                   // ahora la lista muestra 0 no leídos
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

    // Espera a que navbar y footer estén pintados antes de calcular altura
    requestAnimationFrame(() => requestAnimationFrame(ajustarAlturaCard));
    window.addEventListener('resize', ajustarAlturaCard);
});

function ajustarAlturaCard() {
    const card = document.getElementById('chatCard');
    if (!card) return;
    const top = card.getBoundingClientRect().top;
    const gap = 48;
    const altura = window.innerHeight - top - gap;
    card.style.height = Math.max(400, altura) + 'px';
}

function resolverConvInicial() {
    const fromSS = sessionStorage.getItem('ef:openConvId');
    if (fromSS) { sessionStorage.removeItem('ef:openConvId'); return fromSS; }
    return new URLSearchParams(window.location.search).get('conv');
}

let conversacionActiva = null;

async function cargarConversaciones() {
    const lista = document.getElementById('listaConversaciones');
    try {
        const r = await fetch('/chat', { credentials: 'include' });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        const convs = await r.json();

        if (!convs.length) {
            lista.innerHTML = `
                <div class="d-flex flex-column align-items-center justify-content-center h-100 text-center text-muted gap-3 px-4">
                    <i class="bi bi-chat-dots fs-1 opacity-25"></i>
                    <div>
                        <p class="fw-semibold mb-1 small text-dark">No tienes conversaciones aún.</p>
                        <p class="small mb-0" style="line-height:1.5">Cuando encuentres una formación que te interese, podrás contactar con el centro si este está registrado en la plataforma.</p>
                    </div>
                </div>`;
            lista.classList.replace('col-md-4', 'col-12');
            lista.classList.remove('border-end');
            document.getElementById('panelMensajes').classList.add('d-none');
            return;
        }

        conversacionesList = convs;

        lista.innerHTML = convs.map(c => {
            const chipsFormaciones = c.formaciones?.length
                ? c.formaciones.map(f =>
                    `<span class="badge rounded-pill bg-secondary bg-opacity-10 text-secondary fw-normal" style="font-size:.68em;" title="${escapeHtml(f.nombre)}"><i class="bi bi-mortarboard me-1" style="font-size:.6rem"></i>${escapeHtml(f.nombre)}</span>`
                  ).join(' ')
                : '';
            const isActive = String(c.id) === String(conversacionActiva) ? 'chat-item-active' : '';
            return `
            <div class="conv-item p-3 border-bottom ${isActive} ${c.noLeidos > 0 ? 'fw-semibold' : ''}"
                 data-id="${c.id}" role="button">
              <div class="d-flex justify-content-between align-items-baseline mb-1">
                <div class="d-flex align-items-center gap-2 min-w-0 flex-wrap">
                  <span class="text-truncate">${escapeHtml(c.centroNombre)}</span>
                  ${chipsFormaciones}
                </div>
                <small class="text-muted flex-shrink-0 ms-2" style="font-size:.7rem">${formatHora(c.ultimaActividad)}</small>
              </div>
              <div class="d-flex justify-content-between align-items-center gap-2">
                <small class="text-muted text-truncate" style="font-weight:normal;font-size:.8rem">
                  ${c.ultimoMensaje
                    ? (c.ultimoMensajeEsMio
                        ? `<span class="text-secondary">Tú:</span> `
                        : `<i class="bi bi-reply-fill opacity-50 me-1" style="font-size:.65rem;transform:scaleX(-1);display:inline-block"></i>`)
                      + escapeHtml(c.ultimoMensaje)
                    : 'Sin mensajes'}
                </small>
                ${c.noLeidos > 0 ? `<span class="badge bg-primary rounded-pill flex-shrink-0">${c.noLeidos}</span>` : ''}
              </div>
            </div>`;
        }).join('');

        lista.querySelectorAll('.conv-item').forEach(item => {
            const conv = convs.find(c => String(c.id) === item.dataset.id);
            item.addEventListener('click', () => seleccionarConversacion(item.dataset.id, conv));
        });

        const convInicial = resolverConvInicial();
        if (convInicial && !conversacionActiva) {
            const convObj = convs.find(c => String(c.id) === convInicial);
            if (convObj) seleccionarConversacion(convInicial, convObj);
        }
    } catch (e) {
        lista.innerHTML = '';
        mostrarError(typeof e === 'string' ? e : 'Error al cargar conversaciones');
    }
}

function seleccionarConversacion(id, conv) {
    conversacionActiva = id;
    document.querySelectorAll('.conv-item').forEach(i => {
        i.classList.toggle('chat-item-active', i.dataset.id == id);
    });
    document.getElementById('placeholderChat').classList.add('d-none');
    document.getElementById('cabeceraConversacion').classList.remove('d-none');
    document.getElementById('contenedorMensajes').classList.remove('d-none');
    document.getElementById('formularioMensaje').classList.remove('d-none');

    if (conv?.centroUuid) {
        const link = document.getElementById('linkCentro');
        link.textContent = conv.centroNombre ?? '';
        link.href = `/vistas/publico/perfil-centro.html?uuid=${conv.centroUuid}`;
    }

    cargarMensajes(id);
}

async function cargarMensajes(conversacionId) {
    const contenedor = document.getElementById('contenedorMensajes');
    try {
        const [dtoData, eventos] = await Promise.all([
            fetch(`/chat/${conversacionId}/mensajes`, { credentials: 'include' })
                .then(r => { if (!r.ok) throw new Error(`Error ${r.status}`); return r.json(); }),
            fetch(`/chat/${conversacionId}/eventos-solicitud`, { credentials: 'include' })
                .then(r => r.ok ? r.json() : [])
        ]);

        const mensajes = dtoData.mensajes;
        const formaciones = dtoData.formaciones;

        renderCabeceraMensajes({id: conversacionId, formaciones: formaciones});

        const items = [
            ...mensajes.map(m => ({ tipo: 'mensaje', fecha: m.fechaEnvio, data: m })),
            ...eventos.map(e => ({ tipo: 'evento', fecha: e.fecha, data: e }))
        ].sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

        if (!items.length) {
            contenedor.innerHTML = '<div class="text-center text-muted py-5 small">Aún no hay mensajes. ¡Empieza la conversación!</div>';
            return;
        }

        contenedor.innerHTML = items.map(item => {
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
        requestAnimationFrame(() => { contenedor.scrollTop = contenedor.scrollHeight; });

        const convIdx = conversacionesList.findIndex(c => String(c.id) === String(conversacionId));
        if (convIdx >= 0 && conversacionesList[convIdx].noLeidos > 0) {
            cargarConversaciones();
        }
    } catch (e) {
        contenedor.innerHTML = '<div class="alert alert-danger m-3">Error al cargar los mensajes.</div>';
    }
}

async function enviarMensaje() {
    if (!conversacionActiva) return;
    const input = document.getElementById('inputMensaje');
    const texto = input.value.trim();
    if (!texto) return;
    const btnEnviar = document.querySelector('#formEnviar button[type="submit"]');
    btnEnviar.disabled = true;
    try {
        const r = await fetch(`/chat/${conversacionActiva}/mensajes`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contenido: texto })
        });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        input.value = '';
        await cargarMensajes(conversacionActiva);
    } catch (e) {
        mostrarError(typeof e === 'string' ? e : 'Error al enviar mensaje');
    } finally {
        btnEnviar.disabled = false;
    }
}

function formatHora(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const hoy = new Date();
    if (d.toDateString() === hoy.toDateString()) {
        return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function mostrarError(msg) {
    const el = document.getElementById('recuadroAlert');
    el.textContent = msg;
    el.classList.remove('d-none');
}

function htmlEventoSolicitud(evento) {
    const cfg = configEvento(evento.tipoEvento);
    return `
        <div class="d-flex align-items-center gap-2 my-3 px-2 text-muted" style="font-size:.78rem;">
            <hr class="flex-grow-1 m-0 opacity-25">
            <a href="/vistas/estudiante/mis-solicitudes.html"
               class="d-flex align-items-center gap-1 px-2 py-1 rounded-pill border text-decoration-none text-muted ef-evento-solicitud"
               style="background:var(--bs-body-bg);white-space:nowrap;"
               data-solicitud-id="${evento.solicitudId}"
               data-solicitud-destino="mis_solicitudes_destacar">
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

function renderCabeceraMensajes(conv) {
    const contenedor = document.getElementById('cabeceraFormaciones');
    if (!conv.formaciones || !conv.formaciones.length) {
        contenedor.innerHTML = '';
        return;
    }

    const chips = conv.formaciones.map(f => `
        <span class="badge rounded-pill border bg-light text-dark fw-normal d-inline-flex align-items-center gap-1"
              data-form-uuid="${f.uuid}">
            <a href="/vistas/publico/detalle-formacion.html?uuid=${f.uuid}"
               target="_blank" rel="noopener noreferrer"
               class="text-dark text-decoration-none ef-chip-link"
               title="Ver formación: ${escapeHtml(f.nombre)}">
                <i class="bi bi-mortarboard" style="font-size:.7rem"></i> ${escapeHtml(f.nombre)}
            </a>
            <button class="btn p-0 lh-1 ef-chip-remove" style="font-size:.65rem;opacity:.5;"
                    aria-label="Quitar formación"
                    data-conv-id="${conv.id}"
                    data-form-uuid="${f.uuid}"
                    data-form-nombre="${escapeHtml(f.nombre)}">
                <i class="bi bi-x-lg"></i>
            </button>
        </span>
    `).join('');

    contenedor.innerHTML = chips;

    contenedor.querySelectorAll('.ef-chip-remove').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const convId = btn.dataset.convId;
            const formUuid = btn.dataset.formUuid;
            const formNombre = btn.dataset.formNombre;

            if (conv.formaciones.length <= 1) {
                efAlert({
                    title: 'Formación requerida',
                    message: 'La conversación debe tener al menos una formación vinculada.',
                    variant: 'warning',
                });
                return;
            }

            const ok = await efConfirm({
                title: 'Quitar formación',
                message: `¿Quieres quitar <strong>${formNombre}</strong> del contexto de esta conversación?`,
                confirmText: 'Quitar',
                variant: 'warning',
            });
            if (!ok) return;

            try {
                const r = await fetch(`/chat/${convId}/formaciones/${formUuid}`, {
                    method: 'DELETE',
                    credentials: 'include'
                });
                if (!r.ok) {
                    if (r.status === 409) {
                        mostrarError('La conversación debe tener al menos una formación vinculada.');
                    } else {
                        throw `Error ${r.status}`;
                    }
                    return;
                }
                cargarMensajes(convId);
            } catch (err) {
                mostrarError(typeof err === 'string' ? err : 'Error al desvincular formación');
            }
        });
    });
}
