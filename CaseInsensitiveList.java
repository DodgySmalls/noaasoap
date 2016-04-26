package NOAAsoap;
import java.util.List;
import java.util.ArrayList;

/**

    This is not a complete implementation, only those methods which were used were included

**/
public class CaseInsensitiveList extends ArrayList<String> {

    public CaseInsensitiveList(List<String> list) {
        for(String s : list) {
            this.add(s);
        }
    }

    @Override
    public boolean contains(Object obj) {
        String str = (String)obj;
        for (String s : this) {
            if (str.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(Object obj) {
        if(obj == null) {
            for(int i=0; i < this.size(); i++) {
                if(this.get(i) == null || this.get(i).equals("")) {
                    remove(i);
                    return true;
                }
            }
        } else {
            String str = (String)obj;
            for(int i=0; i < this.size(); i++) {
                if(this.get(i).equalsIgnoreCase(str)) {
                    remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int indexOf(Object obj) {
        if(!this.contains(obj)) {
            return -1;
        } else {
            String str = (String)obj;
            for(int i=0; i < this.size(); i++) {
                if(this.get(i).equalsIgnoreCase(str)) {
                    return i;
                }
            }
        }
        return -1;
    }

}//CaseInsensitiveList