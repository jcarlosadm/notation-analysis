package notationAnalysis.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class CountersTest {

    @Test
    public void testReset() {
        Counters counters = new Counters();
        counters.addDisciplinedTotal("aaa", 2);
        counters.addDisciplinedTotal("bbb", 4);
        counters.addUndisciplinedTotal("aaa", 5);
        
        if (counters.getDisciplinedTotal() != 0 && counters.getUndisciplinedTotal() != 0) {
            counters.reset();
            assertTrue(counters.getDisciplinedTotal() == 0 && counters.getUndisciplinedTotal() == 0);
        } else {
            fail();
        }
    }

    @Test
    public void testGetUndisciplinedTotal() {
        Counters counters = new Counters();
        counters.addUndisciplinedTotal("aaa", 2);
        counters.addUndisciplinedTotal("aaa", 4);
        counters.addUndisciplinedTotal("bbb", 5);
        
        assertTrue(counters.getUndisciplinedTotal() == 11);
    }

    @Test
    public void testGetDisciplinedTotal() {
        Counters counters = new Counters();
        counters.addDisciplinedTotal("aaa", 2);
        counters.addDisciplinedTotal("aaa", 4);
        counters.addDisciplinedTotal("bbb", 5);
        
        assertTrue(counters.getDisciplinedTotal() == 11);
    }

    @Test
    public void testGetUndisciplinedTotalString() {
        Counters counters = new Counters();
        counters.addUndisciplinedTotal("aaa", 2);
        counters.addUndisciplinedTotal("aaa", 4);
        counters.addUndisciplinedTotal("bbb", 5);
        
        assertTrue(counters.getUndisciplinedTotal("aaa") == 6);
    }

    @Test
    public void testGetDisciplinedTotalString() {
        Counters counters = new Counters();
        counters.addDisciplinedTotal("aaa", 2);
        counters.addDisciplinedTotal("aaa", 4);
        counters.addDisciplinedTotal("bbb", 5);
        
        assertTrue(counters.getDisciplinedTotal("aaa") == 6);
    }

    @Test
    public void testGetCommits() {
        Counters counters = new Counters();
        counters.addUndisciplinedTotal("aaa", 2);
        counters.addUndisciplinedTotal("aaa", 4);
        counters.addUndisciplinedTotal("bbb", 5);
        
        assertTrue(counters.getCommits().size() == 2);
    }

}
