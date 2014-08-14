package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.reporting.ReportGenerator;

import java.util.*;

/**
 * Created by fubezz on 11.08.14.
 */
public class UserBillCalculator {

    ReportGenerator generator;
    long firstPowerOccurenceOffice = 0;
    long firstPowerOccurenceAsok = 0;

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


    public UserBillCalculator(){
        generator = new ReportGenerator();
    }

    public Bill getBill(String user, int timeWindows){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        long todaySeconds = today.getTimeInMillis() / 1000;
        int days = 1;
        // last 7 days
        UsageReport report = generator.getReport(todaySeconds - 60 * 60 * 2 * days, todaySeconds, 60*60);

        generator.quit();
       // System.err.println("Windowcount: " + report.getUsageTimeFrames().size());
        int size = report.getUsageTimeFrames().size();
        System.err.println("Size: " + size);
        List<UsageTimeFrame> importantFrames = new LinkedList<UsageTimeFrame>();
        importantFrames.add(report.getUsageTimeFrames().get(0));
        System.err.println(importantFrames.size());
        List<Bill> billList = makeBill(importantFrames, user);
        System.err.println("Size: " + billList.size()+ "Price : " + billList.get(0).getPrice());
        return null;
    }

    private List<Bill> makeBill(List<UsageTimeFrame> importantFrames, String user) {

        List<Bill> billList = new LinkedList<Bill>();
        for (UsageTimeFrame frame : importantFrames){
            float[] office = new float[3600];
            float[] asok = new float[3600];
            genPowerArray(office,asok,frame);
            UserTraffics usersTraffic = generateUserTrafficMap(frame);
            HashMap<String, long[]> userStorage = generateUserStrorageMap(frame);
            billList.add(computeBill(office,asok,usersTraffic,userStorage,user));
        }
        return billList;
    }

    private Bill computeBill(float[] office, float[] asok, UserTraffics usersTraffic, HashMap<String, long[]> userStorage, String user) {
        //OFFICE
        long[] userStore = userStorage.get(user);
        float[] userTrafficAsok = usersTraffic.getUserTrafficAsok().get(user);
        float[] userTrafficOffice = usersTraffic.getUserTrafficOffice().get(user);
        System.err.println(usersTraffic.getUserTrafficOffice().values().size());

        float[] pricePart = new float[3600];
        for (int i = 0; i < 3600; i++){
            if (userTrafficOffice[i] == 0){
                Set<String> keys = userStorage.keySet();
                if (keys.size() > 1){
                    long userPart = userStorage.get(user)[i]/2;
                    long rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                        rest += userStorage.get(k)[i]/2;
                    }
                    if (rest != 0) pricePart[i] = (userPart/rest)*office[i];
                    else pricePart[i] = office[i];
                }

            }else{
                Set<String> keys = userStorage.keySet();
                if (keys.size() > 1){
                    long userPart = userStorage.get(user)[i]/2;
                    long rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                            rest += userStorage.get(k)[i]/2;
                    }
                    if (rest != 0) pricePart[i] = (userPart/rest)*office[i];
                    else pricePart[i] = 50;
                }else pricePart[i] = 50;
                keys = usersTraffic.getUserTrafficOffice().keySet();
                if (keys.size()>1){
                    float userPart = userTrafficOffice[i];
                    float rest = 0;
                    for (String k : keys){
                        if (!k.equals(user))
                            rest += usersTraffic.getUserTrafficOffice().get(k)[i];
                    }
                    if (rest != 0) pricePart[i] += (userPart/rest)*(office[i] - 50);
                    else pricePart[i] += office[i] - 50;
                }pricePart[i] += office[i] - 50;
            }
        }
        float sum = 0;
        for (float x : pricePart){
            sum += x;
        }
        double price = sum/1000 * 0.2;
        Bill bill = new Bill(user,sum,null,price);
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
        fillRestOfStorageArray(userStorage);
        return userStorage;
    }

    private void fillRestOfStorageArray(HashMap<String, long[]> userStorage) {
        for (long[] traffArry : userStorage.values()){
            int i = 3600-1;
            long lastValue = traffArry[i];
            if (lastValue < 0) lastValue = 0;
            for (i = 3600 -2; i >= 0; i--){
                if (traffArry[i] == -1){
                    traffArry[i] = lastValue;
                }else if(traffArry[i] > -1){
                    lastValue = traffArry[i];
                }
            }
        }
    }

    private UserTraffics generateUserTrafficMap(UsageTimeFrame frame) {
        HashMap<String,float[]> userTrafficOffice = new HashMap<String,float[]>();
        HashMap<String,float[]> userTrafficAsok = new HashMap<String,float[]>();

        for (TrafficHistoryEntry entry : frame.getTrafficUsage()){
            float[] userArray = null;
            System.err.println(entry.getUsername());
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
                    userArray[(int)diff] = entry.getUsedBytes();
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
                    userArray[(int)diff] = entry.getUsedBytes();
                }
            }
        }
        fillRestOfArray(userTrafficOffice);
        fillRestOfArray(userTrafficAsok);
        return new UserTraffics(userTrafficOffice,userTrafficAsok);
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
                long diff = lastTimeOffice - currentTime;
                for (int k = 0; k < diff; k++){
                    if (i+k < 3600) office[i+k] = entry.getUsedPower();
                }
                i += diff;
            }
            if (entry.getHostname().equals("CitProjectAsok05")){
                long currentTime = entry.getTimestamp();
                long diff = lastTimeAsok - currentTime;
                for (int k = 0; k < diff; k++){
                    if (j+k < 3600) asok[j+k] = entry.getUsedPower();
                }
                j += diff;
            }
        }
        for (int k = 0; k < 3600; k++){
            if (asok[k] == 0) asok[k] = 400;
            if (office[k] == 0) office[k] = 500;
        }

    }

    public static void main(String[] args){
       UserBillCalculator calc = new UserBillCalculator();
       Bill test = calc.getBill("mpjss14", 1);
    }

}
