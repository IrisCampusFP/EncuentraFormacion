// Valida que returnTo sea una ruta local para evitar open redirect (OWASP A01).
function _resolverReturnTo(params) {
    const raw = params.get('returnTo');
    if (!raw) return null;
    try {
        const decoded = decodeURIComponent(raw);
        if (!decoded.startsWith('/') || decoded.startsWith('//')) return null;
        return decoded;
    } catch { return null; }
}

document.addEventListener('DOMContentLoaded', () => {
    const formulario           = document.getElementById('loginForm');
    const logoutAlert           = document.getElementById('logoutAlert');
    const requiereLoginAlert    = document.getElementById('requiereLoginAlert');
    const guardarFormacionAlert = document.getElementById('guardarFormacionAlert');
    const contactarCentroAlert  = document.getElementById('contactarCentroAlert');
    const solicitarAdmisionAlert = document.getElementById('solicitarAdmisionAlert');
    const infoAlert             = document.getElementById('infoAlert');
    const infoAlertText         = document.getElementById('infoAlertText');
    let loginAlertError         = document.getElementById('loginAlertError');
    const btnLogin              = document.getElementById('btnLogin');
    const passwordInput         = document.getElementById('password');
    const usernameInput         = document.getElementById('username');

    // Toggle mostrar/ocultar contraseña
    document.getElementById('togglePassword').addEventListener('click', () => {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        document.getElementById('togglePasswordIcon').className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    // Leer parámetros una sola vez — replaceState borra la URL visible pero
    // urlParams queda en el closure y sigue disponible en el submit handler.
    const urlParams        = new URLSearchParams(window.location.search);
    const urlSinParametros = window.location.pathname;

    if (urlParams.has('logout')) {
        logoutAlert.classList.remove('d-none');
        setTimeout(() => logoutAlert.classList.add('d-none'), 4000);
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    if (urlParams.has('requiereLogin')) {
        requiereLoginAlert.classList.remove('d-none');
        setTimeout(() => requiereLoginAlert.classList.add('d-none'), 6000);
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    if (urlParams.has('guardarFormacion')) {
        guardarFormacionAlert.classList.remove('d-none');
        setTimeout(() => guardarFormacionAlert.classList.add('d-none'), 6000);
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    if (urlParams.has('contactarCentro')) {
        contactarCentroAlert.classList.remove('d-none');
        setTimeout(() => contactarCentroAlert.classList.add('d-none'), 6000);
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    if (urlParams.has('solicitarAdmision')) {
        solicitarAdmisionAlert.classList.remove('d-none');
        setTimeout(() => solicitarAdmisionAlert.classList.add('d-none'), 6000);
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    if (urlParams.has('info')) {
        infoAlertText.textContent = decodeURIComponent(urlParams.get('info'));
        infoAlert.classList.remove('d-none');
        window.history.replaceState({}, document.title, urlSinParametros);
    }

    formulario.addEventListener('submit', async (e) => {
        e.preventDefault();

        logoutAlert.classList.add('d-none');
        loginAlertError.classList.add('d-none');
        usernameInput.classList.remove('is-invalid');
        passwordInput.classList.remove('is-invalid');

        const email    = usernameInput.value;
        const password = passwordInput.value;

        const textoOriginal = btnLogin.innerHTML;
        btnLogin.disabled   = true;
        btnLogin.innerHTML  = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Accediendo...';

        try {
            const response = await fetch('/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();

            if (response.ok) {
                sessionStorage.removeItem('ef_perfil');
                const roles = data.roles || [];

                if (roles.length === 0) {
                    // Sin roles: solicitud de gestión pendiente — no usar returnTo
                    window.location.href = '/vistas/comun/estado-solicitud.html';
                    return;
                }

                // returnTo tiene prioridad sobre la redirección por rol
                const returnTo = _resolverReturnTo(urlParams);
                if (returnTo) {
                    window.location.href = returnTo;
                    return;
                }

                window.location.href = landingSegunRol(roles);
            } else {
                usernameInput.classList.add('is-invalid');
                passwordInput.classList.add('is-invalid');
                loginAlertError.textContent = data.errorMsg || 'No se ha podido iniciar sesión.';
                loginAlertError.classList.remove('d-none');
                btnLogin.disabled  = false;
                btnLogin.innerHTML = textoOriginal;
            }
        } catch (error) {
            console.error('Error en el login:', error);
            loginAlertError.textContent = 'Ha ocurrido un error inesperado. Inténtalo de nuevo.';
            loginAlertError.classList.remove('d-none');
            btnLogin.disabled  = false;
            btnLogin.innerHTML = textoOriginal;
        }
    });

    formulario.addEventListener('input', () => {
        loginAlertError.classList.add('d-none');
        usernameInput.classList.remove('is-invalid');
        passwordInput.classList.remove('is-invalid');
    });
});
