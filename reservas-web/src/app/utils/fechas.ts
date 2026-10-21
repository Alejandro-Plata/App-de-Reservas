// Utilidades de fechas compartidas por varias páginas.

// Convierte el valor de un <input type="datetime-local"> (hora local, sin zona)
// en un instante ISO con zona, que es lo que espera el backend.
export function localAIso(valor: string): string {
  return new Date(valor).toISOString();
}

// yyyy-MM-dd en hora LOCAL (toISOString daría la fecha en UTC y podría salir el día anterior)
export function hoyLocal(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

export function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' });
}

export function formatearHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
}
