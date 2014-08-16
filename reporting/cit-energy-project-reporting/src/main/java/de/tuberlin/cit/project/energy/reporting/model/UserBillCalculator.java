package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.reporting.Properties;

import java.util.*;

/**
 * Created by fubezz on 11.08.14.
 */
public class UserBillCalculator {

    HashMap<String,Integer> idlePowers;
    List<UsageTimeFrame> timeFrames;

    private class ServerTraffic {
        private final UsageTimeFrame timeFrame;
        private final HashMap<String, float[]> userTraffic;
        private final HashMap<String, Integer> lastUserTrafficEnd;

        private ServerTraffic(UsageTimeFrame timeFrame) {
            this.timeFrame = timeFrame;
            this.userTraffic = new HashMap<>();
            this.lastUserTrafficEnd = new HashMap<>();
        }

        public HashMap<String, float[]> getUserTraffic() {
            return userTraffic;
        }

        public void addEntry(TrafficHistoryEntry entry) {
            float traffic[] = this.userTraffic.get(entry.getUsername());
            int rangeStart;
            int rangeEnd = (int) (this.timeFrame.getStartTime() - entry.getTimestamp());

            if (traffic == null) {
                traffic = new float[(int) this.timeFrame.getDurationInSeconds()];
                this.userTraffic.put(entry.getUsername(), traffic);
                rangeStart = 0;

            } else {
                rangeStart = this.lastUserTrafficEnd.get(entry.getUsername()) + 1;
            }

            for (int i = rangeStart; i <= rangeEnd; i++) {
                traffic[i] = entry.getUsedBytes();
            }

            this.lastUserTrafficEnd.put(entry.getUsername(), rangeEnd);
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
            HashMap<String, ServerTraffic> usersTraffic = generateUserTrafficMap(frame);
            HashMap<String, long[]> userStorage = generateUserStorageMap(frame);
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

    private BillForAllServers computeBill(HashMap<String,float[]> serverPowers, HashMap<String, ServerTraffic> usersTraffic, HashMap<String, long[]> userStorage, String user) {

        long[] userStore = userStorage.get(user);
        if (userStore == null){
            userStore = new long[3600];
            Arrays.fill(userStore,0);
        }
        List<Bill> userBillsForServer = new LinkedList<>();
        for (String serverName : usersTraffic.keySet()) {
            ServerTraffic traffic = usersTraffic.get(serverName);
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
                pricePart[i] = (userPart/rest)*idlePower;
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

    private HashMap<String, long[]> generateUserStorageMap(UsageTimeFrame frame) {
        HashMap<String,long[]> userStorage = new HashMap<String,long[]>();

        for (StorageHistoryEntry entry : frame.getStorageUsage()) {
            long[] storageValues = userStorage.get(entry.getUsername());

            if (storageValues == null) {
                storageValues = new long[3600];
                Arrays.fill(storageValues, -1);
                userStorage.put(entry.getUsername(), storageValues);
            }

            int offset = (int) (entry.getTimestamp() - frame.getStartTime());
            storageValues[offset] = entry.getUsedBytes();
        }

        for (String username : frame.getInitialStorageEntries().keySet()) {
            if (userStorage.containsKey(username)) {
                long[] storageValues = userStorage.get(username);
                long lastValue = frame.getInitialStorageEntries().get(username);
                for (int i = 0; i < storageValues.length; i++) {
                    if (storageValues[i] == -1)
                        storageValues[i] = lastValue;
                    else
                        lastValue = storageValues[i];
                }

            } else {
                long[] storageValues = new long[3600];
                Arrays.fill(storageValues, frame.getInitialStorageEntries().get(username));
                userStorage.put(username, storageValues);
            }
        }

        return userStorage;
    }

    /** Produces a Server->User->Traffic mapping. */
    private HashMap<String, ServerTraffic> generateUserTrafficMap(UsageTimeFrame frame) {
        HashMap<String, ServerTraffic> trafficByHost = new HashMap<>();
        List<TrafficHistoryEntry> trafficEntries = frame.getTrafficUsage();

        for (TrafficHistoryEntry entry : trafficEntries) {
            ServerTraffic serverTraffic = trafficByHost.get(entry.getHostname());

            if (serverTraffic == null) {
                serverTraffic = new ServerTraffic(frame);
                trafficByHost.put(entry.getHostname(), serverTraffic);
            }

            serverTraffic.addEntry(entry);
        }

        return trafficByHost;
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

            for (i = 0; i < hostPower.length && hostPower[i] == -1; i++);
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

