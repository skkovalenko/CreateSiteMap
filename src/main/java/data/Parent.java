package data;

import java.util.HashSet;
import java.util.Set;

public class Parent {
    private String url;
    private int depth;
    private Set<Child> childrenSet;

    public Parent(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    public void createChildrenSet(Set<String> urlChildren){
        childrenSet = new HashSet<>();
        for (String s : urlChildren) {
            childrenSet.add(new Child(this, s));
        }
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    public Set<Child> getChildrenSet() {
        return childrenSet;
    }
}
