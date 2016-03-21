/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import contextualAnalyzer.Attribute;
import contextualAnalyzer.ContextChecker;
import contextualAnalyzer.ScopeLevel;

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
public class ASTDisplay implements Visitor<String,Object> {
	
	public static boolean showPosition = false;
	
	public ContextChecker cc = new ContextChecker();
    
    /**
     * print text representation of AST to stdout
     * @param ast root node of AST 
     */
	public void visitTree(AST ast){
		ast.visit(this, "");
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

    public Object visitPackage(Package prog, String arg, boolean contextCheck){
    	show(arg, prog);
        ClassDeclList cl = prog.classDeclList;
        show(arg,"  ClassDeclList [" + cl.size() + "]");
        String pfx = arg + "  . "; 
        cc.idTable.openScope();
        for (ClassDecl c: prog.classDeclList){
            c.visit(this, pfx);
        }
        return null;
    }
    
    
	///////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitClassDecl(ClassDecl clas, String arg){
    	cc.idTable.enter(clas.name, new Attribute(clas.name, clas));
        show(arg, clas);
        show(indent(arg), quote(clas.name) + " classname");
        show(arg,"  FieldDeclList [" + clas.fieldDeclList.size() + "]");
        String pfx = arg + "  . "; 
        cc.idTable.openScope();
        for (FieldDecl f: clas.fieldDeclList){
        	f.visit(this, pfx);
        }
        show(arg,"  MethodDeclList [" + clas.methodDeclList.size() + "]");
        for (MethodDecl m: clas.methodDeclList)
        	m.visit(this, pfx);
        cc.idTable.closeScope();
        return null;
    }
    
    public Object visitFieldDecl(FieldDecl f, String arg){
       	cc.idTable.enter(f.name, new Attribute(f.name, f));
    	show(arg, "(" + (f.isPrivate ? "private": "public") 
    			+ (f.isStatic ? " static) " :") ") + f.toString());
    	f.type.visit(this, indent(arg));
    	show(indent(arg), quote(f.name) + " fieldname");
        return null;
    }
    
    public Object visitMethodDecl(MethodDecl m, String arg){
    	cc.idTable.enter(m.name, new Attribute(m.name, m));
    	
       	show(arg, "(" + (m.isPrivate ? "private": "public") 
    			+ (m.isStatic ? " static) " :") ") + m.toString());
    	m.type.visit(this, indent(arg));
    	show(indent(arg), quote(m.name) + " methodname");
        ParameterDeclList pdl = m.parameterDeclList;
        show(arg, "  ParameterDeclList [" + pdl.size() + "]");
        String pfx = ((String) arg) + "  . ";
        
        //Open scope on param decls
        cc.idTable.openScope();
        for (ParameterDecl pd: pdl) {
        	pd.visit(this, pfx);
        }
        StatementList sl = m.statementList;
        show(arg, "  StmtList [" + sl.size() + "]");
        
        cc.idTable.openScope();
        for (Statement s: sl) {
           s.visit(this, pfx);
        }
        //Close scope on the declarations in the statements
        cc.idTable.closeScope();
        
        //Close scope on the parameter decls
        cc.idTable.closeScope();
        
        return null;
    }
    
    public Object visitParameterDecl(ParameterDecl pd, String arg){
    	cc.idTable.enter(pd.name, new Attribute(pd.name,pd));
        show(arg, pd);
        pd.type.visit(this, indent(arg));
        show(indent(arg), quote(pd.name) + "parametername ");
        return null;
    } 
    
    public Object visitVarDecl(VarDecl vd, String arg){
    	cc.idTable.enter(vd.name, new Attribute(vd.name,vd));
        show(arg, vd);
        vd.type.visit(this, indent(arg));
        show(indent(arg), quote(vd.name) + " varname");
        return null;
    }
 
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	///////////////////////////////////////////////////////////////////////////////
    
    public Object visitBaseType(BaseType type, String arg){
        show(arg, type.typeKind + " " + type.toString());
        return null;
    }
    
