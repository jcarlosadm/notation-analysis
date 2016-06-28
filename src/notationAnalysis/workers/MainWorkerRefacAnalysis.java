package notationAnalysis.workers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.workers.MainWorker;
import gitCommitStatistics.workers.Worker;
import notationAnalysis.util.Report;

public class MainWorkerRefacAnalysis extends MainWorker {

    private static final String REPORT_PATH = "report.txt";

    private static final double TOLERANCE_LEVEL = 0.5;

    private static final String DISCIPLINED_STRING = "disciplined";

    private static final String UNDISCIPLINED_STRING = "undisciplined";

    private static final int BODY_INDEX = 1;

    private static final int HEAD_INDEX = 0;

    private static final int DISCIPLINED_INDEX = 1;

    private static final int UNDISCIPLINED_INDEX = 0;

    private static final String NOTATION_SEPARATOR = "@";

    private static final String UNDISCIPLINED_MARK = "u";

    private static MainWorkerRefacAnalysis instance = null;

    public static MainWorkerRefacAnalysis getInstance() {
        if (instance == null) {
            instance = new MainWorkerRefacAnalysis();
        }

        return instance;
    }

    @Override
    protected Worker getWorkerInstance(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap,
            int commitIndex) {
        return new WorkerRefacAnalysis("Worker-" + commitIndex % numberOfWorkers, commits.get(commitIndex),
                changeMap.get(commits.get(commitIndex)));
    }

    @Override
    protected boolean writeResults(String repo) {
        return true;
    }

    @Override
    public void createBackup() {
    }

