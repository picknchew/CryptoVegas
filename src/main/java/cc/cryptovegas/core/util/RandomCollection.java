package cc.cryptovegas.core.util;

import java.security.SecureRandom;
import java.util.NavigableMap;
import java.util.TreeMap;

public class RandomCollection<E> {
    private final ThreadLocal<SecureRandom> secureRandom = ThreadLocal.withInitial(SecureRandom::new);
    private final NavigableMap<Double, E> map = new TreeMap<>();
    private double total = 0;

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0) {
            return this;
        }

        total += weight;
        map.put(total, result);

        return this;
    }

    public E next() {
        double value = secureRandom.get().nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
}