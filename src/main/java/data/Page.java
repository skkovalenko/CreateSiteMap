package data;

import java.util.HashSet;
import java.util.Set;

public class Page {
    private String url;
    private int depth;
    private Set<Page> childrenSet;

    public Page(String url){
        this.url = url;
    }
    public Page(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }
    public void createChildrenSet(Set<String> urlChildren){
        childrenSet = new HashSet<>();
        for (String urlChild : urlChildren) {
            childrenSet.add(new Page(urlChild));
        }
    }
    public String getUrl() {
        return url;
    }
    public int getDepth() {
        return depth;
    }
    public Set<Page> getChildrenSet() {
        return childrenSet;
    }
}
