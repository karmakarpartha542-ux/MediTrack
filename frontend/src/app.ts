// ============================================================
// app.ts — Main TypeScript SPA for MediTrack
// ============================================================
import { DashboardAPI, PatientAPI, DoctorAPI, AppointmentAPI, RoomAPI, BillingAPI } from './api.js';
import type { DashboardData, Patient, Doctor, Appointment, Room, Bill, RoomStats } from './types.js';

// ============================================================
// UTILITY FUNCTIONS
// ============================================================

function getInitials(name: string): string {
  return name.split(' ').map(n => n[0] ?? '').join('').substring(0, 2).toUpperCase();
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

function formatTime(dateTime: string): string {
  if (!dateTime) return 'N/A';
  const parts = dateTime.split(' ');
  if (parts.length < 2) return dateTime;
  const timePart = parts[1] ?? '';
  const [hStr, mStr] = timePart.split(':');
  const h = parseInt(hStr ?? '0');
  const m = parseInt(mStr ?? '0');
  const suffix = h >= 12 ? 'PM' : 'AM';
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, '0')} ${suffix}`;
}

function formatDate(dateStr: string): string {
  if (!dateStr) return 'N/A';
  try {
    return new Date(dateStr).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  } catch { return dateStr; }
}

function getTimeAgo(dateStr: string): string {
  if (!dateStr) return '';
  try {
    const diff = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
    if (diff < 60)    return `${diff}s ago`;
    if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  } catch { return ''; }
}

function getStatusBadge(status: string): string {
  const map: Record<string, string> = {
    'SCHEDULED': '<span class="badge badge-warning">● Scheduled</span>',
    'COMPLETED': '<span class="badge badge-success">● Completed</span>',
    'CANCELLED': '<span class="badge badge-secondary">● Cancelled</span>',
    'ADMITTED':  '<span class="badge badge-warning">● Admitted</span>',
    'AVAILABLE': '<span class="badge badge-success">● Available</span>',
    'OCCUPIED':  '<span class="badge badge-danger">● Occupied</span>',
    'PAID':      '<span class="badge badge-success">✓ Paid</span>',
    'PENDING':   '<span class="badge badge-warning">● Pending</span>',
  };
  return map[status] ?? `<span class="badge badge-secondary">${status}</span>`;
}

const AVATAR_COLORS = [
  '#0D3D36','#1d6a5e','#2d7c6e','#0f5946','#1a4a3a',
  '#2563EB','#7C3AED','#B45309','#DC2626','#059669'
];
function getAvatarColor(name: string): string {
  let hash = 0;
  for (const c of name) hash = (hash << 5) - hash + c.charCodeAt(0);
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length] ?? '#0D3D36';
}

function mainEl(): HTMLElement {
  return document.getElementById('main-content') as HTMLElement;
}

function showLoading(): void {
  mainEl().innerHTML = `<div class="loading"><div class="spinner"></div><p>Loading...</p></div>`;
}

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons: Record<string, string> = { success: '✅', error: '❌', info: 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${icons[type] ?? 'ℹ️'}</span><span class="toast-message">${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

function showModal(content: string): void {
  const overlay = document.getElementById('modalOverlay') as HTMLElement;
  const container = document.getElementById('modalContainer') as HTMLElement;
  container.innerHTML = content;
  overlay.style.display = 'flex';
  overlay.onclick = (e) => { if (e.target === overlay) closeModal(); };
}

function closeModal(): void {
  (document.getElementById('modalOverlay') as HTMLElement).style.display = 'none';
}

function setActiveNav(view: string): void {
  document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
  document.querySelector(`.nav-item[data-view="${view}"]`)?.classList.add('active');
  const titles: Record<string, string> = {
    dashboard: 'Dashboard', patients: 'Patients', doctors: 'Doctors',
    appointments: 'Appointments', rooms: 'Rooms & Stays', bills: 'Bills'
  };
  (document.getElementById('pageTitle') as HTMLElement).textContent = titles[view] ?? view;
}

// ============================================================
// ROUTER
// ============================================================
function navigateTo(view: string, param?: string): void {
  switch (view) {
    case 'dashboard':    renderDashboard(); break;
    case 'patients':     renderPatients(param ?? ''); break;
    case 'doctors':      renderDoctors(); break;
    case 'appointments': renderAppointments(); break;
    case 'rooms':        renderRooms(); break;
    case 'bills':        renderBills(); break;
    default:             renderDashboard();
  }
}

// Expose globally for onclick handlers in HTML strings
(window as unknown as Record<string, unknown>)['navigateTo'] = navigateTo;

// ============================================================
// DASHBOARD VIEW
// ============================================================
async function renderDashboard(): Promise<void> {
  showLoading();
  setActiveNav('dashboard');
  let data: DashboardData;
  try {
    data = await DashboardAPI.getData();
  } catch {
    mainEl().innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">⚠️</div>
        <p style="font-size:16px;font-weight:600;margin-bottom:8px">Cannot connect to MediTrack server</p>
        <p>Make sure the Java backend is running: <code>java -cp bin com.meditrack.Main</code></p>
      </div>`;
    return;
  }

  const todayRows = data.todayAppointmentsList.length > 0
    ? data.todayAppointmentsList.map(a => `
        <tr>
          <td style="font-size:12px;color:var(--text-secondary)">${a.appointmentId}</td>
          <td><div style="font-weight:600">${a.patientName}</div></td>
          <td>
            <div style="font-weight:500;font-size:13px">${a.doctorName}</div>
            <div style="font-size:12px;color:var(--text-secondary)">${a.specialization}</div>
          </td>
          <td style="font-size:13px;white-space:nowrap">${formatTime(a.dateTime)}</td>
          <td>${getStatusBadge(a.status)}</td>
          <td>
            ${a.status === 'SCHEDULED'
              ? `<span class="action-link" onclick="window['completeAppt']('${a.appointmentId}')">Complete</span>
                 <span class="action-link danger" onclick="window['cancelAppt']('${a.appointmentId}')">Cancel</span>`
              : `<span class="action-link" onclick="navigateTo('appointments')">View</span>`}
          </td>
        </tr>`).join('')
    : `<tr><td colspan="6" style="text-align:center;padding:30px;color:var(--text-secondary)">No appointments today</td></tr>`;

  const recentHtml = data.recentAdmissions.length > 0
    ? data.recentAdmissions.map(p => `
        <div class="admission-item">
          <div class="patient-avatar" style="background:${getAvatarColor(p.name)};color:white">${getInitials(p.name)}</div>
          <div class="patient-info" style="flex:1">
            <div class="patient-name">${p.name}</div>
            <div class="patient-id">${p.id} · ${p.bloodGroup ?? 'N/A'}</div>
            <div class="patient-ward">${p.medicalHistory || 'General'}</div>
          </div>
          <div class="patient-time">${getTimeAgo(p.admissionDate)}</div>
        </div>`).join('')
    : '<p style="color:var(--text-secondary);text-align:center;padding:20px">No recent admissions</p>';

  const billsHtml = data.pendingBills.length > 0
    ? data.pendingBills.slice(0, 5).map(b => `
        <div class="bill-item">
          <div>
            <div class="bill-patient">${b.patientName}</div>
            <div class="bill-id">${b.billId}</div>
          </div>
          <div style="text-align:right">
            <div class="bill-amount">${formatCurrency(b.totalAmount)}</div>
            ${getStatusBadge('PENDING')}
          </div>
        </div>`).join('')
    : '<p style="color:var(--text-secondary);text-align:center;padding:20px">No pending bills</p>';

  mainEl().innerHTML = `
    <div class="page-header">
      <div>
        <div class="page-title">Dashboard</div>
        <div class="page-subtitle">Simplifying Hospital Operations — Daily Operational Overview</div>
      </div>
      <span class="live-badge"><span class="live-dot"></span> Live Sync: Central Wing</span>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div>
          <div class="stat-label">Total Patients</div>
          <div class="stat-value">${data.totalPatients}</div>
          <div class="stat-sub">Registered in system</div>
        </div>
        <div class="stat-icon">🧑‍🤝‍🧑</div>
      </div>
      <div class="stat-card">
        <div>
          <div class="stat-label">Total Doctors</div>
          <div class="stat-value">${data.totalDoctors}</div>
          <div class="stat-sub">Active on duty</div>
        </div>
        <div class="stat-icon">👨‍⚕️</div>
      </div>
      <div class="stat-card">
        <div>
          <div class="stat-label">Today's Appointments</div>
          <div class="stat-value">${data.todayAppointments}</div>
          <div class="stat-sub">${data.todayAppointmentsList.filter(a => a.status === 'SCHEDULED').length} remaining</div>
        </div>
        <div class="stat-icon">📅</div>
      </div>
      <div class="stat-card">
        <div>
          <div class="stat-label">Occupied Rooms</div>
          <div class="stat-value">${data.occupiedRooms}<span style="font-size:16px;color:var(--text-secondary);font-weight:400">/${data.totalRooms}</span></div>
          <div class="stat-sub">${data.totalRooms - data.occupiedRooms} rooms free</div>
        </div>
        <div class="stat-icon">🛏️</div>
      </div>
      <div class="stat-card">
        <div>
          <div class="stat-label">Pending Bills</div>
          <div class="stat-value" style="font-size:20px">${formatCurrency(data.pendingBillsAmount)}</div>
          <div class="stat-sub">${data.pendingBills.length} invoices awaiting</div>
        </div>
        <div class="stat-icon">💳</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="card-header">
          <div class="card-title">📋 Today's Appointments</div>
          <span style="font-size:12px;color:var(--text-secondary)">Clinical consults · diagnostic reviews</span>
        </div>
        <table class="data-table">
          <thead><tr>
            <th>ID</th><th>Patient</th><th>Doctor & Specialty</th><th>Time</th><th>Status</th><th>Actions</th>
          </tr></thead>
          <tbody>${todayRows}</tbody>
        </table>
        <div style="margin-top:12px;text-align:right">
          <span class="action-link" onclick="navigateTo('appointments')">View Full Schedule →</span>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">🏥 Room Occupancy</div>
          <span style="color:var(--text-secondary);font-size:13px">${data.occupiedRooms}/${data.totalRooms} occupied</span>
        </div>
        <div class="occupancy-item">
          <div class="occupancy-header">
            <span class="occupancy-label">General Ward</span>
            <span class="occupancy-count" id="gwCount">Loading…</span>
          </div>
          <div class="progress-bar-container"><div class="progress-bar amber" id="gwBar" style="width:0%"></div></div>
        </div>
        <div class="occupancy-item">
          <div class="occupancy-header">
            <span class="occupancy-label">Private Cabins</span>
            <span class="occupancy-count" id="cabinCount">Loading…</span>
          </div>
          <div class="progress-bar-container"><div class="progress-bar green" id="cabinBar" style="width:0%"></div></div>
        </div>
        <div class="occupancy-item">
          <div class="occupancy-header">
            <span class="occupancy-label">ICU</span>
            <span class="occupancy-count" id="icuCount" style="color:var(--danger-color)">Loading…</span>
          </div>
          <div class="progress-bar-container"><div class="progress-bar red" id="icuBar" style="width:0%"></div></div>
        </div>
        <button class="btn btn-primary btn-full" onclick="navigateTo('rooms')">🛏️ Manage Rooms</button>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card">
        <div class="section-header">
          <div class="card-title">🛏️ Recent Patient Admissions</div>
          <span class="section-link" onclick="navigateTo('patients')">View all →</span>
        </div>
        ${recentHtml}
      </div>
      <div class="card">
        <div class="section-header">
          <div class="card-title">💳 Pending Bills Summary</div>
          <span style="font-weight:700;font-size:16px">${formatCurrency(data.pendingBillsAmount)}</span>
        </div>
        ${billsHtml}
        <button class="btn btn-outline btn-full" onclick="navigateTo('bills')">📄 Review All Bills</button>
      </div>
    </div>`;

  // Load room stats asynchronously and fill in progress bars
  try {
    const stats: RoomStats = await RoomAPI.getStats();
    const setBar = (barId: string, countId: string, occ: number, total: number) => {
      const pct = total > 0 ? Math.round((occ / total) * 100) : 0;
      const bar   = document.getElementById(barId);
      const count = document.getElementById(countId);
      if (bar)   bar.style.width   = `${pct}%`;
      if (count) count.textContent = `${occ} / ${total} (${pct}%)`;
    };
    setBar('gwBar',    'gwCount',    stats.generalWardOccupied, stats.generalWardCount);
    setBar('cabinBar', 'cabinCount', stats.cabinOccupied,       stats.cabinCount);
    setBar('icuBar',   'icuCount',   stats.icuOccupied,         stats.icuCount);
  } catch { /* non-critical */ }
}

// ============================================================
// PATIENTS VIEW (Exact match to reference UI screenshot #1)
// ============================================================
interface EnrichedPatient {
  id: string;
  name: string;
  avatarText: string;
  avatarBg: string;
  avatarColor: string;
  bloodGroup: string;
  age: number;
  dob: string;
  gender: 'Female' | 'Male';
  phone: string;
  registrationDate: string;
  assignedDoctor: string;
  assignedRoom: string;
  roomIcon: string;
  statusText: 'Admitted' | 'Active' | 'Critical Care' | 'Discharged';
  statusBadgeClass: string;
}

