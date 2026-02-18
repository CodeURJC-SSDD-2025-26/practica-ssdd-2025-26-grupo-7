function actualizarNavegacion() {
    const navContent = document.getElementById('nav-content');
    if (!navContent) return;

    const sesionActiva = localStorage.getItem('onetape_sesion') === 'true';
    const path = window.location.pathname.split("/").pop() || 'index.html';

    // Función auxiliar para marcar como activo
    const isActive = (page) => (path === page ? 'active' : '');

    if (sesionActiva) {
        navContent.innerHTML = `
            <li class="nav-item"><a class="nav-link ${isActive('index.html')}" href="index.html">Inicio</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('tournaments.html')}" href="tournaments.html">Torneos</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('matches.html')}" href="matches.html">Matches</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('teams.html')}" href="teams.html">Equipos</a></li>
            <li class="nav-item dropdown ms-lg-3">
                <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" data-bs-toggle="dropdown">
                    <div class="nav-avatar-container me-2"><img src="assets/images/avatar.png" class="nav-avatar"></div>
                    <span class="text-white fw-bold">Mi Cuenta</span>
                </a>
                <ul class="dropdown-menu dropdown-menu-end dropdown-menu-dark shadow-lg">
                    <li><a class="dropdown-item ${isActive('profile.html')}" href="profile.html">Mi Perfil</a></li>
                    <li><a class="dropdown-item ${isActive('admin.html')}" href="admin.html">Panel Admin</a></li>
                    <li><hr class="dropdown-divider border-secondary"></li>
                    <li><a class="dropdown-item text-danger" href="#" id="btn-logout">Cerrar Sesión</a></li>
                </ul>
            </li>`;
        
        document.getElementById('btn-logout')?.addEventListener('click', () => {
            localStorage.setItem('onetape_sesion', 'false');
            window.location.href = 'index.html';
        });
    } else {
        navContent.innerHTML = `
            <li class="nav-item"><a class="nav-link ${isActive('index.html')}" href="index.html">Inicio</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('tournaments.html')}" href="tournaments.html">Torneos</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('matches.html')}" href="matches.html">Matches</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('login.html')}" href="login.html">Iniciar Sesión</a></li>
            <li class="nav-item"><a class="nav-link ${isActive('register.html')}" href="register.html">Registrar</a></li>`;
    }
}

document.addEventListener('DOMContentLoaded', actualizarNavegacion);