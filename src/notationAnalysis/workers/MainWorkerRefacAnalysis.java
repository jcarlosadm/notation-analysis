package notationAnalysis.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.workers.MainWorker;
import gitCommitStatistics.workers.Worker;
import notationAnalysis.util.Report;

public class MainWorkerRefacAnalysis extends MainWorker {

    private static final String REPORT_FOLDER_NAME = "reports";

    private static final String REPORT_NAME = "report";
    private static final String REPORT_EXTENSION = "txt";

    private static final String DISCIPLINED_STRING = "disciplined";

    private static final String UNDISCIPLINED_STRING = "undisciplined";

    /*
     * private static final int DISCIPLINED_INDEX = 1;
     * 
     * private static final int UNDISCIPLINED_INDEX = 0;
     */

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
    protected void makeAnalysisOnCurrentRepo(GitManager gitManager, String repo) {

        if (!this.createReportFolder()) {
            return;
        }

        String reponame = repo.substring(repo.lastIndexOf(File.separator) + 1, repo.lastIndexOf("."));

        String reportPath = PropertiesManager.getPropertie("path") + File.separator + REPORT_FOLDER_NAME
                + File.separator + REPORT_NAME + "_" + reponame + "." + REPORT_EXTENSION;
        Report report = Report.getInstance(reportPath);
        if (report == null) {
            System.out.println("error to create report");
            return;
        }

        List<String> commits = new ArrayList<>();
        List<String> completeCommits = gitManager.getCommitHashList();
        Collections.reverse(completeCommits);

        for (String cCommit : completeCommits) {
            if (this.resultMap.containsKey(cCommit)) {
                commits.add(cCommit);
            }
        }

        int commitIndex = 0;
        if (commits.size() <= 0) {
            this.finishAnalysis(report);
            return;
        }
        
        String commitId = commits.get(commitIndex);
        Hashtable<String, ArrayList<String>> fileHash = null;

        List<String> undisciplinedList = new ArrayList<>();
        Hashtable<String, List<String>> hashNotationsOther = null;

        double toleranceLevel = 0.5;
        try {
            toleranceLevel = Double.parseDouble(PropertiesManager.getPropertie("comparator.tolerance.level"));
        } catch (Exception e) {
            toleranceLevel = 0.5;
        }

        while (commitIndex < (commits.size() - 1)) {
            commitId = commits.get(commitIndex);

            if (!this.resultMap.containsKey(commitId)) {
                ++commitIndex;
                continue;
            }

            fileHash = this.resultMap.get(commitId);
            for (String file : fileHash.keySet()) {

                BufferedReader bReader = this.createBufferedReader(file, commitId);
                if (bReader == null) {
                    continue;
                }

                // use it?
                // List<String> head = fileHash.get(file);
                List<String> body = new ArrayList<>();
                String line = "";
                try {
                    while ((line = bReader.readLine()) != null) {
                        body.add(line);
                    }

                    bReader.close();
                } catch (Exception e) {
                    System.out.println("error to read file");
                    continue;
                }

                undisciplinedList.clear();
                this.addUndisciplinedList(undisciplinedList, body);

                // TODO use it?
                /*
                 * if (!this.checkUndNotationTotal(head, undisciplinedList)) {
                 * System.out.println("undisciplined total don\'t match");
                 * continue; }
                 */

                String commitIdOther = commits.get(commitIndex + 1);
                if (!this.resultMap.containsKey(commitIdOther)
                        || !this.resultMap.get(commitIdOther).containsKey(file)) {
                    continue;
                }

                BufferedReader bReader2 = this.createBufferedReader(file, commitIdOther);
                if (bReader2 == null) {
                    continue;
                }

                // TODO use it?
                // List<String> headOther =
                // this.resultMap.get(commitIdOther).get(file);
                List<String> bodyOther = new ArrayList<>();
                line = "";
                try {
                    while ((line = bReader2.readLine()) != null) {
                        bodyOther.add(line);
                    }

                    bReader.close();
                } catch (Exception e) {
                    System.out.println("error to read file");
                    continue;
                }

                hashNotationsOther = new Hashtable<>();
                this.fillDisciplined(hashNotationsOther, bodyOther);
                this.fillUndisciplined(hashNotationsOther, bodyOther);

                // TODO use it?
                /*
                 * if (!this.checkNotationNumbers(headOther,
                 * hashNotationsOther)) { System.out.
                 * println("disciplined and undisciplined total don\'t match");
                 * continue; }
                 */

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

                        if (this.compare(undNotation, dNotationOther) >= toleranceLevel) {
                            try {
                                report.write(commitId + " " + commitIdOther + " " + System.lineSeparator());
                                report.write("******************undisciplined notation******************"
                                        + System.lineSeparator());
                                report.write(undNotation);
                                report.writeNewline();
                                report.write("******************disciplined notation******************"
                                        + System.lineSeparator());
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

        this.finishAnalysis(report);

    }

    private void finishAnalysis(Report report) {
        if (!report.close())
            System.out.println("error to close report");

        boolean recreateBackupFolder;
        try {
            recreateBackupFolder = Boolean.parseBoolean(PropertiesManager.getPropertie("recreate.backup.folder"));
        } catch (Exception e) {
            recreateBackupFolder = true;
        }
        
        if (recreateBackupFolder)
            this.deleteAndCreateBackupFolder();

        System.out.println("finished analysis");
    }

    private boolean createReportFolder() {
        String resultPath = PropertiesManager.getPropertie("path");
        File resultFolder = new File(resultPath);
        if (!resultFolder.exists() || !resultFolder.isDirectory()) {
            System.out.println("result folder don\'t exists");
            return false;
        }

        String reportFolderPath = resultPath + File.separator + REPORT_FOLDER_NAME;
        File reportFolder = new File(reportFolderPath);
        if (!reportFolder.exists() || !reportFolder.isDirectory()) {
            if (!reportFolder.mkdir()) {
                System.out.println("fail to create report folder");
                return false;
            }
        }

        return true;
    }

    private void deleteAndCreateBackupFolder() {
        String backupPath = PropertiesManager.getPropertie("path") + File.separator + "backup";
        File backupFolder = new File(backupPath);
        if (backupFolder.exists() && backupFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(backupFolder);
            } catch (Exception e) {
                System.out.println("error to delete backup folder");
            }
        }
        backupFolder.mkdir();
    }

    private BufferedReader createBufferedReader(String file, String commit) {
        String resultFolderPath = PropertiesManager.getPropertie("path") + File.separator + "backup" + File.separator
                + commit;
        File backupFolder = new File(resultFolderPath);
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            System.out.println("backup folder not exists");
            return null;
        }

        String filename = file.substring(0, file.lastIndexOf("."));
        String filepath = resultFolderPath + File.separator + filename;
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(new File(filepath)));
        } catch (FileNotFoundException e1) {
            System.out.println("error to read file");
            return null;
        }

        return bReader;
    }

