CREATE TABLE IF NOT EXISTS schedule_calendar_date_override (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL REFERENCES schedule_calendar(id) ON DELETE CASCADE,
    override_date DATE NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    open_time TIME NULL,
    close_time TIME NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_calendar_override_calendar_date
    ON schedule_calendar_date_override (calendar_id, override_date);

CREATE INDEX IF NOT EXISTS idx_schedule_calendar_override_date
    ON schedule_calendar_date_override (override_date);
