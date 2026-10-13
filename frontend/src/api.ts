// ============================================================
// api.ts — REST API client for MediTrack backend (port 8080)
// ============================================================
import type {
  Patient, Doctor, Room, RoomStats, Appointment,
  Bill, DashboardData, ApiResponse
} from './types.js';

const BASE_URL = 'http://localhost:8080/api';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options
  });
  const data: ApiResponse<T> = await response.json();
  if (!data.success) throw new Error(data.error ?? 'Request failed');
  return data.data as T;
}

// ── Patients ─────────────────────────────────────────────────
export const PatientAPI = {
  getAll:   (): Promise<Patient[]> =>
    request('/patients'),
  getById:  (id: string): Promise<Patient> =>
    request(`/patients?id=${encodeURIComponent(id)}`),
  search:   (query: string): Promise<Patient[]> =>
    request(`/patients?search=${encodeURIComponent(query)}`),
  register: (data: Partial<Patient>): Promise<Patient> =>
    request('/patients', { method: 'POST', body: JSON.stringify(data) }),
  update:   (data: Patient): Promise<Patient> =>
    request('/patients', { method: 'PUT', body: JSON.stringify(data) }),
};

// ── Doctors ──────────────────────────────────────────────────
export const DoctorAPI = {
  getAll:             (): Promise<Doctor[]> =>
    request('/doctors'),
  getAvailable:       (): Promise<Doctor[]> =>
    request('/doctors?available=true'),
  search:             (query: string): Promise<Doctor[]> =>
    request(`/doctors?search=${encodeURIComponent(query)}`),
  add:                (data: Partial<Doctor>): Promise<Doctor> =>
    request('/doctors', { method: 'POST', body: JSON.stringify(data) }),
  update:             (data: Doctor): Promise<Doctor> =>
    request('/doctors', { method: 'PUT', body: JSON.stringify(data) }),
  toggleAvailability: (id: string): Promise<Doctor> =>
    request(`/doctors?toggle=${encodeURIComponent(id)}`, { method: 'PUT' }),
};

// ── Appointments ─────────────────────────────────────────────
export const AppointmentAPI = {
  getAll:       (): Promise<Appointment[]> =>
    request('/appointments'),
  getToday:     (): Promise<Appointment[]> =>
    request('/appointments?today=true'),
  getByPatient: (patientId: string): Promise<Appointment[]> =>
    request(`/appointments?patientId=${encodeURIComponent(patientId)}`),
  book:         (data: Partial<Appointment>): Promise<Appointment> =>
    request('/appointments', { method: 'POST', body: JSON.stringify(data) }),
  cancel:       (id: string): Promise<Appointment> =>
    request(`/appointments?id=${encodeURIComponent(id)}&action=cancel`, { method: 'PUT' }),
  complete:     (id: string): Promise<Appointment> =>
    request(`/appointments?id=${encodeURIComponent(id)}&action=complete`, { method: 'PUT' }),
};

// ── Rooms ─────────────────────────────────────────────────────
export const RoomAPI = {
  getAll:      (): Promise<Room[]> =>
    request('/rooms'),
  getStats:    (): Promise<RoomStats> =>
    request('/rooms/stats'),
  getAvailable:(): Promise<Room[]> =>
    request('/rooms?available=true'),
  assignRoom:  (roomId: string, patientId: string): Promise<Room> =>
    request('/rooms', { method: 'POST', body: JSON.stringify({ roomId, patientId }) }),
  vacateRoom:  (roomId: string): Promise<Room> =>
    request('/rooms', { method: 'POST', body: JSON.stringify({ roomId }) }),
};

// ── Billing ──────────────────────────────────────────────────
export const BillingAPI = {
  getAll:       (): Promise<Bill[]> =>
    request('/billing'),
  getPending:   (): Promise<Bill[]> =>
    request('/billing?pending=true'),
  getByPatient: (patientId: string): Promise<Bill[]> =>
    request(`/billing?patientId=${encodeURIComponent(patientId)}`),
  generateBill: (data: Record<string, unknown>): Promise<Bill> =>
    request('/billing/generate', { method: 'POST', body: JSON.stringify(data) }),
  markPaid:     (id: string): Promise<Bill> =>
    request(`/billing?id=${encodeURIComponent(id)}&action=pay`, { method: 'PUT' }),
};

// ── Dashboard ─────────────────────────────────────────────────
export const DashboardAPI = {
  getData: (): Promise<DashboardData> => request('/dashboard'),
};
