// Cierre de sesión para las vistas con navbar propio (admin, gestor, estudiante).
// Las vistas públicas usan cerrarSesion() de navbar.js en su lugar.

async function logout() {
    try {
        await fetch('/auth/logout', { method: 'POST', credentials: 'include' });
    } finally {
        window.location.href = '/vistas/auth/login.html';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('a[href="/logout"]').forEach(btn => {
        btn.addEventListener('click', e => {
            e.preventDefault();
            logout();
        });
    });
});
