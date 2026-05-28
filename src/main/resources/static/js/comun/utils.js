// Funciones de utilidad compartidas entre las páginas públicas

// ─── Historial de navegación para breadcrumb dinámico ────────────────────────
// Cada página llama a breadcrumbPush({ label, url }) al conocer su título.
// breadcrumbRender(containerId) construye el <ol> en el elemento dado.

const NAV_HISTORY_KEY = 'ef_nav_history';
const NAV_HISTORY_MAX = 6;

function _navHistory() {
    try { return JSON.parse(sessionStorage.getItem(NAV_HISTORY_KEY) || '[]'); }
    catch { return []; }
}

function _navHistorySave(stack) {
    sessionStorage.setItem(NAV_HISTORY_KEY, JSON.stringify(stack));
}

function referrerPath() {
    try {
        if (!document.referrer) return null;
        const u = new URL(document.referrer);
        return u.pathname + u.search;
    } catch { return null; }
}

function breadcrumbPush(label, url, parentUrl) {
    let stack = _navHistory().filter(e => e.url !== '/');
    const existing = stack.findIndex(e => e.url === url);

    if (existing !== -1) {
        // URL ya en el stack: recortar hasta ahí
        const trimmed = stack.slice(0, existing + 1);
        trimmed[existing].label = label;
        _navHistorySave(trimmed);
        return;
    }

    // Si se indica parentUrl y no coincide con el último elemento del stack,
    // el usuario viene de una cadena de navegación diferente → limpiar
    if (parentUrl) {
        const last = stack[stack.length - 1];
        if (!last || last.url !== parentUrl) {
            stack = [];
        }
    }

    stack.push({ label, url });
    if (stack.length > NAV_HISTORY_MAX) stack.shift();
    _navHistorySave(stack);
}

function breadcrumbRender(listId) {
    const ol = document.getElementById(listId);
    if (!ol) return;
    const stack = _navHistory();

    const buscarLi = document.createElement('li');
    buscarLi.className = 'breadcrumb-item';
    const buscarA = document.createElement('a');
    buscarA.href = '/';
    buscarA.textContent = 'Buscar';
    buscarA.addEventListener('click', () => _navHistorySave([]));
    buscarLi.appendChild(buscarA);

    ol.innerHTML = '';
    ol.appendChild(buscarLi);

    stack.forEach((e, i) => {
        const li = document.createElement('li');
        const isLast = i === stack.length - 1;
        li.className = `breadcrumb-item${isLast ? ' active' : ''}`;
        if (isLast) {
            li.setAttribute('aria-current', 'page');
            li.textContent = e.label;
        } else {
            const a = document.createElement('a');
            a.href = e.url;
            a.textContent = e.label;
            a.addEventListener('click', () => _navHistorySave(stack.slice(0, i + 1)));
            li.appendChild(a);
        }
        ol.appendChild(li);
    });
}


function normalizar(str) {
    if (!str) return '';
    return str.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '');
}

function escapar(str) {
    const d = document.createElement('div');
    d.textContent = str ?? '';
    return d.innerHTML;
}

function truncar(str, max) {
    return str.length > max ? str.substring(0, max) + '…' : str;
}

function renderEstrellas(media) {
    const redondeado = Math.round(media * 2) / 2;
    const entera = Math.floor(redondeado);
    const mitad  = redondeado % 1 !== 0;
    let html = '';
    for (let i = 1; i <= 5; i++) {
        let tipo;
        if (i <= entera)            tipo = '-fill';
        else if (i === entera + 1 && mitad) tipo = '-half';
        else                        tipo = '';
        html += `<i class="bi bi-star${tipo} text-warning ef-fs-sm"></i>`;
    }
    return html;
}

function formatModalidad(m) {
    if (!m) return '';
    return { PRESENCIAL: 'Presencial', SEMIPRESENCIAL: 'Semipresencial', DISTANCIA: 'A distancia' }[m.toUpperCase()] || m;
}

function formatHorario(h) {
    if (!h) return '';
    return { MANANA: 'Mañana', TARDE: 'Tarde', NOCHE: 'Noche', FLEXIBLE: 'Flexible' }[h.toUpperCase()] || h;
}

function formatTipoCentro(t) {
    if (!t) return '';
    return { PUBLICO: 'Público', PRIVADO: 'Privado', CONCERTADO: 'Concertado' }[t.toUpperCase()] || t;
}

