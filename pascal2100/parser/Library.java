package no.uio.ifi.pascal2100.parser;

import no.uio.ifi.pascal2100.main.CodeFile;

public class Library extends Block{
	ProcDecl writeDecl;
	ConstDecl eolDecl;
	TypeDecl integerType;
	TypeDecl charType;
	TypeDecl booleanType;
	EnumLiteral trueType;
	EnumLiteral falseType;

	public Library(int n) {
		super(n);
		writeDecl = new ProcDecl("write",n);
		eolDecl = new ConstDecl("eol", n);
		integerType = new TypeDecl("integer", n);
		charType = new TypeDecl("char", n);
		booleanType = new TypeDecl("boolean", n);
		
		trueType = new EnumLiteral("true",n);
		trueType.value = "true";
		trueType.type = new EnumType(n);
		EnumType trueEnum = (EnumType) trueType.type;
		trueEnum.enumLitList.add(trueType);
		
		falseType = new EnumLiteral("false",n);
		falseType.value = "false";
		falseType.type = new EnumType(n);
		EnumType falseEnum = (EnumType) falseType.type;
		falseEnum.enumLitList.add(falseType);
		
		addDecl("write", writeDecl);
		addDecl("eol", eolDecl);
		addDecl("integer", integerType);
		addDecl("char", charType);
		addDecl("boolean", booleanType );
		addDecl("true", trueType);
		addDecl("false", falseType);

	}
	@Override
	public String identify() {
		return " in the library";
	}
	
	@Override
	public void genCode(CodeFile f){

	}
}