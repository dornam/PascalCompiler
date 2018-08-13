package no.uio.ifi.pascal2100.parser;

import no.uio.ifi.pascal2100.main.CodeFile;
import no.uio.ifi.pascal2100.main.Main;
import no.uio.ifi.pascal2100.scanner.Scanner;
import no.uio.ifi.pascal2100.scanner.TokenKind;

public class Program extends PascalDecl{

	Block body;
	Library lib;
	public static String nam;
	public static String assName;
	Program(String id, int lNum) {
		super(id, lNum);
	}

	@Override
	public String identify() {
		return "<program>" + nam +  "on line " + lineNum;
	}

	@Override
	public void prettyPrint() {
		Main.log.prettyPrintLn();
		Main.log.prettyPrintLn("*************************");
		Main.log.prettyPrintLn();
		Main.log.prettyPrintLn("program " + nam + ";");
		body.prettyPrint();
		Main.log.prettyPrintLn(".");
	}

	public static Program parse(Scanner s){

		enterParser("program");
		Program p = new Program(s.curToken.id, s.curLineNum());
		s.skip(TokenKind.programToken);
		Program.nam = s.curToken.id;
		s.skip(TokenKind.nameToken);
		s.skip(TokenKind.semicolonToken);
		p.body = Block.parse(s);
		s.skip(TokenKind.dotToken);
		leaveParser("program");

		return p;
	}

	@Override
	void checkWhetherAssignable(PascalSyntax where) {

	}

	@Override
	void checkWhetherFunction(PascalSyntax where) {

	}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {

	}

	@Override
	void checkWhetherValue(PascalSyntax where) {


	}

	@Override
	public void check(Block curScope, Library lib) {
		
		if(body != null){
			//body.blockLevel ++;
			body.check(curScope, lib);
		}
	}

	@Override
	public	void genCode(CodeFile f) {
		assName = f.getLabel(nam);
		f.genInstr("", ".extern", "write_char", "");
		f.genInstr("", ".extern", "write_int", "");
		f.genInstr("", ".extern", "write_string", "");
		f.genInstr("", ".globl", "_main", "");
		f.genInstr("", ".globl", "main", "");
		f.genInstr("_main", "", "", "");
		f.genInstr("main", "call", "prog$" + assName , "Start program");
		f.genInstr("", "movl", "$0,%eax", "Set status 0 and");
		f.genInstr("", "ret", "", "terminate the program");
		if(body != null){
			body.genCode(f);
		}
		f.genInstr("", "leave", "", "End of " + nam);
		f.genInstr("", "ret", "", "");
	}
}