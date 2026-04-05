package com.braided_beauty.braided_beauty.services;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

/**
 * Production Stripe gateway backed directly by the Stripe Java SDK.
 */
@Component
public class StripeGatewayImpl implements StripeGateway {

    /**
     * Delegates refund creation to the Stripe SDK.
     */
    @Override
    public Refund createRefund(RefundCreateParams params) throws StripeException {
        return Refund.create(params);
    }

    /**
     * Delegates Checkout session creation to the Stripe SDK.
     */
    @Override
    public Session createCheckoutSession(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }

    /**
     * Delegates Checkout session lookup to the Stripe SDK.
     */
    @Override
    public Session retrieveCheckoutSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
