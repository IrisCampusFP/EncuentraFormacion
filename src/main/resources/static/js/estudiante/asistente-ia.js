async function _estaAutenticado() {
    if (sessionStorage.getItem('ef_perfil')) return true;
    try {
        const res = await fetch('/perfil', { credentials: 'include' });
        return res.ok;
    } catch { return false; }
}

document.addEventListener('DOMContentLoaded', async () => {
    const listaSesiones       = document.getElementById('listaSesiones');
    const contenedorMensajes  = document.getElementById('contenedorMensajes');
    const formEnviar          = document.getElementById('formEnviar');
    const inputMensaje        = document.getElementById('inputMensaje');
    const btnNuevaSesion      = document.getElementById('btnNuevaSesion');
    const typingIndicator     = document.getElementById('typingIndicator');
    const tituloChatActual    = document.getElementById('tituloChatActual');
    const estadoChat          = document.getElementById('estadoChat');
    const placeholderMensajes = document.getElementById('placeholderMensajes');
    const recuadroAlert       = document.getElementById('recuadroAlert');
    const placeholderSesiones = document.getElementById('placeholderSesiones');
    const btnEliminarTodas    = document.getElementById('btnEliminarTodas');

    const autenticado = await _estaAutenticado();

    // --- Modo preview (no autenticado) ---
    if (!autenticado) {
        btnNuevaSesion.classList.add('d-none');

        const banner = document.createElement('div');
        banner.className = 'alert alert-info d-flex align-items-center gap-2 mb-3';
        banner.innerHTML = `
            <i class="bi bi-stars fs-5 flex-shrink-0"></i>
            <span>
                <strong>Inicia sesión como estudiante</strong> para usar el Orientador IA Personalizado.
                <a id="bannerLoginLink" href="#" class="alert-link ms-1">Iniciar sesión</a>
                &nbsp;·&nbsp;
                <a href="/vistas/auth/elegir-tipo-cuenta.html" class="alert-link">Registrarse</a>
            </span>`;
        document.querySelector('.ef-page-header').insertAdjacentElement('afterend', banner);
        document.getElementById('bannerLoginLink').href = `/vistas/auth/login.html?requiereLogin&returnTo=${encodeURIComponent(window.location.href)}`;

        const irAlLogin = () => {
            window.location.href = `/vistas/auth/login.html?requiereLogin&returnTo=${encodeURIComponent(window.location.href)}`;
        };

        formEnviar.addEventListener('submit', e => { e.preventDefault(); irAlLogin(); });

        inputMensaje.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey && !e.altKey) {
                e.preventDefault();
                formEnviar.requestSubmit();
            }
        });

        document.addEventListener('click', e => {
            if (e.target.closest('[data-suggestion]')) irAlLogin();
        });
        return;
    }

    // --- Modo normal (autenticado) ---

    let sesionActualId  = null;
    let cargandoMensaje = false;

    // --- Inicialización ---
    cargarSesiones();

    // --- Eventos ---
    btnNuevaSesion.addEventListener('click', () => crearNuevaSesion());

    btnEliminarTodas.addEventListener('click', async () => {
        const ok = await efConfirm({
            title: 'Eliminar todas las conversaciones',
            message: '¿Seguro que quieres eliminar todo el historial? Esta acción no se puede deshacer.',
            confirmText: 'Eliminar todo',
            variant: 'danger'
        });
        if (!ok) return;

        try {
            const res = await fetch('/api/asistente/sesiones', { method: 'DELETE' });
            if (!res.ok) throw new Error();

            sesionActualId = null;
            contenedorMensajes.innerHTML = '';
            placeholderMensajes.classList.remove('d-none');
            tituloChatActual.textContent = 'Orientador Personalizado';
            estadoChat.textContent = 'Listo para ayudarte';
            cargarSesiones({ silencioso: true });
        } catch {
            mostrarError('No se pudo eliminar el historial.');
        }
    });

    formEnviar.addEventListener('submit', async (e) => {
        e.preventDefault();
        const texto = inputMensaje.value.trim();
        if (!texto || cargandoMensaje) return;

        if (!sesionActualId) {
            await crearNuevaSesion(false);
        }

        enviarMensaje(texto);
    });

    // Enter envía · Shift+Enter o Alt+Enter baja de línea
    inputMensaje.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey && !e.altKey) {
            e.preventDefault();
            formEnviar.requestSubmit();
        }
    });

    // Contador de caracteres
    inputMensaje.addEventListener('input', function() {
        document.getElementById('charCounter').textContent = `${this.value.length}/2000`;
    });

    // Sugerencias de bienvenida
    document.addEventListener('click', e => {
        const suggestionBtn = e.target.closest('[data-suggestion]');
        if (suggestionBtn) {
            inputMensaje.value = suggestionBtn.dataset.suggestion;
            inputMensaje.dispatchEvent(new Event('input'));
            formEnviar.requestSubmit();
        }
    });

    // --- Funciones de API ---

    async function cargarSesiones({ silencioso = false } = {}) {
        try {
            const res = await fetch('/api/asistente/sesiones');
            if (!res.ok) throw new Error('GET sesiones status: ' + res.status);
            const sesiones = await res.json();
            renderizarListaSesiones(sesiones);
        } catch (err) {
            console.error('[cargarSesiones] error:', err);
            if (!silencioso) mostrarError('No se pudo conectar con el asistente. Inténtalo más tarde.');
            throw err;
        }
    }

    async function crearNuevaSesion(seleccionar = true) {
        try {
            const res = await fetch('/api/asistente/sesiones', { method: 'POST' });
            if (!res.ok) throw new Error('POST sesiones status: ' + res.status);
            const nuevaSesion = await res.json();

            await cargarSesiones({ silencioso: true });
            if (seleccionar) {
                seleccionarSesion(nuevaSesion.id, nuevaSesion.titulo);
            } else {
                sesionActualId = nuevaSesion.id;
                tituloChatActual.textContent = nuevaSesion.titulo;
            }
        } catch (err) {
            console.error('[crearNuevaSesion] error:', err);
            mostrarError('Error al iniciar una nueva consulta.');
        }
    }

    async function seleccionarSesion(id, titulo) {
        if (sesionActualId === id) return;

        sesionActualId = id;
        tituloChatActual.textContent = titulo;
        estadoChat.textContent = 'Cargando historial...';

        document.querySelectorAll('.sesion-item').forEach(el => {
            el.classList.toggle('active', el.dataset.id == id);
        });

        contenedorMensajes.innerHTML = '';
        placeholderMensajes.classList.add('d-none');

        try {
            const res = await fetch(`/api/asistente/sesiones/${id}/historial`);
            if (!res.ok) throw new Error();
            const data = await res.json();

            tituloChatActual.textContent = data.titulo;
            estadoChat.textContent = '';
            renderizarMensajes(data.mensajes);
            scrollAlFinal();
        } catch {
            mostrarError('Error al cargar la conversación.');
        }
    }

    async function enviarMensaje(texto) {
        renderizarMensaje({ rol: 'USER', contenido: texto, fechaEnvio: new Date().toISOString() });
        inputMensaje.value = '';
        placeholderMensajes.classList.add('d-none');

        cargandoMensaje = true;
        typingIndicator.classList.remove('d-none');
        scrollAlFinal();

        try {
            const res = await fetch(`/api/asistente/sesiones/${sesionActualId}/mensajes`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ contenido: texto })
            });

            typingIndicator.classList.add('d-none');

            if (!res.ok) {
                estadoChat.textContent = '';
                const status = res.status;
                if (status === 429) {
                    mostrarError('El asistente está recibiendo demasiadas solicitudes. Inténtalo de nuevo en unos minutos.');
                } else if (status === 504) {
                    mostrarError('El asistente ha tardado demasiado en responder. Inténtalo de nuevo.');
                } else {
                    mostrarError('El asistente no está disponible en este momento. Inténtalo más tarde.');
                }
                return;
            }

            estadoChat.textContent = '';
            const data = await res.json();
            renderizarMensaje({ rol: 'ASSISTANT', contenido: data.contenido, fechaEnvio: new Date().toISOString() }, data.formacionUuids);

        } catch {
            typingIndicator.classList.add('d-none');
            estadoChat.textContent = '';
            mostrarError('El asistente ha tenido un problema. Prueba de nuevo en unos segundos.');
        } finally {
            cargandoMensaje = false;
        }
    }

    async function eliminarSesion(id) {
        const ok = await efConfirm({
            title: 'Eliminar consulta',
            message: '¿Seguro que quieres eliminar esta consulta y todo su historial? Esta acción no se puede deshacer.',
            confirmText: 'Eliminar',
            variant: 'danger'
        });
        if (!ok) return;

        try {
            const res = await fetch(`/api/asistente/sesiones/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error();

            if (sesionActualId === id) {
                sesionActualId = null;
                contenedorMensajes.innerHTML = '';
                placeholderMensajes.classList.remove('d-none');
                tituloChatActual.textContent = 'Orientador Personalizado';
                estadoChat.textContent = 'Listo para ayudarte';
            }
            cargarSesiones({ silencioso: true });
        } catch {
            mostrarError('No se pudo eliminar la consulta.');
        }
    }

    function renombrarSesion(id, tituloActual) {
        const modalEl = document.getElementById('modalRenombrar');
        const modal = new bootstrap.Modal(modalEl);
        const input = document.getElementById('inputNuevoTitulo');
        const btnConfirmar = document.getElementById('btnConfirmarRenombrar');

        input.value = tituloActual;
        modal.show();

        modalEl.addEventListener('shown.bs.modal', () => {
            input.focus();
            input.select();
        }, { once: true });

        const ejecutarRenombrado = async () => {
            const nuevoTitulo = input.value.trim();
            if (!nuevoTitulo || nuevoTitulo === tituloActual) { modal.hide(); return; }
            modal.hide();
            try {
                const res = await fetch(`/api/asistente/sesiones/${id}`, {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ titulo: nuevoTitulo })
                });
                if (!res.ok) throw new Error();
                if (sesionActualId === id) tituloChatActual.textContent = nuevoTitulo;
                cargarSesiones({ silencioso: true });
            } catch {
                mostrarError('No se pudo renombrar la consulta.');
            }
        };

        const onKeydown = (e) => { if (e.key === 'Enter') { e.preventDefault(); ejecutarRenombrado(); } };
        input.addEventListener('keydown', onKeydown);
        btnConfirmar.addEventListener('click', ejecutarRenombrado, { once: true });
        modalEl.addEventListener('hidden.bs.modal', () => {
            input.removeEventListener('keydown', onKeydown);
        }, { once: true });
    }

    // --- Renderizado ---

    function renderizarListaSesiones(sesiones) {
        if (sesiones.length === 0) {
            listaSesiones.innerHTML = '';
            placeholderSesiones.classList.remove('d-none');
            listaSesiones.appendChild(placeholderSesiones);
            btnEliminarTodas.classList.add('d-none');
            return;
        }

        listaSesiones.innerHTML = '';
        placeholderSesiones.classList.add('d-none');
        btnEliminarTodas.classList.remove('d-none');

        sesiones.forEach(s => {
            const tpl = document.getElementById('tplSesionItem').content.cloneNode(true);
            const item = tpl.querySelector('.sesion-item');
            item.dataset.id = s.id;
            item.querySelector('.sesion-titulo').textContent = s.titulo;
            item.querySelector('.sesion-fecha').textContent = formatearFechaRelativa(s.ultimaActividad);

            if (s.id === sesionActualId) item.classList.add('active');

            item.querySelector('.sesion-selector').addEventListener('click', () => seleccionarSesion(s.id, s.titulo));

            item.querySelector('.btn-renombrar-sesion').addEventListener('click', (e) => {
                e.stopPropagation();
                renombrarSesion(s.id, s.titulo);
            });

            item.querySelector('.btn-eliminar-sesion').addEventListener('click', (e) => {
                e.stopPropagation();
                eliminarSesion(s.id);
            });

            listaSesiones.appendChild(item);
        });
    }

    function renderizarMensajes(mensajes) {
        mensajes.forEach(m => {
            let uuids = [];
            if (m.rol === 'ASSISTANT') {
                const regex = /\[FORMACION:([\w-]+)\]/g;
                let match;
                while ((match = regex.exec(m.contenido)) !== null) {
                    uuids.push(match[1]);
                }
            }
            renderizarMensaje(m, uuids);
        });
    }

    function renderizarMensaje(mensaje, formacionUuids = []) {
        const esUsuario = mensaje.rol === 'USER';
        const tplId = esUsuario ? 'tplMensajeUsuario' : 'tplMensajeAsistente';
        const tpl = document.getElementById(tplId).content.cloneNode(true);

        const bubble = tpl.querySelector(esUsuario ? '.message-bubble p' : '.message-text');
        bubble.innerHTML = esUsuario ? mensaje.contenido : parsearMarkdown(mensaje.contenido);

        tpl.querySelector('.message-time').textContent = formatearFechaHora(mensaje.fechaEnvio);

        if (!esUsuario && formacionUuids && formacionUuids.length > 0) {
            const container = tpl.querySelector('.related-formations');
            renderizarCardsFormacion(container, formacionUuids);
        }

        contenedorMensajes.appendChild(tpl);
        scrollAlFinal();
    }

    async function renderizarCardsFormacion(container, uuids) {
        for (const uuid of uuids) {
            try {
                const res = await fetch(`/formaciones/${uuid}`);
                if (!res.ok) continue;
                const formacion = await res.json();

                const tpl = document.getElementById('tplCardFormacion').content.cloneNode(true);
                tpl.querySelector('.nombre-formacion').textContent = formacion.nombre;
                tpl.querySelector('.centro-formacion').textContent = formacion.centroNombre;
                tpl.querySelector('.btn-detalle').href = `/vistas/publico/detalle-formacion.html?uuid=${formacion.uuid}`;

                container.appendChild(tpl);
            } catch { /* ignorar */ }
        }
    }

    // --- Helpers ---

    function parsearMarkdown(text) {
        if (!text) return '';
        return text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\n/g, '<br>')
            .replace(/\[FORMACION:(.*?)\]/g, '');
    }

    function formatearFechaRelativa(isoStr) {
        if (!isoStr) return '';
        const d = new Date(isoStr);
        const ahora = new Date();
        const diffMs = ahora - d;
        const diffH = diffMs / 3600000;

        if (diffH < 24 && d.toDateString() === ahora.toDateString()) {
            return 'Hoy · ' + d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
        }
        // ayer
        const ayer = new Date(ahora);
        ayer.setDate(ahora.getDate() - 1);
        if (d.toDateString() === ayer.toDateString()) return 'Ayer';

        return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
    }

    function formatearFechaHora(isoStr) {
        if (!isoStr) return '';
        return new Date(isoStr).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    }

    function scrollAlFinal() {
        setTimeout(() => {
            contenedorMensajes.scrollTo({ top: contenedorMensajes.scrollHeight, behavior: 'smooth' });
        }, 50);
    }

    function mostrarError(msg) {
        recuadroAlert.textContent = msg;
        recuadroAlert.classList.remove('d-none');
        setTimeout(() => recuadroAlert.classList.add('d-none'), 5000);
    }
});
