package Master2020.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {



    public static Function<List<?>, Function<Integer, Consumer<Integer>>> reverse = list -> int1 -> int2 -> {
        reverse(list, int1, int2);
    };

    public static Function<List<?>, Function<Integer, Consumer<Integer>>> insert = list -> int1 -> int2 -> {
        insert(list, int1, int2);
    };

    public static Function<List<?>, Function<Integer, Consumer<Integer>>> swap = list -> int1 -> int2 -> {
        Collections.swap(list, int1, int2);
    };

    public static <E> void insert(List<E> list, int pos1, int pos2){
        E insertElement = list.get(pos1);
        if (pos1 < pos2){
            for (int i = pos1 ; i < pos2 ; i++){
                list.set(i, list.get(i + 1));
            }
        }
        else{
            for (int i = pos1 ; i > pos2 ; i--){
                list.set(i, list.get(i - 1));
            }
        }
        list.set(pos2, insertElement);
    }

    public static <E> void reverse(List<E> list, int pos1, int pos2){
        List<E> sublist = list.subList(pos1, pos2 + 1);
        Collections.reverse(sublist);
        int count = 0;
        for (int i = pos1 ; i < pos2 + 1; i++){
            list.set(i, sublist.get(count));
            count++;

        }
    }

    public static <T> List<List<T>> Permutate(List<T> original) {
        if (original.isEmpty()) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        T firstElement = original.remove(0);
        List<List<T>> returnValue = new ArrayList<>();
        List<List<T>> permutations = Permutate(original);
        for (List<T> smallerPermutated : permutations) {
            for (int index=0; index <= smallerPermutated.size(); index++) {
                List<T> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }



    public static void main(String[] args){
        ArrayList<Integer> l = new ArrayList<>();
        for (int i = 0 ; i < 10 ; i++){
            l.add(i);
        }


        Function<List<?>, Function<Integer, Consumer<Integer>>> function = le -> int1 -> int2 -> {
            reverse(le, int1, int2);
        };
        insert(l, 5, 1);
        insert(l, 1, 5);
        System.out.println(l);
    }
}


