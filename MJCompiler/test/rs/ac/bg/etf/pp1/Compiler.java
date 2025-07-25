package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

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
			
			//PROVERA USPEHA PARSIRANJA I SEMANTICKE ANALIZE
			if(!p.errorDetected) {
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
