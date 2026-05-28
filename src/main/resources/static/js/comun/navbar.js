const _PERFIL_KEY   = 'ef_perfil';
const _CONTEXTO_KEY = 'ef_contexto';

// ─── Caché de perfil ──────────────────────────────────────────────────────────

async function _obtenerPerfil() {
    const cached = sessionStorage.getItem(_PERFIL_KEY);
    if (cached) return JSON.parse(cached);
    try {
        const res = await fetch('/perfil', { credentials: 'include' });
        if (!res.ok) return null;
        const perfil = await res.json();
        sessionStorage.setItem(_PERFIL_KEY, JSON.stringify(perfil));
        return perfil;
    } catch { return null; }
}

function _limpiarCachePerfil() {
    sessionStorage.removeItem(_PERFIL_KEY);
}

// ─── Fragmentos estáticos ─────────────────────────────────────────────────────

const _DIVIDER = '<li class="nav-item"><span class="ef-navbar-divider"></span></li>';

function _linksBusqueda({ contexto = null, visible = true } = {}) {
    const ctx  = contexto ? ` data-context-block="${contexto}"` : '';
    const hide = !visible ? ' d-none' : '';
    return `
        <li class="nav-item${hide}"${ctx}>
            <a class="nav-link" href="/" title="Buscar formaciones">
                <i class="bi bi-search me-1"></i><span class="d-none d-lg-inline">Formaciones</span>
            </a>
        </li>
        <li class="nav-item${hide}"${ctx}>
            <a class="nav-link" href="/vistas/publico/buscar-centros.html" title="Buscar centros">
                <i class="bi bi-building me-1"></i><span class="d-none d-lg-inline">Centros</span>
            </a>
        </li>`;
}

const _LINK_IA = `
    <li class="nav-item">
        <a class="nav-link" href="/vistas/estudiante/asistente-ia.html">
            <i class="bi bi-stars me-2 ef-nav-ia-icon"></i><span class="ef-nav-ia-text">Orientador IA</span>
        </a>
    </li>`;

// ─── Constructores de bloques ─────────────────────────────────────────────────

function _linksDeRol(rol, { contexto = null, visible = true } = {}) {
    const ctx  = contexto ? ` data-context-block="${contexto}"` : '';
    const hide = !visible ? ' d-none' : '';
    switch (rol) {
        case 'GESTOR_CENTRO':
            return `
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/gestor/vista-gestion-centro.html"><i class="bi bi-speedometer2 me-1"></i>Panel</a></li>
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/gestor/solicitudes.html"><i class="bi bi-inbox-fill me-1"></i>Solicitudes</a></li>
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/gestor/chat.html"><i class="bi bi-chat-dots-fill me-1"></i>Chats</a></li>`;
        case 'ESTUDIANTE':
            return `
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/estudiante/guardadas.html"><i class="bi bi-bookmark-fill me-1"></i>Formaciones guardadas</a></li>
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/estudiante/asistente-ia.html"><i class="bi bi-stars me-2 ef-nav-ia-icon"></i><span class="ef-nav-ia-text">Orientador IA</span></a></li>
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/estudiante/chat.html"><i class="bi bi-chat-dots-fill me-1"></i>Chats</a></li>
                <li class="nav-item${hide}"${ctx}><a class="nav-link" href="/vistas/estudiante/mis-solicitudes.html"><i class="bi bi-file-earmark-text-fill me-1"></i>Solicitudes de admisión</a></li>`;
        default:
            return '';
    }
}

function _linksExplorar({ contexto = null, visible = true } = {}) {
    const ctx  = contexto ? ` data-context-block="${contexto}"` : '';
    const hide = !visible ? ' d-none' : '';
    return `
        <li class="nav-item dropdown${hide}"${ctx}>
            <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-compass me-1"></i><span class="d-none d-lg-inline">Explorar</span>
            </a>
            <ul class="dropdown-menu shadow-sm border-0">
                <li><a class="dropdown-item" href="/"><i class="bi bi-search me-2"></i>Buscar formaciones</a></li>
                <li><a class="dropdown-item" href="/vistas/publico/buscar-centros.html"><i class="bi bi-building me-2"></i>Buscar centros</a></li>
            </ul>
        </li>`;
}

