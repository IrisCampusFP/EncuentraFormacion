document.addEventListener('DOMContentLoaded', () => {
    cargarPerfil();

    document.getElementById('formPerfil').addEventListener('submit', async e => {
        e.preventDefault();
        await guardarPerfil();
    });

    document.getElementById('formPassword').addEventListener('submit', async e => {
        e.preventDefault();
        await cambiarPassword();
    });

    // Toggles contraseña
    [
        ['togglePassActual',      'passActual',      'togglePassActualIcon'],
        ['togglePassNueva',       'passNueva',       'togglePassNuevaIcon'],
        ['togglePassNuevaConfirm','passNuevaConfirm','togglePassNuevaConfirmIcon'],
    ].forEach(([btnId, inputId, iconId]) => {
        document.getElementById(btnId).addEventListener('click', () => {
            const input = document.getElementById(inputId);
            const icon  = document.getElementById(iconId);
            const show  = input.type === 'password';
            input.type     = show ? 'text' : 'password';
            icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
        });
    });

    // Validación en tiempo real contraseña
    const passwordRegex = /^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>\-_=+\[\]]).{8,}$/;
    const passFeedback  = document.getElementById('passFeedback');

    function mostrarFeedbackPassword() {
        const nueva   = document.getElementById('passNueva').value;
        const confirm = document.getElementById('passNuevaConfirm').value;

        if (nueva && !passwordRegex.test(nueva)) {
            passFeedback.textContent = 'Mínimo 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%^&*...).';
            passFeedback.style.display = 'block';
            return;
        }
        if (nueva && confirm && nueva !== confirm) {
            passFeedback.textContent = 'Las contraseñas no coinciden.';
            passFeedback.style.display = 'block';
            return;
        }
        passFeedback.style.display = 'none';
    }

    document.getElementById('passNueva').addEventListener('input', mostrarFeedbackPassword);
    document.getElementById('passNuevaConfirm').addEventListener('input', mostrarFeedbackPassword);
});

async function cargarPerfil() {
    try {
        const r = await fetch('/perfil', { credentials: 'include' });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        const p = await r.json();

        document.getElementById('nombre').value = p.nombre ?? '';
        document.getElementById('apellidos').value = p.apellidos ?? '';
        document.getElementById('email').value = p.email ?? '';
        document.getElementById('telefono').value = p.telefono ?? '';
        document.getElementById('fechaNacimiento').value = p.fechaNacimiento ?? '';
        document.getElementById('sexo').value = p.sexo ?? '';
        document.getElementById('username').value = p.username ?? '';
        document.getElementById('fechaAlta').textContent = p.fechaAlta
            ? `Cuenta creada el ${formatFecha(p.fechaAlta)}` : '';

        const esEstudiante = p.roles?.some(r => r.nombre === 'ESTUDIANTE');
        if (esEstudiante) {
            await cargarOpcionesGrado(p.gradoEstudiosId);
            await cargarOpcionesProvincia(p.provinciaId);
            document.getElementById('localidad').value = p.localidad ?? '';
            document.getElementById('seccionEstudiante').classList.remove('d-none');
        }
    } catch (e) {
        mostrarError(typeof e === 'string' ? e : 'Error al cargar el perfil');
    }
}

async function cargarOpcionesGrado(gradoActualId) {
    const select = document.getElementById('gradoEstudiosId');
    try {
        const r = await fetch('/grado-estudios', { credentials: 'include' });
        if (!r.ok) return;
        const grados = await r.json();
        grados.forEach(g => {
            const opt = document.createElement('option');
            opt.value = g.id;
            opt.textContent = g.nombre;
            if (g.id === gradoActualId) opt.selected = true;
            select.appendChild(opt);
        });
    } catch (_) { /* sin bloquear el resto */ }
}

let provinciaSeleccionada = null; // { id, nombre }

async function cargarOpcionesProvincia(provinciaActualId) {
    try {
        const r = await fetch('/provincias', { credentials: 'include' });
        if (!r.ok) return;
        const provincias = await r.json();
        const actual = provincias.find(p => p.id === provinciaActualId) || null;
        inicializarComboboxProvincia(provincias, actual);
    } catch (_) { /* sin bloquear el resto */ }
}

