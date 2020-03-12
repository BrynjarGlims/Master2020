package StoringResults;

public class Converter {

    public static String periodConverter(int periodID){
        switch (periodID){
            case 0:
                return "Monday";
            case 1:
                return "Tuesday";
            case 2:
                return "Wednesday";
            case 3:
                return "Thursday";
            case 4:
                return "Friday";
            case 5:
                return "Saturday";
        }
        return "No day found";

    }

    public static String dividableConverter(boolean isDividable){
        if (isDividable)
            return "dividable";
        else
            return "not dividable";
    }


    public static int getPeriod(int[] orderPeriods){
        int counter = 0;
        for (int isPeriod : orderPeriods){
            if (isPeriod == 1) {
                return counter;
            }
            counter++;
        }
        return -1;
    }



    }
