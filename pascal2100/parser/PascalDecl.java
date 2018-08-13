package no.uio.ifi.pascal2100.parser;

import no.uio.ifi.pascal2100.main.CodeFile;
import no.uio.ifi.pascal2100.main.Main;
import no.uio.ifi.pascal2100.scanner.Scanner;
import no.uio.ifi.pascal2100.scanner.TokenKind;

public abstract class PascalDecl extends PascalSyntax {
	static String name;
	String progProcFuncName;
	String assemNameblockLevel, assemNameOffset;
	int declLevel = 0, declOffset = 0;
	Type type = null;

	PascalDecl(String id, int lNum) {
		super(lNum);
		name = id;
	}

	static PascalDecl parse(Scanner s){

		PascalDecl pd = null;
		if(s.curToken.kind == TokenKind.nameToken && s.nextToken.kind == TokenKind.leftParToken){
			pd = ProcDecl.parse(s);
		}
		else if(s.curToken.kind == TokenKind.nameToken && s.nextToken.kind == TokenKind.equalToken){
			pd = ConstDecl.parse(s);
		}
		else if(s.curToken.kind == TokenKind.nameToken && s.nextToken.kind == TokenKind.colonToken){
			pd = VarDecl.parse(s);
		}
		else {}
		return pd;
	}

	/**
	 * checkWhetherAssignable: Utility method to check whether this PascalDecl is
	 * assignable, i.e., may be used to the left of a :=. 
	 * The compiler must check that a name is used properly;
	 * for instance, using a variable name a in "a()" is illegal.
	 * This is handled in the following way:
	 * <ul>
	 * <li> When a name a is found in a setting which implies that should be
	 *      assignable, the parser will first search for a's declaration d.
	 * <li> The parser will call d.checkWhetherAssignable(this).
	 * <li> Every sub-class of PascalDecl will implement a checkWhetherAssignable.
	 *      If the declaration is indeed assignable, checkWhetherAssignable will do
	 *      nothing, but if it is not, the method will give an error message.
	 * </ul>
	 * Examples
	 * <dl>
	 *  <dt>VarDecl.checkWhetherAssignable(...)</dt>
	 *  <dd>will do nothing, as everything is all right.</dd>
	 *  <dt>TypeDecl.checkWhetherAssignable(...)</dt>
	 *  <dd>will give an error message.</dd>
	 * </dl>
	 */
	/*Del 3:*/
	abstract void checkWhetherAssignable(PascalSyntax where);
	abstract void checkWhetherFunction(PascalSyntax where);
	abstract void checkWhetherProcedure(PascalSyntax where);
	abstract void checkWhetherValue(PascalSyntax where);

}


class ConstDecl extends PascalDecl{

	Constant constant;
	String name;
	ConstDecl(String id, int lNum) {
		super(id, lNum);
	}


	static ConstDecl parse(Scanner s) {

		enterParser("const decl");
		ConstDecl cd = new ConstDecl(s.curToken.id,s.curLineNum());
		cd.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		s.skip(TokenKind.equalToken);
		cd.constant = Constant.parse(s);
		s.skip(TokenKind.semicolonToken);
		leaveParser("const decl");
		return cd;
	}

	@Override
	public String identify() {
		if(isInLibrary())
			return "<const decl> in the Library";
		return "<const decl> on line " + lineNum;
	}

	@Override
	void prettyPrint() {

		Main.log.prettyIndent();
		Main.log.prettyPrint(name + " = ");
		constant.prettyPrint();
		Main.log.prettyPrintLn(";");
		Main.log.prettyOutdent();
	}


	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {
		where.error("Constant can not assign as a function!");
	}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {
		where.error("Constant can not assign as a procedure!");
	}

	@Override
	void checkWhetherValue(PascalSyntax where) {
		where.error("Constant can not assign as a value!");
	}

	@Override
	void check(Block curScope, Library lib) {
		constant.check(curScope, lib);
	}


	@Override
	public void genCode(CodeFile f) {
		
		if(constant instanceof NumberLiteral){
			int value = ((NumberLiteral) constant).valInt;
			f.genInstr("", "movl", "$" + value + ",%eax", "" + value);
		}
		if(constant instanceof CharLiteral){
			int ascii = (int) ((CharLiteral) constant).valChar.charAt(0);
			String charValue = ((CharLiteral) constant).valChar;
			f.genInstr("", "movl", "$" + ascii + ",%eax" , 
					charValue + " with ascii code " + ascii );
		}
		if(constant instanceof StringLiteral){
			int ascii = 0;
			for(int i = 0; i < ((StringLiteral) constant).valString.length(); i++){
				ascii += (int) ((StringLiteral) constant).valString.charAt(i);
			}
			String strValue = ((StringLiteral) constant).valString;
			f.genInstr("", "movl", "$" + ascii + ",%eax" , strValue);
		}
	}

}

