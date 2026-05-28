document.addEventListener('DOMContentLoaded', () => {
    // Toggle mostrar/ocultar contraseña
    document.getElementById('togglePassword').addEventListener('click', () => {
        const input = document.getElementById('password');
        const icon  = document.getElementById('togglePasswordIcon');
        const show  = input.type === 'password';
        input.type  = show ? 'text' : 'password';
        icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
    });
    document.getElementById('togglePassword2').addEventListener('click', () => {
        const input = document.getElementById('password2');
        const icon  = document.getElementById('togglePassword2Icon');
        const show  = input.type === 'password';
        input.type  = show ? 'text' : 'password';
        icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    // Variables registro usuario
    const formulario = document.getElementById("formularioRegistroGestor");
    const emailInput = document.getElementById("email");
    const usernameInput = document.getElementById("username");
    let emailValido = true;
    let usernameValido = true;
    const emailError = document.getElementById("emailError");
    const usernameError = document.getElementById("usernameError");
    const registroError = document.getElementById("registroError");

    // Variables búsqueda centro
    const btnComprobarCodigo = document.getElementById("btnComprobarCodigo");
    const codigoBusquedaInput = document.getElementById("codigoBusqueda");
    const btnComprobarCodigoText = document.getElementById("btnComprobarCodigoText");
    const btnComprobarCodigoSpinner = document.getElementById("btnComprobarCodigoSpinner");
    const mensajeCodigoResult = document.getElementById("mensajeCodigoResult");

    // Contenedores secciones
    const seccionDatosCentro = document.getElementById("seccionDatosCentro");
    const seccionTitularidad = document.getElementById("seccionTitularidad");
    const btnEnviar = document.getElementById("btnSubmitGestor");

    // Combobox de provincias
    let comboboxProvincia = null;

    fetch("/provincias")
        .then(r => r.json())
        .then(provincias => {
            comboboxProvincia = crearCombobox(provincias, 'comboProvincia', 'provinciaCentro', 'comboProvinciaWrap', 'comboProvinciaLista', 'comboProvincia-clear');
        })
        .catch(() => {
            document.getElementById("provinciaCentro").placeholder = "Error al cargar provincias";
        });

    // Campos registro centro
    const idCentroExistente = document.getElementById("idCentroExistente");
    const inputsCentro = [
        document.getElementById("nombreCentro"),
        document.getElementById("direccionCentro"),
        document.getElementById("localidadCentro"),
        document.getElementById("provinciaCentro"),
        document.getElementById("telefonoCentro"),
        document.getElementById("emailCentro"),
        document.getElementById("webCentro"),
        document.getElementById("tipoCentro")
    ];

    // Validación en tiempo real email y username únicos
    emailInput.addEventListener("blur", async () => {
        const email = emailInput.value.trim();
        emailError.innerHTML = "";
        emailInput.classList.remove("is-invalid");
        emailValido = true;

        if (email) {
            try {
                const r = await fetch(`/check-email-unique?email=${encodeURIComponent(email)}`);
                const data = await r.json();
                if (data.existe) {
                    emailError.innerHTML = 'Este correo ya está registrado. <a href="/vistas/auth/login.html" class="text-reset text-decoration-underline fw-bold">Inicia sesión</a>';
                    emailInput.classList.add("is-invalid");
                    emailValido = false;
                }
            } catch (error) {
                console.error('Error al validar email', error);
            }
        }
    });

    usernameInput.addEventListener("blur", async () => {
        const username = usernameInput.value.trim();
        usernameError.textContent = "";
        usernameInput.classList.remove("is-invalid");
        usernameValido = true;

        if (username) {
            try {
                const r = await fetch(`/check-username-unique?username=${encodeURIComponent(username)}`);
                const data = await r.json();
                if (data.existe) {
                    usernameError.textContent = "Este nombre de usuario ya está en uso.";
                    usernameInput.classList.add("is-invalid");
                    usernameValido = false;
                }
            } catch (error) {
                console.error('Error al validar username', error);
            }
        }
    });

    // Validación en tiempo real contraseña
    const passwordInput = document.getElementById("password");
    const password2Input = document.getElementById("password2");
    const passwordFeedback = document.getElementById("passwordFeedback");
    const passwordRegex = /^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>\-_=+\[\]]).{8,}$/;

    function mostrarFeedbackPassword() {
        const val = passwordInput.value;
        const val2 = password2Input.value;
        const politicaInvalida = val && !passwordRegex.test(val);
        const noCoinciden = val && val2 && val !== val2;

        passwordInput.classList.toggle("is-invalid", !!politicaInvalida);
        password2Input.classList.toggle("is-invalid", !politicaInvalida && !!noCoinciden);

        if (politicaInvalida) {
            passwordFeedback.textContent = "Mínimo 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%...).";
            passwordFeedback.style.display = "block";
        } else if (noCoinciden) {
            passwordFeedback.textContent = "Las contraseñas no coinciden.";
            passwordFeedback.style.display = "block";
        } else {
            passwordFeedback.style.display = "none";
        }
    }

    passwordInput.addEventListener("blur", mostrarFeedbackPassword);
    password2Input.addEventListener("blur", mostrarFeedbackPassword);

    // COMPROBACIÓN CÓDIGO CENTRO
    btnComprobarCodigo.addEventListener("click", async () => {
        const codigo = codigoBusquedaInput.value.trim();
        if (!codigo) {
            mostrarMensajeCodigo("Por favor, introduce el código de tu centro.", "alert-danger");
            return;
        }
        if (!/^\d{8}$/.test(codigo)) {
            mostrarMensajeCodigo("El código del centro debe tener exactamente 8 dígitos numéricos.", "alert-danger");
            return;
        }

        // Mostramos que está cargando en el botón usando el spinner de Bootstrap
        btnComprobarCodigo.disabled = true;
        btnComprobarCodigoText.classList.add("d-none");
        btnComprobarCodigoSpinner.classList.remove("d-none");
        mensajeCodigoResult.classList.add("d-none");

        try {
            const response = await fetch(`/centros/existe?codigo=${encodeURIComponent(codigo)}`);
            if (response.status === 404) {
                prepararCentroNuevo();
            } else if (!response.ok) {
                const errData = await response.json();
                throw errData.errorMsg || "Error al comprobar el código.";
            } else {
                const centro = await response.json();
                if (centro) {
                    prepararCentroExistente(centro);
                }
            }
        } catch (error) {
            mostrarMensajeCodigo(error, "alert-danger");
        } finally {
            // Quitar estado cargando (retiramos el spinner y habilitamos el botón de nuevo)
            btnComprobarCodigo.disabled = false;
            btnComprobarCodigoText.classList.remove("d-none");
            btnComprobarCodigoSpinner.classList.add("d-none");
        }
    });

    function mostrarMensajeCodigo(mensaje, claseAlert) {
        mensajeCodigoResult.innerHTML = mensaje;
        mensajeCodigoResult.className = `alert ${claseAlert} mb-4`;
        mensajeCodigoResult.classList.remove("d-none");
    }

    function prepararCentroExistente(centro) {
        const estadoCentro = centro.verificado ? "" : ' <span class="badge bg-warning text-dark">Pendiente de validación</span>';
        mostrarMensajeCodigo(`Centro encontrado: <strong>${centro.nombreComercial}</strong>${estadoCentro}. Sube una prueba de vinculación con el centro y envía el formulario.`, "alert-success");

        idCentroExistente.value = centro.id;

        // Ocultamos la sección de rellenar datos del centro y hacemos sus campos no requeridos
        seccionDatosCentro.classList.add("d-none");
        inputsCentro.forEach(input => input.removeAttribute("required"));

        // Mostramos la sección de la prueba de titularidad y el botón de enviar formulario
        seccionTitularidad.classList.remove("d-none");
        btnEnviar.classList.remove("d-none");
        btnEnviar.textContent = "Registrarme y solicitar gestión del centro";
    }

    function prepararCentroNuevo() {
        mostrarMensajeCodigo(`No hay ningún centro registrado con el código <strong>${codigoBusquedaInput.value}</strong>. Por favor, rellena los siguientes datos para darlo de alta.`, "alert-warning");

        idCentroExistente.value = "";

        // Mostramos los inputs para registrar un nuevo centro
        seccionDatosCentro.classList.remove("d-none");
        // Establecemos los campos obligatorios como required
        document.getElementById("nombreCentro").required = true;
        document.getElementById("direccionCentro").required = true;
        document.getElementById("localidadCentro").required = true;
        comboboxProvincia?.limpiar();
        document.getElementById("tipoCentro").required = true;

        // Mostramos la sección de la prueba de titularidad y el botón de enviar formulario
        seccionTitularidad.classList.remove("d-none");
        btnEnviar.classList.remove("d-none");
        btnEnviar.textContent = "Registrarme, registrar centro y enviar solicitud";
    }

    // ENVÍO DEL FORMULARIO
    formulario.addEventListener("submit", async (e) => {
        e.preventDefault();

        // Comprobación email y username válidos
        if (!emailValido) {
            mostrarError("Correo electrónico no válido.");
            return;
        }
        if (!usernameValido) {
            mostrarError("Nombre de usuario no válido.");
            return;
        }

        // Comprobar que las contraseñas coinciden y cumplen política
        const password = passwordInput.value;
        if (!passwordRegex.test(password)) {
            mostrarError("La contraseña no cumple con los requisitos de seguridad.");
            return;
        }

        if (password2Input && password !== password2Input.value) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        // Comprobar que el código fue comprobado antes de enviar
        if (seccionTitularidad && seccionTitularidad.classList.contains("d-none")) {
            mostrarError("Debes comprobar el código de tu centro antes de enviar el formulario.");
            return;
        }

        // Comprobar que se ha seleccionado provincia (centro nuevo)
        if (!idCentroExistente.value && seccionDatosCentro && !seccionDatosCentro.classList.contains("d-none")) {
            if (!comboboxProvincia?.valor) {
                mostrarError("Debes seleccionar una provincia de la lista.");
                return;
            }
        }

        // Comprobar que se ha adjuntado una prueba de titularidad
        const titularidadInput = document.getElementById("titularidadCentro");
        if (seccionTitularidad && !seccionTitularidad.classList.contains("d-none")) {
            if (!titularidadInput || !titularidadInput.files || titularidadInput.files.length === 0) {
                mostrarError("Debes adjuntar una prueba de titularidad o vinculación con el centro.");
                return;
            }
        }

        const txtBotonEnviar = btnEnviar.innerHTML;
        btnEnviar.disabled = true;
        btnEnviar.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Registrando...';

        // Estructura según el DTO: RegistroGestorRequestDTO
        const dto = {
            datosUsuario: {
                nombre: document.getElementById("nombre").value.trim(),
                apellidos: document.getElementById("apellidos").value.trim(),
                username: document.getElementById("username").value.trim(),
                email: document.getElementById("email").value.trim(),
                password: password,
                fechaNacimiento: document.getElementById("fechaNacimiento").value || null,
                telefono: document.getElementById("telefono").value.trim() || null,
                dni: document.getElementById("dni").value.trim() || null,
                sexo: document.getElementById("sexo").value || null
            },
            idCentroExistente: idCentroExistente.value ? parseInt(idCentroExistente.value) : null,
            datosCentroNuevo: null
        };

        // Si el centro no existe, enviamos los datos del nuevo centro
        if (!dto.idCentroExistente) {
            dto.datosCentroNuevo = {
                codigo: codigoBusquedaInput.value.trim(),
                nombreComercial: document.getElementById("nombreCentro").value.trim(),
                direccion: document.getElementById("direccionCentro").value.trim(),
                localidad: document.getElementById("localidadCentro").value.trim(),
                provincia: comboboxProvincia?.valor,
                telefono: document.getElementById("telefonoCentro").value.trim() || null,
                email: document.getElementById("emailCentro").value.trim() || null,
                paginaWeb: document.getElementById("webCentro").value.trim() || null,
                tipo: document.getElementById("tipoCentro").value
            };
        }

        /* NOTA: Como tenemos que enviar un archivo (la prueba de titularidad),
         * el cuerpo de la petición (body) no puede ser JSON, tiene que ser FormData */

        const formData = new FormData();
        formData.append("datosUsuarioCentro", new Blob([JSON.stringify(dto)], { type: "application/json" }));

        const pruebaTitularidad = document.getElementById("titularidadCentro").files[0];
        if (pruebaTitularidad) {
            if (!['image/jpeg', 'image/png', 'application/pdf'].includes(pruebaTitularidad.type)) {
                mostrarError("El archivo debe ser una imagen (JPG, PNG) o un documento PDF.");
                btnEnviar.disabled = false;
                btnEnviar.innerHTML = txtBotonEnviar;
                return;
            }
            if (pruebaTitularidad.size > 10 * 1024 * 1024) {
                mostrarError("El archivo no puede superar los 10 MB.");
                btnEnviar.disabled = false;
                btnEnviar.innerHTML = txtBotonEnviar;
                return;
            }
            formData.append("pruebaTitularidad", pruebaTitularidad);
        }

        // Limpiar errores previos
        registroError.classList.add("d-none");

        try {
            const response = await fetch("/registro/gestor", {
                method: "POST",
                // Como hay que enviar un archivo, se utiliza FormData
                body: formData
            });

            if (!response.ok) {
                const body = await response.json();
                // Si es un error de validación (400 con un objeto de campos)
                if (response.status === 400) {
                    const mensajes = Object.values(body).join(". ");
                    throw mensajes;
                }
                throw body.errorMsg || "Error al completar el registro. Revisa los datos.";
            }
            const data = await response.json();

            await loginAutomatico(
                document.getElementById("email").value.trim(),
                passwordInput.value
            );
        } catch (error) {
            mostrarError(error || "Ha ocurrido un error inesperado.");
            console.error('Error durante el registro', error);
        } finally {
            // Se restablece el texto del botón de enviar tras el spinner
            btnEnviar.disabled = false;
            btnEnviar.innerHTML = txtBotonEnviar;
        }
    });

    function mostrarError(mensaje) {
        registroError.classList.remove("d-none");
        registroError.textContent = mensaje;
        registroError.scrollIntoView({ behavior: "smooth", block: "center" });
    }
});

