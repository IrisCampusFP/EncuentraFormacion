document.addEventListener('DOMContentLoaded', () => {
    const contenedorDatos  = document.getElementById('datosSolicitud');
    const msgError         = document.getElementById('msgError');
    const msgEmail         = document.getElementById('msgEmail');
    const msgEstado        = document.getElementById('msgEstado');
    const btnCancelar      = document.getElementById('btnCancelar');
    const btnAccion        = document.getElementById('btnAccionPrincipal');

    // Si se accede con ?id=N se muestra esa solicitud concreta; si no, la más reciente pendiente
    const idEnUrl = new URLSearchParams(window.location.search).get('id');

    let idSolicitud = null;
    let sinRol      = true;

    function badgeEstado(estado) {
        const ESTADOS = {
            PENDIENTE: { clase: 'text-bg-warning',   icono: 'bi-clock',         texto: 'En revisión'  },
            ACEPTADA:  { clase: 'text-bg-success',   icono: 'bi-check-circle',  texto: 'Aceptada'     },
            RECHAZADA: { clase: 'text-bg-danger',    icono: 'bi-x-circle',      texto: 'Rechazada'    },
            CANCELADA: { clase: 'text-bg-secondary', icono: 'bi-dash-circle',   texto: 'Cancelada'    },
        };
        const { clase, icono, texto } = ESTADOS[estado] || { clase: 'text-bg-secondary', icono: 'bi-question', texto: estado };
        return `<span class="badge ${clase} fs-6 px-3 py-2"><i class="bi ${icono} me-1"></i>${texto}</span>`;
    }

    function dato(label, valor) {
        return `
            <div class="mb-3">
                <div class="text-muted small fw-semibold text-uppercase mb-1" style="letter-spacing:.04em">${label}</div>
                <div>${valor || '<span class="text-muted">—</span>'}</div>
            </div>`;
    }

    async function cargarDatos() {
        try {
            // Endpoint de solicitud: por id específico o la más reciente pendiente
            const urlSolicitud = idEnUrl
                ? `/solicitudes-gestion/mis-solicitudes/${idEnUrl}`
                : '/solicitudes-gestion/mi-solicitud';

            const [resSolicitud, resUsuario] = await Promise.all([
                fetch(urlSolicitud, { credentials: 'include' }),
                fetch('/perfil', { credentials: 'include' }),
            ]);

            if (!resSolicitud.ok) throw new Error('No se han podido cargar los datos de tu solicitud.');
            if (!resUsuario.ok)   throw new Error('No se han podido cargar tus datos de usuario.');

            const solicitud = await resSolicitud.json();
            const usuario   = await resUsuario.json();

            const resCentro = await fetch(
                `/solicitudes-gestion/mis-solicitudes/${solicitud.id}/centro`, { credentials: 'include' });

            if (!resCentro.ok) throw new Error('No se han podido cargar los datos del centro asociado.');

            const centro = await resCentro.json();

            idSolicitud = solicitud.id;
            sinRol      = !usuario.roles || usuario.roles.length === 0;

            // Adaptar el banner de estado al estado real de la solicitud
            const bannerConfig = {
                PENDIENTE: { clase: 'alert-warning', icono: 'bi-clock-history', titulo: 'Solicitud pendiente de revisión' },
                ACEPTADA:  { clase: 'alert-success', icono: 'bi-check-circle',  titulo: 'Solicitud aceptada'             },
                RECHAZADA: { clase: 'alert-danger',  icono: 'bi-x-circle',      titulo: 'Solicitud rechazada'            },
                CANCELADA: { clase: 'alert-secondary', icono: 'bi-dash-circle', titulo: 'Solicitud cancelada'            },
            };
            const banner = bannerConfig[solicitud.estado] || bannerConfig.PENDIENTE;
            msgEstado.className = `alert ${banner.clase} d-flex align-items-start gap-3 mb-4`;
            msgEstado.querySelector('i').className = `${banner.icono} fs-5 flex-shrink-0 mt-1 bi`;
            msgEstado.querySelector('strong').textContent = banner.titulo;

            // Ocultar botón de cancelar si la solicitud ya no es cancelable
            if (solicitud.estado !== 'PENDIENTE') {
                btnCancelar.classList.add('d-none');
            }

            // Configurar botón principal según rol
            if (sinRol) {
                btnAccion.innerHTML = '<i class="bi bi-box-arrow-right me-1"></i>Salir';
                btnAccion.addEventListener('click', () => cerrarSesion());
            } else {
                btnAccion.innerHTML = '<i class="bi bi-arrow-left me-1"></i>Volver';
                btnAccion.addEventListener('click', () => history.back());
            }

            // Actualizar mensaje de estado con el email
            msgEmail.textContent = `Se enviará un correo a ${usuario.email} con la resolución.`;

            // Dirección formateada
            const direccionCompleta = [centro.direccion, centro.localidad, centro.provincia]
                .filter(Boolean).join(', ');

            const webHtml = centro.paginaWeb
                ? `<a href="${centro.paginaWeb}" target="_blank" rel="noopener">${centro.paginaWeb}</a>`
                : null;

            const fechaFormateada = solicitud.fechaSolicitud
                ? new Date(solicitud.fechaSolicitud).toLocaleDateString('es-ES', { day: '2-digit', month: 'long', year: 'numeric' })
                : '—';

            contenedorDatos.innerHTML = `
                <h6 class="fw-semibold mb-3 pb-2 border-bottom">
                    <i class="bi bi-building me-2 text-primary"></i>Datos del centro
                </h6>
                <div class="row">
                    <div class="col-sm-6">
                        ${dato('Nombre comercial', centro.nombreComercial)}
                        ${dato('Código de centro', centro.codigo)}
                        ${dato('Teléfono', centro.telefono)}
                        ${dato('Correo electrónico', centro.email)}
                    </div>
                    <div class="col-sm-6">
                        ${dato('Dirección', direccionCompleta)}
                        ${dato('Tipo de centro', centro.tipo)}
                        ${dato('Página web', webHtml)}
                    </div>
                </div>

                <h6 class="fw-semibold mb-3 mt-2 pb-2 border-bottom">
                    <i class="bi bi-file-earmark-text me-2 text-primary"></i>Detalles de la solicitud
                </h6>
                <div class="row">
                    <div class="col-sm-6">
                        ${dato('Solicitante', `${solicitud.nombre ?? ''} ${solicitud.apellidos ?? ''}`.trim())}
                        ${dato('Fecha de envío', fechaFormateada)}
                    </div>
                    <div class="col-sm-6">
                        <div class="mb-3">
                            <div class="text-muted small fw-semibold text-uppercase mb-1" style="letter-spacing:.04em">Estado actual</div>
                            ${badgeEstado(solicitud.estado)}
                        </div>
                    </div>
                </div>`;

            btnCancelar.classList.remove('disabled');

        } catch (error) {
            contenedorDatos.innerHTML = `
                <div class="text-center py-4 text-danger">
                    <i class="bi bi-exclamation-triangle fs-4 d-block mb-2"></i>
                    ${error.message}
                </div>`;
        }
    }

    cargarDatos();

    btnCancelar.addEventListener('click', async () => {
        if (!idSolicitud) return;

        const ok = await efConfirm({
            title:       'Cancelar solicitud',
            message:     sinRol
                ? 'Esto cancelará tu solicitud de alta en el sistema y <strong>eliminará también tus datos de usuario</strong>.'
                : '¿Seguro que quieres cancelar esta solicitud?',
            confirmText: 'Sí, cancelar',
            cancelText:  'Volver',
            variant:     'danger',
        });
        if (!ok) return;

        btnCancelar.disabled = true;
        btnCancelar.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Cancelando...';

        try {
            const res = await fetch(`/solicitudes-gestion/${idSolicitud}`, { method: 'DELETE', credentials: 'include' });

            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.errorMsg || 'Error al cancelar la solicitud.');
            }

            if (sinRol) {
                alert('Tu solicitud ha sido eliminada. Puedes volver a registrarte para enviar una nueva.');
                window.location.href = '/logout';
            } else {
                alert('Tu solicitud ha sido cancelada correctamente.');
                window.location.href = '/';
            }
        } catch (error) {
            msgError.textContent = error.message;
            msgError.classList.remove('d-none');
            btnCancelar.disabled = false;
            btnCancelar.innerHTML = '<i class="bi bi-x-circle me-1"></i>Cancelar solicitud';
        }
    });
});
