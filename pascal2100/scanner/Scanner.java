package no.uio.ifi.pascal2100.scanner;

import no.uio.ifi.pascal2100.main.*;
import static no.uio.ifi.pascal2100.scanner.TokenKind.*;
import java.io.*;


/**
 * This is the Scanner class, which along with the Token and TokenKind classes
 * analyzes the semantics part of our Pascal2100 compiler program.
 * 
 * @author Mehdi & Dorna @ IFI/UiO September 2015
 *
 */
public class Scanner {
	public Token curToken = null, nextToken = null; 

	private LineNumberReader sourceFile = null;
	private String sourceFileName, sourceLine = "";
	private int sourcePos = 0;
	private char curC, nextC;
	private boolean check = false;

	public Scanner(String fileName){
		sourceFileName = fileName;
		try {
			FileReader fr = new FileReader(fileName);
			sourceFile = new LineNumberReader(fr);
		} catch (FileNotFoundException e) {
			Main.error("Cannot read " + fileName + "!");
		}

		readNextToken();  readNextToken();
	}


	public String identify() {
		return "Scanner reading " + sourceFileName;
	}

	/**
	 * Current line number 
	 * @return Current line number
	 */
	public int curLineNum() {
		return curToken.lineNum;
	}

	/**
	 * Calls the error method from the Main class and generates the appropriate error message
	 * @param message
	 */
	private void error(String message) {
		Main.error("Scanner error on line " + curLineNum() + ": " + message);
	}

	/**
	 * This method takes the text inside  the .pas file character by character and analyzes
	 * them and set the token to their appropriate token kind.
	 */
	public void readNextToken() {
		// Del 1 her

		if(sourceLine.equals("") && sourceFile != null){
			Main.log.noteSourceLine(getFileLineNum(), sourceLine);
			readNextLine();
			readNextChar();
		}
		curToken = nextToken;
		if(nextToken != null){
			if(nextToken.kind != eofToken) nextToken = null;
		}
			
		while(nextToken == null){
			
			if(Character.isWhitespace(curC)){
				check = true;
				while(Character.isWhitespace(nextC)){
					if(sourcePos >= sourceLine.length()){
						readNextLine();
						if(sourceFile == null) 
							break;
					}
					else{
						readNextChar();
					}
				}
			}
			
			if(sourcePos >= sourceLine.length() && Character.isWhitespace(curC) && sourceFile != null){
				check = true;
				readNextLine();
				readNextChar();
			}

			readNextChar();
			if(curC == '/' && nextC == '*'){
				readNextChar();
				readNextChar();
				while(!(curC == '*' && nextC == '/')){
					readNextChar();
					if(sourcePos >= sourceLine.length() && Character.isWhitespace(curC)){
						if(sourceLine == "" && sourceFile == null){
							this.error(" No end for comment");
							break;
						}
						readNextLine();
						readNextChar();
					}
					if(curC == '/' && nextC == '*'){
						Main.error(getFileLineNum(), "Illegal character! New comment block not allowed inside another comment block!");
						break;
					}
				}
				if(Character.isWhitespace(curC)) check = true;
				readNextChar();
				readNextChar();
			}
			if(curC == '{'){
				readNextChar();
				while (nextC != '}') readNextChar();
				readNextChar();
				readNextChar();
				if(Character.isWhitespace(curC)) check = true;
			}
			if(isDigit(curC)){
				String s = "";
				s += curC;
				while(isDigit(nextC)){
					readNextChar();
					s += curC;
				}
				nextToken = new Token(Integer.parseInt(s), getFileLineNum());
				if(Character.isWhitespace(curC)) check = true;
			}
			else if(isLetterAZ(curC)){
				String s = "";
				s += curC;
				while(isLetterAZ(nextC) || isDigit(nextC) || nextC == '_'){
					readNextChar();
					s += curC;
				}
				//s = s.toLowerCase();
				nextToken = new Token(s, getFileLineNum());
				if(Character.isWhitespace(curC)) check = true;
			}
			else if(curC == '\'' ){
				String s ="";
					while(nextC != '\''){
						
						if(curC == ','){
							Main.error(getFileLineNum(), "Text string without end!");
							break;
						}
						readNextChar();
						s += curC;
					}
					readNextChar();
					nextToken = new Token(s, s, getFileLineNum());
					if(Character.isWhitespace(curC)) check = true;
				
			}
			else{
				switch (curC) {
				case '+': nextToken = new Token(addToken, getFileLineNum());
				break;
				case ':': if(nextC == '='){
					nextToken = new Token(assignToken, getFileLineNum());
					readNextChar();
				}
				else nextToken = new Token(colonToken, getFileLineNum());
				break;
				case ',': nextToken = new Token(commaToken, getFileLineNum());
				break;
				case '.': if(nextC == '.'){
					nextToken = new Token(rangeToken, getFileLineNum());
					readNextChar();
				}
				else nextToken = new Token(dotToken, getFileLineNum());
				break;
				case '=': nextToken = new Token(equalToken, getFileLineNum());
				break;
				case '>': if(nextC == '='){
					nextToken = new Token(greaterEqualToken, getFileLineNum());
					readNextChar();
				}
				else nextToken = new Token(greaterToken, getFileLineNum());
				break;
				case '[': nextToken = new Token(leftBracketToken, getFileLineNum());
				break;
				case '(': nextToken = new Token(leftParToken, getFileLineNum());
				break;
				case '<': if(nextC == '='){
					nextToken = new Token(lessEqualToken, getFileLineNum());
					readNextChar();
				}
				else if(nextC == '>'){
					nextToken = new Token(notEqualToken, getFileLineNum());
					readNextChar();
				}
				else nextToken = new Token(lessToken, getFileLineNum());
				break;

				case '*': nextToken = new Token(multiplyToken, getFileLineNum());
				break;
				case ']': nextToken = new Token(rightBracketToken, getFileLineNum());
				break;
				case ')': nextToken = new Token(rightParToken, getFileLineNum());
				break;
				case ';': nextToken = new Token(semicolonToken, getFileLineNum());
				break;
				case '-': nextToken = new Token(subtractToken, getFileLineNum());
				break;
				default: if(!Character.isWhitespace(curC)) Main.error(getFileLineNum(), "Illegal character: '" + curC + "'!");
				
				break;
				}
			}
		}
		Main.log.noteToken(nextToken);
	}

