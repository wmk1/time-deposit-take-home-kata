package org.ikigaidigital;

/** Basic plan: 1% a year, without an upper day limit. */
public final class BasicInterestPolicy extends AnnualRateInterestPolicy {

    public BasicInterestPolicy() {
        super("basic", 0.01);
    }

    @Override
    boolean paysInterestOnDay(int days) {
        return true;
    }
}