function _construirPanelToggle(contextoActivo) {
    const esGestor = contextoActivo === 'GESTOR_CENTRO';
    const icono = esGestor ? 'bi-mortarboard-fill' : 'bi-speedometer2';
    const label = esGestor ? 'Panel Estudiante' : 'Panel Gestor';
    return `
        <li class="nav-item">
            <button class="btn btn-sm ef-panel-toggle fw-medium px-3" data-panel-toggle>
                <i class="bi ${icono} me-1" data-panel-icon></i><span data-panel-label>${label}</span>
            </button>
        </li>`;
}

function _construirDropdown(nombre, { urlGestion = '/vistas/comun/solicitar-gestion-centro.html', labelGestion = 'Gestionar un centro', iconoGestion = 'bi-building-add' } = {}) {
    return `
        <li class="nav-item dropdown">
            <button class="btn btn-sm ef-user-btn dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-person-circle me-1"></i><span class="d-none d-sm-inline">${nombre}</span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0">
                <li>
                    <a class="dropdown-item" href="/vistas/comun/perfil.html">
                        <i class="bi bi-person-gear me-2"></i>Editar perfil
                    </a>
                </li>
                <li>
                    <a class="dropdown-item" href="${urlGestion}">
                        <i class="bi ${iconoGestion} me-2"></i>${labelGestion}
                    </a>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <button class="dropdown-item text-danger" data-action="cerrarSesion">
                        <i class="bi bi-box-arrow-right me-2"></i>Cerrar sesión
                    </button>
                </li>
            </ul>
        </li>`;
}

// ─── Composición de zonas ─────────────────────────────────────────────────────

function _construirIzquierda(tieneAdmin, tieneGestor, tieneEstudiante) {
    // GESTOR + ESTUDIANTE: solo links de rol por contexto; Explorar y panel toggle van a la derecha.
    if (tieneGestor && tieneEstudiante) {
        const ctx      = sessionStorage.getItem(_CONTEXTO_KEY) || 'GESTOR_CENTRO';
        const esGestor = ctx === 'GESTOR_CENTRO';

        return _linksDeRol('GESTOR_CENTRO', { contexto: 'GESTOR_CENTRO', visible:  esGestor }) +
               _linksBusqueda(            { contexto: 'ESTUDIANTE',    visible: !esGestor }) +
               _linksDeRol('ESTUDIANTE',  { contexto: 'ESTUDIANTE',    visible: !esGestor });
    }

    // ESTUDIANTE sin GESTOR (con o sin ADMIN): buscadores sueltos + links, sin Explorar ni separador.
    if (tieneEstudiante) {
        return _linksBusqueda() + _linksDeRol('ESTUDIANTE');
    }

    // GESTOR sin ESTUDIANTE (con o sin ADMIN): solo links de gestor; Explorar va a la derecha.
    if (tieneGestor) {
        return _linksDeRol('GESTOR_CENTRO');
    }

    // ADMIN solo: sin links izquierda; Explorar va a la derecha.
    if (tieneAdmin) {
        return '';
    }

    return '';
}

function _construirDerecha(perfil, tieneAdmin, tieneGestor, tieneEstudiante) {
    const nombre = perfil.nombre || 'Usuario';

    let explorarYPanel = '';
    if (tieneGestor && tieneEstudiante) {
        const ctx      = sessionStorage.getItem(_CONTEXTO_KEY) || 'GESTOR_CENTRO';
        const esGestor = ctx === 'GESTOR_CENTRO';
        explorarYPanel = _linksExplorar({ contexto: 'GESTOR_CENTRO', visible: esGestor }) + _construirPanelToggle(ctx);
    } else if (tieneGestor || (tieneAdmin && !tieneEstudiante)) {
        explorarYPanel = _linksExplorar();
    }

    const btnSolicitudGestion = perfil.tieneSolicitudGestionPendiente ? `
        <li class="nav-item">
            <a class="btn btn-sm btn-outline-warning" href="/vistas/comun/solicitar-gestion-centro.html">
                <i class="bi bi-hourglass-split me-1"></i>Solicitud de gestión pendiente
            </a>
        </li>` : '';

    const contextoNotif = (tieneGestor && tieneEstudiante)
        ? (sessionStorage.getItem(_CONTEXTO_KEY) || 'GESTOR_CENTRO')
        : (tieneGestor ? 'GESTOR_CENTRO' : 'ESTUDIANTE');
    const urlNotif = contextoNotif === 'GESTOR_CENTRO'
        ? '/vistas/gestor/notificaciones.html'
        : '/vistas/estudiante/notificaciones.html';
    const btnNotificaciones = (tieneEstudiante || tieneGestor) ? `
        <li class="nav-item">
            <a class="nav-link ef-btn-icon position-relative" href="${urlNotif}" title="Notificaciones">
                <i class="bi bi-bell-fill"></i>
                <span id="badgeNotif" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger d-none ef-fs-xxs">0</span>
            </a>
        </li>` : '';

    const btnAdmin = tieneAdmin ? `
        <li class="nav-item">
            <a class="btn btn-sm btn-gradient-accent fw-bold px-3 text-white" href="/vistas/admin/menu-admin.html">
                <i class="bi bi-shield-fill me-1"></i>Panel Admin
            </a>
        </li>` : '';

    return `${explorarYPanel}${btnSolicitudGestion}${btnAdmin}${_construirDropdown(nombre)}${btnNotificaciones}`;
}

