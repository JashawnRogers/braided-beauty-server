package com.braided_beauty.braided_beauty.services;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Thin wrapper around Stripe SDK calls used by {@link PaymentService}.
 */
public interface StripeGateway {

    /**
     * Creates a Stripe refund using the provided parameters.
     */
    Refund createRefund(RefundCreateParams params) throws StripeException;

    /**
     * Creates a Stripe Checkout session using the provided parameters.
     */
    Session createCheckoutSession(SessionCreateParams params) throws StripeException;

    /**
     * Retrieves an existing Stripe Checkout session by ID.
     */
    Session retrieveCheckoutSession(String sessionId) throws StripeException;

    /**
     * Expires an open Stripe Checkout session by ID.
     */
    Session expireCheckoutSession(String sessionId) throws StripeException;
}
