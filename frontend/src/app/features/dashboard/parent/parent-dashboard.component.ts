import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription, filter, forkJoin } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ParentService } from '../../../core/services/parent.service';
import { LucideAngularModule } from 'lucide-angular';
import { Child } from '../../../core/models/child.model';
import { Appointment } from '../../../core/models/appointment.model';
import { GrowthMeasurement, DietaryRestriction, NutritionalPlan } from '../../../core/models/treatment.model';

@Component({
  selector: 'app-parent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './parent-dashboard.component.html',
  styleUrl: './parent-dashboard.component.scss',
})
export class ParentDashboardComponent implements OnInit, OnDestroy {
  currentUser$ = this.auth.currentUser$;
  mobileMenuOpen = false;
  userMenuOpen = false;
  activeTab = 'dashboard';
  childRouteActive = false;
  private routeSub!: Subscription;

  loading = true;
  stats = { children: 0, appointments: 0 };
  children: Child[] = [];
  appointments: Appointment[] = [];
  availableDoctors: any[] = [];

  showChildModal = false;
  editingChild: Child | null = null;
  childForm!: FormGroup;
  childSaving = false;

  showApptModal = false;
  apptForm!: FormGroup;
  apptSaving = false;

  showDoctorModal = false;
  selectedChildForDoctor: Child | null = null;

  selectedChildId = '';
  measurements: GrowthMeasurement[] = [];
  restrictions: DietaryRestriction[] = [];
  plans: NutritionalPlan[] = [];
  growthSubTab: 'measurements' | 'restrictions' | 'plans' = 'measurements';

  showMeasurementModal = false;
  editingMeasurement: GrowthMeasurement | null = null;
  measurementForm!: FormGroup;
  measurementSaving = false;

  showRestrictionModal = false;
  editingRestriction: DietaryRestriction | null = null;
  restrictionForm!: FormGroup;
  restrictionSaving = false;

  alertMsg = '';
  alertType: 'success' | 'error' = 'success';

  selectedStatus: string = 'all';

  constructor(
    private auth: AuthService,
    private router: Router,
    private parentService: ParentService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.checkChildRoute(this.router.url);
    this.routeSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.checkChildRoute(e.urlAfterRedirects));

    this.childForm = this.fb.group({
      fullName: ['', Validators.required],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      notes: ['']
    });

    this.apptForm = this.fb.group({
      childId: ['', Validators.required],
      doctorId: ['', Validators.required],
      dateTime: ['', Validators.required],
      reason: ['', Validators.required],
      parentNotes: ['']
    });

    this.measurementForm = this.fb.group({
      date: ['', Validators.required],
      weight: ['', [Validators.required, Validators.min(0.1)]],
      height: ['', [Validators.required, Validators.min(1)]],
      headCircumference: [''],
      notes: ['']
    });

    this.restrictionForm = this.fb.group({
      type: ['', Validators.required],
      allergen: ['', Validators.required],
      severity: ['', Validators.required],
      description: [''],
      notes: ['']
    });

    this.loadData();
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private checkChildRoute(url: string): void {
    this.childRouteActive = url !== '/parent' && url.startsWith('/parent/');
  }