    // TODO use it?
    /*
     * private boolean checkUndNotationTotal(List<String> headValues,
     * List<String> undisciplinedList) { int totalUndDisciplined =
     * this.getTotalUndisciplined(headValues);
     * 
     * return (totalUndDisciplined == undisciplinedList.size()); }
     */

    private void addUndisciplinedList(List<String> undisciplinedList, List<String> body) {
        undisciplinedList.clear();
        undisciplinedList.addAll(this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    /**
     * Get similarity between two strings. Algorithm found in
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in
     * -java
     * 
     * @param str1
     *            String one
     * @param str2
     *            String two
     * @return similarity level between 0 and 1, inclusive
     */
    private double compare(String str1, String str2) {
        String longer = str1, shorter = str2;
        if (str1.length() < str2.length()) { // longer should always have
                                             // greater length
            longer = str2;
            shorter = str1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
            /* both strings are zero length */ }
        /*
         * // If you have StringUtils, you can use it to calculate the edit
         * distance: return (longerLength -
         * StringUtils.getLevenshteinDistance(longer, shorter)) / (double)
         * longerLength;
         */
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    // TODO use it?
    /*
     * private boolean checkNotationNumbers(List<String> headValues,
     * Hashtable<String, List<String>> hashNotations) { int totalDisciplined =
     * this.getTotalDisciplined(headValues); int totalUndisciplined =
     * this.getTotalUndisciplined(headValues);
     * 
     * boolean matchUnd = (totalUndisciplined ==
     * hashNotations.get(UNDISCIPLINED_STRING).size()); boolean matchD =
     * (totalDisciplined == hashNotations.get(DISCIPLINED_STRING).size());
     * 
     * return (matchD && matchUnd); }
     * 
     * /*private int getValueFromHead(List<String> head, int index) { int value
     * = 0; try { value = Integer.parseInt(head.get(index)); } catch (Exception
     * e) { value = 0; }
     * 
     * return value; }
     * 
     * private int getTotalUndisciplined(List<String> head) { return
     * this.getValueFromHead(head, UNDISCIPLINED_INDEX); }
     * 
     * private int getTotalDisciplined(List<String> head) { return
     * this.getValueFromHead(head, DISCIPLINED_INDEX); }
     */

    private List<String> getNotationList(List<String> body, String notationType) {
        List<String> list = new ArrayList<>();

        boolean undisciplined = false;
        boolean disciplined = false;
        if (notationType.equals(UNDISCIPLINED_STRING)) {
            undisciplined = true;
        } else {
            disciplined = true;
        }

        List<String> lines = body;
        boolean found = false;
        String auxString = "";
        for (String line : lines) {
            if (found && line.trim().startsWith(NOTATION_SEPARATOR)) {
                found = false;
                if (auxString != null && !auxString.isEmpty()) {
                    list.add(auxString);
                }
                auxString = "";
            }

            if (!found && line.trim().startsWith(NOTATION_SEPARATOR)
                    && ((undisciplined == true && line.toLowerCase().contains(UNDISCIPLINED_MARK))
                            || (disciplined == true && !line.toLowerCase().contains(UNDISCIPLINED_MARK)))) {
                found = true;
            } else if (found == true) {
                auxString += line + System.lineSeparator();
            }
        }
        if (found && auxString != null && !auxString.isEmpty()) {
            list.add(auxString);
        }

        return list;
    }

    private void fillUndisciplined(Hashtable<String, List<String>> hashNotations, List<String> body) {
        hashNotations.put(UNDISCIPLINED_STRING, this.getNotationList(body, UNDISCIPLINED_STRING));
    }

    private void fillDisciplined(Hashtable<String, List<String>> hashNotations, List<String> body) {
        hashNotations.put(DISCIPLINED_STRING, this.getNotationList(body, DISCIPLINED_STRING));
    }
}
