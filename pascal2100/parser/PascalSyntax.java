package no.uio.ifi.pascal2100.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import no.uio.ifi.pascal2100.main.*;
import no.uio.ifi.pascal2100.scanner.*;


public abstract class PascalSyntax {
	public int lineNum;
	PascalSyntax(int n) {
		lineNum = n;
	}

	boolean isInLibrary() {
		return lineNum < 0;
	}

	abstract void check(Block curScope, Library lib);
	abstract void genCode(CodeFile f);
	abstract public String identify();
	abstract void prettyPrint();

	void error(String message) {
		Main.error("Error at line " + lineNum + ": " + message);
	}

	static void enterParser(String nonTerm) {
		Main.log.enterParser(nonTerm);
	}

	static void leaveParser(String nonTerm) {
		Main.log.leaveParser(nonTerm);
	}

}



abstract class Factor extends PascalSyntax{
	//String name;
	Factor(int n) {
		super(n);
	}

	@Override
	public String identify() {

		return "<factor> on line " + lineNum;
	}

	static Factor parse(Scanner s){

		enterParser("factor");
		Factor f = null;
		switch (s.curToken.kind) {
		case leftParToken:
			f = InnerExpr.parse(s); break;
		case notToken:
			f = Negation.parse(s); break;
		case intValToken:
		case stringValToken:
			f = Constant.parse(s); break;
		case nameToken:
			switch (s.nextToken.kind) {
			case leftBracketToken:
				f = Variable.parse(s); break;
			case leftParToken:
				f = FuncCall.parse(s); break;
			default:
				f = Variable.parse(s); break;
			}break;

		default: f = Constant.parse(s);
		break;
		}
		leaveParser("factor");
		return f;
	}
	@Override
	void genCode(CodeFile f){}

}

abstract class Constant extends Factor{
	ConstDecl ref;
	Constant(int n) {
		super(n);
	}

	@Override
	public String identify() {

		return "<constant> on line " + lineNum;
	}

	static Constant parse(Scanner s){

		enterParser("constant");
		Constant c = null;
		switch (s.curToken.kind) {
		case intValToken:
			c = NumberLiteral.parse(s);  
			break;
		case stringValToken:
			if(s.curToken.strVal.length() == 1){
				c = CharLiteral.parse(s);  break;
			}else{
				c = StringLiteral.parse(s); break;
			}
		default:
			c = NamedConst.parse(s);
			break;
		}
		leaveParser("constant");
		return c;
	}

	@Override
	void check(Block curScope, Library lib) {

	}
	@Override void genCode(CodeFile f){}
	
}

class NumberLiteral extends Constant{

	int valInt;
	NumberLiteral(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<number literal> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(""+valInt);

	}
	static NumberLiteral parse(Scanner s){

		enterParser("number literal");
		NumberLiteral nl = new NumberLiteral(s.curLineNum());
		nl.valInt = s.curToken.intVal;
		s.skip(TokenKind.intValToken);
		leaveParser("number literal");
		return nl;
	}

	@Override
	void genCode(CodeFile f) {
		f.genInstr("", "movl", "$" + valInt + ",%eax", "" + valInt);
	}
}

class CharLiteral extends Constant{
	String valChar;
	CharLiteral(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<char literal> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("'" + valChar + "'");
	}

	static CharLiteral parse(Scanner s){

		enterParser("char literal");
		CharLiteral cl = new CharLiteral(s.curLineNum());
		cl.valChar = s.curToken.strVal;
		s.skip(TokenKind.stringValToken);
		leaveParser("char literal");
		return cl;
	}

	@Override
	void genCode(CodeFile f) {
		int ascii = (int) valChar.charAt(0);
		f.genInstr("", "movl", "$" + ascii + ",%eax" , valChar + " with ascii code " + ascii );

	}

}

class StringLiteral extends Constant{
	String valString;
	StringLiteral(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<string literal> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("'"+ valString + "'");
	}

	static StringLiteral parse(Scanner s){

		enterParser("string literal");
		StringLiteral sl = new StringLiteral(s.curLineNum());
		sl.valString = s.curToken.strVal;
		s.skip(TokenKind.stringValToken);
		leaveParser("string literal");
		return sl;
	}

	@Override
	void genCode(CodeFile f) {
		int ascii = 0;
		for(int i = 0; i < valString.length(); i++){
			ascii += (int) valString.charAt(i);
		}
		f.genInstr("", "movl", "$" + ascii + ",%eax" , valString);
	}

}

class NamedConst extends Constant{
	String nam;
	NamedConst(int n) {
		super(n);
	}

