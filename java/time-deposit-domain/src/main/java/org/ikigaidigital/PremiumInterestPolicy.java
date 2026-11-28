package org.ikigaidigital;

/** Premium plan: 5% a year, interest starts after 45 days (day 46 onwards), without an upper day limit. */
public final class PremiumInterestPolicy extends AnnualRateInterestPolicy {

    public PremiumInterestPolicy() {
        super("premium", 0.05);
    }

    @Override
    boolean paysInterestOnDay(int days) {
        return days > 45;
    }
}
