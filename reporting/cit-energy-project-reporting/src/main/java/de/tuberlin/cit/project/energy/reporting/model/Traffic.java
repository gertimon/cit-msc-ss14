package de.tuberlin.cit.project.energy.reporting.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * traffic collected for a specific one and all users.
 *
 * @author Tobias
 */
public class Traffic {

    private final Map<Plan, Map<String, Double>> planTrafficMap;

    public Traffic(Map<Plan, Map<String, Double>> planTrafficMap) {
        // TODO fetch values for a specific range
        this.planTrafficMap = planTrafficMap;
    }

    /**
     *
     * @param username
     * @return the traffic given for one user separatet by plan
     */
    public Map<Plan, Double> calculateUserTraffic(String username) {

        Map<Plan, Double> result = new HashMap<>();

        for (Iterator<Plan> it = getPlanIterator(); it.hasNext();) {
            Plan plan = it.next();
            Map<String, Double> traffic = planTrafficMap.get(plan);
            if (traffic != null && traffic.containsKey(username)) {
                result.put(plan, traffic.get(username));
            }
        }

        return result;
    }

    /**
     *
     * @return the sum of traffic given for each plan
     */
    public Map<Plan, Double> getTrafficSum() {

        Map<Plan, Double> result = new HashMap<>();

        for (Iterator<Plan> it = getPlanIterator(); it.hasNext();) {
            Plan plan = it.next();
            Map<String, Double> traffic = planTrafficMap.get(plan);
            if (traffic != null && !traffic.isEmpty()) {
                double trafficSum = 0.0;
                for (double value : traffic.values()) {
                    trafficSum += value;
                }
                result.put(plan, trafficSum);
            }
        }

        return result;
    }

    private Iterator<Plan> getPlanIterator() {
        return planTrafficMap.keySet().iterator();
    }
}