	@Override
	public String identify(){
		return "<name const> on line " + lineNum;
	}

	@Override
	void prettyPrint(){
		Main.log.prettyPrint(nam);
	}

	static NamedConst parse(Scanner s){

		enterParser("name const");
		NamedConst nc = new NamedConst(s.curLineNum());
		nc.nam = s.curToken.id;
		s.skip(TokenKind.nameToken);
		leaveParser("name const");
		return nc;
	}

	@Override
	void check(Block curScope, Library lib) {
		PascalDecl p = curScope.findDecl(nam, this);
		ref = (ConstDecl) p;

	}

	@Override
	void genCode(CodeFile f) {
		
	}
}

class InnerExpr extends Factor{
	Expression expr;
	InnerExpr(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<inner expr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("(");
		expr.prettyPrint();
		Main.log.prettyPrint(")");
	}

	static InnerExpr parse(Scanner s){

		enterParser("inner expr");
		InnerExpr ix = new InnerExpr(s.curLineNum());
		s.skip(TokenKind.leftParToken);
		while(s.curToken.kind != TokenKind.rightParToken){
			ix.expr = Expression.parse(s);
		}
		s.skip(TokenKind.rightParToken);
		leaveParser("inner expr");
		return ix;
	}

	@Override
	void check(Block curScope, Library lib) {
		expr.check(curScope, lib);

	}

	@Override
	void genCode(CodeFile f) {
		
		expr.genCode(f);
	}

}

class Negation extends Factor {
	Factor factor;
	Negation(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<negation> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(" not ");
		factor.prettyPrint();


	}

	static Negation parse(Scanner s){

		enterParser("negation");
		Negation neg = new Negation(s.curLineNum());
		s.skip(TokenKind.notToken);
		neg.factor =  Factor.parse(s);
		leaveParser("negation");
		return neg;
	}

	@Override
	void check(Block curScope, Library lib) {
		factor.check(curScope, lib);

	}

	@Override
	void genCode(CodeFile f) {
		factor.genCode(f);
		f.genInstr("", "xorl", "$1,%eax", " not");
		
	}

}

class FuncCall extends Factor{
	String nam;
	ExpressionList innerExprList;
	String assName;
	FuncDecl funcDeclRef;
	FuncCall(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<func call> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(nam);
		if(innerExprList != null){
			Main.log.prettyPrint(" (");
			innerExprList.prettyPrint();
			Main.log.prettyPrint(") ");
		}
	}

	static FuncCall parse(Scanner s){

		enterParser("func call");
		FuncCall fc = new FuncCall(s.curLineNum());
		fc.nam = s.curToken.id;
		s.skip(TokenKind.nameToken);
		if(s.curToken.kind == TokenKind.leftParToken){
			s.skip(TokenKind.leftParToken);
			while(s.curToken.kind != TokenKind.rightParToken){
				fc.innerExprList = ExpressionList.parse(s);
			}
			s.skip(TokenKind.rightParToken);
		}
		leaveParser("func call");
		return fc;
	}

	@Override
	void check(Block curScope, Library lib) {
		PascalDecl p = curScope.findDecl(nam, this);
		funcDeclRef = (FuncDecl) p;
		funcDeclRef.checkWhetherAssignable(this);
		funcDeclRef.checkWhetherFunction(this);
		if(innerExprList != null){
			for(Expression e : innerExprList.exprList){
				e.check(curScope, lib);
			}
		}
		

	}

	@Override
	void genCode(CodeFile f) {

		innerExprList.genCode(f);
		f.genInstr("", "call", "func$" + funcDeclRef.progProcFuncName, "function " + nam + " call");
		f.genInstr("", "addl", "$" + 4*innerExprList.exprList.size()+ ",%esp" , "Pop parameters");
	}

}

class Variable extends Factor{
	String nam;
	Expression expr;
	PascalDecl declRef;
	ArrayList<Expression> expList = new ArrayList<>();
	Variable(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<variable> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(nam + "");
		if (expr != null){
			Main.log.prettyPrint(" [");
			for(Expression e : expList){
				e.prettyPrint();
			}
			Main.log.prettyPrint("]");
		}

	}

	static Variable parse(Scanner s){

		enterParser("variable");
		Variable v = new Variable(s.curLineNum());
		v.nam = s.curToken.id;
		s.skip(TokenKind.nameToken);
		if(s.curToken.kind == TokenKind.leftBracketToken){
			s.skip(TokenKind.leftBracketToken);
			while(s.curToken.kind != TokenKind.rightBracketToken){
				v.expr = Expression.parse(s);
				v.expList.add(v.expr);
			}
			s.skip(TokenKind.rightBracketToken);
		}
		leaveParser("variable");
		return v;
	}

