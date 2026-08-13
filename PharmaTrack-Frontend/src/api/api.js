/**
 * PharmaTrack API Service Layer
 * Centralized HTTP client for all backend endpoints using Axios.
 * Each object maps to a Spring Boot REST controller.
 */
import axios from 'axios';

const API_BASE = '/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// --- Response interceptor for global error handling ---
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Session expired / not authenticated -> let the AuthContext clear the user
    if (error.response?.status === 401) {
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    const data = error.response?.data;
    const validationErrors = data?.validationErrors || null;
    let message;
    if (validationErrors && Object.keys(validationErrors).length > 0) {
      message = Object.entries(validationErrors).map(([field, msg]) => `${field}: ${msg}`).join('; ');
    } else {
      message =
        data?.message ||
        data?.error ||
        error.message ||
        'An unexpected error occurred';
    }
    const err = new Error(message);
    err.validationErrors = validationErrors;
    return Promise.reject(err);
  }
);

// ===================== USERS =====================
export const userApi = {
  getAll:          ()             => api.get('/users').then(r => r.data),
  getById:         (id)           => api.get(`/users/${id}`).then(r => r.data),
  getByUsername:   (username)     => api.get(`/users/username/${username}`).then(r => r.data),
  getByRole:       (role)         => api.get(`/users/role/${role}`).then(r => r.data),
  create:          (data)         => api.post('/users', data).then(r => r.data),
  update:          (id, data)     => api.put(`/users/${id}`, data).then(r => r.data),
  patch:          (id, data)     => api.patch(`/users/${id}`, data).then(r => r.data),
  delete:          (id)           => api.delete(`/users/${id}`),
  checkUsername:    (username)     => api.get(`/users/check/username/${username}`).then(r => r.data),
  checkEmail:       (email)        => api.get(`/users/check/email/${email}`).then(r => r.data),
};

// ===================== MEDICINES =====================
export const medicineApi = {
  getAll:            ()             => api.get('/medicines').then(r => r.data),
  getById:           (id)           => api.get(`/medicines/${id}`).then(r => r.data),
  getByCode:         (code)         => api.get(`/medicines/code/${code}`).then(r => r.data),
  getByCategory:     (catId)        => api.get(`/medicines/category/${catId}`).then(r => r.data),
  getByManufacturer: (mfgId)        => api.get(`/medicines/manufacturer/${mfgId}`).then(r => r.data),
  searchByName:      (name)         => api.get('/medicines/search', { params: { name } }).then(r => r.data),
  create:            (data)         => api.post('/medicines', data).then(r => r.data),
  update:            (id, data)     => api.put(`/medicines/${id}`, data).then(r => r.data),
  patch:            (id, data)     => api.patch(`/medicines/${id}`, data).then(r => r.data),
  delete:            (id)           => api.delete(`/medicines/${id}`),
  checkCode:         (code)         => api.get(`/medicines/check/code/${code}`).then(r => r.data),
};

// ===================== CATEGORIES =====================
export const categoryApi = {
  getAll:      ()             => api.get('/categories').then(r => r.data),
  getById:     (id)           => api.get(`/categories/${id}`).then(r => r.data),
  getByName:   (name)         => api.get(`/categories/name/${name}`).then(r => r.data),
  create:      (data)         => api.post('/categories', data).then(r => r.data),
  update:      (id, data)     => api.put(`/categories/${id}`, data).then(r => r.data),
  patch:      (id, data)     => api.patch(`/categories/${id}`, data).then(r => r.data),
  delete:      (id)           => api.delete(`/categories/${id}`),
  checkName:   (name)         => api.get(`/categories/check/name/${name}`).then(r => r.data),
};

// ===================== MANUFACTURERS =====================
export const manufacturerApi = {
  getAll:      ()             => api.get('/manufacturers').then(r => r.data),
  getById:     (id)           => api.get(`/manufacturers/${id}`).then(r => r.data),
  getByName:   (name)         => api.get(`/manufacturers/name/${name}`).then(r => r.data),
  create:      (data)         => api.post('/manufacturers', data).then(r => r.data),
  update:      (id, data)     => api.put(`/manufacturers/${id}`, data).then(r => r.data),
  patch:      (id, data)     => api.patch(`/manufacturers/${id}`, data).then(r => r.data),
  delete:      (id)           => api.delete(`/manufacturers/${id}`),
  checkName:   (name)         => api.get(`/manufacturers/check/name/${name}`).then(r => r.data),
};

// ===================== SUPPLIERS =====================
export const supplierApi = {
  getAll:         ()             => api.get('/suppliers').then(r => r.data),
  getById:        (id)           => api.get(`/suppliers/${id}`).then(r => r.data),
  getByCode:      (code)         => api.get(`/suppliers/code/${code}`).then(r => r.data),
  getActive:      ()             => api.get('/suppliers/active').then(r => r.data),
  create:         (data)         => api.post('/suppliers', data).then(r => r.data),
  update:         (id, data)     => api.put(`/suppliers/${id}`, data).then(r => r.data),
  patch:         (id, data)     => api.patch(`/suppliers/${id}`, data).then(r => r.data),
  delete:         (id)           => api.delete(`/suppliers/${id}`),
  checkCode:      (code)         => api.get(`/suppliers/check/code/${code}`).then(r => r.data),
};

