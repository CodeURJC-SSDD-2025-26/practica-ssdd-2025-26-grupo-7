document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');

    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();

            const emailInput = loginForm.querySelector('input[type="email"]');
            const emailValue = emailInput ? emailInput.value : '';

            localStorage.setItem('onetape_sesion', 'true');

            if (emailValue.includes('@admin.')) {
                localStorage.setItem('onetape_role', 'admin');
                window.location.href = 'admin.html';
            } else {
                localStorage.setItem('onetape_role', 'player');
                window.location.href = 'profile.html';
            }
        });
    }
});
