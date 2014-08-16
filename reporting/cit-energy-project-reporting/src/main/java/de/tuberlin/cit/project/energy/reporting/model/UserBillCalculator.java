package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.reporting.Properties;

import java.util.*;

/**
 * Created by fubezz on 11.08.14.
 */
public class UserBillCalculator {

    HashMap<String,Integer> idlePowers;
    List<UsageTimeFrame> timeFrames;

    private class UserTrafficOfServer {
        String name;
        HashMap<String, float[]> userTraffic;

        private UserTrafficOfServer(String name) {
            this.name = name;
        }

        public void setUserTraffic(HashMap<String,float[]> userTraffic) {
            this.userTraffic = userTraffic;
        }
        public HashMap<String, float[]> getUserTraffic() {
            return userTraffic;
        }
        public String getServerName(){
            return this.name;
        }
    }


    public UserBillCalculator(LinkedList<UsageTimeFrame> frames){
        timeFrames = frames;
        idlePowers = new HashMap<>();
        idlePowers.put("CitProjectAsok05",400);
        idlePowers.put("CitProjectOffice",75);
    }

    public List<HashMap<String, BillForAllServers>> getBill(){
        return makeBill(this.timeFrames);
    }

    private List<HashMap<String, BillForAllServers>> makeBill(List<UsageTimeFrame> importantFrames) {

        List<HashMap<String,BillForAllServers>> billList = new LinkedList<>();

        for (UsageTimeFrame frame : importantFrames){
            Set<String> dataNodes = frame.getPowerUsageByHost().keySet();
            Iterator<String> nodeIt = dataNodes.iterator();
            HashMap<String,float[]> dataNodePower = genPowerArray(nodeIt ,frame);
            List<UserTrafficOfServer> usersTraffic = generateUserTrafficMap(frame);
            HashMap<String, long[]> userStorage = generateUserStrorageMap(frame);
            Iterator<String> users = userStorage.keySet().iterator();
            HashMap<String,BillForAllServers> billforUserOfServers = new HashMap<>();
            while(users.hasNext()){
                String userName = users.next();
                BillForAllServers bills = computeBill(dataNodePower, usersTraffic, userStorage, userName);
                bills.setStartTime(frame.getStartTime());
                bills.setEndTime(frame.getEndTime());
                billforUserOfServers.put(userName,bills);
            }
            billList.add(billforUserOfServers);
        }
        return billList;
    }

    private BillForAllServers computeBill(HashMap<String,float[]> serverPowers, List<UserTrafficOfServer> usersTraffic, HashMap<String, long[]> userStorage, String user) {

        long[] userStore = userStorage.get(user);
        if (userStore == null){
            userStore = new long[3600];
            Arrays.fill(userStore,0);
        }
        List<Bill> userBillsForServer = new LinkedList<>();
        for (UserTrafficOfServer traffic : usersTraffic){
            String serverName = traffic.getServerName();
            float[] userTrafficOFServer = traffic.getUserTraffic().get(user);
            if (userTrafficOFServer == null){
                userTrafficOFServer = new float[3600];
                Arrays.fill(userTrafficOFServer,0);
            }
            Bill serverBill = computePrice(serverName,serverPowers.get(serverName),idlePowers.get(serverName),userTrafficOFServer,userStore,traffic.getUserTraffic(),userStorage,user);
            userBillsForServer.add(serverBill);
        }
        BillForAllServers compBill = new BillForAllServers(userBillsForServer);
        return compBill;
    }

    private Bill computePrice(String serverName, float[] server, int idlePower,float[] userTrafficForServer,long[] userStore ,HashMap<String, float[]> usersTrafficForServer, HashMap<String, long[]> usersStorage, String user) {
        float[] pricePart = new float[3600];
        double allTraffic = 0;
        double allStorage = 0;
        for (int i = 0; i < 3600; i++){
            if (userTrafficForServer[i] == 0){
                Set<String> keys = usersStorage.keySet();
                long userPart = userStore[i];
                long rest = 0;
                for (String k : keys){
                    rest += usersStorage.get(k)[i];
                }
                pricePart[i] = (userPart/rest)*server[i];
                allStorage += rest;
            }
            else{
                Set<String> keys = usersStorage.keySet();
                long userPart = userStore[i];
                long rest = 0;
                for (String k : keys){
                    rest += usersStorage.get(k)[i];
                    allStorage += rest;
                }
                pricePart[i] = (userPart/rest)*idlePower;
                keys = usersTrafficForServer.keySet();
                float userPart2 = userTrafficForServer[i];
                float rest2 = 0;
                for (String k : keys){
                    rest2 += usersTrafficForServer.get(k)[i];
                    allTraffic +=rest2;
                }
                pricePart[i] += (userPart2/rest2)*(server[i] - idlePower);
            }
        }
        float sum = 0;
        float averageTraffic = 0;
        long averageStorage = 0;

        for (int i = 0;i<3600;i++){
            sum += pricePart[i];
            averageTraffic += userTrafficForServer[i];
            averageStorage += userStore[i];
        }
        float kWhOfUser = (sum/3600)/1000;
        double price = kWhOfUser * Properties.KWH_PRICE;

        averageTraffic = averageTraffic/3600;
        averageStorage = averageStorage/3600;
        allTraffic = (allTraffic/3600);
        allStorage = allStorage/3600;
        double averageStoragePercent = (averageStorage/allStorage) * 100;
        double averageTrafficPercent = (averageTraffic/allTraffic) * 100;
        if (Double.isNaN(averageTrafficPercent)) averageTrafficPercent = 0.0;


        Bill bill = new Bill(serverName,user,kWhOfUser,averageTraffic,averageStorage,price,averageStoragePercent,averageTrafficPercent);
        return bill;
    }