// ===================== INVENTORY BATCHES =====================
export const inventoryBatchApi = {
  getAll:                ()             => api.get('/inventory-batches').then(r => r.data),
  getById:               (id)           => api.get(`/inventory-batches/${id}`).then(r => r.data),
  getByBatchNumber:      (num)          => api.get(`/inventory-batches/number/${num}`).then(r => r.data),
  getByMedicine:         (medId)        => api.get(`/inventory-batches/medicine/${medId}`).then(r => r.data),
  getBySupplier:         (supId)        => api.get(`/inventory-batches/supplier/${supId}`).then(r => r.data),
  getExpiring:           (date)         => api.get('/inventory-batches/expiring', { params: { expiryDate: date } }).then(r => r.data),
  getAvailableByMedicine:(medId)        => api.get(`/inventory-batches/available/medicine/${medId}`).then(r => r.data),
  create:                (data)         => api.post('/inventory-batches', data).then(r => r.data),
  update:                (id, data)     => api.put(`/inventory-batches/${id}`, data).then(r => r.data),
  patch:                (id, data)     => api.patch(`/inventory-batches/${id}`, data).then(r => r.data),
  delete:                (id)           => api.delete(`/inventory-batches/${id}`),
};

// ===================== STOCK MOVEMENTS =====================
export const stockMovementApi = {
  getAll:          ()             => api.get('/stock-movements').then(r => r.data),
  getById:         (id)           => api.get(`/stock-movements/${id}`).then(r => r.data),
  getByType:       (type)         => api.get(`/stock-movements/type/${type}`).then(r => r.data),
  getByMedicine:   (medId)        => api.get(`/stock-movements/medicine/${medId}`).then(r => r.data),
  getByBatch:      (batchId)      => api.get(`/stock-movements/batch/${batchId}`).then(r => r.data),
  getByUser:       (userId)       => api.get(`/stock-movements/user/${userId}`).then(r => r.data),
  create:          (data)         => api.post('/stock-movements', data).then(r => r.data),
  processStockIn:  (data)         => api.post('/stock-movements/stock-in', data).then(r => r.data),
  processStockOut: (data)         => api.post('/stock-movements/stock-out', data).then(r => r.data),
};

// ===================== PRESCRIPTIONS =====================
export const prescriptionApi = {
  getAll:            ()             => api.get('/prescriptions').then(r => r.data),
  getById:           (id)           => api.get(`/prescriptions/${id}`).then(r => r.data),
  getByNumber:       (num)          => api.get(`/prescriptions/number/${num}`).then(r => r.data),
  getByPatient:      (name)         => api.get(`/prescriptions/patient/${name}`).then(r => r.data),
  getByDoctor:       (name)         => api.get(`/prescriptions/doctor/${name}`).then(r => r.data),
  getUnDispensed:    ()             => api.get('/prescriptions/un-dispensed').then(r => r.data),
  create:            (data)         => api.post('/prescriptions', data).then(r => r.data),
  update:            (id, data)     => api.put(`/prescriptions/${id}`, data).then(r => r.data),
  patch:            (id, data)     => api.patch(`/prescriptions/${id}`, data).then(r => r.data),
  markAsDispensed:   (id)           => api.patch(`/prescriptions/${id}/dispense`).then(r => r.data),
  delete:            (id)           => api.delete(`/prescriptions/${id}`),
};

// ===================== DISPENSING RECORDS =====================
export const dispensingRecordApi = {
  getAll:            ()             => api.get('/dispensing-records').then(r => r.data),
  getById:           (id)           => api.get(`/dispensing-records/${id}`).then(r => r.data),
  getByNumber:       (num)          => api.get(`/dispensing-records/number/${num}`).then(r => r.data),
  getByPrescription: (rxId)         => api.get(`/dispensing-records/prescription/${rxId}`).then(r => r.data),
  getByMedicine:     (medId)        => api.get(`/dispensing-records/medicine/${medId}`).then(r => r.data),
  getByUser:         (userId)       => api.get(`/dispensing-records/user/${userId}`).then(r => r.data),
  create:            (data)         => api.post('/dispensing-records', data).then(r => r.data),
  approve:           (id)           => api.post(`/dispensing-records/${id}/approve`).then(r => r.data),
  voidRecord:        (id)           => api.post(`/dispensing-records/${id}/void`).then(r => r.data),
};

// ===================== AUTH (Spring Security form login) =====================
// Plain axios instance (no /api prefix) because /login and /logout are Spring Security
// framework endpoints, not our REST controllers. Same-origin => session cookie flows.
const authHttp = axios.create({
  baseURL: '/',
  headers: { 'Content-Type': 'application/json' },
});

export const authApi = {
  // Returns the currently logged-in user (401 if not authenticated)
  me:   () => api.get('/auth/me').then(r => r.data),
  // Spring Security form login: POST /login with form-encoded credentials
  login: (username, password) =>
    authHttp.post('/login', new URLSearchParams({ username, password }), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    }).then(r => r.data),
  // Invalidates the server-side session
  logout: () => authHttp.post('/logout').then(r => r.data),
};

// ===================== AUDIT LOGS =====================
export const auditLogApi = {
  getAll:            ()             => api.get('/audit-logs').then(r => r.data),
  getById:           (id)           => api.get(`/audit-logs/${id}`).then(r => r.data),
  getByEntityType:   (type)         => api.get(`/audit-logs/entity-type/${type}`).then(r => r.data),
  getByEntity:       (type, id)     => api.get(`/audit-logs/entity/${type}/${id}`).then(r => r.data),
  getByAction:       (action)       => api.get(`/audit-logs/action/${action}`).then(r => r.data),
  getByUser:         (userId)       => api.get(`/audit-logs/user/${userId}`).then(r => r.data),
  getByDateRange:    (start, end)   => api.get('/audit-logs/date-range', { params: { start, end } }).then(r => r.data),
  create:            (data)         => api.post('/audit-logs', data).then(r => r.data),
  logAction:         (params)       => api.post('/audit-logs/log', null, { params }).then(r => r.data),
};

export default api;
