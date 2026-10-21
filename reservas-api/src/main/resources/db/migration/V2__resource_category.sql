-- Categoría para poder filtrar el catálogo (Salas / Pistas / Equipos).
-- Los recursos que ya existan quedan como SALA.
ALTER TABLE resources
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'SALA'
        CHECK (category IN ('SALA', 'PISTA', 'EQUIPO'));

CREATE INDEX idx_resources_category ON resources (category);
