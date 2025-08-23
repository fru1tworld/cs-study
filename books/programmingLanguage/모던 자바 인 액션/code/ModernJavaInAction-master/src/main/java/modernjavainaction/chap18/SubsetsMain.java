package modernjavainaction.chap18;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubsetsMain {

  public static void main(String[] args) {
    List<List<Integer>> subs = subsets(Arrays.asList(1, 4, 9));
    subs.forEach(System.out::println);
  }

  public static <T> List<List<T>> subsets(List<T> l) {
    if (l.isEmpty()) {
      List<List<T>> ans = new ArrayList<>();
      ans.add(Collections.emptyList());
      return ans;
    }
    T first = l.get(0);
    List<T> rest = l.subList(1, l.size());
    List<List<T>> subans = subsets(rest);
    List<List<T>> subans2 = insertAll(first, subans);
    return concat(subans, subans2);
  }

  public static <T> List<List<T>> insertAll(T first, List<List<T>> lists) {
    List<List<T>> result = new ArrayList<>();
    for (List<T> l : lists) {
      List<T> copyList = new ArrayList<>();
      copyList.add(first);
      copyList.addAll(l);
      result.add(copyList);
    }
    return result;
  }

  static <T> List<List<T>> concat(List<List<T>> a, List<List<T>> b) {
    List<List<T>> r = new ArrayList<>(a);
    r.addAll(b);
    return r;
  }

}