class ParamDecl extends PascalDecl{
	TypeName tn;
	String name;
	ParamDecl paramDeclRef;
	ParamDecl(String id, int lNum) {
		super(id, lNum);
	}
	static ParamDecl parse(Scanner s){

		enterParser("param decl");
		ParamDecl pd = new ParamDecl(s.curToken.id, s.curLineNum());
		pd.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		s.skip(TokenKind.colonToken);
		pd.tn = TypeName.parse(s);
		leaveParser("param decl");
		return pd;
	}

	@Override
	public String identify() {
		return "<param decl> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(name + ":");
		tn.prettyPrint();
	}
	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {}

	@Override
	void checkWhetherValue(PascalSyntax where) {}

	@Override
	void check(Block curScope, Library lib) {
		
		tn.check(curScope, lib);
	}
	@Override
	void genCode(CodeFile f) {
		tn.genCode(f);
	}

}

class EnumLiteral extends PascalDecl{
	int ascii = -1;
	String value;
	EnumLiteral enumLitRef;
	EnumLiteral(String id, int lNum) {
		super(id, lNum);
		

	}

	@Override
	public String identify() {
		if(isInLibrary())
			return "<enum literal> in the Library";
		return "<enum literal> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(" " + value);
	}

	static EnumLiteral parse(Scanner s){

		enterParser("enum literal");
		EnumLiteral el = new EnumLiteral(s.curToken.id, s.curLineNum());
		el.value = s.curToken.strVal;
		s.skip(TokenKind.nameToken);
		leaveParser("enum literal");
		return el;

	}

	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {}

	@Override
	void checkWhetherValue(PascalSyntax where) {}

	@Override
	void check(Block curScope, Library lib) {}

	@Override
	void genCode(CodeFile f) {}

}

class TypeDecl extends PascalDecl{

	TypeName typeN;
	String name;
	TypeDecl typeDeclRef;
	TypeDecl(String id, int lNum) {
		super(id, lNum);

	}

	@Override
	public String identify() {
		if(isInLibrary())
			return "<type decl> in the Library";

		return "<type decl> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(name +" = ");
		type.prettyPrint();
		Main.log.prettyPrintLn(";");
	}

	static TypeDecl parse(Scanner s){

		enterParser("type decl");
		TypeDecl td = new TypeDecl(s.curToken.id, s.curLineNum());
		td.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		s.skip(TokenKind.equalToken);
		td.type = Type.parse(s);
		s.skip(TokenKind.semicolonToken);
		leaveParser("type decl");
		return td;
	}

	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {
		where.error("Type can not assign as a function!");
	}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {
		where.error("Type can not assign as a procedure!");
	}

	@Override
	void checkWhetherValue(PascalSyntax where) {
		where.error("Type can not assign as a value!");
	}

	@Override
	void check(Block curScope, Library lib) {
		type.check(curScope, lib);

	}

	@Override
	void genCode(CodeFile f) {
		type.genCode(f);

	}

}

class VarDecl extends PascalDecl{

	String name;
	VarDecl varDeclRef;
	VarDecl(String id, int lNum) {
		super(id, lNum);
		
	}

	@Override
	public String identify() {

		return "<var decl> on line " + lineNum;
	}

	
	@Override
	void prettyPrint() {
		Main.log.prettyPrint(name + " : ");
		type.prettyPrint();
		Main.log.prettyPrintLn(";");
	}

	static VarDecl parse(Scanner s){

		enterParser("var decl");
		VarDecl vd = new VarDecl(s.curToken.id, s.curLineNum());
		vd.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		s.skip(TokenKind.colonToken);
		vd.type = Type.parse(s);
		s.skip(TokenKind.semicolonToken);
		leaveParser("var decl");
		return vd;
	}

	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {
		where.error("Variable can not assign as a function!");

	}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {
		where.error("Variable can not assign as a procedure!");

	}

	@Override
	void checkWhetherValue(PascalSyntax where) {
		where.error("Variable can not assign as a value!");

	}

	@Override
	void check(Block curScope, Library lib) {
		type.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		type.genCode(f);
	}
}

class ProcDecl extends PascalDecl{

	String name;
	ParamDeclList pdl;
	Block block;

	ProcDecl(String id, int lNum) {
		super(id, lNum);
	}

