// Gestión del modo claro / oscuro.
// Se guarda la preferencia del usuario en localStorage y se aplica
// antes de que el navegador pinte el contenido, para evitar el parpadeo.

(() => {
    const CLAVE = 'ef-tema';

    function getTemaActual() {
        const guardado = localStorage.getItem(CLAVE);
        if (guardado === 'dark' || guardado === 'light') return guardado;
        // Si no hay preferencia guardada, usa la del sistema operativo
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function aplicarTema(tema) {
        document.documentElement.setAttribute('data-bs-theme', tema);

        const icono = document.getElementById('themeIcon');
        if (icono) {
            // Sol = modo oscuro activo (pulsar cambia a claro)
            // Luna = modo claro activo (pulsar cambia a oscuro)
            icono.className = tema === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
        }

        const btn = document.getElementById('themeToggle');
        if (btn) {
            btn.title = tema === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
        }
    }

    // Aplicar el tema inmediatamente para evitar el flash de color incorrecto
    aplicarTema(getTemaActual());

    document.addEventListener('DOMContentLoaded', () => {
        // Sincronizar el icono una vez que el DOM esté listo
        aplicarTema(getTemaActual());
    });

    // Event delegation: funciona aunque el botón se inyecte dinámicamente (navbar.js)
    document.addEventListener('click', e => {
        if (!e.target.closest('#themeToggle')) return;
        const actual = document.documentElement.getAttribute('data-bs-theme') || 'light';
        const nuevo = actual === 'dark' ? 'light' : 'dark';
        localStorage.setItem(CLAVE, nuevo);
        aplicarTema(nuevo);
    });

    // Si el usuario cambia la preferencia del sistema mientras tiene la web abierta,
    // actualizar solo si no ha elegido manualmente un tema
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (!localStorage.getItem(CLAVE)) {
            aplicarTema(e.matches ? 'dark' : 'light');
        }
    });
})();
