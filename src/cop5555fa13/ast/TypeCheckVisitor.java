/**************************************************************************
 *********************** Type Check Visitor *****************************
 *************************************************************************/

package cop5555fa13.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cop5555fa13.TokenStream.Kind;

/**
 * Type check visitor class. For every type of node in Abstract Syntax tree
 * created by parser, this class visits each node and checks types of each
 * variable. For declarations, a global symbol table is created first.
 * 
 * @author nachiket
 * 
 */
public class TypeCheckVisitor implements ASTVisitor {

	/**
	 * Global symbol table for Type CHeck Visitor
	 */
	Map<String, Dec> symbolTable;
	/**
	 * container for collection of error messages.
	 */
	StringBuilder errorLog;
	List<ASTNode> errorNodeList;
	/**
	 * Name of the pgogram.
	 */
	String progName;

	public TypeCheckVisitor() {
		symbolTable = new HashMap<String, Dec>();
		errorLog = new StringBuilder();
		errorNodeList = new ArrayList<ASTNode>();
	}

	/**
	 * To retrieve Error Nodes
	 * 
	 * @return: a node from Abstract Syntax Tree.
	 */
	public List<ASTNode> getErrorNodeList() {
		return errorNodeList;
	}

	/**
	 * CHecks whether input program is type correct or not.
	 * 
	 * @return true if program is correct else false.
	 */
	public boolean isCorrect() {
		return errorNodeList.size() == 0;
	}

	/**
	 * To retrieve the error messages collected.
	 * 
	 * @return a string containing all errors.
	 */
	public String getLog() {
		return errorLog.toString();
	}

	/**
	 * Declaration.
	 */
	@Override
	public Object visitDec(Dec dec, Object arg) {

		Dec symTableEntry = symbolTable.get(dec.ident.getText());

		if (dec.ident.getText().equals(progName) || symTableEntry != null) {
			errorNodeList.add(dec);
			errorLog.append("Declaration: Invalid declaration." + '\n');
		} else {
			symbolTable.put(dec.ident.getText(), dec);
		}
		return null;
	}

	/**
	 * Program.
	 */
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {

		this.progName = program.getProgName();
		for (Dec dec : program.decList) {
			dec.visit(this, null);
		}
		for (Stmt stmt : program.stmtList) {
			stmt.visit(this, null);
		}
		return null;
	}

	/**
	 * Alternative Statement (if- else)
	 */
	@Override
	public Object visitAlternativeStmt(AlternativeStmt alternativeStmt,
			Object arg) throws Exception {

		alternativeStmt.expr.visit(this, null);
		if (alternativeStmt.expr.type == Kind._boolean) {
			for (Stmt ifStmt : alternativeStmt.ifStmtList) {
				ifStmt.visit(this, null);
			}
			for (Stmt elStmt : alternativeStmt.elseStmtList) {
				elStmt.visit(this, null);
			}
		} else {
			errorNodeList.add(alternativeStmt);
			errorLog.append("AlternativeStmt: Invalid type of conditional expression." + '\n');
		}
		return null;
	}

	/**
	 * Pause statement.
	 */
	@Override
	public Object visitPauseStmt(PauseStmt pauseStmt, Object arg)
			throws Exception {

		pauseStmt.expr.visit(this, null);
		if (!(pauseStmt.expr.type == Kind._int)) {
			errorNodeList.add(pauseStmt);
			errorLog.append("PauseStmt: Invalid pause statement." + '\n');
		}
		return null;
	}

	/**
	 * Iteration Statement (while)
	 */
	@Override
	public Object visitIterationStmt(IterationStmt iterationStmt, Object arg)
			throws Exception {

		iterationStmt.expr.visit(this, null);
		if (iterationStmt.expr.type == Kind._boolean) {
			for (Stmt stmt : iterationStmt.stmtList) {
				stmt.visit(this, null);
			}
		} else {
			errorNodeList.add(iterationStmt);
			errorLog.append("IterationStmt: Invalid type of conditional expression." + '\n');
		}
		return null;
	}

