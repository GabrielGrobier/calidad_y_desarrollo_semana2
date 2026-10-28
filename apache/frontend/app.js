const API = '/api';

const state = {
  token: sessionStorage.getItem('jwt') || '',
  username: sessionStorage.getItem('username') || '',
  roles: JSON.parse(sessionStorage.getItem('roles') || '[]'),
  products: [],
  imageUrls: [],
  imageUrls: []
};

const $ = (id) => document.getElementById(id);
const money = (value) => new Intl.NumberFormat('es-CL', { style:'currency', currency:'CLP', maximumFractionDigits:0 }).format(Number(value || 0));
const dateTime = (value) => value ? new Intl.DateTimeFormat('es-CL', { dateStyle:'short', timeStyle:'short' }).format(new Date(value)) : '-';

function showMessage(message, type='ok') {
  const el = $('globalMessage');
  el.className = `alert ${type === 'error' ? 'alert-error' : 'alert-ok'}`;
  el.textContent = message;
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 4200);
}

function authHeaders(extra={}) {
  return {
    ...extra,
    ...(state.token ? { Authorization: `Bearer ${state.token}` } : {})
  };
}

async function api(path, options={}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: authHeaders(options.headers || {})
  });

  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof body === 'object' ? (body.error || JSON.stringify(body)) : (body || `HTTP ${response.status}`);
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return body;
}

function decodeJwt(token) {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const decode = (part) => {
    const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - normalized.length % 4) % 4);
    return JSON.parse(decodeURIComponent(Array.from(atob(padded)).map(c => '%' + c.charCodeAt(0).toString(16).padStart(2,'0')).join('')));
  };
  return { header: decode(parts[0]), payload: decode(parts[1]), signature: parts[2] };
}

function role(name) { return state.roles.includes(`ROLE_${name}`); }

function applyRoleVisibility() {
  document.querySelectorAll('.role-cliente').forEach(el => el.classList.toggle('hidden', !role('CLIENTE')));
  document.querySelectorAll('.role-admin').forEach(el => {
    const alsoSeller = el.classList.contains('role-vendedor');
    el.classList.toggle('hidden', !(role('ADMIN') || (alsoSeller && role('VENDEDOR'))));
  });
  document.querySelectorAll('.role-vendedor:not(.role-admin)').forEach(el => el.classList.toggle('hidden', !role('VENDEDOR')));
}

function persistSession(username, token, roles) {
  state.username = username; state.token = token; state.roles = roles;
  sessionStorage.setItem('username', username);
  sessionStorage.setItem('jwt', token);
  sessionStorage.setItem('roles', JSON.stringify(roles));
}

function clearSession() {
  state.username = ''; state.token = ''; state.roles = [];
  sessionStorage.removeItem('username'); sessionStorage.removeItem('jwt'); sessionStorage.removeItem('roles');
}

function renderSession() {
  const logged = Boolean(state.token);
  $('loginView').classList.toggle('hidden', logged);
  $('dashboardView').classList.toggle('hidden', !logged);
  $('userArea').classList.toggle('hidden', !logged);
  if (!logged) return;

  $('userLabel').textContent = `${state.username} · ${state.roles.map(r => r.replace('ROLE_','')).join(', ')}`;
  $('tokenPreview').textContent = state.token;
  applyRoleVisibility();
  renderJwt();
  loadProducts();
}

function renderJwt() {
  if (!state.token) return;
  const decoded = decodeJwt(state.token);
  if (!decoded) return;
  $('jwtHeader').textContent = JSON.stringify(decoded.header, null, 2);
  $('jwtPayload').textContent = JSON.stringify(decoded.payload, null, 2);
  $('jwtSignature').textContent = decoded.signature;
}

async function loadProducts() {
  const grid = $('productsGrid');
  state.imageUrls.forEach(url => URL.revokeObjectURL(url));
  state.imageUrls = [];
  state.imageUrls.forEach(url => URL.revokeObjectURL(url));
  state.imageUrls = [];
  grid.innerHTML = '<div class="empty">Cargando productos...</div>';
  try {
    state.products = await api('/productos');
    if (!state.products.length) {
      grid.innerHTML = '<div class="empty">No hay productos registrados.</div>';
      return;
    }
    grid.innerHTML = state.products.map(p => `
      <article class="product-card">
        <div class="product-art" id="product-image-${p.id}">${escapeHtml(p.categoria || 'PRODUCTO')}</div>
        <div class="product-body">
          <h3>${escapeHtml(p.nombre)}</h3>
          <p class="muted">${escapeHtml(p.descripcion || 'Sin descripción')}</p>
          <div class="product-meta">
            <span class="price">${money(p.precio)}</span>
            <span class="stock">Stock: ${p.stock}</span>
          </div>
          <div class="product-actions">
            ${role('CLIENTE') ? `<button class="btn btn-primary buy-btn" data-id="${p.id}" ${p.stock <= 0 ? 'disabled' : ''}>Comprar</button>` : ''}
          </div>
        </div>
      </article>`).join('');

    await Promise.all(state.products.filter(p => p.imagenKey).map(async p => {
      try {
        const response = await fetch(`${API}/productos/${p.id}/imagen`, {headers:authHeaders()});
        if (!response.ok) return;
        const url = URL.createObjectURL(await response.blob());
        state.imageUrls.push(url);
        const box = $(`product-image-${p.id}`);
        if (box) box.innerHTML = `<img src="${url}" alt="Imagen de ${escapeHtml(p.nombre)}" style="width:100%;height:100%;object-fit:cover">`;
      } catch (e) { console.warn('Imagen no disponible', e); }
    }));
    await Promise.all(state.products.filter(p => p.imagenKey).map(async p => {
      try {
        const response = await fetch(`${API}/productos/${p.id}/imagen`, {
          headers: authHeaders()
        });

        if (!response.ok) return;

        const url = URL.createObjectURL(await response.blob());

        state.imageUrls.push(url);

        const box = $(`product-image-${p.id}`);

        if (box) {
          box.innerHTML = `<img src="${url}" alt="Imagen de ${escapeHtml(p.nombre)}" style="width:100%;height:100%;object-fit:cover">`;
        }

      } catch (e) {
        console.warn('Imagen no disponible', e);
      }
    }));

    document.querySelectorAll('.buy-btn').forEach(btn => btn.addEventListener('click', () => openPurchase(Number(btn.dataset.id))));
  } catch (e) {
    grid.innerHTML = `<div class="alert alert-error">${escapeHtml(e.message)}</div>`;
    if (e.status === 401 || e.status === 403) logout(false);
  }
}