	@Override
	void check(Block curScope, Library lib) {
		 PascalDecl p = curScope.findDecl(nam, this);
		 declRef = p;
		if(expr != null)
			expr.check(curScope, lib);
		
	}

	@Override
	void genCode(CodeFile f) {
		if(declRef instanceof ConstDecl){
			((ConstDecl)declRef).genCode(f);
		}else if(declRef instanceof FuncDecl){
			f.genInstr("", "movl", "%eax,-32(%ebp)", nam +" :=");
		}else if(declRef instanceof TypeDecl){
			((TypeDecl)declRef).genCode(f);
		}
		else if(declRef instanceof EnumLiteral){
			((EnumLiteral)declRef).type.genCode(f);
		}
		else{
		f.genInstr("", "movl", declRef.assemNameblockLevel + ",%edx", "");
		f.genInstr("", "movl", declRef.assemNameOffset + ",%eax", "" + nam);
		if(expr != null)
			expr.genCode(f);
		}
	}

}

class ParamDeclList extends PascalSyntax{

	ArrayList<ParamDecl> paramList = new ArrayList<ParamDecl>();
	ParamDeclList(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<param decl list> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("(");
		ListIterator<ParamDecl> it = paramList.listIterator();
		while(it.hasNext()){
			it.next().prettyPrint();
			if(it.hasNext())
				Main.log.prettyPrint(" ; ");
		}
		Main.log.prettyPrint(")");
	}
	static ParamDeclList parse(Scanner s){

		enterParser("param decl list");
		ParamDeclList pdl = new ParamDeclList(s.curLineNum());
		s.skip(TokenKind.leftParToken);
		while(s.curToken.kind != TokenKind.rightParToken){
			ParamDecl pd = ParamDecl.parse(s);
			pdl.paramList.add(pd);
			if(s.curToken.kind == TokenKind.semicolonToken)
				s.skip(TokenKind.semicolonToken);
		}
		leaveParser("param decl list");
		return pdl;
	}

	@Override
	void check(Block curScope, Library lib) {

		for (ParamDecl p: paramList){
			p.declLevel = curScope.blockLevel;
			p.check(curScope, lib);
			
		}

	}

	@Override
	void genCode(CodeFile f) {
		int i = 8;
		for (ParamDecl p: paramList){
			p.declOffset = i;
			p.assemNameblockLevel = (-4*p.declLevel) +"(%ebp)";
			p.assemNameOffset = i + "(%edx)";
			i += 4;
			p.genCode(f);
			
		}

	}

}

class ExpressionList extends PascalSyntax{

	ArrayList<Expression> exprList = new ArrayList<>();

	ExpressionList(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return null;
	}

	@Override
	void prettyPrint() {
		ListIterator<Expression> it = exprList.listIterator();
		while(it.hasNext()){
			it.next().prettyPrint();
			if(it.hasNext())
				Main.log.prettyPrint(",");
		}
	}
	static ExpressionList parse(Scanner s){

		ExpressionList innerExprList = new ExpressionList(s.curLineNum());
		while(s.curToken.kind != TokenKind.rightParToken){
			Expression expr = Expression.parse(s);
			innerExprList.exprList.add(expr);
			if(s.curToken.kind == TokenKind.commaToken)
				s.skip(TokenKind.commaToken);
		}
		return innerExprList;
	}

	@Override
	void check(Block curScope, Library lib) {
		for(Expression e : exprList){
			e.check(curScope, lib);
		}

	}

	@Override
	void genCode(CodeFile f) {
		for(int i = exprList.size()-1; i >-1; i--){
			exprList.get(i).genCode(f);
			f.genInstr("", "pushl", "%eax", "Push param #" + (i+1));
		}
	}
}

class Expression extends PascalSyntax{
	SimpleExpr simexpLeft, simexpRight;
	RelOperator relopr;
	Type type;

	Expression(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<expression> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		simexpLeft.prettyPrint();
		if (relopr != null){
			relopr.prettyPrint();
			simexpRight.prettyPrint();
		}
	}

	static Expression parse(Scanner s){

		enterParser("expression");
		Expression exp = new Expression(s.curLineNum());
		exp.simexpLeft = SimpleExpr.parse(s);
		if(s.curToken.kind.isRelOpr()){
			exp.relopr = RelOperator.parse(s);
			exp.simexpRight = SimpleExpr.parse(s);
		}
		leaveParser("expression");
		return exp;
	}

	@Override
	void check(Block curScope, Library lib) {
		simexpLeft.check(curScope, lib);
		if(relopr != null){
			relopr.check(curScope, lib);
			simexpRight.check(curScope, lib);
		}

	}

