package notationAnalysis;

import java.io.File;

import org.apache.commons.io.FileUtils;

import gitCommitStatistics.properties.PropertiesManager;
import notationAnalysis.workers.MainWorkerRefacAnalysis;

public class Main {

    private static final boolean DELETE_RESULT_FOLDER = true;

    public static void main(String[] args) {
        if (DELETE_RESULT_FOLDER) {
            File resultFolder = new File(PropertiesManager.getPropertie("path"));
            if (resultFolder.exists() && resultFolder.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(resultFolder);
                } catch (Exception e) {
                }
            }
        }

        MainWorkerRefacAnalysis.getInstance();
    }
}