const BADGE_TIPO_ESTUDIOS = {
    'Educación Infantil 1er ciclo': 'ef-badge-infantil',
    'Educación Infantil 2º ciclo':  'ef-badge-infantil',
    'Educación Primaria':           'ef-badge-primaria',
    'ESO':                          'ef-badge-eso',
    'Bachillerato':                 'ef-badge-bachillerato',
    'FP Básica':                    'ef-badge-fp-basica',
    'FP Grado Medio':               'ef-badge-grado-med',
    'FP Grado Superior':            'ef-badge-grado-sup',
    'Educación Especial':           'ef-badge-espec',
    'Idiomas':                      'ef-badge-idiomas',
    'Música y Artes':               'ef-badge-artes',
    'Enseñanzas Deportivas':        'ef-badge-deportes',
    'Educación de Adultos':         'ef-badge-adultos',
    'Curso / Formación no reglada': 'ef-badge-curso',
    'Certificado de Profesionalidad': 'ef-badge-cert-prof',
    'Grado Universitario':          'ef-badge-universidad',
    'Máster Oficial':               'ef-badge-master-oficial',
    'Máster / Título propio':       'ef-badge-master',
    'Doctorado':                    'ef-badge-doctorado',
};

const BADGE_MODALIDAD = {
    PRESENCIAL:     'ef-badge-presencial',
    SEMIPRESENCIAL: 'ef-badge-semipresencial',
    DISTANCIA:      'ef-badge-distancia',
};

const BADGE_TIPO_CENTRO = {
    PUBLICO:    { class: 'ef-badge-publico',    label: 'Público' },
    PRIVADO:    { class: 'ef-badge-privado',    label: 'Privado' },
    CONCERTADO: { class: 'ef-badge-concertado', label: 'Concertado' },
    MIXTO:      { class: 'ef-badge-default',    label: 'Mixto' },
};

function formatFecha(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
}

function formatFechaHora(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleString('es-ES', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatPrecio(precio) {
    if (precio == null) return { texto: 'No disponible', color: 'ef-color-muted' };
    const num = parseFloat(precio);
    if (num === 0) return { texto: 'Gratis', color: 'text-success' };
    return { texto: `${num.toLocaleString('es-ES', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} €`, color: 'ef-color-primary' };
}

async function loginAutomatico(email, password) {
    const response = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email, password })
    });
    const data = await response.json();
    if (response.ok) {
        sessionStorage.removeItem("ef_perfil");
        const roles = data.roles || [];
        window.location.href = roles.length === 0
            ? "/vistas/comun/estado-solicitud.html"
            : landingSegunRol(roles);
    } else {
        window.location.href = "/vistas/auth/login.html";
    }
}

function landingSegunRol(roles) {
    const r = roles ?? JSON.parse(sessionStorage.getItem('ef_perfil') || 'null')?.roles?.map(r => r.nombre) ?? [];
    if (r.includes('ADMIN'))         return '/vistas/admin/menu-admin.html';
    if (r.includes('GESTOR_CENTRO')) return '/vistas/gestor/vista-gestion-centro.html';
    return '/';
}

// ─── Combobox accesible con búsqueda y selección desde lista ─────────────────
function crearCombobox(opciones, comboId, inputId, wrapId, listaId, clearId) {
    let seleccionado = null;
    const input      = document.getElementById(inputId);
    const wrap       = document.getElementById(wrapId);
    const lista      = document.getElementById(listaId);
    const btnLimpiar = document.getElementById(clearId);

    function abrir(texto) {
        const q = normalizar(texto);
        const filtradas = q ? opciones.filter(o => normalizar(o.nombre).includes(q)) : opciones;
        lista.innerHTML = filtradas.length === 0
            ? '<li class="ef-combobox-sin-resultados">Sin resultados</li>'
            : filtradas.map(o => `<li data-nombre="${o.nombre}">${o.nombre}</li>`).join("");
        lista.classList.remove("d-none");
    }

    function cerrar() {
        lista.classList.add("d-none");
        if (!seleccionado) input.value = "";
    }

    function seleccionar(nombre) {
        seleccionado = nombre;
        input.value = nombre;
        wrap.classList.add("seleccionado");
        btnLimpiar.classList.remove("d-none");
        lista.classList.add("d-none");
    }

    function preseleccionar(nombre) {
        if (!nombre) return;
        seleccionado = nombre;
        input.value = nombre;
        lista.classList.add("d-none");
    }

    function limpiar() {
        seleccionado = null;
        input.value = "";
        wrap.classList.remove("seleccionado");
        btnLimpiar.classList.add("d-none");
        lista.classList.add("d-none");
    }

    input.addEventListener("focus", () => abrir(input.value));
    input.addEventListener("input", () => {
        seleccionado = null;
        wrap.classList.remove("seleccionado");
        btnLimpiar.classList.add("d-none");
        abrir(input.value);
    });
    lista.addEventListener("mousedown", e => {
        const li = e.target.closest("li[data-nombre]");
        if (!li) return;
        e.preventDefault();
        seleccionar(li.dataset.nombre);
    });
    btnLimpiar.addEventListener("click", () => limpiar());
    document.addEventListener("click", e => { if (!e.target.closest(`#${comboId}`)) cerrar(); });

    return { seleccionar, preseleccionar, limpiar, get valor() { return seleccionado; } };
}

// ─── Event Delegation Global (CSP-Safe) ──────────────────────────────────────
document.addEventListener('click', e => {
    const clickable = e.target.closest('[data-href]');
    if (clickable) {
        window.location.href = clickable.getAttribute('data-href');
    }
});