	@Override
	void genCode(CodeFile f) {
		simexpLeft.genCode(f);
	
		if(relopr != null){
			f.genInstr("", "pushl", "%eax", "");
			simexpRight.genCode(f);
			relopr.genCode(f);
		}
	}

}

abstract class Operator extends PascalSyntax{

	TokenKind oprToken;
	Operator(int n) {
		super(n);
	}

	static Operator parse(Scanner s){

		Operator op = null;
		if(s.curToken.kind.isFactorOpr())  op = FactorOperator.parse(s);
		else if(s.curToken.kind.isPrefixOpr()) op = PrefixOperator.parse(s);
		else if(s.curToken.kind.isRelOpr())  op = RelOperator.parse(s);
		else if(s.curToken.kind.isTermOpr())  op = TermOperator.parse(s);
		return op;
	}

}

class FactorOperator extends Operator{

	FactorOperator(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<factor opr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		if (oprToken == TokenKind.multiplyToken) Main.log.prettyPrint(" * ");
		else if (oprToken == TokenKind.divToken) Main.log.prettyPrint(" div ");
		else if (oprToken == TokenKind.modToken) Main.log.prettyPrint(" mod ");
		else Main.log.prettyPrint(" and ");

	}
	static FactorOperator parse(Scanner s){

		enterParser("factor opr");
		FactorOperator fo = new FactorOperator(s.curLineNum());
		if(s.curToken.kind.isFactorOpr()){
			fo.oprToken = s.curToken.kind;
			s.skip(s.curToken.kind);
		}
		leaveParser("factor opr");
		return fo;
	}

	@Override
	void check(Block curScope, Library lib) {


	}

	@Override
	void genCode(CodeFile f) {
		f.genInstr("", "movl", "%eax,%ecx", "");
		f.genInstr("", "popl", "%eax", "");
		
		if (oprToken == TokenKind.multiplyToken) 
			f.genInstr("", "imull", "%ecx,%eax", " *");
		else if (oprToken == TokenKind.divToken){
			f.genInstr("", "cdq", "", "");
			f.genInstr("", "idivl", "%ecx", " / (div)");
		}
		else if (oprToken == TokenKind.modToken){
			f.genInstr("", "cdq", "", "");
			f.genInstr("", "idivl", "%ecx", "");
			f.genInstr("", "movl", "%edx,%eax", " mod");
		}	
		else{ 
			//f.genInstr("", "cdq", "", "");
			f.genInstr("", "andl", "%ecx,%eax", " and");
		}

	}

}

class PrefixOperator extends Operator{

	PrefixOperator(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<prefix opr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		if (oprToken == TokenKind.addToken) Main.log.prettyPrint("+");
		else Main.log.prettyPrint("-");

	}

	static PrefixOperator parse(Scanner s){

		enterParser("prefix opr");
		PrefixOperator po = new PrefixOperator(s.curLineNum());
		if(s.curToken.kind.isPrefixOpr()){
			po.oprToken = s.curToken.kind;
			s.skip(s.curToken.kind);
		}
		leaveParser("prefix opr");
		return po;
	}

	@Override
	void check(Block curScope, Library lib) {


	}

	@Override
	void genCode(CodeFile f) {
		if (oprToken == TokenKind.subtractToken) 
			f.genInstr("", "negl", "%eax", "Prefix -");
	}

}

class RelOperator extends Operator{

	RelOperator(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<rel opr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		String op = "?";
		switch (oprToken) {
		case equalToken:        op = " = ";  break;
		case notEqualToken:     op = " <> ";  break;
		case lessToken:         op = " < ";   break;
		case lessEqualToken:    op = " <= ";  break;
		case greaterToken:      op = " > ";   break;
		case greaterEqualToken: op = " >= ";  break;
		default:
			break;
		}
		Main.log.prettyPrint(op);

	}

	static RelOperator parse(Scanner s){

		enterParser("rel opr");
		RelOperator ro = new RelOperator(s.curLineNum());
		if(s.curToken.kind.isRelOpr()){
			ro.oprToken = s.curToken.kind;
			s.skip(s.curToken.kind);
		}
		leaveParser("rel opr");
		return ro;
	}

	@Override
	void check(Block curScope, Library lib) {
	}

