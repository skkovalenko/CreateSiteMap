import data.Child;
import data.Parent;
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

    private static Set<Parent> parentsSet = new CopyOnWriteArraySet<>();

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

    public static Set<Parent> getParentsSet() {
        return parentsSet;
    }

    @Override
    protected void compute() {
        if(connectAndGetElements()) {
            return;
        }
        String domain = splitAddressSite();
        Parent parentUrl = new Parent(startURL, depthCount);
        Set<String> buffer = parseElements(domain);
        elements = null;
        if(buffer.isEmpty()) {
            return;
        }
        bufferCleanDoubleUrl(buffer);
        if(buffer.isEmpty()){
            return;
        }
        synchronized (this){
            parentsSet.add(parentUrl);
        }
        parentUrl.createChildrenSet(buffer);
        Set<Task> tasks = new HashSet<>();
        depthCount++;
        for(Child child : parentUrl.getChildrenSet()){
            Task task = new Task(child.getUrl(), depthCount);
            tasks.add(task);
        }
        tasks.forEach(ForkJoinTask::fork);
        tasks.forEach(ForkJoinTask::join);
    }
    private void bufferCleanDoubleUrl(Set<String> buffer){
        if(!parentsSet.isEmpty()){
            for(Parent parent : parentsSet){
                if(parent.getDepth() < depthCount){
                    buffer.remove(parent.getUrl());
                    if(parent.getChildrenSet() != null){
                        for(Child child : parent.getChildrenSet()){
                            buffer.remove(child.getUrl());
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
            e.printStackTrace();
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
