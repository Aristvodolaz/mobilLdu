document.addEventListener('DOMContentLoaded', () => {
  const uploadForm = document.getElementById('uploadForm');
  const articleInput = document.getElementById('articleInput');
  const fileInput = document.getElementById('fileInput');
  const dropzone = document.getElementById('dropzone');
  const previewContainer = document.getElementById('previewContainer');
  const imagePreview = document.getElementById('imagePreview');
  const removeFileBtn = document.getElementById('removeFileBtn');
  const submitBtn = document.getElementById('submitBtn');
  const btnSpinner = document.getElementById('btnSpinner');
  const alertBox = document.getElementById('alertBox');
  
  const galleryGrid = document.getElementById('galleryGrid');
  const photoCounter = document.getElementById('photoCounter');
  const searchInput = document.getElementById('searchInput');
  const emptyState = document.getElementById('emptyState');

  let allPhotos = [];

  // Load photos initially
  fetchPhotos();

  // Drag and drop event listeners
  ['dragenter', 'dragover'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzone.classList.add('dragover');
    }, false);
  });

  ['dragleave', 'drop'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      dropzone.classList.remove('dragover');
    }, false);
  });

  dropzone.addEventListener('drop', (e) => {
    const dt = e.dataTransfer;
    const files = dt.files;
    if (files.length > 0) {
      fileInput.files = files;
      handleFileSelected(files[0]);
    }
  });

  fileInput.addEventListener('change', (e) => {
    if (fileInput.files.length > 0) {
      handleFileSelected(fileInput.files[0]);
    }
  });

  removeFileBtn.addEventListener('click', (e) => {
    e.stopPropagation(); // Prevent triggering dropzone click
    resetFileSelection();
  });

  function handleFileSelected(file) {
    if (!file.type.startsWith('image/')) {
      showAlert('Пожалуйста, выберите изображение.', 'error');
      resetFileSelection();
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      imagePreview.src = e.target.result;
      previewContainer.style.display = 'flex';
      // Hide the dropzone instructions
      dropzone.querySelector('.dropzone-text').style.display = 'none';
    };
    reader.readAsDataURL(file);
  }

  function resetFileSelection() {
    fileInput.value = '';
    imagePreview.src = '#';
    previewContainer.style.display = 'none';
    dropzone.querySelector('.dropzone-text').style.display = 'flex';
  }

  // Show Alert Notification
  function showAlert(message, type) {
    alertBox.textContent = message;
    alertBox.className = `alert alert-${type}`;
    alertBox.style.display = 'block';
    
    // Auto hide after 5 seconds
    setTimeout(() => {
      alertBox.style.display = 'none';
    }, 5000);
  }

  // Fetch photos from API
  async function fetchPhotos() {
    try {
      const response = await fetch('/api/photos');
      if (!response.ok) throw new Error('Ошибка при загрузке данных с сервера');
      allPhotos = await response.json();
      renderGallery(allPhotos);
    } catch (error) {
      console.error(error);
      galleryGrid.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon" style="color: var(--color-error)">⚠</span>
          <p>Не удалось загрузить галерею</p>
        </div>
      `;
    }
  }

  // Render gallery
  function renderGallery(photos) {
    // Clear current items except empty state
    galleryGrid.querySelectorAll('.photo-card').forEach(card => card.remove());
    
    if (photos.length === 0) {
      emptyState.style.display = 'flex';
      photoCounter.textContent = '0 шт.';
      return;
    }

    emptyState.style.display = 'none';
    photoCounter.textContent = `${photos.length} шт.`;

    photos.forEach(photo => {
      const card = document.createElement('div');
      card.className = 'photo-card';
      
      const formattedDate = new Date(photo.UploadedAt).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });

      card.innerHTML = `
        <div class="photo-wrapper">
          <img src="${photo.PhotoPath}" alt="Артикул: ${photo.Article}" loading="lazy">
        </div>
        <div class="photo-info">
          <span class="photo-article">${escapeHtml(photo.Article)}</span>
          <span class="photo-date">${formattedDate}</span>
        </div>
      `;
      galleryGrid.appendChild(card);
    });
  }

  // Live filter search
  searchInput.addEventListener('input', () => {
    const term = searchInput.value.toLowerCase().trim();
    if (!term) {
      renderGallery(allPhotos);
      return;
    }

    const filtered = allPhotos.filter(photo => 
      photo.Article.toLowerCase().includes(term)
    );
    renderGallery(filtered);
  });

  // Form Submit Handler
  uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const article = articleInput.value.trim();
    const file = fileInput.files[0];

    if (!article || !file) {
      showAlert('Заполните артикул и выберите фото.', 'error');
      return;
    }

    // Set Loading State
    submitBtn.disabled = true;
    btnSpinner.style.display = 'inline-block';
    submitBtn.querySelector('.btn-text').textContent = 'Отправка...';

    const formData = new FormData();
    formData.append('article', article);
    formData.append('photo', file);

    try {
      const response = await fetch('/api/photos', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();

      if (response.ok) {
        showAlert('Фотография успешно добавлена в базу данных!', 'success');
        uploadForm.reset();
        resetFileSelection();
        fetchPhotos(); // Refresh gallery
      } else {
        showAlert(result.error || 'Ошибка при загрузке.', 'error');
      }
    } catch (error) {
      console.error(error);
      showAlert('Ошибка сети при отправке данных.', 'error');
    } finally {
      // Reset Button State
      submitBtn.disabled = false;
      btnSpinner.style.display = 'none';
      submitBtn.querySelector('.btn-text').textContent = 'Отправить в базу данных';
    }
  });

  // Helper to escape HTML and prevent XSS injection
  function escapeHtml(text) {
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
  }
});