	/**
	 * Takes the line that has got from the readNextLine method and reads the line character by character
	 * 
	 */
	private void readNextChar(){
		if(isLetterAZ(nextC))
			nextC = Character.toLowerCase(nextC);
		curC = nextC;
		if(sourcePos < sourceLine.length()){
			nextC = sourceLine.charAt(sourcePos++);
			
		}
	}

	/**
	 * This method reads the source file line by line
	 */
	private void readNextLine() {
		if (sourceFile != null) {
			try {
				sourceLine = sourceFile.readLine();
				if (sourceLine == null) {
					nextToken = new Token(eofToken, getFileLineNum());
					sourceFile.close();  sourceFile = null;
					sourceLine = "";  
				} else {
					sourceLine += " ";
				}
				sourcePos = 0;
			} catch (IOException e) {
				Main.error("Scanner error: unspecified I/O error!");
			}
		}
		if (sourceFile != null) 
			Main.log.noteSourceLine(getFileLineNum(), sourceLine);
	}

	/**
	 * Gets the line number being read
	 * @return the current line number being read
	 */
	private int getFileLineNum() {
		return (sourceFile!=null ? sourceFile.getLineNumber() : 0);
	}

	// Character test utilities:
	/**
	 * Checks if the character is an alphabetic letter or not
	 * @param c is the input character
	 * @return 
	 */
	private boolean isLetterAZ(char c) {
		return 'A'<=c && c<='Z' || 'a'<=c && c<='z';
	}

	/**
	 * Checks if the character is a digit or not
	 * @param c is the input character
	 * @return
	 */
	private boolean isDigit(char c) {
		return '0'<=c && c<='9';
	}

	// Parser tests:

	public void test(TokenKind t) {
		if (curToken.kind != t)
			testError(t.toString());
	}

	public void testError(String message) {
		Main.error(curLineNum(), 
				"Expected a " + message +
				" but found a " + curToken.kind + "!");
	}

	public void skip(TokenKind t) {
		test(t);
		readNextToken();
		
	}
}