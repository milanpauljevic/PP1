package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;


public class SemanticAnalyzer extends VisitorAdaptor {
	
	
	private boolean errorDetected = false;
	Logger log = Logger.getLogger(getClass());
	HashSet<String> reservedNames = 
					new HashSet<String>(Set.of("int", "char", "bool", "set", "null", "eol", "chr", "ord", "add", "addAll"));
	
	private Struct boolType = Tab.find("bool").getType();
	private Struct setType = Tab.find("set").getType();
	private Obj currProgram;
	private Struct currType;
	private int constant;
	private Struct constantType;
	private Struct methodRetType;
	private Obj currMethod;
	private boolean mainDeclared = false;
	private int loopCounter = 0;
	private boolean returnHappened = false;
	private Stack<ArrayList<Struct>> ActParsStack = new Stack<>();
	
	/* LOG MESSAGES */
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public boolean passed() {
		return !errorDetected;
	}
	
	String ObjToString(Obj obj) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		switch (obj.getKind()) {
			case Obj.Con:  sb.append("Con "); break;
			case Obj.Var:  sb.append("Var "); break;
			case Obj.Type: sb.append("Type "); break;
			case Obj.Meth: sb.append("Meth "); break;
			case Obj.Prog: sb.append("Prog "); break;
		}
		sb.append(obj.getName()).append(typeToString(obj.getType())).append(obj.getAdr()).append(" ").append(obj.getLevel()).append("]");
		return sb.toString();
	}
	
	String typeToString(Struct struct) {
		switch (struct.getKind()) {
			case Struct.Int: 	return " int ";
			case Struct.Bool:  	return " bool ";
			case Struct.Array: 	return " Array of" + typeToString(struct.getElemType());
			case Struct.Enum: 	return " set ";
			case Struct.Char:	return " char ";
			case Struct.None:	return " void ";
		}
		return "Pogresan tip";
	}
	
	//////////////////////
	//////////////////////
	//SEMANTICKA ANALIZA//
	//////////////////////
	//////////////////////
	
	//Program
	@Override
	public void visit(ProgName programName) {
		String progName = programName.getI1();
		if(reservedNames.contains(progName)) {
			report_error("Ime programa ne moze biti "+ progName + ", jer je to rezervirana rec", programName);
			progName = "__GRESKA__";
		}
		
		currProgram = Tab.insert(Obj.Prog, programName.getI1(), Tab.noType);
		Tab.openScope();
	}
	
	@Override
	public void visit(Program program) {
		Tab.chainLocalSymbols(currProgram);
		Tab.closeScope();
		if(!mainDeclared) {
			report_error("U programu nije definisana MAIN metoda", program);
		}
	}
	
	
	//Type
	@Override
	public void visit(Type type) {
		Obj objType = Tab.find(type.getI1());
		if(objType == Tab.noObj) {
			report_error("Nepostojeci tip: "+type.getI1(), type);
			currType = Tab.noType;
		}
		else if(objType.getKind()!=Obj.Type){
			report_error("Nedozvoljen tip: "+type.getI1(), type);
			currType = Tab.noType;
		}
		else {
			currType = objType.getType();
		}
	}
	
	
	//CONST DECLARATIONS
	@Override
	public void visit(ConstDecl constDecl) {
		if(reservedNames.contains(constDecl.getI1())) {
			report_error("Konstanta ne moze imati naziv rezervisane reci: " + constDecl.getI1(), constDecl);
		}
		if(Tab.find(constDecl.getI1())!=Tab.noObj) {
			report_error("Dvostruka definicija konstante " + constDecl.getI1(), constDecl);
		}
		else if(!constantType.assignableTo(currType)){
			report_error("Nedozvoljena dodela vrednosti konstanti: " + constDecl.getI1(), constDecl);
		}
		else {
			Obj obj = Tab.insert(Obj.Con, constDecl.getI1(), currType);
			obj.setAdr(constant);
		}
	}
	
	@Override
	public void visit(ConstValue_number val) {
		constant = val.getN1();
		constantType = Tab.intType;
	}
	
	@Override
	public void visit(ConstValue_bool val) {
		constant = val.getB1();
		constantType = boolType;
	}
	
	@Override
	public void visit(ConstValue_char val) {
		constant = val.getC1();
		constantType = Tab.charType;
	}
	
	
	//VAR DECLARATIONS
	@Override
	public void visit(VarDecl_var varDecl_var) {
		if(reservedNames.contains(varDecl_var.getI1())) {
			report_error("Promenljiva ne moze imati naziv rezervisane reci: " + varDecl_var.getI1(), varDecl_var);
		}
		Obj o= (currMethod==null ? Tab.find(varDecl_var.getI1()) : Tab.currentScope.findSymbol(varDecl_var.getI1()));
		if(o!=Tab.noObj && o!=null) {
			report_error("Dvostruka definicija promenljive " + varDecl_var.getI1(), varDecl_var);
		}
		else {
			Tab.insert(Obj.Var, varDecl_var.getI1(), currType);
		}
	}
	
	@Override
	public void visit(VarDecl_array varDecl_array) {
		if(reservedNames.contains(varDecl_array.getI1())) {
			report_error("Promenljiva ne moze imati naziv rezervisane reci: " + varDecl_array.getI1(), varDecl_array);
		}
		if(Tab.find(varDecl_array.getI1())!=Tab.noObj) {
			report_error("Dvostruka definicija promenljive " + varDecl_array.getI1(), varDecl_array);
		}
		else {
			Tab.insert(Obj.Var, varDecl_array.getI1(), new Struct(Struct.Array, currType));
		}
	}
	
	
	//METHOD DECLARATIONS
	@Override
	public void visit(MethodType_void methodType_void) {
		methodRetType = Tab.noType;
	}
	
	@Override
	public void visit(MethodType_type methodType_type) {
		methodRetType = currType;
	}
	
	@Override
	public void visit(MethodName methodName) {
		if(reservedNames.contains(methodName.getI1())) {
			report_error("Naziv metode ne moze biti rezervisana rec: " + methodName.getI1(), methodName);
		}
		if(Tab.find(methodName.getI1())!=Tab.noObj) {
			report_error("Dvostruko deklarisana metoda: " + methodName.getI1(), methodName);
		}
		if(methodName.getI1().equalsIgnoreCase("main")){
			if(methodRetType != Tab.noType) {
				report_error("Povratna vrednost MAIN metode mora biti VOID!", methodName);
			}
			mainDeclared = true;
		}
		currMethod = Tab.insert(Obj.Meth, methodName.getI1(), methodRetType);
		Tab.openScope();
	}
	
	@Override
	public void visit(MethodRecursionHelper methodRecursionHelper) {
		//u vreme obilaska ovog cvora su definisani svi formalni parametri i lokalne promenljive trenutne metode
		//korisno zbog rekurzije
		Tab.chainLocalSymbols(currMethod);
	}
	
	@Override
	public void visit(MethodDecl methodDecl) {
		if(currMethod==null) {
			report_error("Neocekivana greska u definicji metode", methodDecl);
			return;
		}
		if(!returnHappened && !currMethod.getType().equals(Tab.noType)) {
			report_error("Ne postoji return u metodi " + currMethod.getName(), methodDecl);
		}
		Tab.chainLocalSymbols(currMethod);
		returnHappened = false;
		currMethod = null;
		Tab.closeScope();
	}
	
	
	//FORMAL PARAMETERS
	@Override
	public void visit(FormParam_noArray formParam_noArray) {
		if(reservedNames.contains(formParam_noArray.getI2())) {
			report_error("Formalni parametar ne moze imati naziv rezervisane reci: " + formParam_noArray.getI2(), formParam_noArray);
		}
		if(Tab.currentScope.findSymbol(formParam_noArray.getI2())!=null) {
			report_error("Vec definisan formalni parametar: " + formParam_noArray.getI2(), formParam_noArray);
		}
		if(currMethod.getName().equalsIgnoreCase("main")) {
			report_error("MAIN metoda ne moze da ima formalne parametre", formParam_noArray);
		}
		Obj obj = Tab.insert(Obj.Var, formParam_noArray.getI2(), currType);
		obj.setFpPos(1);
		currMethod.setLevel(currMethod.getLevel()+1);
	}
	
	@Override
	public void visit(FormParam_array formParam_array) {
		if(reservedNames.contains(formParam_array.getI2())) {
			report_error("Formalni parametar ne moze imati naziv rezervisane reci: " + formParam_array.getI2(), formParam_array);
		}
		if(Tab.currentScope.findSymbol(formParam_array.getI2())!=null) {
			report_error("Vec definisan formalni parametar: " + formParam_array.getI2(), formParam_array);
		}
		if(currMethod.getName().equalsIgnoreCase("main")) {
			report_error("MAIN metoda ne moze da ima formalne parametre", formParam_array);
		}
		Obj obj = Tab.insert(Obj.Var, formParam_array.getI2(), new Struct(Struct.Array, currType));
		obj.setFpPos(1);
		currMethod.setLevel(currMethod.getLevel()+1);
	}
	
	
	//FACTORS
	@Override
	public void visit(Factor_des_noPars factor_des_noPars) {
		Obj obj = factor_des_noPars.getDesignator().obj;
		if(obj.getKind() == Obj.Meth && obj.getLevel()==0) {
			factor_des_noPars.struct = obj.getType();
		}
		else {
			report_error("Neadekvatno pozivanje metode " + obj.getName(), factor_des_noPars);
			factor_des_noPars.struct = Tab.noType;
		}
	}
	
	@Override
	public void visit(Factor_new_array factor_new_array) {
		if(factor_new_array.getExpr().struct != Tab.intType) {
			report_error("U uglastim zagradama mora biti expr tipa int", factor_new_array);
			factor_new_array.struct = Tab.noType;
		}
		else {
			if(currType.equals(setType)) {
				factor_new_array.struct = setType;
			}
			else {
				factor_new_array.struct = new Struct(Struct.Array, currType);
			}
		}
	}
	
	@Override
	public void visit(Factor_expr factor_expr) {
		factor_expr.struct = factor_expr.getExpr().struct;
	}
	
	@Override
	public void visit(Factor_des factor_des) {
		factor_des.struct = factor_des.getDesignator().obj.getType();
	}
	
	@Override
	public void visit(Factor_constValue factor_constValue) {
		factor_constValue.struct = constantType;
	}
	
	@Override
	public void visit(Factor_des_pars factor_des_pars) {
		//Ostatak je u MethodCall visit-u
		factor_des_pars.struct = factor_des_pars.getMethodCall().struct;
	}
	
	
	//DESIGNATORS
	@Override
	public void visit(Designator_ident designator_ident) {
		designator_ident.obj=Tab.find(designator_ident.getI1());
		if(designator_ident.obj == Tab.noObj) {
			report_error("Nedefinisan designator " + designator_ident.getI1(), designator_ident);
		}
		else if(designator_ident.obj.getFpPos()==1){
			report_info("Upotreba formalnog parametra: " + ObjToString(designator_ident.obj), designator_ident);
		}
		else {
			switch (designator_ident.obj.getKind()) {
				case Obj.Con:  report_info("Upotreba konstante: " + ObjToString(designator_ident.obj), designator_ident);; break;
				case Obj.Var:  report_info("Upotreba promenljive: " + ObjToString(designator_ident.obj), designator_ident);; break;
				case Obj.Meth: report_info("Upotreba metode: " + ObjToString(designator_ident.obj), designator_ident);; break;
			}
		}
	}
	
	@Override
	public void visit(Designator_array designator_array) {
		String arrayName = designator_array.getDesignatorArrayName().getI1();
		Obj obj=Tab.find(arrayName);
		if(obj.getKind()!=Obj.Var && obj.getType().getKind()!=Struct.Array) {
			designator_array.obj = Tab.noObj;
			report_error("Nije moguce koriscenje designatora " + designator_array.getDesignatorArrayName().getI1() + " kao niza" , designator_array);
		}
		else if(designator_array.getExpr().struct != Tab.intType){
			designator_array.obj = Tab.noObj;
			report_error("Izraz pri indeksiranju mora biti tipa int", designator_array);
		}
		else {
			designator_array.obj = new Obj(Obj.Elem, arrayName + "[@]", obj.getType().getElemType());
			report_info("Upotreba elementa niza " + ObjToString(obj), designator_array);
		}
	}
	

	//EXPR
	@Override
	public void visit(Expr_addop expr_addop) {
		Struct exprType = expr_addop.getExpr().struct;
		Struct termType = expr_addop.getTerm().struct;
		if(exprType.compatibleWith(Tab.intType) && exprType.compatibleWith(termType)) {
			expr_addop.struct = Tab.intType;
		}
		else {
			report_error("Neadekvatni tipovi u clanova addop operacije", expr_addop);
			expr_addop.struct = Tab.noType;
		}
	}
	
	@Override
	public void visit(Expr_term expr_term) {
		expr_term.struct = expr_term.getTerm().struct;
	}
	
	@Override
	public void visit(Expr_map expr_map) {
		Obj desLeft = expr_map.getDesignator().obj;
		Obj desRight = expr_map.getDesignator1().obj;
		if(desLeft.getKind()!=Obj.Meth) {
			report_error("Promenljiva" + desLeft.getName() + " nije metoda", expr_map);
			expr_map.struct = Tab.noType;
		}
		else if(!desLeft.getType().equals(Tab.intType)) {
			report_error("Metoda " + desLeft.getName() + " nema povratnu vrednost int", expr_map);
			expr_map.struct = Tab.noType;
			
		}
		else if(desLeft.getLevel()!=1) {
			report_error("Metoda " + desLeft.getName() + " nema samo jedan parametar", expr_map);
			expr_map.struct = Tab.noType;
		}
		else if(!desLeft.getLocalSymbols().iterator().next().getType().equals(Tab.intType)) {
			report_error("Formalni parametar metode " + desLeft.getName() + " mora biti tipa int", expr_map);
			expr_map.struct = Tab.noType;
		}
		else if(desRight.getKind()!=Obj.Var || desRight.getType().getKind()!=Struct.Array || !desRight.getType().getElemType().equals(Tab.intType)) {
			report_error("Desni designator mora biti int niz", expr_map);
			expr_map.struct = Tab.noType;
		}
		else {
			expr_map.struct = Tab.intType;
		}
	}
	
	
	//UNARY
	@Override
	public void visit(Unary_neg unary_neg) {
		if(!unary_neg.getFactor().struct.equals(Tab.intType)) {
			report_error("Negacija ne int faktora", unary_neg);
			unary_neg.struct = Tab.noType;
		}
		else {
			unary_neg.struct = Tab.intType;
		}
	}
	@Override
	public void visit(Unary_pos unary_pos) {
		unary_pos.struct = unary_pos.getFactor().struct;
	} 
	
	
	//TERM
	@Override
	public void visit(Term_unary term_unary) {
		term_unary.struct = term_unary.getUnary().struct;
	}
	
	@Override
	public void visit(Term_mulop term_mulop) {
		Struct termType = term_mulop.getTerm().struct;
		Struct unaryType = term_mulop.getUnary().struct;
		if(termType.equals(Tab.intType) && termType.equals(unaryType)) {
			term_mulop.struct = Tab.intType;
		}
		else {
			report_error("Neadekvatni tipovi u clanova mulop operacije", term_mulop);
			term_mulop.struct = Tab.noType;
		}
	}
	
	
	//CONDFACT
	@Override
	public void visit(CondFact_op condFact_op) {
		Struct expr1 = condFact_op.getExpr().struct;
		Struct expr2 = condFact_op.getExpr1().struct;
		if(!expr1.compatibleWith(expr2)) {
			report_error("Izrazi nisu kompatibilni", condFact_op);
			condFact_op.struct = Tab.noType;
		}
		else if(expr1.isRefType() && ! (condFact_op.getRelop() instanceof Relop_equal) && ! (condFact_op.getRelop() instanceof Relop_notEqual)) {
			report_error("Referentne tipove je moguce uporedjivati samo sa == i !=", condFact_op);
			condFact_op.struct = Tab.noType;
		}
		else {
			condFact_op.struct = boolType;
		}
	}
	
	public void visit(CondFact_noOp condFact_noOp) {
		if(!condFact_noOp.getExpr().struct.equals(boolType)) {
			report_error("Izraz mora biti tipa bool", condFact_noOp);
			condFact_noOp.struct = Tab.noType;
		}
		else {
			condFact_noOp.struct = boolType;
		}
	}
	
	
	//CONDFACT
	@Override
	public void visit(CondTerm_op condTerm_op) {
		Struct expr1 = condTerm_op.getCondFact().struct;
		Struct expr2 = condTerm_op.getCondTerm().struct;
		if(!expr1.equals(expr2) && ! expr1.equals(boolType)) {
			report_error("Izrazi nisu kompatibilni (tip bool)", condTerm_op);
			condTerm_op.struct = Tab.noType;
		}
		else {
			condTerm_op.struct = boolType;
		}
	}
	
	public void visit(CondTerm_noOp condterm_noOp) {
		if(!condterm_noOp.getCondFact().struct.equals(boolType)) {
			report_error("CondFact mora biti tipa bool", condterm_noOp);
			condterm_noOp.struct = Tab.noType;
		}
		else {
			condterm_noOp.struct = boolType;
		}
	}
	
	
	//CONDITION
	public void visit(Condition_op condition_op) {
		Struct expr1 = condition_op.getCondition().struct;
		Struct expr2 = condition_op.getCondTerm().struct;
		if(!expr1.equals(expr2) && ! expr1.equals(boolType)) {
			report_error("Izrazi nisu kompatibilni (tip bool)", condition_op);
			condition_op.struct = Tab.noType;
		}
		else {
			condition_op.struct = boolType;
		}
	}
	
	public void visit(Condition_noOp condition_noOp) {
		if(!condition_noOp.getCondTerm().struct.equals(boolType)) {
			report_error("CondTerm mora biti tipa bool", condition_noOp);
			condition_noOp.struct = Tab.noType;
		}
		else {
			condition_noOp.struct = boolType;
		}
	}
	
	
	//DESIGNATOR STATEMENTS
	@Override
	public void visit( DesignatorStatement_assignExpr designatorStatement_assignExpr) {
		Obj des = designatorStatement_assignExpr.getDesignator().obj;
		Struct exprType = designatorStatement_assignExpr.getExpr().struct;
		if(des.getKind()!=Obj.Var && des.getKind()!=Obj.Elem) {
			report_error("Designator mora biti promenljiva ili element niza", designatorStatement_assignExpr);
		}
		else if(!exprType.assignableTo(des.getType())) {
			report_error("Izraz se ne moze dodeliti designatoru", designatorStatement_assignExpr);
		}
	}
	
	@Override
	public void visit(DesignatorStatement_actPars designatorStatement_actPars) {
		//Ceo posao je u MethodCall visit-u
	}
	
	@Override
	public void visit(DesignatorStatement_actParsEmpty designatorStatement_actParsEmpty) {
		Obj obj = designatorStatement_actParsEmpty.getDesignator().obj;
		if(!(obj.getKind() == Obj.Meth) || obj.getLevel()!=0) {
			report_error("Neadekvatno pozivanje metode " + obj.getName(), designatorStatement_actParsEmpty);
		}
	}
	@Override
	public void visit(DesignatorStatement_incr designatorStatement_incr) {
		Obj des = designatorStatement_incr.getDesignator().obj;
		if(des.getKind()!=Obj.Var && des.getKind()!=Obj.Elem) {
			report_error("Designator mora biti promenljiva ili elementt niza", designatorStatement_incr);
		}
		else if(!des.getType().equals(Tab.intType)) {
			report_error("Samo tip int moze da se inkrementira", designatorStatement_incr);
		}
	}
	@Override
	public void visit(DesignatorStatement_decr designatorStatement_decr) {
		Obj des = designatorStatement_decr.getDesignator().obj;
		if(des.getKind()!=Obj.Var && des.getKind()!=Obj.Elem) {
			report_error("Designator mora biti promenljiva ili element niza", designatorStatement_decr);
		}
		else if(!des.getType().equals(Tab.intType)) {
			report_error("Samo tip int moze da se dekrementira", designatorStatement_decr);
		}
	}
	@Override
	public void visit(DesignatorStatement_setop designatorStatement_setop) {
		Struct des1 = designatorStatement_setop.getDesignator().obj.getType();
		Struct des2 = designatorStatement_setop.getDesignator1().obj.getType();
		Struct des3 = designatorStatement_setop.getDesignator2().obj.getType();
		if(!des1.equals(setType)) {
			report_error("Prvi designator nije skup", designatorStatement_setop);
		}
		if(!des2.equals(setType)) {
			report_error("Drugi designator nije skup", designatorStatement_setop);
		}
		if(!des3.equals(setType)) {
			report_error("Treci designator nije skup", designatorStatement_setop);
		}
	}
	
	
	//BREAK
	@Override
	public void visit(Statement_break statement_break) {
		if(loopCounter==0) {
			report_error("Break direktiva se moze koristiti samo unutar petlje", statement_break);
		}
	}
	
	
	//CONTINUE
	@Override
	public void visit(Statement_continue statement_continue) {
		if(loopCounter==0) {
			report_error("Continue direktiva se moze koristiti samo unutar petlje", statement_continue);
		}
	}
	
	
	//DO-WHILE
	@Override
	public void visit(DoNonTerm doNonterm) {
		loopCounter++;
	}
	
	@Override
	public void visit(Statement_do_while Statement_do_while) {
		loopCounter--;
	}
	
	
	//RETURN
	@Override
	public void visit(Statement_return_void statement_return_void) {
		if(currMethod == null) {
			report_error("Koriscenje return direktive van globalne funkcije", statement_return_void);
		}
		else if(!currMethod.getType().equals(Tab.noType)) {
			report_error("Pokusaj return; u metodi ciji tip povratne vrednosti nije void", statement_return_void);
		}
		else {
			returnHappened = true;
		}
	}
	
	@Override
	public void visit(Statement_return_type statement_return_type) {
		if(currMethod == null) {
			report_error("Koriscenje return direktive van globalne funkcije", statement_return_type);
		}
		else if(!currMethod.getType().equals(statement_return_type.getExpr().struct)) {
			report_error("Tip vrednosti koja se vraca se ne poklapa sa povratnim tipom metode: " + currMethod.getName(), statement_return_type);
		}
		else {
			returnHappened = true;
		}
	}
	
	
	//IF
	@Override
	public void visit(IfCond_correct ifCond_correct) {
		if(!ifCond_correct.getCondition().struct.equals(boolType)) {
			report_error("Uslov u if-u mora biti tipa bool", ifCond_correct);
		}
	}
	
	
	//READ
	public void visit(Statement_read statement_read) {
		Obj obj = statement_read.getDesignator().obj;
		 if(obj.getKind()!=Obj.Var && obj.getKind()!=Obj.Elem) {
			 report_error("Designator mora biti promenljiva ili element niza", statement_read);
		 }
		 else if(!obj.getType().equals(Tab.intType) && !obj.getType().equals(Tab.charType) && !obj.getType().equals(boolType)) {
			 report_error("Tip promenljive mora biti int, char ili bool", statement_read);
		 }
	}
	
	
	//PRINT
	public void visit(Statement_print statement_print) {
		Struct exprType = statement_print.getExpr().struct;
		if(!exprType.equals(Tab.intType) && !exprType.equals(Tab.charType) && !exprType.equals(boolType) && !exprType.equals(setType)) {
			report_error("Ono sto se ispisuje mora biti tipa int, char, bool ili set", statement_print);
		}
	}
	
	
	//ACT PARS
	@Override
	public void visit(ActParsIntro actParsIntro) {
		ActParsStack.add(new ArrayList<Struct>());
	}
	
	@Override
	public void visit(ActPars_mul actPars_mul) {
		ActParsStack.peek().add(actPars_mul.getExpr().struct);
	}
	
	@Override
	public void visit(ActPars_one actPars_one) {
		ActParsStack.peek().add(actPars_one.getExpr().struct);
	}
	
	@Override
	public void visit(MethodCall methodCall) {
		Obj method = methodCall.getDesignator().obj;
		if(method.getKind()!=Obj.Meth) {
			report_error("Pokusaj pozivanja promeljive kao metode: " + methodCall.getDesignator().obj.getName(), methodCall);
			methodCall.struct = Tab.noType;
		}
		else if(ActParsStack.empty()) {
			report_error("NEOCEKIVANA GRESKA U STEKU ACT_PARS", methodCall);
			methodCall.struct = Tab.noType;
		}
		else {
			report_info("Poziv metode: " + ObjToString(method), methodCall);
			ArrayList<Struct> FormParams = new ArrayList<>();
			for(Obj obj: method.getLocalSymbols()) {
				if(obj.getFpPos()==1)
					FormParams.add(obj.getType());
			}
			if(FormParams.size()!=ActParsStack.peek().size()) {
				report_error("Nije jednak broj formalnih i broj prosledjenih parametara za metodu " + method.getName(), methodCall);
				methodCall.struct = Tab.noType;
			}
			else {
				int sz = FormParams.size();
				for(int i = 0; i<sz;++i) {
					if(!ActParsStack.peek().get(i).assignableTo(FormParams.get(i))) {
						report_error("Parametri na poziciji "+i+" nisu jednaki pri pozivu metode " + method.getName(), methodCall);
						methodCall.struct = Tab.noType;
						ActParsStack.pop();
						return;
					}
				}
				methodCall.struct = method.getType();
				ActParsStack.pop();
			}
		}
	}
	
}
