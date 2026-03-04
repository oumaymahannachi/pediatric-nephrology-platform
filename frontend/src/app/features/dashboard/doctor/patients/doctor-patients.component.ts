import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { DoctorService } from '../../../../core/services/doctor.service';
import { Child } from '../../../../core/models/child.model';

@Component({
  selector: 'app-doctor-patients',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './doctor-patients.component.html',
  styleUrl: './doctor-patients.component.scss'
})
export class DoctorPatientsComponent implements OnInit {
  loading = true;
  patients: Child[] = [];
  filteredPatients: Child[] = [];
  searchQuery = '';
  selectedGender: string = 'all'; // Filter by gender
  isListening = false;
  recognition: any = null;

  constructor(private doctorService: DoctorService) {
    // Initialize Speech Recognition
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = true; // Keep listening until manually stopped
      this.recognition.interimResults = true; // Show results in real-time
      this.recognition.lang = 'fr-FR'; // French language, change to 'en-US' for English
      
      this.recognition.onresult = (event: any) => {
        const transcript = event.results[event.results.length - 1][0].transcript;
        this.searchQuery = transcript;
        this.filterPatients();
      };
      
      this.recognition.onerror = (event: any) => {
        console.error('Speech recognition error:', event.error);
        if (event.error === 'no-speech') {
          // Don't stop on no-speech, keep listening
          return;
        }
        this.isListening = false;
      };
      
      this.recognition.onend = () => {
        // If still supposed to be listening, restart
        if (this.isListening) {
          this.recognition.start();
        }
      };
    }
  }

  ngOnInit(): void {
    this.doctorService.getPatients().subscribe({
      next: (p) => { 
        this.patients = p; 
        this.filteredPatients = p;
        this.loading = false; 
      },
      error: () => { this.loading = false; }
    });
  }

  filterPatients(): void {
    const query = this.searchQuery.toLowerCase().trim();
    
    // Start with all patients
    let filtered = this.patients;
    
    // Filter by gender
    if (this.selectedGender !== 'all') {
      filtered = filtered.filter(patient => 
        patient.gender?.toLowerCase() === this.selectedGender.toLowerCase()
      );
    }
    
    // Filter by search query
    if (query) {
      filtered = filtered.filter(patient => {
        const fullName = patient.fullName?.toLowerCase() || '';
        const gender = patient.gender?.toLowerCase() || '';
        const notes = patient.notes?.toLowerCase() || '';
        
        return fullName.includes(query) || 
               gender.includes(query) || 
               notes.includes(query);
      });
    }
    
    this.filteredPatients = filtered;
  }

  startVoiceSearch(): void {
    if (!this.recognition) {
      alert('Voice recognition is not supported in your browser. Please use Chrome or Edge.');
      return;
    }
    
    if (this.isListening) {
      // Stop listening when button is clicked again
      this.recognition.stop();
      this.isListening = false;
    } else {
      // Start listening
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
    this.filterPatients();
  }

  onGenderChange(): void {
    this.filterPatients();
  }

  getGenderCount(gender: string): number {
    if (gender === 'all') {
      return this.patients.length;
    }
    return this.patients.filter(p => p.gender?.toLowerCase() === gender.toLowerCase()).length;
  }
}
