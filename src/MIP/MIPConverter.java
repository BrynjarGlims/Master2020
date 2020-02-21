package MIP;
import DataFiles.*;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MIPConverter {


    public static void main(String[] args) {
        int[] array = {1,2,3,4,5};
        for(int variable : array){
            variable = 0;
        }
        System.out.println(Arrays.toString(array));
    }



}
