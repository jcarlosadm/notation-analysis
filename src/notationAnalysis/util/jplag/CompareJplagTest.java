package notationAnalysis.util.jplag;

import org.junit.Test;

public class CompareJplagTest {

	@Test
	public void testCompareToJplag() {
		CompareJplag jplag = CompareJplag.getInstance();
		double value = jplag.compareTo("#include<stdio.h>\n void main() \n { int i = 10;}",
				"#include<stdio.h>\n void main() \n { if (k == 2) { ++k; }}");
		System.out.println(value);

	}

}
