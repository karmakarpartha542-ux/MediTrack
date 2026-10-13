// ============================================================
// types.ts — TypeScript interfaces for MediTrack
// ============================================================

export interface Patient {
  id: string;
  name: string;
  phone: string;
  email: string;
  age: number;
  bloodGroup: string;
  medicalHistory: string;
  admissionDate: string;
  isAdmitted: boolean;
  assignedRoomId: string | null;
  role: string;
}

export interface Doctor {
  id: string;
  name: string;
  phone: string;
  email: string;
  age: number;
  specialization: string;
  consultationFee: number;
  isAvailable: boolean;
  department: string;
  role: string;
}

export type RoomType = 'GENERAL_WARD' | 'CABIN' | 'ICU';
export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Room {
  roomId: string;
  roomNumber: string;
  floor: number;
  isOccupied: boolean;
  patientId: string | null;
  roomType: RoomType;
  dailyRate: number;
  hasAC?: boolean;
  ventilatorRequired?: boolean;
  bedNumber?: string;
}

export interface RoomStats {
  totalRooms: number;
  occupiedCount: number;
  availableCount: number;
  generalWardCount: number;
  generalWardOccupied: number;
  cabinCount: number;
  cabinOccupied: number;
  icuCount: number;
  icuOccupied: number;
}

export interface Appointment {
  appointmentId: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  specialization: string;
  dateTime: string;
  status: AppointmentStatus;
  notes: string;
}

export interface BillItem {
  description: string;
  amount: number;
}

export interface Bill {
  billId: string;
  patientId: string;
  patientName: string;
  items: BillItem[];
  totalAmount: number;
  isPaid: boolean;
  createdDate: string;
  notes: string;
}

export interface DashboardData {
  totalPatients: number;
  totalDoctors: number;
  todayAppointments: number;
  occupiedRooms: number;
  totalRooms: number;
  pendingBillsAmount: number;
  recentAdmissions: Patient[];
  pendingBills: Bill[];
  todayAppointmentsList: Appointment[];
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}
