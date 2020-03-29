
import data.Page;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Task extends RecursiveAction {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker EXCEPTION_M = MarkerManager.getMarker("EXCEPTIONS");
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String REG_FOR_URL = "/.+/$";

    private static Set<Page> pagesSet = new CopyOnWriteArraySet<>();
    private static Set<String> bufferUrl = new CopyOnWriteArraySet<>();
    private static String domain;

    private int depthCount;
    private String startUrl;
    private Elements elements;

    public Task(String startUrl) {
        domain = splitAddressSite(startUrl);
        this.startUrl = startUrl;
    }
    public Task(String startUrl, int depthCount) {
        this.depthCount = depthCount;
        this.startUrl = startUrl;
    }

    public static Set<Page> getPagesSet() {
        return pagesSet;
    }

    @Override
    protected void compute() {
        Page pageParent = new Page(startUrl, depthCount);
        pagesSet.add(pageParent);
        bufferUrl.add(startUrl);
        if(connectAndGetElements()) return;
        Set<String> childrenPageUrl = parseElements();
        elements = null;
        if(childrenPageUrl.isEmpty()) return;
        childrenPageUrl.removeIf(s -> bufferUrl.contains(s));
        pageParent.createChildrenSet(childrenPageUrl);
        if(childrenPageUrl.isEmpty()) return;
        bufferUrl.addAll(childrenPageUrl);
        Set<Task> tasks = new HashSet<>();
        depthCount++;
        for(String urlBuff : childrenPageUrl){
            Task task = new Task(urlBuff, depthCount);
            tasks.add(task);
        }
        tasks.forEach(ForkJoinTask::fork);
        tasks.forEach(ForkJoinTask::join);
    }
    private boolean connectAndGetElements(){
        try {
            Thread.sleep(300);
            elements = Jsoup.connect(startUrl).userAgent(USER_AGENT).maxBodySize(0).get().select("a");
        }catch (HttpStatusException e){
            LOGGER.warn(EXCEPTION_M, e.getUrl());
            return true;
        }
        catch (IOException | InterruptedException e) {
            connectAndGetElements();
            LOGGER.warn(EXCEPTION_M, e.getMessage());
            return false;
        }
        return false;
    }
    private Set<String> parseElements(){
        return elements.stream()
                .map(element -> element.attr("href"))
                .filter(s -> s.matches(domain + REG_FOR_URL) || s.matches("^" + REG_FOR_URL))
                .map(s -> {
                    if(!s.matches("^" + domain + ".+$")){
                        return domain + s;
                    }else return s;
                }).collect(Collectors.toSet());
    }
    private String splitAddressSite(String startURL){
        int countChar = 0;
        int endIndex = 0;
        for(int i = 0; i < startURL.length(); i++){
            if(startURL.charAt(i) == '/'){
                countChar++;
            }
            if(countChar == 3){
                endIndex = i;
                break;
            }
        }
        if(countChar == 3){
            return startURL.substring(0, endIndex);
        }
        return startURL;
    }
}