    public Object visitClassType(ClassType type, String arg){
        show(arg, type);
        show(indent(arg), quote(type.className.spelling) + " classname");
        return null;
    }
    
    public Object visitArrayType(ArrayType type, String arg){
        show(arg, type);
        type.eltType.visit(this, indent(arg));
        return null;
    }
    
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitBlockStmt(BlockStmt stmt, String arg){
        cc.idTable.openScope();
    	show(arg, stmt);
        StatementList sl = stmt.sl;
        show(arg,"  StatementList [" + sl.size() + "]");
        String pfx = arg + "  . ";
        for (Statement s: sl) {
        	s.visit(this, pfx);
        }
        cc.idTable.closeScope();
        return null;
    }
    
    public Object visitVardeclStmt(VarDeclStmt stmt, String arg){
        show(arg, stmt);
        stmt.varDecl.visit(this, indent(arg));	
        stmt.initExp.visit(this, indent(arg));
        return null;
    }
    
    public Object visitAssignStmt(AssignStmt stmt, String arg){
        show(arg,stmt);
        stmt.ref.visit(this, indent(arg));
        stmt.val.visit(this, indent(arg));
        return null;
    }
    
    public Object visitIxAssignStmt(IxAssignStmt stmt, String arg){
        show(arg,stmt);
        stmt.ixRef.visit(this, indent(arg));
        stmt.val.visit(this, indent(arg));
        return null;
    }
    
    public Object visitCallStmt(CallStmt stmt, String arg){
        show(arg,stmt);
        stmt.methodRef.visit(this, indent(arg));
        ExprList al = stmt.argList;
        show(arg,"  ExprList [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e: al) {
            e.visit(this, pfx);
        }
        return null;
    }
    
    public Object visitReturnStmt(ReturnStmt stmt, String arg){
        show(arg,stmt);
         if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, indent(arg));
        return null;
    }
    
    public Object visitIfStmt(IfStmt stmt, String arg){
        show(arg,stmt);
        stmt.cond.visit(this, indent(arg));
        stmt.thenStmt.visit(this, indent(arg));
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, indent(arg));
        return null;
    }
    
    public Object visitWhileStmt(WhileStmt stmt, String arg){
        show(arg, stmt);
        stmt.cond.visit(this, indent(arg));
        stmt.body.visit(this, indent(arg));
        return null;
    }
    

	///////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	///////////////////////////////////////////////////////////////////////////////

    public Object visitUnaryExpr(UnaryExpr expr, String arg){
        show(arg, expr);
        expr.operator.visit(this, indent(arg));
        expr.expr.visit(this, indent(indent(arg)));
        return null;
    }
    
    public Object visitBinaryExpr(BinaryExpr expr, String arg){
        show(arg, expr);
        expr.operator.visit(this, indent(arg));
        expr.left.visit(this, indent(indent(arg)));
        expr.right.visit(this, indent(indent(arg)));
        return null;
    }
    
    public Object visitRefExpr(RefExpr expr, String arg){
        show(arg, expr);
        expr.ref.visit(this, indent(arg));
        return null;
    }
    
    public Object visitCallExpr(CallExpr expr, String arg){
        show(arg, expr);
        expr.functionRef.visit(this, indent(arg));
        ExprList al = expr.argList;
        show(arg,"  ExprList + [" + al.size() + "]");
        String pfx = arg + "  . ";
        for (Expression e: al) {
            e.visit(this, pfx);
        }
        return null;
    }
    
    public Object visitLiteralExpr(LiteralExpr expr, String arg){
        show(arg, expr);
        expr.lit.visit(this, indent(arg));
        return null;
    }
 
    public Object visitNewArrayExpr(NewArrayExpr expr, String arg){
        show(arg, expr);
        expr.eltType.visit(this, indent(arg));
        expr.sizeExpr.visit(this, indent(arg));
        return null;
    }
    
    public Object visitNewObjectExpr(NewObjectExpr expr, String arg){
        show(arg, expr);
        expr.classtype.visit(this, indent(arg));
        return null;
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