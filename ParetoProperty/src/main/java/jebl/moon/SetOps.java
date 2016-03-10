package jebl.moon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SetOps {
	public static <T> Set<T> Union(Set<T> A, Set<T> B) {
		Set<T> rtn = null;
		if (A != null) {
			rtn = new HashSet<T>(A); 
		}
		else {
			rtn = new HashSet<T>();
		}
		if (B != null) {
			rtn.addAll(B);
		}
		return rtn;
	}

	public static <T> Set<T> Cross(Set<T> A, Set<T> B) {
		if (A == null || B == null) {
			return new HashSet<T>();
		}		
		else {
			Set<T> rtn = new HashSet<T>(A);
			rtn.retainAll(B);
			return rtn;
		}		
	}

	public static <T> Set<T> Diff(Set<T> A, Set<T> B) {
		if (A == null) {
			return new HashSet<T>();
		}
		Set<T> rtn = new HashSet<T>(A);
		if (B != null) {
			rtn.removeAll(B);
		}
		return rtn;
	}
	
    private static <T> Set<T> copyWithout(Set<T> s, T e) {
        Set<T> result = new HashSet<T>(s);
        result.remove(e);
        return result;
    }

    private static <T> Set<T> copyWith(Set<T> s, T e) {
        Set<T> result = new HashSet<T>(s);
        result.add(e);
        return result;
    }

    public static <T> Set<Set<T>> powerset(Set<T> s) {
        Set<Set<T>> result = new HashSet<Set<T>>();
        if(s.isEmpty()) {
            Set<T> empty = Collections.emptySet();
            result.add(empty);
        } else {
            for (T e : s) {
                Set<T> t = copyWithout(s, e);
                Set<Set<T>> ps = powerset(t);
                result.addAll(ps);
                for (Set<T> ts : ps) {
                    result.add(copyWith(ts, e));
                }
            }
        }
        return result;
    }
}