	/**
	 * Pixel Assignment Statement
	 */
	@Override
	public Object visitAssignPixelStmt(AssignPixelStmt assignPixelStmt,
			Object arg) throws Exception {

		Dec dec = symbolTable.get(assignPixelStmt.lhsIdent.getText());

		if (dec.type == Kind.pixel && dec != null) {
			assignPixelStmt.pixel.visit(this, null);
			assignPixelStmt.setflag(true);
		} else if (dec.type == Kind.image && dec != null) {
			assignPixelStmt.pixel.visit(this, null);
			assignPixelStmt.setflag(false);
			errorNodeList.clear();
		} else {
			errorNodeList.add(assignPixelStmt);
			errorLog.append("AssignPixelStmt: Error matching types of ident and pixel." + '\n');
		}
		return null;
	}

	/**
	 * Pixel.
	 */
	@Override
	public Object visitPixel(Pixel pixel, Object arg) throws Exception {

		pixel.redExpr.visit(this, null);
		pixel.greenExpr.visit(this, null);
		pixel.blueExpr.visit(this, null);

		if (!(pixel.blueExpr.type == Kind._int
				&& pixel.greenExpr.type == Kind._int && pixel.redExpr.type == Kind._int)) {
			errorNodeList.add(pixel);
			errorLog.append("Pixel: Error matching types of red, blue , green expressions to _int." + '\n');
		}
		return null;
	}

