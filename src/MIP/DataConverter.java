package MIP;

import DataFiles.Data;
import ProjectReport.DataMIP;

public class DataConverter {

    static Data data;
    static DataMIP dm;


    public static DataMIP convert( Data newData ){
        data = newData;
        dm = new DataMIP();




        return dm;

    }

}
