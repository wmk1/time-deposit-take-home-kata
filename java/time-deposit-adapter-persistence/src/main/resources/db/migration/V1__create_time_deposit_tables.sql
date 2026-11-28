-- NUMERIC(15,2): up to 15 significant digits a double represents every cent exactly (the decimal value
-- survives the round trip through double), so the double balance of TimeDeposit loses nothing.
CREATE TABLE "timeDeposits" (
    id INTEGER PRIMARY KEY,
    "planType" TEXT NOT NULL,
    days INTEGER NOT NULL,
    balance NUMERIC(15,2) NOT NULL
);

CREATE TABLE withdrawals (
    id INTEGER PRIMARY KEY,
    "timeDepositId" INTEGER NOT NULL REFERENCES "timeDeposits" (id),
    amount NUMERIC(15,2) NOT NULL,
    date DATE NOT NULL
);

CREATE INDEX withdrawals_time_deposit_id_idx ON withdrawals ("timeDepositId");
