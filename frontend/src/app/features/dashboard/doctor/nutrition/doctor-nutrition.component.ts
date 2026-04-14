import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { DoctorService } from '../../../../core/services/doctor.service';
import { Child } from '../../../../core/models/child.model';
import { NutritionalPlan } from '../../../../core/models/treatment.model';

@Component({
  selector: 'app-doctor-nutrition',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './doctor-nutrition.component.html',
  styleUrl: './doctor-nutrition.component.scss'
})
export class DoctorNutritionComponent implements OnInit {
  loading = true;
  patients: Child[] = [];
  nutritionalPlans: NutritionalPlan[] = [];
  selectedStatus: string = 'all';
  searchQuery = '';
  isListening = false;
  recognition: any = null;
  expandedPlans: Set<string> = new Set(); // Track which plans are expanded

  view: 'list' | 'form' = 'list';
  editingPlan: NutritionalPlan | null = null;
  planForm!: FormGroup;
  planSaving = false;

  alertMsg = '';
  alertType: 'success' | 'error' = 'success';

  constructor(private doctorService: DoctorService, private fb: FormBuilder) {
    // Initialize Speech Recognition
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'fr-FR';
      
      this.recognition.onresult = (event: any) => {
        const transcript = event.results[event.results.length - 1][0].transcript;
        this.searchQuery = transcript;
      };
      
      this.recognition.onerror = (event: any) => {
        console.error('Speech recognition error:', event.error);
        if (event.error === 'no-speech') {
          return;
        }
        this.isListening = false;
      };
      
      this.recognition.onend = () => {
        if (this.isListening) {
          this.recognition.start();
        }
      };
    }
  }

  ngOnInit(): void {
    this.planForm = this.fb.group({
      childId: ['', Validators.required],
      title: ['', Validators.required],
      description: [''],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      goals: [''],
      restrictions: [''],
      meals: this.fb.array([])
    });

    this.doctorService.getPatients().subscribe({ next: (p) => this.patients = p });
    this.loadNutritionalPlans();
  }

  get mealsArray(): FormArray { return this.planForm.get('meals') as FormArray; }

  addMealRow(): void {
    this.mealsArray.push(this.fb.group({
      name: ['', Validators.required],
      time: [''],
      description: [''],
      calories: [''],
      notes: ['']
    }));
  }

  removeMealRow(i: number): void { this.mealsArray.removeAt(i); }

  loadNutritionalPlans(): void {
    this.loading = true;
    this.doctorService.getNutritionalPlans().subscribe({
      next: (p) => { this.nutritionalPlans = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openAddPlan(): void {
    this.editingPlan = null;
    this.planForm.reset();
    this.mealsArray.clear();
    this.view = 'form';
  }

  openEditPlan(plan: NutritionalPlan): void {
    this.editingPlan = plan;
    this.planForm.patchValue({
      childId: plan.childId,
      title: plan.title,
      description: plan.description || '',
      startDate: plan.startDate,
      endDate: plan.endDate,
      goals: plan.goals || '',
      restrictions: plan.restrictions || ''
    });
    this.mealsArray.clear();
    if (plan.meals) {
      plan.meals.forEach(meal => {
        this.mealsArray.push(this.fb.group({
          name: [meal.name, Validators.required],
          time: [meal.time || ''],
          description: [meal.description || ''],
          calories: [meal.calories || ''],
          notes: [meal.notes || '']
        }));
      });
    }
    this.view = 'form';
  }

  cancelForm(): void {
    this.view = 'list';
    this.editingPlan = null;
    this.planForm.reset();
    this.mealsArray.clear();
  }

  savePlan(): void {
    if (this.planForm.invalid) return;
    this.planSaving = true;
    const data = this.planForm.value;
    const obs = this.editingPlan
      ? this.doctorService.updateNutritionalPlan(this.editingPlan.id!, data)
      : this.doctorService.createNutritionalPlan(data);
    obs.subscribe({
      next: () => {
        this.view = 'list';
        this.planSaving = false;
        this.showAlert(this.editingPlan ? 'Plan updated' : 'Plan created', 'success');
        this.loadNutritionalPlans();
      },
      error: () => { this.planSaving = false; this.showAlert('Failed to save plan', 'error'); }
    });
  }

  deletePlan(plan: NutritionalPlan): void {
    if (!confirm('Delete this nutritional plan?')) return;
    this.doctorService.deleteNutritionalPlan(plan.id!).subscribe({
      next: () => { this.showAlert('Plan deleted', 'success'); this.loadNutritionalPlans(); },
      error: () => this.showAlert('Failed to delete plan', 'error')
    });
  }

  getPatientName(childId: string): string {
    const p = this.patients.find(c => c.id === childId);
    return p ? p.fullName : childId;
  }

  get filteredPlans(): NutritionalPlan[] {
    const query = this.searchQuery.toLowerCase().trim();
    let filtered = this.nutritionalPlans;
    
    // Filter by status
    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(plan => plan.status === this.selectedStatus);
    }
    
    // Filter by search query (title)
    if (query) {
      filtered = filtered.filter(plan => {
        const title = plan.title?.toLowerCase() || '';
        const description = plan.description?.toLowerCase() || '';
        const goals = plan.goals?.toLowerCase() || '';
        
        return title.includes(query) || 
               description.includes(query) || 
               goals.includes(query);
      });
    }
    
    return filtered;
  }

  getStatusCount(status: string): number {
    if (status === 'all') {
      return this.nutritionalPlans.length;
    }
    return this.nutritionalPlans.filter(plan => plan.status === status).length;
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
  }

  startVoiceSearch(): void {
    if (!this.recognition) {
      alert('Voice recognition is not supported in your browser. Please use Chrome or Edge.');
      return;
    }
    
    if (this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    } else {
      this.isListening = true;
      try {
        this.recognition.start();
      } catch (error) {
        console.error('Error starting recognition:', error);
        this.isListening = false;
      }
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
  }

  togglePlanExpansion(planId: string): void {
    if (this.expandedPlans.has(planId)) {
      this.expandedPlans.delete(planId);
    } else {
      this.expandedPlans.add(planId);
    }
  }

  isPlanExpanded(planId: string): boolean {
    return this.expandedPlans.has(planId);
  }

  getVisibleMeals(plan: NutritionalPlan) {
    if (!plan.meals) return [];
    if (this.isPlanExpanded(plan.id!)) {
      return plan.meals;
    }
    return plan.meals.slice(0, 3);
  }

  private showAlert(msg: string, type: 'success' | 'error'): void {
    this.alertMsg = msg;
    this.alertType = type;
    setTimeout(() => this.alertMsg = '', 4000);
  }
}