    @Override
    protected void makeAnalysisOnCurrentRepo(GitManager gitManager) {

        Report report = Report.getInstance(REPORT_PATH);
        if (report == null) {
            System.out.println("error to create report");
            return;
        }

        List<String> commits = gitManager.getCommitHashList();
        Collections.reverse(commits);

        int commitIndex = 0;
        String commitId = commits.get(commitIndex);
        Hashtable<String, ArrayList<String>> fileHash = null;

        List<String> undisciplinedList = new ArrayList<>();
        Hashtable<String, List<String>> hashNotationsOther = null;

        while (commitIndex < (commits.size() - 1)) {
            commitId = commits.get(commitIndex);

            if (!this.resultMap.containsKey(commitId)) {
                ++commitIndex;
                continue;
            }

            fileHash = this.resultMap.get(commitId);
            for (String file : fileHash.keySet()) {

                String head = fileHash.get(file).get(HEAD_INDEX);
                String body = fileHash.get(file).get(BODY_INDEX);

                undisciplinedList.clear();
                this.addUndisciplinedList(undisciplinedList, body);

                if (!this.checkUndNotationTotal(head, undisciplinedList)) {
                    System.out.println("disciplined and undisciplined total don\'t match");
                    continue;
                }

                String commitIdOther = commits.get(commitIndex + 1);
                if (!this.resultMap.containsKey(commitIdOther)
                        || !this.resultMap.get(commitIdOther).containsKey(file)) {
                    continue;
                }

                String headOther = this.resultMap.get(commitIdOther).get(file).get(HEAD_INDEX);
                String bodyOther = this.resultMap.get(commitIdOther).get(file).get(BODY_INDEX);

                hashNotationsOther = new Hashtable<>();
                this.fillDisciplined(hashNotationsOther, bodyOther);
                this.fillUndisciplined(hashNotationsOther, bodyOther);

                if (!this.checkNotationNumbers(headOther, hashNotationsOther)) {
                    System.out.println("disciplined and undisciplined total don\'t match");
                    continue;
                }

                boolean found = false;
                String auxString = "";
                for (String undNotation : undisciplinedList) {
                    if (found)
                        found = false;

                    for (String undNotationOther : hashNotationsOther.get(UNDISCIPLINED_STRING)) {
                        if (undNotation.equals(undNotationOther)) {
                            found = true;
                            auxString = undNotationOther;
                            break;
                        }
                    }

                    if (found) {
                        hashNotationsOther.get(UNDISCIPLINED_STRING).remove(auxString);
                        continue;
                    }

                    for (String dNotationOther : hashNotationsOther.get(DISCIPLINED_STRING)) {
                        if (this.compare(undNotation, dNotationOther) >= TOLERANCE_LEVEL) {
                            // TODO write report
                            try {
                                report.write(commitId + " "+commitIdOther+" ");
                                report.writeNewline();
                                report.write(undNotation);
                                report.writeNewline();
                                report.write(dNotationOther);
                                report.writeNewline();
                            } catch (IOException e) {
                                System.out.println("error to write report");
                            }
                            
                            auxString = dNotationOther;
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        hashNotationsOther.get(DISCIPLINED_STRING).remove(auxString);
                        continue;
                    }
                }
            }

            ++commitIndex;
        }

        if (!report.close())
            System.out.println("error to close report");

        System.out.println("finished analysis");

    }

    private boolean checkUndNotationTotal(String headValues, List<String> undisciplinedList) {
        int totalDisciplined = this.getTotalDisciplined(headValues);

        return (totalDisciplined == undisciplinedList.size());
    }

    private void addUndisciplinedList(List<String> undisciplinedList, String body) {
        undisciplinedList.clear();
        undisciplinedList.addAll(this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    private double compare(String str1, String str2) {
        // TODO implement
        return 50.0;
    }

    private boolean checkNotationNumbers(String headValues, Hashtable<String, List<String>> hashNotations) {
        int totalDisciplined = this.getTotalDisciplined(headValues);
        int totalUndisciplined = this.getTotalUndisciplined(headValues);

        boolean matchUnd = (totalUndisciplined == hashNotations.get(UNDISCIPLINED_STRING).size());
        boolean matchD = (totalDisciplined == hashNotations.get(DISCIPLINED_STRING).size());

        return (matchD && matchUnd);
    }

    private int getValueFromHead(String head, int index) {
        String[] array = head.split(",");
        array[0] = array[0].substring(1);
        array[1] = array[1].substring(0, array[1].length() - 1);

        int value = 0;
        try {
            value = Integer.parseInt(array[index]);
        } catch (Exception e) {
            value = 0;
        }

        return value;
    }

    private int getTotalUndisciplined(String head) {
        return this.getValueFromHead(head, UNDISCIPLINED_INDEX);
    }

    private int getTotalDisciplined(String head) {
        return this.getValueFromHead(head, DISCIPLINED_INDEX);
    }

    private List<String> getNotationList(String body, String notationType) {
        List<String> list = new ArrayList<>();

        boolean undisciplined = false;
        boolean disciplined = false;
        if (notationType.equals(UNDISCIPLINED_STRING)) {
            undisciplined = true;
        } else {
            disciplined = true;
        }

        String[] lines = body.split(System.getProperty("line.separator"));
        boolean found = false;
        String auxString = "";
        for (String line : lines) {
            if (found && line.trim().startsWith(NOTATION_SEPARATOR)) {
                found = false;
                if (auxString != null && !auxString.isEmpty()
                        && !auxString.equals(NOTATION_SEPARATOR + System.getProperty("line.separator"))) {
                    list.add(auxString);
                }
                auxString = "";
            }

            if (!found && line.trim().startsWith(NOTATION_SEPARATOR)
                    && ((undisciplined == true && line.toLowerCase().contains(UNDISCIPLINED_MARK))
                            || (disciplined == true && !line.toLowerCase().contains(UNDISCIPLINED_MARK)))) {
                found = true;
                auxString += NOTATION_SEPARATOR + System.getProperty("line.separator");
            } else if (found == true) {
                auxString += line + System.getProperty("line.separator");
            }
        }
        if (found && auxString != null && !auxString.isEmpty()
                && !auxString.equals(NOTATION_SEPARATOR + System.getProperty("line.separator"))) {
            list.add(auxString);
        }

        return list;
    }

    private void fillUndisciplined(Hashtable<String, List<String>> hashNotations, String body) {
        hashNotations.put(UNDISCIPLINED_STRING, this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    private void fillDisciplined(Hashtable<String, List<String>> hashNotations, String body) {
        hashNotations.put(DISCIPLINED_STRING, this.getNotationList(body, DISCIPLINED_STRING));
    }
}
