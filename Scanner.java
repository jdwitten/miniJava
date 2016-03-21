package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import miniJava.ErrorReporter;


/*
 * This is the LL(1) grammar for the miniJava Scanner
 * 
 * EOT ::= eot
 * 
 * KEYWORD ::= class | boolean | public | private | static | int | if |
 * 			   | while | else | this | new
 * 
 * ID ::= letter (idSymbols)*
 * 
 * letter ::= a | b |... | z | A | ... | Z
 * 
 *  idSymbols :: = letter | digit | _ | 
 *  
 *  digit ::= 0 |...| 9
 *
 *  NUM ::= digit digit*
 * 
 * LBRACK ::= {
 * RBRACK ::= }
 * LPAREN ::= (
 * RPAREN ::= )
 * LBOX ::= [
 * RBOX ::= ]
 * 
 * 
 * 
 */
public class Scanner {
	
	
	private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private StringBuilder currentSpelling;
	private String testSpelling;
	
	private boolean lineComment = false;
	private boolean multiLineComment = false;
	private boolean comment = false;
	private boolean eot = false; 
	
	private Set<String> KeyWords = new HashSet<String>(Arrays.asList("class", "boolean", "public", "private", "static", "int", "if","while","else","this", "new","void","true","false","return"));
	private Set<Character> idSymbols = new HashSet<Character>(Arrays.asList('a','b','c','d','e','f','g','h','i','j',
																			'k','l', 'm', 'n', 'o','p', 'q','r', 's',
																			't', 'u','v','w','x','y','z', 
																			'A','B','C','D','E','F','G','H','I','J'
																			,'K','L','M','N','O','P','Q','R','S','T','U',
																			 'V','W','X','Y','Z','_','1','2','3','4','5','6','7','8','9','0'));
	private String[] binops = {"+","-","x","/","&&","||",">","<","==","<=",">=","!="};
	private String[] unops = {"-","!"};
	
	
	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;

