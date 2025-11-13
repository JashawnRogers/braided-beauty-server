CREATE TABLE IF NOT EXISTS service_addons (
    service_id UUID NOT NULL,
    add_on_id  UUID NOT NULL,
    PRIMARY KEY (service_id, add_on_id),

    CONSTRAINT fk_service_addons_service
        FOREIGN KEY (service_id)
        REFERENCES services (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_addons_addon
        FOREIGN KEY (add_on_id)
        REFERENCES addons (id)
        ON DELETE RESTRICT
);