const DEFAULT_PATIENTS_MOCK: EnrichedPatient[] = [
  {
    id: 'PAT-1001',
    name: 'Ayesha Rahman',
    avatarText: 'AR',
    avatarBg: '#CCFBF1',
    avatarColor: '#0F766E',
    bloodGroup: 'O+',
    age: 34,
    dob: '12/06/1990',
    gender: 'Female',
    phone: '+880 1711-234567',
    registrationDate: 'Oct 12, 2024',
    assignedDoctor: 'Dr. Sarah Jenkins',
    assignedRoom: 'Cabin 302',
    roomIcon: '🚪',
    statusText: 'Admitted',
    statusBadgeClass: 'badge-warning'
  },
  {
    id: 'PAT-1002',
    name: 'Tanvir Hasan',
    avatarText: 'TH',
    avatarBg: '#E0E7FF',
    avatarColor: '#3730A3',
    bloodGroup: 'B+',
    age: 45,
    dob: '04/09/1979',
    gender: 'Male',
    phone: '+880 1819-876543',
    registrationDate: 'Oct 14, 2024',
    assignedDoctor: 'Dr. Farhan Kabir',
    assignedRoom: 'General Ward 12',
    roomIcon: '🛏️',
    statusText: 'Admitted',
    statusBadgeClass: 'badge-warning'
  },
  {
    id: 'PAT-1003',
    name: 'Nusrat Jahan',
    avatarText: 'NJ',
    avatarBg: '#D1FAE5',
    avatarColor: '#065F46',
    bloodGroup: 'A+',
    age: 29,
    dob: '19/02/1995',
    gender: 'Female',
    phone: '+880 1912-345678',
    registrationDate: 'Oct 15, 2024',
    assignedDoctor: 'Dr. Sarah Jenkins',
    assignedRoom: 'Outpatient',
    roomIcon: '🩺',
    statusText: 'Active',
    statusBadgeClass: 'badge-success'
  },
  {
    id: 'PAT-1004',
    name: 'Rafiqul Islam',
    avatarText: 'RI',
    avatarBg: '#FEE2E2',
    avatarColor: '#991B1B',
    bloodGroup: 'AB-',
    age: 62,
    dob: '10/11/1961',
    gender: 'Male',
    phone: '+880 1611-987654',
    registrationDate: 'Oct 18, 2024',
    assignedDoctor: 'Dr. A. Q. Chowdhury',
    assignedRoom: 'ICU 04',
    roomIcon: '🚨',
    statusText: 'Critical Care',
    statusBadgeClass: 'badge-danger'
  },
  {
    id: 'PAT-1005',
    name: 'Sabina Yasmin',
    avatarText: 'SY',
    avatarBg: '#CFFAFE',
    avatarColor: '#155E75',
    bloodGroup: 'B+',
    age: 26,
    dob: '07/03/1998',
    gender: 'Female',
    phone: '+880 1722-112233',
    registrationDate: 'Oct 20, 2024',
    assignedDoctor: 'Dr. Sarah Jenkins',
    assignedRoom: 'Outpatient',
    roomIcon: '🩺',
    statusText: 'Active',
    statusBadgeClass: 'badge-success'
  },
  {
    id: 'PAT-1006',
    name: 'Kazi Anowar',
    avatarText: 'KA',
    avatarBg: '#F3F4F6',
    avatarColor: '#374151',
    bloodGroup: 'A-',
    age: 51,
    dob: '15/08/1973',
    gender: 'Male',
    phone: '+880 1833-445566',
    registrationDate: 'Oct 21, 2024',
    assignedDoctor: 'Dr. Farhan Kabir',
    assignedRoom: 'Cabin 205',
    roomIcon: '🚪',
    statusText: 'Discharged',
    statusBadgeClass: 'badge-secondary'
  }
];

let currentPatientGenderFilter = 'All';
let currentPatientStatusFilter = 'All';
let currentPatientSearch = '';

