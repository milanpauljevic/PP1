package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPC = -1;

	public int getMainPc() {
		return mainPC;
	}
	
	void initializeBuiltInFunctions() {
		{
	        // 'ord' i 'chr' .
	        Obj ordMethod = Tab.find("ord");
	        Obj chrMethod = Tab.find("chr");
	        ordMethod.setAdr(Code.pc);
	        chrMethod.setAdr(Code.pc);
	        Code.put(Code.enter);
	        Code.put(1);
	        Code.put(1);
	        Code.put(Code.load_n);
	        Code.put(Code.exit);
	        Code.put(Code.return_);
	 
	        Obj lenMethod = Tab.find("len");
	        lenMethod.setAdr(Code.pc);
	        Code.put(Code.enter);
	        Code.put(1);
	        Code.put(1);
	        Code.put(Code.load_n);
	        Code.put(Code.arraylength);
	        Code.put(Code.exit);
	        Code.put(Code.return_);
	        
	        //add i addAll
	        add_Set_A_elem_b();
	        addAll_Set_A_IntArr_B();
	        
	        //print za set
	        printSet();
	        
	        //Unija
	        union();
	    }
		
		
	}
	
	private void add_Set_A_elem_b() {
		Obj addMeth = Tab.find("add");
		addMeth.setAdr(Code.pc);
		
		
		//Inicijalizacija
		Code.put(Code.enter);
		Code.put(addMeth.getLevel());
		Code.put(addMeth.getLocalSymbols().size());
		
		//actSize = a[0];
		Code.put(Code.load_n);
		Code.put(Code.const_n);
		Code.put(Code.aload);
		Code.put(Code.store_2);
		
		//i = 1;
		Code.put(Code.const_1);
		Code.put(Code.store_3);
		
		int whileJump = Code.pc;
		//while(i<=actSize)
		Code.put(Code.load_3);
		Code.put(Code.load_2);
		
		Code.putFalseJump(Code.le, 0);
		int posleWhileFix = Code.pc-2;
		
		//if(a[0] == b)
		Code.put(Code.load_n);
		Code.put(Code.load_3);
		Code.put(Code.aload);
		Code.put(Code.load_1);
		Code.putFalseJump(Code.eq, 0);
		int skipIf = Code.pc-2;
		
		//return jer je pronadjen element u setu
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Code.fixup(skipIf);
		//i++;
		Code.put(Code.inc);
		Code.put(3);
		Code.put(1);
		
		//skok nazad na While
		Code.putJump(whileJump);
		
		//posle While
		Code.fixup(posleWhileFix);
		
		//actSize++;
		Code.put(Code.inc);
		Code.put(2);
		Code.put(1);
		
		//a[0] = actSize;
		Code.put(Code.load_n);
		Code.put(Code.const_n);
		Code.put(Code.load_2);
		Code.put(Code.astore);
		
		//a[actSize] = b;
		Code.put(Code.load_n);
		Code.put(Code.load_2);
		Code.put(Code.load_1);
		Code.put(Code.astore);
		
		//Izlaz
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void addAll_Set_A_IntArr_B() {
		Obj addAllMeth = Tab.find("addAll");
		addAllMeth.setAdr(Code.pc);
		int addAdr = Tab.find("add").getAdr();
		
		//Inicijalizacija
		Code.put(Code.enter);
		Code.put(addAllMeth.getLevel());
		Code.put(addAllMeth.getLocalSymbols().size());
		
		//i = 0;
		Code.put(Code.const_n);
		Code.put(Code.store_3);
		
		//size = b.size();
		Code.put(Code.load_1);
		Code.put(Code.arraylength);
		Code.put(Code.store_2);
		
		
		//while(i<size()){
		int whileStart = Code.pc;
		Code.put(Code.load_3);
		Code.put(Code.load_2);
		Code.putFalseJump(Code.lt, 0);
		int fixupAdr = Code.pc-2;
		
		//add(a, b[i]);
		Code.put(Code.load_n);
		Code.put(Code.load_1);
		Code.put(Code.load_3);
		Code.put(Code.aload);
		int pom = addAdr - Code.pc;
		Code.put(Code.call);
		Code.put2(pom);
		
		//i++;
		Code.put(Code.inc);
		Code.put(3);
		Code.put(1);
		
		Code.putJump(whileStart);
		Code.fixup(fixupAdr);
		
		//Izlaz
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private int unionFunction = 0;
	
	private void union() {
		unionFunction = Code.pc;
		Code.put(Code.enter);
		Code.put(3);
		Code.put(5);
		
		//S2=>S1
		//i=1;
		Code.put(Code.const_1);
		Code.put(Code.store_3);
		
		//size = s2[0];
		Code.put(Code.load_1);
		Code.put(Code.const_n);
		Code.put(Code.aload);
		Code.put(Code.store);
		Code.put(4);
		
		//while(i<=size)
		int whileStart = Code.pc;
		Code.put(Code.load_3);
		Code.put(Code.load);
		Code.put(4);
		Code.putFalseJump(Code.le, 0);
		int whileFixup = Code.pc-2;
		
		//add(s1, s2[i]);
		Code.put(Code.load_n);
		Code.put(Code.load_1);
		Code.put(Code.load_3);
		Code.put(Code.aload);
		int addAdr = Tab.find("add").getAdr();
		int pom = addAdr - Code.pc;
		Code.put(Code.call);
		Code.put2(pom);
		
		//i++;
		Code.put(Code.inc);
		Code.put(3);
		Code.put(1);
		
		Code.putJump(whileStart);
		Code.fixup(whileFixup);
		
		//S3=>S1
		//i=1;
		Code.put(Code.const_1);
		Code.put(Code.store_3);
		
		//size = s3[0];
		Code.put(Code.load_2);
		Code.put(Code.const_n);
		Code.put(Code.aload);
		Code.put(Code.store);
		Code.put(4);
		
		//while(i<=size)
		whileStart = Code.pc;
		Code.put(Code.load_3);
		Code.put(Code.load);
		Code.put(4);
		Code.putFalseJump(Code.le, 0);
		whileFixup = Code.pc-2;
		
		//add(s1, s2[i]);
		Code.put(Code.load_n);
		Code.put(Code.load_2);
		Code.put(Code.load_3);
		Code.put(Code.aload);
		pom = addAdr - Code.pc;
		Code.put(Code.call);
		Code.put2(pom);
		
		//i++;
		Code.put(Code.inc);
		Code.put(3);
		Code.put(1);
		
		Code.putJump(whileStart);
		Code.fixup(whileFixup);
		
		//Code.put(Code.);
		//Izlaz
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	
	public CodeGenerator() {
		initializeBuiltInFunctions();
	}
	
	//////////////////////
	//////////////////////
	///GENERISANJE KODA///
	//////////////////////
	//////////////////////

	
	//RETURN
	@Override
	public void visit(Statement_return_void statement_return_void) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(Statement_return_type statement_return_type) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	//METHOD
	@Override
	public void visit(MethodName methodName) {
		if(methodName.getI1().equalsIgnoreCase("main")){
			this.mainPC = Code.pc;
		}
		methodName.obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(methodName.obj.getLevel());
		Code.put(methodName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	
	//DESIGNATORS
	@Override
	public void visit(DesignatorArrayName designatorArrayName) {
		Code.load(designatorArrayName.obj);
	}
	
	
	//PRINT
	@Override
	public void visit(Statement_print statement_print) {
		int printNum = 0;
		
		if(statement_print.getNumForPrint() instanceof NumForPrint_yes) {
			NumForPrint_yes node = (NumForPrint_yes) statement_print.getNumForPrint();
			printNum = node.getN1();
		}
		Code.loadConst(printNum);
		if(statement_print.getExpr().struct.equals(SemanticAnalyzer.setType)) {
			int pom = printSetFunction - Code.pc;
			Code.put(Code.call);
			Code.put2(pom);
			return;
		}
		if(statement_print.getExpr().struct.equals(Tab.charType)) {
			Code.put(Code.bprint);
		}
		else {
			Code.put(Code.print);
		}
	}
	
	private int printSetFunction=-1;
	
	private void printSet() {
		printSetFunction = Code.pc;
		//void printSet( set s, int pom) int actSize, i;
		Code.put(Code.enter);
		Code.put(2);
		Code.put(4);
		
		//actSize = s[0];
		Code.put(Code.load_n);
		Code.put(Code.const_n);
		Code.put(Code.aload);
		Code.put(Code.store_2);
		
		//if(actSize==0)
		Code.put(Code.load_2);
		Code.put(Code.const_n);
		Code.putFalseJump(Code.eq, 0);
		int fixupIf = Code.pc-2;
		
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		//print(s[1], pom1);
		Code.fixup(fixupIf);
		Code.put(Code.load_n);
		Code.put(Code.const_1);
		Code.put(Code.aload);
		Code.put(Code.load_1);
		Code.put(Code.print);
		
		//i = 2;
		Code.put(Code.const_2);
		Code.put(Code.store_3);
		
		//while(i<=actSize){
		int whileStart = Code.pc;
		Code.put(Code.load_3);
		Code.put(Code.load_2);
		Code.putFalseJump(Code.le, 0);
		int fixupEnd = Code.pc-2;
		
		//print(' ', 0);
		Code.put(Code.const_);
		Code.put4(32);
		Code.put(Code.const_n);
		Code.put(Code.bprint);
		//print(s[i], 0)
		Code.put(Code.load_n);
		Code.put(Code.load_3);
		Code.put(Code.aload);
		Code.put(Code.const_n);
		Code.put(Code.print);
		
		//i++;
		Code.put(Code.inc);
		Code.put(3);
		Code.put(1);
		
		Code.putJump(whileStart);
		
		Code.fixup(fixupEnd);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	
	//READ
	public void visit(Statement_read statement_read) {
		if(statement_read.getDesignator().obj.getType().equals(Tab.charType))
			Code.put(Code.bread);
		else{
			Code.put(Code.read);
		}
		Code.store(statement_read.getDesignator().obj);
	}
	
	
	//METHOD CALL
	@Override
	public void visit(MethodCall methodCall) {
		int s = methodCall.getDesignator().obj.getAdr() - Code.pc ;
		Code.put(Code.call);
		Code.put2(s);
	}
	
	
	//FACTORS
	public void visit(Factor_des_noPars factor_des_noPars) {
		int s = factor_des_noPars.getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(s);
	}
	
	@Override
	public void visit(Factor_new_array factor_new_array) {
		if(factor_new_array.getType().struct.equals(SemanticAnalyzer.setType)) {
			Code.loadConst(1);
			Code.put(Code.add);
		}
		Code.put(Code.newarray);
		if(factor_new_array.getType().struct.equals(Tab.charType)) {
			Code.put(0);
		}
		else {
			Code.put(1);
		}
	}
	
	@Override
	public void visit(Factor_des factor_des) {
		Code.load(factor_des.getDesignator().obj);
	}
	
	@Override
	public void visit(Factor_des_pars factor_des_pars) {
		//MethodCall je ostavio na steku vrednost
	}


	//CONST VALUES
	@Override
	public void visit(ConstValue_char constValue_char) {
		Code.loadConst(constValue_char.getC1());
	}
	
	@Override
	public void visit(ConstValue_bool constValue_bool) {
		Code.loadConst(constValue_bool.getB1());
	}
	
	@Override
	public void visit(ConstValue_number constValue_number) {
		Code.loadConst(constValue_number.getN1());
	}

	
	//UNARY
	@Override
	public void visit(Unary_neg unary_neg) {
		Code.put(Code.neg);
	}
	
	
	//TERM
	@Override
	public void visit(Term_mulop termMulop) {
		if(termMulop.getMulop() instanceof Mulop_mul)
			Code.put(Code.mul);
		else if(termMulop.getMulop() instanceof Mulop_div)
			Code.put(Code.div);
		else 
			Code.put(Code.rem);
	}
	
	
	//EXPR
	@Override
	public void visit(Expr_addop expr_addop) {
		if(expr_addop.getAddop() instanceof Addop_add)
			Code.put(Code.add);
		else
			Code.put(Code.sub);
	}
	
	@Override
	public void visit(Expr_map expr_map) {
		String funkcija = expr_map.getDesignator().obj.getName();
		if(!mapiranje.containsKey(funkcija)) {
			mapiranje.put(funkcija, new ArrayList<>());
			funkZaMap.add(expr_map.getDesignator().obj);
		}
		Code.load(expr_map.getDesignator1().obj);
		Code.put(Code.call);
		mapiranje.get(funkcija).add(Code.pc);
		Code.put2(0);
	}

	
	//DESIGNATOR STATEMENTS
	@Override
	public void visit(DesignatorStatement_incr designatorStatement_incr) {
		if(designatorStatement_incr.getDesignator().obj.getKind()==Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement_incr.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorStatement_incr.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorStatement_decr designatorStatement_decr) {
		if(designatorStatement_decr.getDesignator().obj.getKind()==Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement_decr.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorStatement_decr.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorStatement_setop designatorStatement_setop) {
		Code.load(designatorStatement_setop.getDesignator().obj);
		Code.load(designatorStatement_setop.getDesignator1().obj);
		Code.load(designatorStatement_setop.getDesignator2().obj);
		int pom = unionFunction - Code.pc;
		Code.put(Code.call);
		Code.put2(pom);
	}
	
	@Override
	public void visit(DesignatorStatement_actParsEmpty designatorStatement_actParsEmpty) {
		int s = designatorStatement_actParsEmpty.getDesignator().obj.getAdr()-Code.pc;
		Code.put(Code.call);
		Code.put2(s);
		if(!designatorStatement_actParsEmpty.getDesignator().obj.getType().equals(Tab.noType)) {
			Code.put(Code.pop);
		}
		
	}
	
	@Override
	public void visit(DesignatorStatement_actPars designatorStatement_actPars) {
		//MethodCall zavrsio vecinu
		if(!designatorStatement_actPars.getMethodCall().getDesignator().obj.getType().equals(Tab.noType)) {
			Code.put(Code.pop);
		}
	}

	@Override
	public void visit(DesignatorStatement_assignExpr designatorStatement_assignExpr) {
		Code.store(designatorStatement_assignExpr.getDesignator().obj);
	}
	
	
	//IF AND ELSE

	private Stack<Integer> jumpToElse = new Stack<>();
	private Stack<Integer> ifStmtEnd = new Stack<>();
	
	@Override
	public void visit(IfCond_correct ifCond_correct) {
		Code.putJump(0);//jump to else
		jumpToElse.add(Code.pc-2);
		while(!jumpToThen.empty()) {
			Code.fixup(jumpToThen.pop());
		}
	}
	
	@Override
	public void visit(ElseStatement_no elseStatement_no) {
		Code.fixup(jumpToElse.pop());
	}
	
	@Override
	public void visit(ElseStatement_yes elseStatement_yes) {
		Code.fixup(ifStmtEnd.pop());
	}
	
	@Override
	public void visit(ElseNonTerm elseNonTerm) {
		Code.putJump(0);
		ifStmtEnd.add(Code.pc-2);
		Code.fixup(jumpToElse.pop());
	}
	
	
	//CONDITION
	
	private Stack<Integer> skipCondTerm = new Stack<>();
	private Stack<Integer> jumpToThen = new Stack<>();
	
	@Override 
	public void visit(Condition_op condition_op) {
		Code.putJump(0);
		jumpToThen.add(Code.pc-2);
		while(!skipCondTerm.empty()) {
			Code.fixup(skipCondTerm.pop());
		}
	}
	
	@Override 
	public void visit(Condition_noOp condition_noOp) {
		Code.putJump(0);
		jumpToThen.add(Code.pc-2);
		while(!skipCondTerm.empty()) {
			Code.fixup(skipCondTerm.pop());
		}
	}
	
	
	//CONDTERM
	@Override 
	public void visit(CondTerm_op condTerm_op) {
		//CondFact vrsi posao
	}
	
	@Override 
	public void visit(CondTerm_noOp condTerm_noOp) {
		//CondFact vrsi posao
	}
	
	
	//CONDFACT
	@Override 
	public void visit(CondFact_op condFact_op) {
		//na steku su obe vrednost
		int op;
		if		(condFact_op.getRelop() instanceof Relop_equal) 			op = Code.eq;
		else if	(condFact_op.getRelop() instanceof Relop_notEqual) 			op = Code.ne;
		else if	(condFact_op.getRelop() instanceof Relop_greater) 			op = Code.gt;
		else if	(condFact_op.getRelop() instanceof Relop_greaterOrEqual) 	op = Code.ge;
		else if	(condFact_op.getRelop() instanceof Relop_less) 				op = Code.lt;
		else /*if(condFact_op.getRelop() instanceof Relop_lessOrEqual)*/ 	op = Code.le;
		Code.putFalseJump(op, 0);
		skipCondTerm.add(Code.pc-2);
	}
	
	@Override 
	public void visit(CondFact_noOp condFact_noOp) {
		//na steku imamo true ili false;
		Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		skipCondTerm.add(Code.pc-2);
	}
	
	
	//DO WHILE
	
	private Stack<Integer> doStack = new Stack<>();
	private Stack<List<Integer>> jumpOutStack = new Stack<>();
	private Stack<List<Integer>> continueStack = new Stack<>();
	
	@Override
	public void visit(DoNonTerm doNonTerm) {
		doStack.add(Code.pc);
		jumpOutStack.add(new ArrayList<Integer>());
		continueStack.add(new ArrayList<Integer>());
	}
	
	@Override
	public void visit(WhileNonTerm whileNonTerm) {
		for(int i: continueStack.peek())
			Code.fixup(i);
	}
	
	@Override
	public void visit(WhileArgs_cond_des whileArgs_cond_des) {
		Code.putJump(doStack.peek());
		for(int i: jumpOutStack.peek())
			Code.fixup(i);
	}
	
	@Override
	public void visit(WhileArgs_cond whileArgs_cond) {
		Code.putJump(0);//jump out
		jumpOutStack.peek().add(Code.pc-2);
		while(!jumpToThen.empty()) {
			Code.fixup(jumpToThen.pop());
		}
		Code.putJump(doStack.peek());
		for(int i: jumpOutStack.peek())
			Code.fixup(i);
	}
	
	@Override
	public void visit(WhileArgs_empty whileArgs_empty) {
		Code.putJump(doStack.peek());
		for(int i: jumpOutStack.peek())
			Code.fixup(i);
	}
	
	@Override
	public void visit(CommaNonTerm commaNonTerm) {
		Code.putJump(0);//jump out
		jumpOutStack.peek().add(Code.pc-2);
		while(!jumpToThen.empty()) {
			Code.fixup(jumpToThen.pop());
		}
	}
	
	@Override
	public void visit(Statement_do_while statement_do_while) {
		doStack.pop();
		jumpOutStack.pop();
		continueStack.pop();
	}
	
	
	//CONTINUE I BREAK
	@Override
	public void visit(Statement_break statement_break) {
		Code.putJump(0);
		jumpOutStack.peek().add(Code.pc-2);
	}
	
	@Override
	public void visit(Statement_continue statement_continue) {
		Code.putJump(0);
		continueStack.peek().add(Code.pc-2);
	}
	
	//PROGRAM
	
	private Map<String, List<Integer>> mapiranje = new HashMap<>();
	private List<Obj> funkZaMap = new ArrayList<>();
	
	@Override
	public void visit(Program program) {
		for(Obj funk: funkZaMap) {
			List<Integer> lista = mapiranje.get(funk.getName());
			int adr = funk.getAdr();
			for(int i: lista) {
				Code.fixup(i);
			}
			
			//kod
			Code.put(Code.enter);
			Code.put(1);
			Code.put(4);
			
			//i = 0, suma = 0 vec postavljeno
			
			//size = niz.size();
			Code.put(Code.load_n);
			Code.put(Code.arraylength);
			Code.put(Code.store_2);
			
			//while(i<size)
			int whileStart = Code.pc;
			Code.put(Code.load_1);
			Code.put(Code.load_2);
			Code.putFalseJump(Code.lt, 0);
			int whileFixup = Code.pc-2;
			
			//suma += funkcija(niz[i]);
			Code.put(Code.load_n);
			Code.put(Code.load_1);
			Code.put(Code.aload);
			int pom = adr - Code.pc;
			Code.put(Code.call);
			Code.put2(pom);
			//povratna vrednost je na steku
			Code.put(Code.load_3);
			Code.put(Code.add);
			Code.put(Code.store_3);
			
			//i++;
			Code.put(Code.inc);
			Code.put(1);
			Code.put(1);
			
			Code.putJump(whileStart);
			
			Code.fixup(whileFixup);
			
			//ostavi vrednost na steku
			Code.put(Code.load_3);
			
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
	}
}