    private HashMap<String, long[]> generateUserStrorageMap(UsageTimeFrame frame) {
        HashMap<String,long[]> userStorage = new HashMap<String,long[]>();

        for (StorageHistoryEntry entry : frame.getStorageUsage()) {
            long[] userArray = userStorage.get(entry.getUsername());

            if (userArray == null) {
                userArray = new long[3600];
                Arrays.fill(userArray, -1);
                userStorage.put(entry.getUsername(), userArray);
            }

            long offset = entry.getTimestamp() - frame.getStartTime();
            if (offset >= 0) {
                userArray[(int) offset] = entry.getUsedBytes();
            }

        }

        for (String username : frame.getInitialStorageEntries().keySet()) {
            if (userStorage.containsKey(username)) {
                fillRestOfStorageArray(userStorage, frame.getInitialStorageEntries().get(username));
            } else {
                long[] userArray = new long[3600];
                Arrays.fill(userArray, frame.getInitialStorageEntries().get(username));
                userStorage.put(username, userArray);
            }
        }

        return userStorage;
    }

    private void fillRestOfStorageArray(HashMap<String, long[]> userStorage, long startValue) {
        for (long[] traffArray : userStorage.values()){
            long current = startValue;
            for (int i = 0; i < 3600; i++){
                if (traffArray[i] < 0)
                    traffArray[i] = current;
                else
                    current = traffArray[i];
            }
        }
    }

    private List<UserTrafficOfServer> generateUserTrafficMap(UsageTimeFrame frame) {
        List<UserTrafficOfServer> serverList = new LinkedList<>();
        Iterator<String> server = frame.getPowerUsageByHost().keySet().iterator();
        while (server.hasNext()){
            String serverName = server.next();
            UserTrafficOfServer userTraffic = new UserTrafficOfServer(serverName);
            HashMap<String,float[]> userTrafficForServer = new HashMap<String,float[]>();
            for (TrafficHistoryEntry entry : frame.getTrafficUsage()) {
                float[] userArray = null;
                if (entry.getHostname().equals(serverName)) {
                    if (userTrafficForServer.containsKey(entry.getUsername())) {
                        userArray = userTrafficForServer.get(entry.getUsername());
                    } else {
                        userArray = new float[3600];
                        Arrays.fill(userArray, -1);
                        userTrafficForServer.put(entry.getUsername(), userArray);
                    }
                    long offset = entry.getTimestamp() - frame.getStartTime();
                    if (offset >= 0) {
                        if (userArray[(int) offset] >= 0)
                            userArray[(int) offset] += entry.getUsedBytes();
                        else
                            userArray[(int) offset] = entry.getUsedBytes();
                    }
                }
            }
            fillRestOfArray(userTrafficForServer);
            userTraffic.setUserTraffic(userTrafficForServer);
            serverList.add(userTraffic);
        }
        return serverList;
    }

    private void fillRestOfArray(HashMap<String, float[]> userTraffic) {
        for (float[] traffArry : userTraffic.values()){
            int i = 3600-1;
            float lastValue = traffArry[i];
            if (lastValue < 0) lastValue = 0;
            for (i = 3600 -2; i >= 0; i--){
                if (traffArry[i] == -1 && lastValue == 0){
                    traffArry[i] = 0;
                }else if(traffArry[i] == -1 && lastValue > 0){
                    traffArry[i] = lastValue;
                }else if(traffArry[i] > -1){
                    lastValue = traffArry[i];
                }
            }
        }
    }

    private HashMap<String, float[]> genPowerArray(Iterator<String> dataNodeIt, UsageTimeFrame frame) {
        HashMap<String, float[]> dataNodesPowers = new HashMap<String, float[]>();
        List<PowerHistoryEntry> powerEntries = frame.getPowerUsage();
        for (PowerHistoryEntry entry : powerEntries) {
            float hostPower[] = dataNodesPowers.get(entry.getHostname());

            if (hostPower == null) {
                hostPower = new float[3600];
                Arrays.fill(hostPower, -1);
                dataNodesPowers.put(entry.getHostname(), hostPower);
            }

            int offset = (int) (entry.getTimestamp() - frame.getStartTime());
            hostPower[offset] = entry.getUsedPower();
        }

        // find initial values and fill empty values with previous values
        for (String hostname : dataNodesPowers.keySet()) {
            float hostPower[] = dataNodesPowers.get(hostname);
            int i;

            for (i = 0; i < hostPower.length && hostPower[i] == -1; i++) {
            }
            hostPower[0] = hostPower[i];

            float lastValue = hostPower[0];
            for (i = 1; i < hostPower.length; i++) {
                if (hostPower[i] == -1)
                    hostPower[i] = lastValue;
                else
                    lastValue = hostPower[i];
            }
        }

        return dataNodesPowers;
    }
}

