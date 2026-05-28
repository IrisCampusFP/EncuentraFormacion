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

    cargarGradosEstudios();

    const formularioRegistro = document.getElementById("formularioRegistro");
    const emailInput         = document.getElementById("email");
    const usernameInput      = document.getElementById("username");
    const emailError         = document.getElementById("emailError");
    const usernameError      = document.getElementById("usernameError");
    const registroError      = document.getElementById("registroError");
    const passwordInput      = document.getElementById("password");
    const password2Input     = document.getElementById("password2");
    const passwordFeedback   = document.getElementById("passwordFeedback");
    const passwordRegex      = /^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>\-_=+\[\]]).{8,}$/;

    let emailValido    = true;
    let usernameValido = true;
    let provinciaSeleccionada = null; // { id, nombre }

    // Combobox de provincias
    fetch("/provincias")
        .then(r => r.json())
        .then(provincias => inicializarComboboxProvincia(provincias))
        .catch(() => {
            document.getElementById("provinciaInput").placeholder = "Error al cargar provincias";
        });

    function inicializarComboboxProvincia(opciones) {
        const input      = document.getElementById("provinciaInput");
        const wrap       = document.getElementById("comboProvinciaWrap");
        const lista      = document.getElementById("comboProvinciaLista");
        const btnLimpiar = document.getElementById("comboProvincia-clear");

        function normalizar(s) {
            return s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");
        }

        function abrirDropdown(texto) {
            const q = normalizar(texto);
            const filtradas = q ? opciones.filter(o => normalizar(o.nombre).includes(q)) : opciones;
            lista.innerHTML = filtradas.length === 0
                ? `<li class="ef-combobox-sin-resultados">Sin resultados</li>`
                : filtradas.map(o => `<li data-id="${o.id}" data-nombre="${o.nombre}">${o.nombre}</li>`).join("");
            lista.classList.remove("d-none");
        }

        function cerrarDropdown() {
            lista.classList.add("d-none");
            if (!provinciaSeleccionada) input.value = "";
        }

        function seleccionar(id, nombre) {
            provinciaSeleccionada = { id, nombre };
            input.value = nombre;
            wrap.classList.add("seleccionado");
            btnLimpiar.classList.remove("d-none");
            lista.classList.add("d-none");
        }

        function limpiar() {
            provinciaSeleccionada = null;
            input.value = "";
            wrap.classList.remove("seleccionado");
            btnLimpiar.classList.add("d-none");
            lista.classList.add("d-none");
        }

        input.addEventListener("focus", () => abrirDropdown(input.value));
        input.addEventListener("input", () => {
            if (provinciaSeleccionada) limpiar();
            abrirDropdown(input.value);
        });
        lista.addEventListener("mousedown", e => {
            const li = e.target.closest("li[data-id]");
            if (!li) return;
            e.preventDefault();
            seleccionar(li.dataset.id, li.dataset.nombre);
        });
        btnLimpiar.addEventListener("click", () => limpiar());
        document.addEventListener("click", e => {
            if (!e.target.closest("#comboProvincia")) cerrarDropdown();
        });
    }

    // Validación en tiempo real email único
    emailInput.addEventListener("blur", async () => {
        const email = emailInput.value.trim();
        emailError.innerHTML = "";
        emailInput.classList.remove("is-invalid");
        emailValido = true;

        if (email) {
            try {
                const res = await fetch(`/check-email-unique?email=${encodeURIComponent(email)}`);
                const data = await res.json();
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

    // Validación en tiempo real username único
    usernameInput.addEventListener("blur", async () => {
        const username = usernameInput.value.trim();
        usernameError.textContent = "";
        usernameInput.classList.remove("is-invalid");
        usernameValido = true;

        if (username) {
            try {
                const res = await fetch(`/check-username-unique?username=${encodeURIComponent(username)}`);
                const data = await res.json();
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
    function mostrarFeedbackPassword() {
        const val  = passwordInput.value;
        const val2 = password2Input.value;
        const politicaInvalida = val && !passwordRegex.test(val);
        const noCoinciden      = val && val2 && val !== val2;

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

    // Envío del formulario
    formularioRegistro.addEventListener("submit", async (e) => {
        e.preventDefault();

        if (!emailValido) {
            mostrarError("Correo electrónico no válido.");
            return;
        }
        if (!usernameValido) {
            mostrarError("Nombre de usuario no válido.");
            return;
        }

        const password = passwordInput.value;
        if (!passwordRegex.test(password)) {
            mostrarError("La contraseña no cumple con los requisitos de seguridad.");
            return;
        }
        if (password !== password2Input.value) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        const datosUsuarioEstudiante = {
            datosUsuario: {
                nombre:          document.getElementById("nombre").value.trim(),
                apellidos:       document.getElementById("apellidos").value.trim(),
                username:        document.getElementById("username").value.trim(),
                email:           document.getElementById("email").value.trim(),
                password:        password,
                fechaNacimiento: document.getElementById("fechaNacimiento").value || null,
                telefono:        document.getElementById("telefono").value.trim() || null,
                dni:             document.getElementById("dni").value.trim() || null,
                sexo:            document.getElementById("sexo").value || null
            },
            gradoEstudios: document.getElementById("gradoEstudios").value,
            provinciaId:   provinciaSeleccionada ? parseInt(provinciaSeleccionada.id, 10) : null,
            localidad:     document.getElementById("localidad").value.trim() || null
        };

        registroError.classList.add("d-none");

        const btnEnviar  = document.getElementById("btnSubmitEstudiante");
        const txtOriginal = btnEnviar.innerHTML;
        btnEnviar.disabled = true;
        btnEnviar.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Registrando...';

        try {
            const response = await fetch("/registro", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(datosUsuarioEstudiante)
            });

            if (!response.ok) {
                const body = await response.json();
                if (response.status === 400) {
                    throw Object.values(body).join(". ");
                }
                throw body.errorMsg || "Error al registrar el usuario. Revisa los datos.";
            }

            await loginAutomatico(
                document.getElementById("email").value.trim(),
                passwordInput.value
            );
        } catch (error) {
            mostrarError(error || "Ha ocurrido un error inesperado.");
            console.error(error);
        } finally {
            btnEnviar.innerHTML = txtOriginal;
            btnEnviar.disabled = false;
        }
    });

    function mostrarError(mensaje) {
        registroError.classList.remove("d-none");
        registroError.textContent = mensaje;
        registroError.scrollIntoView({ behavior: "smooth", block: "center" });
    }
});

async function cargarGradosEstudios() {
    const select = document.getElementById("gradoEstudios");
    try {
        const r = await fetch("/grado-estudios");
        if (!r.ok) return;
        const grados = await r.json();
        grados.forEach(g => {
            const opt = document.createElement("option");
            opt.value = g.nombre;
            opt.textContent = g.nombre;
            select.appendChild(opt);
        });
    } catch (_) { /* no bloquea el registro */ }
}
