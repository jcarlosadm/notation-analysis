package notationAnalysis;

import java.io.File;

import org.apache.commons.io.FileUtils;

import gitCommitStatistics.properties.PropertiesManager;
import notationAnalysis.workers.MainWorkerRefacAnalysis;

public class Main {

    public static void main(String[] args) {
        PropertiesManager.setNewPath("./general.properties");
        
        deleteResultFolder();

        MainWorkerRefacAnalysis.getInstance();
    }

    private static void deleteResultFolder() {
        boolean delResultFolder;
        try {
            delResultFolder = Boolean.parseBoolean(PropertiesManager.getPropertie("delete.result.folder.onstart"));
        } catch (Exception e) {
            delResultFolder = true;
        }
        
        if (delResultFolder) {
            File resultFolder = new File(PropertiesManager.getPropertie("path"));
            if (resultFolder.exists() && resultFolder.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(resultFolder);
                } catch (Exception e) {
                }
            }
        }
    }
}
