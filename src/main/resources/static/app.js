"use strict";

// 与页面同源的相对地址；Spring Boot 设置 context-path 或放进 Docker 后无需改动。
const apiBase = new URL("api/", document.baseURI);
const $ = (id) => document.getElementById(id);
const money = (value) => new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(value) || 0);
const dateText = (value) => value ? String(value).replace("T", " ").slice(0, 16) : "—";
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const state = {
  view: "overview",
  products: { page: 0, size: 10, sort: "idAsc", includeInactive: false, data: null },
  orders: { page: 0, size: 10, sort: "timeDesc", data: null },
  availableProducts: [],
  detailOrderId: null,
  toastTimer: null
};

async function api(path, options = {}) {
  const headers = { Accept: "application/json", ...options.headers };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try { response = await fetch(new URL(path, apiBase), { ...options, headers }); }
  catch {
    setConnection(false);
    throw new Error("无法连接后端服务，请确认应用已经启动");
  }
  setConnection(true);
  const raw = await response.text();
  let body;
  try { body = raw ? JSON.parse(raw) : null; } catch { body = null; }
  if (!response.ok) {
    throw new Error(body?.error || `请求失败（HTTP ${response.status}）`);
  }
  return body;
}

function setConnection(connected) {
  const indicator = $("connectionStatus");
  indicator.classList.toggle("is-error", !connected);
  indicator.innerHTML = `<i></i>${connected ? "服务已连接" : "连接失败"}`;
}

function notify(message, error = false) {
  const toast = $("toast");
  toast.textContent = message;
  toast.classList.toggle("is-error", error);
  toast.hidden = false;
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => { toast.hidden = true; }, 4000);
}

function showError(error) { notify(error instanceof Error ? error.message : String(error), true); }
function setFormError(id, message) { const element = $(id); element.textContent = message || ""; element.hidden = !message; }

function navigate(view) {
  if (location.hash.slice(1) === view) showView(view);
  else location.hash = view;
}

