/**
 *   COMP 520 
 *   Simple expression scanner and parser
 *     following package structure of a full compiler
*/

package miniJava;

import java.io.FileInputStream;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

import java.io.FileNotFoundException;
import java.io.InputStream;

import contextualAnalyzer.ContextChecker;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

/**
 * Recognize whether input is an arithmetic expression as defined by
 * a simple context free grammar for expressions and a scanner grammar.
 * 
 */
public class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {

		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				inputStream = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);
		ContextChecker cc = new ContextChecker();

		System.out.println("Syntactic analysis ... ");
		Package ast = parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		
		if (reporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			System.exit(4);
		}
		else {
			System.out.println("valid arithmetic expression");
			  // traverse AST to construct explicitly parenthesized text representation
			 ASTDisplay display = new ASTDisplay();
		     display.showTree(ast);
		}
		
		System.out.println("Contextual analysis ... ID Checking... ");
		Package idCheckedAST = cc.idCheck(ast, reporter);
		
		if(reporter.hasErrors()){
			System.out.println("INVALID arithmetic expression... ID Error");
			System.exit(4);
		}
		System.out.println("ID Checking complete... Now Type Checking");
		
		Package typeCheckedAST = cc.typeCheck(idCheckedAST, reporter);
		if(reporter.hasErrors()){
			System.out.println("INVALID arithmetic expression... Type Error");
			System.exit(4);
		}
		
		System.out.println("Finished Syntactic and Contextual Analysis...Valid MiniJava Program");
		System.exit(0);
	}
}

