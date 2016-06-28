package notationAnalysis.workers;

import java.util.ArrayList;

import gitCommitStatistics.workers.InternWorker;

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

        String head = "";
        String body = "";
        
        try {
            head = dmacrosOutput.substring(0, dmacrosOutput.indexOf(System.getProperty("line.separator")));
            body = dmacrosOutput.substring(dmacrosOutput.indexOf(System.getProperty("line.separator")) + 1);
        } catch (Exception e) {
            head = "[0,0]";
            body = "";
        }

        ArrayList<String> result = new ArrayList<String>();
        result.add(head);
        result.add(body);

        return result;
    }

}
