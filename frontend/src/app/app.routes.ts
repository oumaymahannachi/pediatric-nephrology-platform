import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'signup',
        loadComponent: () => import('./features/auth/signup/signup.component').then(m => m.SignupComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
      },
      {
        path: 'verify-otp',
        loadComponent: () => import('./features/auth/verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent)
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    loadComponent: () => import('./features/dashboard/admin/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/admin/overview/admin-overview.component').then(m => m.AdminOverviewComponent) },
      { path: 'users', loadComponent: () => import('./features/dashboard/admin/users/admin-users.component').then(m => m.AdminUsersComponent) },
      { path: 'treatment', loadComponent: () => import('./features/dashboard/admin/treatment/admin-treatment.component').then(m => m.AdminTreatmentComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/edit-profile.component').then(m => m.EditProfileComponent) }
    ]
  },
  {
    path: 'doctor',
    canActivate: [authGuard, roleGuard],
    data: { role: 'DOCTOR' },
    loadComponent: () => import('./features/dashboard/doctor/doctor-layout.component').then(m => m.DoctorLayoutComponent),
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/doctor/overview/doctor-overview.component').then(m => m.DoctorOverviewComponent) },
      { path: 'patients', loadComponent: () => import('./features/dashboard/doctor/patients/doctor-patients.component').then(m => m.DoctorPatientsComponent) },
      { path: 'appointments', loadComponent: () => import('./features/dashboard/doctor/appointments/doctor-appointments.component').then(m => m.DoctorAppointmentsComponent) },
      { path: 'growth', loadComponent: () => import('./features/dashboard/doctor/growth/doctor-growth.component').then(m => m.DoctorGrowthComponent) },
      { path: 'nutrition', loadComponent: () => import('./features/dashboard/doctor/nutrition/doctor-nutrition.component').then(m => m.DoctorNutritionComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/edit-profile.component').then(m => m.EditProfileComponent) }
    ]
  },
  {
    path: 'parent',
    canActivate: [authGuard, roleGuard],
    data: { role: 'PARENT' },
    loadComponent: () => import('./features/dashboard/parent/parent-dashboard.component').then(m => m.ParentDashboardComponent),
    children: [
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/edit-profile.component').then(m => m.EditProfileComponent)
      }
    ]
  },
  {
    path: 'infirmier',
    canActivate: [authGuard, roleGuard],
    data: { role: 'INFIRMIER' },
    loadComponent: () => import('./features/dashboard/infirmier/infirmier-dashboard.component').then(m => m.InfirmierDashboardComponent)
  },
  {
    path: 'infirmier/profile',
    canActivate: [authGuard, roleGuard],
    data: { role: 'INFIRMIER' },
    loadComponent: () => import('./features/profile/edit-profile.component').then(m => m.EditProfileComponent)
  },
  { path: '**', redirectTo: '' }
];
