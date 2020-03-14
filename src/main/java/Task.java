import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class Task extends RecursiveAction {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker EXCEPTION_M = MarkerManager.getMarker("EXCEPTIONS");
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String REG_FOR_URL = "/.+/$";
    private static final String TAB = "    ";
    //дабы ссылки не повторялись
    private static  Set<String> bufferPathSet = Collections.synchronizedSet(new HashSet<>());
    private static Path pathSiteMap;
    //глубина сайта
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

    public static void setPath(Path path) {
        Task.pathSiteMap = path;
    }

    @Override
    protected void compute() {
        try {
            //Thread.sleep(300);
            elements = Jsoup.connect(startURL).userAgent(USER_AGENT).maxBodySize(0).get().select("a");
        }catch (HttpStatusException e){
            LOGGER.warn(EXCEPTION_M, e.getUrl(), e);
            return;
        }
        catch (IOException  e) {
            e.printStackTrace();
        }
        //Выпиливаем домен
        String domain = splitAddressSite();
        //Собрать все внутренние ссылки не повтаряющиеся
        Set<String> buffer = elements.stream()
                .map(element -> element.attr("href"))
                .filter(s -> s.matches(domain + REG_FOR_URL) || s.matches("^" + REG_FOR_URL))
                .map(s -> {
                    if(!s.matches("^" + domain + ".+$")){
                        return domain + s;
                    }else return s;
                }).collect(Collectors.toSet());
        //экономим память
        elements.clear();
        elements = null;
        try {
            //Запись ссылки в файл

            Files.writeString(pathSiteMap, TAB.repeat(depthCount) + startURL + "\n", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
            if (buffer.isEmpty()) {
                return;
            }
            //сет из новых внутренних ссылок
            Set<String> newPathSet = buffer.stream().filter(s -> !bufferPathSet.contains(s)).collect(Collectors.toSet());
            //ремувим то, что уже было в буфере ссылок
            buffer.removeIf(s -> bufferPathSet.contains(s));
            if (buffer.isEmpty()) {
                return;
            }
            //добавляем новые ссылки в буфер ссылок
            bufferPathSet.addAll(buffer);
            // Итератор, чтобы можн было удалять элементы сета в цикле
            depthCount++;
            Iterator<String> iterator = buffer.iterator();
            while (iterator.hasNext()) {
                Task task = new Task(iterator.next(), depthCount);
                task.fork();
                task.join();
                iterator.remove();//экономим память
            }
            //удаляем новые ссылки из статического буфера ссылок, чтобы в новом дереве они снова отоброжались
            bufferPathSet.removeAll(newPathSet);

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
