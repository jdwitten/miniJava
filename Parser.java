package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

/*
 * Here is my LL(1) version of the miniJava Grammar
 * 
 * Program ::= (ClassDeclaration)*eot
 * 
 * ClassDeclaration ::= class id { (GeneralDeclaration)* }
 * 
 * GeneralDeclaration ::= Visibility Access (void id (MethodDeclaration) | (type (id MethodDeclaration | ;))
 * 
 * MethodDeclaration ::= (ParameterList?){Statement*}
 * 
 * Visibility ::= (public | private)?
 * 
 * Access ::= static?
 * 
 * Type ::= (int|id)[]? | boolean
 * 
 * ParameterList ::= Type id ( , Type id )*
 * 
 * ArgumentList ::= Expression(,Expression)*
 * 
 * Reference ::= (.id)*
 * 
 * Statement ::=
		{ Statement* }
		| id (id | Reference ((ArgumentList?)?) | [Expression?]) = Expression ;
		| OtherStatementTypes id = Expression ; 
		| this Reference (= Expression ; | (ArgumentList?) ; )
		| if ( Expression ) Statement (else Statement)? 
		| while ( Expression ) Statement

OtherStatementTypes ::= boolean | int[]?

Expression ::=
	 id (Reference ( (ArgumentList?)? ) | [ Expression ] | Expression binop Expression )
	| unop Expression
	| ( Expression )
	| num | true | false
	| new (id ( () | [Expression]) | int[Expression]) 
	

 */