		// initialize scanner state
		readChar();
	}
		/**
		 * skip whitespace and scan next token
		 * @return token
		 */
		public Token scan() {
			boolean skip = true;
			// skip whitespace and comments
			while (!eot && notAChar() )
				skipIt();

			// collect spelling and identify token kind
			currentSpelling = new StringBuilder();
			TokenKind kind = scanToken();
			
			while(kind == TokenKind.OPENCOMMENT || kind == TokenKind.COMMENT){
			if(kind == TokenKind.OPENCOMMENT){
				skip = false;
				while (kind != TokenKind.CLOSECOMMENT){
					if(kind == TokenKind.EOT) return new Token(TokenKind.ERROR, currentSpelling.toString());
					currentSpelling = new StringBuilder();
					kind = scanToken();
					while(kind==TokenKind.ERROR && (comment||lineComment)){
						skipIt();
						kind = scanToken();
					}
				}
			}	
			while(kind == TokenKind.COMMENT){
				skip = true;
				currentSpelling = new StringBuilder();
				kind = scanToken();
			}
			
			if(!skip&&!eot){	
				currentSpelling = new StringBuilder();
				kind = scanToken();
			}
			
			}
			// return new token
			return new Token(kind, currentSpelling.toString());
		}
		
		private boolean notAChar(){
			if(currentChar == eolUnix ||
				currentChar == eolWindows ||
				currentChar == tab ||
				currentChar == ' '){
				return true;
			}
			else if(lineComment || multiLineComment) return true;
			
			else return false;
		}
		
		public TokenKind scanToken() {
			
			if (eot)
				return(TokenKind.EOT); 
			
			while(notAChar()){
				skipIt();
			}

			// scan Token
			switch (currentChar) {
			case '+':   
				takeIt();
				return(TokenKind.BINOP);
				
			case '-':
				takeIt();
				if(currentChar == '-') return TokenKind.ERROR;
				return TokenKind.DUALOP;
			case '*':
				takeIt();
				if(currentChar == '/'){
					skipIt();
					comment = false;
					return TokenKind.CLOSECOMMENT;
				}
				else return TokenKind.BINOP;
				
			case '/':
				takeIt();
				if(currentChar=='*'){
					skipIt();
					comment = true;
					return TokenKind.OPENCOMMENT;
				}
				else if(currentChar == '/'){
					lineComment = true;
					skipIt();
					while(lineComment&&!eot){
						skipIt();
					}
					if(eot) return TokenKind.EOT;
					return TokenKind.COMMENT;
				}
				else return TokenKind.BINOP;
				
			case ';':
				takeIt();
				return TokenKind.SEMI;

			case '&':
				takeIt();
				if(currentChar == '&'){ takeIt(); return TokenKind.BINOP;}
				else{
					scanError("Expected a & but got a: " + currentChar);
					return TokenKind.ERROR;
				}
			case '|':
				takeIt();
				if(currentChar == '|'){ takeIt(); return TokenKind.BINOP;}
				else scanError("Expected a | but got a: " + currentChar);
			
			case '=':
				takeIt();
				if(currentChar == '='){ takeIt(); return TokenKind.BINOP;}
				else return TokenKind.EQUALS;
			
			case '>': case '<':
				takeIt();
				if(currentChar=='='){takeIt(); return TokenKind.BINOP;}
				else return TokenKind.BINOP;
			
			case '!':
				takeIt();
				if(currentChar=='='){takeIt(); return TokenKind.BINOP;}
				else return TokenKind.UNOP;
			case '(': 
				takeIt();
				return(TokenKind.LPAREN);

			case ')':
				takeIt();
				return(TokenKind.RPAREN);
			
			case '{':
				takeIt();
				return TokenKind.LBRACK;
			
			case '}':
				takeIt();
				return TokenKind.RBRACK;
				
			case '[':
				takeIt();
				return TokenKind.LBOX;
				
			case ']':
				takeIt();
				return TokenKind.RBOX;
				
			case '.':
				takeIt();
				return TokenKind.PERIOD;
			
			case ',':
				takeIt();
				return TokenKind.COMMA;
				
			
				
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				while (isDigit(currentChar))
					takeIt();
				return(TokenKind.NUM);
			
			case 'a': case'b': case'c':  case'd':  case'e':  case'f':  case'g':  case'h':  case'i': case'j':
			case 'k': case'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's':
			case 't': case 'u':case 'v': case 'w': case 'x': case 'y': case 'z': 
			case 'A': case'B': case'C':  case'D':  case'E':  case'F':  case'G':  case'H':  case'I': case'J':
			case 'K': case'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S':
			case 'T': case 'U':case 'V': case 'W': case 'X': case 'Y': case 'Z':
				while(isIdSymbol()){
					takeIt();
				}
				testSpelling = currentSpelling.toString();
				if(isKeyWord()){
					return TokenKind.KEYWORD;
				}
				else return TokenKind.ID;

			default:
				if(!lineComment && !comment){
					if(eot) return TokenKind.EOT;
					else{
						scanError("Unrecognized character '" + currentChar + "' in input");
						return(TokenKind.ERROR);
					}
				}
				
					
				else return(TokenKind.ERROR);
				
			}
		}
		
		
		private boolean isIdSymbol(){
			if(idSymbols.contains(currentChar)){
				return true;
			}
			else return false;
		}
		
		private boolean isKeyWord(){
			if(KeyWords.contains(testSpelling)){
				return true;
			}
			else return false;
		}
		
		private void takeIt() {
			currentSpelling.append(currentChar);
			nextChar();
		}

		private void skipIt() {
			nextChar();
		}

		private boolean isDigit(char c) {
			return (c >= '0') && (c <= '9');
		}

		private void scanError(String m) {
			reporter.reportError("Scan Error:  " + m);
		}


		private final static char eolUnix = '\n';
		private final static char eolWindows = '\r';
		private final static char tab = '\t';

		/**
		 * advance to next char in inputstream
		 * detect end of file or end of line as end of input
		 */
		private void nextChar() {
			if (!eot)
				readChar();
		}
		//currentChar == eolUnix
		private void readChar() {
			try {
				int c = inputStream.read();
				currentChar = (char) c;
				if (c == -1 ) {
					eot = true;
				}
				if(c == eolUnix || c == eolWindows){
					if(!lineComment) skipIt();
					else lineComment = false;
				}
				
				if(c == tab){
					skipIt();
				}
				
			} catch (IOException e) {
				scanError("I/O Exception!");
				eot = true;
			}
		}
}
