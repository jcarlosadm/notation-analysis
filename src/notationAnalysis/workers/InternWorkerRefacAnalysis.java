package notationAnalysis.workers;

import java.io.File;
import java.util.ArrayList;

import gitCommitStatistics.workers.InternWorker;
import gitCommitStatistics.workers.ProccessManager;

public class InternWorkerRefacAnalysis extends InternWorker {

    public InternWorkerRefacAnalysis(String workerId, String filePath) {
        super(workerId, filePath);
    }

    /**
     * {@inheritDoc} Transform output of dmacros algorithm in an ArrayList like
     * this: [head], [body]. [head] is the total of undisciplined and
     * disciplined annotation. [body] are the code snippets.
     * 
     * @param dmacrosOutput
     *            dmacros output
     * @return ArrayList of strings
     * @see InternWorker#transformDmacrosOutput(String)
     */
    @Override
    protected ArrayList<String> transformDmacrosOutput(String dmacrosOutput) {
        
        /*String resultFolderPath = this.path + File.separator
                + WorkerRefacAnalysis.getResultAnalysisFolderName();
        File resultFolder = new File(resultFolderPath);
        if (!resultFolder.exists() || !resultFolder.isDirectory()) {
            System.out.println("result folder not exists");
            return null;
        }

        String filename = this.filePathAux.substring(
                this.filePathAux.lastIndexOf(File.separator) + 1,
                this.filePathAux.lastIndexOf("."));
        String filePath = resultFolderPath + File.separator + filename;

        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter(new File(filePath)));
        } catch (Exception e) {
            System.out.println("error to create file with results of file " + filename);
            return null;
        }
        
        try {
            bWriter.write(dmacrosOutput);
            bWriter.close();
        } catch (Exception e) {
            System.out.println("error to write body");
        }*/

        ArrayList<String> result = new ArrayList<String>();
        
        result.add("0");
        result.add("0");
        result.add("0");

        return result;
    }
    
    @Override
    protected boolean executeProcess(String command, ProccessManager proccessManager) {
        
        String resultFolderPath = this.path + File.separator
                + WorkerRefacAnalysis.getResultAnalysisFolderName();
        File resultFolder = new File(resultFolderPath);
        if (!resultFolder.exists() || !resultFolder.isDirectory()) {
            System.out.println("result folder not exists");
            return false;
        }

        String filename = this.filePathAux.substring(
                this.filePathAux.lastIndexOf(File.separator) + 1,
                this.filePathAux.lastIndexOf("."));
        String filePath = resultFolderPath + File.separator + filename;
        
        proccessManager.execToFile(command, filePath);
        
        if (proccessManager.hasError()) {
            return false;
        }
        
        return true;
        
    }

}