	/**
	 * Single Pixel Assignment Statement.
	 */
	@Override
	public Object visitSinglePixelAssignmentStmt(
			SinglePixelAssignmentStmt singlePixelAssignmentStmt, Object arg)
			throws Exception {

		singlePixelAssignmentStmt.xExpr.visit(this, null);
		singlePixelAssignmentStmt.yExpr.visit(this, null);
		Dec dec = symbolTable.get(singlePixelAssignmentStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == Kind.image
				&& singlePixelAssignmentStmt.xExpr.type == Kind._int && singlePixelAssignmentStmt.yExpr.type == Kind._int)) {
			errorNodeList.add(singlePixelAssignmentStmt);
			errorLog.append("SinglePixelAssignmentStmt: Error matching types of pixel expressions and identifier." + '\n');
		}
		return null;
	}

	/**
	 * Single Sample Assignment statement.
	 */
	@Override
	public Object visitSingleSampleAssignmentStmt(
			SingleSampleAssignmentStmt singleSampleAssignmentStmt, Object arg)
			throws Exception {

		singleSampleAssignmentStmt.xExpr.visit(this, null);
		singleSampleAssignmentStmt.yExpr.visit(this, null);
		singleSampleAssignmentStmt.rhsExpr.visit(this, null);
		Dec dec = symbolTable
				.get(singleSampleAssignmentStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == Kind.image
				&& singleSampleAssignmentStmt.xExpr.type == Kind._int
				&& singleSampleAssignmentStmt.yExpr.type == Kind._int && singleSampleAssignmentStmt.rhsExpr.type == Kind._int)) {
			errorNodeList.add(singleSampleAssignmentStmt);
			errorLog.append("SingleSampleAssignmentStmt: Error matching types of pixel expressions." + '\n');
		}
		return null;
	}

	/**
	 * Screen Location Assignment Statement.
	 */
	@Override
	public Object visitScreenLocationAssignmentStmt(
			ScreenLocationAssignmentStmt screenLocationAssignmentStmt,
			Object arg) throws Exception {

		screenLocationAssignmentStmt.xScreenExpr.visit(this, null);
		screenLocationAssignmentStmt.yScreenExpr.visit(this, null);
		Dec dec = symbolTable.get(screenLocationAssignmentStmt.lhsIdent
				.getText());

		if (!((dec != null) && dec.type == Kind.image
				&& screenLocationAssignmentStmt.xScreenExpr.type == Kind._int && screenLocationAssignmentStmt.yScreenExpr.type == Kind._int)) {
			errorNodeList.add(screenLocationAssignmentStmt);
			errorLog.append("ScreenLocationAssignmentStmt: Error matching types of pixel expressions." + '\n');
		}
		return null;
	}

	/**
	 * Image shape assignment statement.
	 */
	@Override
	public Object visitShapeAssignmentStmt(
			ShapeAssignmentStmt shapeAssignmentStmt, Object arg)
			throws Exception {

		shapeAssignmentStmt.height.visit(this, null);
		shapeAssignmentStmt.width.visit(this, null);
		Dec dec = symbolTable.get(shapeAssignmentStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == Kind.image
				&& shapeAssignmentStmt.height.type == Kind._int && shapeAssignmentStmt.width.type == Kind._int)) {
			errorNodeList.add(shapeAssignmentStmt);
			errorLog.append("ShapeAssignmentStmt: Error matching types of identifier and pixel (width/ height)." + '\n');
		}
		return null;
	}

	/**
	 * Set visibility of image statement.
	 */
	@Override
	public Object visitSetVisibleAssignmentStmt(
			SetVisibleAssignmentStmt setVisibleAssignmentStmt, Object arg)
			throws Exception {

		setVisibleAssignmentStmt.expr.visit(this, null);
		Dec dec = symbolTable.get(setVisibleAssignmentStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == Kind.image && setVisibleAssignmentStmt.expr.type == Kind._boolean)) {
			errorNodeList.add(setVisibleAssignmentStmt);
			errorLog.append("SetVisibleAssignmentStmt: Error matching types of identifier and pixel expression." + '\n');
		}
		return null;
	}

	/**
	 * Assign filename to image statement.
	 */
	@Override
	public Object FileAssignStmt(cop5555fa13.ast.FileAssignStmt fileAssignStmt,
			Object arg) throws Exception {

		Dec dec = symbolTable.get(fileAssignStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == Kind.image)) {
			errorNodeList.add(fileAssignStmt);
			errorLog.append("FileAssignStmt: Identifier type must be _pixel." + '\n');
		}
		return null;
	}

	/**
	 * Conditional Expression (a<b? a:b)
	 */
	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr,
			Object arg) throws Exception {

		conditionalExpr.condition.visit(this, null);
		conditionalExpr.trueValue.visit(this, null);
		conditionalExpr.falseValue.visit(this, null);

		if (!(conditionalExpr.condition.type == Kind._boolean && conditionalExpr.trueValue.type == conditionalExpr.falseValue.type)) {
			errorNodeList.add(conditionalExpr);
			errorLog.append("ConditionalExpr: Error matching types of expressions." + '\n');
		}

		conditionalExpr.type = conditionalExpr.trueValue.type;

		return null;
	}

	/**
	 * Binary Expression.
	 */
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg)
			throws Exception {

		binaryExpr.e0.visit(this, null);
		binaryExpr.e1.visit(this, null);

		if (binaryExpr.op.kind == Kind.AND || binaryExpr.op.kind == Kind.OR) {
			if (!(binaryExpr.e0.type == Kind._boolean && binaryExpr.e1.type == Kind._boolean)) {
				errorNodeList.add(binaryExpr);
				errorLog.append("BinaryExpr: Error matching types of expressions." + '\n');
			}
			binaryExpr.type = Kind._boolean;
		} else if (binaryExpr.op.kind == Kind.PLUS
				|| binaryExpr.op.kind == Kind.MINUS
				|| binaryExpr.op.kind == Kind.DIV
				|| binaryExpr.op.kind == Kind.MOD
				|| binaryExpr.op.kind == Kind.TIMES
				|| binaryExpr.op.kind == Kind.LSHIFT
				|| binaryExpr.op.kind == Kind.RSHIFT) {

			if (!((binaryExpr.e0.type == Kind._int
					|| binaryExpr.e0.type == Kind.x
					|| binaryExpr.e0.type == Kind.y || binaryExpr.e0.type == Kind.SCREEN_SIZE) && (binaryExpr.e1.type == Kind._int
					|| binaryExpr.e1.type == Kind.x
					|| binaryExpr.e1.type == Kind.y || binaryExpr.e1.type == Kind.SCREEN_SIZE))) {
				errorNodeList.add(binaryExpr);
				errorLog.append("BinaryExpr: Error matching types of expressions." + '\n');
			}
			binaryExpr.type = Kind._int;
		} else if (binaryExpr.op.kind == Kind.EQ
				|| binaryExpr.op.kind == Kind.NEQ) {
			if (!(binaryExpr.e0.type == binaryExpr.e1.type)) {
				errorNodeList.add(binaryExpr);
				errorLog.append("BinaryExpr: Error matching types of expressions." + '\n');
			}
			binaryExpr.type = Kind._boolean;
		} else if (binaryExpr.op.kind == Kind.LT
				|| binaryExpr.op.kind == Kind.GT
				|| binaryExpr.op.kind == Kind.LEQ
				|| binaryExpr.op.kind == Kind.GEQ) {
			if (!((binaryExpr.e0.type == Kind._int
					|| binaryExpr.e0.type == Kind.x || binaryExpr.e0.type == Kind.y || binaryExpr.e0.type == Kind.SCREEN_SIZE) && (binaryExpr.e1.type == Kind._int
					|| binaryExpr.e1.type == Kind.x || binaryExpr.e1.type == Kind.y || binaryExpr.e1.type == Kind.SCREEN_SIZE))) {
				errorNodeList.add(binaryExpr);
				errorLog.append("BinaryExpr: Error matching types of expressions." + '\n');
			}
			binaryExpr.type = Kind._boolean;
		}
		return null;
	}

	/**
	 * Sample Expression.
	 */
	@Override
	public Object visitSampleExpr(SampleExpr sampleExpr, Object arg)
			throws Exception {

		sampleExpr.xLoc.visit(this, null);
		sampleExpr.yLoc.visit(this, null);

		Dec dec = symbolTable.get(sampleExpr.ident.getText());

		if (!((dec != null) && dec.type == Kind.image
				&& sampleExpr.xLoc.type == Kind._int && sampleExpr.yLoc.type == Kind._int)) {
			errorNodeList.add(sampleExpr);
			errorLog.append("SampleExpr: Error matching types of expressions." + '\n');
		}
		sampleExpr.type = Kind._int;
		return null;
	}

	/**
	 * Image Attribute Expression.
	 */
	@Override
	public Object visitImageAttributeExpr(
			ImageAttributeExpr imageAttributeExpr, Object arg) throws Exception {

		imageAttributeExpr.type = Kind._int;
		return null;
	}

	/**
	 * Ident Expression.
	 */
	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg)
			throws Exception {

		Dec dec = symbolTable.get(identExpr.ident.getText());
		if (dec == null) {
			errorNodeList.add(identExpr);
			errorLog.append("IdentExpr: Identifier is not declared." + '\n');
			return null;
		}
		identExpr.type = dec.type;
		return null;
	}

	/**
	 * Integer Literal Expression.
	 */
	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg)
			throws Exception {

		intLitExpr.type = Kind._int;
		return null;
	}

	/**
	 * Boolean Literal Expression.
	 */
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg)
			throws Exception {

		booleanLitExpr.type = Kind._boolean;
		return null;
	}

	/**
	 * Predefined Expression
	 */
	@Override
	public Object visitPreDefExpr(PreDefExpr PreDefExpr, Object arg)
			throws Exception {

		if (PreDefExpr.constantLit.kind == Kind.SCREEN_SIZE) {
			PreDefExpr.type = Kind.SCREEN_SIZE;
		} else if (PreDefExpr.constantLit.kind == Kind.x) {
			PreDefExpr.type = Kind.x;
		} else if (PreDefExpr.constantLit.kind == Kind.y) {
			PreDefExpr.type = Kind.y;
		} else {
			PreDefExpr.type = Kind._int;
		}

		return null;
	}

	/**
	 * Assignment Expression Statement
	 */
	@Override
	public Object visitAssignExprStmt(AssignExprStmt assignExprStmt, Object arg)
			throws Exception {

		assignExprStmt.expr.visit(this, null);
		Dec dec = symbolTable.get(assignExprStmt.lhsIdent.getText());

		if (!((dec != null) && dec.type == assignExprStmt.expr.type)) {
			if (!(dec.type == Kind._int && assignExprStmt.expr.type == Kind.SCREEN_SIZE)) {
				errorNodeList.add(assignExprStmt);
				errorLog.append("AssignExprStmt: Error matching types of expressions." + '\n');
			}
		}
		return null;
	}

}

/************************************************* END *****************************************/
