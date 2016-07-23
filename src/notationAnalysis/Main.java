package notationAnalysis;

import java.io.File;

import org.apache.commons.io.FileUtils;

import gitCommitStatistics.properties.PropertiesManager;
import notationAnalysis.workers.MainWorkerRefacAnalysis;

public class Main {

    public static void main(String[] args) {
        PropertiesManager.setNewPath("./general.properties");

        cleanResultFolder();

        MainWorkerRefacAnalysis.getInstance();
    }

    private static void cleanResultFolder() {
        File resultFolder = new File(PropertiesManager.getPropertie("path"));
        if (!resultFolder.exists() || !resultFolder.isDirectory()) {
            return;
        }

        try {
            deleteFolder(new File(PropertiesManager.getPropertie("path") + File.separator + "results"));
            deleteFolder(new File(PropertiesManager.getPropertie("path") + File.separator + "workers"));
        } catch (Exception e) {
        }
    }

    private static void deleteFolder(File folder) throws Exception {
        if (folder.exists() && folder.isDirectory()) {
            FileUtils.deleteDirectory(folder);
        }
    }
}
