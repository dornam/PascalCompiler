package no.uio.ifi.pascal2100.parser;

import java.util.ArrayList;

import no.uio.ifi.pascal2100.main.CodeFile;
import no.uio.ifi.pascal2100.main.Main;
import no.uio.ifi.pascal2100.scanner.Scanner;
import no.uio.ifi.pascal2100.scanner.TokenKind;

public abstract class Type extends PascalSyntax{
	EnumLiteral enl;
	TypeDecl typeRef;
	Type type;
	boolean isArray = false;
	boolean isName = false;
	boolean isEnume = false;
	boolean isRange = false;

	Type(int n) {
		super(n);
	}
	abstract void checkType(Type tx, PascalSyntax where, String message);

	static Type parse(Scanner s){

		enterParser("type");
		Type t = null;
		switch (s.curToken.kind) {
		case nameToken:
			if (s.nextToken.kind != TokenKind.rangeToken){
				t = TypeName.parse(s); 
				t.isName = true;
			}
			break;
		case arrayToken: 
			t = ArrayType.parse(s); t.isArray = true; break;
		case leftParToken:
			t = EnumType.parse(s); t.isEnume = true; break;

		default:
			t = RangeType.parse(s); t.isRange = true;
			break;
		}
		leaveParser("type");

		return t;
	}
}

class ArrayType extends Type{

	//Type type;
	ArrayList<Type> arrayTypeList = new ArrayList<>();
	ArrayType(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<array-type> on line " + lineNum;
	}

	@Override
	public void prettyPrint() {
		Main.log.prettyPrint("array ");
		Main.log.prettyPrint("[");
		for(Type t : arrayTypeList){
			t.prettyPrint();
		}
		Main.log.prettyPrint("]");
		Main.log.prettyPrint("  of  ");
		type.prettyPrint();

	}
	static ArrayType parse(Scanner s){

		enterParser("array-type");
		ArrayType at = new ArrayType(s.curLineNum());
		s.skip(TokenKind.arrayToken);
		s.skip(TokenKind.leftBracketToken);
		while(s.curToken.kind != TokenKind.rightBracketToken){
			at.type = Type.parse(s);
			at.arrayTypeList.add(at.type);
		}
		s.skip(TokenKind.rightBracketToken);
		s.skip(TokenKind.ofToken);
		at.type = Type.parse(s);
		leaveParser("array-type");

		return (ArrayType) at;
	}

	@Override
	void check(Block curScope, Library lib) {
		for(Type t : arrayTypeList){
			t.check(curScope, lib);
		}
		type.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
/*		int low = 0;
		int up = 0;
		for(Type t : arrayTypeList){
		t.genCode(f);
		
		Type tmp = t;
		if(tmp instanceof RangeType){
			Constant c =((RangeType) tmp).constant.get(0);
			if(c instanceof NumberLiteral)
				low = ((NumberLiteral) c).valInt;
		}
		}
		f.genInstr("", "subl", "$" + low + ",%eax", "");*/
		

	}

	@Override
	void checkType(Type tx, PascalSyntax where, String message) {
		if(tx == this)
			return;
		else
			where.error(message);
	}

}

class EnumType extends Type{
	int ascii = -1;
	ArrayList<EnumLiteral> enumLitList = new ArrayList<EnumLiteral>();
	EnumType(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<enum-type> on line " + lineNum;
	}

	@Override
	public void prettyPrint() {
		Main.log.prettyPrint(" (");
		for(EnumLiteral e : enumLitList){
			e.prettyPrint();
			Main.log.prettyPrint(" , ");
		}
		Main.log.prettyPrintLn(")");

	}

	static EnumType parse(Scanner s){

		enterParser("enum-type");
		EnumType ent = new EnumType(s.curLineNum());
		s.skip(TokenKind.leftParToken);
		while(s.curToken.kind != TokenKind.rightParToken){
			EnumLiteral el = EnumLiteral.parse(s);
			ent.enumLitList.add(el);
			if(s.curToken.kind == TokenKind.commaToken)
				s.skip(TokenKind.commaToken);
		}
		s.skip(TokenKind.rightParToken);
		leaveParser("eunm-type");
		return ent;

	}

	@Override
	void check(Block curScope, Library lib) {
		for(EnumLiteral e : enumLitList){
			e.check(curScope, lib);
		}

	}

	@Override
	void genCode(CodeFile f) {
		for(EnumLiteral e : enumLitList){
			if(e.value.equals("false"))
				f.genInstr("", "movl", "$0,%eax", "  enum value " + e.value 
						+ " (= 0)");
			else if(e.value.equals("true"))
				f.genInstr("", "movl", "$1,%eax", "  enum value " + e.value 
						+ " (= 1)");
			else{
				ascii = ascii + 1;
				f.genInstr("", "movl", "$" + ascii + ",%eax", "  enum value " + e.value 
					+ " (=" + ascii +")");
			}
				
			
			
		}

	}

	@Override
	void checkType(Type tx, PascalSyntax where, String message) {
		if(tx == this)
			return;
		else if(tx instanceof TypeName)
			checkType(((TypeName)tx).type, where, message);
		else
			where.error(message);
	}

}

class RangeType extends Type{

	ArrayList<Constant> constant = new ArrayList<>();
	RangeType(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<range-type> on line " + lineNum;
	}

	@Override
	public void prettyPrint() {

		constant.get(0).prettyPrint();
		Main.log.prettyPrint(" .. ");
		constant.get(1).prettyPrint();
	}

	static RangeType parse(Scanner s){

		enterParser("range-type");
		RangeType rt = new RangeType(s.curLineNum());
		rt.constant.add(Constant.parse(s));
		s.skip(TokenKind.rangeToken);
		rt.constant.add(Constant.parse(s));
		leaveParser("range-type");
		return rt;
	}

	@Override
	void check(Block curScope, Library lib) {
		for(Constant c : constant)
			c.check(curScope, lib);

	}

	@Override
	void genCode(CodeFile f) {
		for(Constant c : constant){
			c.genCode(f);
		}

	}

	@Override
	void checkType(Type tx, PascalSyntax where, String message) {
		if(tx == this)
			return;
		else
			where.error(message);
	}

}

class TypeName extends Type{
	String value;

	TypeName(int n) {
		super(n);
	}

	static TypeName parse(Scanner s) {

		enterParser("type name");
		TypeName n = new TypeName(s.curLineNum());
		n.value = s.curToken.id;
		s.skip(TokenKind.nameToken);
		leaveParser("type name");
		return n;
	}

	@Override
	public String identify() {
		return "<name> on line " + lineNum;
	}

	@Override
	public void prettyPrint() {
		Main.log.prettyPrint(" " + value + "");
	}

	@Override
	void check(Block curScope, Library lib) {
		PascalDecl pd = curScope.findDecl(value, this);
		typeRef = (TypeDecl) pd;

	}

	@Override
	void genCode(CodeFile f) {
		// No need at the moment
	}

	@Override
	void checkType(Type tx, PascalSyntax where, String message) {
		if(tx == this)
			return;
		else
			where.error(message);
	}
}