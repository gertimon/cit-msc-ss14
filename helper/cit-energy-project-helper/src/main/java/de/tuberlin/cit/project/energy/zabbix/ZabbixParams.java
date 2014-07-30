package de.tuberlin.cit.project.energy.zabbix;

public class ZabbixParams {

    public static final String DEFAULT_ZABBIX_REST_URL = "https://mpjss14.cit.tu-berlin.de/zabbix/api_jsonrpc.php";
    public static final String DEFAULT_ZABBIX_USERNAME = "admin";
    public static final String DEFAULT_ZABBIX_PASSWORD = "zabbix!";
    public static final String DEFAULT_ZABBIX_HOST = "mpjss14.cit.tu-berlin.de";
    public static final String DEFAULT_ZABBIX_PORT = "10051";

    public static final String POWER_CONSUMPTION_KEY = "datanode.power";
    public static final String USER_LAST_ADDRESS_MAPPING_KEY = "user.%s.lastAddress";
    public static final String USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY = "user.%s.lastInternalAddress";
    public static final String USER_LAST_USED_PROFILE_KEY = "user.%s.lastUsedProfile";
    public static final String USER_BANDWIDTH_KEY = "user.%s.bandwidth";
    public static final String USER_INTERNAL_BANDWIDTH_KEY = "user.%s.internalBandwidth";
    public static final String USER_DURATION_KEY = "user.%s.duration";
    public static final String USER_DATA_USAGE_DELTA_KEY = "user.%s.dataUsageDeltas";
    public static final String USER_BLOCK_EVENTS_KEY = "user.%s.blockEvents";

    public static final String DATANODE_USER_TEMPLATE_NAME = "CitProjectDatanodeUsers";

    public static final int MAX_USER_IP_MAPPING_AGE = 60 * 60; // use only values updated within the given time frame (in seconds)

    public static final String USERNAME = ""; // tubit username
    public static final String PASSWORD = ""; // tubit password

}
