import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TreatmentService } from '../services/treatment.service';
import { Treatment, StatutTraitement } from '../models/treatment.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface Child {
  id: string;
  fullName: string;
  dateOfBirth: string;
  gender: string;
}

@Component({
  selector: 'app-parent-treatment-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-treatment-list.component.html',
  styleUrls: ['./parent-treatment-list.component.css']
})
export class ParentTreatmentListComponent implements OnInit {
  treatments: Treatment[] = [];
  children: Child[] = [];
  loading = false;
  loadingChildren = false;
  error: string | null = null;
  selectedChildId: string | null = null;

  constructor(
    private treatmentService: TreatmentService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadChildren();
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
      this.loadTreatments(this.selectedChildId);
    } else {
      this.treatments = [];
    }
  }

  loadTreatments(childId: string): void {
    this.loading = true;
    this.error = null;
    
    this.treatmentService.getTreatmentsByPatient(childId).subscribe({
      next: (response) => {
        this.treatments = response.data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des traitements';
        this.loading = false;
        console.error(err);
      }
    });
  }

  getStatutClass(statut: StatutTraitement): string {
    const classes: Record<StatutTraitement, string> = {
      [StatutTraitement.EN_COURS]: 'status-active',
      [StatutTraitement.TERMINE]: 'status-completed',
      [StatutTraitement.SUSPENDU]: 'status-suspended',
      [StatutTraitement.ANNULE]: 'status-cancelled'
    };
    return classes[statut] || '';
  }

  getStatutLabel(statut: StatutTraitement): string {
    const labels: Record<StatutTraitement, string> = {
      [StatutTraitement.EN_COURS]: 'En cours',
      [StatutTraitement.TERMINE]: 'Terminé',
      [StatutTraitement.SUSPENDU]: 'Suspendu',
      [StatutTraitement.ANNULE]: 'Annulé'
    };
    return labels[statut] || statut;
  }
}