function inicializarComboboxProvincia(opciones, seleccionInicial) {
    const input      = document.getElementById('provinciaInput');
    const wrap       = document.getElementById('comboProvinciaWrap');
    const lista      = document.getElementById('comboProvinciaLista');
    const btnLimpiar = document.getElementById('comboProvincia-clear');

    function normalizar(s) {
        return s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    }

    function abrirDropdown(texto) {
        const q = normalizar(texto);
        const filtradas = q ? opciones.filter(o => normalizar(o.nombre).includes(q)) : opciones;
        lista.innerHTML = filtradas.length === 0
            ? `<li class="ef-combobox-sin-resultados">Sin resultados</li>`
            : filtradas.map(o => `<li data-id="${o.id}" data-nombre="${escapar(o.nombre)}">${escapar(o.nombre)}</li>`).join('');
        lista.classList.remove('d-none');
    }

    function cerrarDropdown() {
        lista.classList.add('d-none');
        if (!provinciaSeleccionada) input.value = '';
    }

    function seleccionar(id, nombre) {
        provinciaSeleccionada = { id, nombre };
        input.value = nombre;
        wrap.classList.add('seleccionado');
        btnLimpiar.classList.remove('d-none');
        lista.classList.add('d-none');
    }

    function limpiar() {
        provinciaSeleccionada = null;
        input.value = '';
        wrap.classList.remove('seleccionado');
        btnLimpiar.classList.add('d-none');
        lista.classList.add('d-none');
    }

    if (seleccionInicial) {
        provinciaSeleccionada = { id: seleccionInicial.id, nombre: seleccionInicial.nombre };
        input.value = seleccionInicial.nombre;
    }

    input.addEventListener('focus', () => abrirDropdown(input.value));
    input.addEventListener('input', () => {
        if (provinciaSeleccionada) limpiar();
        abrirDropdown(input.value);
    });
    lista.addEventListener('mousedown', e => {
        const li = e.target.closest('li[data-id]');
        if (!li) return;
        e.preventDefault();
        seleccionar(parseInt(li.dataset.id, 10), li.dataset.nombre);
    });
    btnLimpiar.addEventListener('click', () => limpiar());
    document.addEventListener('click', e => {
        if (!e.target.closest('#comboProvincia')) cerrarDropdown();
    });
}

async function guardarPerfil() {
    ocultarMensajes();
    const btn = document.getElementById('btnGuardar');
    const textoOriginal = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Guardando...';

    const body = {
        username: document.getElementById('username').value.trim(),
        email: document.getElementById('email').value.trim(),
        nombre: document.getElementById('nombre').value.trim(),
        apellidos: document.getElementById('apellidos').value.trim(),
        telefono: document.getElementById('telefono').value.trim() || null,
        fechaNacimiento: document.getElementById('fechaNacimiento').value || null,
        sexo: document.getElementById('sexo').value || null
    };

    const seccionEstudiante = document.getElementById('seccionEstudiante');
    if (!seccionEstudiante.classList.contains('d-none')) {
        const gradoSelect = document.getElementById('gradoEstudiosId');
        if (gradoSelect.value) body.gradoEstudiosId = parseInt(gradoSelect.value, 10);

        body.provinciaId = provinciaSeleccionada ? provinciaSeleccionada.id : null;

        body.localidad = document.getElementById('localidad').value.trim() || null;
    }

    try {
        const r = await fetch('/perfil', {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        document.getElementById('alertExito').classList.remove('d-none');
        document.getElementById('alertExito').scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (e) {
        mostrarError(typeof e === 'string' ? e : 'Error al guardar');
    } finally {
        btn.disabled = false;
        btn.innerHTML = textoOriginal;
    }
}

async function cambiarPassword() {
    const passwordRegex = /^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>\-_=+\[\]]).{8,}$/;
    const passActual      = document.getElementById('passActual').value;
    const passNueva       = document.getElementById('passNueva').value;
    const passNuevaConfirm = document.getElementById('passNuevaConfirm').value;
    const passError       = document.getElementById('passError');
    const passExito       = document.getElementById('passExito');
    const passFeedback    = document.getElementById('passFeedback');

    passError.classList.add('d-none');
    passExito.classList.add('d-none');

    if (!passActual) {
        passError.textContent = 'Debes introducir tu contraseña actual.';
        passError.classList.remove('d-none');
        return;
    }

    if (!passwordRegex.test(passNueva)) {
        passFeedback.textContent = 'Mínimo 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%^&*...).';
        passFeedback.style.display = 'block';
        return;
    }
    if (passNueva !== passNuevaConfirm) {
        passFeedback.textContent = 'Las contraseñas no coinciden.';
        passFeedback.style.display = 'block';
        return;
    }
    passFeedback.style.display = 'none';

    const btn = document.getElementById('btnCambiarPass');
    const textoOriginal = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Guardando...';

    try {
        const r = await fetch('/perfil/password', {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ passwordActual: passActual, passwordNueva: passNueva })
        });
        if (!r.ok) { const d = await r.json(); throw d.errorMsg || `Error ${r.status}`; }
        document.getElementById('formPassword').reset();
        passExito.classList.remove('d-none');
        passExito.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (e) {
        passError.textContent = typeof e === 'string' ? e : 'Error al cambiar la contraseña.';
        passError.classList.remove('d-none');
    } finally {
        btn.disabled = false;
        btn.innerHTML = textoOriginal;
    }
}

function nombreLegibleRol(nombre) {
    switch (nombre) {
        case 'ADMIN': return 'Administrador';
        case 'GESTOR_CENTRO': return 'Gestor de centro';
        case 'ESTUDIANTE': return 'Estudiante';
        default: return nombre;
    }
}

function formatFecha(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('es-ES', { day: '2-digit', month: 'long', year: 'numeric' });
}

function mostrarError(msg) {
    const el = document.getElementById('recuadroAlert');
    el.textContent = msg;
    el.classList.remove('d-none');
}

function ocultarMensajes() {
    document.getElementById('recuadroAlert').classList.add('d-none');
    document.getElementById('alertExito').classList.add('d-none');
}
