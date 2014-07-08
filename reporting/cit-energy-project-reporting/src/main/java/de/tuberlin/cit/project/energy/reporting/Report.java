package de.tuberlin.cit.project.energy.reporting;

/**
 *
 * @author Tobias
 */
public class Report {

    private final String username;
    private final long fromTimeMillis;
    private final long toTimeMillis;

//    private final Plan plan; // static now, overview when a plan was switched would be helpful (TODO plan-switch to zabbix)
    /**
     *
     * @param username
     * @param fromTimeMillis
     * @param toTimeMillis
     */
    public Report(String username, long fromTimeMillis, long toTimeMillis) {
        this.username = username;
        this.fromTimeMillis = fromTimeMillis;
        this.toTimeMillis = toTimeMillis;
        calculate();
    }

    double calculateStorage() {
        return 0.0;
    }

    /**
     * implementation time tests. will be kept as tests itself later.
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder report = new StringBuilder();
        // interesting values
        // complete power usage
        // user power assigned -> cost translation
        // fast traffic used complete
        // cheap traffic used complete
        // fast traffic for user
        // cheap traffic for user
        // user storage median
        //
        return report.toString();
    }

    private void calculate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
