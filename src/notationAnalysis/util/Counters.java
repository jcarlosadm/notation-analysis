package notationAnalysis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Counters {
    
    private int undisciplinedTotal = 0;
    private int disciplinedTotal = 0;
    
    private Map<String, Integer> undiscByCommit = new HashMap<>();
    private Map<String, Integer> discByCommit = new HashMap<>();
    
    private List<String> commits = new ArrayList<>();
    
    public void reset() {
        this.undisciplinedTotal = 0;
        this.disciplinedTotal = 0;
        
        this.undiscByCommit.clear();
        this.discByCommit.clear();
        this.commits.clear();
    }
    
    public void addUndisciplinedTotal(String commit, int total) {
        this.undisciplinedTotal += total;
        
        int subtotal = total;
        if (this.undiscByCommit.containsKey(commit)) {
            subtotal += this.undiscByCommit.get(commit);
        }
        this.undiscByCommit.put(commit, subtotal);
        
        if (!this.commits.contains(commit)) {
            this.commits.add(commit);
        }
    }
    
    public void addDisciplinedTotal(String commit, int total) {
        this.disciplinedTotal += total;
        int subtotal = total;
        if (this.discByCommit.containsKey(commit)) {
            subtotal += this.discByCommit.get(commit);
        }
        this.discByCommit.put(commit, subtotal);
        
        if (!this.commits.contains(commit)) {
            this.commits.add(commit);
        }
    }
    
    public int getUndisciplinedTotal() {
        return this.undisciplinedTotal;
    }
    
    public int getDisciplinedTotal() {
        return this.disciplinedTotal;
    }
    
    public int getUndisciplinedTotal(String commit) {
        return this.undiscByCommit.get(commit);
    }
    
    public int getDisciplinedTotal(String commit) {
        return this.discByCommit.get(commit);
    }
    
    public List<String> getCommits() {
        return this.commits;
    }
}
