function _construirFooterHTML(perfil) {
    const enlaceAuth = perfil
        ? `<a href="/vistas/comun/perfil.html" class="ef-footer__link">Mi perfil</a>`
        : `<a href="/vistas/auth/login.html" class="ef-footer__link">Iniciar sesión</a>`;

    return `
        <footer class="ef-footer">
            <div class="ef-footer__body">
                <div class="container">
                    <div class="row g-5">

                        <div class="col-12 col-md-4">
                            <div class="ef-footer__brand">
                                <span class="ef-footer__brand-icon" aria-hidden="true">EF</span>
                                <span class="ef-footer__brand-name">EncuentraFormación</span>
                            </div>
                            <p class="ef-footer__tagline">
                                Toda la oferta formativa de España en un solo lugar. Compara ciclos, grados, másteres y cursos, y contacta directamente con los centros.
                            </p>
                        </div>

                        <div class="col-6 col-md-2">
                            <h6 class="ef-footer__heading">Estudiantes</h6>
                            <ul class="ef-footer__list">
                                <li><a href="/" class="ef-footer__link">Buscar formación</a></li>
                                <li><a href="/vistas/auth/registro-estudiante.html" class="ef-footer__link">Crear cuenta</a></li>
                                <li>${enlaceAuth}</li>
                            </ul>
                        </div>

                        <div class="col-6 col-md-2">
                            <h6 class="ef-footer__heading">Centros</h6>
                            <ul class="ef-footer__list">
                                <li><a href="/vistas/publico/buscar-centros.html" class="ef-footer__link">Buscar centros</a></li>
                                <li><a href="/vistas/auth/registro-centro.html" class="ef-footer__link">Registra tu centro</a></li>
                                <li><a href="/vistas/auth/registro-gestor.html" class="ef-footer__link">Acceso gestores</a></li>
                            </ul>
                        </div>

                        <div class="col-12 col-md-4">
                            <h6 class="ef-footer__heading">Información</h6>
                            <ul class="ef-footer__list">
                                <li><a href="/vistas/publico/politica-privacidad.html" class="ef-footer__link">Política de privacidad</a></li>
                                <li><a href="/vistas/publico/aviso-legal.html" class="ef-footer__link">Aviso legal</a></li>
                                <li><a href="/vistas/publico/contacto.html" class="ef-footer__link">Contacto</a></li>
                            </ul>
                        </div>

                    </div>
                </div>
            </div>

            <div class="ef-footer__bottom">
                <div class="container">
                    <span>&copy; 2026 EncuentraFormación. Todos los derechos reservados. &mdash; Portal de orientación y contacto formativo. No gestiona matrículas ni tramitaciones oficiales.</span>
                </div>
            </div>
        </footer>`;
}

async function inicializarFooter() {
    const container = document.getElementById('footer-container');
    if (!container) return;

    const cached = sessionStorage.getItem('ef_perfil');
    const perfil = cached ? JSON.parse(cached) : null;

    container.innerHTML = _construirFooterHTML(perfil);
}

document.addEventListener('DOMContentLoaded', inicializarFooter);
