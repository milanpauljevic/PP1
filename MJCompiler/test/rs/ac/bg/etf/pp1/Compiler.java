package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Compiler {
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		
		Logger log = Logger.getLogger(Compiler.class);
		
		Reader br = null;
		try {
			File sourceCode = new File("test/program.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			/*
				Formiranje AST-a
			*/
			MJParser p = new MJParser(lexer);
	        Symbol s = p.parse();
	        
	        /*
	        	ispis sintaksnog stabla
	        */
	        Program prog = (Program)(s.value); 
			
			log.info(prog.toString(""));
			if(p.errorDetected) {
				log.info("Broj gresaka u parsiranju je " + p.numberOfErrors);
			}
			log.info("===================================");
			
			/*
				Inicijalizacija tabele simbola
			*/
			//bool
			Tab.init();
			Struct boolType = new Struct(Struct.Bool);
			Obj boolObj = Tab.insert(Obj.Type, "bool", boolType);
			boolObj.setAdr(-1);
			boolObj.setLevel(-1);
			
			//set
			Struct setType = new Struct(Struct.Enum);
			Obj setObj = Tab.insert(Obj.Type, "set", setType);
			setObj.setAdr(-1);
			setObj.setLevel(-1);
			
			//void add(set a, int b) int actPars, i;
			Obj addMethod = Tab.insert(Obj.Meth, "add", Tab.noType);
			{
				Tab.openScope();
				Obj addParam1 = new Obj(Obj.Var, "a", setType, 0, 1);
				Obj addParam2 = new Obj(Obj.Var, "b", Tab.intType, 1, 1);
				Obj addParam3 = new Obj(Obj.Var, "actPars", Tab.intType, 2, 1);
				Obj addParam4 = new Obj(Obj.Var, "i", Tab.intType, 3, 1);
				addParam1.setFpPos(1);
				addParam2.setFpPos(1);
				Tab.currentScope.addToLocals(addParam1);
				Tab.currentScope.addToLocals(addParam2);
				Tab.currentScope.addToLocals(addParam3);
				Tab.currentScope.addToLocals(addParam4);
				Tab.chainLocalSymbols(addMethod);
				addMethod.setLevel(2);
				Tab.closeScope();
			}
			
			//void addAll(set a, int b[]);
			Obj addAllMethod = Tab.insert(Obj.Meth, "addAll", Tab.noType);
			{
				Tab.openScope();
				Obj addAllParam1 = new Obj(Obj.Var, "a", setType, 0, 1);
				Obj addAllParam2 = new Obj(Obj.Var, "b", new Struct(Struct.Array, Tab.intType), 1, 1);
				Obj addAllParam3 = new Obj(Obj.Var, "size", Tab.intType, 2, 1);
				Obj addAllParam4 = new Obj(Obj.Var, "i", Tab.intType, 3, 1);
				addAllParam1.setFpPos(1);
				addAllParam2.setFpPos(1);
				Tab.currentScope.addToLocals(addAllParam1);
				Tab.currentScope.addToLocals(addAllParam2);
				Tab.currentScope.addToLocals(addAllParam3);
				Tab.currentScope.addToLocals(addAllParam4);
				Tab.chainLocalSymbols(addAllMethod);
				addAllMethod.setLevel(2);
				Tab.closeScope();
			}
			
			//korekcija ord, chr i len
			((Obj)(Tab.find("ord").getLocalSymbols().toArray()[0])).setFpPos(1);
			((Obj)(Tab.find("chr").getLocalSymbols().toArray()[0])).setFpPos(1);
			((Obj)(Tab.find("len").getLocalSymbols().toArray()[0])).setFpPos(1);
			
			/*
				Semanticka analiza
			*/
			SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
			prog.traverseBottomUp(semanticAnalyzer);
			
			/*
				Ispis tabele simbola
			*/
			log.info("===================================");
			Tab.dump();
			
			//PROVERA USPEHA PARSIRANJA I SEMANTICKE ANALIZE
			if(!p.errorDetected && semanticAnalyzer.passed()) {
				
				/*
				 	GENERISANJE KODA
				*/
				
				File objFile = new File("test/program.obj");
				if(objFile.exists()) objFile.delete();
				
				CodeGenerator codeGenerator = new CodeGenerator();
				prog.traverseBottomUp(codeGenerator);
				
				Code.dataSize = semanticAnalyzer.nVars;
				Code.mainPc = codeGenerator.getMainPc();
				Code.write(new FileOutputStream(objFile));
				
				log.info("Parsiranje USPESNO ZAVRSENO!");
			}
			else {
				log.error("Parsiranje NEUSPESNO ZAVRSENO");
			}
			
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
	}
}