package no.uio.ifi.pascal2100.parser;

import java.util.ArrayList;
import java.util.HashMap;

import no.uio.ifi.pascal2100.main.CodeFile;
import no.uio.ifi.pascal2100.main.Main;
import no.uio.ifi.pascal2100.scanner.Scanner;
import no.uio.ifi.pascal2100.scanner.TokenKind;

public class Block extends PascalSyntax{

	StatmList statList;
	ConsDeclPart cdp;
	VarDeclPart vdp;
	TypeDeclPart tdp;
	FuncDecl fd;
	ProcDecl pd;
	Block outerScope;
	int blockLevel = 1;
	int localVarByte = 0;
	public HashMap<String, PascalDecl> decls;
	ArrayList<PascalSyntax> declListPrint = new ArrayList<>();
	ArrayList<ProcDecl> procList = new ArrayList<ProcDecl>();
	ArrayList<FuncDecl> funcList = new ArrayList<FuncDecl>();

	Block(int n) {
		super(n);
		decls = new HashMap<String, PascalDecl>();
	}

	static Block parse(Scanner s) {

		enterParser("block");
		Block b = new Block(s.curLineNum());
		if(s.curToken.kind == TokenKind.constToken){
			b.cdp = ConsDeclPart.parse(s);
			b.declListPrint.add(b.cdp);
		}
		if(s.curToken.kind == TokenKind.typeToken){
			b.tdp = TypeDeclPart.parse(s);
			b.declListPrint.add(b.tdp);
		}
		if(s.curToken.kind == TokenKind.varToken){
			b.vdp = VarDeclPart.parse(s);
			b.declListPrint.add(b.vdp);
		}
		while(s.curToken.kind == TokenKind.functionToken || s.curToken.kind == TokenKind.procedureToken){
			if(s.curToken.kind == TokenKind.functionToken){
				b.fd = FuncDecl.parse(s);
				b.funcList.add(b.fd);
				b.declListPrint.add(b.fd);
			}		
			if(s.curToken.kind == TokenKind.procedureToken){
				b.pd = ProcDecl.parse(s);
				b.procList.add(b.pd);
				b.declListPrint.add(b.pd);
			}
		}

		s.skip(TokenKind.beginToken);
		b.statList = StatmList.parse(s);
		s.skip(TokenKind.endToken);
		if(s.curToken.kind == TokenKind.semicolonToken)
			s.skip(TokenKind.semicolonToken);
		leaveParser("block");
		return b;
	}

	@Override
	public String identify() {
		return "<block> on line " + lineNum;
	}

	@Override
	void prettyPrint() {

		for(PascalSyntax p : declListPrint){
			p.prettyPrint();
		}
		Main.log.prettyPrintLn();
		Main.log.prettyPrintLn("begin");
		Main.log.prettyIndent();
		statList.prettyPrint();
		Main.log.prettyOutdent();
		Main.log.prettyPrint("end");
	}

	void addDecl(String id, PascalDecl pd){
		if(decls.containsKey(id))
			pd.error(id + " declared twice in same block!");
		decls.put(id, pd);
	}

	PascalDecl findDecl(String id, PascalSyntax where){
		PascalDecl pd = decls.get(id);
		if(pd != null){
			Main.log.noteBinding(id, where, pd);
			return pd;
		}
		if(outerScope != null){
			return outerScope.findDecl(id, where);
		}
		where.error("Name " + id + " is unknown!");
		return null;
	}

	@Override
	void check(Block curScope, Library lib) {	
		blockLevel = curScope.blockLevel + 1;
		outerScope = curScope; 
		for(PascalSyntax ps : declListPrint){
			if(ps instanceof ConsDeclPart){
				for(ConstDecl c : cdp.constList){
					addDecl(c.name, c);
				}
				ps.check(this, lib);
			}
			if(ps instanceof TypeDeclPart){
				for(TypeDecl t : tdp.typeList){
					addDecl(t.name, t);
				}
				ps.check(this, lib);
			}
			if(ps instanceof VarDeclPart){

				for(VarDecl v : vdp.varList){
					v.declLevel += curScope.blockLevel;
					addDecl(v.name, v);
				}
				ps.check(this, lib);
			}
			if(ps instanceof ProcDecl){
				if(ps instanceof FuncDecl){
					((FuncDecl) ps).declLevel += blockLevel;
					addDecl(((FuncDecl) ps).name, (FuncDecl) ps);
					ps.check(this, lib);

				}
				else{
					((ProcDecl) ps).declLevel += blockLevel;
					addDecl(((ProcDecl) ps).name, (ProcDecl) ps);
					ps.check(this, lib);

				}
			}
		}
		if(statList != null){
			statList.blockLevel += curScope.blockLevel;
			for(Statement s : statList.statList){
				s.check(this, lib);
			}
		}
	}

