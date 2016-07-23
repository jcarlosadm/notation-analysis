package notationAnalysis.workers;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.workers.InternWorker;
import gitCommitStatistics.workers.MainWorker;
import gitCommitStatistics.workers.Worker;

public class WorkerRefacAnalysis extends Worker {

    private static final String RESULT_ANALYSIS_FOLDERNAME = "resultAnalysis";

    public WorkerRefacAnalysis(String workerId, String hashId, ArrayList<String> filesToAnalise) {
        super(workerId, hashId, filesToAnalise);
    }

    @Override
    protected InternWorker getInternWorkerInstance(int indexFile) {
        return new InternWorkerRefacAnalysis(workerId, filesToAnalise.get(indexFile));
    }

    @Override
    protected void preCommitTask() {
        String resultAnalysisFolderPath = this.path + File.separator + RESULT_ANALYSIS_FOLDERNAME;
        File analysisFolder = new File(resultAnalysisFolderPath);
        if (!analysisFolder.exists() || !analysisFolder.isDirectory()) {
            if (!analysisFolder.mkdir()) {
                System.out.println("error to create analysis folder");
            }
        }
    }

    @Override
    protected void postCommitTask() {
        String resultAnalysisFolderPath = this.path + File.separator + RESULT_ANALYSIS_FOLDERNAME;
        File analysisFolder = new File(resultAnalysisFolderPath);
        if (!analysisFolder.exists() || !analysisFolder.isDirectory()) {
            System.out.println("analysis folder not exists");
            return;
        }

        File[] content = analysisFolder.listFiles();
        if (content.length == 0) {
            return;
        }

        String resultFolderPath = PropertiesManager.getPropertie("path") + File.separator + "backup";
        File backupFolder = new File(resultFolderPath);
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            System.out.println("backup folder not exists");
            return;
        }
        
        boolean reuse;
        try {
            String reuseProperty = PropertiesManager.getPropertie("reuse.dmacros.data");
            reuse = Boolean.parseBoolean(reuseProperty);
        } catch (Exception e) {
            reuse = false;
        }
        
        String resultProjectFolderPath = resultFolderPath + File.separator + MainWorker.getCurrentRepoName();
        File resultProjectFolder = new File(resultProjectFolderPath);
        if (!resultProjectFolder.exists() || !resultProjectFolder.isDirectory()) {
            if (!resultProjectFolder.mkdir()) {
                System.out.println("error to create result project folder");
            }
        } else if (reuse == false) {
            try {
                FileUtils.deleteDirectory(resultProjectFolder);
                if (!resultProjectFolder.mkdir()) {
                    System.out.println("error to create result project folder");
                }
            } catch (Exception e) {
            }
        }

        String commitPath = resultProjectFolderPath + File.separator + this.hashId;
        File commitFolder = new File(commitPath);
        if (!commitFolder.exists() || !commitFolder.isDirectory()) {
            if (!commitFolder.mkdir()) {
                System.out.println("error to create commit analysis directory");
                return;
            }
        } else if (reuse == false) {
            try {
                FileUtils.deleteDirectory(commitFolder);
                if (!commitFolder.mkdir()) {
                    System.out.println("error to create commit analysis directory");
                    return;
                }
            } catch (Exception e) {
            }
        } else {
            return;
        }

        for (File file : content) {
            if (!file.isDirectory()) {
                try {
                    FileUtils.moveFileToDirectory(file, commitFolder, false);
                } catch (Exception e) {
                    System.out.println("error to move file " + file.getName());
                }
            }
        }
    }

    public static String getResultAnalysisFolderName() {
        return RESULT_ANALYSIS_FOLDERNAME;
    }

}