	@Override
	void genCode(CodeFile f) {
		f.genInstr("", "popl", "%ecx", "");
		f.genInstr("", "cmpl", "%eax,%ecx", "");
		f.genInstr("", "movl", "$0,%eax", "");
		switch (oprToken) {
		case equalToken:
			f.genInstr("", "sete", "%al", "Test ="); break;
		case notEqualToken:
			f.genInstr("", "setne", "%al", "Test <>"); break;
		case lessToken:
			f.genInstr("", "setl", "%al", "Test <"); break;
		case lessEqualToken:
			f.genInstr("", "setle", "%al", "Test <="); break;
		case greaterToken:
			f.genInstr("", "setg", "%al", "Test >"); break;
		case greaterEqualToken:
			f.genInstr("", "setge", "%al", "Test >="); break;
		default:
			break;
		};
	}

}

class TermOperator extends Operator{

	TermOperator(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<term opr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		if (oprToken == TokenKind.addToken) Main.log.prettyPrint(" + ");
		else if (oprToken == TokenKind.subtractToken) Main.log.prettyPrint(" - ");
		else Main.log.prettyPrint(" or ");

	}

	static TermOperator parse(Scanner s){

		enterParser("term opr");
		TermOperator to = new TermOperator(s.curLineNum());
		if(s.curToken.kind.isTermOpr()){
			to.oprToken = s.curToken.kind;
			s.skip(s.curToken.kind);
		}
		leaveParser("term opr");
		return to;
	}

	@Override
	void check(Block curScope, Library lib) {


	}

	@Override
	void genCode(CodeFile f) {
		f.genInstr("", "movl", "%eax,%ecx", "");
		f.genInstr("", "popl", "%eax", "");
		if (oprToken == TokenKind.addToken) 
			f.genInstr("", "addl", "%ecx,%eax", " +");
		else if (oprToken == TokenKind.subtractToken) 
			f.genInstr("", "subl", "%ecx,%eax", " -");
		else 
			f.genInstr("", "orl", "%ecx,%eax", " or");

	}

}

class SimpleExpr extends PascalSyntax{

	PrefixOperator prefixOpr;
	TermOperator termOpr;
	Term term;
	ArrayList<Term> termList = new ArrayList<>();
	ArrayList<PascalSyntax> sexList = new ArrayList<>();
	ArrayList<TermOperator> termOprList = new ArrayList<>();

	SimpleExpr(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<simple expr> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		for(PascalSyntax p : sexList){
			p.prettyPrint();
		}
	}
	static SimpleExpr parse(Scanner s){

		enterParser("simple expr");
		SimpleExpr se = new SimpleExpr(s.curLineNum());
		if(s.curToken.kind.isPrefixOpr()){
			se.prefixOpr = PrefixOperator.parse(s);
			se.sexList.add(se.prefixOpr);
			if(s.curToken.kind.isTermOpr())
				Main.error("Expected term but found " + s.curToken.kind.toString());
		}
		se.term = Term.parse(s);
		se.termList.add(se.term);
		se.sexList.add(se.term);
		if(s.curToken.kind.isTermOpr()){
			while(s.curToken.kind.isTermOpr()){
				se.termOpr = TermOperator.parse(s);
				se.termOprList.add(se.termOpr);
				se.sexList.add(se.termOpr);
				se.term = Term.parse(s);
				se.termList.add(se.term);
				se.sexList.add(se.term);
			}
		}

		leaveParser("simple expr");
		return se;
	}

	@Override
	void check(Block curScope, Library lib) {

		for(Term t : termList){
			t.check(curScope, lib);
		}
	}

	@Override
	void genCode(CodeFile f) {
		Iterator<Term> it = termList.iterator();
		Iterator<TermOperator> ito = termOprList.iterator();
		while(it.hasNext()){
			it.next().genCode(f);
			if(prefixOpr != null)
				prefixOpr.genCode(f);
			if(termOprList != null){
				while(ito.hasNext()){
					f.genInstr("", "pushl", "%eax", "");
					it.next().genCode(f);
					ito.next().genCode(f);
				}
			}
		}
	}

}

class StatmList extends PascalSyntax{

	ArrayList<Statement> statList = new ArrayList<>();
	int blockLevel;
	StatmList(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<statm list> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		for(Statement s : statList){
			s.prettyPrint();
			if(s instanceof EmptyStatm)
				break;
			if(s != null)
				Main.log.prettyPrintLn(";");
		}

	}

	static StatmList parse(Scanner s){

		enterParser("statm list");
		StatmList sl = new StatmList(s.curLineNum());
		Statement st = Statement.parse(s);
		sl.statList.add(st);
		while(s.curToken.kind != TokenKind.endToken){	
			if(s.curToken.kind == TokenKind.semicolonToken){
				s.skip(TokenKind.semicolonToken);
			}
			Statement st1 = Statement.parse(s);
			sl.statList.add(st1);
		}
		leaveParser("statm list");
		return sl;
	}