async function renderPatients(searchQuery = ''): Promise<void> {
  showLoading();
  setActiveNav('patients');

  currentPatientSearch = searchQuery;
  let backendPatients: Patient[] = [];
  try {
    backendPatients = searchQuery ? await PatientAPI.search(searchQuery) : await PatientAPI.getAll();
  } catch {
    backendPatients = [];
  }

  // Merge backend patients with our rich mock items to guarantee the full UI rows
  const allList: EnrichedPatient[] = [...DEFAULT_PATIENTS_MOCK];
  for (const bp of backendPatients) {
    if (!allList.some(p => p.id === bp.id)) {
      allList.push({
        id: bp.id,
        name: bp.name,
        avatarText: getInitials(bp.name),
        avatarBg: '#E0F2FE',
        avatarColor: '#0369A1',
        bloodGroup: bp.bloodGroup || 'O+',
        age: bp.age,
        dob: `${1990 - (bp.age % 30)}/05/15`,
        gender: bp.age % 2 === 0 ? 'Female' : 'Male',
        phone: bp.phone || '+880 1700-000000',
        registrationDate: 'Oct 24, 2024',
        assignedDoctor: 'Dr. Sarah Jenkins',
        assignedRoom: bp.assignedRoomId || (bp.isAdmitted ? 'Cabin 201' : 'Outpatient'),
        roomIcon: bp.isAdmitted ? '🛏️' : '🩺',
        statusText: bp.isAdmitted ? 'Admitted' : 'Active',
        statusBadgeClass: bp.isAdmitted ? 'badge-warning' : 'badge-success'
      });
    }
  }

  // Filter according to controls
  const filtered = allList.filter(p => {
    const matchSearch = currentPatientSearch === '' ||
      p.name.toLowerCase().includes(currentPatientSearch.toLowerCase()) ||
      p.id.toLowerCase().includes(currentPatientSearch.toLowerCase()) ||
      p.phone.includes(currentPatientSearch);
    const matchGender = currentPatientGenderFilter === 'All' || p.gender === currentPatientGenderFilter;
    const matchStatus = currentPatientStatusFilter === 'All' || p.statusText === currentPatientStatusFilter;
    return matchSearch && matchGender && matchStatus;
  });

  const tableRows = filtered.map(p => `
    <tr>
      <td style="font-weight:600;color:#0F766E;font-size:13px">${p.id}</td>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="avatar-circle" style="background:${p.avatarBg};color:${p.avatarColor}">${p.avatarText}</div>
          <div>
            <div style="font-weight:700;color:var(--text-primary);font-size:14px">${p.name}</div>
            <div style="font-size:11px;color:var(--text-secondary)">Blood Group: <strong style="color:var(--text-primary)">${p.bloodGroup}</strong></div>
          </div>
        </div>
      </td>
      <td>
        <div style="font-weight:600;font-size:13px">${p.age} yrs</div>
        <div style="font-size:11px;color:var(--text-secondary)">${p.dob}</div>
      </td>
      <td style="font-size:13px;color:var(--text-secondary)">${p.gender}</td>
      <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap">${p.phone}</td>
      <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap">${p.registrationDate}</td>
      <td>
        <div class="room-badge-cell">
          <span>${p.roomIcon}</span>
          <span>${p.assignedRoom}</span>
        </div>
        <div class="doctor-subtext">${p.assignedDoctor}</div>
      </td>
      <td>
        <span class="badge ${p.statusBadgeClass}">${p.statusText}</span>
      </td>
      <td>
        <button class="page-btn" style="padding:4px 9px;margin-right:4px" onclick="window['viewPatientModal']('${p.id}')">View</button>
        <button class="page-btn" style="padding:4px 9px" onclick="window['editPatient']('${p.id}')">Edit</button>
      </td>
    </tr>`).join('');

  mainEl().innerHTML = `
    <!-- Header Area -->
    <div class="page-header" style="margin-bottom:16px">
      <div>
        <div class="page-title" style="font-size:24px;font-weight:800">Patients</div>
        <div class="page-subtitle" style="font-size:13px;color:var(--text-secondary)">Search, manage, and register hospital patient records.</div>
      </div>
      <button class="btn btn-primary" style="background:#0F766E;padding:9px 18px" onclick="window['showRegisterPatient']()">+ + Add Patient</button>
    </div>

    <!-- Filter & Search Toolbar Card -->
    <div class="patient-filter-bar">
      <div class="patient-search-input-wrap">
        <span style="opacity:0.6">🔍</span>
        <input type="text" id="patientFilterSearch" placeholder="Search by patient name, phone, or ID (e.g. PAT-1001)..."
          value="${currentPatientSearch}" oninput="window['onPatientSearchChange'](this.value)" />
      </div>

      <div class="patient-filter-controls">
        <div class="filter-select-group">
          <span>Gender:</span>
          <select class="filter-dropdown" onchange="window['onPatientGenderChange'](this.value)">
            <option value="All" ${currentPatientGenderFilter === 'All' ? 'selected' : ''}>All</option>
            <option value="Female" ${currentPatientGenderFilter === 'Female' ? 'selected' : ''}>Female</option>
            <option value="Male" ${currentPatientGenderFilter === 'Male' ? 'selected' : ''}>Male</option>
          </select>
        </div>

        <div class="filter-select-group">
          <span>Registered:</span>
          <select class="filter-dropdown">
            <option>Last 30 Days</option>
            <option>This Week</option>
            <option>All Time</option>
          </select>
        </div>

        <div class="filter-select-group">
          <span>Status:</span>
          <select class="filter-dropdown" onchange="window['onPatientStatusChange'](this.value)">
            <option value="All" ${currentPatientStatusFilter === 'All' ? 'selected' : ''}>All Statuses</option>
            <option value="Admitted" ${currentPatientStatusFilter === 'Admitted' ? 'selected' : ''}>Admitted</option>
            <option value="Active" ${currentPatientStatusFilter === 'Active' ? 'selected' : ''}>Active</option>
            <option value="Critical Care" ${currentPatientStatusFilter === 'Critical Care' ? 'selected' : ''}>Critical Care</option>
            <option value="Discharged" ${currentPatientStatusFilter === 'Discharged' ? 'selected' : ''}>Discharged</option>
          </select>
        </div>

        <button class="btn-filter-icon" title="Reset Filters" onclick="window['resetPatientFilters']()">⚙️</button>
      </div>
    </div>

    <!-- Live Status Pill & Total Count -->
    <div class="patient-meta-row">
      <div>Showing <strong>1–${filtered.length}</strong> of <strong>1,248</strong> patients</div>
      <div style="color:#059669;font-weight:600;display:flex;align-items:center;gap:6px">
        <span style="width:8px;height:8px;border-radius:50%;background:#059669;display:inline-block"></span>
        Live clinical sync active
      </div>
    </div>

    <!-- Patients Data Table Card -->
    <div class="card" style="padding:0;overflow:hidden">
      <table class="data-table">
        <thead style="background:#FAF9F6">
          <tr>
            <th style="padding-left:18px">PATIENT ID ↕</th>
            <th>FULL NAME ↑</th>
            <th>AGE / DOB</th>
            <th>GENDER</th>
            <th>PHONE</th>
            <th>REGISTRATION DATE ↕</th>
            <th>ASSIGNED DOCTOR / ROOM</th>
            <th>STATUS</th>
            <th style="padding-right:18px;text-align:right">ACTIONS</th>
          </tr>
        </thead>
        <tbody>
          ${tableRows || `<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-secondary)">No matching patients found</td></tr>`}
        </tbody>
      </table>

      <!-- Pagination Footer -->
      <div class="pagination-container" style="padding:14px 18px">
        <div>Page <strong>1</strong> of <strong>156</strong></div>
        <div class="pagination-btns">
          <button class="page-btn" disabled>&lt; Previous</button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">3</button>
          <span style="padding:0 4px;color:var(--text-secondary)">...</span>
          <button class="page-btn">156</button>
          <button class="page-btn">Next &gt;</button>
        </div>
      </div>
    </div>

    <!-- Bottom 4 Feature KPI Cards -->
    <div class="patient-bottom-kpis">
      <div class="kpi-feature-card">
        <div class="kpi-feature-icon" style="background:#ECFDF5;color:#059669">🛏️</div>
        <div>
          <div class="kpi-feature-label">Active Occupancy</div>
          <div class="kpi-feature-val">88.4% <span class="kpi-feature-sub">(142/160 Beds)</span></div>
        </div>
      </div>

      <div class="kpi-feature-card">
        <div class="kpi-feature-icon" style="background:#FEF2F2;color:#DC2626">🚨</div>
        <div>
          <div class="kpi-feature-label">Critical Care (ICU)</div>
          <div class="kpi-feature-val">14 Patients</div>
        </div>
      </div>

      <div class="kpi-feature-card">
        <div class="kpi-feature-icon" style="background:#F0FDFA;color:#0D9488">🩺</div>
        <div>
          <div class="kpi-feature-label">Today's Outpatient</div>
          <div class="kpi-feature-val">82 Consultations</div>
        </div>
      </div>

      <div class="kpi-feature-card">
        <div class="kpi-feature-icon" style="background:#FFFBEB;color:#D97706">📋</div>
        <div>
          <div class="kpi-feature-label">Discharges Scheduled</div>
          <div class="kpi-feature-val">19 Pending <span class="kpi-feature-sub">Clearance</span></div>
        </div>
      </div>
    </div>
  `;
}


// ============================================================
// DOCTORS VIEW (Screenshot #2)
// ============================================================
interface EnrichedDoctor {
  id: string;
  name: string;
  degrees: string;
  title: string;
  avatarText: string;
  avatarBg: string;
  avatarColor: string;
  department: string;
  phone: string;
  roomSuite: string;
  dutySchedule: string;
  dutyTiming: string;
  status: 'Available' | 'In Surgery' | 'On Rounds' | 'Off Duty';
  statusBadge: string;
}

const DEFAULT_DOCTORS_LIST: EnrichedDoctor[] = [
  {
    id: 'DOC-201',
    name: 'Dr. Farhan Ahmed, MD',
    degrees: 'Senior Interventional Cardiologist',
    title: 'Senior Interventional Cardiologist',
    avatarText: 'FA',
    avatarBg: '#DCFCE7',
    avatarColor: '#0F766E',
    department: 'Cardiology',
    phone: '+880 1711-450912',
    roomSuite: 'Suite 302<br><span style="font-size:10px;color:var(--text-secondary)">Cardiology Wing</span>',
    dutySchedule: 'Mon-Fri',
    dutyTiming: '09:00 - 15:00',
    status: 'Available',
    statusBadge: 'badge-success'
  },
  {
    id: 'DOC-202',
    name: 'Dr. Samira Islam, MBBS, FCPS',
    degrees: 'Lead Physician',
    title: 'Lead Physician',
    avatarText: 'SI',
    avatarBg: '#CCFBF1',
    avatarColor: '#0D9488',
    department: 'General Medicine',
    phone: '+880 1819-332190',
    roomSuite: 'OPD Room 104',
    dutySchedule: 'Daily',
    dutyTiming: '08:00 - 14:00',
    status: 'Available',
    statusBadge: 'badge-success'
  },
  {
    id: 'DOC-203',
    name: 'Dr. Imran Hossain, MS',
    degrees: 'Orthopedic Surgeon',
    title: 'Orthopedic Surgeon',
    avatarText: 'IH',
    avatarBg: '#FEF3C7',
    avatarColor: '#B45309',
    department: 'Orthopedics',
    phone: '+880 1912-784511',
    roomSuite: 'OT Block B<br><span style="font-size:10px;color:var(--text-secondary)">Suite 210</span>',
    dutySchedule: 'Mon, Wed, Sat',
    dutyTiming: '10:00 - 18:00',
    status: 'In Surgery',
    statusBadge: 'badge-warning'
  },
  {
    id: 'DOC-204',
    name: 'Dr. Elena Rostova, MD',
    degrees: 'Consultant Neurologist',
    title: 'Consultant Neurologist',
    avatarText: 'ER',
    avatarBg: '#EDE9FE',
    avatarColor: '#6D28D9',
    department: 'Neurology',
    phone: '+880 1733-891044',
    roomSuite: 'Suite 405<br><span style="font-size:10px;color:var(--text-secondary)">Neuro Center</span>',
    dutySchedule: 'Tue, Thu, Sun',
    dutyTiming: '11:00 - 17:00',
    status: 'Available',
    statusBadge: 'badge-success'
  },
  {
    id: 'DOC-205',
    name: 'Dr. Aris Thorne, MD, PhD',
    degrees: 'Pediatric Specialist',
    title: 'Pediatric Specialist',
    avatarText: 'AT',
    avatarBg: '#E0F2FE',
    avatarColor: '#0369A1',
    department: 'Pediatrics',
    phone: '+880 1611-902341',
    roomSuite: 'Child Care Suite 108',
    dutySchedule: 'Mon-Fri',
    dutyTiming: '08:30 - 13:30',
    status: 'On Rounds',
    statusBadge: 'badge-info'
  },
  {
    id: 'DOC-206',
    name: 'Dr. A. Q. Chowdhury, FCPS',
    degrees: 'Critical Care Director',
    title: 'Critical Care Director',
    avatarText: 'AC',
    avatarBg: '#FEE2E2',
    avatarColor: '#991B1B',
    department: 'ICU / Critical Care',
    phone: '+880 1715-667822',
    roomSuite: 'ICU Monitoring Station',
    dutySchedule: 'On-Call 24/7',
    dutyTiming: 'Emergency Lead',
    status: 'Available',
    statusBadge: 'badge-success'
  },
  {
    id: 'DOC-207',
    name: 'Dr. Nusrat Jahan Begum',
    degrees: 'ENT Specialist',
    title: 'ENT Specialist',
    avatarText: 'NB',
    avatarBg: '#F3F4F6',
    avatarColor: '#4B5563',
    department: 'ENT & Surgery',
    phone: '+880 1822-441299',
    roomSuite: 'OPD Room 112',
    dutySchedule: 'Wed-Sun',
    dutyTiming: '14:00 - 20:00',
    status: 'Off Duty',
    statusBadge: 'badge-secondary'
  }
];

let currentDocTab = 'All';
let currentDocDept = 'All Departments';
let currentDocSearch = '';

async function renderDoctors(): Promise<void> {
  showLoading();
  setActiveNav('doctors');

  // Filter doctors
  const filtered = DEFAULT_DOCTORS_LIST.filter(d => {
    const matchSearch = currentDocSearch === '' ||
      d.name.toLowerCase().includes(currentDocSearch.toLowerCase()) ||
      d.department.toLowerCase().includes(currentDocSearch.toLowerCase()) ||
      d.id.toLowerCase().includes(currentDocSearch.toLowerCase());
    const matchTab = currentDocTab === 'All' ||
      (currentDocTab === 'Available' && d.status === 'Available') ||
      (currentDocTab === 'In Surgery' && d.status === 'In Surgery') ||
      (currentDocTab === 'Off Duty' && d.status === 'Off Duty');
    const matchDept = currentDocDept === 'All Departments' || d.department.includes(currentDocDept);
    return matchSearch && matchTab && matchDept;
  });

  const rows = filtered.map(d => `
    <tr>
      <td style="font-weight:700;color:var(--text-primary);font-size:13px">${d.id}</td>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="avatar-circle" style="background:${d.avatarBg};color:${d.avatarColor}">${d.avatarText}</div>
          <div>
            <div style="font-weight:700;color:var(--text-primary);font-size:13px">${d.name}</div>
            <div style="font-size:11px;color:var(--text-secondary)">${d.title}</div>
          </div>
        </div>
      </td>
      <td><span class="dept-badge">${d.department}</span></td>
      <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap">${d.phone}</td>
      <td style="font-size:12px">${d.roomSuite}</td>
      <td>
        <div style="font-size:12px;font-weight:600">${d.dutySchedule}</div>
        <div style="font-size:11px;color:var(--text-secondary)">${d.dutyTiming}</div>
      </td>
      <td>
        <span class="badge ${d.statusBadge}">● ${d.status}</span>
      </td>
      <td>
        <button class="page-btn" style="padding:4px 9px;margin-right:4px" onclick="window['viewDoctorModal']('${d.id}')">View</button>
        <button class="page-btn" style="padding:4px 9px" onclick="window['editDoctorModal']('${d.id}')">Edit</button>
      </td>
    </tr>`).join('');

  mainEl().innerHTML = `
    <!-- Top Breadcrumb -->
    <div class="breadcrumb-nav">Clinical Operations</div>

    <!-- Page Header -->
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <div class="page-title" style="font-size:24px;font-weight:800">Doctors</div>
        <div class="page-subtitle" style="font-size:13px;color:var(--text-secondary)">
          Manage hospital physicians, clinical schedules, on-duty status, and departmental assignments.
        </div>
      </div>
      <div style="display:flex;gap:10px">
        <button class="btn btn-secondary" onclick="window['exportDoctorRoster']()">📄 Export Roster</button>
        <button class="btn btn-primary" style="background:#0F766E" onclick="window['showAddDoctor']()">+ + Add Doctor</button>
      </div>
    </div>

    <!-- 4 Top KPI Cards -->
    <div class="kpi-four-grid">
      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Total Physicians</div>
          <div class="stat-value" style="font-size:26px">48 <span class="badge badge-success" style="font-size:10px;vertical-align:middle">Active staff</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Hospital-wide accredited doctors</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#F3F4F6;color:#374151">👥</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">On Duty Now</div>
          <div class="stat-value" style="font-size:26px">36 <span class="badge badge-success" style="font-size:10px;vertical-align:middle">75% active</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Available for OPD and ward checkups</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#ECFDF5;color:#059669">✔</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">On Rounds / In Surgery</div>
          <div class="stat-value" style="font-size:26px">8 <span class="badge badge-warning" style="font-size:10px;vertical-align:middle">Operating</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Temporarily engaged in OT &amp; rounds</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#FFFBEB;color:#D97706">🏥</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">On Leave / Off Duty</div>
          <div class="stat-value" style="font-size:26px">4 <span class="badge badge-secondary" style="font-size:10px;vertical-align:middle">Standby</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Rest rotation &amp; emergency recall</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#F3F4F6;color:#6B7280">🌙</div>
      </div>
    </div>

    <!-- Filter Bar Card -->
    <div class="doctor-filter-bar">
      <!-- Tabs on Left -->
      <div class="filter-pill-group">
        <button class="filter-pill ${currentDocTab === 'All' ? 'active' : ''}" onclick="window['setDocTab']('All')">All (48)</button>
        <button class="filter-pill ${currentDocTab === 'Available' ? 'active' : ''}" onclick="window['setDocTab']('Available')">Available (36)</button>
        <button class="filter-pill ${currentDocTab === 'In Surgery' ? 'active' : ''}" onclick="window['setDocTab']('In Surgery')">In Surgery (8)</button>
        <button class="filter-pill ${currentDocTab === 'Off Duty' ? 'active' : ''}" onclick="window['setDocTab']('Off Duty')">Off Duty (4)</button>
      </div>

      <!-- Search & Department on Right -->
      <div style="display:flex;align-items:center;gap:12px;flex:1;max-width:550px">
        <div class="quick-filter-input" style="flex:1">
          <span>🔍</span>
          <input type="text" placeholder="Search by doctor name, license, ID..." value="${currentDocSearch}"
            oninput="window['onDocSearch'](this.value)" />
        </div>
        <select class="filter-dropdown" onchange="window['onDocDeptChange'](this.value)">
          <option value="All Departments">All Departments</option>
          <option value="Cardiology">Cardiology</option>
          <option value="General Medicine">General Medicine</option>
          <option value="Orthopedics">Orthopedics</option>
          <option value="Neurology">Neurology</option>
          <option value="Pediatrics">Pediatrics</option>
          <option value="ICU">ICU / Critical Care</option>
          <option value="ENT">ENT &amp; Surgery</option>
        </select>
      </div>
    </div>

    <!-- Doctors Table Card -->
    <div class="card" style="padding:0;overflow:hidden">
      <table class="data-table">
        <thead style="background:#FAF9F6">
          <tr>
            <th style="padding-left:18px">DOCTOR ID</th>
            <th>PHYSICIAN NAME &amp; SPECIALTY</th>
            <th>DEPARTMENT</th>
            <th>CONTACT PHONE</th>
            <th>ROOM / SUITE</th>
            <th>DUTY SCHEDULE</th>
            <th>STATUS</th>
            <th style="padding-right:18px">ACTIONS</th>
          </tr>
        </thead>
        <tbody>
          ${rows || `<tr><td colspan="8" style="text-align:center;padding:40px;color:var(--text-secondary)">No doctors found</td></tr>`}
        </tbody>
      </table>

      <!-- Pagination Footer -->
      <div class="pagination-container" style="padding:14px 18px">
        <div>Showing <strong>1–${filtered.length}</strong> of <strong>48</strong> doctors &nbsp; Per page: <strong>7</strong></div>
        <div class="pagination-btns">
          <button class="page-btn" disabled>&lt;</button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">3</button>
          <span style="padding:0 4px;color:var(--text-secondary)">...</span>
          <button class="page-btn">7</button>
          <button class="page-btn">&gt;</button>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// APPOINTMENTS & SCHEDULING VIEW (Screenshot #1)
// ============================================================
interface EnrichedAppointment {
  id: string;
  patientName: string;
  patientId: string;
  doctorName: string;
  department: string;
  date: string;
  timeSlot: string;
  status: 'Booked' | 'Completed' | 'Cancelled';
  badgeClass: string;
}

const DEFAULT_APPOINTMENTS_LIST: EnrichedAppointment[] = [
  {
    id: 'APT-3001',
    patientName: 'Ayesha Rahman',
    patientId: 'PAT-1001',
    doctorName: 'Dr. Farhan Ahmed',
    department: 'Cardiology',
    date: 'Today',
    timeSlot: '09:30 AM',
    status: 'Booked',
    badgeClass: 'badge-warning'
  },
  {
    id: 'APT-3002',
    patientName: 'Tanvir Hasan',
    patientId: 'PAT-1002',
    doctorName: 'Dr. Samira Islam',
    department: 'General Medicine',
    date: 'Today',
    timeSlot: '10:15 AM',
    status: 'Completed',
    badgeClass: 'badge-success'
  },
  {
    id: 'APT-3003',
    patientName: 'Nusrat Jahan',
    patientId: 'PAT-1003',
    doctorName: 'Dr. Imran Hossain',
    department: 'Orthopedics',
    date: 'Today',
    timeSlot: '11:00 AM',
    status: 'Booked',
    badgeClass: 'badge-warning'
  },
  {
    id: 'APT-3004',
    patientName: 'Tariq Aziz',
    patientId: 'PAT-1007',
    doctorName: 'Dr. Farhan Ahmed',
    department: 'Cardiology',
    date: 'Today',
    timeSlot: '11:45 AM',
    status: 'Cancelled',
    badgeClass: 'badge-danger'
  }
];