public class Parser {
	
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token currentToken;
	private boolean trace = true;
	
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	
	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;	
	}
	
	/**
	 * start parse
	 */
	public Package parse() {
		currentToken = scanner.scan();
		try {
			return parseProgram();
		}
		catch (SyntaxError e) { }
		return null;
	}
	
	
	
	
	//A parse procedure for the miniJava grammar
	
	
	
	//Program ::= (ClassDeclaration)*eot
	private Package parseProgram() throws SyntaxError{
		ClassDeclList cdl = new ClassDeclList();
		while(currentToken.kind==TokenKind.CLASS){
			ClassDecl cd = parseClassDeclaration();
			cdl.add(cd);
		}
		while(currentToken.kind == TokenKind.COMMENT){
			acceptIt();
		}
		accept(TokenKind.EOT);
		return new Package(cdl,null);
	}
	
	
	//ClassDeclaration ::= class id { (GeneralDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError{
		accept(TokenKind.CLASS);
		String cn = currentToken.spelling;
		accept(TokenKind.ID);
		accept(TokenKind.LBRACK);
		
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		
		//(GeneralDeclaration)*
		//Check if current Token is in Starters(GeneralDeclaration)
		while(currentToken.kind == TokenKind.PUBLIC ||
				currentToken.kind == TokenKind.STATIC||
				currentToken.kind == TokenKind.PRIVATE||
				currentToken.kind == TokenKind.INT ||
				currentToken.kind == TokenKind.BOOLEAN ||
				currentToken.kind == TokenKind.ID ||
				currentToken.kind == TokenKind.VOID){
			MemberDecl newDecl = parseGeneralDeclaration();
			if(newDecl.getClass() == FieldDecl.class) fields.add( (FieldDecl)newDecl);
			else if(newDecl.getClass() == MethodDecl.class) methods.add( (MethodDecl)newDecl);
			else parseError("Did not recieve a method or field class");
			}
		accept(TokenKind.RBRACK);
		return new ClassDecl(cn, fields, methods, null);
	}
	
	//GeneralDeclaration ::= Visibility Access (void id (MethodDeclaration) | (type (id MethodDeclaration | ;))
	private MemberDecl parseGeneralDeclaration() throws SyntaxError{
		boolean visibility=false;
		boolean access = false;
		//Visibility
		if(currentToken.kind == TokenKind.PUBLIC ||
			currentToken.kind == TokenKind.PRIVATE){
				visibility = parseVisibility();
			}
		//Access
		if(currentToken.kind==TokenKind.STATIC){
			access = parseAccess();
		}
		
		//( void id (MethodDeclaration) | (type id (MethodDeclaration | ; ) )
		switch(currentToken.kind){
		
			//void id (MethodDeclaration)
			case VOID:
				acceptIt();
				String mn = currentToken.spelling;
				accept(TokenKind.ID);
				FieldDecl member = new FieldDecl(visibility, access, new BaseType(TypeKind.VOID, null), mn, null);
				MethodDecl method = parseMethodDeclaration(member);
				return method;
			
			//(type id (MethodDeclaration | ; ))
			case INT: case ID: case BOOLEAN:
				Type t = parseType();
				String name = currentToken.spelling;
				accept(TokenKind.ID);
				member = new FieldDecl(visibility, access, t, name, null);
				// MethodDeclaration | ;
				switch(currentToken.kind){
				case LPAREN:
					method = parseMethodDeclaration(member);
					return method;
				case SEMI:
					acceptIt();
					return member;
				default:
					parseError("Was expecting LPAREN or SEMI but got: " + currentToken.kind);
				}
				break;
			default:
				parseError("Was expecting VOID, INT, ID, or BOOLEAN but got: " + currentToken.kind);
		}
		return null;
	}
	
	//MethodDeclaration ::= (ParameterList?){Statement*}
	private MethodDecl parseMethodDeclaration(MemberDecl member) throws SyntaxError{
		// (ParameterList?)
		accept(TokenKind.LPAREN);
		ParameterDeclList parameterList = new ParameterDeclList();
		switch(currentToken.kind){
		case INT: case BOOLEAN: case ID:
			parameterList = parseParameterList();
			break;
		default:
		}
		accept(TokenKind.RPAREN);
		
		//{Statement*}
		accept(TokenKind.LBRACK);
		//Check if currentToken is in Starters(Statement)
		StatementList statementList = new StatementList();
		while(currentToken.kind==TokenKind.LBRACK ||
				currentToken.kind == TokenKind.INT ||
				currentToken.kind== TokenKind.BOOLEAN||
				currentToken.kind==TokenKind.ID ||
				currentToken.kind==TokenKind.THIS ||
				currentToken.kind == TokenKind.RETURN ||
				currentToken.kind== TokenKind.IF ||
				currentToken.kind == TokenKind.WHILE){
			
			Statement statement = parseStatement();
			statementList.add(statement);
		}
		accept(TokenKind.RBRACK);
		return new MethodDecl(member, parameterList, statementList, null);
	}
	
	
	//Visibility ::= (public | private)?
	private boolean parseVisibility(){
		switch(currentToken.kind){
		case PUBLIC: 
			acceptIt();
			return false;
		case PRIVATE:
			acceptIt();
			return true;
		default:
			parseError("Expected a PUBLIC or PRIVATE but got:" + currentToken.kind);
			return false;
		}
	}
	
	//Access ::= static?
	private boolean parseAccess(){
		switch(currentToken.kind){
		case STATIC:
			acceptIt();
			return true;
		default:
			return false;
		}
	}
	
	// Type ::= (int|id)[]? | boolean
	private Type parseType() throws SyntaxError{
		Type t;
		//(int|id)[]? | boolean
		switch(currentToken.kind){
		
		//int[]?
		case INT:
			acceptIt();
			t = new BaseType(TypeKind.INT,null);
			if(currentToken.kind ==  TokenKind.LBOX){
				acceptIt();
				accept(TokenKind.RBOX);
				t = new ArrayType((Type)new BaseType(TypeKind.INT,null),null);
			}
			return t;
		
		//id[]?
		case ID:
			Token token = currentToken;
			acceptIt();
			t = new ClassType(new Identifier(token),null);
			if(currentToken.kind ==  TokenKind.LBOX){
				acceptIt();
				accept(TokenKind.RBOX);
				t = new ArrayType(new ClassType(new Identifier(token),null),null);
			}
			return t;
			
		// boolean
		case BOOLEAN:
			acceptIt();
			t = new BaseType(TypeKind.BOOLEAN, null);
			return t;
		default:
			parseError("Expected an int or id but found a: " + currentToken.kind);
			return null;
		}
	}
	
	//ParameterList ::= Type id ( , Type id )*
	private ParameterDeclList parseParameterList(){
		ParameterDeclList list = new ParameterDeclList();
		Type t = parseType();
		String typeName = currentToken.spelling;
		accept(TokenKind.ID);
		ParameterDecl param = new ParameterDecl(t,typeName,null);
		list.add(param);
		//check if currentToken is in Starters(, Type id)
		while(currentToken.kind==TokenKind.COMMA){
			acceptIt();
			t = parseType();
			typeName = currentToken.spelling;
			accept(TokenKind.ID);
			param = new ParameterDecl(t,typeName,null);
			list.add(param);
		}
		return list;
	}
	
	//ArgumentList ::= Expression(,Expression)*
	private ExprList parseArgumentList(){
		ExprList list = new ExprList();	
		if(currentToken.kind== TokenKind.ID ||
				currentToken.kind== TokenKind.THIS ||
				currentToken.kind == TokenKind.UNOP ||
				currentToken.kind == TokenKind.LPAREN ||
				currentToken.kind == TokenKind.NUM ||
				currentToken.kind == TokenKind.TRUE ||								
				currentToken.kind == TokenKind.FALSE ||
				currentToken.kind == TokenKind.NEW){
					Expression expr = parseExpression();
					list.add(expr);
					while(currentToken.kind == TokenKind.COMMA){
						acceptIt();
						expr = parseExpression();
						list.add(expr);
			}
		}
		return list;
	}
	
	//Reference ::= (.id)*
	private QualifiedRef parseReference(Reference mainRef){
		boolean first = true;
		QualifiedRef qr = new QualifiedRef(mainRef,null,null);
		QualifiedRef temp = null;
		while(currentToken.kind==TokenKind.PERIOD){
			if(!first){ qr.ref = temp;}
			acceptIt();
			Identifier id = new Identifier(currentToken);
			qr.id = id;
			accept(TokenKind.ID);
			first = false;
			temp = new QualifiedRef(qr.ref, id, null);
		}
		return qr;
		
	}
	
	//ArrayReference ::=id[Expression]
	private void parseArrayReference(){
		accept(TokenKind.ID);
		accept(TokenKind.LBOX);
		parseExpression();
		accept(TokenKind.RBOX);
	}
	
	/*Statement ::=
{ Statement* }
| id (id | Reference? ((ArgumentList?)?) ( = Expression)? ; | ( [ (Expression]) | ] id))? = Expression;) ) | = expression ;
| OtherStatementTypes id = Expression ; 
| this Reference (= Expression ; | (ArgumentList?) ; )
| if ( Expression ) Statement (else Statement)? 
| while ( Expression ) Statement
*/
	private Statement parseStatement() throws SyntaxError{
		
		switch(currentToken.kind){
		
		//statement ::= { statement*}
		case LBRACK:
			StatementList stmtList = new StatementList();
			acceptIt();
			//check if next token is in Starters(Statement)
			while(currentToken.kind==TokenKind.LBRACK ||
				currentToken.kind == TokenKind.INT ||
				currentToken.kind== TokenKind.BOOLEAN||
				currentToken.kind==TokenKind.ID ||
				currentToken.kind==TokenKind.THIS ||
				currentToken.kind == TokenKind.RETURN ||
				currentToken.kind== TokenKind.IF ||
				currentToken.kind == TokenKind.WHILE){
				
				Statement stmt = parseStatement();
				stmtList.add(stmt);
			}
			accept(TokenKind.RBRACK);
			return new BlockStmt(stmtList,null);
		
		//statement ::= id (...) = Expression ;
		case ID:
			Identifier firstID = new Identifier(currentToken);
			acceptIt();
			
			//statement ::= (id | Reference ((ArgumentList?)?) | [Expression?])
			switch(currentToken.kind){	
			//id
			case ID:
				String secondID = currentToken.spelling;
				VarDecl vd = new VarDecl(new ClassType(firstID,null), secondID,null);
				acceptIt();
				accept(TokenKind.EQUALS);
				Expression expr2 = parseExpression();
				accept(TokenKind.SEMI);
				return new VarDeclStmt(vd, expr2,null);
			//Reference ((ArgumentList?)?)
			case PERIOD: case LPAREN:
				if(currentToken.kind==TokenKind.PERIOD){
					QualifiedRef qr = parseReference(new IdRef(firstID,null));
					if(currentToken.kind == TokenKind.EQUALS){
						accept(TokenKind.EQUALS);
						Expression expr = parseExpression();
						accept(TokenKind.SEMI);
						return new AssignStmt(qr,expr,null);
					}
					else if (currentToken.kind == TokenKind.LPAREN){
						acceptIt();
						ExprList argList = parseArgumentList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMI);
					return new CallStmt(qr, argList,null);
				
					}
				}	
				else if(currentToken.kind== TokenKind.LPAREN){
					acceptIt();
					ExprList argList = parseArgumentList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMI);
					return new CallStmt(new IdRef(firstID,null), argList,null);
				}
				
			//[ (Expression]) | ] id
			case LBOX:
				acceptIt();
				
				//Check if next Token is in the Starters(Expression)
				if(currentToken.kind== TokenKind.ID ||
					currentToken.kind== TokenKind.THIS ||
					currentToken.kind == TokenKind.UNOP ||
					currentToken.kind == TokenKind.DUALOP ||
					currentToken.kind == TokenKind.LPAREN ||
					currentToken.kind == TokenKind.NUM ||
					currentToken.kind == TokenKind.TRUE ||
					currentToken.kind == TokenKind.FALSE ||
					currentToken.kind == TokenKind.NEW){
							Expression indexExpr = parseExpression();
							accept(TokenKind.RBOX);
							IndexedRef iRef = new IndexedRef(new IdRef(firstID,null), indexExpr,null);
							accept(TokenKind.EQUALS);
							Expression assignExpr = parseExpression();
							accept(TokenKind.SEMI);
							return new IxAssignStmt(iRef, assignExpr,null);
					}
				else{
					accept(TokenKind.RBOX);
					ArrayType arr = new ArrayType(new ClassType(firstID, null), null);
					String varName = currentToken.spelling;
					accept(TokenKind.ID);
					VarDecl var = new VarDecl(arr, varName, null);
					accept(TokenKind.EQUALS);
					Expression varExpr = parseExpression();
					accept(TokenKind.SEMI);
					return new VarDeclStmt(var, varExpr,null);
				}
			case EQUALS:
				acceptIt();
				Expression aEx = parseExpression();
				accept(TokenKind.SEMI);
				return new AssignStmt(new IdRef(firstID,null), aEx,null);
			default:
				//parseError("Did not expect a: "+currentToken.kind);
			}
			break;
			
		//Statement ::= this Reference (= Expression ; | (ArgumentList?) ; )
		case THIS:
			acceptIt();
			Reference firstRef = new ThisRef(null);
			if(currentToken.kind == TokenKind.PERIOD){
				firstRef = (QualifiedRef) parseReference(firstRef);
			}
			else firstRef = (ThisRef) firstRef;
			// = Expression ;
			switch(currentToken.kind){
			case EQUALS:
				acceptIt();
				Expression thisExp = parseExpression();
				accept(TokenKind.SEMI);
				return new AssignStmt(firstRef, thisExp,null);
			
			// (ArgumentList?) ;
			case LPAREN:
				acceptIt();
				ExprList aList = parseArgumentList();
				accept(TokenKind.RPAREN);
				accept(TokenKind.SEMI);
				return new CallStmt(firstRef,aList,null);
				
			default:
				parseError("Not Expecting: " + currentToken.kind);
			}
			break;
			
		//Statement ::= OtherStatementTypes id = Expression ; 
		case BOOLEAN: case INT: case RBRACK:
			Type t = parseOtherStatementTypes();
			String vName = currentToken.spelling;
			accept(TokenKind.ID);
			VarDecl var = new VarDecl(t,vName, null);
			accept(TokenKind.EQUALS);
			Expression e = parseExpression();
			accept(TokenKind.SEMI);
			return new VarDeclStmt(var, e,null);
			
		//Statement ::= return Expression? ;
		case RETURN:
			acceptIt();
			//check if next token is in Starters(Expression)
			Expression expr = null;
			if(currentToken.kind== TokenKind.ID ||
				currentToken.kind== TokenKind.THIS ||
				currentToken.kind == TokenKind.UNOP ||
				currentToken.kind == TokenKind.DUALOP||
				currentToken.kind == TokenKind.LPAREN ||
				currentToken.kind == TokenKind.NUM ||
				currentToken.kind == TokenKind.TRUE ||					
				currentToken.kind == TokenKind.FALSE ||
				currentToken.kind == TokenKind.NEW){
				expr = parseExpression();
				accept(TokenKind.SEMI);
				return new ReturnStmt(expr,null);
			}
			accept(TokenKind.SEMI);
			return new ReturnStmt(expr,null);
			
		//Statement::= if ( Expression ) Statement (else Statement)?
		case IF:
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression iExpr = parseExpression();
			accept(TokenKind.RPAREN);
			Statement iStatement = parseStatement();
			if(currentToken.kind==TokenKind.ELSE){
				acceptIt();
				Statement elseStatement = parseStatement();
				return new IfStmt(iExpr, iStatement, elseStatement,null);
			}
			return new IfStmt(iExpr, iStatement,null);
			
		//Statement ::=  while ( Expression ) Statement
		case WHILE:
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression whileExpr = parseExpression();
			accept(TokenKind.RPAREN);
			Statement whileStmt = parseStatement();
			return new WhileStmt(whileExpr, whileStmt,null);
			
		default:
			parseError("Was not expecting: "+ currentToken.kind);
		}
		return null;
	}
	
	//OtherStatementTypes ::= boolean | int[]?
	private Type parseOtherStatementTypes() throws SyntaxError{
		switch(currentToken.kind){
		case BOOLEAN:
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN,null);
			
		case INT:
			acceptIt();
			if(currentToken.kind==TokenKind.LBOX){
				accept(TokenKind.LBOX);
				accept(TokenKind.RBOX);
				return new ArrayType(new BaseType(TypeKind.INT,null),null);
			}
			return new BaseType(TypeKind.INT,null);
			
		default:
			parseError("Expecting a Boolean or an Int but got a : "+ currentToken.kind);
			return null;
		}
	}
	
	
	
	private Expression parseExpression(){
		return parseO();
	}
	
	private Expression parseO(){
		Expression op1 = parseC();
		while(currentToken.kind == TokenKind.BINOP && currentToken.spelling.equals("||")){
			acceptIt();
			Expression op2 = parseC();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, "||")),op1,op2, null);
		}
		return op1;
	}
	
	private Expression parseC(){
		Expression op1 = parseE();
		while(currentToken.kind == TokenKind.BINOP && currentToken.spelling.equals("&&")){
			acceptIt();
			Expression op2 = parseE();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, "&&")),op1,op2, null);
		}
		return op1;
	}
	
	private Expression parseE(){
		Expression op1 = parseR();
		while(currentToken.kind == TokenKind.BINOP && (currentToken.spelling.equals("==") || currentToken.spelling.equals("!="))){
			String spelling = currentToken.spelling;
			acceptIt();
			Expression op2 = parseR();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, spelling)),op1,op2, null);
		}
		return op1;
	}
	
	private Expression parseR(){
		Expression op1 = parseA();
		while(currentToken.kind == TokenKind.BINOP && (currentToken.spelling.equals("<=") || currentToken.spelling.equals(">=") ||
														currentToken.spelling.equals("<") || currentToken.spelling.equals(">") )){
			String spelling = currentToken.spelling;
			acceptIt();
			Expression op2 = parseA();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, spelling)),op1,op2, null);
		}
		return op1;
	}
	
	
	private Expression parseA(){
		Expression op1 = parseM();
		while((currentToken.kind == TokenKind.BINOP || currentToken.kind == TokenKind.DUALOP) && (currentToken.spelling.equals("+") || currentToken.spelling.equals("-"))){
			String spelling = currentToken.spelling;
			acceptIt();
			Expression op2 = parseM();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, spelling)),op1,op2, null);
		}
		return op1;
	}
	
	private Expression parseM(){
		Expression op1 = parseU();
		while(currentToken.kind == TokenKind.BINOP && (currentToken.spelling.equals("*") || currentToken.spelling.equals("/"))){
			String spelling = currentToken.spelling;
			acceptIt();
			Expression op2 = parseU();
			op1 = new BinaryExpr(new Operator(new Token(TokenKind.BINOP, spelling)),op1,op2, null);
		}
		return op1;
	}
	
	private Expression parseU(){
		if((currentToken.kind == TokenKind.UNOP || currentToken.kind == TokenKind.DUALOP) && (currentToken.spelling.equals("-"))){
		while((currentToken.kind == TokenKind.UNOP || currentToken.kind == TokenKind.DUALOP) && currentToken.spelling.equals("-")){
			String spelling = currentToken.spelling;
			acceptIt();
			Expression op = parseU();
			return new UnaryExpr(new Operator(new Token(TokenKind.UNOP, spelling)),op,null);
		}
		parseX();
		}
		
		else if((currentToken.kind == TokenKind.UNOP || currentToken.kind == TokenKind.DUALOP) && currentToken.spelling.equals("!")){
			while((currentToken.kind == TokenKind.UNOP || currentToken.kind == TokenKind.DUALOP) && currentToken.spelling.equals("!")){
				String spelling = currentToken.spelling;
				acceptIt();
				Expression op = parseU();
				return new UnaryExpr(new Operator(new Token(TokenKind.UNOP, spelling)),op,null);
			}
			parseX();
		}

		else{
			Expression op = parseX();
			return op;
		}
		return null;
	}
	
	private Expression parseX(){
		Expression op;
		switch(currentToken.kind){
		case LPAREN:
			acceptIt();
			op = parseExpression();
			accept(TokenKind.RPAREN);
			return op;
		case NUM:
			op =  new LiteralExpr(new IntLiteral(currentToken),null);
			acceptIt();
			return op;
		case ID:
			Identifier id = new Identifier(currentToken);
			acceptIt();
			if(currentToken.kind == TokenKind.PERIOD){
				QualifiedRef ref = parseReference(new IdRef(id,null));
				if(currentToken.kind == TokenKind.LPAREN){
					acceptIt();
					ExprList argList = parseArgumentList();
					accept(TokenKind.RPAREN);
					return new CallExpr(ref,argList,null);
				}
				return new RefExpr(ref,null);
			}
			else if(currentToken.kind == TokenKind.LPAREN){
				acceptIt();
				ExprList argList = parseArgumentList();
				accept(TokenKind.RPAREN);
				return new CallExpr(new IdRef(id,null),argList,null);
			}
			else if(currentToken.kind==TokenKind.LBOX){
				acceptIt();
				Expression ex = parseExpression();
				accept(TokenKind.RBOX);
				return new RefExpr(new IndexedRef(new IdRef(id,null),ex,null),null);
			}
			else return new RefExpr(new IdRef(id,null),null);
		
		case TRUE:
			Token bool = currentToken;
			acceptIt();
			return new LiteralExpr(new BooleanLiteral(bool),null);
		case FALSE:
			Token bool2 = currentToken;
			acceptIt();
			return new LiteralExpr(new BooleanLiteral(bool2),null);
		case THIS:
			acceptIt();
			if(currentToken.kind == TokenKind.PERIOD){
				QualifiedRef ref = parseReference(new ThisRef(null) );
				if(currentToken.kind == TokenKind.LPAREN){
					acceptIt();
					ExprList aList = parseArgumentList();
					accept(TokenKind.RPAREN);
					return new CallExpr(ref,aList,null);
				}
				return new RefExpr(ref,null);
			}
			if(currentToken.kind == TokenKind.LPAREN){
				acceptIt();
				Reference ref = new ThisRef(null);
				ExprList aList = parseArgumentList();
				accept(TokenKind.RPAREN);
				return new CallExpr(ref,aList,null);
			}
			return new RefExpr(new ThisRef(null),null);
		case NEW:
			acceptIt();
			switch(currentToken.kind){
			case ID:
				
				ClassType newId = new ClassType(new Identifier(currentToken),null);
				acceptIt();
				switch(currentToken.kind){
				case LPAREN:
					acceptIt();
					accept(TokenKind.RPAREN);
					return new NewObjectExpr(newId,null); 
				case LBOX:
					acceptIt();
					Expression expre = parseExpression();
					accept(TokenKind.RBOX);
					return new NewArrayExpr(newId, expre,null);
				default:
					parseError("Expected LPAREN or LBOX but got: "+ currentToken.kind);
				}
			break;
			case INT:
				acceptIt();
				accept(TokenKind.LBOX);
				Expression e = parseExpression();
				accept(TokenKind.RBOX);
				return new NewArrayExpr(new BaseType(TypeKind.INT,null),e,null);
			default:
				parseError("Expected ID or INT but got: "+ currentToken.kind);
			}
			break;
			
		default:
			parseError("Expected LPAREN, NUM, ID, TRUE, FALSE, NEW, or THIS but got: "+ currentToken.kind);
		}	
		return null;
	}
	
	
	//Auxilliary support functions for the parser
	
	/**
	 * verify that current token in input matches expected token and advance to next token
	 * @param expectedToken
	 * @throws SyntaxError  if match fails
	 */
	private void accept(TokenKind expectedTokenKind) throws SyntaxError {
		if (currentToken.kind == expectedTokenKind) {
			if (trace)
				pTrace();
			currentToken = scanner.scan();
		}
		else
			parseError("expecting '" + expectedTokenKind +
					"' but found '" + currentToken.kind + "'");
	}
	
	
	private void acceptIt() throws SyntaxError {
		accept(currentToken.kind);
	}
	
	
	
	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}

	// show parse stack whenever terminal is  accepted
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + currentToken.kind + " (\"" + currentToken.spelling + "\")");
		System.out.println();
	}

}