function openPurchase(id) {
  const product = state.products.find(p => p.id === id);
  if (!product) return;
  $('purchaseProductId').value = product.id;
  $('purchaseProductName').textContent = `${product.nombre} · ${money(product.precio)} · stock ${product.stock}`;
  $('purchaseQuantity').value = 1;
  $('purchaseQuantity').max = product.stock;
  $('purchaseModal').classList.remove('hidden');
}

function closePurchase() { $('purchaseModal').classList.add('hidden'); }

function purchasesTable(rows) {
  if (!rows.length) return '<div class="empty">No hay compras para mostrar.</div>';
  return `<table class="data-table"><thead><tr><th>ID</th><th>Cliente</th><th>Producto</th><th>Cantidad</th><th>Total</th><th>Fecha</th></tr></thead><tbody>${rows.map(c => `
    <tr><td>${c.id}</td><td>${escapeHtml(c.cliente?.username || '-')}</td><td>${escapeHtml(c.producto?.nombre || '-')}</td><td>${c.cantidad}</td><td>${money(c.total)}</td><td>${dateTime(c.fecha)}</td></tr>`).join('')}</tbody></table>`;
}

async function loadMyPurchases() {
  const box = $('myPurchasesList'); box.innerHTML = '<div class="empty">Cargando...</div>';
  try { box.innerHTML = purchasesTable(await api('/compras/mis-compras')); }
  catch (e) { box.innerHTML = `<div class="alert alert-error">${escapeHtml(e.message)}</div>`; }
}

async function loadAllPurchases() {
  const box = $('allPurchasesList'); box.innerHTML = '<div class="empty">Cargando...</div>';
  try { box.innerHTML = purchasesTable(await api('/compras')); }
  catch (e) { box.innerHTML = `<div class="alert alert-error">${escapeHtml(e.message)}</div>`; }
}

function escapeHtml(value='') {
  return String(value).replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));
}

function activateTab(name) {
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === name));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.toggle('active', p.id === `tab-${name}`));
  if (name === 'myPurchases') loadMyPurchases();
  if (name === 'allPurchases') loadAllPurchases();
  if (name === 'products') loadProducts();
  if (name === 'jwt') renderJwt();
}

function logout(show=true) {
  clearSession(); renderSession();
  if (show) $('loginError').classList.add('hidden');
}

$('loginForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const btn = $('loginBtn'); const err = $('loginError');
  btn.disabled = true; btn.textContent = 'Validando...'; err.classList.add('hidden');
  try {
    const user = $('username').value.trim();
    const pass = $('password').value;
    const params = new URLSearchParams({ user, encryptedPass: pass });
    const result = await api(`/login?${params.toString()}`, { method:'POST' });
    const decoded = decodeJwt(result.token);
    const roles = decoded?.payload?.roles || [];
    persistSession(result.usuario || user, result.token, roles);
    renderSession();
    activateTab('products');
  } catch (e) {
    err.textContent = `No fue posible iniciar sesión: ${e.message}`; err.classList.remove('hidden');
  } finally {
    btn.disabled = false; btn.textContent = 'Obtener JWT e ingresar';
  }
});

document.querySelectorAll('.demo-user').forEach(btn => btn.addEventListener('click', () => {
  $('username').value = btn.dataset.user; $('password').value = btn.dataset.pass;
}));

document.querySelectorAll('.tab').forEach(tab => tab.addEventListener('click', () => activateTab(tab.dataset.tab)));
$('logoutBtn').addEventListener('click', () => logout());
$('refreshProductsBtn').addEventListener('click', loadProducts);
$('closeModalBtn').addEventListener('click', closePurchase);
$('purchaseModal').addEventListener('click', e => { if (e.target === $('purchaseModal')) closePurchase(); });

$('purchaseForm').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('/compras', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify({ productoId:Number($('purchaseProductId').value), cantidad:Number($('purchaseQuantity').value) })
    });
    closePurchase(); showMessage('Compra registrada correctamente. El stock fue actualizado.'); await loadProducts();
  } catch (err) { showMessage(err.message, 'error'); }
});

$('productForm').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    const created = await api('/productos', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify({
        nombre:$('productName').value.trim(), categoria:$('productCategory').value.trim(),
        precio:Number($('productPrice').value), stock:Number($('productStock').value),
        descripcion:$('productDescription').value.trim()
      })
    });
    const image = $('productImage').files[0];
    if (image) {
      const form = new FormData(); form.append('file', image);
      await api(`/productos/${created.id}/imagen`, {method:'POST', body:form});
    }
    e.target.reset();
    showMessage('Producto guardado correctamente.');
    activateTab('products');
  } catch (err) { showMessage(err.message, 'error'); }
});

$('copyTokenBtn').addEventListener('click', async () => {
  try { await navigator.clipboard.writeText(state.token); showMessage('JWT copiado al portapapeles.'); }
  catch { showMessage('No se pudo copiar automáticamente.', 'error'); }
});

renderSession();
