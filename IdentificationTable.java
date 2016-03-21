package miniJava.AbstractSyntaxTrees;
import java.util.ArrayList;


public class IdentificationTable {
	public ArrayList<ScopeLevel> idTable;
	public int currentScope;
	
	public void IndentificationTable(){
		this.idTable = new ArrayList<ScopeLevel>();
		currentScope = 0;
	}
	
	public void openScope(){
		currentScope++;
		idTable.add(new ScopeLevel(currentScope));
	}
	public void closeScope(){
		for(ScopeLevel s : idTable){
			if(s.scope == currentScope){
				idTable.remove(s);
			}
		}
	}
	
	public void enter(String id, Attribute attr) throws IDError{
		if(currentScope < 3){
			for(ScopeLevel s : idTable){
				if(s.scope == currentScope){
					if(s.checkInScope(attr)){
						throw new IDError();
					}
					else{
						s.enterToScope(attr);
					}
				}
			}
		}
		else{
			for(ScopeLevel s : idTable){
				if(s.scope > 2){
					if(s.checkInScope(attr)){
						throw new IDError();
					}
					else{
						s.enterToScope(attr);
					}
				}
			}
		}
	}
	
	public Attribute retrieve(String id) throws IDError{
		for(ScopeLevel s : idTable){
			for(Attribute a: s.entry){
				if(a.id.equals(id)) return a;
			}
		}
		throw new IDError();
	}
	
	public class IDError extends Error{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}
}
