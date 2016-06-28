package notationAnalysis.workers;

import java.util.ArrayList;

import gitCommitStatistics.workers.InternWorker;
import gitCommitStatistics.workers.Worker;

public class WorkerRefacAnalysis extends Worker{

    public WorkerRefacAnalysis(String workerId, String hashId, ArrayList<String> filesToAnalise) {
        super(workerId, hashId, filesToAnalise);
    }
    
    @Override
    protected InternWorker getInternWorkerInstance(int indexFile) {
        return new InternWorkerRefacAnalysis(workerId, filesToAnalise.get(indexFile));
    }

}