let currentApptTab = 'All';
let currentApptSearch = '';

async function renderAppointments(): Promise<void> {
  showLoading();
  setActiveNav('appointments');

  const filtered = DEFAULT_APPOINTMENTS_LIST.filter(a => {
    const matchSearch = currentApptSearch === '' ||
      a.patientName.toLowerCase().includes(currentApptSearch.toLowerCase()) ||
      a.doctorName.toLowerCase().includes(currentApptSearch.toLowerCase()) ||
      a.id.toLowerCase().includes(currentApptSearch.toLowerCase());
    const matchTab = currentApptTab === 'All' || a.status === currentApptTab;
    return matchSearch && matchTab;
  });

  const tableRows = filtered.map(a => `
    <tr>
      <td style="font-weight:700;color:var(--text-primary);font-size:13px">${a.id}</td>
      <td>
        <div style="font-weight:700;color:var(--text-primary);font-size:13px">${a.patientName}</div>
        <div style="font-size:11px;color:var(--text-secondary)">${a.patientId}</div>
      </td>
      <td>
        <div style="font-weight:600;font-size:13px">${a.doctorName}</div>
        <div style="font-size:11px;color:var(--text-secondary)">${a.department}</div>
      </td>
      <td style="font-size:13px;color:var(--text-secondary)">${a.date}</td>
      <td>
        <div style="font-weight:700;font-size:13px">${a.timeSlot}</div>
      </td>
      <td>
        <span class="badge ${a.badgeClass}">● ${a.status}</span>
      </td>
      <td>
        <button class="page-btn" style="padding:4px 9px" onclick="window['viewApptDetail']('${a.id}')">View</button>
      </td>
    </tr>`).join('');

  mainEl().innerHTML = `
    <!-- Header -->
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="page-title" style="font-size:24px;font-weight:800">Appointments</div>
          <span class="badge badge-info" style="font-size:11px;padding:3px 8px">Clinical Module 04</span>
        </div>
        <div class="page-subtitle" style="font-size:13px;color:var(--text-secondary)">
          Schedule, triage, and track doctor consultations.
        </div>
      </div>
      <div style="display:flex;gap:10px">
        <button class="btn btn-secondary" onclick="window['exportApptRoster']()">📄 Export Roster</button>
        <button class="btn btn-primary" style="background:#0D3D36" onclick="window['focusBookForm']()">+ Book Appointment</button>
      </div>
    </div>

    <!-- 4 Metric KPI Cards -->
    <div class="kpi-four-grid">
      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Today</div>
          <div class="stat-value" style="font-size:28px">84</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Total scheduled consults</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#F3F4F6">📅</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Booked</div>
          <div class="stat-value" style="font-size:28px;color:#0F766E">38</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Awaiting check-in</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#CCFBF1;color:#0F766E">🕒</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Completed</div>
          <div class="stat-value" style="font-size:28px;color:#059669">41</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Sessions finalized</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#ECFDF5;color:#059669">✔</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Cancelled</div>
          <div class="stat-value" style="font-size:28px;color:#DC2626">5</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Slots re-opened</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#FEE2E2;color:#DC2626">✖</div>
      </div>
    </div>

    <!-- Filter Bar Card -->
    <div class="appt-filter-bar">
      <div class="appt-filter-top-row">
        <select class="filter-dropdown" style="padding:7px 12px">
          <option>📅 Today: Oct 24, 2024</option>
          <option>Tomorrow</option>
          <option>Next 7 Days</option>
        </select>

        <select class="filter-dropdown" style="padding:7px 12px;min-width:200px">
          <option>All Doctors</option>
          <option>Dr. Farhan Ahmed (Cardiology)</option>
          <option>Dr. Samira Islam (General Medicine)</option>
          <option>Dr. Imran Hossain (Orthopedics)</option>
        </select>

        <div class="filter-pill-group" style="margin-left:auto">
          <button class="filter-pill ${currentApptTab === 'All' ? 'active' : ''}" onclick="window['setApptTab']('All')">All</button>
          <button class="filter-pill ${currentApptTab === 'Booked' ? 'active' : ''}" onclick="window['setApptTab']('Booked')">Booked</button>
          <button class="filter-pill ${currentApptTab === 'Completed' ? 'active' : ''}" onclick="window['setApptTab']('Completed')">Completed</button>
          <button class="filter-pill ${currentApptTab === 'Cancelled' ? 'active' : ''}" onclick="window['setApptTab']('Cancelled')">Cancelled</button>
        </div>
      </div>

      <div class="quick-filter-input">
        <span>🔍</span>
        <input type="text" placeholder="Quick filter ID or Name..." value="${currentApptSearch}"
          oninput="window['onApptSearch'](this.value)" />
      </div>
    </div>

    <!-- SPLIT LAYOUT: Roster Table (Left) + Book New Appointment Form Card (Right) -->
    <div class="appt-split-layout">
      <!-- Left: Roster Card -->
      <div class="card" style="padding:0;overflow:hidden">
        <div style="display:flex;justify-content:space-between;align-items:center;padding:14px 18px;border-bottom:1px solid var(--border-color)">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:700;font-size:15px">Today's Appointments Roster</span>
            <span style="font-size:11px;background:#F3F4F6;padding:2px 6px;border-radius:10px;color:var(--text-secondary)">4 showing</span>
          </div>
          <div style="display:flex;gap:8px">
            <button class="btn-filter-icon" onclick="renderAppointments()">🔄</button>
            <button class="btn-filter-icon">⚙️</button>
          </div>
        </div>

        <table class="data-table">
          <thead style="background:#FAF9F6">
            <tr>
              <th style="padding-left:18px">Appointment ID</th>
              <th>Patient</th>
              <th>Doctor &amp; Department</th>
              <th>Date</th>
              <th>Time Slot</th>
              <th>Status</th>
              <th style="padding-right:18px">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${tableRows || `<tr><td colspan="7" style="text-align:center;padding:30px;color:var(--text-secondary)">No appointments</td></tr>`}
          </tbody>
        </table>

        <div class="pagination-container" style="padding:14px 18px">
          <div style="font-size:12px">Showing <strong>4</strong> of <strong>84</strong> appointments for Oct 24</div>
          <div class="pagination-btns">
            <button class="page-btn" disabled>&lt;</button>
            <button class="page-btn active">1</button>
            <button class="page-btn">2</button>
            <button class="page-btn">3</button>
            <button class="page-btn">&gt;</button>
          </div>
        </div>
      </div>

      <!-- Right: Book New Appointment Sidebar Form -->
      <div class="book-form-card" id="bookFormSidebar">
        <div class="book-form-header">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-size:18px">📅</span>
            <span style="font-weight:800;font-size:16px">Book New Appointment</span>
          </div>
          <span style="font-size:11px;background:#F3F4F6;padding:3px 8px;border-radius:6px;color:var(--text-secondary)">Quick Intake</span>
        </div>

        <div class="form-group">
          <div style="display:flex;justify-content:space-between;margin-bottom:4px">
            <label class="form-label" style="font-weight:700">Patient Selection *</label>
            <a href="javascript:void(0)" style="font-size:11px;color:#0F766E;font-weight:600" onclick="window['showRegisterPatient']()">+ Register Patient</a>
          </div>
          <select class="form-control" id="fApptPatient">
            <option>Ayesha Rahman - PAT-1001</option>
            <option>Tanvir Hasan - PAT-1002</option>
            <option>Nusrat Jahan - PAT-1003</option>
            <option>Tariq Aziz - PAT-1007</option>
          </select>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">
            Verified NHI Active Record • Blood Group: <strong>O+</strong>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" style="font-weight:700">Doctor &amp; Department *</label>
          <select class="form-control" id="fApptDoctor">
            <option>Dr. Farhan Ahmed - Cardiology</option>
            <option>Dr. Samira Islam - General Medicine</option>
            <option>Dr. Imran Hossain - Orthopedics</option>
            <option>Dr. Elena Rostova - Neurology</option>
          </select>
          <div style="font-size:11px;color:#059669;margin-top:4px;font-weight:600">
            ✔ Doctor is On-Duty in Consultation Suite 3B
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label" style="font-weight:700">Date *</label>
            <input class="form-control" type="date" value="2024-10-24" id="fApptDate" />
          </div>
          <div class="form-group">
            <label class="form-label" style="font-weight:700">Time Slot *</label>
            <select class="form-control" id="fApptTime">
              <option>10:30 AM - Available</option>
              <option>11:15 AM - Available</option>
              <option>02:00 PM - Available</option>
              <option>03:30 PM - Available</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" style="font-weight:700">Visit Classification</label>
          <div class="visit-classify-group">
            <div class="visit-chip active" id="chipRoutine" onclick="window['selectChip']('chipRoutine')">
              <div class="visit-chip-title">Routine</div>
              <div class="visit-chip-sub">Regular consult</div>
            </div>
            <div class="visit-chip" id="chipFollow" onclick="window['selectChip']('chipFollow')">
              <div class="visit-chip-title">Follow-up</div>
              <div class="visit-chip-sub">Post-treatment</div>
            </div>
            <div class="visit-chip" id="chipUrgent" onclick="window['selectChip']('chipUrgent')">
              <div class="visit-chip-title">Urgent</div>
              <div class="visit-chip-sub">Priority queue</div>
            </div>
          </div>
        </div>

        <div class="form-group">
          <div style="display:flex;justify-content:space-between;margin-bottom:4px">
            <label class="form-label" style="font-weight:700">Administrative Notes</label>
            <span style="font-size:11px;color:var(--text-muted)">Optional</span>
          </div>
          <textarea class="form-control" rows="3" placeholder="Enter clinical remarks, referral source, triage symptoms, or intake tags..."></textarea>
          <div style="font-size:10px;color:var(--text-secondary);margin-top:4px">
            ⓘ Slot will be reserved for 15 minutes prior to automated release.
          </div>
        </div>

        <div style="display:flex;gap:10px;margin-top:16px">
          <button class="btn btn-primary" style="background:#0F766E;flex:1.4" onclick="window['confirmNewApptBooking']()">
            ✔ Confirm Appointment
          </button>
          <button class="btn btn-secondary" style="flex:1" onclick="renderAppointments()">
            Cancel
          </button>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// BILLING & INVOICES VIEW (Screenshot #3)
// ============================================================
interface StandaloneInvoiceRow {
  billId: string;
  patientName: string;
  patientId: string;
  stayCharges: number;
  stayDaysDesc: string;
  doctorFee: number;
  medLab: number;
  total: number;
  issueDate: string;
  status: 'Pending' | 'Paid' | 'Overdue';
  statusBadge: string;
}

const DEFAULT_STANDALONE_BILLS: StandaloneInvoiceRow[] = [
  {
    billId: 'BILL-5001',
    patientName: 'Ayesha Rahman',
    patientId: 'PAT-1001',
    stayCharges: 450.00,
    stayDaysDesc: '3 days Cabin 302',
    doctorFee: 120.00,
    medLab: 85.00,
    total: 655.00,
    issueDate: 'Oct 24, 2024',
    status: 'Pending',
    statusBadge: 'badge-warning'
  },
  {
    billId: 'BILL-5002',
    patientName: 'Marcus Brody',
    patientId: 'PAT-1002',
    stayCharges: 150.00,
    stayDaysDesc: '3 days General Ward',
    doctorFee: 90.00,
    medLab: 45.00,
    total: 285.00,
    issueDate: 'Oct 23, 2024',
    status: 'Paid',
    statusBadge: 'badge-success'
  },
  {
    billId: 'BILL-5003',
    patientName: 'Nusrat Jahan',
    patientId: 'PAT-1003',
    stayCharges: 1200.00,
    stayDaysDesc: '3 days ICU Bed 04',
    doctorFee: 350.00,
    medLab: 480.00,
    total: 2030.00,
    issueDate: 'Oct 22, 2024',
    status: 'Paid',
    statusBadge: 'badge-success'
  },
  {
    billId: 'BILL-5004',
    patientName: 'Tanvir Hasan',
    patientId: 'PAT-1004',
    stayCharges: 300.00,
    stayDaysDesc: '2 days Cabin 205',
    doctorFee: 100.00,
    medLab: 60.00,
    total: 460.00,
    issueDate: 'Oct 21, 2024',
    status: 'Overdue',
    statusBadge: 'badge-danger'
  },
  {
    billId: 'BILL-5005',
    patientName: 'Rafiqul Islam',
    patientId: 'PAT-1005',
    stayCharges: 800.00,
    stayDaysDesc: '2 days ICU',
    doctorFee: 200.00,
    medLab: 310.00,
    total: 1310.00,
    issueDate: 'Oct 20, 2024',
    status: 'Paid',
    statusBadge: 'badge-success'
  },
  {
    billId: 'BILL-5006',
    patientName: 'Sabina Yasmin',
    patientId: 'PAT-1006',
    stayCharges: 0.00,
    stayDaysDesc: 'Outpatient Consultation',
    doctorFee: 75.00,
    medLab: 40.00,
    total: 115.00,
    issueDate: 'Oct 19, 2024',
    status: 'Paid',
    statusBadge: 'badge-success'
  },
  {
    billId: 'BILL-5007',
    patientName: 'Kazi Anowar',
    patientId: 'PAT-1007',
    stayCharges: 600.00,
    stayDaysDesc: '4 days Cabin 304',
    doctorFee: 160.00,
    medLab: 130.00,
    total: 890.00,
    issueDate: 'Oct 18, 2024',
    status: 'Pending',
    statusBadge: 'badge-warning'
  }
];

let standaloneBillTab = 'All';
let standaloneBillSearch = '';

async function renderBills(): Promise<void> {
  showLoading();
  setActiveNav('bills');

  const filtered = DEFAULT_STANDALONE_BILLS.filter(b => {
    const matchSearch = standaloneBillSearch === '' ||
      b.patientName.toLowerCase().includes(standaloneBillSearch.toLowerCase()) ||
      b.billId.toLowerCase().includes(standaloneBillSearch.toLowerCase()) ||
      b.patientId.toLowerCase().includes(standaloneBillSearch.toLowerCase());
    const matchTab = standaloneBillTab === 'All' ||
      (standaloneBillTab === 'Paid' && b.status === 'Paid') ||
      (standaloneBillTab === 'Pending' && b.status === 'Pending') ||
      (standaloneBillTab === 'Overdue' && b.status === 'Overdue');
    return matchSearch && matchTab;
  });

  const rows = filtered.map(b => `
    <tr>
      <td style="font-weight:700;color:#0F766E;font-size:13px">${b.billId}</td>
      <td>
        <div style="font-weight:700;color:var(--text-primary);font-size:13px">${b.patientName}</div>
        <div style="font-size:11px;color:var(--text-secondary)">${b.patientId}</div>
      </td>
      <td>
        <div style="font-weight:600;font-size:13px">${formatCurrency(b.stayCharges)}</div>
        <div style="font-size:11px;color:var(--text-secondary)">${b.stayDaysDesc}</div>
      </td>
      <td style="font-size:13px">${formatCurrency(b.doctorFee)}</td>
      <td style="font-size:13px">${formatCurrency(b.medLab)}</td>
      <td style="font-weight:800;font-size:14px;color:var(--text-primary)">${formatCurrency(b.total)}</td>
      <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap">${b.issueDate}</td>
      <td>
        <span class="badge ${b.statusBadge}">● ${b.status}</span>
      </td>
      <td>
        <div style="display:flex;align-items:center;gap:4px">
          <button class="page-btn" style="padding:4px 8px" onclick="window['viewStandaloneInvoice']('${b.billId}')">View</button>
          ${b.status === 'Paid'
            ? `<button class="page-btn" style="padding:4px 8px" onclick="window['downloadInvoicePdf']('${b.billId}')">🧾 Receipt</button>`
            : b.status === 'Overdue'
            ? `<button class="page-btn" style="padding:4px 8px;color:#DC2626" onclick="window['remindPatient']('${b.billId}')">Remind</button>
               <button class="btn btn-primary" style="background:#0F766E;padding:4px 9px;font-size:11px" onclick="window['payBill']('${b.billId}')">Collect</button>`
            : `<button class="btn btn-primary" style="background:#0F766E;padding:4px 9px;font-size:11px" onclick="window['payBill']('${b.billId}')">Collect</button>`
          }
        </div>
      </td>
    </tr>`).join('');

  mainEl().innerHTML = `
    <!-- Page Header -->
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <div class="page-title" style="font-size:24px;font-weight:800">Billing &amp; Invoices</div>
        <div class="page-subtitle" style="font-size:13px;color:var(--text-secondary)">
          Track hospital admissions billing, consultation fees, medicine/lab charges, and patient settlements.
        </div>
      </div>
      <div style="display:flex;gap:10px">
        <button class="btn btn-secondary" onclick="window['exportFinancialLedger']()">📄 Export Financial Ledger</button>
        <button class="btn btn-primary" style="background:#0F766E" onclick="window['showGenerateBill']()">+ Generate Manual Bill</button>
      </div>
    </div>

    <!-- 4 Financial KPI Cards -->
    <div class="kpi-four-grid">
      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Total Billed</div>
          <div class="stat-value" style="font-size:26px">$184,650</div>
          <div style="font-size:11px;color:#059669;margin-top:4px;font-weight:600">↗ +12.4% this fiscal month</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#F3F4F6">💳</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Settled &amp; Paid</div>
          <div class="stat-value" style="font-size:26px;color:#059669">$162,400</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">314 completed invoices</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#ECFDF5;color:#059669">✔</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Pending Settlement</div>
          <div class="stat-value" style="font-size:26px;color:#D97706">$14,250</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">18 invoices awaiting payment</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#FFFBEB;color:#D97706">🕒</div>
      </div>

      <div class="appt-kpi-card">
        <div>
          <div class="stat-label">Overdue / Disputed</div>
          <div class="stat-value" style="font-size:26px;color:#DC2626">$8,000</div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">10 delayed settlements</div>
        </div>
        <div class="appt-kpi-icon-box" style="background:#FEE2E2;color:#DC2626">⚠️</div>
      </div>
    </div>

    <!-- Filters Bar -->
    <div class="billing-filter-wrap">
      <!-- Tabs -->
      <div class="filter-pill-group">
        <button class="filter-pill ${standaloneBillTab === 'All' ? 'active' : ''}" onclick="window['setStandaloneBillTab']('All')">All Invoices <span style="font-size:10px;background:#E5E7EB;padding:1px 5px;border-radius:10px">342</span></button>
        <button class="filter-pill ${standaloneBillTab === 'Paid' ? 'active' : ''}" onclick="window['setStandaloneBillTab']('Paid')">Paid <span style="font-size:10px;background:#DCFCE7;color:#166534;padding:1px 5px;border-radius:10px">314</span></button>
        <button class="filter-pill ${standaloneBillTab === 'Pending' ? 'active' : ''}" onclick="window['setStandaloneBillTab']('Pending')">Pending <span style="font-size:10px;background:#FEF3C7;color:#92400E;padding:1px 5px;border-radius:10px">18</span></button>
        <button class="filter-pill ${standaloneBillTab === 'Overdue' ? 'active' : ''}" onclick="window['setStandaloneBillTab']('Overdue')">Overdue <span style="font-size:10px;background:#FEE2E2;color:#991B1B;padding:1px 5px;border-radius:10px">10</span></button>
      </div>

      <!-- Controls -->
      <div style="display:flex;align-items:center;gap:10px">
        <div class="date-range-dropdown">
          <span>📅</span> Oct 01, 2024 - Oct 25, 2024 ▾
        </div>
        <select class="filter-dropdown">
          <option>All Methods</option>
          <option>Cash</option>
          <option>Credit Card</option>
          <option>Insurance</option>
        </select>
        <button class="btn-filter-icon">⚙️</button>
      </div>
    </div>

    <!-- Search input -->
    <div class="quick-filter-input" style="margin-bottom:18px">
      <span>🔍</span>
      <input type="text" placeholder="Filter by Bill ID (e.g. BILL-5001), Patient name, or National ID..."
        value="${standaloneBillSearch}" oninput="window['onStandaloneBillSearch'](this.value)" />
    </div>

    <!-- Bills Table Card -->
    <div class="card" style="padding:0;overflow:hidden">
      <table class="data-table">
        <thead style="background:#FAF9F6">
          <tr>
            <th style="padding-left:18px">BILL ID</th>
            <th>PATIENT NAME &amp; ID</th>
            <th>STAY / ROOM CHARGES</th>
            <th>DOCTOR FEE</th>
            <th>DIAGNOSTICS &amp; MEDS</th>
            <th>TOTAL AMOUNT</th>
            <th>ISSUE DATE</th>
            <th>STATUS</th>
            <th style="padding-right:18px">ACTIONS</th>
          </tr>
        </thead>
        <tbody>
          ${rows || `<tr><td colspan="9" style="text-align:center;padding:30px;color:var(--text-secondary)">No bills found</td></tr>`}
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination-container" style="padding:14px 18px">
        <div>Showing <strong>1–${filtered.length}</strong> of <strong>342</strong> bills &nbsp; Rows per page: <strong>7</strong> &nbsp; <span style="color:var(--text-muted)">All figures displayed in USD ($)</span></div>
        <div class="pagination-btns">
          <button class="page-btn" disabled>&lt;</button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">3</button>
          <span style="padding:0 4px;color:var(--text-secondary)">...</span>
          <button class="page-btn">49</button>
          <button class="page-btn">&gt;</button>
        </div>
      </div>
    </div>
  `;
}

interface DetailedInvoice {
  billId: string;
  patientName: string;
  patientId: string;
  stayCharges: number;
  stayDays: number;
  roomDesc: string;
  doctorFee: number;
  medLabCharges: number;
  total: number;
  status: 'Pending' | 'Paid' | 'Unpaid';
  badgeClass: string;
  attendingDoctor: string;
  invoiceDate: string;
  dueDate: string;
  roomLocation: string;
  age: number;
  items: Array<{
    service: string;
    subtext: string;
    qtyDays: string;
    rate: number;
    subtotal: number;
  }>;
}

const MOCK_INVOICES: DetailedInvoice[] = [
  {
    billId: 'BILL-5001',
    patientName: 'Ayesha Rahman',
    patientId: 'PAT-1001',
    stayCharges: 450.00,
    stayDays: 3,
    roomDesc: 'Cabin',
    doctorFee: 120.00,
    medLabCharges: 85.00,
    total: 655.00,
    status: 'Pending',
    badgeClass: 'badge-warning',
    attendingDoctor: 'Dr. Sarah Jenkins',
    invoiceDate: 'Oct 24, 2024',
    dueDate: 'Oct 29, 2024',
    roomLocation: 'Room 304 (Floor 3)',
    age: 34,
    items: [
      { service: 'Private Cabin Suite', subtext: 'Room #304 Stays & Nursing Care', qtyDays: '3 Days', rate: 150.00, subtotal: 450.00 },
      { service: 'Doctor Consultation', subtext: 'Senior Consultant Rounds (x3)', qtyDays: '3 Visits', rate: 40.00, subtotal: 120.00 },
      { service: 'Pharmacy & Lab Panels', subtext: 'CBC, Liver Panel & Post-op Meds', qtyDays: '1 Set', rate: 85.00, subtotal: 85.00 }
    ]
  },
  {
    billId: 'BILL-5002',
    patientName: 'Marcus Vance',
    patientId: 'PAT-0984',
    stayCharges: 150.00,
    stayDays: 3,
    roomDesc: 'Ward',
    doctorFee: 90.00,
    medLabCharges: 45.00,
    total: 285.00,
    status: 'Unpaid',
    badgeClass: 'badge-danger',
    attendingDoctor: 'Dr. Farhan Kabir',
    invoiceDate: 'Oct 23, 2024',
    dueDate: 'Oct 28, 2024',
    roomLocation: 'General Ward 12 (Floor 1)',
    age: 42,
    items: [
      { service: 'General Ward Bed', subtext: 'Ward 12 Bed 04 Accommodation', qtyDays: '3 Days', rate: 50.00, subtotal: 150.00 },
      { service: 'Attending Physician Rounds', subtext: 'Daily clinical evaluation', qtyDays: '3 Visits', rate: 30.00, subtotal: 90.00 },
      { service: 'Routine Diagnostic Lab', subtext: 'Blood Chemistry Panel', qtyDays: '1 Set', rate: 45.00, subtotal: 45.00 }
    ]
  },
  {
    billId: 'BILL-5003',
    patientName: 'Elena Rostov',
    patientId: 'PAT-1102',
    stayCharges: 1200.00,
    stayDays: 3,
    roomDesc: 'ICU',
    doctorFee: 350.00,
    medLabCharges: 480.00,
    total: 2030.00,
    status: 'Paid',
    badgeClass: 'badge-success',
    attendingDoctor: 'Dr. A. Q. Chowdhury',
    invoiceDate: 'Oct 22, 2024',
    dueDate: 'Oct 27, 2024',
    roomLocation: 'ICU Bed 02 (Floor 4)',
    age: 58,
    items: [
      { service: 'ICU Critical Care Suite', subtext: 'Ventilator & 24/7 Monitoring', qtyDays: '3 Days', rate: 400.00, subtotal: 1200.00 },
      { service: 'Intensivist Specialist Care', subtext: 'Critical care physician consults', qtyDays: '1 Case', rate: 350.00, subtotal: 350.00 },
      { service: 'Critical Pharmaceuticals', subtext: 'IV Infusions & Emergency Panels', qtyDays: '1 Batch', rate: 480.00, subtotal: 480.00 }
    ]
  },
  {
    billId: 'BILL-5004',
    patientName: 'David Chen',
    patientId: 'PAT-0849',
    stayCharges: 300.00,
    stayDays: 2,
    roomDesc: 'Cabin',
    doctorFee: 110.00,
    medLabCharges: 65.00,
    total: 475.00,
    status: 'Pending',
    badgeClass: 'badge-warning',
    attendingDoctor: 'Dr. Sarah Jenkins',
    invoiceDate: 'Oct 24, 2024',
    dueDate: 'Oct 29, 2024',
    roomLocation: 'Room 208 (Floor 2)',
    age: 49,
    items: [
      { service: 'Private Single Cabin', subtext: 'Room #208 Stay & Nursing', qtyDays: '2 Days', rate: 150.00, subtotal: 300.00 },
      { service: 'Specialist Medical Visit', subtext: 'Consultation & Prescription', qtyDays: '2 Visits', rate: 55.00, subtotal: 110.00 },
      { service: 'Diagnostic Ultrasound', subtext: 'Abdominal scan', qtyDays: '1 Procedure', rate: 65.00, subtotal: 65.00 }
    ]
  }
];

let selectedInvoiceId = 'BILL-5001';
let currentBillingTab = 'All';

async function renderRooms(): Promise<void> {
  await renderHospitalStayAndBilling('rooms');
}

async function renderHospitalStayAndBilling(activeNavKey: 'rooms' | 'bills'): Promise<void> {
  showLoading();
  setActiveNav(activeNavKey);

  const currentInvoice = MOCK_INVOICES.find(inv => inv.billId === selectedInvoiceId) || MOCK_INVOICES[0]!;

  const filteredInvoices = MOCK_INVOICES.filter(inv => {
    if (currentBillingTab === 'Unpaid') return inv.status === 'Unpaid';
    if (currentBillingTab === 'Paid') return inv.status === 'Paid';
    if (currentBillingTab === 'Pending') return inv.status === 'Pending';
    return true;
  });

  const invoiceTableRows = filteredInvoices.map(inv => {
    const isSelected = inv.billId === currentInvoice.billId;
    return `
      <tr style="cursor:pointer;background:${isSelected ? '#F0FDF4' : 'transparent'}" onclick="window['selectInvoice']('${inv.billId}')">
        <td style="font-weight:700;color:#0F766E;font-size:13px">${inv.billId}</td>
        <td>
          <div style="font-weight:700;color:var(--text-primary);font-size:13px">${inv.patientName}</div>
          <div style="font-size:11px;color:var(--text-secondary)">${inv.patientId}</div>
        </td>
        <td>
          <div style="font-weight:600;font-size:13px">${formatCurrency(inv.stayCharges)}</div>
          <div style="font-size:11px;color:var(--text-secondary)">${inv.stayDays} days ${inv.roomDesc}</div>
        </td>
        <td style="font-size:13px">${formatCurrency(inv.doctorFee)}</td>
        <td style="font-size:13px">${formatCurrency(inv.medLabCharges)}</td>
        <td style="font-weight:800;font-size:14px;color:var(--text-primary)">${formatCurrency(inv.total)}</td>
      </tr>`;
  }).join('');


  // Itemized clinical charges for receipt
  const receiptItemsRows = currentInvoice.items.map(item => `
    <tr>
      <td>
        <div style="font-weight:600;color:var(--text-primary)">${item.service}</div>
        <div style="font-size:10px;color:var(--text-secondary)">${item.subtext}</div>
      </td>
      <td style="color:var(--text-secondary)">${item.qtyDays}</td>
      <td style="color:var(--text-secondary)">${formatCurrency(item.rate)}</td>
      <td style="font-weight:600;color:var(--text-primary)">${formatCurrency(item.subtotal)}</td>
    </tr>
  `).join('');

  mainEl().innerHTML = `
    <!-- Top Breadcrumb & Page Actions -->
    <div class="breadcrumb-nav">
      Clinical Operations &gt; <a href="javascript:void(0)" onclick="navigateTo('rooms')">Rooms, Stays &amp; Invoicing</a>
    </div>

    <div class="page-header" style="margin-bottom:20px">
      <div>
        <div class="page-title" style="font-size:24px;font-weight:800">Hospital Stay &amp; Financial Management</div>
      </div>
      <div style="display:flex;gap:10px">
        <button class="btn btn-secondary" onclick="window['exportDailyLedger']()">📄 Export Daily Ledger</button>
        <button class="btn btn-primary" style="background:#0D3D36" onclick="window['showAssignRoomModalWrapper']()">🛏️ Assign Room</button>
      </div>
    </div>

    <!-- SECTION 1: Rooms & Stays Overview -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <div style="font-size:16px;font-weight:700;display:flex;align-items:center;gap:8px">
        <span>🛏️</span> Rooms &amp; Stays Overview
      </div>
      <div style="font-size:11px;color:var(--text-secondary)">Live Ward Synchronizer: Active</div>
    </div>

    <!-- 4 Overview Metric Cards -->
    <div class="overview-top-kpis">
      <div class="metric-summary-card">
        <div>
          <div class="stat-label">Total Capacity</div>
          <div class="stat-value" style="font-size:26px">90 <span style="font-size:14px;font-weight:500;color:var(--text-secondary)">Beds</span></div>
          <div style="font-size:11px;color:#0F766E;margin-top:4px">Across 3 Specialized Units</div>
        </div>
        <div class="stat-icon">🏥</div>
      </div>

      <div class="metric-summary-card">
        <div>
          <div class="stat-label">Occupied</div>
          <div class="stat-value" style="font-size:26px">76 <span class="badge badge-success" style="font-size:11px;vertical-align:middle">84%</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">14 Discharges Scheduled</div>
        </div>
        <div class="stat-icon">👥</div>
      </div>

      <div class="metric-summary-card">
        <div>
          <div class="stat-label">Available</div>
          <div class="stat-value" style="font-size:26px">14 <span style="font-size:14px;font-weight:500;color:var(--text-secondary)">Beds</span></div>
          <div style="font-size:11px;color:#059669;margin-top:4px">✔ Ready for Triage</div>
        </div>
        <div class="stat-icon">🛏️</div>
      </div>

      <div class="metric-summary-card">
        <div>
          <div class="stat-label">Cleaning / Prep</div>
          <div class="stat-value" style="font-size:26px">0 <span style="font-size:14px;font-weight:500;color:var(--text-secondary)">Pending</span></div>
          <div style="font-size:11px;color:var(--text-secondary);margin-top:4px">Sanitization team standby</div>
        </div>
        <div class="stat-icon">🧼</div>
      </div>
    </div>

    <!-- 3 Room Tier Cards with Progress Bars -->
    <div class="room-tier-cards-grid">
      <!-- General Ward Card -->
      <div class="room-tier-card">
        <div class="room-tier-header">
          <div>
            <div style="display:flex;align-items:center;gap:6px">
              <span>🛏️</span>
              <span class="room-tier-title">General Ward</span>
            </div>
            <div class="room-tier-sub">Wing A &amp; B — Shared Facility</div>
          </div>
          <span class="price-pill">$50/day</span>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:12px;margin:12px 0 6px">
          <span style="color:var(--text-secondary)">Occupancy:</span>
          <strong>38 / 45 Beds occupied</strong>
        </div>
        <div class="progress-bar-container" style="height:6px">
          <div class="progress-bar amber" style="width:84.4%"></div>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:11px;margin:8px 0 14px">
          <span style="color:#059669;font-weight:600">7 Beds Available</span>
          <span style="color:var(--text-secondary)">84.4% Rate</span>
        </div>
        <button class="btn btn-outline btn-full" style="padding:7px;font-size:12px" onclick="navigateTo('rooms')">👁 View Ward Beds</button>
      </div>

      <!-- Private Cabins Card -->
      <div class="room-tier-card">
        <div class="room-tier-header">
          <div>
            <div style="display:flex;align-items:center;gap:6px">
              <span>🚪</span>
              <span class="room-tier-title">Private Cabins</span>
            </div>
            <div class="room-tier-sub">Floor 2 — Single Suites</div>
          </div>
          <span class="price-pill">$150/day</span>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:12px;margin:12px 0 6px">
          <span style="color:var(--text-secondary)">Occupancy:</span>
          <strong>24 / 28 occupied</strong>
        </div>
        <div class="progress-bar-container" style="height:6px">
          <div class="progress-bar green" style="width:85.7%"></div>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:11px;margin:8px 0 14px">
          <span style="color:#059669;font-weight:600">4 Cabins Available</span>
          <span style="color:var(--text-secondary)">85.7% Rate</span>
        </div>
        <button class="btn btn-outline btn-full" style="padding:7px;font-size:12px" onclick="navigateTo('rooms')">👁 View Cabin Allocation</button>
      </div>

      <!-- ICU / Critical Card -->
      <div class="room-tier-card">
        <div class="room-tier-header">
          <div>
            <div style="display:flex;align-items:center;gap:6px">
              <span>🚨</span>
              <span class="room-tier-title">ICU / Critical</span>
            </div>
            <div class="room-tier-sub">North Wing — Intensive Care</div>
          </div>
          <span class="price-pill">$400/day</span>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:12px;margin:12px 0 6px">
          <span style="color:var(--text-secondary)">Occupancy:</span>
          <strong>14 / 17 occupied</strong>
        </div>
        <div class="progress-bar-container" style="height:6px">
          <div class="progress-bar red" style="width:82.3%"></div>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:11px;margin:8px 0 14px">
          <span style="color:#D97706;font-weight:600">3 Beds Available (Monitored)</span>
          <span style="color:var(--text-secondary)">82.3% Rate</span>
        </div>
        <button class="btn btn-outline btn-full" style="padding:7px;font-size:12px" onclick="navigateTo('rooms')">👁 View Critical Monitor</button>
      </div>
    </div>

    <!-- SECTION 2: Hospital Billing Records & Invoicing Header -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
      <div style="font-size:16px;font-weight:700;display:flex;align-items:center;gap:8px">
        <span>💳</span> Hospital Billing Records &amp; Invoicing
      </div>
      <div style="display:flex;gap:8px">
        <button class="btn btn-secondary" style="font-size:12px;padding:6px 12px">⚡ Advanced Filter</button>
        <button class="btn btn-primary" style="background:#0F766E;font-size:12px;padding:6px 14px" onclick="window['showGenerateBill']()">+ Generate Manual Bill</button>
      </div>
    </div>

    <!-- Filter Tabs (All Bills 342, Unpaid 18, Paid 314, Pending 10) -->
    <div class="filter-tabs" style="margin-bottom:14px">
      <button class="filter-tab ${currentBillingTab === 'All' ? 'active' : ''}" onclick="window['setBillingTab']('All')">All Bills <span style="font-size:10px;background:#E5E7EB;padding:1px 5px;border-radius:10px;margin-left:4px">342</span></button>
      <button class="filter-tab ${currentBillingTab === 'Unpaid' ? 'active' : ''}" onclick="window['setBillingTab']('Unpaid')">Unpaid <span style="font-size:10px;background:#FEE2E2;color:#991B1B;padding:1px 5px;border-radius:10px;margin-left:4px">18</span></button>
      <button class="filter-tab ${currentBillingTab === 'Paid' ? 'active' : ''}" onclick="window['setBillingTab']('Paid')">Paid <span style="font-size:10px;background:#DCFCE7;color:#166534;padding:1px 5px;border-radius:10px;margin-left:4px">314</span></button>
      <button class="filter-tab ${currentBillingTab === 'Pending' ? 'active' : ''}" onclick="window['setBillingTab']('Pending')">Pending <span style="font-size:10px;background:#FEF3C7;color:#92400E;padding:1px 5px;border-radius:10px;margin-left:4px">10</span></button>
    </div>

    <!-- SPLIT LAYOUT: Invoices Table (Left) + Detailed Invoice Slip Preview (Right) -->
    <div class="bill-split-layout">
      <!-- Left: Invoices Table -->
      <div class="card" style="padding:0;overflow:hidden">
        <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 16px;border-bottom:1px solid var(--border-color);font-size:12px">
          <div>Showing <strong>4</strong> of <strong>342</strong> Billing Invoices</div>
          <div style="color:#059669;font-weight:600">Auto-reconciliation: ● Synced</div>
        </div>

        <table class="data-table">
          <thead style="background:#FAF9F6">
            <tr>
              <th style="padding-left:16px">BILL ID</th>
              <th>PATIENT</th>
              <th>STAY CHARGES</th>
              <th>DOCTOR</th>
              <th>MED / LAB</th>
              <th style="padding-right:16px">TOTAL</th>
            </tr>
          </thead>
          <tbody>
            ${invoiceTableRows}
          </tbody>
        </table>

        <!-- Table Pagination -->
        <div class="pagination-container" style="padding:12px 16px">
          <div style="font-size:12px">Showing 1 to 4 of 342 records</div>
          <div class="pagination-btns">
            <button class="page-btn" disabled>&lt;</button>
            <button class="page-btn active">1</button>
            <button class="page-btn">2</button>
            <button class="page-btn">3</button>
            <span style="padding:0 4px">...</span>
            <button class="page-btn">&gt;</button>
          </div>
        </div>
      </div>

      <!-- Right: Detailed Invoice Slip Receipt -->
      <div class="invoice-receipt-card">
        <div class="receipt-header">
          <div style="display:flex;align-items:center;gap:8px">
            <img src="assets/logo.png" alt="MediTrack" style="width:26px;height:26px;border-radius:6px;object-fit:cover" />
            <div>
              <div style="font-weight:800;font-size:14px;color:var(--text-primary)">MediTrack</div>
              <div style="font-size:9px;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.5px">Hospital Management System</div>
            </div>
          </div>
          <div style="text-align:right">
            <span class="badge ${currentInvoice.badgeClass}" style="text-transform:uppercase;font-size:10px;font-weight:700">PENDING SETTLEMENT</span>
            <div style="font-size:11px;color:var(--text-secondary);margin-top:3px">#${currentInvoice.billId}</div>
          </div>
        </div>

        <!-- Meta Grid -->
        <div class="receipt-meta-grid">
          <div>
            <div style="font-size:10px;color:var(--text-secondary);text-transform:uppercase">Billed To:</div>
            <div class="receipt-patient-title">${currentInvoice.patientName}</div>
            <div style="font-size:11px;color:var(--text-secondary);margin-top:2px">
              MRN: ${currentInvoice.patientId} · Age: ${currentInvoice.age} / F
            </div>
            <div style="font-size:11px;color:var(--text-secondary)">
              Attending: <strong>${currentInvoice.attendingDoctor}</strong>
            </div>
          </div>
          <div style="text-align:right">
            <div style="font-size:10px;color:var(--text-secondary);text-transform:uppercase">Invoice Details:</div>
            <div style="font-size:11px;color:var(--text-primary);font-weight:600">Date: ${currentInvoice.invoiceDate}</div>
            <div style="font-size:11px;color:var(--text-secondary)">Due Date: ${currentInvoice.dueDate}</div>
            <div style="font-size:11px;color:#0F766E;font-weight:600;margin-top:2px">${currentInvoice.roomLocation}</div>
          </div>
        </div>

        <div style="font-size:11px;font-weight:700;text-transform:uppercase;color:var(--text-secondary);letter-spacing:0.5px;margin-bottom:8px">
          Itemized Clinical Charges
        </div>

        <!-- Itemized Table -->
        <table class="receipt-items-table">
          <thead>
            <tr>
              <th>Service Description</th>
              <th>Qty / Days</th>
              <th>Rate</th>
              <th>Subtotal</th>
            </tr>
          </thead>
          <tbody>
            ${receiptItemsRows}
          </tbody>
        </table>

        <!-- Charges Summary -->
        <div style="font-size:12px;margin-top:14px">
          <div style="display:flex;justify-content:space-between;margin-bottom:6px">
            <span style="color:var(--text-secondary)">Subtotal Charges:</span>
            <strong>${formatCurrency(currentInvoice.total)}</strong>
          </div>
          <div style="display:flex;justify-content:space-between;margin-bottom:6px">
            <span style="color:var(--text-secondary)">Hospital Institutional Tax (0%):</span>
            <span>$0.00</span>
          </div>
          <div style="display:flex;justify-content:space-between;margin-bottom:6px">
            <span style="color:var(--text-secondary)">Insurance Co-Pay Pre-authorization:</span>
            <span style="color:#059669;font-weight:600">Accepted</span>
          </div>
        </div>

        <!-- Total Due Banner -->
        <div class="receipt-total-row">
          <div style="font-size:15px;font-weight:800;color:var(--text-primary)">Total Due:</div>
          <div style="font-size:22px;font-weight:800;color:#0F766E">${formatCurrency(currentInvoice.total)}</div>
        </div>

        <!-- Action Buttons -->
        <div class="receipt-action-btns">
          <button class="btn btn-secondary" onclick="window.print()">🖨️ Print Receipt</button>
          <button class="btn btn-primary" style="background:#0F766E" onclick="window['downloadInvoicePdf']('${currentInvoice.billId}')">📥 Download PDF</button>
        </div>

        <button class="btn btn-primary btn-full" style="background:#0D3D36;margin-top:10px" onclick="window['payBill']('${currentInvoice.billId}')">
          💳 Collect Payment &amp; Discharge
        </button>
      </div>
    </div>

    <!-- Bottom Compliance and Hospital Version Bar -->
    <div class="footer-compliance-bar">
      <div>● MediTrack Clinical Information System v4.8 • Regional Hospital Node #03</div>
      <div>HIPAA Compliance Portal</div>
      <div>Pharmacy Formulary v34.2</div>
      <div>Emergency Support: Ext 4900</div>
    </div>
  `;
}


// ============================================================
// MODAL FORMS
// ============================================================

function showRegisterPatient(): void {
  showModal(`
    <div class="modal-header">
      <div class="modal-title">Register New Patient</div>
      <button class="modal-close" onclick="window['closeModal']()">×</button>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Full Name *</label>
        <input class="form-control" id="pName" placeholder="e.g. John Doe" />
      </div>
      <div class="form-group">
        <label class="form-label">Age *</label>
        <input class="form-control" id="pAge" type="number" placeholder="25" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Phone</label>
        <input class="form-control" id="pPhone" placeholder="+1-555-0000" />
      </div>
      <div class="form-group">
        <label class="form-label">Blood Group</label>
        <select class="form-control" id="pBlood">
          <option>A+</option><option>A-</option><option>B+</option><option>B-</option>
          <option>O+</option><option>O-</option><option>AB+</option><option>AB-</option>
        </select>
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">Email</label>
      <input class="form-control" id="pEmail" placeholder="patient@email.com" />
    </div>
    <div class="form-group">
      <label class="form-label">Medical History / Condition</label>
      <input class="form-control" id="pHistory" placeholder="e.g. Hypertension, Diabetes" />
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="window['closeModal']()">Cancel</button>
      <button class="btn btn-primary" onclick="window['submitRegisterPatient']()">Register Patient</button>
    </div>`);
}

async function submitRegisterPatient(): Promise<void> {
  const name    = (document.getElementById('pName') as HTMLInputElement).value.trim();
  const age     = parseInt((document.getElementById('pAge') as HTMLInputElement).value);
  const phone   = (document.getElementById('pPhone') as HTMLInputElement).value.trim();
  const email   = (document.getElementById('pEmail') as HTMLInputElement).value.trim();
  const blood   = (document.getElementById('pBlood') as HTMLSelectElement).value;
  const history = (document.getElementById('pHistory') as HTMLInputElement).value.trim();

  if (!name) { showToast('Name is required', 'error'); return; }
  try {
    await PatientAPI.register({ name, age, phone, email, bloodGroup: blood, medicalHistory: history });
    closeModal();
    showToast(`Patient "${name}" registered successfully!`, 'success');
    renderPatients();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

function showAddDoctor(): void {
  showModal(`
    <div class="modal-header">
      <div class="modal-title">Add New Doctor</div>
      <button class="modal-close" onclick="window['closeModal']()">×</button>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Full Name *</label>
        <input class="form-control" id="dName" placeholder="Dr. Jane Smith" />
      </div>
      <div class="form-group">
        <label class="form-label">Age</label>
        <input class="form-control" id="dAge" type="number" placeholder="35" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Specialization</label>
        <select class="form-control" id="dSpec">
          <option value="CARDIOLOGY">Cardiology</option>
          <option value="NEUROLOGY">Neurology</option>
          <option value="ORTHOPEDICS">Orthopedics</option>
          <option value="GENERAL_MEDICINE">General Medicine</option>
          <option value="PEDIATRICS">Pediatrics</option>
          <option value="ONCOLOGY">Oncology</option>
          <option value="RADIOLOGY">Radiology</option>
          <option value="EMERGENCY_MEDICINE">Emergency Medicine</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Consultation Fee ($)</label>
        <input class="form-control" id="dFee" type="number" placeholder="800" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Department</label>
        <input class="form-control" id="dDept" placeholder="Cardiology Unit" />
      </div>
      <div class="form-group">
        <label class="form-label">Phone</label>
        <input class="form-control" id="dPhone" placeholder="+1-555-0000" />
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">Email</label>
      <input class="form-control" id="dEmail" placeholder="doctor@meditrack.io" />
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="window['closeModal']()">Cancel</button>
      <button class="btn btn-primary" onclick="window['submitAddDoctor']()">Add Doctor</button>
    </div>`);
}

async function submitAddDoctor(): Promise<void> {
  const name = (document.getElementById('dName') as HTMLInputElement).value.trim();
  const age  = parseInt((document.getElementById('dAge') as HTMLInputElement).value) || 30;
  const spec = (document.getElementById('dSpec') as HTMLSelectElement).value;
  const fee  = parseFloat((document.getElementById('dFee') as HTMLInputElement).value) || 500;
  const dept = (document.getElementById('dDept') as HTMLInputElement).value.trim();
  const phone= (document.getElementById('dPhone') as HTMLInputElement).value.trim();
  const email= (document.getElementById('dEmail') as HTMLInputElement).value.trim();

  if (!name) { showToast('Name is required', 'error'); return; }
  try {
    await DoctorAPI.add({ name, age, specialization: spec, consultationFee: fee, department: dept, phone, email });
    closeModal();
    showToast(`Dr. ${name} added successfully!`, 'success');
    renderDoctors();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

async function showBookAppointment(): Promise<void> {
  let patients: Patient[] = [];
  let doctors: Doctor[] = [];
  try {
    [patients, doctors] = await Promise.all([PatientAPI.getAll(), DoctorAPI.getAvailable()]);
  } catch { /* ignore */ }

  const patOpts = patients.map(p => `<option value="${p.id}" data-name="${p.name}">${p.name} (${p.id})</option>`).join('');
  const docOpts = doctors.map(d => `<option value="${d.id}" data-name="${d.name}" data-spec="${d.specialization}">${d.name} — ${d.specialization.replace(/_/g,' ')}</option>`).join('');
  const today = new Date().toISOString().slice(0, 16);

  showModal(`
    <div class="modal-header">
      <div class="modal-title">Book Appointment</div>
      <button class="modal-close" onclick="window['closeModal']()">×</button>
    </div>
    <div class="form-group">
      <label class="form-label">Patient *</label>
      <select class="form-control" id="aPatient">${patOpts}</select>
    </div>
    <div class="form-group">
      <label class="form-label">Doctor (Available) *</label>
      <select class="form-control" id="aDoctor">${docOpts}</select>
    </div>
    <div class="form-group">
      <label class="form-label">Date &amp; Time *</label>
      <input class="form-control" id="aDateTime" type="datetime-local" value="${today}" />
    </div>
    <div class="form-group">
      <label class="form-label">Notes</label>
      <input class="form-control" id="aNotes" placeholder="Reason for visit…" />
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="window['closeModal']()">Cancel</button>
      <button class="btn btn-primary" onclick="window['submitBookAppointment']()">Book Appointment</button>
    </div>`);
}

async function submitBookAppointment(): Promise<void> {
  const patSelect = document.getElementById('aPatient') as HTMLSelectElement;
  const docSelect = document.getElementById('aDoctor') as HTMLSelectElement;
  const dtRaw     = (document.getElementById('aDateTime') as HTMLInputElement).value;
  const notes     = (document.getElementById('aNotes') as HTMLInputElement).value.trim();

  const patientId   = patSelect.value;
  const patientName = patSelect.options[patSelect.selectedIndex]?.getAttribute('data-name') ?? '';
  const doctorId    = docSelect.value;
  const dateTime    = dtRaw.replace('T', ' ');

  try {
    await AppointmentAPI.book({ patientId, patientName, doctorId, dateTime, notes });
    closeModal();
    showToast('Appointment booked!', 'success');
    renderAppointments();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

function showGenerateBill(): void {
  showModal(`
    <div class="modal-header">
      <div class="modal-title">Generate Bill</div>
      <button class="modal-close" onclick="window['closeModal']()">×</button>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Patient ID *</label>
        <input class="form-control" id="bPatientId" placeholder="PAT-1001" />
      </div>
      <div class="form-group">
        <label class="form-label">Patient Name *</label>
        <input class="form-control" id="bPatientName" placeholder="Full name" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Room ID (optional)</label>
        <input class="form-control" id="bRoomId" placeholder="GW-001" />
      </div>
      <div class="form-group">
        <label class="form-label">Days Stayed</label>
        <input class="form-control" id="bDays" type="number" placeholder="3" value="1" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Consultation Fee ($)</label>
        <input class="form-control" id="bConsultFee" type="number" placeholder="500" />
      </div>
      <div class="form-group">
        <label class="form-label">Doctor Description</label>
        <input class="form-control" id="bDoctorDesc" placeholder="Dr. Name (Specialty)" />
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">Notes</label>
      <input class="form-control" id="bNotes" placeholder="Additional notes…" />
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="window['closeModal']()">Cancel</button>
      <button class="btn btn-primary" onclick="window['submitGenerateBill']()">Generate Bill</button>
    </div>`);
}

async function submitGenerateBill(): Promise<void> {
  const patientId   = (document.getElementById('bPatientId') as HTMLInputElement).value.trim();
  const patientName = (document.getElementById('bPatientName') as HTMLInputElement).value.trim();
  const roomId      = (document.getElementById('bRoomId') as HTMLInputElement).value.trim();
  const daysStayed  = parseInt((document.getElementById('bDays') as HTMLInputElement).value) || 1;
  const consultationFee  = parseFloat((document.getElementById('bConsultFee') as HTMLInputElement).value) || 0;
  const doctorDescription= (document.getElementById('bDoctorDesc') as HTMLInputElement).value.trim();
  const notes       = (document.getElementById('bNotes') as HTMLInputElement).value.trim();

  if (!patientId || !patientName) { showToast('Patient ID and Name are required', 'error'); return; }
  try {
    const bill = await BillingAPI.generateBill({ patientId, patientName, roomId, daysStayed, consultationFee, doctorDescription, notes });
    closeModal();
    showToast(`Bill generated: ${formatCurrency(bill.totalAmount)}`, 'success');
    renderBills();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

function showAssignRoom(roomId: string): void {
  showModal(`
    <div class="modal-header">
      <div class="modal-title">Assign Patient to Room ${roomId}</div>
      <button class="modal-close" onclick="window['closeModal']()">×</button>
    </div>
    <div class="form-group">
      <label class="form-label">Patient ID *</label>
      <input class="form-control" id="arPatientId" placeholder="PAT-1001" />
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="window['closeModal']()">Cancel</button>
      <button class="btn btn-primary" onclick="window['submitAssignRoom']('${roomId}')">Assign Room</button>
    </div>`);
}

async function submitAssignRoom(roomId: string): Promise<void> {
  const patientId = (document.getElementById('arPatientId') as HTMLInputElement).value.trim();
  if (!patientId) { showToast('Patient ID is required', 'error'); return; }
  try {
    await RoomAPI.assignRoom(roomId, patientId);
    closeModal();
    showToast(`Room ${roomId} assigned to patient ${patientId}`, 'success');
    renderRooms();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

// ============================================================
// ACTION HANDLERS (exposed globally)
// ============================================================

async function toggleDoctor(id: string, checkbox: HTMLInputElement): Promise<void> {
  try {
    const doc = await DoctorAPI.toggleAvailability(id);
    const label = document.getElementById(`avail-${id}`);
    if (label) label.textContent = doc.isAvailable ? 'Available' : 'Off Duty';
    showToast(`${doc.name} is now ${doc.isAvailable ? 'available' : 'off duty'}`, 'info');
  } catch (e) {
    checkbox.checked = !checkbox.checked; // revert
    showToast(`Error: ${e}`, 'error');
  }
}

async function cancelAppt(id: string): Promise<void> {
  try {
    await AppointmentAPI.cancel(id);
    showToast('Appointment cancelled', 'info');
    // Refresh current view
    const active = document.querySelector('.nav-item.active')?.getAttribute('data-view');
    if (active === 'dashboard') renderDashboard(); else renderAppointments();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

async function completeAppt(id: string): Promise<void> {
  try {
    await AppointmentAPI.complete(id);
    showToast('Appointment marked as completed!', 'success');
    const active = document.querySelector('.nav-item.active')?.getAttribute('data-view');
    if (active === 'dashboard') renderDashboard(); else renderAppointments();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

async function payBill(id: string): Promise<void> {
  try {
    const bill = await BillingAPI.markPaid(id);
    showToast(`Bill ${bill.billId} marked as paid! ${formatCurrency(bill.totalAmount)}`, 'success');
    renderBills();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

async function vacateRoom(roomId: string): Promise<void> {
  try {
    await RoomAPI.vacateRoom(roomId);
    showToast(`Room ${roomId} vacated successfully`, 'info');
    renderRooms();
  } catch (e) { showToast(`Error: ${e}`, 'error'); }
}

function viewPatientBills(patientId: string): void {
  navigateTo('bills');
  showToast(`Showing bills for ${patientId}`, 'info');
}

function viewBillDetail(billId: string): void {
  showToast(`Bill ${billId} — details view coming soon`, 'info');
}

function editPatient(patientId: string): void {
  showToast(`Edit patient ${patientId} — coming soon`, 'info');
}

let searchDebounce: ReturnType<typeof setTimeout>;
function searchPatients(query: string): void {
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(() => renderPatients(query), 400);
}

// ============================================================
// GLOBAL WINDOW BINDINGS
// ============================================================
const w = window as unknown as Record<string, unknown>;
w['navigateTo']             = navigateTo;
w['closeModal']             = closeModal;
w['showRegisterPatient']    = showRegisterPatient;
w['submitRegisterPatient']  = submitRegisterPatient;
w['showAddDoctor']          = showAddDoctor;
w['submitAddDoctor']        = submitAddDoctor;
w['showBookAppointment']    = showBookAppointment;
w['submitBookAppointment']  = submitBookAppointment;
w['showGenerateBill']       = showGenerateBill;
w['submitGenerateBill']     = submitGenerateBill;
w['showAssignRoom']         = showAssignRoom;
w['submitAssignRoom']       = submitAssignRoom;
w['toggleDoctor']           = toggleDoctor;
w['cancelAppt']             = cancelAppt;
w['completeAppt']           = completeAppt;
w['payBill']                = payBill;
w['vacateRoom']             = vacateRoom;
w['viewPatientBills']       = viewPatientBills;
w['viewBillDetail']         = viewBillDetail;
w['editPatient']            = editPatient;
w['searchPatients']         = searchPatients;

// Handlers for screenshot #1 (Patients)
w['onPatientSearchChange']   = (val: string) => { currentPatientSearch = val; renderPatients(val); };
w['onPatientGenderChange']   = (val: string) => { currentPatientGenderFilter = val; renderPatients(currentPatientSearch); };
w['onPatientStatusChange']   = (val: string) => { currentPatientStatusFilter = val; renderPatients(currentPatientSearch); };
w['resetPatientFilters']     = () => { currentPatientGenderFilter = 'All'; currentPatientStatusFilter = 'All'; currentPatientSearch = ''; renderPatients(); };
w['viewPatientModal']        = (id: string) => {
  const p = DEFAULT_PATIENTS_MOCK.find(item => item.id === id);
  if (p) {
    showModal(`
      <div class="modal-header">
        <div class="modal-title">${p.name} (${p.id})</div>
        <button class="modal-close" onclick="window['closeModal']()">×</button>
      </div>
      <div class="detail-grid" style="margin-bottom:16px">
        <div class="detail-item"><div class="detail-label">Status</div><div class="detail-value"><span class="badge ${p.statusBadgeClass}">${p.statusText}</span></div></div>
        <div class="detail-item"><div class="detail-label">Blood Group</div><div class="detail-value">${p.bloodGroup}</div></div>
        <div class="detail-item"><div class="detail-label">Age / DOB</div><div class="detail-value">${p.age} yrs (${p.dob})</div></div>
        <div class="detail-item"><div class="detail-label">Phone</div><div class="detail-value">${p.phone}</div></div>
        <div class="detail-item"><div class="detail-label">Assigned Room</div><div class="detail-value">${p.roomIcon} ${p.assignedRoom}</div></div>
        <div class="detail-item"><div class="detail-label">Attending Doctor</div><div class="detail-value">${p.assignedDoctor}</div></div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="window['closeModal']()">Close</button>
      </div>
    `);
  }
};

w['selectInvoice']           = (billId: string) => { selectedInvoiceId = billId; renderHospitalStayAndBilling('rooms'); };
w['setBillingTab']           = (tab: string) => { currentBillingTab = tab; renderHospitalStayAndBilling('rooms'); };
w['exportDailyLedger']       = () => { showToast('Daily Financial Ledger exported successfully!', 'success'); };
w['showAssignRoomModalWrapper'] = () => { showAssignRoom('CAB-001'); };
w['downloadInvoicePdf']      = (billId: string) => { showToast(`Invoice #${billId} downloaded as PDF`, 'info'); };

// Handlers for Doctors (Screenshot #2)
w['setDocTab']               = (tab: string) => { currentDocTab = tab; renderDoctors(); };
w['onDocSearch']             = (val: string) => { currentDocSearch = val; renderDoctors(); };
w['onDocDeptChange']         = (dept: string) => { currentDocDept = dept; renderDoctors(); };
w['exportDoctorRoster']      = () => { showToast('Physician Clinical Roster exported as CSV!', 'success'); };
w['viewDoctorModal']         = (id: string) => {
  const d = DEFAULT_DOCTORS_LIST.find(item => item.id === id);
  if (d) {
    showModal(`
      <div class="modal-header">
        <div class="modal-title">${d.name} (${d.id})</div>
        <button class="modal-close" onclick="window['closeModal']()">×</button>
      </div>
      <div class="detail-grid" style="margin-bottom:16px">
        <div class="detail-item"><div class="detail-label">Status</div><div class="detail-value"><span class="badge ${d.statusBadge}">● ${d.status}</span></div></div>
        <div class="detail-item"><div class="detail-label">Department</div><div class="detail-value">${d.department}</div></div>
        <div class="detail-item"><div class="detail-label">Designation</div><div class="detail-value">${d.title}</div></div>
        <div class="detail-item"><div class="detail-label">Phone</div><div class="detail-value">${d.phone}</div></div>
        <div class="detail-item"><div class="detail-label">Room / Suite</div><div class="detail-value">${d.roomSuite}</div></div>
        <div class="detail-item"><div class="detail-label">Duty Schedule</div><div class="detail-value">${d.dutySchedule} (${d.dutyTiming})</div></div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="window['closeModal']()">Close</button>
      </div>
    `);
  }
};
w['editDoctorModal']         = (id: string) => { showToast(`Edit Doctor ${id} details`, 'info'); };

// Handlers for Appointments (Screenshot #1)
w['setApptTab']              = (tab: string) => { currentApptTab = tab; renderAppointments(); };
w['onApptSearch']            = (val: string) => { currentApptSearch = val; renderAppointments(); };
w['exportApptRoster']        = () => { showToast("Today's Appointments Roster exported!", 'success'); };
w['focusBookForm']           = () => { document.getElementById('bookFormSidebar')?.scrollIntoView({ behavior: 'smooth' }); };
w['selectChip']              = (chipId: string) => {
  document.querySelectorAll('.visit-chip').forEach(c => c.classList.remove('active'));
  document.getElementById(chipId)?.classList.add('active');
};
w['confirmNewApptBooking']   = () => {
  showToast('Appointment successfully scheduled and reserved for 15 minutes!', 'success');
  renderAppointments();
};
w['viewApptDetail']          = (id: string) => {
  const a = DEFAULT_APPOINTMENTS_LIST.find(item => item.id === id);
  if (a) {
    showModal(`
      <div class="modal-header">
        <div class="modal-title">Appointment ${a.id}</div>
        <button class="modal-close" onclick="window['closeModal']()">×</button>
      </div>
      <div class="detail-grid">
        <div class="detail-item"><div class="detail-label">Patient</div><div class="detail-value">${a.patientName} (${a.patientId})</div></div>
        <div class="detail-item"><div class="detail-label">Doctor</div><div class="detail-value">${a.doctorName}</div></div>
        <div class="detail-item"><div class="detail-label">Department</div><div class="detail-value">${a.department}</div></div>
        <div class="detail-item"><div class="detail-label">Time Slot</div><div class="detail-value">${a.timeSlot}</div></div>
        <div class="detail-item"><div class="detail-label">Status</div><div class="detail-value"><span class="badge ${a.badgeClass}">● ${a.status}</span></div></div>
      </div>
      <div class="modal-footer"><button class="btn btn-secondary" onclick="window['closeModal']()">Close</button></div>
    `);
  }
};

// Handlers for Standalone Billing (Screenshot #3)
w['setStandaloneBillTab']    = (tab: string) => { standaloneBillTab = tab; renderBills(); };
w['onStandaloneBillSearch']  = (val: string) => { standaloneBillSearch = val; renderBills(); };
w['exportFinancialLedger']   = () => { showToast('Complete Financial Invoices Ledger exported as CSV!', 'success'); };
w['viewStandaloneInvoice']   = (billId: string) => {
  const b = DEFAULT_STANDALONE_BILLS.find(item => item.billId === billId);
  if (b) {
    showModal(`
      <div class="modal-header">
        <div class="modal-title">Invoice #${b.billId}</div>
        <button class="modal-close" onclick="window['closeModal']()">×</button>
      </div>
      <div class="detail-grid" style="margin-bottom:14px">
        <div class="detail-item"><div class="detail-label">Patient</div><div class="detail-value">${b.patientName} (${b.patientId})</div></div>
        <div class="detail-item"><div class="detail-label">Status</div><div class="detail-value"><span class="badge ${b.statusBadge}">● ${b.status}</span></div></div>
        <div class="detail-item"><div class="detail-label">Stay Charges</div><div class="detail-value">${formatCurrency(b.stayCharges)} (${b.stayDaysDesc})</div></div>
        <div class="detail-item"><div class="detail-label">Doctor Fee</div><div class="detail-value">${formatCurrency(b.doctorFee)}</div></div>
        <div class="detail-item"><div class="detail-label">Diagnostics & Meds</div><div class="detail-value">${formatCurrency(b.medLab)}</div></div>
        <div class="detail-item"><div class="detail-label">Total Amount</div><div class="detail-value" style="font-size:16px;font-weight:800;color:#0F766E">${formatCurrency(b.total)}</div></div>
      </div>
      <div class="modal-footer"><button class="btn btn-secondary" onclick="window['closeModal']()">Close</button></div>
    `);
  }
};
w['remindPatient']           = (billId: string) => { showToast(`Automated payment SMS reminder sent for invoice #${billId}`, 'info'); };



// ============================================================
// INIT — Wire up nav and boot
// ============================================================
function init(): void {
  // Sidebar navigation
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      const view = item.getAttribute('data-view') ?? 'dashboard';
      navigateTo(view);
    });
  });

  // New Admission button
  document.getElementById('newAdmissionBtn')?.addEventListener('click', showRegisterPatient);

  // Global search (debounced)
  document.getElementById('globalSearch')?.addEventListener('input', (e) => {
    const q = (e.target as HTMLInputElement).value.trim();
    if (q.length > 1) {
      navigateTo('patients', q);
    }
  });

  // Start on dashboard
  renderDashboard();
}

document.addEventListener('DOMContentLoaded', init);