// ─── Ensamblado principal ─────────────────────────────────────────────────────

function _construirLinks(perfil) {
    if (!perfil) {
        return {
            izquierda: `${_linksBusqueda()}${_LINK_IA}`,
            derecha: `
                <li class="nav-item">
                    <a class="nav-link fw-medium" href="/vistas/auth/login.html">Iniciar sesión</a>
                </li>
                <li class="nav-item me-2">
                    <a class="btn btn-sm btn-primary fw-medium px-3" href="/vistas/auth/elegir-tipo-cuenta.html">Registrarse</a>
                </li>`
        };
    }

    const roles  = perfil.roles?.map(r => r.nombre) || [];
    const nombre = perfil.nombre || 'Usuario';

    if (roles.length === 0) {
        return {
            izquierda: _linksBusqueda(),
            derecha: `
                <li class="nav-item">
                    <a class="btn btn-sm btn-outline-warning" href="/vistas/comun/estado-solicitud.html">
                        <i class="bi bi-hourglass-split me-1"></i>Ver solicitud de gestión
                    </a>
                </li>
                ${_construirDropdown(nombre, {
                    urlGestion:    '/vistas/comun/estado-solicitud.html',
                    labelGestion:  'Ver solicitud de gestión',
                    iconoGestion:  'bi-hourglass-split',
                })}`
        };
    }

    const tieneAdmin      = roles.includes('ADMIN');
    const tieneGestor     = roles.includes('GESTOR_CENTRO');
    const tieneEstudiante = roles.includes('ESTUDIANTE');

    return {
        izquierda: _construirIzquierda(tieneAdmin, tieneGestor, tieneEstudiante),
        derecha:   _construirDerecha(perfil, tieneAdmin, tieneGestor, tieneEstudiante)
    };
}

// ─── Plantilla HTML ───────────────────────────────────────────────────────────

function _construirNavbarHTML(perfil) {
    const { izquierda, derecha } = _construirLinks(perfil);
    return `
        <nav class="ef-navbar navbar navbar-expand-lg sticky-top">
            <div class="container-fluid px-4">
                <a href="/" class="ef-brand">
                    <span class="ef-brand-mark">EF</span>
                    <span class="ef-brand-name">EncuentraFormación</span>
                </a>
                <button class="navbar-toggler border-secondary" type="button"
                        data-bs-toggle="collapse" data-bs-target="#efNavbarCollapse"
                        aria-label="Menú">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="efNavbarCollapse">
                    <ul class="navbar-nav mx-auto align-items-center gap-1" id="navLinksIzq">
                        ${izquierda}
                    </ul>
                    <ul class="navbar-nav align-items-center gap-2" id="navLinksDer">
                        ${derecha}
                        <li class="nav-item">
                            <button id="themeToggle" class="ef-btn-icon" title="Cambiar tema">
                                <i id="themeIcon" class="bi bi-moon-fill"></i>
                            </button>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>`;
}

// ─── Badge de notificaciones ──────────────────────────────────────────────────

