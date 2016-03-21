package miniJava.AbstractSyntaxTrees;

import miniJava.ErrorReporter;

public class ContextChecker {
	public IdentificationTable idTable;
	
	public ContextChecker(){
		idTable = new IdentificationTable();
	}
	
	
	public AST idCheck(AST a, ErrorReporter e){
		ASTIDChecker idC = new ASTIDChecker();
		return idC.idCheck(AST a, ErrorReporter e);
	}
}
