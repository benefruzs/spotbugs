/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.InstructionHandle;

import java.util.*;
import java.util.regex.Pattern;


public class FindUndeclaredCheckedExceptions extends OpcodeStackDetector implements StatelessDetector {
    private final BugAccumulator bugAccumulator;
    private final BugReporter bugReporter;
    private final List<ExceptionThrown> throwList = new LinkedList<>();
    private final List<String> throwsList = new LinkedList<>();
    private final List<ExceptionCaught> catchList = new LinkedList<>();
    private final Pattern space = Pattern.compile(" ");

    private XMethod prevInvoke = null;

    private static class ExceptionCaught {
        public String exceptionClass;

        public int startOffset, endOffset, sourcePC;

        public boolean seen = false;

        public boolean dead = false;

        public ExceptionCaught(String exceptionClass, int startOffset, int endOffset, int sourcePC) {
            this.exceptionClass = exceptionClass;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.sourcePC = sourcePC;
        }
    }

    private static class ExceptionThrown {
        public @DottedClassName String exceptionClassName;
        public Set<String> superClasses;

        public int offset;

        public ExceptionThrown(JavaClass cls, int offset) throws ClassNotFoundException {
            this.exceptionClassName = cls.getClassName();
            this.offset = offset;
            this.superClasses = new HashSet<>();
            for (JavaClass superCls : cls.getSuperClasses()) {
                this.superClasses.add(superCls.getClassName());
            }
        }

        public boolean match(String exception) {
            System.out.println("Exception: " + exception);
            System.out.println("Thrown exception: " + exceptionClassName);
            System.out.println("Exception super Classes: " + superClasses.toString());
            return exception.equals(exceptionClassName) || superClasses.contains(exception);
        }
    }



    public FindUndeclaredCheckedExceptions(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitJavaClass(JavaClass c) {
        super.visitJavaClass(c);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Const.ATHROW:
            if (stack.getStackDepth() > 0) {
                OpcodeStack.Item item = stack.getStackItem(0);
                try {
                    JavaClass cls = item.getJavaClass();
                    if (cls != null) {

                        System.out.println(item.getJavaClass().getClassName() + " added to throw list by " + prevInvoke.getName());
                        if (prevInvoke == null) {
                            throwList.add(new ExceptionThrown(item.getJavaClass(), getPC()));
                        } else {
                            if (!prevInvoke.getClassName().equals("java.lang.Throwable") &&
                                    !prevInvoke.getName().equals("addSuppressed")) {
                                throwList.add(new ExceptionThrown(item.getJavaClass(), getPC()));
                            }
                        }
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
            break;

        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
        case Const.INVOKEINTERFACE:
            String className = getClassConstantOperand();
            if (!className.startsWith("[")) {
                try {
                    XClass c = Global.getAnalysisCache().getClassAnalysis(XClass.class,
                            DescriptorFactory.createClassDescriptor(className));
                    XMethod m = Hierarchy2.findInvocationLeastUpperBound(c, getNameConstantOperand(), getSigConstantOperand(),
                            seen == Const.INVOKESTATIC, false);
                    if (m == null) {
                        break;
                    }
                    prevInvoke = m;
                    String[] exceptions = m.getThrownExceptions();
                    if (exceptions != null) {
                        System.out.println("Method: " + m.getName() + " declared to throw: " + Arrays.toString(exceptions));
                        for (String name : exceptions) {
                            try {
                                JavaClass excCls = Global.getAnalysisCache().getClassAnalysis(JavaClass.class,
                                        DescriptorFactory.createClassDescriptor(name));
                                throwList.add(new ExceptionThrown(excCls, getPC()));
                            } catch (CheckedAnalysisException e) {
                                bugReporter.logError("Error looking up " + name, e);
                            } catch (ClassNotFoundException e) {
                                bugReporter.reportMissingClass(e);
                            }
                        }
                    }
                } catch (MissingClassException e) {
                    bugReporter.reportMissingClass(e.getClassDescriptor());
                } catch (CheckedAnalysisException e) {
                    bugReporter.logError("Error looking up " + className, e);
                }
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void visit(CodeException obj) {
        try {
            super.visit(obj);
            int type = obj.getCatchType();
            if (type == 0) {
                return;
            }
            String name = getConstantPool().constantToString(getConstantPool().getConstant(type));

            ExceptionCaught caughtException = new ExceptionCaught(name, obj.getStartPC(), obj.getEndPC(), obj.getHandlerPC());
            catchList.add(caughtException);

            LiveLocalStoreDataflow dataflow = getClassContext().getLiveLocalStoreDataflow(getMethod());
            CFG cfg = getClassContext().getCFG(getMethod());
            Collection<BasicBlock> blockList = cfg.getBlocksContainingInstructionWithOffset(obj.getHandlerPC());
            for (BasicBlock block : blockList) {
                InstructionHandle first = block.getFirstInstruction();
                if (first != null && first.getPosition() == obj.getHandlerPC() && first.getInstruction() instanceof ASTORE) {
                    ASTORE astore = (ASTORE) first.getInstruction();
                    BitSet liveStoreSet = dataflow.getFactAtLocation(new Location(first, block));
                    if (!liveStoreSet.get(astore.getIndex())) {
                        // The ASTORE storing the exception object is dead
                        caughtException.dead = true;
                        break;
                    }
                }
            }
        } catch (MethodUnprofitableException e) {
            Method m = getMethod();
            bugReporter.reportSkippedAnalysis(DescriptorFactory.instance().getMethodDescriptor(getClassName(), getMethodName(),
                    getMethodSig(), m.isStatic()));
        } catch (DataflowAnalysisException | CFGBuilderException e) {
            bugReporter.logError("Error checking for dead exception store", e);
        }
    }

    @Override
    public void visitAfter(Code obj) {
        for (ExceptionThrown thrownException : throwList) {
            boolean inBothLists = false;
            boolean inThrowsList = false;
            for (ExceptionCaught caughtException : catchList) {
                if (thrownException.offset >= caughtException.startOffset && thrownException.offset <= caughtException.endOffset) {
                    System.out.println("Check if " + thrownException.exceptionClassName + " in the catch list");
                    if (thrownException.match(caughtException.exceptionClass)) {
                        caughtException.seen = true;
                        inBothLists = true;
                        break;
                    }
                }
            }
            System.out.println("Check if " + thrownException.exceptionClassName + " in the throw list");
            System.out.println("Throws list: " + throwsList);
            for (String t : throwsList) {
                if (thrownException.match(t)) {
                    inThrowsList = true;
                    break;
                }

            }

            if (!inBothLists && !inThrowsList) {
                bugAccumulator.accumulateBug(new BugInstance(this, "UCE_DO_NOT_THROW_UNDECLARED_CHECKED_EXCEPTION", NORMAL_PRIORITY)
                        .addClassAndMethod(this),
                        SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, thrownException.offset));
            }
        }
        catchList.clear();
        throwList.clear();
        throwsList.clear();
    }

    @Override
    public void visit(Method method) {
        String[] m = space.split(method.toString());

        m[m.length - 1] += ",";
        int i = 0;
        int ii = 0;
        for (String s : m) {
            if (s.equals("Exceptions:")) {
                ii = i;
            }

            if (ii != 0 && ii != i) {
                //remove the last character of the string which is a colon
                String res = m[i].substring(0, s.length() - 1);
                //add the string to the array
                throwsList.add(res);
            }
            i++;
        }
    }
}
