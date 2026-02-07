--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5 (Homebrew)
-- Dumped by pg_dump version 17.5

-- Started on 2026-02-07 10:09:47 MST


--
-- TOC entry 218 (class 1259 OID 17230)
-- Name: add_on; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.add_on (
    duration_minutes integer,
    price numeric(10,2) NOT NULL,
    id uuid NOT NULL,
    name character varying(120) NOT NULL,
    description text
);



--
-- TOC entry 219 (class 1259 OID 17237)
-- Name: appointment_add_ons; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.appointment_add_ons (
    add_on_id uuid NOT NULL,
    appointment_id uuid NOT NULL
);


--
-- TOC entry 220 (class 1259 OID 17240)
-- Name: appointments; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.appointments (
    deposit_amount numeric(38,2),
    duration_minutes integer,
    loyalty_applied boolean NOT NULL,
    remaining_balance numeric(38,2),
    tip_amount numeric(38,2),
    total_amount numeric(38,2),
    appointment_time timestamp(6) without time zone NOT NULL,
    booking_confirmation_expires_at timestamp(6) with time zone,
    created_at timestamp(6) without time zone,
    guest_token_expires_at timestamp(6) without time zone,
    hold_expires_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    service_id uuid,
    user_id uuid,
    booking_confirmation_jti character varying(64),
    appointment_status character varying(255) NOT NULL,
    cancel_reason text,
    guest_cancel_token character varying(255),
    guest_email character varying(255),
    note text,
    payment_status character varying(255) NOT NULL,
    stripe_payment_id character varying(255),
    stripe_session_id character varying(255),
    CONSTRAINT appointments_appointment_status_check CHECK (((appointment_status)::text = ANY ((ARRAY['CONFIRMED'::character varying, 'CANCELED'::character varying, 'COMPLETED'::character varying, 'NO_SHOW'::character varying, 'PENDING_CONFIRMATION'::character varying])::text[]))),
    CONSTRAINT appointments_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PAID_DEPOSIT'::character varying, 'PAID_IN_FULL_ACH'::character varying, 'PAID_IN_FULL_CASH'::character varying, 'PAYMENT_FAILED'::character varying, 'REFUNDED'::character varying, 'NO_DEPOSIT_REQUIRED'::character varying])::text[])))
);


--
-- TOC entry 221 (class 1259 OID 17251)
-- Name: business_hours; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.business_hours (
    close_time time(6) without time zone,
    is_closed boolean NOT NULL,
    open_time time(6) without time zone,
    id uuid NOT NULL,
    day_of_week character varying(255) NOT NULL,
    CONSTRAINT business_hours_day_of_week_check CHECK (((day_of_week)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);


--
-- TOC entry 222 (class 1259 OID 17259)
-- Name: business_settings; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.business_settings (
    apt_buffer_time integer,
    id uuid NOT NULL,
    company_address character varying(255),
    company_email character varying(255),
    company_phone_number character varying(255)
);


--
-- TOC entry 223 (class 1259 OID 17266)
-- Name: categories; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.categories (
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    description character varying(250),
    name character varying(255) NOT NULL
);


--
-- TOC entry 224 (class 1259 OID 17273)
-- Name: loyalty_records; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.loyalty_records (
    enabled boolean,
    points integer,
    redeemed_points integer,
    sign_up_bonus_awarded boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    version bigint NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


--
-- TOC entry 225 (class 1259 OID 17280)
-- Name: loyalty_settings; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.loyalty_settings (
    bronze_tier_threshold integer,
    earn_per_appointment integer,
    gold_tier_threshold integer,
    program_enabled boolean NOT NULL,
    signup_bonus_points integer NOT NULL,
    silver_tier_threshold integer,
    id uuid NOT NULL
);


--
-- TOC entry 226 (class 1259 OID 17285)
-- Name: payment; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.payment (
    amount numeric(38,2),
    refunded_amount numeric(38,2),
    tip_amount numeric(38,2),
    appointment_id uuid,
    id uuid NOT NULL,
    user_id uuid,
    payment_status character varying(255),
    payment_type character varying(255),
    stripe_payment_intent_id character varying(255),
    stripe_session_id character varying(255),
    CONSTRAINT payment_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PAID_DEPOSIT'::character varying, 'PAID_IN_FULL_ACH'::character varying, 'PAID_IN_FULL_CASH'::character varying, 'PAYMENT_FAILED'::character varying, 'REFUNDED'::character varying, 'NO_DEPOSIT_REQUIRED'::character varying])::text[]))),
    CONSTRAINT payment_payment_type_check CHECK (((payment_type)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'FINAL'::character varying])::text[])))
);


