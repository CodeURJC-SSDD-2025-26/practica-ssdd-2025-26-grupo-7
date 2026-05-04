document.getElementById('logout-profile')?.addEventListener('click', () => {
    localStorage.setItem('onetape_sesion', 'false');
    window.location.href = 'index.html';
});

// load avatar
const profileAvatar = document.querySelector('.profile-avatar');
const savedAvatar = localStorage.getItem('onetap_avatar');
if (savedAvatar && profileAvatar) profileAvatar.src = savedAvatar;

// sync modal avatar
document.getElementById('toggle-modal')?.addEventListener('change', function () {
    if (this.checked) {
        const preview = document.getElementById('modal-avatar-preview');
        if (preview && profileAvatar) preview.src = profileAvatar.src;
    }
});

// change avatar
document.getElementById('avatar-input')?.addEventListener('change', function () {
    const file = this.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
        const url = e.target.result;
        if (profileAvatar) profileAvatar.src = url;
        const preview = document.getElementById('modal-avatar-preview');
        if (preview) preview.src = url;
        // update navbar avatar
        const navAvatar = document.getElementById('nav-avatar-img');
        if (navAvatar) navAvatar.src = url;
        localStorage.setItem('onetap_avatar', url);
    };
    reader.readAsDataURL(file);
});