	@Override
	public void genCode(CodeFile f) {
		for(PascalSyntax ps : declListPrint){
			if(ps instanceof ProcDecl){
				if(ps instanceof FuncDecl){
					ps.genCode(f);
				}
				else{
					ps.genCode(f);
					//((ProcDecl)ps)
					
				}
			}
		}
		if((outerScope.blockLevel) == 1){
			f.genInstr("prog$" + Program.assName , "enter", "$"+ 
					((32+localVarByte))+",$1" , "Start of " + Program.nam);
		}
		
		if(statList != null){
			
			statList.genCode(f);
		}
	}
}

class ConsDeclPart extends PascalSyntax{

	ArrayList<ConstDecl> constList = new ArrayList<ConstDecl>();
	ConstDecl consDeclRef;
	Library lib;
	ConsDeclPart(int n) {
		super(n);
	}

	static ConsDeclPart parse(Scanner s) {

		enterParser("const decl part");
		ConsDeclPart cdp = new ConsDeclPart(s.curLineNum());
		s.skip(TokenKind.constToken);
		while(s.curToken.kind == TokenKind.nameToken){
			ConstDecl cd = ConstDecl.parse(s);
			cdp.constList.add(cd);
		}
		leaveParser("const decl part");
		return cdp;
	}

	@Override
	public String identify() {
		return "<const decl part> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrintLn("const ");
		for(ConstDecl c : constList){
			c.prettyPrint();
		}
	}

	@Override
	void check(Block curScope, Library lib) {

		for(ConstDecl c : constList){
			c.check(curScope, lib);
		}
	}

	@Override
	void genCode(CodeFile f) {
		for(ConstDecl c : constList){
			c.genCode(f);
		}

	}

}

class TypeDeclPart extends PascalSyntax{

	ArrayList<TypeDecl> typeList = new ArrayList<TypeDecl>();
	TypeDecl typeDeclRef;
	TypeDeclPart(int n) {
		super(n);
	}

	static TypeDeclPart parse(Scanner s) {

		enterParser("type decl part");
		TypeDeclPart tdp = new TypeDeclPart(s.curLineNum());
		s.skip(TokenKind.typeToken);
		while(s.curToken.kind == TokenKind.nameToken){
			TypeDecl td = TypeDecl.parse(s);
			tdp.typeList.add(td);
		}
		leaveParser("type decl part");
		return tdp;
	}

	@Override
	public String identify() {
		return "<type decl part> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrintLn("type");
		Main.log.prettyIndent();
		for(TypeDecl t : typeList){
			t.prettyPrint();
		}
		Main.log.prettyOutdent();
	}

	@Override
	void check(Block curScope, Library lib) {
		for(TypeDecl t : typeList){
			t.check(curScope, lib);
		}
	}

	@Override
	void genCode(CodeFile f) {
		for(TypeDecl t : typeList){
			t.genCode(f);
		}
	}

}

class VarDeclPart extends PascalSyntax{

	ArrayList<VarDecl> varList = new ArrayList<VarDecl>();
	VarDeclPart(int n) {
		super(n);
	}

	static VarDeclPart parse(Scanner s) {

		enterParser("var decl part");
		VarDeclPart vdp = new VarDeclPart(s.curLineNum());
		s.skip(TokenKind.varToken);
		while(s.curToken.kind == TokenKind.nameToken){
			VarDecl vd = VarDecl.parse(s);
			vdp.varList.add(vd);
		}
		leaveParser("var decl part");
		return vdp;
	}

	@Override
	public String identify() {
		return "<var decl part> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrintLn("var ");
		Main.log.prettyIndent();
		for(VarDecl v : varList){
			v.prettyPrint();
		}
		Main.log.prettyOutdent();
	}

	@Override
	void check(Block curScope, Library lib) {
		int j = -32;
		int byt = 0;
		for(VarDecl v : varList){
			v.check(curScope, lib);
			v.declOffset = j - 4;
			v.assemNameblockLevel = (-4*v.declLevel) + "(%ebp)";
			v.assemNameOffset = v.declOffset + "(%edx)";
			j = v.declOffset;
			byt +=4;
		}
		curScope.localVarByte = byt;
	}

	@Override
	void genCode(CodeFile f) {
		for(VarDecl v : varList){
			v.genCode(f);
		}
	}
}