	@Override
	void check(Block curScope, Library lib) {
		for(Statement s : statList){
			s.check(curScope, lib);
		}

	}

	@Override
	void genCode(CodeFile f) {
		for(Statement s : statList){
			s.genCode(f);
		}

	}

}

class Term extends PascalSyntax{
	Factor factor;
	FactorOperator factorOpr;
	ArrayList<Factor> factorList = new ArrayList<>();
	ArrayList<FactorOperator> facOprList = new ArrayList<>();
	ArrayList<PascalSyntax> termList = new ArrayList<>();

	Term(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<term> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		for(PascalSyntax p : termList){
			p.prettyPrint();
		}


	}

	static Term parse(Scanner s){

		enterParser("term");
		Term t = new Term(s.curLineNum());
		t.factor = Factor.parse(s);
		t.factorList.add(t.factor);
		t.termList.add(t.factor);
		if(s.curToken.kind.isFactorOpr()){
			while(s.curToken.kind.isFactorOpr()){
				t.factorOpr = FactorOperator.parse(s);
				t.facOprList.add(t.factorOpr);
				t.termList.add(t.factorOpr);
				t.factor = Factor.parse(s);
				t.factorList.add(t.factor);
				t.termList.add(t.factor);
			}
		}
		leaveParser("term");
		return t;
	}

	@Override
	void check(Block curScope, Library lib) {
		for(Factor f : factorList){
			f.check(curScope, lib);
		}

	}

	@Override
	void genCode(CodeFile f) {
		Iterator<Factor > itf = factorList.iterator();
		while(itf.hasNext()){
			itf.next().genCode(f);
			if(facOprList != null){
				Iterator<FactorOperator> it = facOprList.iterator();
				while(it.hasNext()){
					f.genInstr("", "pushl", "%eax", "");
					itf.next().genCode(f);
					it.next().genCode(f);
				}
			}
		}
	}
}

abstract class Statement extends PascalSyntax{
	Statement(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<statement> on line " + lineNum;
	}

	static Statement parse(Scanner s){

		enterParser("statement");
		Statement st = null;
		switch (s.curToken.kind) {
		case beginToken:
			st = CompoundStatm.parse(s);  break;
		case ifToken:
			st = IfStatm.parse(s); break;
		case nameToken:

			switch (s.nextToken.kind) {
			case assignToken:
			case leftBracketToken:
				st = AssignStatm.parse(s); break;
			default:
				st = ProcCallStatm.parse(s); break;
			}
			break;
		case whileToken:
			st = WhileStatm.parse(s); break;
		default:
			st = EmptyStatm.parse(s);  break;
		}
		leaveParser("statement");
		return st;
	}

}

class AssignStatm extends Statement{
	Variable var;
	Expression expr;
	AssignStatm(int n) {
		super(n);
	}

	@Override
	public String identify() {
		return "<assign statm> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		var.prettyPrint();
		Main.log.prettyPrint(" := ");
		expr.prettyPrint();
	}

	static AssignStatm parse(Scanner s){

		enterParser("assign statm");
		AssignStatm as = new AssignStatm(s.curLineNum());
		as.var = Variable.parse(s);
		s.skip(TokenKind.assignToken);
		as.expr = Expression.parse(s);
		leaveParser("assign statm");
		return as;
	}

	@Override
	void check(Block curScope, Library lib) {
		var.check(curScope, lib);
		expr.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		expr.genCode(f);
		if(var.declRef instanceof VarDecl){
			f.genInstr("", "movl", (-4*var.declRef.declLevel) + "(%ebp),%edx", "");
			f.genInstr("", "movl", "%eax,"+(var.declRef.declOffset) + "(%edx)",var.nam +  " :=");
		}
		else
			var.genCode(f);
	}

}

class CompoundStatm extends Statement{
	StatmList list;
	CompoundStatm(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<compound statm> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrintLn("begin");
		Main.log.prettyIndent();
		list.prettyPrint();
		Main.log.prettyOutdent();
		Main.log.prettyPrint("end");
	}

	static CompoundStatm parse(Scanner s){

		enterParser("compound statm");
		CompoundStatm cs = new CompoundStatm(s.curLineNum());
		s.skip(TokenKind.beginToken);
		cs.list = StatmList.parse(s);
		s.skip(TokenKind.endToken);
		if(s.curToken.kind == TokenKind.semicolonToken)
			s.skip(TokenKind.semicolonToken);
		leaveParser("compound statm");
		return cs;
	}

	@Override
	void check(Block curScope, Library lib) {
		list.check(curScope, lib);

	}

