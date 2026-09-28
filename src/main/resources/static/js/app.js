const $ = id => document.getElementById(id);
const titles = {
  dashboard: ['Dashboard', ''], products: ['Products', 'Add, edit, search and remove items'],
  billing: ['New Bill', 'Stock is reduced only when you finalize'], bills: ['Bills', 'Every bill, newest first'],
  lowstock: ['Low Stock', 'Products below their reorder level']
};
let products = [], cart = [], billsCache = [];

// ---------- helpers ----------
async function api(url, opt = {}) {
  const res = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opt });
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch (e) {}
  if (!res.ok) throw new Error(data?.message || 'Request failed (' + res.status + ')');
  return data;
}
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Number(n).toFixed(2);
const isoDate = d => d.toLocaleDateString('en-CA');
const today = () => isoDate(new Date());
const empty = msg => '<div class="empty">' + msg + '</div>';
const hue = s => { let h = 0; for (const c of String(s)) h = (h * 31 + c.charCodeAt(0)) % 360; return h; };
const avatar = name => `<span class="avatar" style="background:hsl(${hue(name)} 65% 52%)">${esc(String(name).charAt(0).toUpperCase())}</span>`;
const isLow = p => p.stockQuantity < p.reorderLevel;
const stockBar = p => {
  const pct = Math.min(100, Math.round(p.stockQuantity / Math.max(p.reorderLevel * 2, 1) * 100));
  return `<div class="sbar"><div class="sfill ${isLow(p) ? 'low' : ''}" style="width:${pct}%"></div></div>`;
};

let toastTimer;
function toast(msg, err) {
  const t = $('toast');
  t.textContent = msg;
  t.className = (err ? 'err' : 'ok') + ' show';
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 3500);
}