async function _actualizarBadgeNotificaciones(perfil) {
    const roles        = perfil?.roles?.map(r => r.nombre) || [];
    const esGestor     = roles.includes('GESTOR_CENTRO');
    const esEstudiante = roles.includes('ESTUDIANTE');
    if (!esGestor && !esEstudiante) return;

    const contexto = sessionStorage.getItem(_CONTEXTO_KEY) ||
                     (esGestor ? 'GESTOR_CENTRO' : 'ESTUDIANTE');
    const url = contexto === 'GESTOR_CENTRO'
        ? '/notificaciones/gestor/no-leidas/count'
        : '/notificaciones/no-leidas/count';
    try {
        const res = await fetch(url, { credentials: 'include' });
        if (!res.ok) return;
        const { count } = await res.json();
        const badge = document.getElementById('badgeNotif');
        if (!badge) return;
        badge.textContent = count > 99 ? '99+' : count;
        badge.classList.toggle('d-none', count === 0);
    } catch { /* silencioso */ }
}

// ─── Link activo ──────────────────────────────────────────────────────────────

function _marcarLinkActivo(container) {
    const path = window.location.pathname;
    container.querySelectorAll('a.nav-link[href]').forEach(a => {
        const href = a.getAttribute('href');
        const esActivo = href === '/' ? path === '/' : path === href;
        if (esActivo) a.classList.add('ef-nav-active');
    });
}

// ─── WebSocket ────────────────────────────────────────────────────────────────

function _iniciarWebSocket(perfil) {
    if (!perfil) return;
    const roles = perfil.roles?.map(r => r.nombre) || [];
    if (!roles.includes('GESTOR_CENTRO') && !roles.includes('ESTUDIANTE')) return;

    const arrancar = () => {
        WsClient.init(perfil.id);
        window.addEventListener('ef:notificacion', e => {
            const badge = document.getElementById('badgeNotif');
            if (!badge) return;
            const { count } = e.detail;
            badge.textContent = count > 99 ? '99+' : count;
            badge.classList.toggle('d-none', count === 0);
        });
    };

    if (typeof StompJs !== 'undefined') { arrancar(); return; }

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js';
    script.onload = () => {
        const wsScript = document.createElement('script');
        wsScript.src = '/js/comun/ws-client.js';
        wsScript.onload = arrancar;
        document.head.appendChild(wsScript);
    };
    document.head.appendChild(script);
}

// ─── Inicialización ───────────────────────────────────────────────────────────

async function inicializarNavbar() {
    const perfil    = await _obtenerPerfil();
    const container = document.getElementById('navbar-container');

    if (container) {
        container.innerHTML = _construirNavbarHTML(perfil);

        const tema  = document.documentElement.getAttribute('data-bs-theme') || 'light';
        const icono = document.getElementById('themeIcon');
        if (icono) icono.className = tema === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';

        _marcarLinkActivo(container);
        _actualizarBadgeNotificaciones(perfil);
        _iniciarWebSocket(perfil);

        container.addEventListener('click', e => {
            const toggleBtn = e.target.closest('[data-panel-toggle]');
            if (!toggleBtn) return;

            const currentCtx = sessionStorage.getItem(_CONTEXTO_KEY) || 'GESTOR_CENTRO';
            const newCtx = currentCtx === 'GESTOR_CENTRO' ? 'ESTUDIANTE' : 'GESTOR_CENTRO';
            sessionStorage.setItem(_CONTEXTO_KEY, newCtx);

            window.location.href = newCtx === 'GESTOR_CENTRO'
                ? '/vistas/gestor/vista-gestion-centro.html'
                : '/';
        });

        return;
    }

    // Fallback para páginas que usen navLinksIzq directo (no debería quedar ninguna)
    const navLinks = document.getElementById('navLinksIzq');
    if (navLinks) navLinks.innerHTML = _construirLinks(perfil).izquierda;
}

// ─── Sesión ───────────────────────────────────────────────────────────────────

async function cerrarSesion() {
    _limpiarCachePerfil();
    await fetch('/auth/logout', { method: 'POST', credentials: 'include' });
    window.location.href = '/';
}

// ─── Listeners globales ───────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', inicializarNavbar);

document.addEventListener('click', e => {
    if (e.target.closest('[data-action="cerrarSesion"]')) cerrarSesion();
});