  loadData(): void {
    this.loading = true;
    forkJoin({
      dashboard: this.parentService.getDashboard(),
      children: this.parentService.getChildren(),
      appointments: this.parentService.getAppointments(),
      doctors: this.parentService.getAvailableDoctors()
    }).subscribe({
      next: ({ dashboard, children, appointments, doctors }) => {
        this.stats = { children: (dashboard as any).childrenCount || 0, appointments: (dashboard as any).appointmentsCount || 0 };
        this.children = children;
        this.appointments = appointments;
        this.availableDoctors = doctors;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.mobileMenuOpen = false;
    if (this.childRouteActive) this.router.navigate(['/parent']);
    if (tab === 'growth' && this.children.length > 0 && !this.selectedChildId) {
      this.selectedChildId = this.children[0].id!;
      this.loadGrowthData();
    }
  }

  toggleMobileMenu(): void { this.mobileMenuOpen = !this.mobileMenuOpen; }
  toggleUserMenu(): void { this.userMenuOpen = !this.userMenuOpen; }
  logout(): void { this.auth.logout(); }

  openAddChild(): void {
    this.editingChild = null;
    this.childForm.reset();
    this.showChildModal = true;
  }

  openEditChild(child: Child): void {
    this.editingChild = child;
    this.childForm.patchValue({
      fullName: child.fullName,
      dateOfBirth: child.dateOfBirth,
      gender: child.gender,
      notes: child.notes
    });
    this.showChildModal = true;
  }

  saveChild(): void {
    if (this.childForm.invalid) return;
    this.childSaving = true;
    const data = this.childForm.value;
    const obs = this.editingChild
      ? this.parentService.updateChild(this.editingChild.id!, data)
      : this.parentService.addChild(data);
    obs.subscribe({
      next: () => {
        this.showChildModal = false;
        this.childSaving = false;
        this.showAlert(this.editingChild ? 'Child updated' : 'Child added', 'success');
        this.loadData();
      },
      error: () => { this.childSaving = false; this.showAlert('Failed to save child', 'error'); }
    });
  }

  deleteChild(child: Child): void {
    if (!confirm('Delete ' + child.fullName + '?')) return;
    this.parentService.deleteChild(child.id!).subscribe({
      next: () => { this.showAlert('Child deleted', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to delete child', 'error')
    });
  }

  openDoctorModal(child: Child): void {
    this.selectedChildForDoctor = child;
    this.showDoctorModal = true;
  }

  assignDoctor(doctorId: string): void {
    if (!this.selectedChildForDoctor) return;
    this.parentService.assignDoctor(this.selectedChildForDoctor.id!, doctorId).subscribe({
      next: () => { this.showDoctorModal = false; this.showAlert('Doctor assigned', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to assign doctor', 'error')
    });
  }

  removeDoctor(child: Child, doctorId: string): void {
    this.parentService.removeDoctor(child.id!, doctorId).subscribe({
      next: () => { this.showAlert('Doctor removed', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to remove doctor', 'error')
    });
  }

  openApptModal(): void {
    this.apptForm.reset();
    this.showApptModal = true;
  }

  saveAppointment(): void {
    if (this.apptForm.invalid) return;
    this.apptSaving = true;
    this.parentService.createAppointment(this.apptForm.value).subscribe({
      next: () => {
        this.showApptModal = false;
        this.apptSaving = false;
        this.showAlert('Appointment created', 'success');
        this.loadData();
      },
      error: () => { this.apptSaving = false; this.showAlert('Failed to create appointment', 'error'); }
    });
  }

  cancelAppointment(id: string): void {
    if (!confirm('Cancel this appointment?')) return;
    this.parentService.cancelAppointment(id).subscribe({
      next: () => { this.showAlert('Appointment cancelled', 'success'); this.loadData(); },
      error: () => this.showAlert('Failed to cancel appointment', 'error')
    });
  }

  onChildSelected(): void {
    this.loadGrowthData();
  }

  loadGrowthData(): void {
    if (!this.selectedChildId) return;
    forkJoin({
      measurements: this.parentService.getMeasurements(this.selectedChildId),
      restrictions: this.parentService.getRestrictions(this.selectedChildId),
      plans: this.parentService.getChildPlans(this.selectedChildId)
    }).subscribe({
      next: ({ measurements, restrictions, plans }) => {
        this.measurements = measurements;
        this.restrictions = restrictions;
        this.plans = plans;
      }
    });
  }

  openAddMeasurement(): void {
    this.editingMeasurement = null;
    this.measurementForm.reset();
    this.showMeasurementModal = true;
  }

  openEditMeasurement(m: GrowthMeasurement): void {
    this.editingMeasurement = m;
    this.measurementForm.patchValue({
      date: m.date,
      weight: m.weight,
      height: m.height,
      headCircumference: m.headCircumference || '',
      notes: m.notes || ''
    });
    this.showMeasurementModal = true;
  }

  saveMeasurement(): void {
    if (this.measurementForm.invalid || !this.selectedChildId) return;
    this.measurementSaving = true;
    const data = this.measurementForm.value;
    const obs = this.editingMeasurement
      ? this.parentService.updateMeasurement(this.selectedChildId, this.editingMeasurement.id!, data)
      : this.parentService.addMeasurement(this.selectedChildId, data);
    obs.subscribe({
      next: () => {
        this.showMeasurementModal = false;
        this.measurementSaving = false;
        this.showAlert(this.editingMeasurement ? 'Measurement updated' : 'Measurement added', 'success');
        this.loadGrowthData();
      },
      error: () => { this.measurementSaving = false; this.showAlert('Failed to save measurement', 'error'); }
    });
  }

  deleteMeasurement(m: GrowthMeasurement): void {
    if (!confirm('Delete this measurement?')) return;
    this.parentService.deleteMeasurement(this.selectedChildId, m.id!).subscribe({
      next: () => { this.showAlert('Measurement deleted', 'success'); this.loadGrowthData(); },
      error: () => this.showAlert('Failed to delete measurement', 'error')
    });
  }

  openAddRestriction(): void {
    this.editingRestriction = null;
    this.restrictionForm.reset();
    this.showRestrictionModal = true;
  }

  openEditRestriction(r: DietaryRestriction): void {
    this.editingRestriction = r;
    this.restrictionForm.patchValue({
      type: r.type,
      allergen: r.allergen,
      severity: r.severity,
      description: r.description || '',
      notes: r.notes || ''
    });
    this.showRestrictionModal = true;
  }

  saveRestriction(): void {
    if (this.restrictionForm.invalid || !this.selectedChildId) return;
    this.restrictionSaving = true;
    const data = this.restrictionForm.value;
    const obs = this.editingRestriction
      ? this.parentService.updateRestriction(this.selectedChildId, this.editingRestriction.id!, data)
      : this.parentService.addRestriction(this.selectedChildId, data);
    obs.subscribe({
      next: () => {
        this.showRestrictionModal = false;
        this.restrictionSaving = false;
        this.showAlert(this.editingRestriction ? 'Restriction updated' : 'Restriction added', 'success');
        this.loadGrowthData();
      },
      error: () => { this.restrictionSaving = false; this.showAlert('Failed to save restriction', 'error'); }
    });
  }

  deleteRestriction(r: DietaryRestriction): void {
    if (!confirm('Delete this restriction?')) return;
    this.parentService.deleteRestriction(this.selectedChildId, r.id!).subscribe({
      next: () => { this.showAlert('Restriction deleted', 'success'); this.loadGrowthData(); },
      error: () => this.showAlert('Failed to delete restriction', 'error')
    });
  }

  getSeverityClass(severity: string): string {
    const map: Record<string, string> = { LOW: 'info', MODERATE: 'warning', HIGH: 'danger', SEVERE: 'danger' };
    return map[severity] || '';
  }

  getDoctorName(doctorId: string): string {
    const d = this.availableDoctors.find(doc => doc.id === doctorId);
    return d ? d.fullName : doctorId;
  }

  getChildName(childId: string): string {
    const c = this.children.find(ch => ch.id === childId);
    return c ? c.fullName : childId;
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


  private showAlert(msg: string, type: 'success' | 'error'): void {
    this.alertMsg = msg;
    this.alertType = type;
    setTimeout(() => this.alertMsg = '', 4000);
  }
}
