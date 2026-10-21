export type ReservationStatus = 'CONFIRMED' | 'CANCELLED';

export interface Reservation {
    id: number;
    resourceId: number;
    resourceName: string;
    userEmail: string;
    startAt: string; // ISO-8601 con zona (Instant en el backend)
    endAt: string;
    status: ReservationStatus;
}

export interface ReservationRequest {
    resourceId: number;
    startAt: string;
    endAt: string;
}

// Hueco ya ocupado que devuelve /availability
export interface BusySlot {
    startAt: string;
    endAt: string;
}

export interface ReservationFilters {
    status?: ReservationStatus | '';
    resourceId?: number | null;
    from?: string; // yyyy-MM-dd
    to?: string;
}