	@Override
	void genCode(CodeFile f) {
		list.genCode(f);
	}
}

class IfStatm extends Statement{

	Expression expr;
	ExpressionList expList;
	Statement stat;
	ElsePart elsePart;
	IfStatm(int n) {
		super(n);
	}

	@Override
	public String identify() {

		return "<if-statm> on line " + lineNum;
	}

	@Override
	void prettyPrint() {

		Main.log.prettyPrint("if ");
		expr.prettyPrint();
		Main.log.prettyPrintLn(" then ");
		Main.log.prettyIndent();
		stat.prettyPrint();
		Main.log.prettyOutdent();
		Main.log.prettyPrintLn();
		if (elsePart != null)
			elsePart.prettyPrint();

	}

	static IfStatm parse(Scanner s){

		enterParser("if-statm");
		IfStatm ifs = new IfStatm(s.curLineNum());
		s.skip(TokenKind.ifToken);
		ifs.expr = Expression.parse(s);
		s.skip(TokenKind.thenToken);
		ifs.stat = Statement.parse(s);
		if(s.curToken.kind == TokenKind.elseToken){

			ifs.elsePart = ElsePart.parse(s);
		}
		leaveParser("if-statm");
		return ifs;
	}

	@Override
	void check(Block curScope, Library lib) {
		expr.check(curScope, lib);
		stat.check(curScope, lib);
		if(elsePart != null)
			elsePart.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		if(elsePart != null){
			String testLable = f.getLocalLabel();
			String endLable = f.getLocalLabel();		
			f.genInstr("", "", "", "Start if-statement");
			expr.genCode(f);
			f.genInstr("", "cmpl", "$0,%eax", "");
			f.genInstr("", "je", testLable, "");
			stat.genCode(f);
			f.genInstr("", "jmp", endLable, "");
			f.genInstr(testLable, "", "", "");
			elsePart.stm.genCode(f);
			f.genInstr(endLable, "", "", "End if-statement");
		}
		else{
			String testLable = f.getLocalLabel();
			f.genInstr("", "", "", "Start if-statement");
			expr.genCode(f);
			f.genInstr("", "cmpl", "$0,%eax", "");
			f.genInstr("", "je", testLable, "");
			stat.genCode(f);
			f.genInstr(testLable, "", "", "End if-statement");
		}
	}
}

class ElsePart extends Statement{
	Statement stm;
	ElsePart(int n) {
		super(n);
	}

	@Override
	public String identify() {

		return "<else part> on line " + lineNum;
	}

	@Override
	void prettyPrint() {

		Main.log.prettyPrintLn("else");
		Main.log.prettyIndent();
		stm.prettyPrint();
		Main.log.prettyOutdent();
	}

	static ElsePart parse(Scanner s){

		ElsePart ep = new ElsePart(s.curLineNum());
		s.skip(TokenKind.elseToken);
		ep.stm = Statement.parse(s);
		return ep;

	}

	@Override
	void check(Block curScope, Library lib) {
		stm.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		String testLable = f.getLocalLabel(),
				endLable = f.getLocalLabel();
		f.genInstr("", "jmp", endLable, "");
		f.genInstr(testLable, "", "", "End if-statement");
		f.genInstr("", "", "", "Start of else");
		stm.genCode(f);
		f.genInstr(endLable, "", "", "End else-statement");
	}
}

class WhileStatm extends Statement{

	Expression expr;
	Statement statm;
	WhileStatm(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<while-statm> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint("while ");
		expr.prettyPrint();
		Main.log.prettyPrintLn(" do");
		Main.log.prettyIndent();
		statm.prettyPrint();
		Main.log.prettyOutdent();

	}

	static WhileStatm parse(Scanner s){

		enterParser("while-statm");
		WhileStatm ws = new WhileStatm(s.curLineNum());
		s.skip(TokenKind.whileToken);
		ws.expr = Expression.parse(s);
		s.skip(TokenKind.doToken);
		ws.statm = Statement.parse(s);
		leaveParser("while-statm");
		return ws;
	}

	@Override
	void check(Block curScope, Library lib) {
		expr.check(curScope, lib);
		Main.log.noteTypeCheck("while", expr.type, this);
		statm.check(curScope, lib);
	}

	@Override
	void genCode(CodeFile f) {
		String testLable = f.getLocalLabel(),
				endLable = f.getLocalLabel();
		f.genInstr(testLable, "", "", "Start while-statement");
		expr.genCode(f);
		f.genInstr("", "cmpl", "$0,%eax", "");
		f.genInstr("", "je", endLable, "");
		statm.genCode(f);
		f.genInstr("", "jmp", testLable, "");
		f.genInstr(endLable, "", "", "End while-statement");

	}

}

