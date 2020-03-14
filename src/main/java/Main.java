import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;

public class Main {

        private static String pathLenta = "https://lenta.ru";
        private static String pathSkillBox = "https://skillbox.ru/";
        private static String pathSiteMap = "data/siteMap.txt";

        public static void main(String[] args) {
                Path path = Paths.get(pathSiteMap);
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                Task.setPath(path);
                forkJoinPool.invoke(new Task(pathSkillBox));

        }
}
