
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

    private int depthCount;
    private String startURL;
    private Elements elements;

    public Task(String startURL) {
        this.startURL = startURL;
    }
    public Task(String startURL, int depthCount) {
        this.depthCount = depthCount;
        this.startURL = startURL;
    }

    public static Set<Page> getPagesSet() {
        return pagesSet;
    }

    @Override
    protected void compute() {
        if(connectAndGetElements()) {
            return;
        }
        String domain = splitAddressSite();
        Page pageUrl = new Page(startURL, depthCount);
        pagesSet.add(pageUrl);
        Set<String> buffer = parseElements(domain);
        elements = null;
        if(buffer.isEmpty()) {
            return;
        }
        bufferCleanDoubleUrl(buffer);
        if(buffer.isEmpty()){
            return;
        }
        pageUrl.createChildrenSet(buffer);
        Set<Task> tasks = new HashSet<>();
        depthCount++;
        for(Page childPage : pageUrl.getChildrenSet()){
            Task task = new Task(childPage.getUrl(), depthCount);
            tasks.add(task);
        }
        tasks.forEach(ForkJoinTask::fork);
        tasks.forEach(ForkJoinTask::join);
    }

    private void bufferCleanDoubleUrl(Set<String> buffer){
        if(!pagesSet.isEmpty()){
            for(Page page : pagesSet){
                if(page.getDepth() < depthCount){
                    buffer.remove(page.getUrl());
                    if(page.getChildrenSet() != null){
                        for(Page childPage : page.getChildrenSet()){
                            buffer.remove(childPage.getUrl());
                        }
                    }
                }
            }
        }
    }
    private boolean connectAndGetElements(){
        try {
            Thread.sleep(300);
            elements = Jsoup.connect(startURL).userAgent(USER_AGENT).maxBodySize(0).get().select("a");
        }catch (HttpStatusException e){
            LOGGER.warn(EXCEPTION_M, e.getUrl(), e);
            return true;
        }
        catch (IOException | InterruptedException e) {
            LOGGER.warn(EXCEPTION_M, e.getMessage(), e);
            return true;
        }
        return false;
    }
    private Set<String> parseElements(String domain){
        return elements.stream()
                .map(element -> element.attr("href"))
                .filter(s -> s.matches(domain + REG_FOR_URL) || s.matches("^" + REG_FOR_URL))
                .map(s -> {
                    if(!s.matches("^" + domain + ".+$")){
                        return domain + s;
                    }else return s;
                }).collect(Collectors.toSet());
    }
    private String splitAddressSite(){
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
