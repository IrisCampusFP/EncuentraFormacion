document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));

    const formCentro    = document.getElementById("formularioCentro");
    const recuadroError = document.getElementById("registroError");
    const codigoInput   = document.getElementById("codigo");
    const codigoError   = document.getElementById("codigoError");
    let comboboxProvincia = null;

    codigoInput.addEventListener("blur", async () => {
        const val = codigoInput.value.trim();
        codigoError.textContent = "";
        codigoInput.classList.remove("is-invalid");

        if (!val) return;

        if (!/^\d{8}$/.test(val)) {
            codigoError.textContent = "El código debe tener 8 dígitos numéricos.";
            codigoInput.classList.add("is-invalid");
            return;
        }

        try {
            const r = await fetch(`/centros/existe?codigo=${encodeURIComponent(val)}`);
            if (r.ok) {
                const centro = await r.json();
                if (centro.verificado) {
                    codigoError.innerHTML = `Ya existe un centro con ese código: <a href="/vistas/publico/perfil-centro.html?uuid=${centro.uuid}" class="text-reset text-decoration-underline fw-bold">${centro.nombreComercial}</a>.`;
                } else {
                    codigoError.innerHTML = `Este centro ya está registrado y pendiente de verificación.`;
                }
                codigoInput.classList.add("is-invalid");
            }
        } catch (_) {}
    });

    // Combobox de provincias
    fetch("/provincias")
        .then(r => r.json())
        .then(provincias => {
            comboboxProvincia = crearCombobox(provincias, 'comboProvincia', 'provincia', 'comboProvinciaWrap', 'comboProvinciaLista', 'comboProvincia-clear');
        })
        .catch(() => {
            document.getElementById("provincia").placeholder = "Error al cargar provincias";
        });

    formCentro.addEventListener("submit", async (e) => {
        e.preventDefault();

        const codigo = document.getElementById("codigo").value.trim();
        if (!/^\d{8}$/.test(codigo)) {
            mostrarError("El código del centro debe tener exactamente 8 dígitos numéricos.");
            return;
        }

        if (!comboboxProvincia?.valor) {
            mostrarError("Debes seleccionar una provincia de la lista.");
            return;
        }

        const btnEnviar       = document.getElementById("btnSubmitCentro");
        const textoOriginal   = btnEnviar.innerHTML;
        btnEnviar.disabled    = true;
        btnEnviar.innerHTML   = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Registrando...';

        recuadroError.classList.add("d-none");

        const datosCentro = {
            nombreComercial: document.getElementById("nombre").value.trim(),
            codigo:          codigo,
            direccion:       document.getElementById("direccion").value.trim(),
            localidad:       document.getElementById("localidad").value.trim(),
            provincia:       comboboxProvincia.valor,
            telefono:        document.getElementById("telefono").value.trim() || null,
            email:           document.getElementById("email").value.trim() || null,
            paginaWeb:       document.getElementById("web").value.trim() || null,
            tipo:            document.getElementById("tipo").value
        };

        try {
            const response = await fetch("/registro/centro", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(datosCentro)
            });

            if (!response.ok) {
                const body = await response.json();
                if (response.status === 400) {
                    throw Object.values(body).join(". ");
                }
                throw body.errorMsg || "Error al registrar el centro. Revisa los datos.";
            }

            mostrarConfirmacion("Centro registrado con éxito. Estará disponible en el buscador una vez verificado.");

        } catch (error) {
            mostrarError(error || "Ha ocurrido un error inesperado.");
            console.error('Error durante el registro de centro:', error);
        } finally {
            btnEnviar.innerHTML = textoOriginal;
            btnEnviar.disabled  = false;
        }
    });

    function mostrarConfirmacion(mensaje) {
        document.getElementById("authHeader").classList.add("d-none");
        formCentro.innerHTML = `
            <div class="text-center py-4">
                <i class="bi bi-check-circle-fill text-success" style="font-size:3rem"></i>
                <h5 class="mt-3 mb-2">¡Solicitud enviada!</h5>
                <p class="text-muted mb-4">${mensaje}</p>
                <a href="/" class="btn btn-primary">Volver al inicio</a>
            </div>`;
    }

    function mostrarError(mensaje) {
        recuadroError.classList.remove("d-none");
        recuadroError.textContent = mensaje;
        recuadroError.scrollIntoView({ behavior: "smooth", block: "center" });
    }
});