// ---------- theme ----------
function setTheme(t) {
  document.documentElement.setAttribute('data-theme', t);
  $('themeBtn').textContent = t === 'dark' ? '☀️' : '🌙';
  try { localStorage.setItem('shoplite-theme', t); } catch (e) {}
}
function toggleTheme() {
  setTheme(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
}

// ---------- tabs ----------
document.querySelectorAll('#nav button').forEach(b => b.onclick = () => showTab(b.dataset.tab));

function showTab(name) {
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.id === name));
  document.querySelectorAll('#nav button').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  $('pageTitle').textContent = titles[name][0];
  $('pageSub').textContent = name === 'dashboard' ? greeting() : titles[name][1];
  ({ dashboard: loadDashboard, products: loadProducts, billing: loadPos, bills: loadBills, lowstock: loadLowStock })[name]();
  updateLowBadge();
}
function greeting() {
  const h = new Date().getHours();
  return (h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening') + '! Here is your shop at a glance.';
}
async function updateLowBadge() {
  try {
    const list = await api('/api/products/low-stock');
    $('lowCount').textContent = list.length;
    $('lowCount').classList.toggle('hidden', list.length === 0);
  } catch (e) {}
}

// ---------- dashboard ----------
async function loadDashboard() {
  try {
    const d = await api('/api/dashboard');
    $('stats').innerHTML =
      stat('g1', '📦', d.totalProducts, 'Products') +
      stat('g2', '🧾', d.totalBills, 'Total bills') +
      stat('g3', '⚠️', d.lowStockProducts, 'Low stock items') +
      stat('g4', '💰', money(d.todaysSales), "Today's sales");
  } catch (e) { toast(e.message, true); }
  loadChart(); loadDashLow(); loadDashBills(); loadSales();
}
const stat = (cls, icon, num, label) =>
  `<div class="stat ${cls}"><div class="icon">${icon}</div><div class="num">${esc(num)}</div><div class="label">${label}</div></div>`;

async function loadChart() {
  const days = [];
  for (let i = 6; i >= 0; i--) { const d = new Date(); d.setDate(d.getDate() - i); days.push(d); }
  const vals = await Promise.all(days.map(d =>
    api('/api/bills/sales?date=' + isoDate(d)).then(r => Number(r.totalSales)).catch(() => 0)));
  const max = Math.max(...vals, 1);
  $('chart').innerHTML = vals.map((v, i) =>
    `<div class="bar-col"><span class="bar-val">${v ? money(v) : ''}</span>
     <div class="bar" style="height:${Math.round(v / max * 140)}px"></div>
     <span class="bar-lbl">${days[i].toLocaleDateString(undefined, { weekday: 'short' })}</span></div>`).join('');
}

async function loadDashLow() {
  try {
    const list = await api('/api/products/low-stock');
    $('dashLow').innerHTML = list.length ? list.slice(0, 5).map(p =>
      `<div class="mini-row">${avatar(p.name)}<div class="grow"><b>${esc(p.name)}</b><small>${p.stockQuantity} left · reorder at ${p.reorderLevel}</small></div><span class="badge low">Low</span></div>`).join('')
      : '<p style="color:var(--muted)">All stocked up 🎉</p>';
  } catch (e) {}
}

async function loadDashBills() {
  try {
    const bills = await api('/api/bills');
    billsCache = bills;
    $('dashBills').innerHTML = bills.length ? bills.slice().reverse().slice(0, 5).map(b =>
      `<div class="mini-row"><span class="avatar" style="background:var(--primary)">#</span><div class="grow"><b>Bill #${b.id}</b><small>${new Date(b.createdAt).toLocaleString()}</small></div><b>${money(b.totalAmount)}</b><span class="badge ${b.status === 'FINALIZED' ? 'final' : 'draft'}">${b.status}</span></div>`).join('')
      : '<p style="color:var(--muted)">No bills yet</p>';
  } catch (e) {}
}

async function loadSales() {
  const date = $('salesDate').value || today();
  $('salesDate').value = date;
  try {
    const r = await api('/api/bills/sales?date=' + date);
    $('salesResult').innerHTML = `<b>${esc(r.date)}</b><br>Total sales: <b style="color:var(--green)">${money(r.totalSales)}</b>`;
  } catch (e) { toast(e.message, true); }
}

// ---------- products ----------
function clearSearch() { $('searchBox').value = ''; loadProducts(); }

async function loadProducts() {
  const q = $('searchBox').value.trim();
  try {
    products = q ? await api('/api/products/search?name=' + encodeURIComponent(q)) : await api('/api/products');
    if (!products.length) { $('productTable').innerHTML = empty('No products found'); return; }
    const rows = products.map(p =>
      `<tr><td>${p.productId}</td>
       <td><div class="who">${avatar(p.name)}<div><b>${esc(p.name)}</b><small>${esc(p.description || '')}</small></div></div></td>
       <td><b>${money(p.price)}</b></td>
       <td><b>${p.stockQuantity}</b>${stockBar(p)}</td>
       <td>${p.reorderLevel}</td>
       <td><span class="badge ${isLow(p) ? 'low">Low stock' : 'ok">In stock'}</span></td>
       <td><button class="btn small" onclick="openModal(${p.productId})">Edit</button>
           <button class="btn small red" onclick="deleteProduct(${p.productId})">Delete</button></td></tr>`).join('');
    $('productTable').innerHTML = `<div class="table-wrap"><table><thead><tr><th>ID</th><th>Product</th><th>Price</th><th>Stock</th><th>Reorder at</th><th>Status</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>`;
  } catch (e) { toast(e.message, true); }
}

function openModal(id) {
  const p = id ? products.find(x => x.productId === id) : null;
  $('modalTitle').textContent = p ? 'Edit product #' + id : 'Add product';
  $('pId').value = p ? p.productId : '';
  $('pName').value = p ? p.name : '';
  $('pDesc').value = p ? (p.description || '') : '';
  $('pPrice').value = p ? p.price : '';
  $('pStock').value = p ? p.stockQuantity : '';
  $('pReorder').value = p ? p.reorderLevel : '';
  $('modal').classList.remove('hidden');
}
function closeModal() { $('modal').classList.add('hidden'); }

async function saveProduct() {
  const num = id => $(id).value === '' ? null : Number($(id).value);
  const id = $('pId').value;
  const body = { name: $('pName').value.trim(), description: $('pDesc').value.trim(), price: num('pPrice'), stockQuantity: num('pStock'), reorderLevel: num('pReorder') };
  try {
    await api(id ? '/api/products/' + id : '/api/products', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
    toast(id ? 'Product updated' : 'Product added');
    closeModal(); loadProducts(); updateLowBadge();
  } catch (e) { toast(e.message, true); }
}

async function deleteProduct(id) {
  if (!confirm('Delete product #' + id + '?')) return;
  try {
    await api('/api/products/' + id, { method: 'DELETE' });
    toast('Product deleted'); loadProducts(); updateLowBadge();
  } catch (e) { toast(e.message, true); }
}

// ---------- billing (point of sale) ----------
async function loadPos() {
  try { products = await api('/api/products'); renderPos(); renderCart(); }
  catch (e) { toast(e.message, true); }
}

function renderPos() {
  const q = $('posSearch').value.trim().toLowerCase();
  const list = products.filter(p => p.name.toLowerCase().includes(q));
  $('posGrid').innerHTML = list.length ? list.map(p => {
    const out = p.stockQuantity <= 0;
    return `<button class="pcard${out ? ' out' : ''}" onclick="changeQty(${p.productId},1)"${out ? ' disabled' : ''}>
      ${avatar(p.name)}<div class="pname">${esc(p.name)}</div><div class="pprice">${money(p.price)}</div>
      <div class="pstock">${out ? 'Out of stock' : p.stockQuantity + ' in stock'}</div></button>`;
  }).join('') : empty('No products');
}

function changeQty(id, delta) {
  const p = products.find(x => x.productId === id);
  const item = cart.find(c => c.productId === id);
  const newQty = (item ? item.quantity : 0) + delta;
  if (newQty > p.stockQuantity) { toast('Only ' + p.stockQuantity + ' of ' + p.name + ' in stock', true); return; }
  if (newQty <= 0) cart = cart.filter(c => c.productId !== id);
  else if (item) item.quantity = newQty;
  else cart.push({ productId: id, name: p.name, price: p.price, quantity: newQty });
  renderCart();
}
function clearCart() { cart = []; renderCart(); }

function renderCart() {
  let total = 0;
  $('cartBody').innerHTML = cart.length ? cart.map(c => {
    total += c.price * c.quantity;
    return `<div class="cart-row">${avatar(c.name)}<div class="cname">${esc(c.name)}<br><small>${money(c.price)} each</small></div>
      <div class="qty"><button onclick="changeQty(${c.productId},-1)">−</button><b>${c.quantity}</b><button onclick="changeQty(${c.productId},1)">+</button></div>
      <b>${money(c.price * c.quantity)}</b></div>`;
  }).join('') : '<p style="color:var(--muted);text-align:center;padding:20px 0">Your bill is empty</p>';
  $('cartTotal').textContent = money(total);
}

async function checkout() {
  if (!cart.length) { toast('Add at least one item', true); return; }
  try {
    const bill = await api('/api/bills/checkout', { method: 'POST', body: JSON.stringify({ items: cart.map(c => ({ productId: c.productId, quantity: c.quantity })) }) });
    toast('Bill #' + bill.id + ' finalized');
    cart = []; loadPos(); updateLowBadge(); showReceipt(bill);
  } catch (e) { toast(e.message, true); }
}

// ---------- bills ----------
function clearBillFilter() { $('billDate').value = ''; loadBills(); }

async function loadBills() {
  const date = $('billDate').value;
  try {
    const bills = await api(date ? '/api/bills?date=' + date : '/api/bills');
    billsCache = bills;
    if (!bills.length) { $('billTable').innerHTML = empty('No bills found'); return; }
    const rows = bills.slice().reverse().map(b => {
      const fin = b.status === 'FINALIZED';
      const count = b.items.reduce((s, i) => s + i.quantity, 0);
      return `<tr><td><b>#${b.id}</b></td><td><span class="badge ${fin ? 'final' : 'draft'}">${b.status}</span></td>
        <td>${new Date(b.createdAt).toLocaleString()}</td><td>${b.items.length} items (${count} units)</td><td><b>${money(b.totalAmount)}</b></td>
        <td><button class="btn small" onclick="viewBill(${b.id})">View</button>
        ${fin ? '' : `<button class="btn small green" onclick="finalizeBill(${b.id})">Finalize</button>`}</td></tr>`;
    }).join('');
    $('billTable').innerHTML = `<div class="table-wrap"><table><thead><tr><th>Bill</th><th>Status</th><th>Created</th><th>Items</th><th>Total</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>`;
  } catch (e) { toast(e.message, true); }
}

function viewBill(id) { const b = billsCache.find(x => x.id === id); if (b) showReceipt(b); }

async function finalizeBill(id) {
  try {
    await api('/api/bills/' + id + '/finalize', { method: 'POST' });
    toast('Bill #' + id + ' finalized'); loadBills(); updateLowBadge();
  } catch (e) { toast(e.message, true); }
}

function showReceipt(b) {
  const rows = b.items.map(i => `<tr><td>${esc(i.productName)}</td><td>${i.quantity}</td><td>${money(i.unitPrice)}</td><td>${money(i.lineTotal)}</td></tr>`).join('');
  $('receiptBody').innerHTML =
    `<div class="rc-head"><h3>🛒 ShopLite</h3><p>Bill #${b.id} · ${esc(b.status)}</p><p>${new Date(b.finalizedAt || b.createdAt).toLocaleString()}</p></div>
     <table class="rc"><thead><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead><tbody>${rows || '<tr><td colspan="4">No items</td></tr>'}</tbody></table>
     <div class="rc-total"><span>Total</span><b>${money(b.totalAmount)}</b></div>`;
  $('receipt').classList.remove('hidden');
}
function closeReceipt() { $('receipt').classList.add('hidden'); }

// Prints ONLY the receipt, using a hidden iframe so the main page is never affected
function printReceipt() {
  let frame = $('printFrame');
  if (!frame) {
    frame = document.createElement('iframe');
    frame.id = 'printFrame';
    frame.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;';
    document.body.appendChild(frame);
  }
  const style = `
    body { font-family: Arial, sans-serif; padding: 20px; color: #000; }
    .rc-head { text-align: center; margin-bottom: 14px; }
    .rc-head h3 { margin: 0; }
    .rc-head p { margin: 3px 0; font-size: 13px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 6px; font-size: 13px; border-bottom: 1px solid #ccc; }
    .rc-total { display: flex; justify-content: space-between; font-size: 18px; font-weight: bold;
                padding-top: 12px; border-top: 2px dashed #000; margin-top: 8px; }`;
  const doc = frame.contentWindow.document;
  doc.open();
  doc.write('<!DOCTYPE html><html><head><title>Receipt</title><style>' + style +
            '</style></head><body>' + $('receiptBody').innerHTML + '</body></html>');
  doc.close();
  setTimeout(() => { frame.contentWindow.focus(); frame.contentWindow.print(); }, 200);
}

// ---------- low stock ----------
async function loadLowStock() {
  try {
    const list = await api('/api/products/low-stock');
    $('lowTable').innerHTML = list.length ? '<div class="low-grid">' + list.map(p =>
      `<div class="low-card"><div class="top">${avatar(p.name)}<div><b>${esc(p.name)}</b><br><small style="color:var(--muted)">${money(p.price)}</small></div><span class="badge low" style="margin-left:auto">Reorder</span></div>
       ${stockBar(p)}<div class="nums"><span>${p.stockQuantity} in stock</span><span>Reorder at ${p.reorderLevel}</span></div></div>`).join('') + '</div>'
      : empty('All products are above their reorder level 🎉');
  } catch (e) { toast(e.message, true); }
}

// close modals when clicking the dark background
['modal', 'receipt'].forEach(id => $(id).addEventListener('click', e => { if (e.target.id === id) $(id).classList.add('hidden'); }));

// ---------- start ----------
let saved = null;
try { saved = localStorage.getItem('shoplite-theme'); } catch (e) {}
setTheme(saved || 'light');
$('todayLabel').textContent = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
$('pageSub').textContent = greeting();
loadDashboard();
updateLowBadge();