package notationAnalysis.workers;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.workers.InternWorker;
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

        String commitPath = resultFolderPath + File.separator + this.hashId;
        File commitFolder = new File(commitPath);
        if (!commitFolder.mkdir()) {
            System.out.println("error to create commit analysis directory");
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
