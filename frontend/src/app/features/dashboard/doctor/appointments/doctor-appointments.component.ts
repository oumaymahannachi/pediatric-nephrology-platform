import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin } from 'rxjs';
import { DoctorService } from '../../../../core/services/doctor.service';
import { Appointment } from '../../../../core/models/appointment.model';

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './doctor-appointments.component.html',
  styleUrl: './doctor-appointments.component.scss'
})
export class DoctorAppointmentsComponent implements OnInit {
  loading = true;
  activeTab: 'all' | 'pending' = 'all';
  appointments: Appointment[] = [];
  pendingAppointments: Appointment[] = [];
  selectedStatus: string = 'all';

  rescheduleId = '';
  rescheduleDate = '';
  rescheduleNotes = '';
  showRescheduleModal = false;

  alertMsg = '';
  alertType: 'success' | 'error' = 'success';

  constructor(private doctorService: DoctorService) {}

  ngOnInit(): void { this.loadData(); }

  loadData(): void {
    this.loading = true;
    forkJoin({
      appointments: this.doctorService.getAppointments(),
      pending: this.doctorService.getPendingAppointments()
    }).subscribe({
      next: ({ appointments, pending }) => {
        this.appointments = appointments;
        this.pendingAppointments = pending;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'warning', ACCEPTED: 'success', REFUSED: 'danger',
      RESCHEDULED: 'info', COMPLETED: 'muted', CANCELLED: 'muted'
    };
    return map[status] || '';
  }

  get filteredAppointments(): Appointment[] {
    if (this.selectedStatus === 'all') {
      return this.appointments;
    }
    return this.appointments.filter(apt => apt.status === this.selectedStatus);
  }

  getStatusCount(status: string): number {
    if (status === 'all') {
      return this.appointments.length;
    }
    return this.appointments.filter(apt => apt.status === status).length;
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
  }

  acceptAppointment(id: string): void {
    this.doctorService.acceptAppointment(id).subscribe({
      next: () => { this.showAlert('Appointment accepted', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to accept', 'error')
    });
  }

  refuseAppointment(id: string): void {
    if (!confirm('Refuse this appointment?')) return;
    this.doctorService.refuseAppointment(id).subscribe({
      next: () => { this.showAlert('Appointment refused', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to refuse', 'error')
    });
  }

  completeAppointment(id: string): void {
    this.doctorService.completeAppointment(id).subscribe({
      next: () => { this.showAlert('Appointment completed', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to complete', 'error')
    });
  }

  openReschedule(apt: Appointment): void {
    this.rescheduleId = apt.id!;
    this.rescheduleDate = '';
    this.rescheduleNotes = '';
    this.showRescheduleModal = true;
  }

  submitReschedule(): void {
    if (!this.rescheduleDate) return;
    this.doctorService.rescheduleAppointment(this.rescheduleId, this.rescheduleDate, this.rescheduleNotes).subscribe({
      next: () => {
        this.showRescheduleModal = false;
        this.showAlert('Appointment rescheduled', 'success');
        this.loadData();
      },
      error: () => this.showAlert('Failed to reschedule', 'error')
    });
  }

  private showAlert(msg: string, type: 'success' | 'error'): void {
    this.alertMsg = msg;
    this.alertType = type;
    setTimeout(() => this.alertMsg = '', 4000);
  }
}
