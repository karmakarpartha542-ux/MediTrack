const BASE_URL = 'http://localhost:8080/api';
async function request(path, options = {}) {
    const response = await fetch(`${BASE_URL}${path}`, {
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options
    });
    const data = await response.json();
    if (!data.success)
        throw new Error(data.error ?? 'Request failed');
    return data.data;
}
// ── Patients ─────────────────────────────────────────────────
export const PatientAPI = {
    getAll: () => request('/patients'),
    getById: (id) => request(`/patients?id=${encodeURIComponent(id)}`),
    search: (query) => request(`/patients?search=${encodeURIComponent(query)}`),
    register: (data) => request('/patients', { method: 'POST', body: JSON.stringify(data) }),
    update: (data) => request('/patients', { method: 'PUT', body: JSON.stringify(data) }),
};
// ── Doctors ──────────────────────────────────────────────────
export const DoctorAPI = {
    getAll: () => request('/doctors'),
    getAvailable: () => request('/doctors?available=true'),
    search: (query) => request(`/doctors?search=${encodeURIComponent(query)}`),
    add: (data) => request('/doctors', { method: 'POST', body: JSON.stringify(data) }),
    update: (data) => request('/doctors', { method: 'PUT', body: JSON.stringify(data) }),
    toggleAvailability: (id) => request(`/doctors?toggle=${encodeURIComponent(id)}`, { method: 'PUT' }),
};
// ── Appointments ─────────────────────────────────────────────
export const AppointmentAPI = {
    getAll: () => request('/appointments'),
    getToday: () => request('/appointments?today=true'),
    getByPatient: (patientId) => request(`/appointments?patientId=${encodeURIComponent(patientId)}`),
    book: (data) => request('/appointments', { method: 'POST', body: JSON.stringify(data) }),
    cancel: (id) => request(`/appointments?id=${encodeURIComponent(id)}&action=cancel`, { method: 'PUT' }),
    complete: (id) => request(`/appointments?id=${encodeURIComponent(id)}&action=complete`, { method: 'PUT' }),
};
// ── Rooms ─────────────────────────────────────────────────────
export const RoomAPI = {
    getAll: () => request('/rooms'),
    getStats: () => request('/rooms/stats'),
    getAvailable: () => request('/rooms?available=true'),
    assignRoom: (roomId, patientId) => request('/rooms', { method: 'POST', body: JSON.stringify({ roomId, patientId }) }),
    vacateRoom: (roomId) => request('/rooms', { method: 'POST', body: JSON.stringify({ roomId }) }),
};
// ── Billing ──────────────────────────────────────────────────
export const BillingAPI = {
    getAll: () => request('/billing'),
    getPending: () => request('/billing?pending=true'),
    getByPatient: (patientId) => request(`/billing?patientId=${encodeURIComponent(patientId)}`),
    generateBill: (data) => request('/billing/generate', { method: 'POST', body: JSON.stringify(data) }),
    markPaid: (id) => request(`/billing?id=${encodeURIComponent(id)}&action=pay`, { method: 'PUT' }),
};
// ── Dashboard ─────────────────────────────────────────────────
export const DashboardAPI = {
    getData: () => request('/dashboard'),
};
