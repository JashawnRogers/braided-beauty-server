ALTER TABLE schedule_calendar
    ADD COLUMN IF NOT EXISTS booking_open_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS booking_close_at TIMESTAMP NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'schedule_calendar'
          AND column_name = 'booking_open_date'
    ) THEN
        EXECUTE $sql$
            UPDATE schedule_calendar
            SET booking_open_at = COALESCE(booking_open_at, booking_open_date::timestamp + time '00:00')
            WHERE booking_open_date IS NOT NULL
        $sql$;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'schedule_calendar'
          AND column_name = 'booking_close_date'
    ) THEN
        EXECUTE $sql$
            UPDATE schedule_calendar
            SET booking_close_at = COALESCE(booking_close_at, booking_close_date::timestamp + time '23:59:59')
            WHERE booking_close_date IS NOT NULL
        $sql$;
    END IF;
END $$;

ALTER TABLE schedule_calendar
    DROP COLUMN IF EXISTS booking_open_date,
    DROP COLUMN IF EXISTS booking_close_date;

CREATE INDEX IF NOT EXISTS idx_schedule_calendar_booking_open_at
    ON schedule_calendar (booking_open_at);

CREATE INDEX IF NOT EXISTS idx_schedule_calendar_booking_close_at
    ON schedule_calendar (booking_close_at);
