export type Role = 'USER' | 'ADMIN';

// Lo que devuelve /auth/login y /auth/register (ver "Contrato asumido" en el README)
export interface AuthResponse {
    token: string;
    email: string;
    role: Role;
}

export interface Session {
    email: string;
    role: Role;
}
