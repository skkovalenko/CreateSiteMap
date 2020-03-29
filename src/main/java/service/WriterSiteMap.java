package service;
import data.Page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class WriterSiteMap {
    private static final String TAB = "    ";
    private Map<Integer, Set<Page>> depthPageMap = new HashMap<>();
    private Path path;

    public WriterSiteMap(Set<Page> pageSet, Path path) {
        this.path = path;
        for(Page page : pageSet){
            if(!depthPageMap.containsKey(page.getDepth())){
                depthPageMap.put(page.getDepth(), new HashSet<>());
            }
            depthPageMap.get(page.getDepth()).add(page);
        }
    }

    public void recursiveWrite(String url, int depth) throws IOException {
        Page page = findParent(url, depth);
        if(page == null){
            return;
        }
        Files.writeString(path, TAB.repeat(depth) + page.getUrl() + "\n", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        if(page.getChildrenSet() == null || page.getChildrenSet().isEmpty()){
            return;
        }
        depth++;
        for(Page childPage : page.getChildrenSet()){
            recursiveWrite(childPage.getUrl(), depth);
        }
    }

    public Page findParent(String url, int depth){
        if(depthPageMap.containsKey(depth)){
            for(Page page : depthPageMap.get(depth)){
                if(page.getUrl().equals(url)){
                    return page;
                }
            }
        }
        return null;
    }
}
