// Forma de la página que devuelve Spring Data (Page<T>)
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number; // página actual, empieza en 0
    size: number;
}
