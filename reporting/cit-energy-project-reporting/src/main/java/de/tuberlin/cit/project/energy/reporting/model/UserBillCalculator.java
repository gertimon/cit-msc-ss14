package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.reporting.ReportGenerator;

import java.util.*;

/**
 * Created by fubezz on 11.08.14.
 */
public class UserBillCalculator {


    long firstPowerOccurenceOffice = 0;
    long firstPowerOccurenceAsok = 0;
    List<UsageTimeFrame> timeFrames;

    private class UserTraffics{
        HashMap<String, float[]> userTrafficOffice;
        HashMap<String, float[]> userTrafficAsok;

        private UserTraffics(HashMap<String, float[]> userTrafficAsok, HashMap<String, float[]> userTrafficOffice) {
            this.userTrafficAsok = userTrafficAsok;
            this.userTrafficOffice = userTrafficOffice;
        }

        public HashMap<String, float[]> getUserTrafficAsok() {

            return userTrafficAsok;
        }

        public HashMap<String, float[]> getUserTrafficOffice() {
            return userTrafficOffice;
        }

    }


    public UserBillCalculator(LinkedList<UsageTimeFrame> frames){
        timeFrames = frames;

    }

    public List<BillForAllServers> getBill(String user){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        long todaySeconds = today.getTimeInMillis() / 1000;
        int size = timeFrames.size();


        List<BillForAllServers> billList = makeBill(timeFrames, user);

        return billList;
    }

    private List<BillForAllServers> makeBill(List<UsageTimeFrame> importantFrames, String user) {

        List<BillForAllServers> billList = new LinkedList<BillForAllServers>();
        for (UsageTimeFrame frame : importantFrames){
            float[] office = new float[3600];
            float[] asok = new float[3600];
            genPowerArray(office,asok,frame);
            UserTraffics usersTraffic = generateUserTrafficMap(frame);
            HashMap<String, long[]> userStorage = generateUserStrorageMap(frame);
            BillForAllServers bills = computeBill(office,asok,usersTraffic,userStorage,user);
            bills.setFromTime(frame.getStartTime());
            bills.setToTime(frame.getEndTime());
            billList.add(bills);
        }
        return billList;
    }

    private BillForAllServers computeBill(float[] office, float[] asok, UserTraffics usersTraffic, HashMap<String, long[]> userStorage, String user) {
        //OFFICE
        long[] userStore = userStorage.get(user);
        if (userStore == null){
            userStore = new long[3600];
            Arrays.fill(userStore,0);
        }
        float[] userTrafficAsok = usersTraffic.getUserTrafficAsok().get(user);
        if (userTrafficAsok == null){
            userTrafficAsok = new float[3600];
            Arrays.fill(userTrafficAsok,0);
        }
        float[] userTrafficOffice = usersTraffic.getUserTrafficOffice().get(user);
        if (userTrafficOffice == null){
            userTrafficOffice = new float[3600];
            Arrays.fill(userTrafficOffice,0);
        }
        Bill asokBill = computePrice("CitPrijectAsok05",asok,400,userTrafficAsok,userStore,usersTraffic.getUserTrafficAsok(),userStorage,user);
        Bill officeBill = computePrice("CitProjectOffice",office,75,userTrafficOffice,userStore,usersTraffic.getUserTrafficOffice(),userStorage,user);
        BillForAllServers compBill = new BillForAllServers(asokBill,officeBill);
        return compBill;
    }

    private Bill computePrice(String serverName, float[] server, int idlePower,float[] userTrafficForServer,long[] userStore ,HashMap<String, float[]> usersTrafficForServer, HashMap<String, long[]> usersStorage, String user) {
        float[] pricePart = new float[3600];
        for (int i = 0; i < 3600; i++){
            if (userTrafficForServer[i] == 0){
                Set<String> keys = usersStorage.keySet();
                if (keys.size() > 1){
                    long userPart = userStore[i];
                    long rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                            rest += usersStorage.get(k)[i];
                    }
                    if (rest != 0) pricePart[i] = (userPart/rest)*server[i];
                    else pricePart[i] = server[i];
                }
            }else{
                Set<String> keys = usersStorage.keySet();
                if (keys.size() > 1){
                    long userPart = userStore[i];
                    long rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                            rest += usersStorage.get(k)[i];
                    }
                    if (rest != 0) pricePart[i] = (userPart/rest)*server[i];
                    else pricePart[i] = idlePower;
                }else pricePart[i] = idlePower;
                keys = usersTrafficForServer.keySet();
                if (keys.size()>1){
                    float userPart = userTrafficForServer[i];
                    float rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                            rest += usersTrafficForServer.get(k)[i];
                    }
                    if (rest != 0) pricePart[i] += (userPart/rest)*(server[i] - idlePower);
                    else pricePart[i] += server[i] - idlePower;
                }pricePart[i] += server[i] - idlePower;
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
        double price = kWhOfUser * 0.2;

        averageTraffic = averageTraffic / 3600;
        averageStorage = averageStorage/3600;

