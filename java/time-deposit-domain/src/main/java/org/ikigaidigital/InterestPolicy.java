package org.ikigaidigital;

/**
 * Monthly interest rule of one plan type.
 *
 * <p>Deliberately not sealed: the README asks for a design that can take new interest rules, so a new
 * plan is added by implementing this interface, without changing the existing policies.
 */
public interface InterestPolicy {

    /** Whether this policy calculates the interest for the given plan type. */
    boolean supports(String planType);

    /**
     * Interest for one month as the raw, unrounded double; 0 when the deposit gets no interest.
     * Rounding is not done here: {@link TimeDepositCalculator} rounds once, the same way for every plan.
     */
    double monthlyInterest(TimeDeposit deposit);
}