--
-- TOC entry 227 (class 1259 OID 17294)
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.refresh_tokens (
    expires_at timestamp(6) with time zone NOT NULL,
    issued_at timestamp(6) with time zone NOT NULL,
    revoked_at timestamp(6) with time zone,
    family_id uuid NOT NULL,
    token_id uuid NOT NULL,
    user_id uuid NOT NULL,
    replaced_by_token_hash character varying(43),
    token_hash character varying(43) NOT NULL,
    device_info character varying(256)
);


--
-- TOC entry 228 (class 1259 OID 17301)
-- Name: service_addons; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.service_addons (
    add_on_id uuid NOT NULL,
    service_id uuid NOT NULL
);


--
-- TOC entry 229 (class 1259 OID 17304)
-- Name: service_keys; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.service_keys (
    "position" integer NOT NULL,
    service_id uuid NOT NULL,
    url character varying(512)
);


--
-- TOC entry 230 (class 1259 OID 17311)
-- Name: services; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.services (
    deposit_amount numeric(10,2) NOT NULL,
    duration_minutes integer NOT NULL,
    points_earned integer NOT NULL,
    price numeric(10,2) NOT NULL,
    times_booked integer,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    category_id uuid NOT NULL,
    id uuid NOT NULL,
    name character varying(120) NOT NULL,
    description text,
    video_key character varying(255)
);


--
-- TOC entry 231 (class 1259 OID 17318)
-- Name: users; Type: TABLE; Schema: public; Owner: jashawnrogers
--

CREATE TABLE public.users (
    enabled boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    email character varying(255),
    name character varying(255),
    oauth_provider character varying(255),
    oauth_subject character varying(255),
    password character varying(255),
    phone_number character varying(255),
    stripe_customer_id character varying(255),
    user_type character varying(255) NOT NULL,
    CONSTRAINT users_user_type_check CHECK (((user_type)::text = ANY ((ARRAY['GUEST'::character varying, 'MEMBER'::character varying, 'AMBASSADOR'::character varying, 'ADMIN'::character varying])::text[])))
);

--
-- TOC entry 3602 (class 2606 OID 17236)
-- Name: add_on add_on_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.add_on
    ADD CONSTRAINT add_on_pkey PRIMARY KEY (id);


--
-- TOC entry 3604 (class 2606 OID 17250)
-- Name: appointments appointments_appointment_time_key; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_appointment_time_key UNIQUE (appointment_time);


--
-- TOC entry 3606 (class 2606 OID 17248)
-- Name: appointments appointments_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);


--
-- TOC entry 3608 (class 2606 OID 17256)
-- Name: business_hours business_hours_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.business_hours
    ADD CONSTRAINT business_hours_pkey PRIMARY KEY (id);


--
-- TOC entry 3612 (class 2606 OID 17265)
-- Name: business_settings business_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.business_settings
    ADD CONSTRAINT business_settings_pkey PRIMARY KEY (id);


--
-- TOC entry 3614 (class 2606 OID 17272)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- TOC entry 3616 (class 2606 OID 17277)
-- Name: loyalty_records loyalty_records_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.loyalty_records
    ADD CONSTRAINT loyalty_records_pkey PRIMARY KEY (id);


--
-- TOC entry 3618 (class 2606 OID 17279)
-- Name: loyalty_records loyalty_records_user_id_key; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.loyalty_records
    ADD CONSTRAINT loyalty_records_user_id_key UNIQUE (user_id);


--
-- TOC entry 3620 (class 2606 OID 17284)
-- Name: loyalty_settings loyalty_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.loyalty_settings
    ADD CONSTRAINT loyalty_settings_pkey PRIMARY KEY (id);


--
-- TOC entry 3622 (class 2606 OID 17293)
-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);


--
-- TOC entry 3627 (class 2606 OID 17298)
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (token_id);


--
-- TOC entry 3629 (class 2606 OID 17300)
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- TOC entry 3631 (class 2606 OID 17310)
-- Name: service_keys service_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.service_keys
    ADD CONSTRAINT service_keys_pkey PRIMARY KEY ("position", service_id);


--
-- TOC entry 3633 (class 2606 OID 17317)
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


