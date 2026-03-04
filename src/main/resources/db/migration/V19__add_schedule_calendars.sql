CREATE TABLE IF NOT EXISTS schedule_calendar (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#1D4ED8',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    booking_open_date DATE NULL,
    booking_close_date DATE NULL,
    max_bookings_per_day INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_calendar_name_ci
    ON schedule_calendar (lower(name));

INSERT INTO schedule_calendar (id, name, color, active, max_bookings_per_day, created_at, updated_at)
SELECT gen_random_uuid(), 'Default', '#1D4ED8', TRUE, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_calendar WHERE lower(name) = 'default'
);

CREATE TABLE IF NOT EXISTS schedule_calendar_hours (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL REFERENCES schedule_calendar(id),
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NULL,
    close_time TIME NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT schedule_calendar_hours_day_check
        CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_calendar_hours_calendar_day
    ON schedule_calendar_hours (calendar_id, day_of_week);

ALTER TABLE services ADD COLUMN IF NOT EXISTS schedule_calendar_id UUID;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS schedule_calendar_id UUID;

UPDATE services s
SET schedule_calendar_id = c.id
FROM schedule_calendar c
WHERE lower(c.name) = 'default'
  AND s.schedule_calendar_id IS NULL;

UPDATE appointments a
SET schedule_calendar_id = c.id
FROM schedule_calendar c
WHERE lower(c.name) = 'default'
  AND a.schedule_calendar_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_services_schedule_calendar'
    ) THEN
        ALTER TABLE services
            ADD CONSTRAINT fk_services_schedule_calendar
            FOREIGN KEY (schedule_calendar_id) REFERENCES schedule_calendar(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_appointments_schedule_calendar'
    ) THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_schedule_calendar
            FOREIGN KEY (schedule_calendar_id) REFERENCES schedule_calendar(id);
    END IF;
END $$;

ALTER TABLE services
    ALTER COLUMN schedule_calendar_id SET NOT NULL;

ALTER TABLE appointments
    ALTER COLUMN schedule_calendar_id SET NOT NULL;

INSERT INTO schedule_calendar_hours (id, calendar_id, day_of_week, open_time, close_time, is_closed)
SELECT gen_random_uuid(), c.id, bh.day_of_week, bh.open_time, bh.close_time, bh.is_closed
FROM business_hours bh
CROSS JOIN LATERAL (
    SELECT id FROM schedule_calendar WHERE lower(name) = 'default' LIMIT 1
) c
ON CONFLICT (calendar_id, day_of_week) DO NOTHING;

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS uk_appointment_time;
ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_appointment_time_key;
DROP INDEX IF EXISTS ux_appointments_time_active;
DROP INDEX IF EXISTS uk_appointment_time;

CREATE INDEX IF NOT EXISTS idx_appointments_appointment_time
    ON appointments (appointment_time);

CREATE INDEX IF NOT EXISTS idx_appointments_calendar_time
    ON appointments (schedule_calendar_id, appointment_time);

CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_calendar_time_active
    ON appointments (schedule_calendar_id, appointment_time)
    WHERE appointment_status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED');
