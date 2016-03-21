package miniJava.SyntacticAnalyzer;
import java.util.HashMap;


public class Token {
	public TokenKind kind;
	public String spelling;
	private HashMap<String, TokenKind> KeyWords = new HashMap<String, TokenKind>();
	public SourcePosition posn;
	
	
	
	public Token(TokenKind kind, String spelling) {
		
		KeyWords.put("class",TokenKind.CLASS);
		KeyWords.put("boolean",TokenKind.BOOLEAN);
		KeyWords.put("public",TokenKind.PUBLIC);
		KeyWords.put("private",TokenKind.PRIVATE);
		KeyWords.put("static",TokenKind.STATIC);
		KeyWords.put("int",TokenKind.INT);
		KeyWords.put("if",TokenKind.IF);
		KeyWords.put("while",TokenKind.WHILE);
		KeyWords.put("else",TokenKind.ELSE);
		KeyWords.put("this",TokenKind.THIS);
		KeyWords.put("new",TokenKind.NEW);
		KeyWords.put("return",TokenKind.RETURN);
		KeyWords.put("true",TokenKind.TRUE);
		KeyWords.put("false",TokenKind.FALSE);
		KeyWords.put("void",TokenKind.VOID);
		
		this.kind = kind;
		this.spelling = spelling;
		this.posn = null;
		
		if(this.kind == TokenKind.KEYWORD){
			this.kind = KeyWords.get(this.spelling);
		}
	}
}