	@Override
	public String identify() {
		if(isInLibrary())
			return "<proc decl> in the Library";

		return "<proc decl> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("procedure " + name);
		if (pdl != null){
			pdl.prettyPrint();
		}
		Main.log.prettyPrintLn(";");
		Main.log.prettyIndent();
		block.prettyPrint();
		Main.log.prettyPrint(";");
		Main.log.prettyOutdent();
		Main.log.prettyPrintLn();
		Main.log.prettyPrintLn();
	}
	static ProcDecl parse(Scanner s){

		enterParser("proc decl");
		ProcDecl pd = new ProcDecl(s.curToken.id, s.curLineNum());
		s.skip(TokenKind.procedureToken);
		pd.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		if(s.curToken.kind == TokenKind.leftParToken){
			while(s.curToken.kind != TokenKind.rightParToken){
				pd.pdl = ParamDeclList.parse(s);
			}
			s.skip(TokenKind.rightParToken);
		}
		s.skip(TokenKind.semicolonToken);
		pd.block = Block.parse(s);

		leaveParser("proc decl");
		return pd;
	}

	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {}

	@Override
	void checkWhetherValue(PascalSyntax where) {
		where.error("Procedure can not assign as a value!");

	}

	@Override
	void check(Block curScope, Library lib) {
		if(pdl != null){
			for(ParamDecl pd : pdl.paramList){
				pd.declLevel += block.blockLevel;
				block.addDecl(pd.name, pd);
			}
			pdl.check(curScope, lib);
		}
		block.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		progProcFuncName = f.getLabel(name);
		if(pdl == null){
			block.genCode(f);
			f.genInstr("", "leave", "", "End of procedure " + name);
			f.genInstr("", "ret", "", "");
		}else{
			f.genInstr("proc$" + progProcFuncName  , "", "", "");
			f.genInstr("", "enter", "$" + (32+ (block.localVarByte)) + ",$" + declLevel, 
					"Start of procedure " +name);
			if(pdl != null)
				pdl.genCode(f);
			block.genCode(f);
			f.genInstr("", "leave", "", "End of procedure " + name);
			f.genInstr("", "ret", "", "");
		}
	}
}

class FuncDecl extends ProcDecl{
	TypeName typeN;

	FuncDecl(String id, int lNum) {
		super(id, lNum);

	}

	@Override
	public String identify() {

		return "<func decl> on line " + lineNum;
	}

	static FuncDecl parse(Scanner s){

		enterParser("func decl");
		FuncDecl fd = new FuncDecl(s.curToken.id, s.curLineNum());
		s.skip(TokenKind.functionToken);
		fd.name = s.curToken.id;
		s.skip(TokenKind.nameToken);
		if(s.curToken.kind == TokenKind.leftParToken){
			while(s.curToken.kind != TokenKind.rightParToken){
				fd.pdl = ParamDeclList.parse(s);
			}
			s.skip(TokenKind.rightParToken);
		}
		s.skip(TokenKind.colonToken);
		fd.typeN = TypeName.parse(s); 
		s.skip(TokenKind.semicolonToken);
		fd.block = Block.parse(s);
		leaveParser("func decl");
		return fd;

	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("function " + name + " ");
		if (pdl != null) pdl.prettyPrint();
		Main.log.prettyPrint(": ");
		typeN.prettyPrint();
		Main.log.prettyPrintLn(";");
		Main.log.prettyIndent();
		block.prettyPrint();
		Main.log.prettyPrint(";");
		Main.log.prettyOutdent();
	}

	@Override
	void check(Block curScope, Library lib) {

		if(pdl != null){
			for(ParamDecl pd : pdl.paramList){
				pd.declLevel += block.blockLevel;
				block.addDecl(pd.name, pd);
			}
			pdl.check(curScope, lib);
		}
		typeN.check(curScope, lib);
		block.check(curScope, lib);
	}
	
	@Override
	void genCode(CodeFile f) {
		progProcFuncName = f.getLabel(name);
		if(pdl == null){
			block.genCode(f);
			f.genInstr("" , "movl", "-32(%ebp),%eax", "Fetch return value");
			f.genInstr("", "leave", "", "End of function " + name);
			f.genInstr("", "ret", "", "");
		}else{
			f.genInstr("func$" + progProcFuncName , "", "", "");
			f.genInstr("", "enter", "$" + (32+ block.localVarByte)+ ",$" + declLevel, 
					"Start of function " + name);	
			if(pdl != null)
				pdl.genCode(f);
			block.genCode(f);
			f.genInstr("" , "movl", "-32(%ebp),%eax", "Fetch return value");
			f.genInstr("", "leave", "", "End of function " + name);
			f.genInstr("", "ret", "", "");
		}
	}
	
	@Override
	void checkWhetherAssignable(PascalSyntax where) {}

	@Override
	void checkWhetherFunction(PascalSyntax where) {}

	@Override
	void checkWhetherProcedure(PascalSyntax where) {}

	@Override
	void checkWhetherValue(PascalSyntax where) {
		where.error("Function can not assign as a value!");

	}
}
