import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { NgApexchartsModule, ChartComponent, ApexChart, ApexXAxis, ApexYAxis, ApexDataLabels, ApexStroke, ApexMarkers, ApexGrid, ApexTooltip, ApexLegend } from 'ng-apexcharts';
import { DoctorService } from '../../../../core/services/doctor.service';
import { Child } from '../../../../core/models/child.model';
import { GrowthMeasurement, DietaryRestriction, NutritionalPlan } from '../../../../core/models/treatment.model';

export type ChartOptions = {
  series: any[];
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  dataLabels: ApexDataLabels;
  stroke: ApexStroke;
  markers: ApexMarkers;
  grid: ApexGrid;
  tooltip: ApexTooltip;
  legend: ApexLegend;
  colors: string[];
};

@Component({
  selector: 'app-doctor-growth',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, NgApexchartsModule],
  templateUrl: './doctor-growth.component.html',
  styleUrl: './doctor-growth.component.scss'
})
export class DoctorGrowthComponent implements OnInit {
  @ViewChild('chart') chart!: ChartComponent;
  
  loading = true;
  patients: Child[] = [];
  selectedPatientId = '';
  growthSubTab: 'measurements' | 'restrictions' | 'plans' = 'measurements';

  patientMeasurements: GrowthMeasurement[] = [];
  patientRestrictions: DietaryRestriction[] = [];
  patientPlans: NutritionalPlan[] = [];

  chartOptions: Partial<ChartOptions> = {};

  constructor(
    private doctorService: DoctorService,
    private cdr: ChangeDetectorRef
  ) {
    this.initializeChart();
  }

  ngOnInit(): void {
    this.doctorService.getPatients().subscribe({
      next: (p) => {
        this.patients = p;
        if (p.length > 0) {
          this.selectedPatientId = p[0].id!;
          this.loadPatientGrowth();
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onPatientSelected(): void { this.loadPatientGrowth(); }

  loadPatientGrowth(): void {
    if (!this.selectedPatientId) return;
    
    this.doctorService.getPatientMeasurements(this.selectedPatientId).subscribe({
      next: (m) => {
        // Trier par date décroissante (plus récent en premier)
        this.patientMeasurements = m.sort((a, b) => {
          return new Date(b.date).getTime() - new Date(a.date).getTime();
        });
        this.updateChart();
        // Force change detection to ensure all measurements render
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading measurements:', err);
      }
    });
    
    this.doctorService.getPatientRestrictions(this.selectedPatientId).subscribe({
      next: (r) => {
        this.patientRestrictions = r;
        this.cdr.detectChanges();
      }
    });
    this.doctorService.getPatientPlans(this.selectedPatientId).subscribe({
      next: (p) => {
        this.patientPlans = p;
        this.cdr.detectChanges();
      }
    });
  }

  initializeChart(): void {
    this.chartOptions = {
      series: [{
        name: 'BMI',
        data: []
      }],
      chart: {
        type: 'line',
        height: 350,
        toolbar: {
          show: true,
          tools: {
            download: true,
            zoom: true,
            zoomin: true,
            zoomout: true,
            pan: true,
            reset: true
          }
        },
        animations: {
          enabled: true,
          speed: 800,
          animateGradually: {
            enabled: true,
            delay: 150
          },
          dynamicAnimation: {
            enabled: true,
            speed: 350
          }
        }
      },
      stroke: {
        curve: 'smooth',
        width: 3
      },
      markers: {
        size: 6,
        colors: ['#fff'],
        strokeColors: '#0077CC',
        strokeWidth: 3,
        hover: {
          size: 8
        }
      },
      colors: ['#0077CC'],
      dataLabels: {
        enabled: false
      },
      xaxis: {
        type: 'datetime',
        labels: {
          format: 'MMM yyyy',
          style: {
            colors: '#6c757d',
            fontSize: '12px',
            fontWeight: 500
          }
        },
        title: {
          text: 'Date',
          style: {
            color: '#495057',
            fontSize: '14px',
            fontWeight: 600
          }
        }
      },
      yaxis: {
        title: {
          text: 'BMI',
          style: {
            color: '#495057',
            fontSize: '14px',
            fontWeight: 600
          }
        },
        labels: {
          style: {
            colors: '#6c757d',
            fontSize: '12px',
            fontWeight: 500
          },
          formatter: (value) => value.toFixed(1)
        }
      },
      grid: {
        borderColor: '#e9ecef',
        strokeDashArray: 4,
        xaxis: {
          lines: {
            show: true
          }
        },
        yaxis: {
          lines: {
            show: true
          }
        }
      },
      tooltip: {
        enabled: true,
        x: {
          format: 'dd MMM yyyy'
        },
        y: {
          formatter: (value) => value.toFixed(2)
        },
        style: {
          fontSize: '13px'
        }
      },
      legend: {
        show: true,
        position: 'top',
        horizontalAlign: 'right',
        fontSize: '13px',
        fontWeight: 600,
        labels: {
          colors: '#495057'
        }
      }
    };
  }

  updateChart(): void {
    if (this.patientMeasurements.length === 0) {
      this.chartOptions = {
        ...this.chartOptions,
        series: [{
          name: 'BMI',
          data: []
        }]
      };
      return;
    }

    // Trier les mesures par date (ordre croissant pour le graphique)
    const sortedMeasurements = [...this.patientMeasurements].sort((a, b) => {
      return new Date(a.date).getTime() - new Date(b.date).getTime();
    });

    console.log('Sorted measurements for chart:', sortedMeasurements);

    // Préparer les données pour le graphique
    const chartData = sortedMeasurements
      .filter(m => m.bmi !== null && m.bmi !== undefined && m.bmi > 0)
      .map(m => {
        const timestamp = new Date(m.date).getTime();
        console.log('Chart point:', { date: m.date, timestamp, bmi: m.bmi });
        return {
          x: timestamp,
          y: Number(m.bmi)
        };
      });

    console.log('Chart data prepared:', chartData);

    // Forcer la mise à jour du graphique
    this.chartOptions = {
      ...this.chartOptions,
      series: [{
        name: 'BMI',
        data: chartData
      }]
    };

    // Forcer le rafraîchissement du composant chart si disponible
    if (this.chart) {
      setTimeout(() => {
        this.chart.updateOptions(this.chartOptions);
      }, 100);
    }
  }

  hasBmiData(): boolean {
    return this.patientMeasurements.some(m => m.bmi && m.bmi > 0);
  }

  trackByMeasurementId(index: number, measurement: GrowthMeasurement): string {
    return measurement.id || `${measurement.date}-${index}`;
  }
}
