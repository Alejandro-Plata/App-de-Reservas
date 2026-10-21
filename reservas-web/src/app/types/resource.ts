export type ResourceCategory = 'SALA' | 'PISTA' | 'EQUIPO';

export interface Resource {
    id: number;
    name: string;
    description: string;
    category: ResourceCategory;
    capacity: number;
    active: boolean;
}

// Lo que se envía en POST / PUT (sin id ni active: el active se cambia con DELETE)
export interface ResourceRequest {
    name: string;
    description: string;
    category: ResourceCategory;
    capacity: number;
}
