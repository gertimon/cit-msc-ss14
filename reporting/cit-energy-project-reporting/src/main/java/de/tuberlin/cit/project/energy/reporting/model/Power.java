package de.tuberlin.cit.project.energy.reporting.model;

/**
 * represents power in kwh used for a specific range.
 *
 * @author Tobias
 */
public class Power {

    private final double powerKwh;

    public Power(double powerKwh) {
        this.powerKwh = powerKwh;
    }

    public double getPowerKwh() {
        return powerKwh;
    }

}
