package service;

import data.Child;
import data.Parent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class WriterSiteMap {

    private static final String TAB = "    ";

    private Map<Integer, Set<Parent>> depthParentsMap = new HashMap<>();
    private int depthMax;
    private Path path;

    public WriterSiteMap(Set<Parent> parentSet, Path path) {
        this.path = path;

        for(Parent parent : parentSet){
            if(!depthParentsMap.containsKey(parent.getDepth())){
                depthParentsMap.put(parent.getDepth(), new HashSet<>());
            }
            depthParentsMap.get(parent.getDepth()).add(parent);
            if(depthMax < parent.getDepth()){
                depthMax = parent.getDepth();
            }
        }
    }

    public void recursiveWrite(String url, int depth) throws IOException {
        Parent parent = findParent(url, depth);
        if(parent == null){
            return;
        }
        Files.writeString(path, TAB.repeat(depth) + parent.getUrl() + "\n", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        if(parent.getChildrenSet() == null || parent.getChildrenSet().isEmpty()){
            return;
        }
        depth++;
        for(Child child : parent.getChildrenSet()){
            recursiveWrite(child.getUrl(), depth);
        }
    }

    public Parent findParent(String url, int depth){
        if(depthParentsMap.containsKey(depth)){
            for(Parent parent : depthParentsMap.get(depth)){
                if(parent.getUrl().equals(url)){
                    return parent;
                }
            }
        }
        return null;
    }

}
