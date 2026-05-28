document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));

    const seccionActivas      = document.getElementById('seccionSolicitudesActivas');
    const listaActivas        = document.getElementById('listaSolicitudesActivas');
    const formError           = document.getElementById('formError');
    const formSuccess         = document.getElementById('formSuccess');
    const codigoInput         = document.getElementById('codigoBusqueda');
    const codigoError         = document.getElementById('codigoError');
    const btnComprobar        = document.getElementById('btnComprobar');
    const btnComprobarText    = document.getElementById('btnComprobarText');
    const btnComprobarSpinner = document.getElementById('btnComprobarSpinner');
    const mensajeBusqueda     = document.getElementById('mensajeBusqueda');
    const seccionTitularidad  = document.getElementById('seccionTitularidad');
    const seccionCentroNuevo  = document.getElementById('seccionCentroNuevo');
    const centroIdInput       = document.getElementById('centroIdSeleccionado');
    const pruebaTitularidad   = document.getElementById('pruebaTitularidad');
    const btnEnviar           = document.getElementById('btnEnviar');
    const form                = document.getElementById('formSolicitud');
    let comboboxProvincia = null;

    // ── Validación inline código ─────────────────────────────────────────────────

    codigoInput.addEventListener('blur', () => {
        const val = codigoInput.value.trim();
        codigoError.textContent = '';
        codigoInput.classList.remove('is-invalid');
        if (!val) return;
        if (!/^\d{8}$/.test(val)) {
            codigoError.textContent = 'El código debe tener 8 dígitos numéricos.';
            codigoInput.classList.add('is-invalid');
        }
    });

    codigoInput.addEventListener('input', () => {
        codigoInput.classList.remove('is-invalid');
        codigoError.textContent = '';
        ocultarTitularidad();
        mensajeBusqueda.classList.add('d-none');
    });


    // ── Combobox de provincia ────────────────────────────────────────────────────

    fetch('/provincias')
        .then(r => r.json())
        .then(provincias => {
            comboboxProvincia = crearCombobox(provincias, 'comboProvincia', 'provinciaCentro', 'comboProvinciaWrap', 'comboProvinciaLista', 'comboProvincia-clear');
        })
        .catch(() => {
            document.getElementById('provinciaCentro').placeholder = 'Error al cargar provincias';
        });

    const ESTADO_LABEL = {
        PENDIENTE: { texto: 'En revisión',  clase: 'text-bg-warning' },
        ACEPTADA:  { texto: 'Aceptada',     clase: 'text-bg-success' },
        RECHAZADA: { texto: 'Rechazada',    clase: 'text-bg-danger'  },
        CANCELADA: { texto: 'Cancelada',    clase: 'text-bg-secondary' },
    };

    // ── Carga de solicitudes existentes ─────────────────────────────────────────

    async function cargarSolicitudesActivas() {
        try {
            const res = await fetch('/solicitudes-gestion/mis-solicitudes', { credentials: 'include' });
            if (!res.ok) return;

            const solicitudes = await res.json();
            if (!solicitudes || solicitudes.length === 0) return;

            seccionActivas.classList.remove('d-none');
            listaActivas.innerHTML = solicitudes.map(s => tarjetaSolicitud(s)).join('');

            listaActivas.querySelectorAll('[data-cancelar]').forEach(btn => {
                btn.addEventListener('click', () => cancelarSolicitud(
                    parseInt(btn.dataset.cancelar), btn.dataset.centro));
            });

        } catch { /* silencioso — no bloquea el formulario */ }
    }

    function tarjetaSolicitud(s) {
        const { texto, clase } = ESTADO_LABEL[s.estado] || { texto: s.estado, clase: 'text-bg-secondary' };
        const fecha = s.fechaSolicitud
            ? new Date(s.fechaSolicitud).toLocaleDateString('es-ES', { day: '2-digit', month: 'long', year: 'numeric' })
            : '—';
        const btnCancelar = s.estado === 'PENDIENTE'
            ? `<button class="btn btn-sm btn-outline-danger" data-cancelar="${s.id}" data-centro="${s.nombreCentro ?? ''}">
                   <i class="bi bi-x-circle me-1"></i>Cancelar
               </button>`
            : '';

        return `
            <div class="card border-0 shadow-sm mb-3">
                <div class="card-body p-4 d-flex justify-content-between align-items-start gap-3 flex-wrap">
                    <div>
                        <div class="fw-semibold mb-1">
                            <i class="bi bi-building me-2 text-primary"></i>${s.nombreCentro ?? '—'}
                        </div>
                        <div class="text-muted small">Enviada el ${fecha}</div>
                    </div>
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <span class="badge ${clase} px-3 py-2">${texto}</span>
                        ${btnCancelar}
                    </div>
                </div>
            </div>`;
    }

    async function cancelarSolicitud(idSolicitud, nombreCentro) {
        const confirmado = confirm(
            `¿Cancelar la solicitud para "${nombreCentro}"? Esta acción no se puede deshacer.`);
        if (!confirmado) return;

        try {
            const res = await fetch(`/solicitudes-gestion/${idSolicitud}`, {
                method: 'DELETE',
                credentials: 'include',
            });
            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.errorMsg || 'Error al cancelar la solicitud.');
            }
            await cargarSolicitudesActivas();
        } catch (err) {
            mostrarError(err.message);
        }
    }

    // ── Búsqueda de centro ───────────────────────────────────────────────────────

    btnComprobar.addEventListener('click', async () => {
        const codigo = codigoInput.value.trim();
        codigoError.textContent = '';
        codigoInput.classList.remove('is-invalid');

        if (!codigo) {
            codigoError.textContent = 'Introduce el código del centro.';
            codigoInput.classList.add('is-invalid');
            return;
        }

        if (!/^\d{8}$/.test(codigo)) {
            codigoError.textContent = 'El código debe tener 8 dígitos numéricos.';
            codigoInput.classList.add('is-invalid');
            return;
        }

        setBuscarCargando(true);
        ocultarTitularidad();

        try {
            const res = await fetch(`/centros/existe?codigo=${encodeURIComponent(codigo)}`, {
                credentials: 'include',
            });

            if (res.status === 404) {
                mostrarMensajeBusqueda(
                    `No hay ningún centro registrado con el código <strong>${codigo}</strong>. ` +
                    `Rellena los datos a continuación para darlo de alta y enviar la solicitud.`,
                    'alert-warning');
                seccionCentroNuevo.classList.remove('d-none');
                seccionCentroNuevo.scrollIntoView({ behavior: 'smooth', block: 'start' });
                return;
            }

            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.errorMsg || 'Error al buscar el centro.');
            }

            const centro = await res.json();
            centroIdInput.value = centro.id;
            mostrarMensajeBusqueda(
                `<i class="bi bi-check-circle-fill me-2 text-success"></i>Centro encontrado: <strong>${centro.nombreComercial}</strong>` +
                (centro.verificado ? '' : ' <span class="badge text-bg-warning ms-2">Pendiente de verificación</span>'),
                'alert-success');
            seccionTitularidad.classList.remove('d-none');

        } catch (err) {
            mostrarMensajeBusqueda(err.message, 'alert-danger');
        } finally {
            setBuscarCargando(false);
        }
    });

    function setBuscarCargando(cargando) {
        btnComprobar.disabled = cargando;
        btnComprobarText.classList.toggle('d-none', cargando);
        btnComprobarSpinner.classList.toggle('d-none', !cargando);
    }

    function ocultarTitularidad() {
        seccionTitularidad.classList.add('d-none');
        seccionCentroNuevo.classList.add('d-none');
        centroIdInput.value = '';
        pruebaTitularidad.value = '';
    }

    function mostrarMensajeBusqueda(html, claseAlert) {
        mensajeBusqueda.innerHTML = html;
        mensajeBusqueda.className = `alert ${claseAlert} mb-3`;
        mensajeBusqueda.classList.remove('d-none');
    }

    // ── Envío del formulario ─────────────────────────────────────────────────────

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        ocultarAlertas();

        const centroId         = centroIdInput.value;
        const centroNuevoVisible = !seccionCentroNuevo.classList.contains('d-none');

        if (!centroId && !centroNuevoVisible) {
            mostrarError('Debes buscar un centro antes de enviar.');
            return;
        }

        if (centroId) {
            await enviarCentroExistente();
        } else {
            await enviarCentroNuevo();
        }
    });

    async function enviarCentroExistente() {
        if (!pruebaTitularidad.files || pruebaTitularidad.files.length === 0) {
            mostrarError('Debes adjuntar una prueba de vinculación con el centro.');
            return;
        }
        const archivo = pruebaTitularidad.files[0];
        if (!['image/jpeg', 'image/png', 'application/pdf'].includes(archivo.type)) {
            mostrarError('El archivo debe ser una imagen (JPG, PNG) o un documento PDF.');
            return;
        }
        if (archivo.size > 10 * 1024 * 1024) {
            mostrarError('El archivo no puede superar los 10 MB.');
            return;
        }

        const btn = btnEnviar;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Enviando...';

        try {
            const datos = { centroId: parseInt(centroIdInput.value) };
            const formData = new FormData();
            formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
            formData.append('pruebaTitularidad', archivo);

            const res = await fetch('/solicitudes-gestion', {
                method: 'POST', credentials: 'include', body: formData,
            });
            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.errorMsg || 'Error al enviar la solicitud.');
            }
            await onEnvioExitoso();
        } catch (err) {
            mostrarError(err.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = 'Enviar solicitud';
        }
    }

    async function enviarCentroNuevo() {
        const nombre    = document.getElementById('nombreCentro').value.trim();
        const direccion = document.getElementById('direccionCentro').value.trim();
        const localidad = document.getElementById('localidadCentro').value.trim();
        const tipo      = document.getElementById('tipoCentro').value;

        if (!nombre || !direccion || !localidad || !comboboxProvincia?.valor || !tipo) {
            mostrarError('Nombre, dirección, localidad, provincia y tipo de centro son obligatorios.');
            return;
        }

        const prueba = document.getElementById('pruebaTitularidadNuevo');
        if (!prueba.files || prueba.files.length === 0) {
            mostrarError('Debes adjuntar una prueba de vinculación con el centro.');
            return;
        }
        const archivo = prueba.files[0];
        if (!['image/jpeg', 'image/png', 'application/pdf'].includes(archivo.type)) {
            mostrarError('El archivo debe ser una imagen (JPG, PNG) o un documento PDF.');
            return;
        }
        if (archivo.size > 10 * 1024 * 1024) {
            mostrarError('El archivo no puede superar los 10 MB.');
            return;
        }

        const btn = document.getElementById('btnEnviarNuevo');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Enviando...';

        try {
            const datosCentro = {
                codigo:          codigoInput.value.trim(),
                nombreComercial: nombre,
                direccion,
                localidad,
                provincia: comboboxProvincia.valor,
                tipo,
                telefono:  document.getElementById('telefonoCentro').value.trim() || null,
                email:     document.getElementById('emailCentro').value.trim()    || null,
                paginaWeb: document.getElementById('webCentro').value.trim()      || null,
            };

            const formData = new FormData();
            formData.append('datosCentro', new Blob([JSON.stringify(datosCentro)], { type: 'application/json' }));
            formData.append('pruebaTitularidad', archivo);

            const res = await fetch('/solicitudes-gestion/con-centro-nuevo', {
                method: 'POST', credentials: 'include', body: formData,
            });
            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.errorMsg || 'Error al registrar el centro y enviar la solicitud.');
            }
            await onEnvioExitoso();
        } catch (err) {
            mostrarError(err.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = 'Registrar centro y enviar solicitud';
        }
    }

    async function onEnvioExitoso() {
        formSuccess.textContent = 'Solicitud enviada correctamente. Será revisada y recibirás una notificación con el resultado.';
        formSuccess.classList.remove('d-none');
        form.reset();
        ocultarTitularidad();
        mensajeBusqueda.classList.add('d-none');
        await cargarSolicitudesActivas();
        formSuccess.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // ── Utilidades ───────────────────────────────────────────────────────────────

    function mostrarError(mensaje) {
        formError.textContent = mensaje;
        formError.classList.remove('d-none');
        formError.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    function ocultarAlertas() {
        formError.classList.add('d-none');
        formSuccess.classList.add('d-none');
    }

    // ── Navegación ───────────────────────────────────────────────────────────────

    document.querySelector('[data-volver]')?.addEventListener('click', () => {
        if (history.length > 1) history.back();
        else window.location.href = landingSegunRol();
    });

    // ── Init ─────────────────────────────────────────────────────────────────────

    cargarSolicitudesActivas();
});
