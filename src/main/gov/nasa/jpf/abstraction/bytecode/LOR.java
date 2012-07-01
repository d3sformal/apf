package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.numeric.Abstraction;
import gov.nasa.jpf.abstraction.numeric.FocusAbstractChoiceGenerator;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;

public class LOR extends gov.nasa.jpf.jvm.bytecode.LOR {

	@Override
	public Instruction execute(SystemState ss, KernelState ks, ThreadInfo th) {
		StackFrame sf = th.getTopFrame();
		Abstraction abs_v1 = (Abstraction) sf.getOperandAttr(1);
		Abstraction abs_v2 = (Abstraction) sf.getOperandAttr(3);

		if (abs_v1 == null && abs_v2 == null)
			return super.execute(ss, ks, th);
		else {
			long v1 = th.longPeek(0);
			long v2 = th.longPeek(2);

			Abstraction result = Abstraction._or(v1, abs_v1, v2, abs_v2);
			System.out.printf("Values: %d (%s), %d (%s)\n", v1, abs_v1, v2, abs_v2);

			if (result.isTop()) {
				ChoiceGenerator<?> cg;
				if (!th.isFirstStepInsn()) { // first time around
					int size = result.get_num_tokens();
					cg = new FocusAbstractChoiceGenerator(size);
					ss.setNextChoiceGenerator(cg);
					return this;
				} else { // this is what really returns results
					cg = ss.getChoiceGenerator();
					assert (cg instanceof FocusAbstractChoiceGenerator);
					int key = (Integer) cg.getNextChoice();
					result = result.get_token(key);
					System.out.printf("Result: %s\n", result);
				}
			} else
				System.out.printf("Result: %s\n", result);

			th.longPop();
			th.longPop();
			
			th.longPush(0);
			sf = th.getTopFrame();
			sf.setLongOperandAttr(result);

			return getNext(th);
		}
	}

}
