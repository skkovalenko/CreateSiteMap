import service.WriterSiteMap;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ForkJoinPool;

public class Main {

        private static String pathLenta = "https://lenta.ru";
        private static String pathSkillBox = "https://skillbox.ru/";
        private static String pathSiteMap = "data/siteMap.txt";
        private static final int DEPTH_START = 0;
        public static void main(String[] args) throws IOException {
                Path path = Paths.get(pathSiteMap);
                ForkJoinPool forkJoinPool = new ForkJoinPool(7);
                forkJoinPool.invoke(new Task(pathSkillBox));
                WriterSiteMap writerSiteMap = new WriterSiteMap(Task.getParentsSet(), path);

                writerSiteMap.recursiveWrite(pathSkillBox, DEPTH_START);
                System.out.println(Task.getParentsSet().size());


        }
}