function showView(view) {
  if (!["overview", "products", "orders"].includes(view)) view = "overview";
  state.view = view;
  const names = { overview: "总览", products: "商品管理", orders: "订单管理" };
  $("currentViewName").textContent = names[view];
  for (const name of Object.keys(names)) $(name + "View").hidden = name !== view;
  document.querySelectorAll(".nav-item").forEach((button) => {
    const active = button.dataset.view === view;
    button.classList.toggle("is-active", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  if (view === "overview") loadOverview();
  if (view === "products") loadProducts();
  if (view === "orders") loadOrders();
}

async function loadOverview() {
  $("recentOrders").innerHTML = '<p class="loading-copy">正在读取订单…</p>';
  try {
    const [products, orders] = await Promise.all([
      api("products?page=0&size=1&sort=idAsc"),
      api("orders?page=0&size=5&sort=timeDesc")
    ]);
    $("statProducts").textContent = products.total.toLocaleString("zh-CN");
    $("statOrders").textContent = orders.total.toLocaleString("zh-CN");
    $("statLatest").textContent = orders.items.length ? money(orders.items[0].totalPrice) : "—";
    $("recentOrders").innerHTML = orders.items.length ? orders.items.map((order) => `
      <div class="recent-item"><span class="recent-badge">▤</span><span class="recent-text"><strong>订单 #${escapeHtml(order.id)}</strong><small>${escapeHtml(dateText(order.orderedAt))}</small></span><span class="recent-price">${money(order.totalPrice)}</span></div>
    `).join("") : '<p class="empty-copy">还没有订单。创建第一笔采购记录吧。</p>';
  } catch (error) {
    $("statProducts").textContent = "—";
    $("statOrders").textContent = "—";
    $("statLatest").textContent = "—";
    $("recentOrders").innerHTML = '<p class="empty-copy">暂时无法读取数据，请确认后端和数据库已启动。</p>';
    showError(error);
  }
}

function pageInfo(data) {
  if (!data) return "—";
  if (data.total === 0) return "共 0 条记录";
  return `共 ${data.total} 条 · 第 ${data.page + 1} / ${data.totalPages} 页`;
}

async function loadProducts() {
  const query = state.products;
  $("productRows").innerHTML = '<tr><td colspan="5" class="loading-copy">正在读取商品…</td></tr>';
  try {
    const params = new URLSearchParams({ page: String(query.page), size: String(query.size), sort: query.sort, includeInactive: String(query.includeInactive) });
    const data = await api("products?" + params);
    if (query.page > 0 && query.page >= data.totalPages) { query.page = Math.max(0, data.totalPages - 1); return loadProducts(); }
    query.data = data;
    $("productCount").textContent = data.total;
    $("productPageInfo").textContent = pageInfo(data);
    $("productPrev").disabled = data.page === 0;
    $("productNext").disabled = data.page + 1 >= data.totalPages;
    $("productEmpty").hidden = data.items.length > 0;
    $("productRows").innerHTML = data.items.map((product) => `
      <tr><td><span class="id-chip">#${escapeHtml(product.id)}</span></td><td><strong>${escapeHtml(product.name)}</strong></td><td>${money(product.price)}</td><td><span class="status-pill ${product.active ? "" : "inactive"}">${product.active ? "在售" : "已停用"}</span></td><td><div class="table-actions">${product.active ? `<button class="table-action" data-action="edit-product" data-id="${escapeHtml(product.id)}">编辑</button><button class="table-action danger" data-action="deactivate-product" data-id="${escapeHtml(product.id)}">停用</button>` : "—"}</div></td></tr>
    `).join("");
  } catch (error) {
    $("productRows").innerHTML = '<tr><td colspan="5" class="loading-copy">加载失败，请检查连接后重试。</td></tr>';
    showError(error);
  }
}

async function loadOrders() {
  const query = state.orders;
  $("orderRows").innerHTML = '<tr><td colspan="4" class="loading-copy">正在读取订单…</td></tr>';
  try {
    const params = new URLSearchParams({ page: String(query.page), size: String(query.size), sort: query.sort });
    const data = await api("orders?" + params);
    if (query.page > 0 && query.page >= data.totalPages) { query.page = Math.max(0, data.totalPages - 1); return loadOrders(); }
    query.data = data;
    $("orderCount").textContent = data.total;
    $("orderPageInfo").textContent = pageInfo(data);
    $("orderPrev").disabled = data.page === 0;
    $("orderNext").disabled = data.page + 1 >= data.totalPages;
    $("orderEmpty").hidden = data.items.length > 0;
    $("orderRows").innerHTML = data.items.map((order) => `
      <tr><td><span class="id-chip">#${escapeHtml(order.id)}</span></td><td>${escapeHtml(dateText(order.orderedAt))}</td><td><strong>${money(order.totalPrice)}</strong></td><td><div class="table-actions"><button class="table-action" data-action="detail-order" data-id="${escapeHtml(order.id)}">查看详情</button><button class="table-action" data-action="edit-order" data-id="${escapeHtml(order.id)}">修改</button><button class="table-action danger" data-action="delete-order" data-id="${escapeHtml(order.id)}">删除</button></div></td></tr>
    `).join("");
  } catch (error) {
    $("orderRows").innerHTML = '<tr><td colspan="4" class="loading-copy">加载失败，请检查连接后重试。</td></tr>';
    showError(error);
  }
}

async function openProductDialog(id = null) {
  $("productForm").reset();
  setFormError("productFormError", "");
  $("productId").value = id ?? "";
  $("productDialogTitle").textContent = id ? "修改商品" : "新增商品";
  $("productSubmit").textContent = id ? "保存修改" : "保存商品";
  if (id) {
    try {
      const product = await api("products/" + id);
      $("productName").value = product.name;
      $("productPrice").value = product.price;
    } catch (error) { showError(error); return; }
  }
  $("productDialog").showModal();
  $("productName").focus();
}

async function saveProduct(event) {
  event.preventDefault();
  const name = $("productName").value.trim();
  const priceText = $("productPrice").value.trim();
  if (!name) return setFormError("productFormError", "商品名称不能为空");
  if (!/^(?:\d+)(?:\.\d{1,2})?$/.test(priceText) || Number(priceText) > 9999999999.99)
    return setFormError("productFormError", "价格须为 0 至 9999999999.99，最多两位小数");
  setFormError("productFormError", "");
  const id = $("productId").value;
  const submit = $("productSubmit");
  submit.disabled = true;
  try {
    await api(id ? "products/" + id : "products", { method: id ? "PUT" : "POST", body: JSON.stringify({ name, price: Number(priceText) }) });
    $("productDialog").close();
    notify(id ? "商品修改成功" : "商品新增成功");
    if (state.view === "products") loadProducts();
    else if (state.view === "overview") loadOverview();
  } catch (error) { setFormError("productFormError", error.message); }
  finally { submit.disabled = false; }
}

async function fetchActiveProducts() {
  const first = await api("products?page=0&size=100&sort=idAsc");
  const items = [...first.items];
  for (let page = 1; page < first.totalPages; page++) {
    const next = await api(`products?page=${page}&size=100&sort=idAsc`);
    items.push(...next.items);
  }
  return items;
}

function addOrderLine(productId = "", quantity = 1) {
  const row = document.createElement("div");
  row.className = "line-item";
  row.innerHTML = `<select aria-label="选择商品" required><option value="">选择商品</option>${state.availableProducts.map((product) => `<option value="${escapeHtml(product.id)}">${escapeHtml(product.name)} · ${money(product.price)}</option>`).join("")}</select><input type="number" aria-label="数量" min="1" max="10000" step="1" value="${escapeHtml(quantity)}" required><button type="button" class="remove-line" aria-label="移除商品">×</button>`;
  row.querySelector("select").value = String(productId);
  $("orderItemRows").append(row);
  updateOrderPreview();
}

function updateOrderPreview() {
  let cents = 0;
  document.querySelectorAll("#orderItemRows .line-item").forEach((row) => {
    const product = state.availableProducts.find((item) => String(item.id) === row.querySelector("select").value);
    const quantity = Number(row.querySelector("input").value);
    if (product && Number.isInteger(quantity) && quantity > 0) cents += Math.round(Number(product.price) * 100) * quantity;
  });
  $("orderTotalPreview").textContent = money(cents / 100);
}

async function openOrderDialog(id = null) {
  setFormError("orderFormError", "");
  $("orderId").value = id ?? "";
  $("orderDialogTitle").textContent = id ? "修改订单" : "创建订单";
  $("orderSubmit").textContent = id ? "保存修改" : "创建订单";
  $("orderItemRows").replaceChildren();
  try {
    const [products, order] = await Promise.all([fetchActiveProducts(), id ? api("orders/" + id) : Promise.resolve(null)]);
    state.availableProducts = products;
    if (!products.length) { notify("暂无在售商品，请先新增商品", true); navigate("products"); return; }
    if (order) {
      let unavailable = false;
      order.items.forEach((item) => {
        const active = products.some((product) => product.id === item.productId);
        addOrderLine(active ? item.productId : "", item.quantity);
        if (!active) unavailable = true;
      });
      if (unavailable) setFormError("orderFormError", "原订单含已停用商品，请重新选择在售商品后保存");
    } else addOrderLine();
    $("orderDialog").showModal();
  } catch (error) { showError(error); }
}

function readOrderItems() {
  const rows = [...document.querySelectorAll("#orderItemRows .line-item")];
  if (!rows.length) throw new Error("订单至少需要一个商品");
  const items = {};
  for (const row of rows) {
    const id = row.querySelector("select").value;
    const quantity = Number(row.querySelector("input").value);
    if (!id) throw new Error("请选择商品");
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 10000) throw new Error("商品数量必须在 1 至 10000 之间");
    if (Object.hasOwn(items, id)) throw new Error("同一商品请只添加一次");
    items[id] = quantity;
  }
  return items;
}

async function saveOrder(event) {
  event.preventDefault();
  let items;
  try { items = readOrderItems(); }
  catch (error) { setFormError("orderFormError", error.message); return; }
  setFormError("orderFormError", "");
  const id = $("orderId").value;
  const submit = $("orderSubmit");
  submit.disabled = true;
  try {
    await api(id ? "orders/" + id : "orders", { method: id ? "PUT" : "POST", body: JSON.stringify({ items }) });
    $("orderDialog").close();
    notify(id ? "订单修改成功" : "订单创建成功");
    if (state.view === "orders") loadOrders();
    else if (state.view === "overview") loadOverview();
  } catch (error) { setFormError("orderFormError", error.message); }
  finally { submit.disabled = false; }
}

async function openOrderDetail(id) {
  try {
    const order = await api("orders/" + id);
    state.detailOrderId = id;
    $("detailTitle").textContent = `订单 #${order.id}`;
    $("detailContent").innerHTML = `
      <div class="detail-meta"><div class="detail-meta-item"><span>订单编号</span><strong>#${escapeHtml(order.id)}</strong></div><div class="detail-meta-item"><span>下单时间</span><strong>${escapeHtml(dateText(order.orderedAt))}</strong></div></div>
      <div class="detail-section-label">商品明细 · ${order.items.length} 件商品</div>
      ${order.items.map((item) => `<div class="detail-line"><span class="detail-line-symbol">▦</span><span class="detail-line-body"><strong>${escapeHtml(item.productName)}</strong><small>编号 #${escapeHtml(item.productId)} · ${money(item.unitPrice)} × ${escapeHtml(item.quantity)}</small></span><span class="detail-line-price">${money(Number(item.unitPrice) * item.quantity)}</span></div>`).join("")}
      <div class="detail-total"><span>订单总价</span><strong>${money(order.totalPrice)}</strong></div>`;
    $("detailDialog").showModal();
  } catch (error) { showError(error); }
}

function confirmAction(title, message) {
  return new Promise((resolve) => {
    const dialog = $("confirmDialog");
    $("confirmTitle").textContent = title;
    $("confirmMessage").textContent = message;
    const finish = (accepted) => { dialog.close(); resolve(accepted); };
    $("confirmAccept").onclick = () => finish(true);
    $("confirmCancel").onclick = () => finish(false);
    dialog.oncancel = (event) => { event.preventDefault(); finish(false); };
    dialog.showModal();
  });
}

async function handleProductAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const id = button.dataset.id;
  if (button.dataset.action === "edit-product") return openProductDialog(id);
  if (button.dataset.action === "deactivate-product") {
    if (!await confirmAction("停用这个商品？", "停用后不能用于新订单，已有订单及其历史价格仍会保留。")) return;
    try { await api("products/" + id, { method: "DELETE" }); notify("商品停用成功"); loadProducts(); }
    catch (error) { showError(error); }
  }
}

async function handleOrderAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const id = button.dataset.id;
  if (button.dataset.action === "detail-order") return openOrderDetail(id);
  if (button.dataset.action === "edit-order") return openOrderDialog(id);
  if (button.dataset.action === "delete-order") {
    if (!await confirmAction("删除这个订单？", "订单和商品明细将被永久删除，此操作不能撤销。")) return;
    try { await api("orders/" + id, { method: "DELETE" }); notify("订单删除成功"); loadOrders(); }
    catch (error) { showError(error); }
  }
}

document.addEventListener("click", (event) => {
  const nav = event.target.closest("[data-view], [data-jump]");
  if (nav) navigate(nav.dataset.view || nav.dataset.jump);
  const close = event.target.closest("[data-close]");
  if (close) $(close.dataset.close).close();
});
document.querySelectorAll(".form-dialog, .detail-dialog").forEach((dialog) => dialog.addEventListener("click", (event) => { if (event.target === dialog) dialog.close(); }));
window.addEventListener("hashchange", () => showView(location.hash.slice(1)));
$("productRows").addEventListener("click", handleProductAction);
$("orderRows").addEventListener("click", handleOrderAction);
$("productForm").addEventListener("submit", saveProduct);
$("orderForm").addEventListener("submit", saveOrder);
$("productSort").addEventListener("change", (event) => { state.products.sort = event.target.value; state.products.page = 0; loadProducts(); });
$("includeInactive").addEventListener("change", (event) => { state.products.includeInactive = event.target.checked; state.products.page = 0; loadProducts(); });
$("orderSort").addEventListener("change", (event) => { state.orders.sort = event.target.value; state.orders.page = 0; loadOrders(); });
$("productPrev").addEventListener("click", () => { state.products.page--; loadProducts(); });
$("productNext").addEventListener("click", () => { state.products.page++; loadProducts(); });
$("orderPrev").addEventListener("click", () => { state.orders.page--; loadOrders(); });
$("orderNext").addEventListener("click", () => { state.orders.page++; loadOrders(); });
$("newProductButton").addEventListener("click", () => openProductDialog());
$("quickNewProduct").addEventListener("click", () => openProductDialog());
$("newOrderButton").addEventListener("click", () => openOrderDialog());
$("quickNewOrder").addEventListener("click", () => openOrderDialog());
$("heroNewOrder").addEventListener("click", () => openOrderDialog());
$("addOrderItem").addEventListener("click", () => addOrderLine());
$("orderItemRows").addEventListener("input", updateOrderPreview);
$("orderItemRows").addEventListener("change", updateOrderPreview);
$("orderItemRows").addEventListener("click", (event) => {
  if (event.target.closest(".remove-line")) { event.target.closest(".line-item").remove(); updateOrderPreview(); }
});
$("detailEdit").addEventListener("click", () => { $("detailDialog").close(); openOrderDialog(state.detailOrderId); });
$("todayText").textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric" }).format(new Date());
showView(location.hash.slice(1) || "overview");
