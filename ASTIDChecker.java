/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.AbstractSyntaxTrees.IdentificationTable.IDError;

/*
 * Display AST in text form
 *   In-order traversal of AST, visiting each node with a method of the form  
 *   
 *       public Object visitXXX( XXX astnode, String arg)
 *       
 *   where arg is a prefix string (indentation) to precede display of ast node
 *   and a null Object is returned as the result.
 *   
 *   implements Visitor<argtype,resulttype>
 */
public class ASTIDChecker implements Visitor<String,Object> {
	
	public static boolean showPosition = false;
	
	public ContextChecker cc = new ContextChecker();
    
    /**
     * print text representation of AST to stdout
     * @param ast root node of AST 
     */
	public AST visitTree(AST ast){
		return (AST)ast.visit(this, null);
		
	}
	
    public void showTree(AST ast){
        System.out.println("======= AST Display =========================");
        ast.visit(this, "");
        System.out.println("=============================================");
    }   
    
    // methods to format output
    
    /**
     * display arbitrary text for a node
     * @param prefix  spaced indent to indicate depth in AST
     * @param text    preformatted node display
     */
    private void show(String prefix, String text) {
        System.out.println(prefix + text);
    }
    
    /**
     * display AST node by name
     * @param prefix  spaced indent to indicate depth in AST
     * @param node    AST node, will be shown by name
     */
    private void show(String prefix, AST node) {
    	System.out.println(prefix + node.toString());
    }
    
    /**
     * quote a string
     * @param text    string to quote
     */
    private String quote(String text) {
    	return ("\"" + text + "\"");
    }
    
    /**
     * increase depth in AST
     * @param prefix  current spacing to indicate depth in AST
     * @return  new spacing 
     */
    private String indent(String prefix) {
        return prefix + "  ";
    }
    
    //Returns the ContextChecker after contextual Analysis
    public ContextChecker getCC(){
    	return cc;
    }
	///////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////// 

    public Object visitPackage(Package prog, String arg){
        ClassDeclList cl = prog.classDeclList;
        cc.idTable.openScope();
        ClassDeclList newCl = new ClassDeclList();
        for (ClassDecl c: prog.classDeclList){
            newCl.add((ClassDecl)c.visit(this, null));
        }
        prog.classDeclList = newCl;
        return prog;
    }
    
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitClassDecl(ClassDecl clas, String arg){
    	cc.idTable.enter(clas.name, new Attribute(clas.name, clas));
        cc.idTable.openScope();
        FieldDeclList newFl = new FieldDeclList();
        for (FieldDecl f: clas.fieldDeclList){
        	newFl.add((FieldDecl)f.visit(this, null));
        }
        MethodDeclList newMl = new MethodDeclList();
        for (MethodDecl m: clas.methodDeclList)
        	newMl.add((MethodDecl)m.visit(this, null));
        cc.idTable.closeScope();
        
        return new ClassDecl(clas.name, newFl, newMl, null);
    }
    
    public Object visitFieldDecl(FieldDecl f, String arg){
       	cc.idTable.enter(f.name, new Attribute(f.name, f));
    	f.type = (Type)f.type.visit(this, null);
        return f;
    }
    
    public Object visitMethodDecl(MethodDecl m, String arg){
    	cc.idTable.enter(m.name, new Attribute(m.name, m));
    	m.type.visit(this, null);
        ParameterDeclList pdl = m.parameterDeclList;
        
        //Open scope on param decls
        cc.idTable.openScope();
	ParameterDeclList newPDL = new ParameterDeclList();
        for (ParameterDecl pd: pdl) {
        	newPDL.add((ParameterDecl)pd.visit(this, null));
        }
	
        StatementList sl = m.statementList;
	StatementList newSL = new StatementList();
        
        cc.idTable.openScope();
        for (Statement s: sl) {
           newSL.add((Statement)s.visit(this, null));
        }
        //Close scope on the declarations in the statements
        cc.idTable.closeScope();
        
        //Close scope on the parameter decls
        cc.idTable.closeScope();
        
        return new MethodDecl(new FieldDecl(m.isPrivate, m.isStatic, m.type, m.name, null), newPDL, newSL, null) ;
    }
    
    public Object visitParameterDecl(ParameterDecl pd, String arg){
    	cc.idTable.enter(pd.name, new Attribute(pd.name,pd));
        return new ParameterDecl((Type)pd.type.visit(this, null), pd.name, null);
    } 
    
    public Object visitVarDecl(VarDecl vd, String arg){
    	cc.idTable.enter(vd.name, new Attribute(vd.name,vd));
        return new VarDecl((Type)vd.type.visit(this, null), vd.name, null);
    }
 
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitBaseType(BaseType type, String arg){
        switch(type.typeKind){
        case INT:
        	 return new BaseType(TypeKind.INT, null);
        case BOOLEAN:
        	return new BaseType(TypeKind.BOOLEAN,null);
        case VOID:
        	return new BaseType(TypeKind.VOID, null);
        case NULL:
        	return new BaseType(TypeKind.NULL, null);
        default:
        	return null;
        	
        }
    }
    
