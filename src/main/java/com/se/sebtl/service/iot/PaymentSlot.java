package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;

@Service
public class PaymentSlot {
    private boolean cashAvailable = true;
    private boolean bankingAvailable = true;

    public void setCashFail() { cashAvailable = false; }
    public void setBankingFail() { bankingAvailable = false; }
    public void setBothFail() { cashAvailable = false; bankingAvailable = false; }

    public boolean isCashAvailable() { return cashAvailable; }
    public boolean isBankingAvailable() { return bankingAvailable; }

    public boolean processQR(double fee) { return bankingAvailable; }
    public boolean processCash(double fee) { return cashAvailable; }
}