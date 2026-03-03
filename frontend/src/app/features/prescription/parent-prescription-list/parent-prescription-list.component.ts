import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PrescriptionService } from '../services/prescription.service';
import { Prescription, StatutPrescription } from '../models/prescription.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface Child {
  id: string;
  fullName: string;
  dateOfBirth: string;
  gender: string;
}

@Component({
  selector: 'app-parent-prescription-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-prescription-list.component.html',
  styleUrls: ['./parent-prescription-list.component.css']
})
export class ParentPrescriptionListComponent implements OnInit {
  prescriptions: Prescription[] = [];
  filteredPrescriptions: Prescription[] = [];
  children: Child[] = [];
  loading = false;
  loadingChildren = false;
  error: string | null = null;
  selectedChildId: string | null = null;
  
  // Search and sort
  searchTerm = '';
  sortBy: 'date' | 'status' | 'diagnostic' = 'date';
  sortOrder: 'asc' | 'desc' = 'desc';

  constructor(
    private prescriptionService: PrescriptionService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    const navigation = this.router.getCurrentNavigation();
    this.selectedChildId = navigation?.extras?.state?.['childId'];
    
    this.loadChildren();
    
    if (this.selectedChildId) {
      this.loadPrescriptions(this.selectedChildId);
    }
  }

  loadChildren(): void {
    this.loadingChildren = true;
    this.http.get<any>(`${environment.apiUrl}/parent/children`).subscribe({
      next: (response) => {
        this.children = response.map((c: any) => ({
          id: c.id,
          fullName: c.fullName,
          dateOfBirth: c.dateOfBirth,
          gender: c.gender
        }));
        this.loadingChildren = false;
      },
      error: (err) => {
        console.error('Error loading children:', err);
        this.loadingChildren = false;
      }
    });
  }

  onChildChange(): void {
    if (this.selectedChildId) {
      this.loadPrescriptions(this.selectedChildId);
    } else {
      this.prescriptions = [];
      this.filteredPrescriptions = [];
    }
  }

  loadPrescriptions(childId: string): void {
    this.loading = true;
    this.error = null;
    
    this.prescriptionService.getPrescriptionsByPatient(childId).subscribe({
      next: (response) => {
        this.prescriptions = response.data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error loading prescriptions';
        this.loading = false;
        console.error(err);
      }
    });
  }
  
  applyFilters(): void {
    let filtered = [...this.prescriptions];
    
    // Search filter
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        p.diagnostic.toLowerCase().includes(term) ||
        p.notes?.toLowerCase().includes(term) ||
        p.medicaments.some(m => 
          m.nomCommercial.toLowerCase().includes(term) ||
          m.dci.toLowerCase().includes(term)
        )
      );
    }
    
    // Sort
    filtered.sort((a, b) => {
      let comparison = 0;
      
      if (this.sortBy === 'date') {
        comparison = new Date(a.datePrescription).getTime() - new Date(b.datePrescription).getTime();
      } else if (this.sortBy === 'status') {
        comparison = a.statut.localeCompare(b.statut);
      } else if (this.sortBy === 'diagnostic') {
        comparison = a.diagnostic.localeCompare(b.diagnostic);
      }
      
      return this.sortOrder === 'asc' ? comparison : -comparison;
    });
    
    this.filteredPrescriptions = filtered;
  }
  
  onSearchChange(): void {
    this.applyFilters();
  }
  
  onSortChange(): void {
    this.applyFilters();
  }
  
  toggleSortOrder(): void {
    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    this.applyFilters();
  }
  
  clearSearch(): void {
    this.searchTerm = '';
    this.applyFilters();
  }

  getStatutClass(statut: StatutPrescription): string {
    const classes: Record<StatutPrescription, string> = {
      [StatutPrescription.ACTIVE]: 'status-active',
      [StatutPrescription.EXPIREE]: 'status-expired',
      [StatutPrescription.TERMINEE]: 'status-completed',
      [StatutPrescription.ANNULEE]: 'status-cancelled',
      [StatutPrescription.RENOUVELEE]: 'status-renewed'
    };
    return classes[statut] || '';
  }

  getStatutLabel(statut: StatutPrescription): string {
    const labels: Record<StatutPrescription, string> = {
      [StatutPrescription.ACTIVE]: 'Active',
      [StatutPrescription.EXPIREE]: 'Expired',
      [StatutPrescription.TERMINEE]: 'Completed',
      [StatutPrescription.ANNULEE]: 'Cancelled',
      [StatutPrescription.RENOUVELEE]: 'Renewed'
    };
    return labels[statut] || statut;
  }

  goBack(): void {
    this.router.navigate(['/parent']);
  }
}