    public Object visitClassType(ClassType type, String arg){
        try{
        	type.decl = cc.idTable.retrieve(type.className.spelling).decl;
        }
        catch(IDError e){
        	System.out.println(e.message);
        	return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        return type;
    }
    
    public Object visitArrayType(ArrayType type, String arg){
        return new ArrayType((Type)type.eltType.visit(this, indent(arg)),null);
      
    }
    
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitBlockStmt(BlockStmt stmt, String arg){
        cc.idTable.openScope();
        StatementList sl = stmt.sl;
        StatementList newSL = new StatementList();
        for (Statement s: sl) {
        	newSL.add((Statement)s.visit(this,null));
        }
        cc.idTable.closeScope();
        return new BlockStmt(newSL,null);
    }
    
    public Object visitVardeclStmt(VarDeclStmt stmt, String arg){
        stmt.varDecl = (VarDecl)stmt.varDecl.visit(this, indent(arg));	
        stmt.initExp = (Expression) stmt.initExp.visit(this, indent(arg));
        return stmt;
    }
    
    public Object visitAssignStmt(AssignStmt stmt, String arg){
        stmt.ref = (Reference)stmt.ref.visit(this, indent(arg));
        stmt.val = (Expression)stmt.val.visit(this, indent(arg));
        return stmt;
    }
    
    public Object visitIxAssignStmt(IxAssignStmt stmt, String arg){
        stmt.ixRef = (IndexedRef)stmt.ixRef.visit(this, indent(arg));
        stmt.val = (Expression)stmt.val.visit(this, indent(arg));
        return stmt;
    }
    
    public Object visitCallStmt(CallStmt stmt, String arg){
    	Reference r = (Reference)stmt.methodRef.visit(this, indent(arg));
        ExprList al = stmt.argList;
        ExprList newAl = new ExprList();
        for (Expression e: al) {
            newAl.add((Expression)e.visit(this, null));
        }
        return new CallStmt(r, newAl, null);
    }
    
    public Object visitReturnStmt(ReturnStmt stmt, String arg){
         if (stmt.returnExpr != null)
            stmt.returnExpr = (Expression)stmt.returnExpr.visit(this, indent(arg));
        return stmt;
    }
    
    public Object visitIfStmt(IfStmt stmt, String arg){
        stmt.cond = (Expression)stmt.cond.visit(this, indent(arg));
        stmt.thenStmt = (Statement)stmt.thenStmt.visit(this, indent(arg));
        if (stmt.elseStmt != null)
            stmt.elseStmt = (Statement)stmt.elseStmt.visit(this, indent(arg));
        return stmt;
    }
    
    public Object visitWhileStmt(WhileStmt stmt, String arg){
        stmt.cond = (Expression)stmt.cond.visit(this, indent(arg));
        stmt.body = (Statement)stmt.body.visit(this, indent(arg));
        return stmt;
    }
    

	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitUnaryExpr(UnaryExpr expr, String arg){
        expr.operator = (Operator)expr.operator.visit(this, indent(arg));
        expr.expr = (Expression)expr.expr.visit(this, indent(indent(arg)));
        return expr;
    }
    
    public Object visitBinaryExpr(BinaryExpr expr, String arg){
        expr.operator = (Operator)expr.operator.visit(this, indent(arg));
        expr.left = (Expression)expr.left.visit(this, indent(indent(arg)));
        expr.right = (Expression)expr.right.visit(this, indent(indent(arg)));
        return expr;
    }
    
    public Object visitRefExpr(RefExpr expr, String arg){
        expr.ref = (Reference)expr.ref.visit(this, indent(arg));
        return expr;
    }
    
    public Object visitCallExpr(CallExpr expr, String arg){
    
        Reference newR = (Reference)expr.functionRef.visit(this, indent(arg));
        ExprList al = expr.argList;
        ExprList newAl= new ExprList();
        for (Expression e: al) {
            newAl.add((Expression)e.visit(this, null));
        }
        return new CallExpr(newR, al,null);
    }
    
    public Object visitLiteralExpr(LiteralExpr expr, String arg){
        expr.lit = (Terminal)expr.lit.visit(this, indent(arg));
        return expr;
    }
 
    public Object visitNewArrayExpr(NewArrayExpr expr, String arg){
        expr.eltType = (Type)expr.eltType.visit(this, indent(arg));
        expr.sizeExpr = (Expression)expr.sizeExpr.visit(this, indent(arg));
        return expr;
    }
    
    public Object visitNewObjectExpr(NewObjectExpr expr, String arg){
        expr.classtype	= (ClassType)expr.classtype.visit(this, indent(arg));
        return expr;
    }
    

	///////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	///////////////////////////////////////////////////////////////////////////////
	
    public Object visitQualifiedRef(QualifiedRef qr, String arg) {
    	show(arg, qr);
    	qr.id.visit(this, indent(arg));
    	qr.ref.visit(this, indent(arg));
	    return null;
    }
    
    public Object visitIndexedRef(IndexedRef ir, String arg) {
    	show(arg, ir);
    	ir.indexExpr.visit(this, indent(arg));
    	ir.idRef.visit(this, indent(arg));
    	return null;
    }
    
    public Object visitIdRef(IdRef ref, String arg) {
    	show(arg,ref);
    	ref.id.visit(this, indent(arg));
    	return null;
    }
   
    public Object visitThisRef(ThisRef ref, String arg) {
    	show(arg,ref);
    	return null;
    }
    
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitIdentifier(Identifier id, String arg){
        show(arg, quote(id.spelling) + " " + id.toString());
        int i = cc.idTable.currentScope;
        while(i>=0){
        	ScopeLevel refs = cc.idTable.idTable.get(i);
        	for(Attribute ref : refs.entry){
        		if(ref.id.equals(id)){
        			id.decl = ref.decl;
        			return null;
        		}
        	i--;
        	}
        }
        return null;
    }
    
    public Object visitOperator(Operator op, String arg){
        show(arg, quote(op.spelling) + " " + op.toString());
        return null;
    }
    
    public Object visitIntLiteral(IntLiteral num, String arg){
        show(arg, quote(num.spelling) + " " + num.toString());
        return null;
    }
    
    public Object visitBooleanLiteral(BooleanLiteral bool, String arg){
        show(arg, quote(bool.spelling) + " " + bool.toString());
        return null;
    }
}
