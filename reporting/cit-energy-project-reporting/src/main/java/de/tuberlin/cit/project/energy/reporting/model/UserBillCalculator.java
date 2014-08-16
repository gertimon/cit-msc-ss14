package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.reporting.Properties;

import java.util.*;

/**
 * Created by fubezz on 11.08.14.
 */
public class UserBillCalculator {


//    long firstPowerOccurenceOffice = 0;
//    long firstPowerOccurenceAsok = 0;
    HashMap<String,Integer> idlePowers;
    HashMap<String,Long> firstOccurencesServer;
    List<UsageTimeFrame> timeFrames;

    private class UserTrafficOfServer {
        String name;
        HashMap<String, float[]> userTraffic;
        // HashMap<String, float[]> userTrafficAsok;

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
        firstOccurencesServer = new HashMap<String,Long>();
        idlePowers = new HashMap<>();
        idlePowers.put("CitProjectAsok05",400);
        idlePowers.put("CitProjectOffice",75);

    }

    public List<HashMap<String, BillForAllServers>> getBill(){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        long todaySeconds = today.getTimeInMillis() / 1000;
        int size = timeFrames.size();


        List<HashMap<String, BillForAllServers>> billList = makeBill(timeFrames);

        return billList;
    }

    private List<HashMap<String, BillForAllServers>> makeBill(List<UsageTimeFrame> importantFrames) {

        List<HashMap<String,BillForAllServers>> billList = new LinkedList<>();

        for (UsageTimeFrame frame : importantFrames){
            Set<String> dataNodes = frame.getPowerUsageByHost().keySet();
            Iterator<String> nodeIt = dataNodes.iterator();
//            float[] office = new float[3600];
//            float[] asok = new float[3600];
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
//        float[] userTrafficAsok = usersTraffic.getUserTrafficAsok().get(user);
//        if (userTrafficAsok == null){
//            userTrafficAsok = new float[3600];
//            Arrays.fill(userTrafficAsok,0);
//        }
//        float[] userTrafficOffice = usersTraffic.getUserTrafficOffice().get(user);
//        if (userTrafficOffice == null){
//            userTrafficOffice = new float[3600];
//            Arrays.fill(userTrafficOffice,0);
//        }
//        Bill asokBill = computePrice("CitProjectAsok05",asok,400,userTrafficAsok,userStore,usersTraffic.getUserTrafficAsok(),userStorage,user);
//        Bill officeBill = computePrice("CitProjectOffice",office,75,userTrafficOffice,userStore,usersTraffic.getUserTrafficOffice(),userStorage,user);
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
//                if (keys.size()>1){
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
        if (frame.getStorageUsage().size() > 0) {
            for (StorageHistoryEntry entry : frame.getStorageUsage()) {
                long[] userArray = null;
                if (userStorage.containsKey(entry.getUsername())) {
                    userArray = userStorage.get(entry.getUsername());
                } else {
                    userArray = new long[3600];
                    Arrays.fill(userArray, -1);
                    userStorage.put(entry.getUsername(), userArray);
                }
                long diff = entry.getTimestamp() - firstOccurencesServer.get("CitProjectOffice");
                if (diff >= 0) {
                    userArray[(int) diff] = entry.getUsedBytes();
                }

            }
            fillRestOfStorageArray(userStorage,frame.getInitialStorageEntry().getUsedBytes());
        }else{
            long[] userArray = new long[3600];
            Arrays.fill(userArray,frame.getInitialStorageEntry().getUsedBytes());
            userStorage.put(frame.getInitialStorageEntry().getUsername(),userArray);
        }
        return userStorage;
    }

    private void fillRestOfStorageArray(HashMap<String, long[]> userStorage, long startValue) {
        for (long[] traffArray : userStorage.values()){
            long current = startValue;
            for (int i = 0; i < 3600; i++){
                if (traffArray[i] < 0) {
                    traffArray[i] = current;
                }else current = traffArray[i];
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
                    long diff = entry.getTimestamp() - firstOccurencesServer.get(serverName);
                    if (diff >= 0) {
                        if (userArray[(int) diff] >= 0) userArray[(int) diff] += entry.getUsedBytes();
                        else userArray[(int) diff] = entry.getUsedBytes();
                    }
                }
            }
            fillRestOfArray(userTrafficForServer);
            userTraffic.setUserTraffic(userTrafficForServer);
            serverList.add(userTraffic);
        }
        return serverList;
//
//        HashMap<String,float[]> userTrafficOffice = new HashMap<String,float[]>();
//        HashMap<String,float[]> userTrafficAsok = new HashMap<String,float[]>();
//
//        for (TrafficHistoryEntry entry : frame.getTrafficUsage()){
//            float[] userArray = null;
//            if (entry.getHostname().equals("CitProjectAsok05")){
//                if (userTrafficAsok.containsKey(entry.getUsername())){
//                    userArray = userTrafficAsok.get(entry.getUsername());
//                }else{
//                    userArray = new float[3600];
//                    Arrays.fill(userArray,-1);
//                    userTrafficAsok.put(entry.getUsername(), userArray);
//                }
//                long diff = entry.getTimestamp() - firstPowerOccurenceAsok;
//                if (diff >= 0){
//                    if (userArray[(int)diff] >= 0) userArray[(int)diff] += entry.getUsedBytes();
//                    else userArray[(int)diff] = entry.getUsedBytes();
//                }
//            }else if (entry.getHostname().equals("CitProjectOffice")){
//                if (userTrafficOffice.containsKey(entry.getUsername())){
//                    userArray = userTrafficOffice.get(entry.getUsername());
//                }else{
//                    userArray = new float[3600];
//                    Arrays.fill(userArray,-1);
//                    userTrafficOffice.put(entry.getUsername(), userArray);
//                }
//                long diff = entry.getTimestamp() - firstPowerOccurenceOffice;
//                if (diff >= 0){
//                    if (userArray[(int)diff] >= 0) userArray[(int)diff] += entry.getUsedBytes();
//                    else userArray[(int)diff] = entry.getUsedBytes();
//                }
//            }
//        }
//        fillRestOfArray(userTrafficOffice);
//        fillRestOfArray(userTrafficAsok);
//        return new UserTraffics(userTrafficAsok,userTrafficOffice);
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
        while (dataNodeIt.hasNext()) {
            String dataNodeName = dataNodeIt.next();
            float[] dataNodePower = new float[3600];
            int i = 0;
            long lastTimeServer = 0;
            Iterator<PowerHistoryEntry> it = frame.getPowerUsage().iterator();

            while (it.hasNext()) {
                PowerHistoryEntry entry = it.next();
                if (entry.getHostname().equals(dataNodeName)) {
                    dataNodePower[0] = entry.getUsedPower();
                    it.remove();
                    i++;
                    firstOccurencesServer.put(dataNodeName, entry.getTimestamp());
                    lastTimeServer = entry.getTimestamp();
                    break;
                }
            }
            for (PowerHistoryEntry entry : frame.getPowerUsage()) {
                if (entry.getHostname().equals(dataNodeName)) {
                    long currentTime = entry.getTimestamp();
                    long diff = currentTime - lastTimeServer;
                    for (int k = 0; k < diff; k++) {
                        if (i + k < 3600) dataNodePower[i + k] = entry.getUsedPower();
                    }
                    i += diff;
                    lastTimeServer = currentTime;
                }
            }
            dataNodesPowers.put(dataNodeName, dataNodePower);
        }
        return dataNodesPowers;
    }
}
//        int i = 0;
//        int j = 0;
//        long lastTimeOffice = 0;
//        long lastTimeAsok = 0;
//        boolean foundFirstOffice = false;
//        boolean foundFirstAsok = false;
//        Iterator<PowerHistoryEntry> it = frame.getPowerUsage().iterator();
//        while (it.hasNext()){
//            PowerHistoryEntry entry = it.next();
//            if (entry.getHostname().equals("CitProjectOffice") && !foundFirstOffice){
//                office[0] = entry.getUsedPower();
//                it.remove();
//                i++;
//                firstPowerOccurenceOffice = entry.getTimestamp();
//                lastTimeOffice = entry.getTimestamp();
//                foundFirstOffice = true;
//            }
//            if (entry.getHostname().equals("CitProjectAsok05") && !foundFirstAsok){
//                asok[0] = entry.getUsedPower();
//                it.remove();
//                j++;
//                firstPowerOccurenceAsok = entry.getTimestamp();
//                lastTimeAsok = entry.getTimestamp();
//                foundFirstAsok = true;
//            }
//            if (foundFirstAsok && foundFirstOffice) break;
//        }
//        for (PowerHistoryEntry entry : frame.getPowerUsage()){
//            if (entry.getHostname().equals("CitProjectOffice")){
//                long currentTime = entry.getTimestamp();
//                long diff = currentTime - lastTimeOffice;
//                for (int k = 0; k < diff; k++){
//                    if (i+k < 3600) office[i+k] = entry.getUsedPower();
//                }
//                i += diff;
//                lastTimeOffice = currentTime;
//            }
//            if (entry.getHostname().equals("CitProjectAsok05")){
//                long currentTime = entry.getTimestamp();
//                long diff = currentTime - lastTimeAsok;
//                for (int k = 0; k < diff; k++){
//                    if (j+k < 3600) asok[j+k] = entry.getUsedPower();
//                }
//                j += diff;
//                lastTimeAsok = currentTime;
//            }
//        }
//    }


//}
