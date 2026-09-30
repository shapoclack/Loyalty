package org.example.service;

import java.security.SecureRandom;

/** Номер карты: префикс 7700 + 12 случайных цифр. */
public class CardNumberGenerator {

    private static final String PREFIX = "7700";
    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(PREFIX);
        while (sb.length() < 16) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
