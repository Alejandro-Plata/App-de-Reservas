import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Recursos } from './pages/recursos/recursos';
import { RecursoDetalle } from './pages/recurso-detalle/recurso-detalle';
import { MisReservas } from './pages/mis-reservas/mis-reservas';
import { AdminReservas } from './pages/admin-reservas/admin-reservas';
import { adminGuard, authGuard } from './guards/auth-guard';

export const routes: Routes = [
    { path: '', redirectTo: 'recursos', pathMatch: 'full' },
    { path: 'login', component: Login },
    { path: 'register', component: Register },
    { path: 'recursos', component: Recursos, canActivate: [authGuard] },
    { path: 'recursos/:id', component: RecursoDetalle, canActivate: [authGuard] },
    { path: 'mis-reservas', component: MisReservas, canActivate: [authGuard] },
    { path: 'admin/reservas', component: AdminReservas, canActivate: [adminGuard] },
    { path: '**', redirectTo: 'recursos' }
];