        Bill bill = new Bill(serverName,user,kWhOfUser,averageTraffic,averageStorage,price);
        return bill;
    }

    private HashMap<String, long[]> generateUserStrorageMap(UsageTimeFrame frame) {
        HashMap<String,long[]> userStorage = new HashMap<String,long[]>();
        for (StorageHistoryEntry entry : frame.getStorageUsage()) {
            long[] userArray = null;
                if (userStorage.containsKey(entry.getUsername())) {
                    userArray = userStorage.get(entry.getUsername());
                } else {
                    userArray = new long[3600];
                    Arrays.fill(userArray, -1);
                    userStorage.put(entry.getUsername(), userArray);
                }
                long diff = entry.getTimestamp() - firstPowerOccurenceAsok;
                if (diff >= 0) {
                    userArray[(int) diff] = entry.getUsedBytes();
                }

        }
        fillRestOfStorageArray(userStorage,frame.getInitialStorageEntry().getUsedBytes());
        return userStorage;
    }

    private void fillRestOfStorageArray(HashMap<String, long[]> userStorage, long startValue) {
        for (long[] traffArray : userStorage.values()){
            long current = startValue;
            for (int i = 0; i < 3600; i++){
                if (traffArray[i] == -1) {
                    traffArray[i] = current;
                }else current = traffArray[i];
            }
        }
    }

    private UserTraffics generateUserTrafficMap(UsageTimeFrame frame) {
        HashMap<String,float[]> userTrafficOffice = new HashMap<String,float[]>();
        HashMap<String,float[]> userTrafficAsok = new HashMap<String,float[]>();

        for (TrafficHistoryEntry entry : frame.getTrafficUsage()){
            float[] userArray = null;
            if (entry.getHostname().equals("CitProjectAsok05")){
                if (userTrafficAsok.containsKey(entry.getUsername())){
                    userArray = userTrafficAsok.get(entry.getUsername());
                }else{
                    userArray = new float[3600];
                    Arrays.fill(userArray,-1);
                    userTrafficAsok.put(entry.getUsername(), userArray);
                }
                long diff = entry.getTimestamp() - firstPowerOccurenceAsok;
                if (diff >= 0){
                    if (userArray[(int)diff] >= 0) userArray[(int)diff] += entry.getUsedBytes();
                    else userArray[(int)diff] = entry.getUsedBytes();
                }
            }else if (entry.getHostname().equals("CitProjectOffice")){
                if (userTrafficOffice.containsKey(entry.getUsername())){
                    userArray = userTrafficOffice.get(entry.getUsername());
                }else{
                    userArray = new float[3600];
                    Arrays.fill(userArray,-1);
                    userTrafficOffice.put(entry.getUsername(), userArray);
                }
                long diff = entry.getTimestamp() - firstPowerOccurenceOffice;
                if (diff >= 0){
                    if (userArray[(int)diff] >= 0) userArray[(int)diff] += entry.getUsedBytes();
                    else userArray[(int)diff] = entry.getUsedBytes();
                }
            }
        }
        fillRestOfArray(userTrafficOffice);
        fillRestOfArray(userTrafficAsok);
        return new UserTraffics(userTrafficAsok,userTrafficOffice);
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

    private void genPowerArray(float[] office, float[] asok, UsageTimeFrame frame){
        int i = 0;
        int j = 0;
        long lastTimeOffice = 0;
        long lastTimeAsok = 0;
        boolean foundFirstOffice = false;
        boolean foundFirstAsok = false;
        Iterator<PowerHistoryEntry> it = frame.getPowerUsage().iterator();
        while (it.hasNext()){
            PowerHistoryEntry entry = it.next();
            if (entry.getHostname().equals("CitProjectOffice") && !foundFirstOffice){
                office[0] = entry.getUsedPower();
                it.remove();
                i++;
                firstPowerOccurenceOffice = entry.getTimestamp();
                lastTimeOffice = entry.getTimestamp();
                foundFirstOffice = true;
            }
            if (entry.getHostname().equals("CitProjectAsok05") && !foundFirstAsok){
                asok[0] = entry.getUsedPower();
                it.remove();
                j++;
                firstPowerOccurenceAsok = entry.getTimestamp();
                lastTimeAsok = entry.getTimestamp();
                foundFirstAsok = true;
            }
            if (foundFirstAsok && foundFirstOffice) break;
        }
        for (PowerHistoryEntry entry : frame.getPowerUsage()){
            if (entry.getHostname().equals("CitProjectOffice")){
                long currentTime = entry.getTimestamp();
                long diff = currentTime - lastTimeOffice;
                for (int k = 0; k < diff; k++){
                    if (i+k < 3600) office[i+k] = entry.getUsedPower();
                }
                i += diff;
                lastTimeOffice = currentTime;
            }
            if (entry.getHostname().equals("CitProjectAsok05")){
                long currentTime = entry.getTimestamp();
                long diff = currentTime - lastTimeAsok;
                for (int k = 0; k < diff; k++){
                    if (j+k < 3600) asok[j+k] = entry.getUsedPower();
                }
                j += diff;
                lastTimeAsok = currentTime;
            }
        }
    }


}
