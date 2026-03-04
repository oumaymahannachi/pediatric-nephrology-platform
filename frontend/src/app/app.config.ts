import { ApplicationConfig, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import {
  LucideAngularModule,
  ArrowLeft, Award, Baby, BarChart2, BarChart3, Bell, Building2, Calendar, Check,
  ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Circle, Clipboard, ClipboardList,
  Edit, Eye, EyeOff, Facebook, FileText, Filter, Flame, Heart, Home, Inbox, Info,
  Instagram, LayoutDashboard, LineChart, Linkedin, List, Lock, LogOut, Mail, Menu,
  MessageSquare, Mic, Pencil, Phone, Plus, Ruler, Save, Search, SearchX, Settings,
  Shield, ShieldAlert, ShieldCheck, Star, Stethoscope, Target, TrendingUp, Twitter,
  User, UserCircle, UserCheck, UserSearch, Users, Utensils, Weight, X,
  Activity, AlertCircle, AlertTriangle, Ban, CheckCircle, Clock, HeartPulse,
  MapPin, RefreshCw, Syringe, Thermometer, Trash2
} from 'lucide-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimations(),
    importProvidersFrom(LucideAngularModule.pick({
      ArrowLeft, Award, Baby, BarChart2, BarChart3, Bell, Building2, Calendar, Check,
      ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Circle, Clipboard, ClipboardList,
      Edit, Eye, EyeOff, Facebook, FileText, Filter, Flame, Heart, Home, Inbox, Info,
      Instagram, LayoutDashboard, LineChart, Linkedin, List, Lock, LogOut, Mail, Menu,
      MessageSquare, Mic, Pencil, Phone, Plus, Ruler, Save, Search, SearchX, Settings,
      Shield, ShieldAlert, ShieldCheck, Star, Stethoscope, Target, TrendingUp, Twitter,
      User, UserCircle, UserCheck, UserSearch, Users, Utensils, Weight, X,
      Activity, AlertCircle, AlertTriangle, Ban, CheckCircle, Clock, HeartPulse,
      MapPin, RefreshCw, Syringe, Thermometer, Trash2
    }))
  ]
};