class ProcCallStatm extends Statement{

	String nam;
	ExpressionList exprList;
	String assName;
	ProcDecl procRef;
	ProcCallStatm(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<proc call> on line " + lineNum;
	}

	@Override
	void prettyPrint() {
		Main.log.prettyPrint(nam);
		if(exprList != null){
			Main.log.prettyPrint(" (");
			exprList.prettyPrint();
			Main.log.prettyPrint(")");
		}
		
	}

	static ProcCallStatm parse(Scanner s){

		enterParser("proc call");
		ProcCallStatm pcs = new ProcCallStatm(s.curLineNum());
		pcs.nam = s.curToken.id;
		s.skip(TokenKind.nameToken);
		if(s.curToken.kind == TokenKind.leftParToken){
			s.skip(TokenKind.leftParToken);
			while(s.curToken.kind != TokenKind.rightParToken){
				pcs.exprList = ExpressionList.parse(s);
			}
			s.skip(TokenKind.rightParToken);
		}
		leaveParser("proc call");
		return pcs;
	}

	@Override
	void check(Block curScope, Library lib) {

		PascalDecl pd = curScope.findDecl(nam, this);
		procRef = (ProcDecl)pd;
		procRef.checkWhetherAssignable(this);
		procRef.checkWhetherProcedure(this);
		if(exprList != null){
			for(Expression ex : exprList.exprList){
				ex.check(curScope, lib);
			}
		}
		
	}

	@Override
	void genCode(CodeFile f) {
		if(nam.equals("write")){
			int i = 1;
			for(Expression ex : exprList.exprList){
				Factor ff = ex.simexpLeft.term.factor;
				if(ff instanceof Variable){
					if(((Variable) ff).nam.equals("eol")){
						ex.genCode(f);
						f.genInstr("", "movl", "$10,%eax", "eol with ascii 10");
						f.genInstr("", "pushl", "%eax", "Push param #" + i);
						f.genInstr("", "call", "write_char", "");
						f.genInstr("", "addl", "$4,%esp", "Pop parameter");
					}else{
					ex.genCode(f);
					f.genInstr("", "pushl", "%eax", "Push param #" + i);
					f.genInstr("", "call", "write_int", "");
					f.genInstr("", "addl", "$4,%esp", "Pop parameter");
					}
				}
				else if(ff instanceof CharLiteral){
					ex.genCode(f);
					f.genInstr("", "pushl", "%eax", "Push param #" + i);
					f.genInstr("", "call", "write_char", "");
					f.genInstr("", "addl", "$4,%esp", "Pop parameter");
				}
				else if(ff instanceof StringLiteral){
					String s = f.getLocalLabel();
					f.genInstr("", ".data", "", "");
					f.genInstr(s, ".asciz", "\"" +((StringLiteral) ff).valString + "\"", "");
					f.genInstr("", ".align", "2", "");
					f.genInstr("", ".text", "", "");
					f.genInstr("", "leal", s+ ",%eax", "Addr(\" " 
							+((StringLiteral) ff).valString +"\")");
					f.genInstr("", "pushl", "%eax", "Push param #" + i);
					f.genInstr("", "call", "write_string", "");
					f.genInstr("", "addl", "$4,%esp", "Pop parameter");
				}
				else{
					ex.genCode(f);
					f.genInstr("", "pushl", "%eax", "Push param #" + i);
					f.genInstr("", "call", "write_int", "");
					f.genInstr("", "addl", "$4,%esp", "Pop parameter");
				}
				i++;	
			}
			
		}else{
			int capacity = 0;
			assName = procRef.progProcFuncName;
			if(exprList != null){
				exprList.genCode(f);
				capacity = 4*exprList.exprList.size();
				f.genInstr("", "call", "proc$" + assName, "procedure " + nam + " call");
				f.genInstr("", "addl", "$" + capacity + ",%esp" , "");
		}
			else{
				f.genInstr("", "call", "proc$" + assName, "");
			}
			
		
		}
	}
}

class EmptyStatm extends Statement{

	EmptyStatm(int n) {
		super(n);

	}

	@Override
	public String identify() {

		return "<empty statm> on line " + lineNum;
	}

	@Override
	void prettyPrint() {

	}

	static EmptyStatm parse(Scanner s){

		enterParser("empty statm");
		EmptyStatm es = new EmptyStatm(s.curLineNum());
		leaveParser("empty statm");
		return es;
	}

	@Override
	void check(Block curScope, Library lib) {}

	@Override
	void genCode(CodeFile f) {}
}