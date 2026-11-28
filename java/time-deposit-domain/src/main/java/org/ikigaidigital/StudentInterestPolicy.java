package org.ikigaidigital;

/** Student plan: 3% a year, no interest after one year (day 366 onwards). */
public final class StudentInterestPolicy extends AnnualRateInterestPolicy {

    public StudentInterestPolicy() {
        super("student", 0.03);
    }

    @Override
    boolean paysInterestOnDay(int days) {
        return days < 366;
    }
}