--
-- TOC entry 3610 (class 2606 OID 17258)
-- Name: business_hours uk_business_hours_day; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.business_hours
    ADD CONSTRAINT uk_business_hours_day UNIQUE (day_of_week);


--
-- TOC entry 3635 (class 2606 OID 17329)
-- Name: users uk_user_email; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_user_email UNIQUE (email);


--
-- TOC entry 3637 (class 2606 OID 17327)
-- Name: users uk_user_oauth; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_user_oauth UNIQUE (oauth_subject);


--
-- TOC entry 3639 (class 2606 OID 17325)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3623 (class 1259 OID 17332)
-- Name: ix_refresh_token_expires_at; Type: INDEX; Schema: public; Owner: jashawnrogers
--

CREATE INDEX ix_refresh_token_expires_at ON public.refresh_tokens USING btree (expires_at);


--
-- TOC entry 3624 (class 1259 OID 17331)
-- Name: ix_refresh_token_family_id; Type: INDEX; Schema: public; Owner: jashawnrogers
--

CREATE INDEX ix_refresh_token_family_id ON public.refresh_tokens USING btree (family_id);


--
-- TOC entry 3625 (class 1259 OID 17330)
-- Name: ix_refresh_token_user_id; Type: INDEX; Schema: public; Owner: jashawnrogers
--

CREATE INDEX ix_refresh_token_user_id ON public.refresh_tokens USING btree (user_id);


--
-- TOC entry 3647 (class 2606 OID 17368)
-- Name: refresh_tokens fk1lih5y2npsf8u5o3vhdb9y0os; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3640 (class 2606 OID 17333)
-- Name: appointment_add_ons fk20xwhcg27t1haig3h5mu0c9m9; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointment_add_ons
    ADD CONSTRAINT fk20xwhcg27t1haig3h5mu0c9m9 FOREIGN KEY (add_on_id) REFERENCES public.add_on(id);


--
-- TOC entry 3641 (class 2606 OID 17338)
-- Name: appointment_add_ons fk2ud0pg0qmjuvjcae7n7kpmo3v; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointment_add_ons
    ADD CONSTRAINT fk2ud0pg0qmjuvjcae7n7kpmo3v FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);


--
-- TOC entry 3642 (class 2606 OID 17343)
-- Name: appointments fk5iltr7k9pows18hk8nc101vc1; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk5iltr7k9pows18hk8nc101vc1 FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 3648 (class 2606 OID 17378)
-- Name: service_addons fk82cu0x5dbsuk9888fh1ssmdd0; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.service_addons
    ADD CONSTRAINT fk82cu0x5dbsuk9888fh1ssmdd0 FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 3643 (class 2606 OID 17348)
-- Name: appointments fk886ced1atxgvnf1o3oxtj5m4s; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk886ced1atxgvnf1o3oxtj5m4s FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3649 (class 2606 OID 17373)
-- Name: service_addons fkawndifk8jp0mka7uf47ea8yrr; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.service_addons
    ADD CONSTRAINT fkawndifk8jp0mka7uf47ea8yrr FOREIGN KEY (add_on_id) REFERENCES public.add_on(id);


--
-- TOC entry 3651 (class 2606 OID 17388)
-- Name: services fkhv7d5p40ipfq91065vlmqk8xv; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT fkhv7d5p40ipfq91065vlmqk8xv FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- TOC entry 3650 (class 2606 OID 17383)
-- Name: service_keys fklj3sbkcn3aokatehgg794ll0o; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.service_keys
    ADD CONSTRAINT fklj3sbkcn3aokatehgg794ll0o FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 3645 (class 2606 OID 17358)
-- Name: payment fkmchv2syv4689dbkneloa2aw01; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fkmchv2syv4689dbkneloa2aw01 FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);


--
-- TOC entry 3646 (class 2606 OID 17363)
-- Name: payment fkmi2669nkjesvp7cd257fptl6f; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fkmi2669nkjesvp7cd257fptl6f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3644 (class 2606 OID 17353)
-- Name: loyalty_records fkr1tybx6ui3hpk1ry41h0lfpqu; Type: FK CONSTRAINT; Schema: public; Owner: jashawnrogers
--

ALTER TABLE ONLY public.loyalty_records
    ADD CONSTRAINT fkr1tybx6ui3hpk1ry41h0lfpqu FOREIGN KEY (user_id) REFERENCES public.users(id);


-- Completed on 2026-02-07 10:09:47 MST

--
-- PostgreSQL database dump complete
--

